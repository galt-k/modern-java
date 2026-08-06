import java.util.concurrent.*;

// Task holds the logic, target time, and the Completable Future handle
class Task implements Comparable<Task> {
    private final Runnable runnable;
    private final long execTimeNanos;
    final CompletableFuture<> future = new CompletableFuture<>();
    public Task(Runnable task, long delayMs){
        this.runnable = task;
        this.execTimeNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(delayMs);    }

    @Override
    public int compareTo(Task other){
        return Long.compare(execTimeNanos, other.execTimeNanos);
    }

}

public class ThreadSafeDeferredCallback {
    //create an executor for schedullign virutal threads
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    //create a priority queue
    private final PriorityBlockingQueue<Task> queue = new PriorityBlockingQueue<>();
    //need a background thread that watches the queu timing
    private final Thread WatcherThread;
    private boolean isRunning;

    public ThreadSafeDeferredCallback(){
        this.WatcherThread = new Thread(() -> {schedulerLoop()});
        this.WatcherThread.setDaemon(true);
        this.WatcherThread.start();
        this.isRunning = true;
    }

    public CompletableFuture<Void> registerCallBack(Runnable runnable, long delayTime){
        // create a task
        Task task = new Task(runnable, delayTime);
        // How do I wrap futre around the task? If i wrap it now can i add it into the queue? becasue the queus will call comparable funtion on the objects?
        //add it to the queue
        queue.add(task);
        // need a background thread that check if a task is available to run in the virutla thread executor pool.
        return task.future;
    }

    private void schedulerLoop(){
        while (isRunning) {
            //take the earlist task (blocks if queue is empty)
            Task task = queue.peek();
            if (task != null) {
                Thread.sleep(10);
                continue;
            }

            // Handle task cancellation if client cacelled the futre
            if(task.future.isCancelled()){
                queue.poll();
                continue;
            }

            long now = System.nanoTime();
            long delaynanos = task.execTimeNanos - now;
            if(delaynanos <= 0){
                Task readyTask = queue.poll();
                if(readyTask != null && !readyTask.future.isCancelled()){
                    //offload execution to virtual threads
                    virtualExecutor.submit(() -> {
                        try {
                            readyTask.runnable.run();
                            readyTask.future.complete(null);
                        } catch (Throwable t) {
                            readyTask.future.completeExceptionally(t);
                        }
                    });
                } else {
                    // TIme not reached yet
                    long sleepMs = TimeUnit.NANOSECONDS.toMillis(delaynanos);
                    if (sleepMs > 0) {
                        Thread.sleep(sleepMs);
                    } else {
                        // Spin briefly if remaining delay is less than 1ms
                        Thread.sleep(0, (int) (delaynanos % 1_000_000));
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void shutdown(){
        this.isRunning = false;
        this.WatcherThread.interrupt();
        virtualExecutor.shutdown();
    }



}
