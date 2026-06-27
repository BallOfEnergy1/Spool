package com.gamma.spool.util;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.jetbrains.annotations.NotNull;

import it.unimi.dsi.fastutil.longs.AbstractLong2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectSet;

// I HATE LITERALLY EVERYTHING
public class RWLockedLong2ObjectMap<T> extends AbstractLong2ObjectFunction<T> implements ReadWriteLock, Long2ObjectMap<T> {

    private final ReadWriteLock lock;
    private final Long2ObjectMap<T> wrapped;

    public RWLockedLong2ObjectMap(ReadWriteLock lock, Long2ObjectMap<T> wrapped) {
        this.lock = lock;
        this.wrapped = wrapped;
    }

    public Long2ObjectMap<T> getWrapped() {
        return wrapped;
    }

    public Lock readLock() {
        return lock.readLock();
    }

    public Lock writeLock() {
        return lock.writeLock();
    }

    @Override
    public int size() {
        readLock().lock();
        try {
            return wrapped.size();
        } finally {
            readLock().unlock();
        }
    }

    @Override
    public ObjectSet<Entry<T>> long2ObjectEntrySet() {
        readLock().lock();
        try {
            return wrapped.long2ObjectEntrySet();
        } finally {
            readLock().unlock();
        }
    }

    @NotNull
    @Override
    public LongSet keySet() {
        readLock().lock();
        try {
            return wrapped.keySet();
        } finally {
            readLock().unlock();
        }
    }

    @NotNull
    @Override
    public ObjectCollection<T> values() {
        readLock().lock();
        try {
            return wrapped.values();
        } finally {
            readLock().unlock();
        }
    }

    @Override
    public T get(long key) {
        readLock().lock();
        try {
            return wrapped.get(key);
        } finally {
            readLock().unlock();
        }
    }

    @Override
    public boolean containsKey(long key) {
        readLock().lock();
        try {
            return wrapped.containsKey(key);
        } finally {
            readLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        lock.readLock()
            .lock();
        try {
            return wrapped.isEmpty();
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @Override
    public boolean containsValue(Object value) {
        readLock().lock();
        try {
            return wrapped.containsValue(value);
        } finally {
            readLock().unlock();
        }
    }

    @Override
    public void putAll(@NotNull Map<? extends Long, ? extends T> m) {
        writeLock().lock();
        try {
            wrapped.putAll(m);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public T put(long key, T value) {
        writeLock().lock();
        try {
            return wrapped.put(key, value);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public T remove(long key) {
        writeLock().lock();
        try {
            return wrapped.remove(key);
        } finally {
            writeLock().unlock();
        }
    }
}
