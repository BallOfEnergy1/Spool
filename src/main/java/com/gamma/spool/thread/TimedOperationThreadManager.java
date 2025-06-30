package com.gamma.spool.thread;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.gamma.spool.Spool;
import com.gamma.spool.SpoolException;
import com.gamma.spool.config.DebugConfig;

/**
 * Extension of the {@link ThreadManager} that uses a {@link ScheduledThreadPoolExecutor}.
 * This manager supports scheduling tasks with a delay and provides similar behavior
 * to the standard {@link ThreadManager}.
 */
public class TimedOperationThreadManager extends ThreadManager {

    public TimedOperationThreadManager(String name, int threads) {
        super(name, threads);
    }

    @Override
    public void startPool() {
        if (DebugConfig.debugLogging)
            Spool.logger.info("Starting pool ({}) with {} threads.", this.getName(), this.getNumThreads());
        if (this.isStarted()) throw new SpoolException("Pool already started (" + this.getName() + ")!");
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
        else((ScheduledThreadPoolExecutor) pool).schedule(task, time, unit);
    }
}
