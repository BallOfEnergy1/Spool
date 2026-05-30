package com.gamma.spool.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.minecraft.util.LongHashMap;
import net.minecraft.world.chunk.Chunk;

import org.jctools.maps.NonBlockingHashMapLong;

// I really *really* hate this class.
// TODO: Make this a generic class
public class NonBlockingMCLongHashMap extends LongHashMap implements ConcurrentMap<Long, Future<Chunk>> {

    NonBlockingHashMapLong<Future<Chunk>> map = new NonBlockingHashMapLong<>();

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    @Deprecated
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public boolean containsKey(long key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    @Deprecated
    public Future<Chunk> get(Object key) {
        return map.get(key);
    }

    public Future<Chunk> get(long key) {
        return map.get(key);
    }

    @Override
    @Deprecated
    public Future<Chunk> put(Long key, Future<Chunk> value) {
        return map.put(key, value);
    }

    public Future<Chunk> put(long key, Future<Chunk> value) {
        return map.put(key, value);
    }

    @Override
    @Deprecated
    public Future<Chunk> remove(Object key) {
        return map.remove(key);
    }

    @Override
    public Future<Chunk> remove(long key) {
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends Long, ? extends Future<Chunk>> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<Long> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<Future<Chunk>> values() {
        return map.values();
    }

    @Override
    public Set<Entry<Long, Future<Chunk>>> entrySet() {
        return map.entrySet();
    }

    @Override
    public Future<Chunk> putIfAbsent(Long key, Future<Chunk> value) {
        return map.putIfAbsent(key, value);
    }

    @Override
    @Deprecated
    public boolean remove(Object key, Object value) {
        return map.remove(key, value);
    }

    public boolean remove(long key, Future<Chunk> value) {
        return map.remove(key, value);
    }

    @Override
    @Deprecated
    public boolean replace(Long key, Future<Chunk> oldValue, Future<Chunk> newValue) {
        return map.replace(key, oldValue, newValue);
    }

    public boolean replace(long key, Future<Chunk> oldValue, Future<Chunk> newValue) {
        return map.replace(key, oldValue, newValue);
    }

    @Override
    @Deprecated
    public Future<Chunk> replace(Long key, Future<Chunk> value) {
        return map.replace(key, value);
    }

    public Future<Chunk> replace(long key, Future<Chunk> value) {
        return map.replace(key, value);
    }

    @Override
    public int getNumHashElements() {
        return this.size();
    }

    @Override
    public Chunk getValueByKey(long key) {
        Future<Chunk> future = this.get(key);
        if (future == null) return null;
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean containsItem(long key) {
        return this.containsKey(key);
    }

    @Override
    public void add(long key, Object value) {
        this.put(key, CompletableFuture.completedFuture((Chunk) value));
    }
}
