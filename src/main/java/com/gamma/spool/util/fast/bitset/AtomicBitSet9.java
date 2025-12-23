package com.gamma.spool.util.fast.bitset;

public class AtomicBitSet9 implements FastAtomicBitSet {

    static final class Cell {

        public int value;
    }

    private final Cell[] cells;

    private final int length;

    public AtomicBitSet9(int bitsLength) {
        cells = new Cell[length = bitsLength];
        for (int idx = 0; idx < bitsLength; idx++) {
            cells[idx] = new Cell();
        }
    }

    @Override
    public boolean get(int bitIndex) {
        return VarHandleSupport.getOpaque(cells[bitIndex]) != 0;
    }

    @Override
    public void set(int bitIndex, boolean v) {
        VarHandleSupport.setRelease(cells[bitIndex], v ? 1 : 0);
    }

    @Override
    public void set(int bitIndex) {
        VarHandleSupport.setRelease(cells[bitIndex], 1);
    }

    @Override
    public void clear(int bitIndex) {
        VarHandleSupport.setRelease(cells[bitIndex], 0);
    }

    @Override
    public void flip(int bitIndex) {
        int prev;
        do {
            prev = VarHandleSupport.getOpaque(cells[bitIndex]);
        } while (!VarHandleSupport.compareAndSet(cells[bitIndex], prev, prev ^ 1));
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
