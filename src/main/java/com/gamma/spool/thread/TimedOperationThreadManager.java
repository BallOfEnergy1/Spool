package com.gamma.spool.thread;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TimedOperationThreadManager extends ThreadManager {

    public TimedOperationThreadManager(String name, int threads) {
        super(name, threads);
    }

    @Override
    public void startPool() {
        pool = new ScheduledThreadPoolExecutor(threads, namedThreadFactory);
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
