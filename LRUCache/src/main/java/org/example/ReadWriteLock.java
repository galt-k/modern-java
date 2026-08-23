package org.example;

public class ReadWriteLock {
    private int readers = 0;
    private int writers = 0;
    private int writeRequests = 0;

    //1. Acquire the READ lock
    public synchronized void acquireReadLock() throws InterruptedException {
        //Block if a write is active or if the writers are waiting (Prevents writers starvation)
        while(writers > 0 || writeRequests > 0){
            //Enters wait set, sleeps
            wait();
        }
        readers++;
    }

    //Release the read lock
    public synchronized void releaseReadLock() throws InterruptedException {
        readers--;
        if(readers==0){
            notifyAll();
        }
    }

    public synchronized void acquireWriteLock() throws InterruptedException {
        writeRequests++;
        //Block if any readers or writers are qctive
        while(readers > 0 || writers > 0){
            try {
                wait();
            } catch (InterruptedException e) {
                writeRequests--;
                notifyAll();
                throw e;
            }
        }

        writeRequests--;
        writers++;
    }

    public synchronized void releaseWriteLock() throws InterruptedException {
        writers--;
        notifyAll();
    }

}
