package com.gamma.spool.util.fast.bitset;

import com.gamma.spool.util.fast.FastImpl;

public interface FastAtomicBitSet extends FastImpl {

    boolean get(int bitIndex);

    void set(int bitIndex, boolean value);

    void set(int bitIndex);

    void clear(int bitIndex);

    void flip(int bitIndex);

    int numClearBits();

    int numSetBits();

    int length();
}
