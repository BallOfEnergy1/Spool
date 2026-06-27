package com.gamma.spool.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.minecraft.world.World;

import org.jetbrains.annotations.NotNull;

/**
 * Sided lock implementation which can lock on either the client or the server, but not both.
 */
public class SidedLock implements ReadWriteLock {

    private static final Lock NOTHING = new DoNothingLock();

    private final Lock readLock;
    private final Lock writeLock;

    public SidedLock(World world) {
        this(new ReentrantReadWriteLock(true), world, false);
    }

    public SidedLock(ReadWriteLock lock, World world) {
        this(lock, world, false);
    }

    public SidedLock(ReadWriteLock lock, World world, boolean clientSide) {
        if (world == null) throw new NullPointerException("World cannot be null");
        if (lock == null) throw new NullPointerException("Lock cannot be null");
        // precompute all the stuff since a world cant change remotedness.
        boolean shouldLock = (world.isRemote == clientSide);
        readLock = shouldLock ? lock.readLock() : NOTHING;
        writeLock = shouldLock ? lock.writeLock() : NOTHING;
    }

    @NotNull
    @Override
    public Lock readLock() {
        return readLock;
    }

    @NotNull
    @Override
    public Lock writeLock() {
        return writeLock;
    }

    // This is a dummy lock for when we're not on a side that can lock.
    private static class DoNothingLock implements Lock {

        @Override
        public void lock() {}

        @Override
        public void lockInterruptibly() {}

        @Override
        public boolean tryLock() {
            return true;
        }

        @Override
        public boolean tryLock(long time, @NotNull TimeUnit unit) {
            return true;
        }

        @Override
        public void unlock() {}

        @NotNull
        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }
}
