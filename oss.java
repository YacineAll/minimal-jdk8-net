import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TestFileGenerator {
    private static final int DEFAULT_FILE_SIZE_KB = 100;
    private static final int DEFAULT_FILE_COUNT = 3000;
    private static final Random random = new Random();
    
    public static void main(String[] args) {
        String testDirPath = "hadoop_test_files";
        int fileCount = DEFAULT_FILE_COUNT;
        int fileSizeKb = DEFAULT_FILE_SIZE_KB;
        
        // Override defaults if arguments provided
        if (args.length >= 1) testDirPath = args[0];
        if (args.length >= 2) fileCount = Integer.parseInt(args[1]);
        if (args.length >= 3) fileSizeKb = Integer.parseInt(args[2]);
        
        generateTestFiles(testDirPath, fileCount, fileSizeKb);
    }
    
    public static void generateTestFiles(String baseDirPath, int fileCount, int fileSizeKb) {
        File baseDir = new File(baseDirPath);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            System.err.println("Failed to create directory: " + baseDirPath);
            return;
        }
        
        // Create subdirectories to distribute files
        int subDirCount = Math.min(10, fileCount / 100);
        subDirCount = Math.max(1, subDirCount); // At least 1 directory
        
        System.out.println("Generating " + fileCount + " files of " + fileSizeKb + 
                           "KB each across " + subDirCount + " directories");
        
        // Use thread pool to speed up file creation
        ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        );
        
        AtomicInteger filesCreated = new AtomicInteger(0);
        
        for (int i = 0; i < fileCount; i++) {
            final int fileIndex = i;
            final int dirIndex = i % subDirCount;
            
            executor.submit(() -> {
                try {
                    // Create directory if needed
                    File dir = new File(baseDir, "dir_" + dirIndex);
                    if (!dir.exists() && !dir.mkdirs()) {
                        System.err.println("Failed to create directory: " + dir.getPath());
                        return;
                    }
                    
                    // Create and write to file
                    File file = new File(dir, "test_file_" + fileIndex + ".dat");
                    writeRandomContent(file, fileSizeKb * 1024);
                    
                    int count = filesCreated.incrementAndGet();
                    if (count % 100 == 0) {
                        System.out.println("Created " + count + " files...");
                    }
                } catch (IOException e) {
                    System.err.println("Error creating file: " + e.getMessage());
                }
            });
        }
        
        // Shutdown executor and wait for completion
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                System.err.println("Timeout occurred before all files were created");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("File generation interrupted");
        }
        
        System.out.println("Successfully generated " + filesCreated.get() + " files");
    }
    
    private static void writeRandomContent(File file, int sizeInBytes) throws IOException {
        byte[] buffer = new byte[8192]; // 8KB buffer for writing
        
        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            
            int bytesRemaining = sizeInBytes;
            
            while (bytesRemaining > 0) {
                int bytesToWrite = Math.min(buffer.length, bytesRemaining);
                
                // Generate random bytes
                random.nextBytes(buffer);
                
                // Write bytes to file
                bos.write(buffer, 0, bytesToWrite);
                bytesRemaining -= bytesToWrite;
            }
            
            bos.flush();
        }
    }
}
