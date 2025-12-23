package com.gamma.spool.util.fast.bitset;

import java.util.concurrent.locks.StampedLock;

public class AtomicBitSetCompact implements FastAtomicBitSet {

    private final StampedLock lock = new StampedLock();

    private final long[] array;

    private final int length;

    public AtomicBitSetCompact(int bitsLength) {
        array = new long[(length = bitsLength) >> 6];
    }

    @Override
    public void set(int bitIndex) {
        long stamp = lock.writeLock();
        int idx = bitIndex >> 6;
        long bit = 1L << bitIndex;
        try {
            array[idx] |= bit;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public void clear(int bitIndex) {
        long stamp = lock.writeLock();
        int idx = bitIndex >> 6;
        long bit = 1L << bitIndex;
        try {
            array[idx] &= ~bit;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public void set(int bitIndex, boolean value) {
        if (value) set(bitIndex);
        else clear(bitIndex);
    }

    @Override
    public void flip(int bitIndex) {
        int idx = bitIndex >> 6;
        long bit = 1L << bitIndex;
        long stamp = lock.writeLock();
        try {
            array[idx] ^= bit;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public boolean get(int bitIndex) {
        int idx = bitIndex >> 6;
        long bit = 1L << bitIndex;
        long stamp = lock.readLock();
        try {
            return get0(idx, bit);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    private boolean get0(int idx, long bit) {
        return (array[idx] & bit) != 0;
    }

    private boolean get0(int bitIndex) {
        int idx = bitIndex >> 6;
        long bit = 1L << bitIndex;
        return (array[idx] & bit) != 0;
    }

    @Override
    public int numSetBits() {
        int idx;
        int num;
        long stamp = lock.readLock();
        try {
            boolean value = get0(idx = num = 0);

            while (true) {
                if (value) num++;
                if (idx++ == length) return num;
                value = get0(idx);
            }
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public int numClearBits() {
        int idx;
        int num;
        long stamp = lock.readLock();
        try {
            boolean value = get(idx = num = 0);

            while (true) {
                if (!value) num++;
                if (idx++ == length) return num;
                value = get(idx);
            }
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public int length() {
        return length;
    }
}
