package com.gamma.spool.util.concurrent.interfaces;

import com.gamma.spool.api.concurrent.IExternalThreadSafe;

public interface IThreadSafe {

    static boolean isConcurrent(Object obj) {
        return obj instanceof IThreadSafe || obj instanceof IExternalThreadSafe;
    }
}
