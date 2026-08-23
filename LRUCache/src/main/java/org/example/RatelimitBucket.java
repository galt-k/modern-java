package org.example;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class BucketState {
    final double tokens;
    final long lastRefillTimestamp;
    BucketState(double tokens, long lastRefillTimestamp) {
        this.tokens = tokens;
        this.lastRefillTimestamp = lastRefillTimestamp;
    }
}

//Non-blocking CAS approach
public class RatelimitBucket {

    private final int capacity;
    private AtomicInteger AvailableTokens;
    private final AtomicReference<BucketState> bucketState;
    private final double refillRateperMs;
    public RatelimitBucket(int capacity, double tokensPerSecond) {
        this.AvailableTokens = new AtomicInteger(capacity);
        this.capacity = capacity;
        this.refillRateperMs = tokensPerSecond/1000.0;
        this.bucketState = new AtomicReference<>(new BucketState(capacity, System.currentTimeMillis()));
    }

    public boolean tryGetToken(){
        /* Get the current size.
         * if size is 0, then check the timestamp
         */
        while (true) {
            BucketState current = bucketState.get();
            long now = System.currentTimeMillis();

            //1. calculate refilled Tokens lazily;
            long elapsedTime = Math.max(0, now - current.lastRefillTimestamp);
            double generatedTokens = elapsedTime * refillRateperMs;
            double newTokens = Math.min(capacity, current.tokens + generatedTokens);

            //2. check if there is atleast 1 token?
            if(newTokens >= 1.0){
                //update the state
                BucketState next = new BucketState(newTokens - 1.0, now);
                // atomic CAS update: only succeds if no other thread modified state
                if(bucketState.compareAndSet(current, next)){
                    return true;
                }
            }else{
                return false;
            }
        }
        /// ///////////////////////////////////////////////////////////
//        if(this.AvailableTokens.get() == 0){
//            //check the last timestamp get the current time, cal the deltaT
//            long currentTimestamp =  System.currentTimeMillis();
//            long deltaT =  currentTimestamp - lastUpdatedTimestamp.get();
//            long newToken = deltaT * (long) 0.01;
//            if(deltaT >= 1){
//                // update the size as newToken - 1
//                // THis should be CAS operation, instead of the below logic
//                this.AvailableTokens.set((int) newToken - 1);
//                lastUpdatedTimestamp.set(currentTimestamp);
//
//            }
//        }else{
//            this.AvailableTokens.getAndDecrement();
//        }
    }
}
