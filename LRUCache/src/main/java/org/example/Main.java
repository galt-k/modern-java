package org.example;
//package org.example.ApproxLRU1;


import java.util.concurrent.ConcurrentLinkedQueue;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws Exception {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
//        try (ApproxLRU1 cache = new ApproxLRU1(10, 5)) {
//
//            for (int i = 0; i < 10; i++) {
//                cache.put(i, i * 100);
//            }
//
//            System.out.println("get(3) = " + cache.get(3));
//            System.out.println("Size: " + cache.size());
//
//            // The background Buffer thread processes access events
//            // asynchronously — give it a brief moment to drain before
//            // relying on updated timestamps for anything time-sensitive.
//            Thread.sleep(50);
//
//            cache.put(10, 1000); // forces an eviction since capacity is 10
//            System.out.println("Size after exceeding capacity: " + cache.size());
//
//        } // buffer.stop() called here automatically
        // 1. Initialize a queue with a capacity of 3
        ConcurrentBlockingQueue queue = new ConcurrentBlockingQueue(3);
        System.out.println("--- Starting Queue Tests ---");

        // 2. Test initial empty state
        System.out.println("Get from empty queue (Expects -1): " + queue.get());

        // 3. Test putting elements up to capacity
        System.out.println("\nPutting values: 10, 20, 30");
        queue.put(10);
        queue.put(20);
        queue.put(30);

        // 4. Test capacity limit (this should return early based on your logic)
        System.out.println("Attempting to put 40 into a full queue...");
        queue.put(40);

        // 5. Test removing elements sequentially
        System.out.println("\nRetrieving elements:");
        System.out.println("Got: " + queue.get() + " (Expects 10)");
        System.out.println("Got: " + queue.get() + " (Expects 20)");
        System.out.println("Got: " + queue.get() + " (Expects 30)");

        // 6. Test empty state recovery
        System.out.println("Got from now-empty queue (Expects -1): " + queue.get());

        // 7. Test re-adding after draining
        System.out.println("\nPutting a new value after drain: 50");
        queue.put(50);
        System.out.println("Got: " + queue.get() + " (Expects 50)");


        System.out.println("Done — background thread stopped, JVM can exit cleanly.");



    }
}