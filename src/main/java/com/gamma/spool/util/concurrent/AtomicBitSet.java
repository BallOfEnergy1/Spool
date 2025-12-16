package com.gamma.spool.util.concurrent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLongArray;

public class AtomicBitSet {

    private final AtomicLongArray array;
    private AtomicBoolean allSet = new AtomicBoolean(false);

    public AtomicBitSet(int bitsLength) {
        int intLength = (bitsLength + 63) >>> 6;
        array = new AtomicLongArray(intLength);
    }

    public void set(int n) {
        int idx = n >>> 6;
        int bit = 1 << n;
        if (!get(idx, bit)) array.getAndAdd(idx, bit);
    }

    public void clear(int n) {
        int idx = n >>> 6;
        int bit = 1 << n;
        if (get(idx, bit)) {
            array.getAndAdd(idx, -bit);
            allSet.set(false);
        }
    }

    public boolean get(int n) {
        int idx = n >>> 6;
        int bit = 1 << n;
        return get(idx, bit);
    }

    private boolean get(int idx, int bit) {
        long num = array.get(idx);
        return (num & bit) != 0;
    }

    public int firstClearBit() {
        if (allSet.get()) return -1;
        int idx;
        long word = ~array.get(idx = 0);

        while (true) {
            if (word != 0) return (idx << 6) + Long.numberOfTrailingZeros(word);
            if (idx++ == array.length()) {
                allSet.set(true);
                return -1;
            }
            word = ~array.get(idx);
        }
    }

    public int numSetBits() {
        if (allSet.get()) return array.length() << 6;
        int idx;
        int num;
        long word = array.get(num = idx = 0);

        while (true) {
            if (word != 0) num += Long.bitCount(word);
            if (idx++ == array.length()) return num;
            word = array.get(idx);
        }
    }

    public int numClearBits() {
        if (allSet.get()) return 0;
        int idx;
        int num;
        long word = ~array.get(num = idx = 0);

        while (true) {
            if (word != 0) num += Long.bitCount(word);
            if (idx++ == array.length()) return num;
            word = ~array.get(idx);
        }
    }
}
