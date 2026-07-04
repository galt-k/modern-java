package org.example;

import javax.swing.text.Segment;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import static java.nio.file.Files.*;



public class Main {

    private static final String FILE_PATH = "cities.txt";
    public record Segment(long start, int size){};

    public static void main(String[] args) throws IOException {
        Map<Integer, Integer> frequencyMap = new HashMap<>();
        var path = Paths.get(FILE_PATH);

        System.out.println("Executing High-Speed Memory Map via Java 21...");

        // 1. Capture start time
        long startTime = System.nanoTime();

        // 2. Run the method
        //basic_v1(String.valueOf(path)); //510
        //basic_v2_Channel(path);//307
        //streams_v3(path); // 237ms
        //WordTrackerStrain.futures_v5(path);
        //WordTrackerStrain.futures_v4(path);
        WordTrackerStrain.virtualThreads_v6(path);



        // 3. Capture end time
        long endTime = System.nanoTime();

        // 4. Calculate total elapsed time
        long durationNano = endTime - startTime;
        double durationMillis = durationNano / 1_000_000.0; // Convert to milliseconds

        System.out.printf("Execution time: %,d ns (%.3f ms)%n", durationNano, durationMillis);



//        // 1. Open a Confined Arena to manage off-heap lifecycle
//        try (Arena arena = Arena.ofConfined();
//             FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
//
//            long fileSize = channel.size();
//
//            // 2. Map the file directly to an off-heap native memory segment.
//            // Fixes error line [44,43]: Using MemorySegment directly instead of ByteBuffer.
//            MemorySegment fileSegment = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize, arena);
//
//            int currentWordHash = 0;
//            boolean hasCharacters = false;
//
//            // 3. Scan straight through the mapped file space
//            for (long offset = 0; offset < fileSize; offset++) {
//                byte b = fileSegment.get(ValueLayout.JAVA_BYTE, offset);
//
//                if (b == ',' || b == '\n' || b == '\r') {
//                    if (hasCharacters) {
//                        // Fixes error line [59,75]: Providing both the key and default value (0) to getOrDefault
//                        frequencyMap.put(currentWordHash, frequencyMap.getOrDefault(currentWordHash, 0) + 1);
//                        currentWordHash = 0;
//                        hasCharacters = false;
//                    }
//                } else {
//                    currentWordHash = 31 * currentWordHash + b;
//                    hasCharacters = true;
//                }
//            }
//
//            // Flush the very last word if file doesn't end with a delimiter
//            if (hasCharacters) {
//                frequencyMap.put(currentWordHash, frequencyMap.getOrDefault(currentWordHash, 0) + 1);
//            }
//        } // The mapped file memory space is fully unmapped here safely!
//
//        System.out.println("Processing Complete!");
//        System.out.println("Total Unique City Hashes Tracked: " + frequencyMap.size());
    }


    private static int basic_v1(String path){
        //find the size of the file in bytes.
        try(RandomAccessFile file = new RandomAccessFile(path,"r");){
            //find the size of the file in bytes
            int bytes = (int) file.length();
            System.out.println("Size of the file in bytes = " + bytes);
            //how many chunk of bytes of size 100 can be read from the above?
            int chunkSize = 100;
            int numOfChunks = Math.ceilDiv( bytes , chunkSize);
            System.out.println("Number of chunks = " + numOfChunks);
            // iterate through the chunks
            // Get the pointer and load all the bytes into an array
            int offset = 0;
            Map<String, Integer> cityMap = new HashMap<>();
            boolean reset = true;
            StringBuilder mutableString =new StringBuilder();
            for(int chunk =0; chunk< numOfChunks; chunk++){
                //ofsset is the position of the pointer in the file byte seqyence.
                //empty buffer into whcih the dat is read.
                byte[] byteBuffer = new byte[chunkSize];
                int chunkBytes = file.read(byteBuffer, 0, chunkSize);
                //Now the byteBufer will have the actual bytes.
                //Iterate the byteBUffer and extract the cities

                for(int i = 0; i < chunkBytes; i++) {
                    byte b = byteBuffer[i];
                    //if its a character add it to the string builder
                    //if its not a char like ',',';' or " " means you found a city
                    if(reset){
                        mutableString.setLength(0);
                    }
                    if(b == ',' || b == ';' || b == '\n' || b == '\r'){
                        //System.out.println("found a split character");
                        //add the city to the map.
                        if(mutableString.length() != 0) {
                            cityMap.put(mutableString.toString(), cityMap.getOrDefault(mutableString.toString(), 0) + 1);
                            //make the string builder empty.
                        }
                        //mutableString.setLength(0);
                        reset = true;
                    }else{
                        reset =false;
                        //Add it to the mutableString
                        mutableString.append((char)b);

                    }

                }
                //increase the offsert
                offset += chunkSize;
            }

            // Final safe check for trailing words without closing delimiters
            String finalCity = mutableString.toString().trim();
            if(!reset && finalCity.length() != 0){
                cityMap.put(finalCity, cityMap.getOrDefault(finalCity, 0) + 1);
            }

            //get the size of the Hashmap
            System.out.println("Number of cities =  " + cityMap.size());
            System.out.println("Cities -> " + cityMap);


        }catch (IOException e){
            System.out.println(e.getMessage());
        }

        return 0;
    }

