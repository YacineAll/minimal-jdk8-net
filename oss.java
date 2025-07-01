import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.*;

@Service
public class JsonUtilityService {
    
    /**
     * Removes empty and null fields from a JsonNode in-place (modifies the original node)
     * @param node The JsonNode to clean (must be ObjectNode for modification)
     */
    public void removeEmptyAndNullFieldsInPlace(JsonNode node) {
        if (node == null || !node.isObject()) {
            return;
        }
        
        ObjectNode objectNode = (ObjectNode) node;
        List<String> fieldsToRemove = new ArrayList<>();
        
        objectNode.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode fieldValue = entry.getValue();
            
            // Mark for removal if null or empty string
            if (fieldValue.isNull() || 
                (fieldValue.isTextual() && fieldValue.asText().trim().isEmpty())) {
                fieldsToRemove.add(fieldName);
            } else if (fieldValue.isObject()) {
                // Recursively clean nested objects
                removeEmptyAndNullFieldsInPlace(fieldValue);
                // Remove if object becomes empty after cleaning
                if (!fieldValue.fields().hasNext()) {
                    fieldsToRemove.add(fieldName);
                }
            } else if (fieldValue.isArray()) {
                // Clean array elements
                cleanArrayInPlace((ArrayNode) fieldValue);
                // Remove if array becomes empty after cleaning
                if (fieldValue.size() == 0) {
                    fieldsToRemove.add(fieldName);
                }
            }
        });
        
        // Remove marked fields
        fieldsToRemove.forEach(objectNode::remove);
    }
    
    /**
     * Helper method for cleaning arrays in-place
     * @param arrayNode The ArrayNode to clean
     */
    private void cleanArrayInPlace(ArrayNode arrayNode) {
        List<Integer> indicesToRemove = new ArrayList<>();
        
        for (int i = 0; i < arrayNode.size(); i++) {
            JsonNode element = arrayNode.get(i);
            
            if (element.isNull() || 
                (element.isTextual() && element.asText().trim().isEmpty())) {
                indicesToRemove.add(i);
            } else if (element.isObject()) {
                removeEmptyAndNullFieldsInPlace(element);
                if (!element.fields().hasNext()) {
                    indicesToRemove.add(i);
                }
            }
        }
        
        // Remove in reverse order to maintain indices
        for (int i = indicesToRemove.size() - 1; i >= 0; i--) {
            arrayNode.remove(indicesToRemove.get(i));
        }
    }
    
    /**
     * Checks if a JsonNode is effectively empty
     * @param node The JsonNode to check
     * @return true if the node is null, empty string, empty object, or empty array
     */
    public boolean isEmpty(JsonNode node) {
        if (node == null || node.isNull()) {
            return true;
        }
        
        if (node.isTextual()) {
            return node.asText().trim().isEmpty();
        }
        
        if (node.isObject()) {
            return !node.fields().hasNext();
        }
        
        if (node.isArray()) {
            return node.size() == 0;
        }
        
        // For other types (numbers, booleans), they are not considered empty
        return false;
    }
    
    /**
     * Extended isEmpty check with additional options
     * @param node The JsonNode to check
     * @param checkZeroNumbers If true, considers zero numbers as empty
     * @param checkFalseBoolean If true, considers false boolean as empty
     * @return true if the node is considered empty based on criteria
     */
    public boolean isEmpty(JsonNode node, boolean checkZeroNumbers, boolean checkFalseBoolean) {
        // Basic empty check first
        if (isEmpty(node)) {
            return true;
        }
        
        // Additional checks
        if (checkZeroNumbers && node.isNumber() && node.asDouble() == 0.0) {
            return true;
        }
        
        if (checkFalseBoolean && node.isBoolean() && !node.asBoolean()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Checks if a specific field in a JsonNode is empty
     * @param node The parent JsonNode
     * @param fieldPath The field path (e.g., "user.name" or "user.contact.email")
     * @return true if the field is empty or doesn't exist
     */
    public boolean isFieldEmpty(JsonNode node, String fieldPath) {
        JsonNode fieldNode = getFieldByPath(node, fieldPath);
        return isEmpty(fieldNode);
    }
    
    /**
     * Helper method to get a field by path
     * @param node The root JsonNode
     * @param path The field path (dot notation)
     * @return The JsonNode at the path, or null if not found
     */
    private JsonNode getFieldByPath(JsonNode node, String path) {
        if (node == null || path == null || path.isEmpty()) {
            return null;
        }
        
        String[] pathParts = path.split("\\.");
        JsonNode currentNode = node;
        
        for (String part : pathParts) {
            currentNode = currentNode.path(part);
            if (currentNode.isMissingNode()) {
                return null;
            }
        }
        
        return currentNode;
    }
    
    // Example usage demonstration
    public void demonstrateUsage() throws Exception {
        String jsonString = """
            {
                "name": "John Doe",
                "email": "",
                "phone": null,
                "age": 30,
                "active": true,
                "balance": 0.0,
                "address": {
                    "street": "123 Main St",
                    "city": "",
                    "state": null,
                    "country": {
                        "code": "",
                        "name": null
                    }
                },
                "hobbies": ["reading", "", null, "swimming"],
                "preferences": {},
                "tags": []
            }
            """;
        
        // Parse JSON
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = (ObjectNode) mapper.readTree(jsonString);
        
        System.out.println("Original JSON:");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode));
        
        // Test isEmpty function
        System.out.println("\n=== isEmpty Tests ===");
        System.out.println("email field is empty: " + isEmpty(rootNode.get("email")));
        System.out.println("phone field is empty: " + isEmpty(rootNode.get("phone")));
        System.out.println("name field is empty: " + isEmpty(rootNode.get("name")));
        System.out.println("preferences object is empty: " + isEmpty(rootNode.get("preferences")));
        System.out.println("tags array is empty: " + isEmpty(rootNode.get("tags")));
        System.out.println("balance is empty (with zero check): " + isEmpty(rootNode.get("balance"), true, false));
        
        // Test field path checking
        System.out.println("\n=== Field Path Tests ===");
        System.out.println("address.city is empty: " + isFieldEmpty(rootNode, "address.city"));
        System.out.println("address.street is empty: " + isFieldEmpty(rootNode, "address.street"));
        System.out.println("address.country.name is empty: " + isFieldEmpty(rootNode, "address.country.name"));
        
        // Clean the JSON in-place
        System.out.println("\n=== Before In-Place Cleanup ===");
        System.out.println("Fields count: " + countFields(rootNode));
        
        removeEmptyAndNullFieldsInPlace(rootNode);
        
        System.out.println("\n=== After In-Place Cleanup ===");
        System.out.println("Fields count: " + countFields(rootNode));
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode));
    }
    
    // Helper method to count fields recursively
    private int countFields(JsonNode node) {
        if (node == null || !node.isObject()) {
            return 0;
        }
        
        int count = 0;
        var fields = node.fields();
        while (fields.hasNext()) {
            var field = fields.next();
            count++;
            if (field.getValue().isObject()) {
                count += countFields(field.getValue());
            }
        }
        return count;
    }
}
