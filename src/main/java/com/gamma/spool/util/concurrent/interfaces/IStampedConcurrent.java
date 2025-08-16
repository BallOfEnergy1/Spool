package com.gamma.spool.util.concurrent.interfaces;

import java.util.concurrent.locks.StampedLock;

/**
 * Interface providing concurrency utilities based on a {@link StampedLock}.
 */
public interface IStampedConcurrent extends IThreadSafe {

    StampedLock getLock();

    default long readLock() {
        return getLock().readLock();
    }

    default void readUnlock(long stamp) {
        getLock().unlockRead(stamp);
    }

    default boolean isReadLocked() {
        return getLock().isReadLocked();
    }

    default long writeLock() {
        return getLock().writeLock();
    }

    default void writeUnlock(long stamp) {
        getLock().unlockWrite(stamp);
    }

    default boolean isWriteLocked() {
        return getLock().isWriteLocked();
    }

    default boolean isLocked() {
        return isReadLocked() || isWriteLocked();
    }

    default long convertToOptimisticReadLock(long stamp) {
        stamp = getLock().tryConvertToOptimisticRead(stamp);
        if (!getLock().validate(stamp)) {
            throw new IllegalStateException("Failed to convert to optimistic read lock; stamp invalid.");
        }
        return stamp;
    }

    default long convertToReadLock(long stamp) {
        stamp = getLock().tryConvertToReadLock(stamp);
        if (!getLock().validate(stamp)) {
            throw new IllegalStateException("Failed to convert to read lock; stamp invalid.");
        }
        return stamp;
    }

    default long convertToWriteLock(long stamp) {
        stamp = getLock().tryConvertToWriteLock(stamp);
        if (!getLock().validate(stamp)) {
            getLock().tryUnlockRead();
            stamp = getLock().writeLock();
        }
        return stamp;
    }

    default long optimisticReadLock() {
        return getLock().tryOptimisticRead();
    }

    default long optimisticReadUnlock(long stamp) {
        if (!getLock().validate(stamp)) {
            return getLock().readLock();
        }
        return 0;
    }
}
