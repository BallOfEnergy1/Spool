package com.gamma.spool.thread;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.gamma.spool.config.ThreadManagerConfig;
import com.gamma.spool.core.SpoolLogger;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * This class ONLY works when >J21 is being run.
 *
 * In exchange for banishing all debugging and profiling code, this class allows for near-zero
 * virtual thread overhead, making it extremely performant for frequently-blocking tasks.
 */
public class VirtualThreadManager extends RollingAverageWrapper {

    private ExecutorService pool;

    private final String name;
    protected final int threads;

    private final Phaser phaser = new Phaser(0);
    private final Semaphore threadLimiter;

    public VirtualThreadManager(String name, int threads) {
        this.threads = threads;
        this.name = name;
        threadLimiter = new Semaphore(threads);
    }

    public void startPool() {
        SpoolLogger.debug("Starting pool ({}) with {} threads.", this.getName(), this.threads);
        if (this.isStarted()) throw new IllegalStateException("Pool already started (" + this.getName() + ")!");
        pool = Executors.newThreadPerTaskExecutor(
            new ThreadFactoryBuilder().setThreadFactory(
                Thread.ofVirtual()
                    .factory())
                .setNameFormat("Spool-" + name + "-%d")
                .build());
    }

    public void terminatePool() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(
                (long) ThreadManagerConfig.globalTerminatingSingleThreadTimeout,
                TimeUnit.MILLISECONDS)) pool.shutdownNow();
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
        pool.submit(() -> {
            threadLimiter.acquireUninterruptibly();
            phaser.register();
            finalTask.run();
            phaser.arriveAndDeregister();
            threadLimiter.release();
        });
    }

    public <A> void execute(Consumer<A> task, A arg1) {
        if (ThreadManagerConfig.betterTaskProfiling) task = ExecutionTasks.wrapTask(task);
        Consumer<A> finalTask = task;
        pool.submit(() -> {
            threadLimiter.acquireUninterruptibly();
            phaser.register();
            finalTask.accept(arg1);
            phaser.arriveAndDeregister();
            threadLimiter.release();
        });
    }

    public <A, B> void execute(BiConsumer<A, B> task, final A arg1, final B arg2) {
        if (ThreadManagerConfig.betterTaskProfiling) task = ExecutionTasks.wrapTask(task);
        BiConsumer<A, B> finalTask = task;
        pool.submit(() -> {
            threadLimiter.acquireUninterruptibly();
            phaser.register();
            finalTask.accept(arg1, arg2);
            phaser.arriveAndDeregister();
            threadLimiter.release();
        });
    }

    public void waitUntilAllTasksDone(boolean timeout) {
        phaser.arriveAndAwaitAdvance();
    }

    @Override
    public Optional<Throwable> getPendingExceptionIfAny() {
        return Optional.empty();
    }

    @Override
    public int getNumThreads() {
        return threads;
    }

    @Override
    public long getTimeExecuting() {
        return -1;
    }

    @Override
    public long getTimeOverhead() {
        return -1;
    }

    @Override
    public long getTimeWaiting() {
        return -1;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isPoolDisabled() {
        return false;
    }

    @Override
    public void disablePool() {}

    @Override
    public void enablePool() {}
}
