package com.gamma.spool.util.concurrent;

import net.minecraft.util.IntHashMap;

public class ConcurrentIntHashMap extends IntHashMap {

    @Override
    public void addKey(int p_76038_1_, Object p_76038_2_) {
        synchronized (this) {
            super.addKey(p_76038_1_, p_76038_2_);
        }
    }

    @Override
    public void clearMap() {
        synchronized (this) {
            super.clearMap();
        }
    }

    @Override
    public boolean containsItem(int p_76037_1_) {
        synchronized (this) {
            return super.containsItem(p_76037_1_);
        }
    }

    @Override
    public Object removeObject(int p_76049_1_) {
        synchronized (this) {
            return super.removeObject(p_76049_1_);
        }
    }

    @Override
    public Object lookup(int p_76041_1_) {
        synchronized (this) {
            return super.lookup(p_76041_1_);
        }
    }
}
