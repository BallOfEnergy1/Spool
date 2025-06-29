package com.gamma.spool.thread;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import com.gamma.spool.Spool;
import com.gamma.spool.SpoolException;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;

public class KeyedPoolThreadManager implements IThreadManager { // Technically, is this resizable? yes! Can it implement
                                                                // IResizableThreadManager? not really...

    Int2ObjectArrayMap<ExecutorService> keyedPool = new Int2ObjectArrayMap<>();
    public Queue<Future<?>> futures = new ConcurrentLinkedDeque<>();

    AtomicLong timeSpentExecuting = new AtomicLong();
    AtomicLong overhead = new AtomicLong();

    public long timeExecuting;
    public long timeWaiting;
    public long timeOverhead;

    private final String name;
    protected int threads;

    public KeyedPoolThreadManager(String name) {
        this.name = name;
    }

    public void addKeyedThread(int threadKey, String name) {
        // Maybe performance loss? This won't happen very often, so not a big problem.
        keyedPool.put(
            threadKey,
            Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder().setNameFormat("Spool-" + name)
                    .build()));
        threads++;
    }

    public void removeKeyedThread(int threadKey) {
        ExecutorService executor = keyedPool.get(threadKey);
        if (executor == null) return;
        else {
            executor.shutdown();
            try {
                if (!executor
                    .awaitTermination(Spool.configManager.globalTerminatingSingleThreadTimeout, TimeUnit.MILLISECONDS))
                    executor.shutdownNow();
            } catch (InterruptedException e) {
                throw new SpoolException("Thread termination interrupted: " + e.getMessage());
            }
        }
        keyedPool.remove(threadKey);
        threads--;
    }

    public int getNumThreads() {
        return keyedPool.size();
    }

    public long getTimeExecuting() {
        return timeExecuting;
    }

    public long getTimeOverhead() {
        return timeOverhead;
    }

    public long getTimeWaiting() {
        return timeWaiting;
    }

    public String getName() {
        return name;
    }

    // This is all a mess...
    public void terminatePool() {
        for (ExecutorService executor : keyedPool.values()) {
            // Shutdown all executors, prohibiting new tasks from being added.
            // Realistically, this doesn't do much except allowing me to use `awaitTermination()` later on.
            executor.shutdown();
        }

        // This section allows for dictating a *total* timeout time, meaning after `timeoutTime` milliseconds *total*,
        // tasks will begin being cancelled.

        int timeoutTime = Spool.configManager.globalTerminatingSingleThreadTimeout / keyedPool.size(); // milliseconds
        long elapsedTime = 0;
        for (ExecutorService executor : keyedPool.values()) {
            long time = System.nanoTime();
            try {
                if (!executor.awaitTermination(Math.max(timeoutTime - elapsedTime, 0), TimeUnit.MILLISECONDS))
                    executor.shutdownNow();
            } catch (InterruptedException e) {
                throw new SpoolException("Pool termination interrupted: " + e.getMessage());
            }
            elapsedTime += TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS);
        }
    }

    // Assumes the thread key is 0.
    public void execute(Runnable task) {
        execute(0, task);
    }

    public void execute(int threadKey, Runnable task) {
        if (!keyedPool.containsKey(threadKey)) {
            // go initialize the thread you dingus
            throw new SpoolException("Keyed thread never initialized for key " + threadKey + "!");
        }
        long time = 0;
        if (Spool.configManager.debug) time = System.nanoTime();
        futures.add(
            keyedPool.get(threadKey)
                .submit(() -> {
                    long timeInternal = 0;
                    if (Spool.configManager.debug) timeInternal = System.nanoTime();
                    task.run();
                    if (Spool.configManager.debug) timeSpentExecuting.addAndGet(System.nanoTime() - timeInternal);
                }));
        if (Spool.configManager.debug) overhead.addAndGet(System.nanoTime() - time);
    }

    public void waitUntilAllTasksDone(boolean timeout) {

        // This section allows for dictating a *total* timeout time, meaning after `timeoutTime` milliseconds *total*,
        // tasks will begin being cancelled.

        int timeoutTime;
        if (timeout) timeoutTime = Spool.configManager.globalRunningSingleThreadTimeout / keyedPool.size(); // milliseconds
        else timeoutTime = Spool.configManager.globalTerminatingSingleThreadTimeout / keyedPool.size(); // milliseconds

        long elapsedTime = 0;
        Iterator<Future<?>> iterator = futures.iterator();
        while (iterator.hasNext()) {
            Future<?> future = iterator.next();
            long time = System.nanoTime();
            try {
                future.get(Math.max(timeoutTime - elapsedTime, 0), TimeUnit.MILLISECONDS);
                iterator.remove();
            } catch (InterruptedException | ExecutionException e) {
                throw new SpoolException("Pool waiting failed: " + e.getMessage());
            } catch (TimeoutException e) {
                Spool.logger.warn("Keyed pool ({}) did not finish all tasks in time!", name);
                break;
            }
            elapsedTime += TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS);
        }
        timeWaiting = TimeUnit.NANOSECONDS.convert(elapsedTime, TimeUnit.MILLISECONDS);
        if (!futures.isEmpty()) {
            Spool.logger.warn("Pool ({}) discarding {} updates.", name, futures.size());
            // TODO: make this also not drop tasks
            futures.clear();
        }
        if (Spool.configManager.debug) {
            timeExecuting = timeSpentExecuting.getAndSet(0);
            timeOverhead = overhead.getAndSet(0);
        }
    }

    // The below functions don't really make sense in this context,
    // especially considering there is no real difference between "running" and "stopped" here.
    // The only difference is that the `keyedPool` will be empty when it's "stopped".

    public boolean isStarted() {
        return true;
    }

    public void startPoolIfNeeded() {
        // NO-OP
    }

    public void startPool() {
        // NO-OP
    }
}
