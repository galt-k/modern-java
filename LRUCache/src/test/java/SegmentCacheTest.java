import org.example.SegmentCache;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class SegmentCacheTest {
    @Test
    void putAndGetReturnsCorrectValue(){
        SegmentCache cache = new SegmentCache(5, 5, 25);
        cache.put(1,100);
        assertEquals(100, cache.get(1));
    }

    /**
     * Concurrent stress test many threads hammering putget across
     * a wide range
     * Executors.newFixedThreadPool(16) gives you 16 reusabel threads
     * I used this over manually creating new Thread()
     * Submitting tasks to an executor doesn't block- submit
     **/
//    @Test
//    void concurrentTest(){
//        //create a segment cache
//        SegmentCache cache = new SegmentCache(5, 5, 25);
//        //create a 5 thread executor pool.
//        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor(5);
//        try{
//            cache.put(1,100);
//            cache.put(2,100);
//            cache.put(3,100);
//
//        } finally {
//            executorService.shutdown();
//        }
//
//
//    }

    @RepeatedTest(3)
    void concurrentPutAndGetDoesNotCorruptOrDeadlock() throws InterruptedException {
        int totalCapacity = 1000;
        int numSegments = 8;
        SegmentCache cache = new SegmentCache(totalCapacity, 5, numSegments);

        int threadCount = 16;
        int opsPerThread = 5000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errors = new AtomicInteger(0);

        // Submitting tasks to an executor doesnt block- submit() returns immediately.
        // Without the latch, the test method would finish and assert before any thred actually did work.
        //

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    java.util.Random random = new java.util.Random();
                    for (int i = 0; i < opsPerThread; i++) {
                        int key = random.nextInt(2000);
                        cache.put(key, key * 10);
                        try {
                            cache.get(key); // may or may not exist depending on eviction timing
                        } catch (java.util.NoSuchElementException ignored) {
                            // expected sometimes — key may have been evicted
                            // between put and get by another thread
                        }
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Threads did not complete in time — possible deadlock");
        assertEquals(0, errors.get(), "Unexpected exceptions occurred during concurrent access");
        //assertTrue(cache.size() <= totalCapacity,
        //        "Size exceeded capacity after concurrent load: " + cache.size());
    }

}
