package com.gamma.spool.thread;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.gamma.spool.Spool;
import com.gamma.spool.SpoolException;

public class ForkThreadManager implements IResizableThreadManager {

    public ForkJoinPool pool;

    ForkJoinPool.ForkJoinWorkerThreadFactory namedThreadFactory;

    AtomicLong timeSpentExecuting = new AtomicLong();
    AtomicLong overhead = new AtomicLong();

    public long timeExecuting;
    public long timeWaiting;
    public long timeOverhead;

    /**
     * (Atomic)Boolean that holds the state of the thread manager. If this is true, then the pool is resizing.
     */
    AtomicBoolean isResizing = new AtomicBoolean(false);

    /**
     * Queue that holds the missed tasks that were added during the resizing process.
     */
    Queue<Runnable> resizingExecuteLaterQueue = new ConcurrentLinkedQueue<>();

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
    protected int threads;

    public ForkThreadManager(String name, int threads) {
        this.threads = threads;
        this.name = name;
        namedThreadFactory = pool -> new ForkJoinWorkerThread(pool) {

            {
                // Customize the thread name
                setName("Spool-" + name + "-" + getPoolIndex());
            }
        };
    }

    public void startPool() {
        pool = new ForkJoinPool(threads, namedThreadFactory, null, true);
    }

    public void terminatePool() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(
                (long) Spool.configManager.globalTerminatingSingleThreadTimeout / pool.getActiveThreadCount(),
                TimeUnit.SECONDS)) pool.shutdownNow();
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
        if (isResizing.get()) {
            resizingExecuteLaterQueue.add(task);
            return;
        }
        long time = 0;
        if (Spool.configManager.debug) time = System.nanoTime();
        pool.submit(() -> {
            long timeInternal = 0;
            if (Spool.configManager.debug) timeInternal = System.nanoTime();
            task.run();
            if (Spool.configManager.debug) timeSpentExecuting.addAndGet(System.nanoTime() - timeInternal);
        });
        if (Spool.configManager.debug) overhead.addAndGet(System.nanoTime() - time);
    }

    public void waitUntilAllTasksDone(boolean timeout) {
        long time = 0;
        if (Spool.configManager.debug) time = System.nanoTime();
        if (timeout) {
            if (!pool.awaitQuiescence(
                (long) Spool.configManager.globalRunningSingleThreadTimeout / pool.getActiveThreadCount(),
                TimeUnit.MILLISECONDS)) {
                Spool.logger.warn("Pool ({}) did not reach quiescence in time!", name);
            }
        } else {
            if (!pool.awaitQuiescence(
                Spool.configManager.globalTerminatingSingleThreadTimeout / pool.getActiveThreadCount(),
                TimeUnit.MILLISECONDS)) {
                Spool.logger.warn("Pool ({}) did not reach quiescence in time (termination)!", name);
            }
        }
        if (Spool.configManager.debug) {
            timeExecuting = timeSpentExecuting.getAndSet(0);
            timeOverhead = overhead.getAndSet(0);
            timeWaiting = System.nanoTime() - time;
        }
    }

    public void resize(int threads) {
        isResizing.set(true);
        this.terminatePool();
        this.threads = threads;
        this.startPool();
        isResizing.set(false);
        if (!resizingExecuteLaterQueue.isEmpty()) resizingExecuteLaterQueue.forEach(this::execute);
    }
}
