"""
Fill / overwrite date fields of an event JSON with random dates
rendered in random ISO 8601 formats.

- Missing intermediate objects/lists are created.
- Existing values are overwritten.
- Path syntax:
    a.b.c        -> nested keys
    a.b[0].c     -> list index (list is created/extended if needed)
    a.b[].c      -> every element of the list (one element is created if empty)
  If a key without [] already holds a list (e.g. eventsList), every element is updated.

Python 3.8+, standard library only.
"""

import copy
import json
import random
import re
from datetime import datetime, timedelta, timezone

# --------------------------------------------------------------------------
# Paths to update (from your sheet)
# --------------------------------------------------------------------------
DATE_PATHS = [
    "payload.paymentEngineRunningData.sndgDts[].sndDt",
    "payload.businessPaymentInformation.creDtTm",
    "payload.paymentEngineRunningData.debtFbkDt",
    "payload.paymentEngineRunningData.creditFbkDt",
    "payload.paymentEngineRunningData.actnOnPmt[0].actDt",
    "payload.businessPaymentInformation.othrDts[].othrDt",
    "payload.businessPaymentInformation.intrBkSttlmDt",
    "payload.businessPaymentInformation.reqdExctnDt",
    "payload.paymentEngineRunningData.pmtTrtmtDtTm.strtTrtmtDtTm.dtStrtTrtmt",
    "payload.paymentEngineRunningData.valDtToUseAtCdtSd",
    "payload.paymentEngineRunningData.bookgDts.cdtBookgDt",
    "payload.paymentEngineRunningData.bookgDts.dbtBookgDt",
    "payload.paymentEngineRunningData.bookgDts.dbtBookEndgDt",
    "payload.paymentEngineRunningData.bookgDts.cdtBookEndgDt",
    "payload.paymentEngineRunningData.valgDts.cdtValDt",
    "payload.paymentEngineRunningData.valgDts.dbtValDt",
    "payload.businessPaymentInformation.orgnlTxRef.orgnlIntrBkSttlmDt",
    "rLinks.lastUpdate",  # check: the start of this path is cut off in the screenshot
    "lastEvent.processingTrace.processedAt",
    "eventsList.processingTrace.processedAt",
]

