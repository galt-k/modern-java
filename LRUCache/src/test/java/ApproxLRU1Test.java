package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApproxLRU1Test {

    @Test
    void putAndGetReturnsCorrectValue() {
        ApproxLRU1 lru = new ApproxLRU1(10, 5);
        lru.put(1, 100);
        assertEquals(100, lru.get(1));
    }

    @Test
    void updatingExistingKeyOverwritesValue() {
        ApproxLRU1 lru = new ApproxLRU1(10, 5);
        lru.put(1, 100);
        lru.put(1, 200);
        assertEquals(200, lru.get(1));
    }

    @Test
    void sizeNeverExceedsCapacity() {
        ApproxLRU1 lru = new ApproxLRU1(10, 5);
        for (int i = 0; i < 20; i++) {
            lru.put(i, i);
        }
        assertTrue(lru.size() <= 10);
    }

    @Test
    void getOnMissingKeyThrows() {
        ApproxLRU1 lru = new ApproxLRU1(10, 5);
        assertThrows(java.util.NoSuchElementException.class, () -> lru.get(999));
    }

    @Test
    void recentlyAccessedKeysSurviveMoreOftenThanUntouchedOnes() throws InterruptedException {
        // Statistical test, not exact — approximated LRU is probabilistic by
        // design. We assert a loose bound, not an exact count.
        int n = 40;
        ApproxLRU1 lru = new ApproxLRU1(n, 5);

        for (int i = 0; i < n; i++) {
            lru.put(i, i);
            Thread.sleep(2);
        }

        // touch the first half, making them "fresh"
        for (int i = 0; i < n / 2; i++) {
            lru.get(i);
            Thread.sleep(2);
        }

        // force evictions by adding 50% more new keys
        for (int i = n; i < n + (n / 2); i++) {
            lru.put(i, i);
            Thread.sleep(2);
        }

        int survivedFirstHalf = 0;
        int survivedSecondHalf = 0;
        for (int i = 0; i < n / 2; i++) {
            if (keyExists(lru, i)) survivedFirstHalf++;
        }
        for (int i = n / 2; i < n; i++) {
            if (keyExists(lru, i)) survivedSecondHalf++;
        }

        // Loose assertion: touched keys should survive at least as often as
        // untouched ones. Not a strict inequality since sampling is random
        // and small sample sizes can occasionally flip this.
        assertTrue(survivedFirstHalf >= survivedSecondHalf,
                "Expected touched keys to survive at least as often as untouched ones. "
                        + "First half survived: " + survivedFirstHalf
                        + ", second half survived: " + survivedSecondHalf);
    }

    private boolean keyExists(ApproxLRU1 lru, int key) {
        try {
            lru.get(key);
            return true;
        } catch (java.util.NoSuchElementException e) {
            return false;
        }
    }
}