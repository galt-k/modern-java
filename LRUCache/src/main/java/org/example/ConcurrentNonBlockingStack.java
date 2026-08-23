package org.example;

import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;

class StackNode<T>{
    public T value;
    public StackNode<T> next;
    //public StackNode prev;

    public StackNode(T value){
        this.value = value;
        this.next = null;
    }
}

public class ConcurrentNonBlockingStack<T> {
    //Atomic reference container initialized to hold the top stackNode(defaults to null)

    public AtomicReference<StackNode<T>> top ;
    public ConcurrentNonBlockingStack() {
        this.top = new AtomicReference<>();
    }

    public void push(T value){
        //Create the new Node
        StackNode<T> newNode = new StackNode<>(value);
        StackNode<T> oldTop;
        do {
            // get the top node and update it
            oldTop = top.get();
            //StackNode oldTop = newNode.prev;
            newNode.next = oldTop;
        } while (!top.compareAndSet(oldTop, newNode));
    }

    public T pop(){
        StackNode<T> oldTop;
        StackNode<T> newTop;
        do {
            oldTop = top.get();
            if (oldTop == null) {
                return null; // Safe exit for empty stack
            }
            newTop = oldTop.next;
        } while ( !top.compareAndSet(oldTop, newTop));
        return oldTop.value;
    }

    public T peek(){
        StackNode<T> currentTop = top.get();
        return currentTop == null ? null : currentTop.value;
    }

    public boolean isEmpty(){
        return top.get() == null;
    }

}
