package com.gamma.spool.util;

import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.IntFunction;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.stream.LongStream;

import org.jetbrains.annotations.NotNull;

import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSpliterator;

public class RWLockedLongSet implements ReadWriteLock, LongSet {

    private final ReadWriteLock lock;
    private final LongSet wrapped;

    public RWLockedLongSet(ReadWriteLock lock, LongSet wrapped) {
        this.lock = lock;
        this.wrapped = wrapped;
    }

    public LongSet getWrapped() {
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
        lock.readLock()
            .lock();
        try {
            return wrapped.size();
        } finally {
            lock.readLock()
                .unlock();
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
    public boolean contains(long o) {
        lock.readLock()
            .lock();
        try {
            return wrapped.contains(o);
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @Override
    public long[] toLongArray() {
        lock.readLock()
            .lock();
        try {
            return wrapped.toLongArray();
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @Override
    public long[] toArray(long[] a) {
        lock.readLock()
            .lock();
        try {
            return wrapped.toArray(a);
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @Override
    public boolean addAll(LongCollection c) {
        lock.writeLock()
            .lock();
        try {
            return wrapped.addAll(c);
        } finally {
            lock.writeLock()
                .unlock();
        }
    }

    @Override
    public boolean containsAll(LongCollection c) {
        lock.readLock()
            .lock();
        try {
            return wrapped.containsAll(c);
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @Override
    public boolean removeAll(LongCollection c) {
        lock.writeLock()
            .lock();
        try {
            return wrapped.removeAll(c);
        } finally {
            lock.writeLock()
                .unlock();
        }
    }

    @NotNull
    @Override
    public LongIterator iterator() {
        lock.readLock()
            .lock();
        try {
            return wrapped.iterator();
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @Override
    public LongIterator longIterator() {
        lock.readLock()
            .lock();
        try {
            return wrapped.longIterator();
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @NotNull
    @Override
    public Object[] toArray() {
        lock.readLock()
            .lock();
        try {
            return wrapped.toArray();
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @NotNull
    @Override
    public <T1> T1[] toArray(@NotNull T1[] a) {
        lock.readLock()
            .lock();
        try {
            return wrapped.toArray(a);
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @Override
    public boolean add(long t) {
        lock.writeLock()
            .lock();
        try {
            return wrapped.add(t);
        } finally {
            lock.writeLock()
                .unlock();
        }
    }

    @Override
    public boolean remove(long o) {
        lock.writeLock()
            .lock();
        try {
            return wrapped.remove(o);
        } finally {
            lock.writeLock()
                .unlock();
        }
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        lock.readLock()
            .lock();
        try {
            return new LongOpenHashSet(wrapped).containsAll(c);
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends Long> c) {
        lock.writeLock()
            .lock();
        try {
            return wrapped.addAll(c);
        } finally {
            lock.writeLock()
                .unlock();
        }
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        lock.writeLock()
            .lock();
        try {
            return wrapped.removeAll(c);
        } finally {
            lock.writeLock()
                .unlock();
        }
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        lock.writeLock()
            .lock();
        try {
            return wrapped.retainAll(c);
        } finally {
            lock.writeLock()
                .unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock()
            .lock();
        try {
            wrapped.clear();
        } finally {
            lock.writeLock()
                .unlock();
        }
    }

    @NotNull
    @Override
    public LongSpliterator spliterator() {
        lock.readLock()
            .lock();
        try {
            return wrapped.spliterator();
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @Override
    public LongSpliterator longSpliterator() {
        lock.readLock()
            .lock();
        try {
            return wrapped.longSpliterator();
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @Override
    public <T1> T1[] toArray(@NotNull IntFunction<T1[]> generator) {
        lock.readLock()
            .lock();
        try {
            return wrapped.toArray(generator);
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @Override
    public boolean removeIf(@NotNull LongPredicate filter) {
        lock.writeLock()
            .lock();
        try {
            return wrapped.removeIf(filter);
        } finally {
            lock.writeLock()
                .unlock();
        }
    }

    @Override
    public boolean removeIf(it.unimi.dsi.fastutil.longs.LongPredicate filter) {
        lock.writeLock()
            .lock();
        try {
            return wrapped.removeIf(filter);
        } finally {
            lock.writeLock()
                .unlock();
        }
    }

    @Override
    public boolean retainAll(LongCollection c) {
        lock.writeLock()
            .lock();
        try {
            return wrapped.retainAll(c);
        } finally {
            lock.writeLock()
                .unlock();
        }
    }

    @NotNull
    @Override
    public LongStream longStream() {
        lock.readLock()
            .lock();
        try {
            return wrapped.longStream();
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @NotNull
    @Override
    public LongStream longParallelStream() {
        lock.readLock()
            .lock();
        try {
            return wrapped.longParallelStream();
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @Override
    public void forEach(LongConsumer action) {
        lock.readLock()
            .lock();
        try {
            wrapped.forEach(action);
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @Override
    public void forEach(it.unimi.dsi.fastutil.longs.LongConsumer action) {
        lock.readLock()
            .lock();
        try {
            wrapped.forEach(action);
        } finally {
            lock.readLock()
                .unlock();
        }
    }
}