    private static void basic_v2_Channel(Path path) {
        System.out.println("Executing via High-Speed Channel Memory Map...");

        // 1. Open the channel ONCE outside the loop
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            long fileSize = channel.size();
            System.out.println("Size of the file in bytes = " + fileSize);

            // 2. Map the ENTIRE file into memory at once. No more chunks!
            ByteBuffer bf = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);

            Map<String, Integer> cityMap = new HashMap<>();
            StringBuilder mutableString = new StringBuilder();

            // 3. Scan straight through the buffer using hasRemaining()
            while (bf.hasRemaining()) {
                byte b = bf.get(); // Automatically reads a byte and advances the pointer

                if (b == ',' || b == ';' || b == '\n' || b == '\r') {
                    String city = mutableString.toString().trim();
                    if (city.length() != 0) {
                        cityMap.put(city, cityMap.getOrDefault(city, 0) + 1);
                    }
                    mutableString.setLength(0); // Instantly clear the builder
                } else {
                    mutableString.append((char) b);
                }
            }

            // Safe final flush for trailing words
            String finalCity = mutableString.toString().trim();
            if (finalCity.length() != 0) {
                cityMap.put(finalCity, cityMap.getOrDefault(finalCity, 0) + 1);
            }

            System.out.println("Number of cities =  " + cityMap.size());
            System.out.println("Cities -> " + cityMap.keySet());

        } catch (IOException e) {
            System.out.println("Error processing file: " + e.getMessage());
        }
    }


    // Streams
    private static void streams_v3(Path path){
        //Get the segment partitions
        List<Segment> segments =  getSegments(path);
        System.out.println("Number of segments = " + segments.size());
        // Stream the segments
        Map<String, Integer> cityMap = segments.stream().parallel().collect(
                () -> new HashMap<String, Integer>(),
                //accumulator
                (map, segment)-> {
                    //use channel to map the memory using the segment
                    try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
                        //map the segments cordinates to memory
                        ByteBuffer bf = channel.map(FileChannel.MapMode.READ_ONLY, segment.start, segment.size);
                        //
                        StringBuilder mutableString = new StringBuilder();

                        // 3. Scan straight through the buffer using hasRemaining()
                        while (bf.hasRemaining()) {
                            byte b = bf.get(); // Automatically reads a byte and advances the pointer

                            if (b == ',' || b == ';' || b == '\n' || b == '\r') {
                                String city = mutableString.toString().trim();
                                if (city.length() != 0) {
                                    map.put(city, map.getOrDefault(city, 0) + 1);
                                }
                                mutableString.setLength(0); // Instantly clear the builder
                            } else {
                                mutableString.append((char) b);
                            }
                        }
                        // Final flush for the last city in the segment
                        String finalCity = mutableString.toString().trim();
                        if (!finalCity.isEmpty()) {
                            map.put(finalCity, map.getOrDefault(finalCity, 0) + 1);
                        }


                    }catch (IOException e){

                    }

                },
                //combiner
                //if key exisits in map2 then add both and put it in map1
                (mainMap, localMap) -> {
                    localMap.forEach((city, count) ->
                            mainMap.merge(city, count, Integer::sum)
                    );
                }
        );

        System.out.println("Number of cities =  " + cityMap.size());
        System.out.println("Cities -> " + cityMap.keySet());

    }

    public static List<Segment> getSegments(Path path){
        try(RandomAccessFile file = new RandomAccessFile(String.valueOf(path), "r")){
            long totalFileSize = file.length();
            System.out.println("Size of the file in bytes = " + totalFileSize);
            int cores = Runtime.getRuntime().availableProcessors();
            int segmentSize = ((int) (totalFileSize / cores));
            //int segmentSize = (int) totalFileSize / 100;
            List<Segment> segments = new ArrayList<>();
            long filePos = 0;
            while(filePos < totalFileSize - segmentSize){
                //add the start position and size to the segment
                file.seek(filePos + segmentSize);
                int b;
                while ((b = file.read()) != -1 && b != '\n') {
                    // Do nothing, just safely advancing the pointer
                }
                //add the segment to the list
                segments.add(new Segment(filePos, (int) (file.getFilePointer()-filePos)));
                filePos = file.getFilePointer();
            }

            segments.add(new Segment(filePos, (int) (totalFileSize - filePos)));
            return segments;


        }catch (IOException e){
            throw new RuntimeException(e);
        }

    }
}