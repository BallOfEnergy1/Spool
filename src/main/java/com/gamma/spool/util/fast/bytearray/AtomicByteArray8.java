package com.gamma.spool.util.fast.bytearray;

import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.annotations.VisibleForTesting;

public class AtomicByteArray8 implements FastAtomicByteArray {

    private static final int BYTES_PER_LONG = 8;

    private final AtomicLongArray array;
    private final int length;

    public AtomicByteArray8(final int length) {
        this.length = length;
        this.array = new AtomicLongArray((length + 7) / BYTES_PER_LONG);
    }

    public void set(final int i, final byte newValue) {
        final int idx = i >>> 3;
        final int shift = (i & 7) << 3;
        final long mask = 0xFFL << shift;
        final long valueToInject = (newValue & 0xFFL) << shift;

        long oldLong, newLong;
        do {
            oldLong = this.array.get(idx);
            newLong = (oldLong & ~mask) | valueToInject;
        } while (!this.array.compareAndSet(idx, oldLong, newLong));
        cachedArray.set(null);
    }

    public byte get(final int i) {
        return (byte) ((this.array.get(i >>> 3) >> ((i & 7) << 3)) & 0xFFL);
    }

    public boolean compareAndSet(int i, byte expect, byte newValue) {
        int idx = i >>> 3;
        int shift = (i & 7) << 3;
        long mask = 0xFFL << shift;
        long expected = (expect & 0xFFL) << shift;
        long valueToInject = (newValue & 0xFFL) << shift;

        long num2, num;
        do {
            num = this.array.get(idx);

            if ((num & mask) != expected) return false;

            num2 = (num & ~mask) | valueToInject;
        } while (!this.array.compareAndSet(idx, num, num2));
        return true;
    }

    @VisibleForTesting
    public byte incrementAndGet(final int i) {
        byte old, newValue;
        do {
            old = get(i);
            newValue = (byte) (old + 1);
        } while (!compareAndSet(i, old, newValue));
        return newValue;
    }

    public int length() {
        return this.length;
    }

    private final AtomicReference<byte[]> cachedArray = new AtomicReference<>();

    public byte[] getCopy() {
        byte[] arr;
        if ((arr = cachedArray.get()) == null) {
            arr = new byte[this.length()];
            for (int idx = 0; idx < arr.length; idx++) {
                arr[idx] = this.get(idx);
            }
            cachedArray.set(arr);
        }
        return arr;
    }

    public void setAll0() {
        for (int idx = 0; idx < length; idx++) {
            set(idx, (byte) 0);
        }
    }
}
