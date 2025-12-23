package com.gamma.spool.util.fast.bitset;

import com.gamma.spool.config.ImplConfig;
import com.gamma.spool.util.fast.Java8Util;

public class BitSetFactory {

    private static final boolean HAS_VARHANDLE = Java8Util.hasVarHandleSupport();

    public static FastAtomicBitSet create(int size) {
        if (HAS_VARHANDLE && ImplConfig.useJava9Features) {
            return new AtomicBitSet9(size);
        }
        if (ImplConfig.useCompactImpls) {
            return new AtomicBitSetCompact(size);
        }
        return new AtomicBitSet8(size);
    }
}
