package org.example;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class Node {
    int value;
    Node next;
    public Node(int value) {
        this.value = value;
    }
}

public class ConcurrentBlockingQueue {
    private AtomicInteger size = new AtomicInteger(0);
    private Node head;
    private Node tail;
    private int capacity;
    //create a new Reentrant lock
    ReentrantLock headLock = new ReentrantLock(true); // I wanted it to be a fair chance.
    ReentrantLock tailLock = new ReentrantLock(true); // I wanted it to be a fair chance.
    //create a new condition lock
    Condition notFull = tailLock.newCondition();
    Condition notEmpty = headLock.newCondition();

    public ConcurrentBlockingQueue(int capacity) {
        this.size.set(0);
        Node dummy = new Node(-1);
        this.head = dummy;
        this.tail = dummy;
        this.capacity = capacity;
    }
    // create a new node and update the tail pointer
    // used by producers, always a tail operation.
    public void put(int value) throws InterruptedException {
        //here add a lock
        int countBeforeInsertion = -1;
        tailLock.lock();
        try {
            while (this.size.get() == this.capacity) {
                //here i want to ensure the thread goes into the blocking state and
                notFull.await(); // waiting for the ideal condition to occur. The ideal condition is the queue to be notFull.
            }

            Node newNode = new Node(value);
            countBeforeInsertion = this.size.getAndIncrement();

            // FIXED: Clean branch ensuring we don't double-link a node to itself
            this.tail.next = newNode;
            this.tail = newNode;

            if(countBeforeInsertion + 1 < this.capacity) {
                notFull.signal();
            }

        } finally {
            tailLock.unlock();
        }

        if(countBeforeInsertion == 0) {
            //wake up the consumer threads
            headLock.lock();
            try {
                notEmpty.signal();
            } finally {
                headLock.unlock();
            }
        }
        return;
    }

    public int get() throws InterruptedException {
        // return the value in the head
        // update the head pointer
        // should also update the size
        int result;
        int countBeforeInsertion = -1;
        headLock.lock();
        try {
            while (this.head.next == null) {
                // if the queue is empty, the threads should go into waiting mode.
                notEmpty.await();
            }
            // the goal here is to not use tail.
            Node firsRealNode = this.head.next;
            result= firsRealNode.value;

            this.head = firsRealNode;


            //decrement the size by fetching the old value
            countBeforeInsertion = this.size.getAndDecrement();

            // if there are still items left? wake up the next consumer threads
            if(countBeforeInsertion > 1 ) {
                notEmpty.signal();
            }
        } finally {
            headLock.unlock();
        }
        //cross lock notification
        if(countBeforeInsertion == capacity) {
            tailLock.lock();
            try {
                notFull.signal();
            } finally {
                tailLock.unlock();
            }
        }

        return result;

    }
}
