package org.example;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ReadWriteLockTest {

    private final ReadWriteLock lock = new ReadWriteLock();

    // Track concurrent active threads to verify mutual exclusion
    private final AtomicInteger activeReaders = new AtomicInteger(0);
    private final AtomicInteger activeWriters = new AtomicInteger(0);
    private final AtomicInteger maxConcurrentReaders = new AtomicInteger(0);
    private final AtomicInteger totalInvariantsViolated = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        ReadWriteLockTest test = new ReadWriteLockTest();
        test.runConcurrentReadWriteTest();
    }

    public void runConcurrentReadWriteTest() throws InterruptedException {
        int numReaders = 10;
        int numWriters = 3;
        int iterationsPerThread = 20;

        ExecutorService executor = Executors.newFixedThreadPool(numReaders + numWriters);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numReaders + numWriters);

        System.out.println("Starting ReadWriteLock Stress Test...");

        // Launch Readers
        for (int i = 0; i < numReaders; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize thread startup
                    for (int j = 0; j < iterationsPerThread; j++) {
                        lock.acquireReadLock();

                        int currentReaders = activeReaders.incrementAndGet();
                        maxConcurrentReaders.accumulateAndGet(currentReaders, Math::max);

                        // INVARIANT CHECK: No writers should be active while reading
                        if (activeWriters.get() > 0) {
                            System.err.printf("[FAIL] Reader %d saw %d active writers!\n", id, activeWriters.get());
                            totalInvariantsViolated.incrementAndGet();
                        }

                        Thread.sleep(10); // Simulate reading work

                        activeReaders.decrementAndGet();
                        lock.releaseReadLock();

                        Thread.sleep(5); // Brief pause before next acquire
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Launch Writers
        for (int i = 0; i < numWriters; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize thread startup
                    for (int j = 0; j < iterationsPerThread; j++) {
                        lock.acquireWriteLock();

                        int currentWriters = activeWriters.incrementAndGet();

                        // INVARIANT CHECK 1: Only ONE writer allowed at a time
                        if (currentWriters > 1) {
                            System.err.printf("[FAIL] Writer %d entered, but active writers = %d!\n", id, currentWriters);
                            totalInvariantsViolated.incrementAndGet();
                        }

                        // INVARIANT CHECK 2: Zero readers allowed while writing
                        if (activeReaders.get() > 0) {
                            System.err.printf("[FAIL] Writer %d entered, but active readers = %d!\n", id, activeReaders.get());
                            totalInvariantsViolated.incrementAndGet();
                        }

                        Thread.sleep(20); // Simulate writing work

                        activeWriters.decrementAndGet();
                        lock.releaseWriteLock();

                        Thread.sleep(10); // Brief pause before next acquire
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release all threads simultaneously
        startLatch.countDown();

        // Wait for work to complete
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // --- RESULTS ---
        System.out.println("---------------------------------------------");
        System.out.println("Test Completed in Time: " + completed);
        System.out.println("Max Concurrent Readers Observed: " + maxConcurrentReaders.get());
        System.out.println("Total Invariant Violations: " + totalInvariantsViolated.get());

        if (completed && totalInvariantsViolated.get() == 0 && maxConcurrentReaders.get() > 1) {
            System.out.println("STATUS: PASS (Locks held mutually exclusive, multiple readers succeeded)");
        } else {
            System.out.println("STATUS: FAIL");
        }
    }
}