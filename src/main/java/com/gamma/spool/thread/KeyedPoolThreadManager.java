package com.gamma.spool.thread;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jctools.maps.NonBlockingHashMapLong;

import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.config.ThreadManagerConfig;
import com.gamma.spool.core.SpoolLogger;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.PriorityQueues;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;

/**
 * Manages a set of keyed thread pools, where each pool corresponds to a unique key and tasks can be executed
 * independently within the context of each keyed pool. This class extends {@link RollingAverageWrapper}.
 * <p>
 * The KeyedPoolThreadManager enables the creation and management of thread pools that are referenced by integer keys.
 * Tasks can be submitted to specific keyed threads.
 * <p>
 * Each pool is backed by a single-threaded {@link ExecutorService}.
 * <p>
 * This class avoids the overhead of submitting tasks to a main "pool" for executing everything and instead allows
 * delegating different tasks to their own threads in the same "pool" style.
 *
 * @implNote Threads *must* be added to the pool before a task can be executed under that key.
 *           Failure to do so will throw a {@link IllegalStateException}
 */
public class KeyedPoolThreadManager extends RollingAverageWrapper {

    /**
     * Represents the default key used for identifying the default thread.
     * This key is used when a thread key is not explicitly provided
     * or when the thread limit is reached, and new tasks must be
     * assigned to the default executor thread.
     */
    public static final int DEFAULT_THREAD_KEY = Integer.MIN_VALUE + 1;

    /**
     * Represents the unique key used to identify the <i><b>program's main thread</b></i> in the keyed thread pool.
     * This key is constant and reserved exclusively for operations associated with the main thread.
     * <p>
     * Tasks assigned to this key will be executed <i>in the main thread</i> at the time that the
     * <code>.execute()</code>
     * function is run.
     */
    public static final int MAIN_THREAD_KEY = Integer.MIN_VALUE + 2;

    final NonBlockingHashMapLong<ExecutorService> keyedPool = new NonBlockingHashMapLong<>();
    final NonBlockingHashMapLong<String> keyDefaultRemap = new NonBlockingHashMapLong<>();
    public final PriorityQueue<Future<?>> futures = PriorityQueues.synchronize(
        new ObjectHeapPriorityQueue<>(
            8192, // 8192 is the default capacity; will be expanded as needed.
            (o1, o2) -> Boolean.compare(o2.isDone(), o1.isDone())));

    protected IntSet keys;
    protected IntSet mappedKeys;
    protected IntSet allKeys;

    final AtomicLong timeSpentExecuting = new AtomicLong();
    final AtomicLong overhead = new AtomicLong();

    public long timeExecuting;
    public long timeWaiting;
    public long timeOverhead;

    protected final String name;

    protected int threads;
    protected final int threadLimit;

    protected boolean isDisabled;

    public KeyedPoolThreadManager(String name, int threadLimit) {
        this.name = name;
        this.threadLimit = threadLimit + 1;
    }

