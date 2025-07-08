package com.gamma.spool.util.caching;

import com.gamma.spool.SpoolCoreMod;

public class CachedItem<T> {

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
