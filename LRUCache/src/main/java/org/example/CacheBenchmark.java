package org.example;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Compares Stage 1 (single global-lock cache) vs Stage 2 (segmented cache)
 * under a read-heavy workload (95% get / 5% put), across varying thread
 * counts.
 *
 * Run with (after `mvn package`):
 *   java -jar target/benchmarks.jar
 *
 * To sweep thread counts explicitly:
 *   java -jar target/benchmarks.jar -t 1
 *   java -jar target/benchmarks.jar -t 4
 *   java -jar target/benchmarks.jar -t 8
 *   java -jar target/benchmarks.jar -t 16
 *   java -jar target/benchmarks.jar -t 32
 *
 * Or let JMH's @Threads annotation control it directly (see below) and just run:
 *   java -jar target/benchmarks.jar
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class CacheBenchmark {

    // Key space larger than capacity so eviction pressure is real and
    // constant during the benchmark, not a one-time warmup event.
    static final int KEY_SPACE = 20_000;
    static final int CAPACITY = 10_000;
    static final int EVICTION_SAMPLES = 5;
    static final int NUM_SEGMENTS = 8;

    ApproxLRU1 globalLockCache;
    SegmentCache segmentedCache;

    @Setup(Level.Trial)
    public void setup() {
        globalLockCache = new ApproxLRU1(CAPACITY, EVICTION_SAMPLES);
        segmentedCache = new SegmentCache(CAPACITY, EVICTION_SAMPLES, NUM_SEGMENTS);

        // pre-fill so both caches start under realistic load
        for (int i = 0; i < CAPACITY; i++) {
            globalLockCache.put(i, i);
            segmentedCache.put(i, i);
        }
    }

    // --- Stage 1: global-lock cache ---

    @Benchmark
    @Threads(1)
    public void globalLock_t1(Blackhole bh) {
        doMixedWorkload(bh, globalLockCache::get, globalLockCache::put);
    }

    @Benchmark
    @Threads(4)
    public void globalLock_t4(Blackhole bh) {
        doMixedWorkload(bh, globalLockCache::get, globalLockCache::put);
    }

    @Benchmark
    @Threads(8)
    public void globalLock_t8(Blackhole bh) {
        doMixedWorkload(bh, globalLockCache::get, globalLockCache::put);
    }

    @Benchmark
    @Threads(16)
    public void globalLock_t16(Blackhole bh) {
        doMixedWorkload(bh, globalLockCache::get, globalLockCache::put);
    }

    // --- Stage 2: segmented cache ---

    @Benchmark
    @Threads(1)
    public void segmented_t1(Blackhole bh) {
        doMixedWorkload(bh, segmentedCache::get, segmentedCache::put);
    }

    @Benchmark
    @Threads(4)
    public void segmented_t4(Blackhole bh) {
        doMixedWorkload(bh, segmentedCache::get, segmentedCache::put);
    }

    @Benchmark
    @Threads(8)
    public void segmented_t8(Blackhole bh) {
        doMixedWorkload(bh, segmentedCache::get, segmentedCache::put);
    }

    @Benchmark
    @Threads(16)
    public void segmented_t16(Blackhole bh) {
        doMixedWorkload(bh, segmentedCache::get, segmentedCache::put);
    }

    // --- shared workload logic ---

    interface Getter { int get(int key); }
    interface Putter { void put(int key, int value); }

    private void doMixedWorkload(Blackhole bh, Getter getter, Putter putter) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int key = random.nextInt(KEY_SPACE);

        if (random.nextInt(100) < 95) {
            // 95% reads
            try {
                bh.consume(getter.get(key));
            } catch (java.util.NoSuchElementException ignored) {
                // expected sometimes — key may not exist yet or was evicted
            }
        } else {
            // 5% writes
            putter.put(key, key);
        }
    }
}