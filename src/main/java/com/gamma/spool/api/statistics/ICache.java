package com.gamma.spool.api.statistics;

public interface ICache {

    void invalidate(boolean lifecycle);

    ICachedItem<?>[] getCacheItems();

    int getCount();

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
