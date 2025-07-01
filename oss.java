import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.*;

@Service
public class JsonCleanupService {
    
    private final ObjectMapper objectMapper;
    
    public JsonCleanupService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    // Method 1: Remove empty and null fields recursively
    public JsonNode removeEmptyAndNullFields(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            ObjectNode cleanedNode = objectMapper.createObjectNode();
            
            objectNode.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode fieldValue = entry.getValue();
                
                // Skip null values
                if (fieldValue.isNull()) {
                    return;
                }
                
                // Skip empty strings
                if (fieldValue.isTextual() && fieldValue.asText().trim().isEmpty()) {
                    return;
                }
                
                // Recursively clean nested objects and arrays
                JsonNode cleanedValue = removeEmptyAndNullFields(fieldValue);
                
                // Only add if the cleaned value is not null and not empty
                if (cleanedValue != null && !isEmpty(cleanedValue)) {
                    cleanedNode.set(fieldName, cleanedValue);
                }
            });
            
            return cleanedNode;
            
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            ArrayNode cleanedArray = objectMapper.createArrayNode();
            
            for (JsonNode element : arrayNode) {
                JsonNode cleanedElement = removeEmptyAndNullFields(element);
                if (cleanedElement != null && !isEmpty(cleanedElement)) {
                    cleanedArray.add(cleanedElement);
                }
            }
            
            return cleanedArray;
            
        } else {
            // For primitive values, return as-is if not empty
            if (node.isTextual() && node.asText().trim().isEmpty()) {
                return null;
            }
            return node;
        }
    }
    
    // Method 2: In-place cleanup (modifies the original ObjectNode)
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
    
    // Method 3: Configurable cleanup with custom rules
    public JsonNode removeFieldsByCondition(JsonNode node, CleanupConfig config) {
        if (node == null || node.isNull()) {
            return null;
        }
        
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            ObjectNode cleanedNode = objectMapper.createObjectNode();
            
            objectNode.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode fieldValue = entry.getValue();
                
                // Apply cleanup rules
                if (shouldRemoveField(fieldValue, config)) {
                    return; // Skip this field
                }
                
                // Recursively clean nested structures
                JsonNode cleanedValue = removeFieldsByCondition(fieldValue, config);
                
                if (cleanedValue != null && !isEmpty(cleanedValue)) {
                    cleanedNode.set(fieldName, cleanedValue);
                }
            });
            
            return cleanedNode;
            
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            ArrayNode cleanedArray = objectMapper.createArrayNode();
            
            for (JsonNode element : arrayNode) {
                JsonNode cleanedElement = removeFieldsByCondition(element, config);
                if (cleanedElement != null && !isEmpty(cleanedElement)) {
                    cleanedArray.add(cleanedElement);
                }
            }
            
            return cleanedArray;
            
        } else {
            return shouldRemoveField(node, config) ? null : node;
        }
    }
    
    // Method 4: Clean JSON from string
    public String cleanJsonString(String jsonString) throws Exception {
        JsonNode originalNode = objectMapper.readTree(jsonString);
        JsonNode cleanedNode = removeEmptyAndNullFields(originalNode);
        
        if (cleanedNode == null || isEmpty(cleanedNode)) {
            return "{}"; // Return empty object if everything was removed
        }
        
        return objectMapper.writeValueAsString(cleanedNode);
    }
    
    // Method 5: Clean with pretty printing
    public String cleanJsonStringPretty(String jsonString) throws Exception {
        JsonNode originalNode = objectMapper.readTree(jsonString);
        JsonNode cleanedNode = removeEmptyAndNullFields(originalNode);
        
        if (cleanedNode == null || isEmpty(cleanedNode)) {
            return "{}";
        }
        
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cleanedNode);
    }
    
    // Helper method to check if a JsonNode is effectively empty
    private boolean isEmpty(JsonNode node) {
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
        
        return false;
    }
    
    // Helper method for in-place array cleaning
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
    
    // Helper method to determine if field should be removed based on config
    private boolean shouldRemoveField(JsonNode fieldValue, CleanupConfig config) {
        if (config.removeNull && fieldValue.isNull()) {
            return true;
        }
        
        if (config.removeEmptyStrings && fieldValue.isTextual() && 
            fieldValue.asText().trim().isEmpty()) {
            return true;
        }
        
        if (config.removeWhitespaceOnly && fieldValue.isTextual() && 
            fieldValue.asText().trim().isEmpty()) {
            return true;
        }
        
        if (config.removeZeroNumbers && fieldValue.isNumber() && 
            fieldValue.asDouble() == 0.0) {
            return true;
        }
        
        if (config.removeEmptyArrays && fieldValue.isArray() && 
            fieldValue.size() == 0) {
            return true;
        }
        
        if (config.removeEmptyObjects && fieldValue.isObject() && 
            !fieldValue.fields().hasNext()) {
            return true;
        }
        
        return false;
    }
    
    // Configuration class for cleanup rules
    public static class CleanupConfig {
        public boolean removeNull = true;
        public boolean removeEmptyStrings = true;
        public boolean removeWhitespaceOnly = true;
        public boolean removeZeroNumbers = false;
        public boolean removeEmptyArrays = true;
        public boolean removeEmptyObjects = true;
        
        public static CleanupConfig defaultConfig() {
            return new CleanupConfig();
        }
        
        public static CleanupConfig strictConfig() {
            CleanupConfig config = new CleanupConfig();
            config.removeZeroNumbers = true;
            return config;
        }
    }
    
    // Example usage demonstration
    public void demonstrateCleanup() throws Exception {
        // Sample JSON with empty and null values
        String messyJson = """
            {
                "name": "John Doe",
                "email": "",
                "phone": null,
                "age": 30,
                "address": {
                    "street": "123 Main St",
                    "city": "",
                    "state": null,
                    "zip": "12345",
                    "country": {
                        "code": "",
                        "name": null
                    }
                },
                "hobbies": ["reading", "", null, "swimming"],
                "preferences": {
                    "color": "",
                    "food": null
                },
                "metadata": null,
                "tags": [],
                "notes": "   "
            }
            """;
        
        System.out.println("Original JSON:");
        System.out.println(messyJson);
        
        // Method 1: Basic cleanup
        System.out.println("\n=== Method 1: Basic Cleanup ===");
        String cleaned1 = cleanJsonString(messyJson);
        System.out.println(cleaned1);
        
        // Method 2: In-place cleanup
        System.out.println("\n=== Method 2: In-place Cleanup ===");
        ObjectNode originalNode = (ObjectNode) objectMapper.readTree(messyJson);
        removeEmptyAndNullFieldsInPlace(originalNode);
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(originalNode));
        
        // Method 3: Configurable cleanup
        System.out.println("\n=== Method 3: Strict Cleanup (including zeros) ===");
        JsonNode strictCleaned = removeFieldsByCondition(
            objectMapper.readTree(messyJson), 
            CleanupConfig.strictConfig()
        );
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(strictCleaned));
        
        // Method 4: Pretty printed cleanup
        System.out.println("\n=== Method 4: Pretty Printed Cleanup ===");
        String prettycleaned = cleanJsonStringPretty(messyJson);
        System.out.println(prettycleaned);
    }
}
