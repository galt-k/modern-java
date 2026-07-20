package org.example;
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

    public ConcurrentBlockingQueue(int capacity) {
        this.size = 0;
        this.head = null;
        this.tail = null;
        this.capacity = capacity;
    }
    // create a new node and update the tail pointer
    public void put(int value) {
        synchronized (this) {
            if(this.size == this.capacity) {
                return;
            }

            Node newNode = new Node(value);
            this.size++;
            if (this.head == null) {
                this.head = newNode;
                this.tail = newNode;
                return;
            }
            this.tail.next = newNode;
            this.tail = newNode;
        }
        return;
    }

    public int get() {
        // return the value in the head
        // update the head pointer
        // should also update the size
        synchronized (this) {
            if(this.head==null) {
                return -1;
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
            return result;
        }


    }
}
