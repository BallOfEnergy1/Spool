package com.gamma.spool.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.jetbrains.annotations.NotNull;

public class RWLockedList<T> implements ReadWriteLock, List<T> {

    private final ReadWriteLock lock;
    private final List<T> wrapped;

    public RWLockedList(ReadWriteLock lock, List<T> wrapped) {
        this.lock = lock;
        this.wrapped = wrapped;
    }

    public List<T> getWrapped() {
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
    public boolean addAll(int index, @NotNull Collection<? extends T> c) {
        lock.writeLock()
            .lock();
        try {
            return wrapped.addAll(index, c);
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

    @Override
    public T get(int index) {
        lock.readLock()
            .lock();
        try {
            return wrapped.get(index);
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @Override
    public T set(int index, T element) {
        lock.writeLock()
            .lock();
        try {
            return wrapped.set(index, element);
        } finally {
            lock.writeLock()
                .unlock();
        }
    }

    @Override
    public void add(int index, T element) {
        lock.writeLock()
            .lock();
        try {
            wrapped.add(index, element);
        } finally {
            lock.writeLock()
                .unlock();
        }
    }

    @Override
    public T remove(int index) {
        lock.writeLock()
            .lock();
        try {
            return wrapped.remove(index);
        } finally {
            lock.writeLock()
                .unlock();
        }
    }

    @Override
    public int indexOf(Object o) {
        lock.readLock()
            .lock();
        try {
            return wrapped.indexOf(o);
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @Override
    public int lastIndexOf(Object o) {
        lock.readLock()
            .lock();
        try {
            return wrapped.lastIndexOf(o);
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator() {
        lock.readLock()
            .lock();
        try {
            return wrapped.listIterator();
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator(int index) {
        lock.readLock()
            .lock();
        try {
            return wrapped.listIterator(index);
        } finally {
            lock.readLock()
                .unlock();
        }
    }

    @NotNull
    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        lock.readLock()
            .lock();
        try {
            return wrapped.subList(fromIndex, toIndex);
        } finally {
            lock.readLock()
                .unlock();
        }
    }
}
