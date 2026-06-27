package com.gamma.spool.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

public class RWLockedSet<T> implements ReadWriteLock, Set<T> {

    private final ReadWriteLock lock;
    private final Set<T> wrapped;

    public RWLockedSet(ReadWriteLock lock, Set<T> wrapped) {
        this.lock = lock;
        this.wrapped = wrapped;
    }

    public Set<T> getWrapped() {
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
    public boolean contains(Object o) {
        lock.readLock()
            .lock();
        try {
            return wrapped.contains(o);
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        lock.readLock()
            .lock();
        try {
            return wrapped.iterator();
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
    public boolean add(T t) {
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
    public boolean remove(Object o) {
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
            return new HashSet<>(wrapped).containsAll(c);
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> c) {
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
    public Spliterator<T> spliterator() {
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
    public boolean removeIf(@NotNull Predicate<? super T> filter) {
        lock.writeLock()
            .lock();
        try {
            return wrapped.removeIf(filter);
        } finally {
            lock.writeLock()
                .unlock();
        }
    }

    @NotNull
    @Override
    public Stream<T> stream() {
        lock.readLock()
            .lock();
        try {
            return wrapped.stream();
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @NotNull
    @Override
    public Stream<T> parallelStream() {
        lock.readLock()
            .lock();
        try {
            return wrapped.parallelStream();
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @Override
    public void forEach(Consumer<? super T> action) {
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
