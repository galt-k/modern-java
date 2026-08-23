package org.example;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class RatelimitBucketTest {
    private static final int CAPACITY = 10;
    private static final double REFILL_RATE_PER_SEC = 10.0; // 10 tokens / sec (1 token every 100ms)
    private RatelimitBucket bucket;

    @BeforeEach
    void setUp() {
        bucket = new RatelimitBucket(CAPACITY, REFILL_RATE_PER_SEC);
    }

    @Test
    @DisplayName("Should allow requests up to capacity, then instantly return false without blocking")
    void testBasicCapacityAndNonBlockingBehavior() {
        // Drain the initial capacity of 10 tokens
        for (int i = 0; i < CAPACITY; i++) {
            assertTrue(bucket.tryGetToken(), "Token acquire should succeed within capacity limit");
        }

        // Measure execution time for the 11th call to verify it does NOT block
        long startTime = System.nanoTime();
        boolean acquired = bucket.tryGetToken();
        long durationNs = System.nanoTime() - startTime;

        assertFalse(acquired, "Should return false immediately when bucket is empty");
        // Ensure execution took under 1 millisecond (confirming no park/sleep happened)
        assertTrue(durationNs < 1_000_000L, "Call took longer than 1ms, indicating blocking behavior!");
    }

    @Test
    @DisplayName("High Concurrency: Ensures exact token counting under heavy multi-threaded CAS contention")
    void testHighConcurrencyNoDoubleSpend() throws InterruptedException, ExecutionException {
        int threadCount = 32;
        int requestsPerThread = 1_000;

        // Bucket configured with capacity 50 and 0 refill rate (so total available tokens is strictly 50)
        RatelimitBucket rigidBucket = new RatelimitBucket(50, 0.0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);

        AtomicInteger totalTokensAcquired = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGate.await(); // Synchronize all threads to bombard the CAS simultaneously
                    for (int j = 0; j < requestsPerThread; j++) {
                        if (rigidBucket.tryGetToken()) {
                            totalTokensAcquired.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        // Release all 32 threads at the exact same millisecond
        startGate.countDown();

        // Wait for all threads to complete
        boolean finished = doneGate.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Threads timed out, possible deadlock or livelock!");
        assertEquals(50, totalTokensAcquired.get(),
                "Exact token count mismatch! Race condition caused double-spending or token loss.");
    }

    @Test
    @DisplayName("Cap Test: Tokens should never exceed maximum capacity even after long idle times")
    void testCapacityCap() throws InterruptedException {
        // Sleep long enough to generate 100+ tokens
        Thread.sleep(500);

        // Even though time passed for 100 tokens, bucket capacity is capped at 10
        int count = 0;
        while (bucket.tryGetToken()) {
            count++;
        }

        assertEquals(CAPACITY, count, "Bucket allowed more tokens than max capacity!");
    }

}