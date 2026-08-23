package org.example;
// ====================================================================
// DO NOT MODIFY BELOW THIS LINE
// Imagine these classes come from an uneditable compiled library (.jar)
// ====================================================================

import java.util.concurrent.CountDownLatch;

interface Callback {
    void done();
}

class Executor {
    public void asynchronousExecution(Callback callback) throws Exception {
        Thread t = new Thread(() -> {
            // Simulate long-running work
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            // Execute the callback passed in by the caller
            callback.done();
        });
        t.start();
    }
}

public class AsyncToSync {
    //create the executor
    Executor executor = new Executor();
    public void execute() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        executor.asynchronousExecution(new Callback() {
            @Override
            public void done() {
                try {
                    //original Callback logic
                    System.out.println("done");
                } finally {
                    latch.countDown();
                }
            }
        });

        latch.await();

        System.out.println("main thread exiting");
    }
}
