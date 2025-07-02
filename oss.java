import com.fasterxml.jackson.databind.JsonNode;

public class JsonUtils {
    public static boolean pathExists(JsonNode root, String path) {
        if (root == null || path == null || path.isEmpty()) return false;

        String[] fields = path.startsWith("/") ? path.substring(1).split("/") : path.split("/");

        JsonNode current = root;
        for (String field : fields) {
            if (current.has(field)) {
                current = current.get(field);
            } else {
                return false;
            }
        }
        return true;
    }
}


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void ensurePathWithValue(ObjectNode root, String path, String value) {
        String[] fields = path.startsWith("/") ? path.substring(1).split("/") : path.split("/");

        ObjectNode current = root;
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];

            boolean isLast = i == fields.length - 1;

            JsonNode next = current.get(field);

            if (isLast) {
                if (next == null || next.isNull()) {
                    current.put(field, value);
                }
            } else {
                if (next == null || !next.isObject()) {
                    ObjectNode child = mapper.createObjectNode();
                    current.set(field, child);
                    current = child;
                } else {
                    current = (ObjectNode) next;
                }
            }
        }
    }
}
