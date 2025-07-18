package com.gamma.spool.util.concurrent;

import java.util.concurrent.locks.StampedLock;

/**
 * Interface providing concurrency utilities based on a {@link StampedLock}.
 * <p>
 * The IConcurrent interface facilitates the management of read and write locks
 * and allows the use of optimistic read locks to enhance concurrent performance.
 * This interface is designed to support scenarios where thread-safe operations
 * are required with fine-grained control over locking mechanisms.
 */
public interface IConcurrent {

    StampedLock getLock();

    ThreadLocal<LockContext> lockContext = ThreadLocal.withInitial(LockContext::new);

    default void optimisticReadLock() {
        LockContext context = lockContext.get();
        if (context.readCount == 0) {
            context.readStamp = getLock().tryOptimisticRead(); // Optimistic reads!
            if (context.readStamp == 0) { // Lock failed, exclusively locked
                context.readStamp = getLock().readLock(); // Lock exclusively
            }
        }
        context.readCount++;
    }

    default boolean optimisticReadUnlock() {
        LockContext context = lockContext.get();
        if (context.readCount > 0) {
            context.readCount--;
            if (context.readCount == 0) {
                if (!getLock().validate(context.readStamp)) {
                    context.readStamp = getLock().readLock(); // Relock
                    return false; // Indicate that verification failed
                }
            }
        }
        return true;
    }

    default void readLock() {
        LockContext context = lockContext.get();
        if (context.writeCount > 0) { // If it's already exclusively write-locked.
            context.readCount++;
            return;
        }
        if (context.readCount == 0) {
            context.readStamp = getLock().readLock();
        }
        context.readCount++;
    }

    default void readUnlock() {
        LockContext context = lockContext.get();
        if (context.readCount > 0) {
            context.readCount--;
            if (context.writeCount > 0) { // If it's already exclusively write-locked.
                return;
            }
            if (context.readCount == 0) {
                getLock().unlockRead(context.readStamp);
            }
        }
    }

    default void writeLock() {
        LockContext context = lockContext.get();
        if (context.writeCount == 0) {
            context.writeStamp = getLock().writeLock();
        }
        context.writeCount++;
    }

    default void writeUnlock() {
        LockContext context = lockContext.get();
        if (context.writeCount > 0) {
            context.writeCount--;
            if (context.writeCount == 0) {
                getLock().unlockWrite(context.writeStamp);
            }
        }
    }

    class LockContext {

        long readStamp;
        long writeStamp;
        int readCount;
        int writeCount;
    }
}
