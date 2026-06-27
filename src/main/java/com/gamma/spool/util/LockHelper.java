package com.gamma.spool.util;

import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public final class LockHelper {

    private static final Int2ObjectMap<ReadWriteLock> ACTIVE_CHUNK_SET = new Int2ObjectOpenHashMap<>();

    public static void readLockActiveChunkSet(int dim) {
        ACTIVE_CHUNK_SET.computeIfAbsent(dim, _ -> new ReentrantReadWriteLock(true))
            .readLock()
            .lock();
    }

    public static void readUnlockActiveChunkSet(int dim) {
        ACTIVE_CHUNK_SET.computeIfAbsent(dim, _ -> new ReentrantReadWriteLock(true))
            .readLock()
            .unlock();
    }

    public static void writeLockActiveChunkSet(int dim) {
        ACTIVE_CHUNK_SET.computeIfAbsent(dim, _ -> new ReentrantReadWriteLock(true))
            .writeLock()
            .lock();
    }

    public static void writeUnlockActiveChunkSet(int dim) {
        ACTIVE_CHUNK_SET.computeIfAbsent(dim, _ -> new ReentrantReadWriteLock(true))
            .writeLock()
            .unlock();
    }

    public static <T> RWLockedSet<T> createRWLockedActiveChunkSet(int dim, Set<T> wrapped) {
        return new RWLockedSet<>(ACTIVE_CHUNK_SET.computeIfAbsent(dim, _ -> new ReentrantReadWriteLock(true)), wrapped);
    }
}
