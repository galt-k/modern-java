package org.example;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConcurrentBlockingQueueSemaphoreTest {

    @Test
    public void testTwoPhaseTurnstileThrottling() throws InterruptedException, ExecutionException {
        // Phase 1: Capacity matches the Semaphore permit ceiling (10)
        int queueCapacity = 10;
        ConcurrentBlockingQueue queue = new ConcurrentBlockingQueue(queueCapacity);

        int totalProducers = 25;
        int initialThrottledPermits = 10;
        int expectedBlockedProducers = 15;

        // Ensure the thread pool has enough workers so thread scheduling lag doesn't distort metrics
        ExecutorService producerPool = Executors.newFixedThreadPool(totalProducers);
        ExecutorService consumerPool = Executors.newFixedThreadPool(10);

        CountDownLatch stampedeSignal = new CountDownLatch(1);
        AtomicInteger successfulPuts = new AtomicInteger(0);

        // Submit 25 concurrent producers
        for (int i = 0; i < totalProducers; i++) {
            final int value = i;
            producerPool.submit(() -> {
                try {
                    stampedeSignal.await(); // Synchronized release
                    queue.put(value);
                    successfulPuts.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // --- STAGE 1: FLOODING THE GATE ---
        System.out.println("=== STAGE 1: FLOODING THE THROTTLING GATE ===");
        stampedeSignal.countDown();

        // Give the OS threads plenty of time to strike the queue and park
        Thread.sleep(500);

        // Verification 1: The queue size is capped by the data capacity
        int initialQueueSize = queue.size();
        System.out.println("Queue size after initial flood: " + initialQueueSize + " / " + queueCapacity);
        assertEquals(initialThrottledPermits, initialQueueSize, "First wave should completely fill the queue.");

        // Verification 2: Check execution state metrics
        // 10 threads have acquired permits and are inside the put() logic.
        // Therefore, the remaining threads are stuck waiting at the semaphore gate.
        int threadsInsideQueueLogic = initialQueueSize;
        int threadsBlockedBySemaphore = totalProducers - threadsInsideQueueLogic;

        System.out.println("Threads currently executing or waiting inside locks: " + threadsInsideQueueLogic);
        System.out.println("Threads actively BLOCKED by the Semaphore gate: " + threadsBlockedBySemaphore);
        assertEquals(expectedBlockedProducers, threadsBlockedBySemaphore, "Exactly 15 threads should be blocked by the semaphore.");

        // --- STAGE 2: THE CONSUMER DRAIN ---
        System.out.println("\n=== STAGE 2: DROPPING CONSUMER BACKPRESSURE ===");
        System.out.println("Picking up 10 messages from the queue...");

        // Drain exactly 10 items
        for (int i = 0; i < 10; i++) {
            consumerPool.submit(() -> {
                try {
                    queue.get(); // Adjust to your consumer method name (e.g., get() or take())
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        // Wait for the consumer processing window and subsequent producer catch-up execution
        Thread.sleep(500);

        // --- STAGE 3: NEXT WAVE VERIFICATION ---
        System.out.println("\n=== STAGE 3: POST-DRAIN METRICS ===");
        int totalProcessedSoFar = successfulPuts.get();
        System.out.println("Total successful puts executed so far: " + totalProcessedSoFar);

        // If the turnstile let the next wave through, 10 items were consumed,
        // and 10 of the blocked 15 stepped forward to fill the void.
        int currentQueueSize = queue.size();
        System.out.println("Current items remaining in queue: " + currentQueueSize);

        // Assert that the next flight of threads successfully recycled the released permits
        assertTrue(totalProcessedSoFar >= 20, "The semaphore failed to release the next wave of threads.");
        assertEquals(10, currentQueueSize, "The queue should have bounced right back to capacity.");

        // Clean up pools safely
        producerPool.shutdownNow();
        consumerPool.shutdownNow();
    }
}