package com.gamma.spool.util.fast.bytearray;

import com.gamma.spool.util.fast.FastImpl;

public interface FastAtomicByteArray extends FastImpl {

    byte get(int idx);

    void set(int idx, byte value);

    boolean compareAndSet(int idx, byte expect, byte newValue);

    byte incrementAndGet(int idx);

    byte[] getCopy();

    void setAll0();

    int length();
}
