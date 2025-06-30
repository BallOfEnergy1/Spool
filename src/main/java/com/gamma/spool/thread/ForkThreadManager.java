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
import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.config.ThreadManagerConfig;

/**
 * A thread manager implementation using a ForkJoinPool to manage a pool of threads.
 * This class implements the {@link IResizableThreadManager} interface, enabling both task
 * execution and safe resizing of the pool.
 * <p>
 * This class allows for efficiently executing tasks inside other tasks.
 */
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
        if (DebugConfig.debugLogging)
            Spool.logger.info("Starting pool ({}) with {} threads.", this.getName(), this.getNumThreads());
        if (this.isStarted()) throw new SpoolException("Pool already started (" + this.getName() + ")!");
        pool = new ForkJoinPool(threads, namedThreadFactory, null, true);
    }

    public void terminatePool() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(
                (long) ThreadManagerConfig.globalTerminatingSingleThreadTimeout / pool.getActiveThreadCount(),
                TimeUnit.SECONDS)) pool.shutdownNow();
        } catch (InterruptedException e) {
            throw new SpoolException("Pool termination interrupted: " + e.getMessage());
        }
        pool = null;
    }

    public boolean isStarted() {
        return pool != null && !pool.isShutdown();
    }

    public void execute(Runnable task) {
        if (isResizing.get()) {
            resizingExecuteLaterQueue.add(task);
            return;
        }
        long time = 0;
        if (DebugConfig.debug) time = System.nanoTime();
        pool.submit(() -> {
            long timeInternal = 0;
            if (DebugConfig.debug) timeInternal = System.nanoTime();
            task.run();
            if (DebugConfig.debug) timeSpentExecuting.addAndGet(System.nanoTime() - timeInternal);
        });
        if (DebugConfig.debug) overhead.addAndGet(System.nanoTime() - time);
    }

    public void waitUntilAllTasksDone(boolean timeout) {
        long time = 0;
        if (DebugConfig.debug) time = System.nanoTime();
        if (timeout) {
            if (!pool.awaitQuiescence(
                (long) ThreadManagerConfig.globalRunningSingleThreadTimeout / pool.getActiveThreadCount(),
                TimeUnit.MILLISECONDS)) {
                Spool.logger.warn("Pool ({}) did not reach quiescence in time!", name);
            }
        } else {
            if (!pool.awaitQuiescence(
                ThreadManagerConfig.globalTerminatingSingleThreadTimeout / pool.getActiveThreadCount(),
                TimeUnit.MILLISECONDS)) {
                Spool.logger.warn("Pool ({}) did not reach quiescence in time (termination)!", name);
            }
        }
        if (pool.hasQueuedSubmissions()) {
            // This type of pool does not support clearing the task queue, meaning we just get to... not...
            // This is primarily because it uses the idea of Quiescence instead of a traditional futures queue.
            // In this manager, tasks will never be dropped unless terminating.
            Spool.logger.warn(
                "Pool ({}) overflowed {} updates, they will be executed whenever possible to avoid dropping updates.",
                name,
                pool.getQueuedSubmissionCount());
        }
        if (DebugConfig.debug) {
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
