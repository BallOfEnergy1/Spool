package com.gamma.spool.thread;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.gamma.spool.core.SpoolLogger;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

/**
 * Extension of the {@link ThreadManager} that uses a {@link ScheduledThreadPoolExecutor}.
 * This manager supports scheduling tasks with a delay and provides similar behavior
 * to the standard {@link ThreadManager}.
 */
public class TimedOperationThreadManager extends ThreadManager {

    public TimedOperationThreadManager(String name, int threads) {
        super(name, threads);
    }

    public final Long2LongMap taskMap = new Long2LongOpenHashMap();

    @Override
    public void startPool() {
        SpoolLogger.debug("Starting pool ({}) with {} threads.", this.getName(), threads);
        if (this.isStarted()) throw new IllegalStateException("Pool already started (" + this.getName() + ")!");
        pool = new ScheduledThreadPoolExecutor(threads, namedThreadFactory);

        // Toggles to make the pool (termination mostly) behavior as similar to ThreadManager as possible.
        ((ScheduledThreadPoolExecutor) pool).setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        ((ScheduledThreadPoolExecutor) pool).setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        ((ScheduledThreadPoolExecutor) pool).setRemoveOnCancelPolicy(true);

        pool.prestartCoreThread();
    }

    @Override
    public void execute(Runnable task) {
        this.execute(task, 0, null);
    }

    public void execute(Runnable task, long time, TimeUnit unit) {
        if (time == 0) super.execute(task);
        else {
            long currentTime = System.currentTimeMillis();
            taskMap.put(currentTime, currentTime + TimeUnit.MILLISECONDS.convert(time, unit));
            ((ScheduledThreadPoolExecutor) pool).schedule(() -> {
                // Slow, but we can sacrifice it here since this won't be used much.
                task.run();
                taskMap.remove(currentTime);
            }, time, unit);
        }
    }

    @Override
    public <A> void execute(Consumer<A> func, A arg) {
        throw new UnsupportedOperationException("TimedOperationThreadManager does not support executing consumers!");
    }

    @Override
    public <A, B> void execute(BiConsumer<A, B> func, A arg1, B arg2) {
        throw new UnsupportedOperationException("TimedOperationThreadManager does not support executing bi-consumers!");
    }
}
