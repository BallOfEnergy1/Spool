package com.gamma.spool.util.caching;

public interface ICache {

    void invalidate();

    CachedItem<?>[] getCachesForDebug();

    String getNameForDebug();

    default long calculateSize() {
        long size = 0;
        for (CachedItem<?> cache : this.getCachesForDebug()) {
            if (cache == null) continue;
            size += cache.calculateSize();
        }
        return size;
    }
}
