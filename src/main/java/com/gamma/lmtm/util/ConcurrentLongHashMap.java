package com.gamma.lmtm.util;

import net.minecraft.util.LongHashMap;

public class ConcurrentLongHashMap extends LongHashMap {

    @Override
    public void add(long p_76163_1_, Object p_76163_3_) {
        synchronized (this) {
            super.add(p_76163_1_, p_76163_3_);
        }
    }

    @Override
    public int getNumHashElements() {
        synchronized (this) {
            return super.getNumHashElements();
        }
    }

    @Override
    public Object getValueByKey(long p_76164_1_) {
        synchronized (this) {
            return super.getValueByKey(p_76164_1_);
        }
    }

    @Override
    public Object remove(long p_76159_1_) {
        synchronized (this) {
            return super.remove(p_76159_1_);
        }
    }
}
