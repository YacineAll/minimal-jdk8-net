from datetime import datetime, timezone, timedelta
from zoneinfo import ZoneInfo


def _frac(d, digits):
    """Fraction of second with 1..9 digits (Python stops at microseconds, so pad with zeros)."""
    return f"{d.microsecond:06d}{'000'}"[:digits]


def _offset(d, sep=":", seconds=False):
    """Offset as +HH:MM (or +HH:MM:SS). Z is never produced here."""
    off = d.utcoffset() or timedelta(0)
    total = int(off.total_seconds())
    sign = "+" if total >= 0 else "-"
    total = abs(total)
    hh, mm, ss = total // 3600, (total % 3600) // 60, total % 60
    out = f"{sign}{hh:02d}{sep}{mm:02d}"
    return out + f"{sep}{ss:02d}" if seconds else out


def _utc(d):
    return d.astimezone(timezone.utc)


# Only the formats accepted by DateTimeNormalizerHandler (FLEXIBLE_ISO).
# `d` must be a timezone-aware datetime.
ISO_FORMATS = {
    # ---- date only ----
    "date":                   lambda d: d.strftime("%Y-%m-%d"),                                      # 2026-11-20
    "date_z":                 lambda d: _utc(d).strftime("%Y-%m-%dZ"),                               # 2026-11-20Z
    "date_offset":            lambda d: d.strftime("%Y-%m-%d") + _offset(d),                         # 2026-11-20+01:00
    # ---- local date-time (no offset -> Europe/Paris) ----
    "dt_local_min":           lambda d: d.strftime("%Y-%m-%dT%H:%M"),                                # 2026-11-20T16:14
    "dt_local_sec":           lambda d: d.strftime("%Y-%m-%dT%H:%M:%S"),                             # 2026-11-20T16:14:05
    "dt_local_frac1":         lambda d: d.strftime("%Y-%m-%dT%H:%M:%S.") + _frac(d, 1),              # ...05.1
    "dt_local_ms":            lambda d: d.strftime("%Y-%m-%dT%H:%M:%S.") + _frac(d, 3),              # ...05.123
    "dt_local_us":            lambda d: d.strftime("%Y-%m-%dT%H:%M:%S.") + _frac(d, 6),              # ...05.123456
    "dt_local_ns":            lambda d: d.strftime("%Y-%m-%dT%H:%M:%S.") + _frac(d, 9),              # ...05.123456000
    # ---- UTC "Z" ----
    "dt_utc_min_z":           lambda d: _utc(d).strftime("%Y-%m-%dT%H:%MZ"),                         # 2026-11-20T15:14Z
    "dt_utc_sec_z":           lambda d: _utc(d).strftime("%Y-%m-%dT%H:%M:%SZ"),                      # 2026-11-20T15:14:05Z
    "dt_utc_ms_z":            lambda d: _utc(d).strftime("%Y-%m-%dT%H:%M:%S.") + _frac(d, 3) + "Z",  # ...05.123Z
    "dt_utc_us_z":            lambda d: _utc(d).strftime("%Y-%m-%dT%H:%M:%S.") + _frac(d, 6) + "Z",  # ...05.123456Z
    "dt_utc_ns_z":            lambda d: _utc(d).strftime("%Y-%m-%dT%H:%M:%S.") + _frac(d, 9) + "Z",  # ...05.123456000Z
    "dt_utc_plus00":          lambda d: _utc(d).strftime("%Y-%m-%dT%H:%M:%S+00:00"),                 # ...05+00:00
    # ---- with offset (+HH:MM or +HH:MM:SS only) ----
    "dt_offset_min":          lambda d: d.strftime("%Y-%m-%dT%H:%M") + _offset(d),                   # 2026-11-20T16:14+01:00
    "dt_offset_sec":          lambda d: d.strftime("%Y-%m-%dT%H:%M:%S") + _offset(d),                # ...05+01:00
    "dt_offset_with_seconds": lambda d: d.strftime("%Y-%m-%dT%H:%M:%S") + _offset(d, seconds=True),  # ...05+01:00:00
    "dt_offset_ms":           lambda d: d.strftime("%Y-%m-%dT%H:%M:%S.") + _frac(d, 3) + _offset(d), # ...05.123+01:00
    "dt_offset_us":           lambda d: d.strftime("%Y-%m-%dT%H:%M:%S.") + _frac(d, 6) + _offset(d), # ...05.123456+01:00
    "dt_negative_offset":     lambda d: d.astimezone(ZoneInfo("America/New_York"))
                                         .strftime("%Y-%m-%dT%H:%M:%S") + _offset(d.astimezone(ZoneInfo("America/New_York"))),  # ...-05:00
    "python_isoformat":       lambda d: d.isoformat(),                                               # 2026-11-20T16:14:05.123456+01:00
}


if __name__ == "__main__":
    now = datetime.now(ZoneInfo("Europe/Paris"))
    for name, fmt in ISO_FORMATS.items():
        print(f"{name:24} {fmt(now)}")
