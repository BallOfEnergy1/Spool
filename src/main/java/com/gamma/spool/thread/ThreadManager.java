package com.gamma.spool.thread;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.gamma.spool.Spool;
import com.gamma.spool.SpoolException;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class ThreadManager implements IResizableThreadManager {

    public ThreadPoolExecutor pool;

    public Queue<Future<?>> futures = new ConcurrentLinkedDeque<>();

    ThreadFactory namedThreadFactory;

    AtomicLong timeSpentExecuting = new AtomicLong();
    AtomicLong overhead = new AtomicLong();

    private final String name;

    public long timeExecuting;
    public long timeWaiting;
    public long timeOverhead;
    public int futuresSize;
    public int overflowSize;

    /**
     * (Atomic)Boolean that holds the state of the thread manager. If this is true, then the pool is resizing.
     */
    AtomicBoolean isResizing = new AtomicBoolean(false);

    /**
     * Queue that holds the missed tasks that were added during the resizing process.
     */
    Queue<Runnable> resizingExecuteLaterQueue = new ConcurrentLinkedQueue<>();

    @Override
    public String getName() {
        return name;
    }

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

    // List of tasks that weren't completed to ensure that they eventually do get finished.
    public Queue<Runnable> toExecuteLater = new ConcurrentLinkedQueue<>();

    protected int threads;

    public ThreadManager(String name, int threads) {
        this.threads = threads;
        this.name = name;
        namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("Spool-" + name + "-%d")
            .build();
    }

    public void startPool() {
        pool = new ThreadPoolExecutor(
            threads,
            threads,
            50,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(16384, true),
            namedThreadFactory);
        pool.prestartCoreThread();
    }

    public void terminatePool() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(
                (long) Spool.configManager.globalTerminatingSingleThreadTimeout / pool.getActiveCount(),
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
        try {
            long time = 0;
            if (Spool.configManager.debug) time = System.nanoTime();
            futures.add(pool.submit(() -> {
                long timeInternal = 0;
                if (Spool.configManager.debug) timeInternal = System.nanoTime();
                task.run();
                if (Spool.configManager.debug) timeSpentExecuting.addAndGet(System.nanoTime() - timeInternal);
            }));
            if (Spool.configManager.debug) overhead.addAndGet(System.nanoTime() - time);
        } catch (RejectedExecutionException e) {
            toExecuteLater.add(task);
        }
    }

    public void waitUntilAllTasksDone(boolean timeout) {
        boolean failed = false;
        long timeSpentWaiting = 0;

        int timeoutTime;
        if (timeout) timeoutTime = Spool.configManager.globalRunningSingleThreadTimeout / pool.getActiveCount(); // milliseconds
        else timeoutTime = Spool.configManager.globalTerminatingSingleThreadTimeout / pool.getActiveCount(); // milliseconds

        if (Spool.configManager.debug) {
            futuresSize = futures.size();
        }

        try {
            List<Runnable> runTasks = new ArrayList<>();
            try {
                for (Runnable runnable : toExecuteLater) {
                    futures.add(pool.submit(runnable));
                    runTasks.add(runnable);
                }
            } catch (RejectedExecutionException e) {
                failed = true;
                toExecuteLater.removeAll(runTasks);
            }
            Iterator<Future<?>> iterator = futures.iterator();
            while (iterator.hasNext()) {
                Future<?> future = iterator.next();
                long time = System.nanoTime();
                try {
                    future.get(Math.max(timeoutTime - timeSpentWaiting, 0), TimeUnit.MILLISECONDS);
                    iterator.remove();
                } catch (TimeoutException e) {
                    Spool.logger.warn("Pool ({}) did not finish all tasks in time!", name);
                    // THIS WILL DROP UPDATES LATER ON
                    // TODO: make this NOT do that lmao
                    break;
                }
                timeSpentWaiting += TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new SpoolException(e.getMessage());
        }

        if (!failed) toExecuteLater.clear();
        if (!futures.isEmpty()) {
            Spool.logger.warn("Pool ({}) discarding {} updates.", name, futures.size()); // TODO: see other TODO above
                                                                                         // this
            futures.clear();
        }
        if (Spool.configManager.debug) {
            overflowSize = toExecuteLater.size();
            timeExecuting = timeSpentExecuting.getAndSet(0);
            timeOverhead = overhead.getAndSet(0);
            timeWaiting = TimeUnit.NANOSECONDS.convert(timeSpentWaiting, TimeUnit.MILLISECONDS);;
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
