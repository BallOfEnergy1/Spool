package com.gamma.spool.util.fast.bytearray;

import com.gamma.spool.config.ImplConfig;
import com.gamma.spool.util.fast.Java8Util;

public class ByteArrayFactory {

    private static final boolean HAS_VARHANDLE = Java8Util.hasVarHandleSupport();

    public static FastAtomicByteArray create(int size) {
        if (HAS_VARHANDLE && ImplConfig.useJava9Features) {
            throw new UnsupportedOperationException("AtomicByteArray9 not implemented.");
            // return new AtomicByteArray9(size);
        }
        return new AtomicByteArray8(size);
    }
}
