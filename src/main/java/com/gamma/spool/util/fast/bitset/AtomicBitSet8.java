package com.gamma.spool.util.fast.bitset;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class AtomicBitSet8 implements FastAtomicBitSet {

    private static final class Cell {

        volatile int value; // 1 = true, 0 = false
    }

    private static final AtomicIntegerFieldUpdater<Cell> UPDATER = AtomicIntegerFieldUpdater
        .newUpdater(Cell.class, "value");

    private final Cell[] cells;

    private final int length;

    public AtomicBitSet8(int bitsLength) {
        cells = new Cell[length = bitsLength];
        for (int i = 0; i < bitsLength; i++) {
            cells[i] = new Cell();
        }
    }

    @Override
    public void set(int bitIndex) {
        cells[bitIndex].value = 1;
    }

    @Override
    public void clear(int bitIndex) {
        cells[bitIndex].value = 0;
    }

    @Override
    public void set(int bitIndex, boolean value) {
        cells[bitIndex].value = (value ? 1 : 0);
    }

    @Override
    public void flip(int bitIndex) {
        Cell c = cells[bitIndex];
        int prev;
        do {
            prev = c.value;
        } while (!UPDATER.compareAndSet(c, prev, prev ^ 1));
    }

    @Override
    public boolean get(int bitIndex) {
        return cells[bitIndex].value != 0;
    }

    @Override
    public int numSetBits() {
        int idx;
        int num;
        boolean value = get(idx = num = 0);

        while (true) {
            if (value) num++;
            if (idx++ == length) return num;
            value = get(idx);
        }
    }

    @Override
    public int numClearBits() {
        int idx;
        int num;
        boolean value = get(idx = num = 0);

        while (true) {
            if (!value) num++;
            if (idx++ == length) return num;
            value = get(idx);
        }
    }

    @Override
    public int length() {
        return length;
    }
}
