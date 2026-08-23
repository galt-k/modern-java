package org.example;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class ConcurrentNonBlockingStackTest {
    private ConcurrentNonBlockingStack<Integer> stack;

    @BeforeEach
    void setUp() {
        stack = new ConcurrentNonBlockingStack<>();
    }

    @Test
    @DisplayName("Concurrent Push & Pop: Verifies zero data loss under high contention")
    void testConcurrentPushAndPop() throws InterruptedException {
        int threadCount = 16;
        int itemsPerThread = 1_000;
        int totalExpectedItems = threadCount * itemsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount * 2);

        // Latches to synchronize thread start and wait for completion
        CountDownLatch readyLatch = new CountDownLatch(threadCount * 2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount * 2);

        // Thread-safe collection to record all popped values
        Set<Integer> poppedValues = Collections.newSetFromMap(new ConcurrentHashMap<>());

        // 1. Submit Producer Threads (Pushes)
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Hold until all threads are ready
                    for (int j = 0; j < itemsPerThread; j++) {
                        // Generate unique integer: e.g., thread 0 pushes 0..999, thread 1 pushes 1000..1999
                        int val = (threadId * itemsPerThread) + j;
                        stack.push(val);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // 2. Submit Consumer Threads (Pops + Peeks)
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Hold until all threads are ready
                    int poppedCount = 0;
                    while (poppedCount < itemsPerThread) {
                        // Non-destructive read test alongside pop
                        stack.peek();

                        Integer val = stack.pop();
                        if (val != null) {
                            poppedValues.add(val);
                            poppedCount++;
                        } else {
                            // Empty state handling: yield to allow producers to make progress
                            Thread.yield();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Release all threads simultaneously
        readyLatch.await();
        startLatch.countDown();

        // Wait up to 10 seconds for all operations to finish
        boolean finished = finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // 3. Assertions
        assertTrue(finished, "Test timed out! Possible infinite CAS loop or deadlock.");
        assertEquals(totalExpectedItems, poppedValues.size(),
                "Data loss or duplicate pop detected! Expected " + totalExpectedItems + " unique items.");
        assertTrue(stack.isEmpty(), "Stack should be empty after all items are popped.");
    }
}
