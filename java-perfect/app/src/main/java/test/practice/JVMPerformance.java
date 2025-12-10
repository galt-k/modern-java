package test.practice;

import java.lang.management.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.openjdk.jol.info.ClassLayout;
//import org.openjdk.jol.info.GraphLayout;
import org.openjdk.jol.info.GraphLayout;

public class JVMPerformance {

    //1. Object layout - Using JOL
    static void objectLayout() {
        System.out.println("1. Object layout (JOL) - ");
        ClassLayout layout = ClassLayout.parseInstance(new Object());
        System.out.println(layout.toPrintable());
        
        Object[] array = new Object[1000];
        // Replace the failing line with this
        System.out.println(ClassLayout.parseInstance(array).toPrintable());        // System.out.println("  (JOL shows header, padding, alignment - objects are 16-byte aligned) \n");

        // 2. Array with references — GraphLayout shows full cost
        String[] str_arr = new String[10];
        Arrays.fill(str_arr, "hello");  // each String is ~40 bytes

        System.out.println(GraphLayout.parseInstance(str_arr).toFootprint());

    }

    //2. Escape analysis Demo Allocation on stack vs Heap
    record Point(int x, int y){};

    static Point createPointNoEscape() {
        Point p = new Point(10, 20); // Does not escape-> Stack allocation impossible
        return p; // ESCAPES- Retuned to the caller 
    }

    static void escapeAnalysis(){
        System.out.println("2. Escape Analysis Demo");

        long start = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < 10_000_000; i++) {
            Point p = new Point(i, i*2); // Does this object escape? 
            sum += p.x() + p.y(); // Used Locally only 
        }
        System.out.println("Sum: " + sum + " in " + Duration.ofNanos(System.nanoTime() - start).toMillis() + "ms");
        System.out.println(" Run with -XX:+PrintCompilation -XX:+UnlockDiagnosticVMOptions -XX:+PrintEscapeAnalysis\n");
    }

    // 3. Allocation Rate & GC Pressure
    // Allocation Rate = Total bytes of objects created / time taken
    static void allocationRate() {
        System.out.println("3. High Allocation Rate -> GC Pressure");

        List<byte[]> list = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10_000_000; i++) {
            list.add(new byte[1024]); //1KB objects -> 10GB Allocated
            if (i % 1_000_000 == 0) System.gc(); // force GC to see pauses 
        }
        System.out.println("  Allocated ~10GB in " + (System.currentTimeMillis() - start));
        System.out.println("  Watch GC logs with -Xlog:gc*:file=gc.log\n");
    }

    //4. GC Comparison - Run with different flags
    static void gcDemo() {
        System.out.println("4. GC DEMO- RUN with different GC Flags");

        System.out.println("   java -XX:+UseG1GC           → default");
        System.out.println("   java -XX:+UseZGC             → low pause (<1ms)");
        System.out.println("   java -XX:+UseShenandoahGC    → low pause, concurrent");
        System.out.println("   Add -Xlog:gc* -Xmx4g to see logs\n");

        // Trigger allocations
        List<Object> sink = new ArrayList<>();
        for (int i = 0; i < 1_000_000; i++) {
            sink.add(new byte[1024]);
        }
        System.out.println("   Allocated 1 GB — check GC pauses!\n");
    }

    // 5. Thread local allocation Buffers & Thread Stacks
    
    static void threadLocalAllocation() throws Exception {
        System.out.println("5. Virtual Threads vs Platform Threads memory");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 100_000; i++) {
                executor.submit(() -> {
                    // Each virtual thread has tiny stack (few KB)
                    sleepSeconds(1);
                });
            }
        }
        System.out.println(" 100k virtual threads - low memory. virtual threads scale to millions with low memory, while platform threads are limited to thousands.");
    }

    private static void sleepSeconds(int sec) {
        try { Thread.sleep(Duration.ofSeconds(sec)); } catch (Exception ignored) {}
    }

    //6. Run metrics (jcmd style)
    static void runtimeMetrics() {
        System.out.println("6. Runtime metrics");

        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory() / 1024 / 1024;
        long total = runtime.totalMemory() / 1024 / 1024;
        long free = runtime.freeMemory() / 1024 / 1024;

        System.out.println(" Max Memory: " + max + " MB");
        System.out.println(" Total: " + total + " MB, Free: "+ free +" MB");

        System.out.println(" Run jcmd <pid> GC.heap_info for full details \n");


    }

    public static void main(String[] args) throws Exception {
        System.out.println("JVM PERFORMANCE & MEMORY MASTERY 2025\n" + "=".repeat(60));

        //objectLayout();
        //escapeAnalysis();
        //allocationRate();
        //gcDemo();
        threadLocalAllocation();
        //runtimeMetrics();
        System.out.println("Press Enter to exit...");
    }
}
