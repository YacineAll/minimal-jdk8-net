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
     * Example usage in your existing upsert pipeline
     */
    public static void exampleUsage() {
        // Your incoming event
        Document incomingEvent = new Document()
            .append("eventTechId", "event123")
            .append("eventData", "some data")
            .append("timestamp", System.currentTimeMillis());
        
        String eventTechId = incomingEvent.getString("eventTechId");
        
        // Create the conditional stage
        Bson conditionalEventStage = createConditionalEventStage(incomingEvent, eventTechId);
        
        // Add to your existing pipeline
        List<Bson> pipeline = Arrays.asList(
            // ... your existing pipeline stages ...
            conditionalEventStage
            // ... any additional stages ...
        );
        
        // Use in updateOne with upsert
        // collection.updateOne(filter, pipeline, new UpdateOptions().upsert(true));
    }
}
