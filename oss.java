import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class PathMatcher {

    private final String[][] patterns;

    public PathMatcher(List<String> patterns) {
        this.patterns = new String[patterns.size()][];
        for (int i = 0; i < patterns.size(); i++) {
            this.patterns[i] = patterns.get(i).split("/");
        }
    }

    /** Applies fn to every value whose key matches one of the patterns, in place. */
    public void replace(Map<String, Object> data, Function<Object, Object> fn) {
        data.replaceAll((k, v) -> matches(k) ? fn.apply(v) : v);
    }

    private boolean matches(String key) {
        for (String[] p : patterns) {
            if (matches(p, key)) return true;
        }
        return false;
    }

    private static boolean matches(String[] segments, String key) {
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
}
