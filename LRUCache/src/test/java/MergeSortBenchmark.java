package org.example;

import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(value = 1)
public class MergeSortBenchmark {

    @Param({"100", "10000", "100000"})
    private int arraySize;

    private int[] masterArray;
    private int[] testArray;

    @Setup(Level.Trial)
    public void setupTrial() {
        Random random = new Random(42);
        masterArray = new int[arraySize];
        for (int i = 0; i < arraySize; i++) {
            masterArray[i] = random.nextInt();
        }
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        testArray = masterArray.clone();
    }

    @Benchmark
    public int[] measureSingleThreadedMergeSort() {
        MultithreadingMergeSort.mergeSort(testArray, 0, testArray.length - 1);
        return testArray;
    }

    @Benchmark
    public int[] measureJdkParallelSort() {
        Arrays.parallelSort(testArray);
        return testArray;
    }

    @Benchmark
    public int[] measureCustomParallelSort() {
        MultithreadingMergeSort.customParallelMergeSort(testArray, 0, testArray.length - 1);
        return testArray;
    }

    // JUnit 5 entry point allows running the benchmark directly in the IDE test tab
    @Test
    public void executeJmhBenchmark() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(MergeSortBenchmark.class.getSimpleName())
                .shouldFailOnError(true)
                .build();

        new Runner(opt).run();
    }
}