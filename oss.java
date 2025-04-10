import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public class DayFileProcessor {
    
    private static final String KAFKA_TOPIC = "file-contents-topic";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String BASE_DIRECTORY = "/path/to/your/base/directory"; // Change this
    private static final int MAX_CONCURRENCY = Runtime.getRuntime().availableProcessors();
    
    public static void main(String[] args) {
        // Create Kafka producer configuration
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        
        try (Producer<String, String> producer = new KafkaProducer<>(props);
             ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            Path basePath = Paths.get(BASE_DIRECTORY);
            
            // Find all directories matching the pattern "/day=*/"
            try (Stream<Path> dayDirs = Files.list(basePath)
                    .filter(path -> Files.isDirectory(path) && path.getFileName().toString().startsWith("day="))) {
                
                dayDirs.forEach(dayDir -> processDirectory(dayDir, producer, executor));
            }
            
            // Ensure all tasks are completed
            executor.shutdown();
            if (!executor.awaitTermination(1, java.util.concurrent.TimeUnit.HOURS)) {
                System.err.println("Tasks did not complete in the allocated time");
            }
            
        } catch (IOException e) {
            System.err.println("Error accessing directories: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Processing was interrupted: " + e.getMessage());
        }
    }
    
    private static void processDirectory(Path directory, Producer<String, String> producer, ExecutorService executor) {
        System.out.println("Processing directory: " + directory);
        
        try (Stream<Path> files = Files.list(directory).filter(Files::isRegularFile)) {
            files.forEach(file -> {
                CompletableFuture.runAsync(() -> processFile(file, producer), executor)
                    .exceptionally(ex -> {
                        System.err.println("Error processing file " + file + ": " + ex.getMessage());
                        return null;
                    });
            });
        } catch (IOException e) {
            System.err.println("Error listing files in directory " + directory + ": " + e.getMessage());
        }
    }
    
    private static void processFile(Path file, Producer<String, String> producer) {
        System.out.println("Processing file: " + file);
        
        try (Stream<String> lines = Files.lines(file)) {
            lines.forEach(line -> sendToKafka(line, file.toString(), producer));
        } catch (IOException e) {
            System.err.println("Error reading file " + file + ": " + e.getMessage());
        }
    }
    
    private static void sendToKafka(String line, String fileKey, Producer<String, String> producer) {
        ProducerRecord<String, String> record = new ProducerRecord<>(KAFKA_TOPIC, fileKey, line);
        
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                System.err.println("Error sending message to Kafka: " + exception.getMessage());
            } else {
                // Optional: Uncomment for verbose logging of successful sends
                // System.out.println("Message sent to partition " + metadata.partition() + 
                //                   " at offset " + metadata.offset());
            }
        });
    }
}
