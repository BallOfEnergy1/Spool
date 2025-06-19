package com.gamma.spool.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.gamma.spool.SpoolException;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class ThreadManager implements IThreadManager {

    public ThreadPoolExecutor pool;

    public Queue<Future<?>> futures = new ConcurrentLinkedDeque<>();

    ThreadFactory namedThreadFactory;

    AtomicLong timeSpentExecuting = new AtomicLong();
    AtomicLong timeSpentWaiting = new AtomicLong();
    AtomicLong overhead = new AtomicLong();

    private final String name;

    public long timeExecuting;
    public long timeWaiting;
    public long timeOverhead;
    public int futuresSize;
    public int overflowSize;

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
    public List<Runnable> toExecuteLater = new ArrayList<>();

    private final int threads;

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
        try {
            long time = System.nanoTime();
            futures.add(pool.submit(() -> {
                long timeInternal = System.nanoTime();
                task.run();
                timeSpentExecuting.addAndGet(System.nanoTime() - timeInternal);
            }));
            overhead.addAndGet(System.nanoTime() - time);
        } catch (RejectedExecutionException e) {
            toExecuteLater.add(task);
        }
    }

    public void waitUntilAllTasksDone(boolean timeout) {
        // Timeout does nothing here...
        boolean failed = false;
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
            long time = System.nanoTime();
            for (Future<?> future : futures) {
                future.get();
            }
            timeSpentWaiting.set(System.nanoTime() - time);
        } catch (InterruptedException | ExecutionException e) {
            throw new SpoolException(e.getMessage());
        }
        overflowSize = toExecuteLater.size();
        if (!failed) toExecuteLater.clear();
        futuresSize = futures.size();
        futures.clear();
        timeExecuting = timeSpentExecuting.getAndSet(0);
        timeOverhead = overhead.getAndSet(0);
        timeWaiting = timeSpentWaiting.getAndSet(0);
    }
}
