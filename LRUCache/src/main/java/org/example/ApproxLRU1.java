package org.example;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

class CacheEntry{
    public volatile int value;
    private volatile long lastAccessedTime;//default time

    public CacheEntry(int value){
        this.value = value;
        this.lastAccessedTime = System.currentTimeMillis(); // still have to update this while accessng the put or get
    }

    //add touch methd
    public void touch(){
        this.lastAccessedTime = System.currentTimeMillis();
    }

    long getLastAccessedTime(){
        return this.lastAccessedTime;
    }
}

public class ApproxLRU1 implements AutoCloseable {
    //create a ConcurrenHashMap
    private final Map<Integer, CacheEntry> mapper = new ConcurrentHashMap<>();
    private final int capacity;
    private final int evictionSamples;
    private final Random random = new Random();
    private final Buffer buffer;

    public ApproxLRU1(int capacity, int evictionSamples) {
        this.capacity = capacity;
        this.evictionSamples = evictionSamples;
        this.buffer = new Buffer(this::touchKeyFromBackground);
        this.buffer.start();
    }

    public void put(int key, int value) {

//        if(!mapper.containsKey(key)){
//            //create a cache entry
//            CacheEntry cacheEntry = new CacheEntry(value);
//            mapper.put(key, cacheEntry);
//        }else{
//          //get the cahcheenty and update the value of the cache entry
//          mapper.get(key).value = value;
//        }

        // compute is atomic per key: read-check0write happens as one operation
        // so no other thread can interleave between "does thiskey exists" &
        // update/create the entry as two calls- eliminate this

        mapper.compute(key, (k, existing) -> {
            if(existing == null) {
                return new CacheEntry(value);
            } else {
                existing.value = value;
                existing.touch();
                return existing;
            }

        });

        // check the size of the cache, if it exceeds, then call the evict method
        if(mapper.size() > capacity){
            evict();
        }
    }

    public int get(int key) {
        CacheEntry entry = mapper.get(key);
        if (entry == null) {
            throw new NoSuchElementException("No such key: " + key);
        }
        // No direct touch() here anymore — just enqueue the access event
        // and return immediately. The background Buffer thread applies
        // the actual timestamp update later, off this read path.
        buffer.addMessage(key);
        return entry.value;
    }

    void touchKeyFromBackground(int key) {
        CacheEntry entry = mapper.get(key);
        if (entry != null) {
            entry.touch();
        }
        // if entry is null, the key was evicted before the background
        // thread got to it — nothing to do, safe to ignore
    }

    private synchronized void evict(){
        // get the random keys and access there last time accessed and remove the lowest one from the map.
        // Use Random func on all the keys five times and remove the oldest one.
        Integer[] keys = mapper.keySet().toArray(new Integer[0]);
        if(keys.length == 0){
            return;
        }

        Integer worstKey = null;
        long oldestTime = Long.MAX_VALUE;
        for(int i = 0; i < evictionSamples; i++){
            //Just create random selections
            Integer sampledKey = keys[random.nextInt(keys.length)];
            CacheEntry entry = mapper.get(sampledKey);
            // entry can be null if another thread removed it between
            // snapshot and this lookup - just skip it, It's already
            if(entry != null && entry.getLastAccessedTime() < oldestTime){
                oldestTime = entry.getLastAccessedTime();
                worstKey = sampledKey;
            }
        }
        if(worstKey != null){
            mapper.remove(worstKey);
        }
    }

    public int size(){
        return mapper.size();
    }

    @Override
    public void close() throws Exception {
        buffer.stop();
    }
}
