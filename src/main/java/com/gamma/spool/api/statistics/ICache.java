package com.gamma.spool.api.statistics;

import com.gamma.spool.util.caching.CachedItem;

public interface ICache {

    void invalidate();

    CachedItem<?>[] getCacheItems();

    @SuppressWarnings("SameReturnValue") // Purely because there's only one cache.
    String getNameForDebug();

    default long calculateSize() {
        long size = 0;
        for (CachedItem<?> cache : this.getCacheItems()) {
            if (cache == null) continue;
            size += cache.calculateSize();
        }
        return size;
    }
}
