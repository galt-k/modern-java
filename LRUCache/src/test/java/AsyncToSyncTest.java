package org.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncToSyncTest {

    @Test
    @DisplayName("Should block caller thread until background execution finishes")
    @Timeout(value = 10, unit = TimeUnit.SECONDS) // Safety guard against infinite blocking
    void testExecutionBlocksAndPrintsInCorrectOrder() throws Exception {
        AsyncToSync runner = new AsyncToSync();

        // Capture standard output to verify print sequence
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        long startTime = System.currentTimeMillis();

        try {
            runner.execute();
        } finally {
            System.setOut(originalOut); // Restore original stdout
        }

        long elapsedTime = System.currentTimeMillis() - startTime;

        // 1. Assert timing: must have blocked for ~5000ms (allowing small OS scheduling margin)
        assertTrue(elapsedTime >= 4900,
                "Expected execute() to block for at least 5000ms, but elapsed time was " + elapsedTime + "ms");

        // 2. Assert output sequence: 'done' MUST appear before 'main thread exiting'
        String output = outContent.toString().trim().replace("\r\n", "\n");
        String expectedOutput = "done\nmain thread exiting";

        assertEquals(expectedOutput, output, "Output sequence should strictly print 'done' before 'main thread exiting'");
    }

    @Test
    @DisplayName("Should support multiple sequential calls without state leakage")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testMultipleSequentialExecutions() throws Exception {
        AsyncToSync runner = new AsyncToSync();

        long startTime = System.currentTimeMillis();

        // First execution
        runner.execute();
        // Second execution (tests that latch state isn't stale)
        runner.execute();

        long elapsedTime = System.currentTimeMillis() - startTime;

        // 2 executions x 5000ms = ~10000ms total blocking time
        assertTrue(elapsedTime >= 9800,
                "Two sequential runs should block for ~10000ms, but elapsed time was " + elapsedTime + "ms");
    }
}