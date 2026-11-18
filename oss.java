import java.util.Map;
import java.util.function.Function;

public final class PathMatcher {

    private final String[] segments;

    public PathMatcher(String pattern) {
        this.segments = pattern.split("/");
    }

    /** Applies fn to every value whose key matches the pattern, in place. */
    public void replace(Map<String, Object> data, Function<Object, Object> fn) {
        data.replaceAll((k, v) -> matches(k) ? fn.apply(v) : v);
    }

    private boolean matches(String key) {
        int pos = 0;
        final int len = key.length();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                if (pos >= len || key.charAt(pos) != '/') return false;
                pos++;
            }
            String seg = segments[i];
            if (seg.length() == 1 && seg.charAt(0) == '*') {
                if (pos >= len || key.charAt(pos) != '@') return false;
                int start = ++pos;
                while (pos < len) {
                    char c = key.charAt(pos);
                    if (c < '0' || c > '9') break;
                    pos++;
                }
                if (pos == start) return false;
            } else {
                if (!key.startsWith(seg, pos)) return false;
                pos += seg.length();
            }
        }
        return pos == len;
    }






private static final int NO_MATCH = -1;

private boolean matches(String key) {
    int pos = 0;
    for (int i = 0; i < segments.length; i++) {
        if (i > 0) {
            pos = consumeChar(key, pos, '/');
        }
        if (pos != NO_MATCH) {
            pos = matchSegment(key, pos, segments[i]);
        }
        if (pos == NO_MATCH) {
            return false;
        }
    }
    return pos == key.length();
}

/** Returns the position after the segment, or NO_MATCH. */
private static int matchSegment(String key, int pos, String seg) {
    if ("*".equals(seg)) {
        return matchWildcard(key, pos);
    }
    return key.startsWith(seg, pos) ? pos + seg.length() : NO_MATCH;
}

/** Matches '@' followed by at least one ASCII digit. */
private static int matchWildcard(String key, int pos) {
    int start = consumeChar(key, pos, '@');
    if (start == NO_MATCH) {
        return NO_MATCH;
    }
    int end = start;
    while (end < key.length() && isAsciiDigit(key.charAt(end))) {
        end++;
    }
    return end > start ? end : NO_MATCH;
}

/** Returns pos + 1 if the char at pos is the expected one, otherwise NO_MATCH. */
private static int consumeChar(String key, int pos, char expected) {
    return pos < key.length() && key.charAt(pos) == expected ? pos + 1 : NO_MATCH;
}

private static boolean isAsciiDigit(char c) {
    return c >= '0' && c <= '9';
}


}
