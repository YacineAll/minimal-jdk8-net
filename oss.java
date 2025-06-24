import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import java.util.Arrays;
import java.util.List;

public class ConditionalEventUpsert {
    
    /**
     * Creates a pipeline stage that conditionally adds an event to both 
     * eventTechIdsList and completeEventList if the eventTechId doesn't already exist
     * 
     * @param incomingEvent The event object to potentially add
     * @param eventTechId The unique identifier for the event
     * @return Bson pipeline stage for conditional upsert
     */
    public static Bson createConditionalEventStage(Document incomingEvent, String eventTechId) {
        return new Document("$set", new Document("events", 
            new Document("$mergeObjects", Arrays.asList(
                // Start with existing events object (or empty object if null)
                new Document("$ifNull", Arrays.asList("$events", new Document())),
                // Merge with conditionally updated fields
                new Document()
                    .append("eventTechIdsList", 
                        new Document("$cond", Arrays.asList(
                            // Condition: if eventTechId is NOT in the existing list
                            new Document("$not", Arrays.asList(
                                new Document("$in", Arrays.asList(
                                    eventTechId,
                                    new Document("$ifNull", Arrays.asList(
                                        "$events.eventTechIdsList", 
                                        Arrays.asList()
                                    ))
                                ))
                            )),
                            // If true: append eventTechId to existing list
                            new Document("$concatArrays", Arrays.asList(
                                new Document("$ifNull", Arrays.asList(
                                    "$events.eventTechIdsList", 
                                    Arrays.asList()
                                )),
                                Arrays.asList(eventTechId)
                            )),
                            // If false: keep existing list unchanged
                            new Document("$ifNull", Arrays.asList(
                                "$events.eventTechIdsList", 
                                Arrays.asList()
                            ))
                        ))
                    )
                    .append("completeEventList",
                        new Document("$cond", Arrays.asList(
                            // Same condition: if eventTechId is NOT in the existing list
                            new Document("$not", Arrays.asList(
                                new Document("$in", Arrays.asList(
                                    eventTechId,
                                    new Document("$ifNull", Arrays.asList(
                                        "$events.eventTechIdsList", 
                                        Arrays.asList()
                                    ))
                                ))
                            )),
                            // If true: append event object to existing list
                            new Document("$concatArrays", Arrays.asList(
                                new Document("$ifNull", Arrays.asList(
                                    "$events.completeEventList", 
                                    Arrays.asList()
                                )),
                                Arrays.asList(incomingEvent)
                            )),
                            // If false: keep existing list unchanged
                            new Document("$ifNull", Arrays.asList(
                                "$events.completeEventList", 
                                Arrays.asList()
                            ))
                        ))
                    )
            ))
        ));
    }
    
    /**
     * Alternative approach - more concise using variables
     */
    public static Bson createConditionalEventStageWithVariables(Document incomingEvent, String eventTechId) {
        return new Document("$set", new Document("events", 
            new Document("$let", new Document()
                .append("vars", new Document()
                    .append("existingEvents", new Document("$ifNull", Arrays.asList("$events", new Document())))
                    .append("existingIds", new Document("$ifNull", Arrays.asList("$events.eventTechIdsList", Arrays.asList())))
                    .append("existingEventList", new Document("$ifNull", Arrays.asList("$events.completeEventList", Arrays.asList())))
                    .append("shouldAdd", new Document("$not", 
                        new Document("$in", Arrays.asList(eventTechId, new Document("$ifNull", Arrays.asList("$events.eventTechIdsList", Arrays.asList()))))
                    ))
                )
                .append("in", new Document("$mergeObjects", Arrays.asList(
                    "$existingEvents",
                    new Document()
                        .append("eventTechIdsList", new Document("$cond", Arrays.asList(
                            "$shouldAdd",
                            new Document("$concatArrays", Arrays.asList("$existingIds", Arrays.asList(eventTechId))),
                            "$existingIds"
                        )))
                        .append("completeEventList", new Document("$cond", Arrays.asList(
                            "$shouldAdd", 
                            new Document("$concatArrays", Arrays.asList("$existingEventList", Arrays.asList(incomingEvent))),
                            "$existingEventList"
                        )))
                )))
            )
        ));
    }
    
    /**
     * Example usage scenarios for each solution
     */
    public static void exampleUsageScenarios() {
        Document incomingEvent = new Document()
            .append("eventTechId", "event123")
            .append("eventData", "some data")
            .append("timestamp", System.currentTimeMillis());
        
        String eventTechId = incomingEvent.getString("eventTechId");
        
        // SCENARIO 1: You have an existing $set stage and want to add to it
        Document existingSetStage = new Document()
            .append("someField", "someValue")
            .append("anotherField", new Document("$add", Arrays.asList("$field1", "$field2")));
        
        // Add conditional event fields to existing $set
        addConditionalEventToExistingSet(existingSetStage, incomingEvent, eventTechId);
        
        List<Bson> pipeline1 = Arrays.asList(
            // ... other stages ...
            new Document("$set", existingSetStage)
            // ... other stages ...
        );
        
        // SCENARIO 2: Add as separate stage after your existing $set
        List<Bson> pipeline2 = Arrays.asList(
            // ... other stages ...
            new Document("$set", new Document()
                .append("someField", "someValue")
                .append("anotherField", new Document("$add", Arrays.asList("$field1", "$field2")))
            ),
            // Add the conditional event stage separately
            createSeparateConditionalEventStage(incomingEvent, eventTechId)
            // ... other stages ...
        );
        
        // SCENARIO 3: Manual integration - get the field definitions
        List<Document> eventFields = createConditionalEventFields(incomingEvent, eventTechId);
        
        Document manualSetStage = new Document()
            .append("someField", "someValue")
            .append("anotherField", new Document("$add", Arrays.asList("$field1", "$field2")));
        
        // Add each event field manually
        for (Document field : eventFields) {
            for (String key : field.keySet()) {
                manualSetStage.append(key, field.get(key));
            }
        }
        
        List<Bson> pipeline3 = Arrays.asList(
            // ... other stages ...
            new Document("$set", manualSetStage)
            // ... other stages ...
        );
    }
}
