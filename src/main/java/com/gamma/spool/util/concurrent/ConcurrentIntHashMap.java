package com.gamma.spool.util.concurrent;

import net.minecraft.util.IntHashMap;

import org.jctools.maps.NonBlockingHashMapLong;

public class ConcurrentIntHashMap extends IntHashMap {

    // Yes, this does use more space than it needs. Do I really care? Not really.
    private final NonBlockingHashMapLong<Object> map = new NonBlockingHashMapLong<>();

    public ConcurrentIntHashMap() {
        super();
    }

    @Override
    public void addKey(int p_76038_1_, Object p_76038_2_) {
        map.put(p_76038_1_, p_76038_2_);
    }

    @Override
    public void clearMap() {
        map.clear();
    }

    @Override
    public boolean containsItem(int p_76037_1_) {
        return map.containsKey(p_76037_1_);
    }

    @Override
    public Object removeObject(int p_76049_1_) {
        return map.remove(p_76049_1_);
    }

    @Override
    public Object lookup(int p_76041_1_) {
        return map.get(p_76041_1_);
    }
}
