package org.example;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WordTrackerStrain {
    // A thread safe metrics map to aggregate the eventual reulst sof the future
    private static final Map<String, UnifiedCityMetric> globalMetricAvg = new ConcurrentHashMap<>();
    private static final Map<String, long[]> globalMetricReport = new ConcurrentHashMap<>();
    private static ExecutorService executor = Executors.newFixedThreadPool(1);
    //private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();


    //private static final String FILE_PATH = "cities2.txt";

    // Our unified data structure holding both values from the chunk
    public static class UnifiedCityMetric {
        private int count = 0;
        private double totalMetricSum = 0.0;

        public synchronized void addCount(int amount) {
            this.count += amount;
        }

        public synchronized void addMetricSum(double sumValue) {
            this.totalMetricSum += sumValue;
        }

        public synchronized int getCount() {
            return count;
        }

        public synchronized double getAverage() {
            return count == 0 ? 0.0 : totalMetricSum / count;
        }
    }

    public static void futures_v5(Path path){
        // Generate the globalMap with the count and SumOfMetric as 0.
        List<Main.Segment> segments =  getSegments(path);
        System.out.println("Number of segments = " + segments.size());
        // Now Create athe completable futures with a batch of 10 and update the Global Map with the SumValue.
        segments.stream().parallel().forEach( segment -> {
            try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
                //map the segments cordinates to memory
                ByteBuffer bf = channel.map(FileChannel.MapMode.READ_ONLY, segment.start(), segment.size());
                StringBuilder mutableString = new StringBuilder();

                while (bf.hasRemaining()) {
                    byte b = bf.get(); // Automatically reads a byte and advances the pointer

                    if (b == ',' || b == ';' || b == '\n' || b == '\r') {
                        String city = mutableString.toString().trim();
                        if (!city.isEmpty()) {
                            // add it to the globalMap
                            //Compute array atomically if mising. Primitive defaults to 0
                            long[] metrics = globalMetricReport.computeIfAbsent(city, k-> new long[2]);

                            // A ConcurrencyHashMap does not lock array content natively
                            // we have to synchonize on the array refernce to safely increment cross-thread
                            synchronized (metrics) {
                                metrics[0] += 1;
                            }
                        }
                        mutableString.setLength(0); // Instantly clear the builder
                    } else {
                        mutableString.append((char) b);
                    }
                }
                //get the remaining the chunk
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        });

        System.out.println("Total number of cities = " + globalMetricReport.size());

        // Extract unique Keys to prepare for network batching
        List<String> uniqueCities =new ArrayList<>(globalMetricReport.keySet());

        // Here you have to create Stream which create the futures by splitting into 10
        // Construct a stream that divides unique cities into clean chunks of 10

        List<CompletableFuture<Void>> futures = IntStream
                .iterate(0, i -> i < uniqueCities.size(), i->i+10)
                .mapToObj(i -> uniqueCities.subList(i, Math.min(i+10, uniqueCities.size())))
                .map(chunk -> {
                            //call the getMetric Future then accept the calculation future.
                            return getMetric(chunk).thenAcceptAsync(responseMap -> {
                                //for each of the resposne update the value in globalMap
                                responseMap.forEach((city, randomMetric) -> {
                                    long[] metrics = globalMetricReport.get(city);
                                    if (metrics != null) {
                                        synchronized (metrics) {
                                            metrics[1] += (metrics[0] * randomMetric);
                                        }
                                    }
                                });

                            }, executor);
                        }).collect(Collectors.toList());

        //wait for all the furues are completed.
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        // 2. Add this line immediately to allow background worker threads to exit cleanly
        executor.shutdown();

        System.out.println("\n=== FINAL UNIFIED METRICS REPORT ===");
        globalMetricReport.forEach((city, metrics) -> {
            double avg = metrics[0] == 0 ? 0.0 : (double) metrics[1] / metrics[0];
            System.out.printf("City: %-15s | Total Count: %-6d | Weighted Avg: %.2f%n",
                    city, metrics[0], avg);
        });

        // Add this right before the final closing brace of futures_v5
        System.out.println("Process complete. Press ENTER to exit and release threads...");
        new java.util.Scanner(System.in).nextLine();
    }


    public static void futures_v4(Path path){
        //List<Main.Segment> segments = Main.getSegments(path);
        //segments.stream()
        //Test Calling the completable future
//        List<String> cities = new ArrayList<>(Arrays.asList("Chicago", "Newyork"));
//        System.out.println("Cities -> " + cities);
//        CompletableFuture<Map<String, Integer>> resultMapFuture = getMetric(cities);
//        //Wait until future is completed
//        resultMapFuture.thenAccept(result -> {
//            System.out.println("Futures -> " + result);
//        }).join();
        //Main thread should cimpklete the stream procesing
        // at the end it should wait for all the futures to complete.
        //Get the segment partitions
        List<Main.Segment> segments =  getSegments(path);
        System.out.println("Number of segments = " + segments.size());

        // Track all the background futures across parallel threads safely
        List<CompletableFuture<Void>> allNetworkFutures = new ArrayList<>();

        segments.stream().parallel().forEach( segment -> {
                    try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
                        //map the segments cordinates to memory
                        ByteBuffer bf = channel.map(FileChannel.MapMode.READ_ONLY, segment.start(), segment.size());
                        StringBuilder mutableString = new StringBuilder();
                        Set<String> cityKeys = new HashSet<>();
                        //Track how many times each city appeared with in the specific 10-unique city batch
                        Map<String, Integer> batchCounts = new HashMap<>();

                        while (bf.hasRemaining()) {
                            byte b = bf.get(); // Automatically reads a byte and advances the pointer

                            if (b == ',' || b == ';' || b == '\n' || b == '\r') {
                                String city = mutableString.toString().trim();
                                if (!city.isEmpty()) {
                                    // Permanently recrod the raw global parse count
                                    globalMetricAvg.computeIfAbsent(city, k -> new UnifiedCityMetric()).addCount(1);

                                    // 2. Track occurrences inside our current interval batch
                                    cityKeys.add(city);
                                    batchCounts.put(city, batchCounts.getOrDefault(city, 0) + 1);

                                    if(cityKeys.size()==10){
                                        //send the grpc call
                                        List<String> citiesToQuery = new ArrayList<>(cityKeys);
                                        Map<String, Integer> currentBatchCounts = new HashMap<>(batchCounts);


                                        //Clear the trackers instantly so the next batch can start collecting.
                                        cityKeys.clear();
                                        batchCounts.clear();

                                        //Fire the network request asynchronously
                                        CompletableFuture<Void> pipelineFuture = getMetric(citiesToQuery)
                                                .thenAcceptAsync(grpcResponseMap -> {
                                                    //Callback executes acynhronouslywhne the 15ms wait ends.
                                                    grpcResponseMap.forEach((cityName, randomMetric) -> {
                                                        int occurencesInBatch = currentBatchCounts.getOrDefault(cityName, 0);
                                                        double totalbatchWeight = occurencesInBatch * randomMetric;

                                                        globalMetricAvg.computeIfAbsent(cityName, key -> new UnifiedCityMetric())
                                                                .addMetricSum(totalbatchWeight);
                                                    });
                                                }, executor);
                                        allNetworkFutures.add(pipelineFuture);
                                    }
                                }
                                mutableString.setLength(0); // Instantly clear the builder
                            } else {
                                mutableString.append((char) b);
                            }
                        }
                        // Final leftover flush for this segment if the file ended before hitting a perfect multiple of 10
                        if (!cityKeys.isEmpty()) {
                            List<String> finalCities = new ArrayList<>(cityKeys);
                            Map<String, Integer> finalBatchCounts = new HashMap<>(batchCounts);

                            CompletableFuture<Void> finalFlushFuture = getMetric(finalCities)
                                    .thenAccept(gRpcResponseMap -> {
                                        gRpcResponseMap.forEach((cityName, randomMetricValue) -> {
                                            int occurrencesInBatch = finalBatchCounts.getOrDefault(cityName, 0);
                                            double totalBatchWeight = (double) occurrencesInBatch * randomMetricValue;

                                            globalMetricAvg.computeIfAbsent(cityName, k -> new UnifiedCityMetric())
                                                    .addMetricSum(totalBatchWeight);
                                        });
                                    });
                            allNetworkFutures.add(finalFlushFuture);
                        }
                    } catch (IOException e) {
                        System.err.println("Error reading file: " + path);
                        throw new RuntimeException(e);
                    }
        });

        System.out.println("Local stream scanning complete. Waiting for background gRPC tasks to join...");

        CompletableFuture.allOf(allNetworkFutures.toArray(new CompletableFuture[0])).join();
        // Print final reports calculating avg = sum / count
        System.out.println("\n=== UNIFIED METRICS REVENUE REPORT ===");
        globalMetricAvg.forEach((city, metrics) -> {
            System.out.printf("City: %-15s | Total Count: %-6d | Weighted Avg: %.2f%n",
                    city,
                    metrics.getCount(),
                    metrics.getAverage()
            );
        });

        // Add this right before the final closing brace of futures_v5
        System.out.println("Process complete. Press ENTER to exit and release threads...");
        new java.util.Scanner(System.in).nextLine();
    }

    private static CompletableFuture<Map<String,Integer>> getMetric(List<String> cities){
        //pass those citites as the payload and return a map
        //Map <String,Integer> map = new HashMap<>();
        //Make the gRPC call here a mock call with sleep time of 15ms.
        // gRPC should return the map with the cities passed as keys and random numbers
        // as values.

        CompletableFuture<Map<String,Integer>> future = new CompletableFuture<>().supplyAsync(
                ()-> {
                    // sleep time of 15sec
                    try {
                        Thread.sleep(15);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    Map<String, Integer> unknownMap = new HashMap<>();
                    for(String city: cities){
                        unknownMap.put(city, (int) (Math.random() * 100) + 1);
                    }
                    return unknownMap;
                }
        );
        return future;

    }

    public static List<Main.Segment> getSegments(Path path){
        try(RandomAccessFile file = new RandomAccessFile(String.valueOf(path), "r")){
            long totalFileSize = file.length();
            System.out.println("Size of the file in bytes = " + totalFileSize);
            int cores = Runtime.getRuntime().availableProcessors();
            int segmentSize = ((int) (totalFileSize / cores));
            //int segmentSize = (int) totalFileSize / 100;
            List<Main.Segment> segments = new ArrayList<>();
            long filePos = 0;
            while(filePos < totalFileSize - segmentSize){
                //add the start position and size to the segment
                file.seek(filePos + segmentSize);
                int b;
                while ((b = file.read()) != -1 && b != '\n') {
                    // Do nothing, just safely advancing the pointer
                }
                //add the segment to the list
                segments.add(new Main.Segment(filePos, (int) (file.getFilePointer()-filePos)));
                filePos = file.getFilePointer();
            }

            segments.add(new Main.Segment(filePos, (int) (totalFileSize - filePos)));
            return segments;


        }catch (IOException e){
            throw new RuntimeException(e);
        }

    }

    public static void virtualThreads_v6(Path path){
        List<Main.Segment> segments =  getSegments(path);
        System.out.println("Number of segments = " + segments.size());

        segments.stream().parallel().forEach(segment -> {
            try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
                // map the segments coordinates to memory
                ByteBuffer bf = channel.map(FileChannel.MapMode.READ_ONLY, segment.start(), segment.size());
                StringBuilder mutableString = new StringBuilder();

                while (bf.hasRemaining()) {
                    byte b = bf.get(); // Automatically reads a byte and advances the pointer

                    if (b == ',' || b == ';' || b == '\n' || b == '\r') {
                        String city = mutableString.toString().trim();
                        if (!city.isEmpty()) {
                            // add it to the globalMap
                            // Compute array atomically if missing. Primitive defaults to 0
                            long[] metrics = globalMetricReport.computeIfAbsent(city, k -> new long[2]);

                            // A ConcurrentHashMap does not lock array content natively
                            // we have to synchronize on the array reference to safely increment cross-thread
                            synchronized (metrics) {
                                metrics[0] += 1;
                            }
                        }
                        mutableString.setLength(0); // Instantly clear the builder
                    } else {
                        mutableString.append((char) b);
                    }
                }
                // get the remaining the chunk
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        });

        System.out.println("Total number of cities = " + globalMetricReport.size());

        // Extract unique Keys to prepare for network batching
        List<String> uniqueCities = new ArrayList<>(globalMetricReport.keySet());
        //======================================================================
        //By using AutoCloseable VirtualThreadPerTaskExectuor, the closing brace
        //of this block acts as an automatic synchonization barrier,
        //completely repalcing the need for CompletableFuture.allOF(..).join()

        try(var virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()){
            IntStream.iterate(0, i -> i < uniqueCities.size(), i -> i + 10)
                    .mapToObj(i -> uniqueCities.subList(i, Math.min( i+10 , uniqueCities.size())))
                    .forEach(chunk -> {
                        //================================================================
                        // Virtual thread change STEP 3: Submit a simple, blocking runnable task.
                        // Each iteration spawns a brand new, disposable virtua thread.
                        // ================================================================
                        virtualExecutor.submit(() -> {
                          //================================
                          //Flatten out the asyhnchronous callback logic directly
                          // we call the blocking network code directly. JVM will transaparently
                          //unmount this virtual thread during its 15ms sleep without freezing the OS.
                          Map<String, Integer> responseMap = getMetricSynchronous(chunk);

                          // next line will run sequentially right after the blocking call unblocks
                            responseMap.forEach((city, randomMetric) -> {
                                long[] metrics = globalMetricReport.get(city);
                                if(metrics!= null){
                                    synchronized (metrics) {
                                        metrics[1] += (metrics[0] * randomMetric);
                                    }
                                }
                            });
                        });
                    });
        }

        //========================================================
        //Virtual thread change step 6: Removed manula executor.shutdowm and .join

        System.out.println("\n=== FINAL UNIFIED METRICS REPORT ===");
        globalMetricReport.forEach((city, metrics) -> {
            double avg = metrics[0] == 0 ? 0.0 : (double) metrics[1] / metrics[0];
            System.out.printf("City: %-15s | Total Count: %-6d | Weighted Avg: %.2f%n",
                    city, metrics[0], avg);
        });

        // Add this right before the final closing brace of futures_v5
        System.out.println("Process complete. Press ENTER to exit and release threads...");
        new java.util.Scanner(System.in).nextLine();



    }

    // =========================================================================
// VIRTUAL THREAD CHANGE STEP 7: Accompanying synchronous blocking helper
// =========================================================================
    private static Map<String, Integer> getMetricSynchronous(List<String> cities) {
        try {
            // This blocks the Virtual Thread, safely yielding the underlying platform thread
            Thread.sleep(90000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        Map<String, Integer> mockResponse = new HashMap<>();
        Random random = new Random();
        for (String city : cities) {
            mockResponse.put(city, random.nextInt(100) + 1);
        }
        return mockResponse;
    }
}
