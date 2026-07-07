package com.gamma.spool.util;

import java.util.Map;
import java.util.concurrent.locks.StampedLock;

import org.jetbrains.annotations.NotNull;

import it.unimi.dsi.fastutil.longs.AbstractLong2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectSet;

// I HATE LITERALLY EVERYTHING
public class StampedRWLockedLong2ObjectMap<T> extends AbstractLong2ObjectFunction<T> implements Long2ObjectMap<T> {

    private final StampedLock lock;
    private final Long2ObjectMap<T> wrapped;

    public StampedRWLockedLong2ObjectMap(StampedLock lock, Long2ObjectMap<T> wrapped) {
        this.lock = lock;
        this.wrapped = wrapped;
    }

    @Override
    public int size() {
        long stamp = lock.tryOptimisticRead();
        int size;
        try {
            size = wrapped.size();
        } finally {
            if (!lock.validate(stamp)) {
                stamp = lock.readLock();
                try {
                    size = wrapped.size();
                } finally {
                    lock.unlockRead(stamp);
                }
            }
        }
        return size;
    }

    @Override
    public ObjectSet<Entry<T>> long2ObjectEntrySet() {
        long stamp = lock.tryOptimisticRead();
        ObjectSet<Entry<T>> set;
        try {
            set = wrapped.long2ObjectEntrySet();
        } finally {
            if (!lock.validate(stamp)) {
                stamp = lock.readLock();
                try {
                    set = wrapped.long2ObjectEntrySet();
                } finally {
                    lock.unlockRead(stamp);
                }
            }
        }
        return set;
    }

    @NotNull
    @Override
    public LongSet keySet() {
        long stamp = lock.tryOptimisticRead();
        LongSet set;
        try {
            set = wrapped.keySet();
        } finally {
            if (!lock.validate(stamp)) {
                stamp = lock.readLock();
                try {
                    set = wrapped.keySet();
                } finally {
                    lock.unlockRead(stamp);
                }
            }
        }
        return set;
    }

    @NotNull
    @Override
    public ObjectCollection<T> values() {
        long stamp = lock.tryOptimisticRead();
        ObjectCollection<T> collection;
        try {
            collection = wrapped.values();
        } finally {
            if (!lock.validate(stamp)) {
                stamp = lock.readLock();
                try {
                    collection = wrapped.values();
                } finally {
                    lock.unlockRead(stamp);
                }
            }
        }
        return collection;
    }

    @Override
    public T get(long key) {
        long stamp = lock.tryOptimisticRead();
        T value;
        try {
            value = wrapped.get(key);
        } finally {
            if (!lock.validate(stamp)) {
                stamp = lock.readLock();
                try {
                    value = wrapped.get(key);
                } finally {
                    lock.unlockRead(stamp);
                }
            }
        }
        return value;
    }

    @Override
    public boolean containsKey(long key) {
        long stamp = lock.tryOptimisticRead();
        boolean value;
        try {
            value = wrapped.containsKey(key);
        } finally {
            if (!lock.validate(stamp)) {
                stamp = lock.readLock();
                try {
                    value = wrapped.containsKey(key);
                } finally {
                    lock.unlockRead(stamp);
                }
            }
        }
        return value;
    }

    @Override
    public boolean isEmpty() {
        long stamp = lock.tryOptimisticRead();
        boolean value;
        try {
            value = wrapped.isEmpty();
        } finally {
            if (!lock.validate(stamp)) {
                stamp = lock.readLock();
                try {
                    value = wrapped.isEmpty();
                } finally {
                    lock.unlockRead(stamp);
                }
            }
        }
        return value;
    }

    @Override
    public boolean containsValue(Object value) {
        long stamp = lock.tryOptimisticRead();
        boolean contains;
        try {
            contains = wrapped.containsValue(value);
        } finally {
            if (!lock.validate(stamp)) {
                stamp = lock.readLock();
                try {
                    contains = wrapped.containsValue(value);
                } finally {
                    lock.unlockRead(stamp);
                }
            }
        }
        return contains;
    }

    @Override
    public void putAll(@NotNull Map<? extends Long, ? extends T> m) {
        long stamp = lock.writeLock();
        try {
            wrapped.putAll(m);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public T put(long key, T value) {
        long stamp = lock.writeLock();
        try {
            return wrapped.put(key, value);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public T remove(long key) {
        long stamp = lock.writeLock();
        try {
            return wrapped.remove(key);
        } finally {
            lock.unlockWrite(stamp);
        }
    }
}
