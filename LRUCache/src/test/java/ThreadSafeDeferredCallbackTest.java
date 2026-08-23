package org.example;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ThreadSafeDeferredCallbackTest {

    private ThreadSafeDeferredCallback scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ThreadSafeDeferredCallback();
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdown();
    }

    @Test
    @DisplayName("Should execute a callback after the specified delay")
    void testBasicExecutionDelay() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        long delayMs = 200;
        long startTime = System.currentTimeMillis();

        CompletableFuture<Void> future = scheduler.registerCallBack(() -> {
            executed.set(true);
        }, delayMs);

        // Block until completed (with timeout safety)
        future.get(2, TimeUnit.SECONDS);

        long elapsedTime = System.currentTimeMillis() - startTime;

        assertTrue(executed.get(), "Task should have been executed");
        assertTrue(elapsedTime >= delayMs - 20, "Task executed too early! Elapsed: " + elapsedTime + "ms");
    }

    @Test
    @DisplayName("Should execute multiple tasks in order of their delays, not insertion order")
    void testExecutionOrdering() throws Exception {
        ConcurrentLinkedQueue<Integer> executionOrder = new ConcurrentLinkedQueue<>();

        // Register tasks out of order
        CompletableFuture<Void> f3 = scheduler.registerCallBack(() -> executionOrder.add(3), 300);
        CompletableFuture<Void> f1 = scheduler.registerCallBack(() -> executionOrder.add(1), 100);
        CompletableFuture<Void> f2 = scheduler.registerCallBack(() -> executionOrder.add(2), 200);

        // Wait for all to finish
        CompletableFuture.allOf(f1, f2, f3).get(2, TimeUnit.SECONDS);

        // Verify correct delay-based sequence
        assertArrayEquals(new Integer[]{1, 2, 3}, executionOrder.toArray(),
                "Tasks should execute in order of delay time");
    }

    @Test
    @DisplayName("Should handle exceptions thrown by callbacks and propagate them to CompletableFuture")
    void testExceptionPropagation() {
        String errorMessage = "Simulated failure";

        CompletableFuture<Void> future = scheduler.registerCallBack(() -> {
            throw new RuntimeException(errorMessage);
        }, 50);

        ExecutionException exception = assertThrows(ExecutionException.class, () -> {
            future.get(2, TimeUnit.SECONDS);
        });

        assertInstanceOf(RuntimeException.class, exception.getCause());
        assertEquals(errorMessage, exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Should not execute task if the future is cancelled before delay elapses")
    void testTaskCancellation() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);

        CompletableFuture<Void> future = scheduler.registerCallBack(() -> {
            executed.set(true);
        }, 500);

        // Cancel task almost immediately
        Thread.sleep(50);
        boolean cancelSuccess = future.cancel(true);

        assertTrue(cancelSuccess, "Future cancellation should return true");
        assertTrue(future.isCancelled(), "Future should mark as cancelled");

        // Wait past the original execution time window
        Thread.sleep(600);

        assertFalse(executed.get(), "Cancelled task should not have executed");
    }

    @Test
    @DisplayName("Should handle concurrent registrations safely")
    void testConcurrentSubmissions() throws Exception {
        int totalTasks = 100;
        ExecutorService clientThreadPool = Executors.newFixedThreadPool(10);
        AtomicInteger completedCount = new AtomicInteger(0);

        CompletableFuture<?>[] futures = new CompletableFuture[totalTasks];

        for (int i = 0; i < totalTasks; i++) {
            final int index = i;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                return scheduler.registerCallBack(() -> {
                    completedCount.incrementAndGet();
                }, 100 + (index % 10) * 10); // delays between 100ms and 190ms
            }, clientThreadPool).thenCompose(f -> f);
        }

        // Wait for all concurrent registrations and executions to complete
        CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);

        assertEquals(totalTasks, completedCount.get(), "All concurrently scheduled tasks should execute");
        clientThreadPool.shutdown();
    }
}