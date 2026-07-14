package org.example;

import java.util.ArrayList;
import java.util.List;

public class SegmentCache {
    //private List<ApproxLRU1> segments =  new ArrayList<ApproxLRU1>();
    private final int numSegments;
    private final ApproxLRU1[] segments;

    public SegmentCache(int numSegments, int evictionSamples, int capacity) {
        this.numSegments = numSegments;
        this.segments = new ApproxLRU1[numSegments];
        int perSegmentCapacity = numSegments / numSegments;
        for (int i = 0; i < numSegments; i++) {
            segments[i] = new ApproxLRU1(perSegmentCapacity, evictionSamples);
        }
    }
    private ApproxLRU1 segmentFor(int key) {
        int idx = Math.floorMod(Integer.hashCode(key), numSegments);
        return segments[idx];
    }

    public int get(int key) {
        //gwt the segment for key
        return segmentFor(key).get(key);
    }

    public void put(int key, int value) {
        segmentFor(key).put(key, value);
    }

}
