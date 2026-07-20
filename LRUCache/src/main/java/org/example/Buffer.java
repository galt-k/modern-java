package org.example;

//class Message{
//    public Integer key;
//    public Message(){
//        this.key
//    }
//}

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntConsumer;

public class Buffer {
    // we need a queue
    private Queue<Integer> accessBuffer = new ConcurrentLinkedQueue<>();
    private Integer chunkSize = 5;
    private ExecutorService executor;
    private volatile boolean running = true;

    private final IntConsumer onKeyAccessed;

    public Buffer(IntConsumer onKeyAccessed) {
        //this.onKeyAccessed = onKeyAccessed;
        this(onKeyAccessed, 5); // delegates to the other constructor, using this()

    }

    public Buffer(IntConsumer onKeyAccessed, int chunkSize) {
        this.onKeyAccessed = onKeyAccessed;
        this.chunkSize = chunkSize;

        this.executor = Executors.newFixedThreadPool(1, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

    }
    public void start() {
        //executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        executor.submit(this::runLoop);
    }

    private void runLoop() {
        while (running){
            List<Integer> batch = drainBatch();
            if(!batch.isEmpty()){
                applyBatch(batch);
            } else{
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private List<Integer> drainBatch() {
        List<Integer> batch = new ArrayList<>();
        Integer item;
        while(batch.size() < chunkSize && (item = accessBuffer.poll()) != null){
            batch.add(item);
        }
        return batch;
    }

    private void applyBatch(List<Integer> batch) {
        //dedupe - only the fact that a key was accessed maters, not
        //many times it appeared within this particular batch.

        Set<Integer> distinctKeys = new HashSet<>(batch);
        for(int key: distinctKeys){
            onKeyAccessed.accept(key);
        }
    }


//    public void process(){
//        //So, in  the batch i will get the disitnct keys
//        // and i will directly update the acess timestamps
//        // for those keys.
//        // So, here i'm avoiding the lock time for the same key.
//        //create number of threads based on the message count
//
//        executor.submit((chunk)->{
//            // get
//        });
//
//    }

    public void addMessage(int key){
        accessBuffer.add(key);
    }

    public void stop(){
        running = false;
        executor.shutdown();
    }
}
