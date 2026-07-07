package com.gamma.spool.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.StampedLock;

import net.minecraft.util.LongHashMap;

import org.jetbrains.annotations.NotNull;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
// import org.jctools.maps.NonBlockingHashMapLong;

public class MCLongHashMap<T> extends LongHashMap implements ConcurrentMap<Long, T> {

    // TODO: Profile each of these in both high and low-load environments.
    // NonBlockingHashMapLong<T> map = new NonBlockingHashMapLong<>();
    // private final Long2ObjectMap<T> map = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());
    private final Long2ObjectMap<T> map = new StampedRWLockedLong2ObjectMap<>(
        new StampedLock(),
        new Long2ObjectOpenHashMap<>());

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
    public T get(Object key) {
        return map.get(key);
    }

    public T get(long key) {
        return map.get(key);
    }

    @Override
    @Deprecated
    public T put(Long key, T value) {
        return map.put(key, value);
    }

    public T put(long key, T value) {
        return map.put(key, value);
    }

    @Override
    @Deprecated
    public T remove(Object key) {
        return map.remove(key);
    }

    @Override
    public T remove(long key) {
        return map.remove(key);
    }

    @Override
    public void putAll(@NotNull Map<? extends Long, ? extends T> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @NotNull
    @Override
    public Set<Long> keySet() {
        return map.keySet();
    }

    @NotNull
    @Override
    public Collection<T> values() {
        return map.values();
    }

    @NotNull
    @Override
    public Set<Entry<Long, T>> entrySet() {
        // noinspection deprecation
        return map.entrySet();
    }

    @Override
    public T putIfAbsent(@NotNull Long key, T value) {
        return map.putIfAbsent(key, value);
    }

    @Override
    @Deprecated
    public boolean remove(@NotNull Object key, Object value) {
        return map.remove(key, value);
    }

    public boolean remove(long key, T value) {
        return map.remove(key, value);
    }

    @Override
    @Deprecated
    public boolean replace(@NotNull Long key, @NotNull T oldValue, @NotNull T newValue) {
        return map.replace(key, oldValue, newValue);
    }

    public boolean replace(long key, T oldValue, T newValue) {
        return map.replace(key, oldValue, newValue);
    }

    @Override
    @Deprecated
    public T replace(@NotNull Long key, @NotNull T value) {
        return map.replace(key, value);
    }

    public T replace(long key, T value) {
        return map.replace(key, value);
    }

    @Override
    public int getNumHashElements() {
        return this.size();
    }

    @Override
    public T getValueByKey(long key) {
        return this.get(key);
    }

    @Override
    public boolean containsItem(long key) {
        return this.containsKey(key);
    }

    @Override
    public void add(long key, Object value) {
        // noinspection unchecked
        this.put(key, (T) value);
    }
}