    /**
     * Adds a new thread with a specific key to the keyed thread pool.
     * If a thread associated with the provided key already exists,
     * no new thread is added.
     * <p>
     * If the thread limit is reached, the new key will be mapped
     * to the default executor thread inside this manager.
     *
     * @param threadKey the unique key identifying the thread to be added
     * @param name      the name to be used when creating the thread
     */
    public void addKeyedThread(int threadKey, String name) {
        if (keyedPool.containsKey(threadKey)) return;
        if (threads >= threadLimit) {
            keyDefaultRemap.put(threadKey, name);
            mappedKeys = null;
            allKeys = null;
            threads++;
            return;
        }
        // Maybe performance loss? This won't happen very often, so not a big problem.
        keyedPool.put(
            threadKey,
            Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder().setNameFormat("Spool-" + name)
                    .build()));
        keys = null;
        allKeys = null;
        threads++;
    }

    private void forceAddDefaultThread(String name) {
        keyedPool
            .put(KeyedPoolThreadManager.DEFAULT_THREAD_KEY, new ForkJoinPool(4, pool -> new ForkJoinWorkerThread(pool) {

                {
                    setName("Spool-" + name + "-" + getPoolIndex());
                }
            }, null, true));
        keys = null;
        allKeys = null;
        threads++;
    }

    /**
     * Terminates and removes a keyed thread from the pool.
     * If a thread associated with the given key exists, it is shut down and removed from the pool.
     *
     * @param threadKey the unique key identifying the thread to be removed
     * @throws RuntimeException if the termination process is interrupted.
     */
    public void removeKeyedThread(int threadKey) {
        ExecutorService executor = keyedPool.get(threadKey);
        if (executor == null) {
            if (keyDefaultRemap.containsKey(threadKey)) {
                keyDefaultRemap.remove(threadKey);
                mappedKeys = null;
                allKeys = null;
                threads--;
                // piss
                SpoolLogger.warn(
                    "KeyedPoolThreadManager ({}) removed key ({}) inside remap list, tasks will not be stopped for this key.",
                    this.getName(),
                    threadKey);
            }
            return;
        } else {
            executor.shutdown();
            try {
                if (!executor
                    .awaitTermination(ThreadManagerConfig.globalTerminatingSingleThreadTimeout, TimeUnit.MILLISECONDS))
                    executor.shutdownNow();
            } catch (InterruptedException e) {
                throw new RuntimeException("Thread termination interrupted: " + e.getMessage());
            }
            keyedPool.remove(threadKey);
            keys = null;
            allKeys = null;
        }
        threads--;
    }

    /**
     * Retrieves the set of keys currently present in the keyed pool,
     * including the default thread key (DEFAULT_THREAD_KEY).
     *
     * @return an IntSet containing the keys in the keyed pool.
     */
    public IntSet getKeys() {
        if (keys == null) keys = IntSet.of(
            Arrays.stream(keyedPool.keySetLong())
                .mapToInt(i -> (int) i)
                .toArray());
        return keys;
    }

    /**
     * Retrieves the set of keys currently present in the remapping table.
     *
     * @return an IntSet containing the keys in the remapping table.
     */
    public IntSet getMappedKeys() {
        if (mappedKeys == null) mappedKeys = IntSet.of(
            Arrays.stream(keyDefaultRemap.keySetLong())
                .mapToInt(i -> (int) i)
                .toArray());
        return mappedKeys;
    }

    public IntSet getAllKeys() {
        if (allKeys == null) {
            allKeys = new IntArraySet(this.getKeys());
            allKeys.addAll(this.getMappedKeys());
        }
        return allKeys;
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
        if (keyedPool.isEmpty()) {
            return;
        }

        for (ExecutorService executor : keyedPool.values()) {
            // Shutdown all executors, prohibiting new tasks from being added.
            // Realistically, this doesn't do much except allowing me to use `awaitTermination()` later on.
            executor.shutdown();
        }

        // This section allows for dictating a *total* timeout time, meaning after `timeoutTime` milliseconds *total*,
        // tasks will begin being canceled.

        int timeoutTime = ThreadManagerConfig.globalTerminatingSingleThreadTimeout / keyedPool.size(); // milliseconds
        long elapsedTime = 0;
        for (ExecutorService executor : keyedPool.values()) {
            long time = System.nanoTime();
            try {
                if (!executor.awaitTermination(Math.max(timeoutTime - elapsedTime, 0), TimeUnit.MILLISECONDS))
                    executor.shutdownNow();
            } catch (InterruptedException e) {
                throw new RuntimeException("Pool termination interrupted: " + e.getMessage());
            }
            elapsedTime += TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS);
        }
        futures.clear();
        keyedPool.clear();
        keyDefaultRemap.clear();
        keys = null;
        mappedKeys = null;
        allKeys = null;
        threads = 0;
    }

    /**
     * Executes a given task in the default thread pool associated with this manager.
     *
     * @param task the task to be executed
     */
    public void execute(Runnable task) {
        execute(DEFAULT_THREAD_KEY, task);
    }

    public <A> void execute(Consumer<A> task, A arg1) {
        execute(DEFAULT_THREAD_KEY, task, arg1);
    }

    public <A, B> void execute(BiConsumer<A, B> task, final A arg1, final B arg2) {
        execute(DEFAULT_THREAD_KEY, task, arg1, arg2);
    }

    /**
     * Executes a given task in the thread associated with the specified thread key.
     * If the specified thread key does not exist in the keyed thread pool, an exception is thrown.
     * <p>
     * If the thread limit was previously reached, the task will be executed under the default
     * thread instead.
     *
     * @param threadKey the unique key identifying the thread where the task will be executed
     * @param task      the task to be executed
     * @throws IllegalStateException if no thread is initialized for the specified thread key
     */
    public void execute(int threadKey, Runnable task) {
        if (isDisabled) {
            task.run();
            return;
        }

        if (ThreadManagerConfig.betterTaskProfiling) task = ExecutionTasks.wrapTask(task);
        Runnable finalTask = task;
        if (threadKey == MAIN_THREAD_KEY) {
            if (DebugConfig.debug) {
                long time = System.nanoTime();
                task.run();
                timeSpentExecuting.addAndGet(System.nanoTime() - time);
            } else task.run();
            return;
        }

        ExecutorService service = keyedPool.get(threadKey);

        if (service == null) {
            if (!keyDefaultRemap.containsKey(threadKey)) {
                SpoolLogger
                    .debugWarn("Attempted to execute a task on an unregistered thread key; using default thread.");
                this.execute(DEFAULT_THREAD_KEY, task);
            } else {
                if (threads < threadLimit) {
                    SpoolLogger.debug(
                        "KeyedPoolThreadManager ({}) moving remapped thread ({}) to main pool due to pool size.",
                        this.getName(),
                        threadKey);
                    addKeyedThread(threadKey, keyDefaultRemap.get(threadKey));
                    keyDefaultRemap.remove(threadKey);
                    mappedKeys = null;
                    keys = null;
                    allKeys = null;
                } else this.execute(MAIN_THREAD_KEY, task); // Execute it under the default thread.
            }
            return;
        }

        if (DebugConfig.debug) {
            long time = System.nanoTime();
            futures.enqueue(service.submit(() -> {
                long timeInternal = System.nanoTime();
                finalTask.run();
                timeSpentExecuting.addAndGet(System.nanoTime() - timeInternal);
            }));
            overhead.addAndGet(System.nanoTime() - time);
        } else futures.enqueue(service.submit(task));
    }

    public <A> void execute(int threadKey, Consumer<A> task, A arg1) {
        if (isDisabled) {
            task.accept(arg1);
            return;
        }

        if (ThreadManagerConfig.betterTaskProfiling) task = ExecutionTasks.wrapTask(task);
        Consumer<A> finalTask = task;
        if (threadKey == MAIN_THREAD_KEY) {
            if (DebugConfig.debug) {
                long time = System.nanoTime();
                task.accept(arg1);
                timeSpentExecuting.addAndGet(System.nanoTime() - time);
            } else task.accept(arg1);
            return;
        }

        ExecutorService service = keyedPool.get(threadKey);

        if (service == null) {
            if (!keyDefaultRemap.containsKey(threadKey)) {
                SpoolLogger
                    .debugWarn("Attempted to execute a task on an unregistered thread key; using default thread.");
                this.execute(DEFAULT_THREAD_KEY, task, arg1);
            } else {
                if (threads < threadLimit) {
                    SpoolLogger.debug(
                        "KeyedPoolThreadManager ({}) moving remapped thread ({}) to main pool due to pool size.",
                        this.getName(),
                        threadKey);
                    addKeyedThread(threadKey, keyDefaultRemap.get(threadKey));
                    keyDefaultRemap.remove(threadKey);
                    mappedKeys = null;
                    allKeys = null;
                    keys = null;
                } else this.execute(MAIN_THREAD_KEY, task, arg1); // Execute it under the default thread.
            }
            return;
        }

        if (DebugConfig.debug) {
            long time = System.nanoTime();
            futures.enqueue(service.submit(() -> {
                long timeInternal = System.nanoTime();
                finalTask.accept(arg1);
                timeSpentExecuting.addAndGet(System.nanoTime() - timeInternal);
            }));
            overhead.addAndGet(System.nanoTime() - time);
        } else futures.enqueue(service.submit(() -> finalTask.accept(arg1)));
    }

    public <A, B> void execute(int threadKey, BiConsumer<A, B> task, final A arg1, final B arg2) {
        if (isDisabled) {
            task.accept(arg1, arg2);
            return;
        }

        if (ThreadManagerConfig.betterTaskProfiling) task = ExecutionTasks.wrapTask(task);
        BiConsumer<A, B> finalTask = task;
        if (threadKey == MAIN_THREAD_KEY) {
            if (DebugConfig.debug) {
                long time = System.nanoTime();
                task.accept(arg1, arg2);
                timeSpentExecuting.addAndGet(System.nanoTime() - time);
            } else task.accept(arg1, arg2);
            return;
        }

        ExecutorService service = keyedPool.get(threadKey);

        if (service == null) {
            if (!keyDefaultRemap.containsKey(threadKey)) {
                SpoolLogger
                    .debugWarn("Attempted to execute a task on an unregistered thread key; using default thread.");
                this.execute(DEFAULT_THREAD_KEY, task, arg1, arg2);
            } else {
                if (threads < threadLimit) {
                    SpoolLogger.debug(
                        "KeyedPoolThreadManager ({}) moving remapped thread ({}) to main pool due to pool size.",
                        this.getName(),
                        threadKey);
                    addKeyedThread(threadKey, keyDefaultRemap.get(threadKey));
                    keyDefaultRemap.remove(threadKey);
                    mappedKeys = null;
                    allKeys = null;
                    keys = null;
                } else this.execute(MAIN_THREAD_KEY, task, arg1, arg2); // Execute it under the default thread.
            }
            return;
        }

        if (DebugConfig.debug) {
            long time = System.nanoTime();
            futures.enqueue(service.submit(() -> {
                long timeInternal = System.nanoTime();
                finalTask.accept(arg1, arg2);
                timeSpentExecuting.addAndGet(System.nanoTime() - timeInternal);
            }));
            overhead.addAndGet(System.nanoTime() - time);
        } else futures.enqueue(service.submit(() -> finalTask.accept(arg1, arg2)));
    }

    private int updateCache = 0;

    public void waitUntilAllTasksDone(boolean timeout) {
        if (isDisabled) {
            return;
        }

        // This section allows for dictating a *total* timeout time, meaning after `timeoutTime` milliseconds *total*,
        // tasks will begin being canceled.

        int timeoutTime;
        if (timeout) timeoutTime = ThreadManagerConfig.globalRunningSingleThreadTimeout / keyedPool.size(); // milliseconds
        else timeoutTime = ThreadManagerConfig.globalTerminatingSingleThreadTimeout / keyedPool.size(); // milliseconds

        long elapsedTime = 0;

        while (!futures.isEmpty()) {
            Future<?> future = futures.dequeue();
            long time = System.nanoTime();
            try {
                if (future != null && !future.isDone()) // For some reason this happens sometimes...
                    future.get(Math.max(timeoutTime - elapsedTime, 0), TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Failed execution for task.", e);
            } catch (TimeoutException e) {
                if (DebugConfig.debugLogging)
                    SpoolLogger.debugWarn("Keyed pool ({}) did not finish all tasks in time!", name);
                SpoolLogger.warnRateLimited("Keyed pool ({}) did not finish all tasks in time!", name);
                break;
            }
            elapsedTime += TimeUnit.MILLISECONDS.convert(System.nanoTime() - time, TimeUnit.NANOSECONDS);
        }

        timeWaiting = TimeUnit.NANOSECONDS.convert(elapsedTime, TimeUnit.MILLISECONDS);

        if (!futures.isEmpty()) {
            if (ThreadManagerConfig.dropTasksOnTimeout) {
                if (DebugConfig.debugLogging)
                    SpoolLogger.debugWarn("Pool ({}) dropped {} updates.", name, futures.size());
                else if (!SpoolLogger
                    .warnRateLimited("Pool ({}) dropped {} updates.", name, futures.size() + updateCache))
                    updateCache += futures.size();
                else updateCache = 0;
                while (!futures.isEmpty()) cancelFuture(futures.dequeue());
            } else {
                if (DebugConfig.debugLogging) SpoolLogger.debugWarn(
                    "Pool ({}) overflowed {} updates, they will be executed whenever possible to avoid dropping updates.",
                    name,
                    futures.size());
                else if (!SpoolLogger.warnRateLimited(
                    "Pool ({}) overflowed {} updates, they will be executed whenever possible to avoid dropping updates.",
                    name,
                    futures.size() + updateCache)) updateCache += futures.size();
                else updateCache = 0;
            }
        }
        if (DebugConfig.debug) {
            timeExecuting = timeSpentExecuting.getAndSet(0);
            timeOverhead = overhead.getAndSet(0);
            updateTimes();
        }
    }

    // The below functions don't really make sense in this context,
    // especially considering there is no real difference between "running" and "stopped" here.
    // The only difference is that the `keyedPool` will be empty when it's "stopped".

    public boolean isStarted() {
        return getKeys().contains(DEFAULT_THREAD_KEY);
    }

    @Override
    public boolean isPoolDisabled() {
        return isDisabled;
    }

    public void startPool() {
        SpoolLogger.debug("Starting pool ({}) with {} threads.", this.getName(), threads + 1);
        if (this.isStarted()) throw new IllegalStateException("Pool already started (" + this.getName() + ")!");
        forceAddDefaultThread(name + "-default");
    }

    @Override
    public void disablePool() {
        isDisabled = true;
    }

    @Override
    public void enablePool() {
        isDisabled = false;
    }
}
