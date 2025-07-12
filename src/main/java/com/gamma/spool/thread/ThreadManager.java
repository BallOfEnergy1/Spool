package com.gamma.spool.thread;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.gamma.spool.SpoolLogger;
import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.config.ThreadManagerConfig;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.ObjectLists;

/**
 * Manages a thread pool for executing tasks, providing functionality to track execution metrics
 * and handle task execution.
 * Implements {@link IThreadManager}.
 */
public class ThreadManager implements IThreadManager {

    public ThreadPoolExecutor pool;

    public final ObjectList<Future<?>> futures = ObjectLists.synchronize(new ObjectArrayList<>());

    final ThreadFactory namedThreadFactory;

    final AtomicLong timeSpentExecuting = new AtomicLong();
    final AtomicLong overhead = new AtomicLong();

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
    public final Queue<Runnable> toExecuteLater = new ConcurrentLinkedQueue<>();

    protected final int threads;

    public ThreadManager(String name, int threads) {
        this.threads = threads;
        this.name = name;
        namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("Spool-" + name + "-%d")
            .build();
    }

    public void startPool() {
        SpoolLogger.debug("Starting pool ({}) with {} threads.", this.getName(), threads);
        if (this.isStarted()) throw new IllegalStateException("Pool already started (" + this.getName() + ")!");
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
        if (pool.getActiveCount() == 0) {
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
        try {
            if (DebugConfig.debug) {
                long time = System.nanoTime();
                futures.add(pool.submit(() -> {
                    long timeInternal = System.nanoTime();
                    finalTask.run();
                    timeSpentExecuting.addAndGet(System.nanoTime() - timeInternal);
                }));
                overhead.addAndGet(System.nanoTime() - time);
            } else futures.add(pool.submit(task));
        } catch (RejectedExecutionException e) {
            if (DebugConfig.debug) toExecuteLater.add(() -> {
                long timeInternal = System.nanoTime();
                finalTask.run();
                timeSpentExecuting.addAndGet(System.nanoTime() - timeInternal);
            });
            else toExecuteLater.add(task);
        }
    }

    public <A> void execute(Consumer<A> task, A arg1) {
        if (ThreadManagerConfig.betterTaskProfiling) task = ExecutionTasks.wrapTask(task);
        Consumer<A> finalTask = task;
        try {
            if (DebugConfig.debug) {
                long time = System.nanoTime();
                futures.add(pool.submit(() -> {
                    long timeInternal = System.nanoTime();
                    finalTask.accept(arg1);
                    timeSpentExecuting.addAndGet(System.nanoTime() - timeInternal);
                }));
                overhead.addAndGet(System.nanoTime() - time);
            } else futures.add(pool.submit(() -> finalTask.accept(arg1)));
        } catch (RejectedExecutionException e) {
            if (DebugConfig.debug) toExecuteLater.add(() -> {
                long timeInternal = System.nanoTime();
                finalTask.accept(arg1);
                timeSpentExecuting.addAndGet(System.nanoTime() - timeInternal);
            });
            else toExecuteLater.add(() -> finalTask.accept(arg1));
        }
    }

    public <A, B> void execute(BiConsumer<A, B> task, final A arg1, final B arg2) {
        if (ThreadManagerConfig.betterTaskProfiling) task = ExecutionTasks.wrapTask(task);
        BiConsumer<A, B> finalTask = task;
        try {
            if (DebugConfig.debug) {
                long time = System.nanoTime();
                futures.add(pool.submit(() -> {
                    long timeInternal = System.nanoTime();
                    finalTask.accept(arg1, arg2);
                    timeSpentExecuting.addAndGet(System.nanoTime() - timeInternal);
                }));
                overhead.addAndGet(System.nanoTime() - time);
            } else futures.add(pool.submit(() -> finalTask.accept(arg1, arg2)));
        } catch (RejectedExecutionException e) {
            if (DebugConfig.debug) toExecuteLater.add(() -> {
                long timeInternal = System.nanoTime();
                finalTask.accept(arg1, arg2);
                timeSpentExecuting.addAndGet(System.nanoTime() - timeInternal);
            });
            else toExecuteLater.add(() -> finalTask.accept(arg1, arg2));
        }
    }

    private int updateCache = 0;

    public void waitUntilAllTasksDone(boolean timeout) {
        if (pool.getActiveCount() == 0 && pool.getQueue()
            .isEmpty() && toExecuteLater.isEmpty()) return;
        boolean failed = false;
        long timeSpentWaiting = 0;

        int timeoutTime;
        if (timeout) timeoutTime = ThreadManagerConfig.globalRunningSingleThreadTimeout / pool.getPoolSize(); // milliseconds
        else timeoutTime = ThreadManagerConfig.globalTerminatingSingleThreadTimeout / pool.getPoolSize(); // milliseconds

        if (DebugConfig.debug) {
            futuresSize = futures.size();
        }

        try {
            List<Runnable> runTasks = new ObjectArrayList<>();
            long totalTime = 0;
            try {
                for (Runnable runnable : toExecuteLater) {
                    long time = 0;
                    if (DebugConfig.debug) time = System.nanoTime();
                    futures.add(pool.submit(runnable));
                    runTasks.add(runnable);
                    if (DebugConfig.debug) totalTime += System.nanoTime() - time;
                }
                if (DebugConfig.debug) overhead.addAndGet(totalTime);
            } catch (RejectedExecutionException e) {
                if (DebugConfig.debug) overhead.addAndGet(totalTime);
                failed = true;
                toExecuteLater.removeAll(runTasks);
            }
            ObjectListIterator<Future<?>> iterator = futures.listIterator();
            while (iterator.hasNext()) {
                Future<?> future = iterator.next();
                long time = System.nanoTime();
                try {
                    if (future == null) // For some reason this happens sometimes...
                        continue;
                    future.get(Math.max(timeoutTime - timeSpentWaiting, 0), TimeUnit.MILLISECONDS);
                    iterator.remove();
                } catch (TimeoutException e) {
                    if (DebugConfig.debugLogging) SpoolLogger.warn("Pool ({}) did not finish all tasks in time!", name);
                    else SpoolLogger.warnRateLimited("Pool ({}) did not finish all tasks in time!", name);
                    break;
                }
                timeSpentWaiting += TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed execution for task.", e);
        }

        if (!failed) toExecuteLater.clear();
        if (!futures.isEmpty()) {
            if (ThreadManagerConfig.dropTasksOnTimeout) {
                if (DebugConfig.debugLogging) SpoolLogger.warn("Pool ({}) dropped {} updates.", name, futures.size());
                else if (!SpoolLogger
                    .warnRateLimited("Pool ({}) dropped {} updates.", name, futures.size() + updateCache))
                    updateCache += futures.size();
                futures.forEach(this::cancelFuture);
                futures.clear();
            } else if (DebugConfig.debugLogging) SpoolLogger.warn(
                "Pool ({}) overflowed {} updates, they will be executed whenever possible to avoid dropping updates.",
                name,
                futures.size());
            else if (!SpoolLogger.warnRateLimited(
                "Pool ({}) overflowed {} updates, they will be executed whenever possible to avoid dropping updates.",
                name,
                futures.size() + updateCache)) updateCache += futures.size();
        }
        if (DebugConfig.debug) {
            overflowSize = toExecuteLater.size();
            timeExecuting = timeSpentExecuting.getAndSet(0);
            timeOverhead = overhead.getAndSet(0);
            timeWaiting = TimeUnit.NANOSECONDS.convert(timeSpentWaiting, TimeUnit.MILLISECONDS);
        }
    }
}
