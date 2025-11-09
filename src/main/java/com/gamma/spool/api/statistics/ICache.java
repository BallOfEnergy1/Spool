package com.gamma.spool.api.statistics;

public interface ICache {

    void invalidate();

    ICachedItem<?>[] getCacheItems();

    @SuppressWarnings("SameReturnValue") // Purely because there's only one cache.
    String getNameForDebug();

    default long calculateSize() {
        long size = 0;
        for (ICachedItem<?> cache : this.getCacheItems()) {
            if (cache == null) continue;
            size += cache.calculateSize();
        }
        return size;
    }
}
