package com.gamma.spool.util.concurrent.interfaces;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Interface providing concurrency utilities based on a {@link ReentrantReadWriteLock}.
 */
public interface IConcurrent extends ILockUser {

    ReentrantReadWriteLock getLock();

    @Override
    default void readLock() {
        getLock().readLock()
            .lock();
    }

    @Override
    default void readUnlock() {
        getLock().readLock()
            .unlock();
    }

    @Override
    default void writeLock() {
        getLock().writeLock()
            .lock();
    }

    @Override
    default void writeUnlock() {
        getLock().writeLock()
            .unlock();
    }
}
