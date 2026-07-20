package org.example;

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
    private int size;
    private Node head;
    private Node tail;
    private int capacity;
    //create a new Reentrant lock
    ReentrantLock lock = new ReentrantLock(true); // I wanted it to be a fair chance.
    //create a new condition lock
    Condition notFull = lock.newCondition();
    Condition notEmpty = lock.newCondition();

    public ConcurrentBlockingQueue(int capacity) {
        this.size = 0;
        this.head = null;
        this.tail = null;
        this.capacity = capacity;
    }
    // create a new node and update the tail pointer
    public void put(int value) throws InterruptedException {
        //here add a lock
        lock.lock();
        try {
            while (this.size == this.capacity) {
                //here i want to ensure the thread goes into the blocking state and
                notFull.await();
            }

            Node newNode = new Node(value);
            this.size++;
            // FIXED: Clean branch ensuring we don't double-link a node to itself
            if (this.head == null) {
                this.head = newNode;
                this.tail = newNode;
            } else {
                this.tail.next = newNode;
                this.tail = newNode;
            }

            //Tell a sleeping consumer that data is available
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }

        return;
    }

    public int get() throws InterruptedException {
        // return the value in the head
        // update the head pointer
        // should also update the size
        lock.lock();
        try {
            while (this.head == null) {
                notEmpty.await();
            }
            int result;
            if (this.head == this.tail) {
                result = this.head.value;
                this.head = null;
                this.tail = null;
            } else {
                result = this.head.value;
                Node temp = this.head;
                this.head = this.head.next;
                temp.next = null;
            }
            this.size--;
            notFull.signalAll();
            return result;
        } finally {
            lock.unlock();
        }

    }
}
