package org.example;
//package org.example.ApproxLRU1;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
        ConcurrentBlockingQueue queue = new ConcurrentBlockingQueue(5);

        int numConsumers = 3;
        ExecutorService executor = Executors.newFixedThreadPool(numConsumers + 1);

        // Coordinates letting consumers start at the exact same time
        CountDownLatch startLatch = new CountDownLatch(1);
        // Tracks when all consumers have successfully finished their execution
        CountDownLatch executionLatch = new CountDownLatch(numConsumers);

        AtomicInteger successfulConsumptions = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        // 1. Start 3 Consumer Threads on an EMPTY queue
        for (int i = 0; i < numConsumers; i++) {
            final int consumerId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for the green light
                    System.out.println("[Consumer " + consumerId + "] Attempting to get...");

                    // This call WILL block because the queue is empty
                    Integer value = queue.get();

                    System.out.println("[Consumer " + consumerId + "] Successfully grabbed: " + value);
                    successfulConsumptions.incrementAndGet();
                } catch (InterruptedException e) {
                    System.out.println("[Consumer " + consumerId + "] Was interrupted!");
                } catch (NullPointerException npe) {
                    System.out.println("[FAIL] Consumer " + consumerId + " hit a NullPointerException!");
                    exceptionCount.incrementAndGet();
                } finally {
                    executionLatch.countDown();
                }
            });
        }

        // Give threads a moment to spin up and enter the lock/condition queues
        Thread.sleep(500);
        System.out.println("[Main] Giving the green light to consumers...");
        startLatch.countDown(); // Consumers now hit the empty queue and go to the waiting room

        // Verify they are actually blocking and not rushing through
        Thread.sleep(1000);
        System.out.println("[Main] Current successful consumptions (should be 0): " + successfulConsumptions.get());

        // 2. Producer offers TWO items (Remember: we have THREE consumers waiting)
        System.out.println("[Main] Producer adding 3 items to the queue...");
        queue.put(100);
        queue.put(200);
        queue.put(300); // Add a third item!

        // Wait for threads to finish processing
        boolean finishedCleanly = executionLatch.await(3, TimeUnit.SECONDS);

        // 3. Evaluation
        System.out.println("\n--- TEST RESULTS ---");
        System.out.println("Successful Consumptions: " + successfulConsumptions.get() + " / 3 expected");
        System.out.println("NullPointerExceptions caught: " + exceptionCount.get());

        if (exceptionCount.get() == 0 && successfulConsumptions.get() == 3) {
            System.out.println("SUCCESS: The while loop protected the empty states perfectly!");
        } else {
            System.out.println("FAILURE: State synchronization failed.");
        }

        // Clean up executor pool
        executor.shutdownNow();


    }
}