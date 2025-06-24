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
        return Aggregates.addFields(
            Arrays.asList(
                // Conditionally update eventTechIdsList
                new Document("events.eventTechIdsList", 
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
                ),
                
                // Conditionally update completeEventList
                new Document("events.completeEventList",
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
            )
        );
    }
    
    /**
     * Alternative approach using $set with more readable structure
     */
    public static Bson createConditionalEventStageAlternative(Document incomingEvent, String eventTechId) {
        Document setStage = new Document("$set", new Document()
            .append("events.eventTechIdsList", 
                new Document("$cond", new Document()
                    .append("if", new Document("$not", 
                        new Document("$in", Arrays.asList(
                            eventTechId,
                            new Document("$ifNull", Arrays.asList("$events.eventTechIdsList", Arrays.asList()))
                        ))
                    ))
                    .append("then", new Document("$concatArrays", Arrays.asList(
                        new Document("$ifNull", Arrays.asList("$events.eventTechIdsList", Arrays.asList())),
                        Arrays.asList(eventTechId)
                    )))
                    .append("else", new Document("$ifNull", Arrays.asList("$events.eventTechIdsList", Arrays.asList())))
                )
            )
            .append("events.completeEventList",
                new Document("$cond", new Document()
                    .append("if", new Document("$not", 
                        new Document("$in", Arrays.asList(
                            eventTechId,
                            new Document("$ifNull", Arrays.asList("$events.eventTechIdsList", Arrays.asList()))
                        ))
                    ))
                    .append("then", new Document("$concatArrays", Arrays.asList(
                        new Document("$ifNull", Arrays.asList("$events.completeEventList", Arrays.asList())),
                        Arrays.asList(incomingEvent)
                    )))
                    .append("else", new Document("$ifNull", Arrays.asList("$events.completeEventList", Arrays.asList())))
                )
            )
        );
        
        return new Document("$set", setStage.get("$set"));
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
