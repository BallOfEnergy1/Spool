package com.gamma.spool.thread;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.gamma.spool.SpoolLogger;
import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.config.ThreadManagerConfig;

/**
 * A thread manager implementation using a ForkJoinPool to manage a pool of threads.
 * This class implements the {@link IThreadManager} interface, enabling task
 * execution.
 * <p>
 * This class allows for efficiently executing tasks inside other tasks.
 */
public class ForkThreadManager implements IThreadManager {

    public ForkJoinPool pool;

    final ForkJoinPool.ForkJoinWorkerThreadFactory namedThreadFactory;

    final AtomicLong timeSpentExecuting = new AtomicLong();
    final AtomicLong overhead = new AtomicLong();

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
    protected final int threads;

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
        SpoolLogger.debug("Starting pool ({}) with {} threads.", this.getName(), this.threads);
        if (this.isStarted()) throw new IllegalStateException("Pool already started (" + this.getName() + ")!");
        pool = new ForkJoinPool(threads, namedThreadFactory, null, true);
    }

    public void terminatePool() {
        pool.shutdown();
        if (pool.getActiveThreadCount() == 0) {
            pool = null;
            return;
        }
        try {
            if (!pool.awaitTermination(
                (long) ThreadManagerConfig.globalTerminatingSingleThreadTimeout / pool.getPoolSize(),
                TimeUnit.SECONDS)) pool.shutdownNow();
        } catch (InterruptedException e) {
            throw new RuntimeException("Pool termination interrupted: " + e.getMessage());
        }
        pool = null;
    }

    public boolean isStarted() {
        return pool != null && !pool.isShutdown();
    }

    public void execute(Runnable task) {
        if (ThreadManagerConfig.betterTaskProfiling) task = ExecutionTasks.wrapTask(task);
        Runnable finalTask = task;
        if (DebugConfig.debug) {
            long time = System.nanoTime();
            pool.submit(() -> {
                long timeInternal = System.nanoTime();
                finalTask.run();
                timeSpentExecuting.addAndGet(System.nanoTime() - timeInternal);
            });
            overhead.addAndGet(System.nanoTime() - time);
        } else if (ThreadManagerConfig.useLambdaOptimization) {
            pool.submit(task);
        }
    }

    public <A> void execute(Consumer<A> task, A arg1) {
        if (ThreadManagerConfig.betterTaskProfiling) task = ExecutionTasks.wrapTask(task);
        Consumer<A> finalTask = task;
        if (DebugConfig.debug) {
            long time = System.nanoTime();
            pool.submit(() -> {
                long timeInternal = System.nanoTime();
                finalTask.accept(arg1);
                timeSpentExecuting.addAndGet(System.nanoTime() - timeInternal);
            });
            overhead.addAndGet(System.nanoTime() - time);
        } else {
            pool.submit(() -> finalTask.accept(arg1));
        }
    }

    public <A, B> void execute(BiConsumer<A, B> task, final A arg1, final B arg2) {
        if (ThreadManagerConfig.betterTaskProfiling) task = ExecutionTasks.wrapTask(task);
        BiConsumer<A, B> finalTask = task;
        if (DebugConfig.debug) {
            long time = System.nanoTime();
            pool.submit(() -> {
                long timeInternal = System.nanoTime();
                finalTask.accept(arg1, arg2);
                timeSpentExecuting.addAndGet(System.nanoTime() - timeInternal);
            });
            overhead.addAndGet(System.nanoTime() - time);
        } else {
            pool.submit(() -> finalTask.accept(arg1, arg2));
        }
    }

    private int updateCache = 0;

    public void waitUntilAllTasksDone(boolean timeout) {
        if (pool.getActiveThreadCount() == 0 && !pool.hasQueuedSubmissions()) return;
        long time = 0;
        if (DebugConfig.debug) time = System.nanoTime();
        if (timeout) {
            if (!pool.awaitQuiescence(
                (long) ThreadManagerConfig.globalRunningSingleThreadTimeout / pool.getPoolSize(),
                TimeUnit.MILLISECONDS)) {
                if (DebugConfig.debugLogging)
                    SpoolLogger.debugWarn("Pool ({}) did not reach quiescence in time!", name);
                else SpoolLogger.warnRateLimited("Pool ({}) did not reach quiescence in time!", name);
            }
        } else {
            if (!pool.awaitQuiescence(
                ThreadManagerConfig.globalTerminatingSingleThreadTimeout / pool.getPoolSize(),
                TimeUnit.MILLISECONDS)) {
                if (DebugConfig.debugLogging)
                    SpoolLogger.debugWarn("Pool ({}) did not reach quiescence in time (termination)!", name);
                else SpoolLogger.warnRateLimited("Pool ({}) did not reach quiescence in time (termination)!", name);
            }
        }
        if (pool.hasQueuedSubmissions()) {
            // This type of pool does not support clearing the task queue, meaning we just get to... not...
            // This is primarily because it uses the idea of Quiescence instead of a traditional futures queue.
            // In this manager, tasks will never be dropped unless terminating.
            if (DebugConfig.debugLogging) SpoolLogger.warn(
                "Pool ({}) overflowed {} updates, they will be executed whenever possible to avoid dropping updates.",
                name,
                pool.getQueuedSubmissionCount());
            else if (!SpoolLogger.warnRateLimited(
                "Pool ({}) overflowed {} updates, they will be executed whenever possible to avoid dropping updates.",
                name,
                pool.getQueuedSubmissionCount() + updateCache)) updateCache += pool.getQueuedSubmissionCount();

        }
        if (DebugConfig.debug) {
            timeExecuting = timeSpentExecuting.getAndSet(0);
            timeOverhead = overhead.getAndSet(0);
            timeWaiting = System.nanoTime() - time;
        }
    }
}
