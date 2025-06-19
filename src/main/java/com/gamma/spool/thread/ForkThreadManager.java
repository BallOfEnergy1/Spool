package com.gamma.spool.thread;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.gamma.spool.Spool;
import com.gamma.spool.SpoolException;

public class ForkThreadManager implements IThreadManager {

    public ForkJoinPool pool;

    ForkJoinPool.ForkJoinWorkerThreadFactory namedThreadFactory;

    AtomicLong timeSpentExecuting = new AtomicLong();
    AtomicLong timeSpentWaiting = new AtomicLong();
    AtomicLong overhead = new AtomicLong();

    public long timeExecuting;
    public long timeWaiting;
    public long timeOverhead;

    @Override
    public int getNumThreads() {
        return pool.getPoolSize();
    }

    @Override
    public long getTimeExecuting() {
        return timeExecuting;
    }

    @Override
    public long getTimeOverhead() {
        return timeOverhead;
    }

    @Override
    public long getTimeWaiting() {
        return timeWaiting;
    }

    @Override
    public String getName() {
        return name;
    }

    private final String name;
    private final int threads;

    public ForkThreadManager(String name, int threads) {
        this.threads = threads;
        this.name = name;
        namedThreadFactory = pool -> new ForkJoinWorkerThread(pool) {

            {
                // Customize the thread name
                setName(name + "-" + getPoolIndex());
            }
        };
    }

    public void startPool() {
        pool = new ForkJoinPool(threads, namedThreadFactory, null, true);
    }

    public void terminatePool() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(1, TimeUnit.SECONDS)) pool.shutdownNow();
        } catch (InterruptedException e) {
            throw new SpoolException("Pool termination interrupted: " + e.getMessage());
        }
        pool = null;
    }

    public boolean isStarted() {
        return pool != null && !pool.isShutdown();
    }

    public void startPoolIfNeeded() {
        if (!this.isStarted()) this.startPool();
    }

    public void execute(Runnable task) {
        long time;
        if (Spool.debug) time = System.nanoTime();
        pool.submit(() -> {
            long timeInternal;
            if (Spool.debug) timeInternal = System.nanoTime();
            task.run();
            if (Spool.debug) timeSpentExecuting.addAndGet(System.nanoTime() - timeInternal);
        });
        if (Spool.debug) overhead.addAndGet(System.nanoTime() - time);
    }

    public void waitUntilAllTasksDone(boolean timeout) {
        long time;
        if (Spool.debug) time = System.nanoTime();
        if (timeout) {
            if (!pool.awaitQuiescence(250, TimeUnit.MILLISECONDS)) {
                Spool.logger.warn("Pool ({}) did not reach quiescence in time!", name);
            }
        } else {
            if (!pool.awaitQuiescence(60, TimeUnit.SECONDS)) { // 60 seconds should be plenty in order for everything to
                                                               // finish...
                throw new SpoolException("Pool (" + name + ") hung (failed 60s timeout).");
            }
        }
        if (Spool.debug) {
            timeSpentWaiting.set(System.nanoTime() - time);
            timeExecuting = timeSpentExecuting.getAndSet(0);
            timeOverhead = overhead.getAndSet(0);
            timeWaiting = timeSpentWaiting.getAndSet(0);
        }
    }
}
