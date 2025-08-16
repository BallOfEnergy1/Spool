package com.gamma.spool.util.concurrent.interfaces;

public interface IThreadSafe {

    static boolean isConcurrent(Object obj) {
        return obj instanceof IThreadSafe;
    }
}
