package com.gamma.spool.util.caching;

import com.gamma.spool.api.statistics.ICachedItem;
import com.gamma.spool.core.SpoolCoreMod;

public class CachedInt implements ICachedItem<Integer> {

    private int item;

    public CachedInt(int item) {
        this.item = item;
    }

    @Deprecated
    public Integer getItem() {
        return item;
    }

    public int getIntItem() {
        return item;
    }

    @Deprecated
    public void setItem(Integer item) {
        this.item = item;
    }

    public void setIntItem(int item) {
        this.item = item;
    }

    public long calculateSize() {
        return SpoolCoreMod.getRecursiveObjectSize(this.getIntItem());
    }
}
