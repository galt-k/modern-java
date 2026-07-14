package org.example;

//class Message{
//    public Integer key;
//    public Message(){
//        this.key
//    }
//}

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Buffer {
    // we need a queue
    private Queue<Integer> accessBuffer = new ConcurrentLinkedQueue<>();
    private Integer chunkSize = 5;
    private ExecutorService executor = Executors.newFixedThreadPool(1);


    public void process(){
        //So, in  the batch i will get the disitnct keys
        // and i will directly update the acess timestamps
        // for those keys.
        // So, here i'm avoiding the lock time for the same key.
        //create number of threads based on the message count

        executor.submit((chunk)->{
            // get
        });

    }

    public void addMessage(int message){
        accessBuffer.add(message);
    }
}
