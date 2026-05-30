package com.gamma.spool.util;

import net.minecraft.util.LongHashMap;

import org.jctools.maps.NonBlockingHashMapLong;

public class ConcurrentLongHashMap extends LongHashMap {

    private final NonBlockingHashMapLong<Object> map = new NonBlockingHashMapLong<>();

    public ConcurrentLongHashMap() {
        super();
    }

    @Override
    public int getNumHashElements() {
        return map.size();
    }

    @Override
    public Object getValueByKey(long key) {
        return map.get(key);
    }

    @Override
    public boolean containsItem(long key) {
        return map.containsKey(key);
    }

    @Override
    public void add(long key, Object value) {
        map.put(key, value);
    }

    @Override
    public Object remove(long key) {
        return map.remove(key);
    }
}
