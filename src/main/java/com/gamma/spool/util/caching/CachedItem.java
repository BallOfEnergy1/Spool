package com.gamma.spool.util.caching;

import com.gamma.spool.SpoolCoreMod;
import com.gamma.spool.api.statistics.ICachedItem;

public class CachedItem<T> implements ICachedItem<T> {

    private T item;

    public CachedItem(T item) {
        this.item = item;
    }

    public T getItem() {
        return item;
    }

    public void setItem(T item) {
        this.item = item;
    }

    public long calculateSize() {
        return SpoolCoreMod.getRecursiveObjectSize(this.getItem());
    }
}
