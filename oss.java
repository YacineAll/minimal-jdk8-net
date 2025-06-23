import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.pipeline;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bson.conversions.Bson;

// …

// Assume you already have:
String transactionId = /* your _id */;
String eventTechId    = /* extracted from the new event */;
Document eventDoc     = /* your cleaned event, no _id field */;

// 1) Filter only by the document _id (we want to run this pipeline unconditionally;
 // you could combine with a nin-guard if you wanted to skip the whole pipeline instead)
Bson filter = eq("_id", transactionId);

// 2) Build the single‐stage pipeline
List<Bson> updatePipeline = Collections.singletonList(
  new Document("$set", new Document("events", 
    new Document("$cond", new Document("if", 
        new Document("$not", 
          new Document("$in", Arrays.asList(eventTechId, "$events.eventTechIdsList"))
        )
      )
      .append("then", new Document()
        .append("completeEventList", 
          new Document("$concatArrays", Arrays.asList(
            "$events.completeEventList", 
            Collections.singletonList(eventDoc)
          ))
        )
        .append("eventTechIdsList", 
          new Document("$concatArrays", Arrays.asList(
            "$events.eventTechIdsList", 
            Collections.singletonList(eventTechId)
          ))
        )
      )
      .append("else", "$events")
  ))
);

// 3) Execute the update
collection.updateOne(
  filter,
  updatePipeline,
  new UpdateOptions().upsert(true)
);