# --------------------------------------------------------------------------
# ISO 8601 formats
# --------------------------------------------------------------------------
def _offset(dt, sep=":", minutes=True):
    """Format the UTC offset: Z / +01:00 / +0100 / +01."""
    off = dt.utcoffset()
    if off == timedelta(0) and random.random() < 0.5:
        return "Z"
    total = int(off.total_seconds() // 60)
    sign = "+" if total >= 0 else "-"
    h, m = divmod(abs(total), 60)
    if not minutes:
        return f"{sign}{h:02d}"
    return f"{sign}{h:02d}{sep}{m:02d}"


def _frac(dt, digits):
    return f"{dt.microsecond:06d}"[:digits]


ISO_FORMATS = {
    # ---- date only ----
    "date_extended":          lambda d: d.strftime("%Y-%m-%d"),                      # 2026-11-20
    "date_basic":             lambda d: d.strftime("%Y%m%d"),                        # 20261120
    "year_month":             lambda d: d.strftime("%Y-%m"),                         # 2026-11
    "week_date":              lambda d: "{}-W{:02d}-{}".format(*d.isocalendar()),    # 2026-W47-5
    "week_date_basic":        lambda d: "{}W{:02d}{}".format(*d.isocalendar()),      # 2026W475
    "ordinal_date":           lambda d: d.strftime("%Y-%j"),                         # 2026-324
    "ordinal_date_basic":     lambda d: d.strftime("%Y%j"),                          # 2026324
    # ---- local date-time (no offset) ----
    "dt_local_min":           lambda d: d.strftime("%Y-%m-%dT%H:%M"),                # 2026-11-20T16:14
    "dt_local_sec":           lambda d: d.strftime("%Y-%m-%dT%H:%M:%S"),             # 2026-11-20T16:14:05
    "dt_local_ms":            lambda d: d.strftime("%Y-%m-%dT%H:%M:%S.") + _frac(d, 3),
    "dt_local_us":            lambda d: d.strftime("%Y-%m-%dT%H:%M:%S.") + _frac(d, 6),
    "dt_local_comma_frac":    lambda d: d.strftime("%Y-%m-%dT%H:%M:%S,") + _frac(d, 3),
    "dt_basic_local":         lambda d: d.strftime("%Y%m%dT%H%M%S"),                 # 20261120T161405
    # ---- UTC "Z" ----
    "dt_utc_min_z":           lambda d: d.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%MZ"),
    "dt_utc_sec_z":           lambda d: d.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
    "dt_utc_ms_z":            lambda d: d.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.") + _frac(d, 3) + "Z",
    "dt_utc_us_z":            lambda d: d.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.") + _frac(d, 6) + "Z",
    "dt_basic_utc_z":         lambda d: d.astimezone(timezone.utc).strftime("%Y%m%dT%H%M%SZ"),
    "dt_utc_plus00":          lambda d: d.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S+00:00"),
    # ---- with offset ----
    "dt_offset_colon":        lambda d: d.strftime("%Y-%m-%dT%H:%M:%S") + _offset(d, ":"),           # +01:00
    "dt_offset_no_colon":     lambda d: d.strftime("%Y-%m-%dT%H:%M:%S") + _offset(d, ""),            # +0100
    "dt_offset_hours_only":   lambda d: d.strftime("%Y-%m-%dT%H:%M:%S") + _offset(d, minutes=False), # +01
    "dt_offset_ms":           lambda d: d.strftime("%Y-%m-%dT%H:%M:%S.") + _frac(d, 3) + _offset(d, ":"),
    "dt_offset_us":           lambda d: d.strftime("%Y-%m-%dT%H:%M:%S.") + _frac(d, 6) + _offset(d, ":"),
    "dt_offset_min":          lambda d: d.strftime("%Y-%m-%dT%H:%M") + _offset(d, ":"),
    "dt_basic_offset":        lambda d: d.strftime("%Y%m%dT%H%M%S") + _offset(d, ""),
    "python_isoformat":       lambda d: d.isoformat(),
}

# Some offsets to make timezone handling interesting (incl. half-hour ones)
OFFSETS_MIN = [0, 60, 120, -300, -240, 330, 345, 540, -210, 840, -720]


def random_datetime(start_year=2000, end_year=2035):
    start = datetime(start_year, 1, 1)
    end = datetime(end_year, 12, 31, 23, 59, 59)
    seconds = random.randint(0, int((end - start).total_seconds()))
    dt = start + timedelta(seconds=seconds, microseconds=random.randint(0, 999_999))
    tz = timezone(timedelta(minutes=random.choice(OFFSETS_MIN)))
    return dt.replace(tzinfo=tz)


def random_iso_date(fmt_name=None):
    """Return (format_name, value). fmt_name=None -> random format."""
    fmt_name = fmt_name or random.choice(list(ISO_FORMATS))
    return fmt_name, ISO_FORMATS[fmt_name](random_datetime())


# --------------------------------------------------------------------------
# Path handling
# --------------------------------------------------------------------------
# Size of arrays for paths with [] (e.g. sndgDts[]):
#   random between ARRAY_MIN and ARRAY_MAX (0 = empty array is tested too),
#   or forced to ARRAY_SIZE when it is not None.
ARRAY_MIN, ARRAY_MAX = 0, 5
ARRAY_SIZE = None

_TOKEN_RE = re.compile(r"([^.\[\]]+)|\[(\d*)\]")


def parse_path(path):
    """'a.b[].c[0].d' -> [('key','a'),('key','b'),('all',None),('key','c'),('idx',0),('key','d')]"""
    tokens = []
    for key, idx in _TOKEN_RE.findall(path):
        if key:
            tokens.append(("key", key))
        elif idx == "":
            tokens.append(("all", None))
        else:
            tokens.append(("idx", int(idx)))
    return tokens


def _empty_for(next_token):
    return [] if next_token and next_token[0] in ("all", "idx") else {}


def set_path(node, tokens, value_factory, report, path_str):
    """Recursively create/overwrite the value at tokens. value_factory() -> (fmt, value)."""
    tok_type, tok = tokens[0]
    rest = tokens[1:]
    nxt = rest[0] if rest else None

    # Implicit list (e.g. eventsList holds a list but path has no []): apply to each element
    if tok_type == "key" and isinstance(node, list):
        if not node:
            node.append({})
        for item in node:
            set_path(item, tokens, value_factory, report, path_str)
        return

    if tok_type == "key":
        if not isinstance(node, dict):
            raise TypeError(f"{path_str}: expected object at '{tok}', got {type(node).__name__}")
        if not rest:
            fmt, val = value_factory()
            node[tok] = val
            report.append((path_str, fmt, val))
            return
        if tok not in node or not isinstance(node[tok], (dict, list)):
            node[tok] = _empty_for(nxt)
        set_path(node[tok], rest, value_factory, report, path_str)

    elif tok_type == "idx":
        if not isinstance(node, list):
            raise TypeError(f"{path_str}: expected array before [{tok}]")
        while len(node) <= tok:
            node.append(_empty_for(nxt) if rest else None)
        if not rest:
            fmt, val = value_factory()
            node[tok] = val
            report.append((f"{path_str}", fmt, val))
            return
        if not isinstance(node[tok], (dict, list)):
            node[tok] = _empty_for(nxt)
        set_path(node[tok], rest, value_factory, report, path_str)

    elif tok_type == "all":
        if not isinstance(node, list):
            raise TypeError(f"{path_str}: expected array before []")
        # Decide how many elements the array must have
        size = ARRAY_SIZE if ARRAY_SIZE is not None else random.randint(ARRAY_MIN, ARRAY_MAX)
        if ARRAY_SIZE is not None and len(node) > size:
            del node[size:]                 # forced size: shrink (e.g. 0 -> empty array)
        while len(node) < size:             # grow; existing elements are kept
            node.append(_empty_for(nxt) if rest else None)
        if not node:
            report.append((path_str, "EMPTY_ARRAY", "[]"))
            return
        for i in range(len(node)):
            if not rest:
                fmt, val = value_factory()
                node[i] = val
                report.append((path_str, fmt, val))
            else:
                if not isinstance(node[i], (dict, list)):
                    node[i] = _empty_for(nxt)
                set_path(node[i], rest, value_factory, report, path_str)


# --------------------------------------------------------------------------
# Public API
# --------------------------------------------------------------------------
def randomize_dates(event, paths=DATE_PATHS, fmt_name=None, seed=None, in_place=False,
                    array_size=None, array_min=0, array_max=5):
    """
    Return (new_event, report). report = [(path, format_name, value), ...]
    fmt_name:   force one format for every field (None = random per field).
    array_size: force the number of elements of every [] array (0 = empty array).
                None = random between array_min and array_max.
    """
    global ARRAY_SIZE, ARRAY_MIN, ARRAY_MAX
    ARRAY_SIZE, ARRAY_MIN, ARRAY_MAX = array_size, array_min, array_max
    if seed is not None:
        random.seed(seed)
    event = event if in_place else copy.deepcopy(event or {})
    report = []
    for p in paths:
        set_path(event, parse_path(p), lambda: random_iso_date(fmt_name), report, p)
    return event, report


def generate_array_size_variants(event, paths=DATE_PATHS, sizes=range(0, 6), seed=None):
    """One event per array size (0 = empty array, 1..5 elements). Yields (size, event)."""
    if seed is not None:
        random.seed(seed)
    for n in sizes:
        new_event, _ = randomize_dates(event, paths, array_size=n)
        yield n, new_event


def generate_all_format_variants(event, paths=DATE_PATHS, seed=None):
    """
    One event per ISO format (every field uses that format) -> guarantees
    that every format is tested on every field.
    Yields (format_name, event).
    """
    if seed is not None:
        random.seed(seed)
    for fmt in ISO_FORMATS:
        new_event, _ = randomize_dates(event, paths, fmt_name=fmt)
        yield fmt, new_event


# --------------------------------------------------------------------------
# Demo
# --------------------------------------------------------------------------
if __name__ == "__main__":
    input_event = {
        # Existing values are overwritten, other fields kept
        "eventsList": [
            {"processingTrace": {"processedAt": "old", "step": "A"}},
            {"processingTrace": {"processedAt": "old", "step": "B"}},
        ],
        "payload": {"businessPaymentInformation": {"creDtTm": "old", "amount": 10}},
    }

    # 1) Random format per field
    out, report = randomize_dates(input_event, seed=42)
    print(json.dumps(out, indent=2))
    print("\n--- fields set ---")
    for path, fmt, val in report:
        print(f"{fmt:22} {val:35} {path}")

    # 2) One event per array size (0..5) for sndgDts[] / othrDts[]
    print("\n--- one event per array size ---")
    for n, ev in generate_array_size_variants({}, seed=7):
        print(n, json.dumps(ev["payload"]["paymentEngineRunningData"]["sndgDts"]))

    # 3) One event per format -> full coverage
    print("\n--- one event per format ---")
    for fmt, ev in generate_all_format_variants({}, seed=1):
        print(f"{fmt:22} creDtTm = {ev['payload']['businessPaymentInformation']['creDtTm']}")
