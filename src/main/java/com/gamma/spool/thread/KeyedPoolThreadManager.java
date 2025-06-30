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
import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.config.ThreadManagerConfig;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * Manages a set of keyed thread pools, where each pool corresponds to a unique key and tasks can be executed
 * independently within the context of each keyed pool. This class implements {@link IThreadManager}.
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
 *           Failure to do so will throw a {@link SpoolException}
 */
public class KeyedPoolThreadManager implements IResizableThreadManager {

    /**
     * Represents the default key used for identifying the default thread.
     * This key is used when a thread key is not explicitly provided
     * or when the thread limit is reached, and new tasks must be
     * assigned to the default executor thread.
     */
    public static final int DEFAULT_THREAD_KEY = Integer.MIN_VALUE + 1;

    Int2ObjectArrayMap<ExecutorService> keyedPool = new Int2ObjectArrayMap<>();
    Int2ObjectArrayMap<String> keyDefaultRemap = new Int2ObjectArrayMap<>();
    public Queue<Future<?>> futures = new ConcurrentLinkedDeque<>();

    AtomicLong timeSpentExecuting = new AtomicLong();
    AtomicLong overhead = new AtomicLong();

    public long timeExecuting;
    public long timeWaiting;
    public long timeOverhead;

    private final String name;

    protected int threads;
    protected int threadLimit;

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
        if (threads >= threadLimit) keyDefaultRemap.put(threadKey, name);
        // Maybe performance loss? This won't happen very often, so not a big problem.
        keyedPool.put(
            threadKey,
            Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder().setNameFormat("Spool-" + name)
                    .build()));
        threads++;
    }

    /**
     * Terminates and removes a keyed thread from the pool.
     * If a thread associated with the given key exists, it is shut down and removed from the pool.
     *
     * @param threadKey the unique key identifying the thread to be removed
     * @throws SpoolException if the termination process is interrupted.
     */
    public void removeKeyedThread(int threadKey) {
        ExecutorService executor = keyedPool.get(threadKey);
        if (executor == null) {
            if (keyDefaultRemap.containsKey(threadKey)) {
                keyDefaultRemap.remove(threadKey);
                // piss
                Spool.logger.warn(
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
                throw new SpoolException("Thread termination interrupted: " + e.getMessage());
            }
        }
        keyedPool.remove(threadKey);
        threads--;
    }

    public void resize(int threads) {
        if (threads > threadLimit) {
            IntIterator iterator = keyDefaultRemap.keySet()
                .iterator();
            for (int i = threadLimit; i < threads; i++) {
                if (iterator.hasNext()) {
                    int key = iterator.nextInt();
                    addKeyedThread(key, keyDefaultRemap.get(key));
                    iterator.remove();
                }
            }
        } else {
            IntIterator iterator = keyedPool.keySet()
                .iterator();
            for (int i = threads; i < threadLimit; i++) {
                if (iterator.hasNext()) {
                    int key = iterator.nextInt();
                    removeKeyedThread(key);
                }
            }
        }
        threadLimit = threads;
    }

    /**
     * Retrieves the set of keys currently present in the keyed pool,
     * including the default thread key (DEFAULT_THREAD_KEY).
     *
     * @return an IntSet containing the keys in the keyed pool.
     */
    public IntSet getKeys() {
        return keyedPool.keySet();
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
        // tasks will begin being canceled.

        int timeoutTime = ThreadManagerConfig.globalTerminatingSingleThreadTimeout / keyedPool.size(); // milliseconds
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
        keyedPool.clear();
        keyDefaultRemap.clear();
    }

    /**
     * Executes a given task in the default thread pool associated with this manager.
     *
     * @param task the task to be executed
     */
    public void execute(Runnable task) {
        execute(DEFAULT_THREAD_KEY, task);
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
     * @throws SpoolException if no thread is initialized for the specified thread key
     */
    public void execute(int threadKey, Runnable task) {
        if (!keyedPool.containsKey(threadKey)) {
            if (!keyDefaultRemap.containsKey(threadKey))
                // go initialize the thread you dingus
                throw new SpoolException("Keyed thread never initialized for key " + threadKey + "!");
            else {
                if (threads < threadLimit) {
                    if (DebugConfig.debugLogging) Spool.logger.info(
                        "KeyedPoolThreadManager ({}) moving remapped thread ({}) to main pool due to pool size.",
                        this.getName(),
                        threadKey);
                    addKeyedThread(threadKey, keyDefaultRemap.get(threadKey));
                    keyDefaultRemap.remove(threadKey);
                } else this.execute(DEFAULT_THREAD_KEY, task); // Execute it under the default thread.
            }
        }
        long time = 0;
        if (DebugConfig.debug) time = System.nanoTime();
        futures.add(
            keyedPool.get(threadKey)
                .submit(() -> {
                    long timeInternal = 0;
                    if (DebugConfig.debug) timeInternal = System.nanoTime();
                    task.run();
                    if (DebugConfig.debug) timeSpentExecuting.addAndGet(System.nanoTime() - timeInternal);
                }));
        if (DebugConfig.debug) overhead.addAndGet(System.nanoTime() - time);
    }

    public void waitUntilAllTasksDone(boolean timeout) {

        // This section allows for dictating a *total* timeout time, meaning after `timeoutTime` milliseconds *total*,
        // tasks will begin being cancelled.

        int timeoutTime;
        if (timeout) timeoutTime = ThreadManagerConfig.globalRunningSingleThreadTimeout / keyedPool.size(); // milliseconds
        else timeoutTime = ThreadManagerConfig.globalTerminatingSingleThreadTimeout / keyedPool.size(); // milliseconds

        long elapsedTime = 0;
        Iterator<Future<?>> iterator = futures.iterator();
        while (iterator.hasNext()) {
            Future<?> future = iterator.next();
            long time = System.nanoTime();
            try {
                future.get(Math.max(timeoutTime - elapsedTime, 0), TimeUnit.MILLISECONDS);
                iterator.remove();
            } catch (InterruptedException | ExecutionException e) {
                Spool.logger.error(e.getMessage(), e);
                throw new SpoolException("Pool waiting failed.");
            } catch (TimeoutException e) {
                Spool.logger.warn("Keyed pool ({}) did not finish all tasks in time!", name);
                break;
            }
            elapsedTime += TimeUnit.MILLISECONDS.convert(System.nanoTime() - time, TimeUnit.NANOSECONDS);
        }
        timeWaiting = TimeUnit.NANOSECONDS.convert(elapsedTime, TimeUnit.MILLISECONDS);
        if (!futures.isEmpty()) {
            if (ThreadManagerConfig.dropTasksOnTimeout) {
                Spool.logger.warn("Pool ({}) dropped {} updates.", name, futures.size());
                futures.forEach((a) -> a.cancel(true));
                futures.clear();
            } else Spool.logger.warn(
                "Pool ({}) overflowed {} updates, they will be executed whenever possible to avoid dropping updates.",
                name,
                futures.size());
        }
        if (DebugConfig.debug) {
            timeExecuting = timeSpentExecuting.getAndSet(0);
            timeOverhead = overhead.getAndSet(0);
        }
    }

    // The below functions don't really make sense in this context,
    // especially considering there is no real difference between "running" and "stopped" here.
    // The only difference is that the `keyedPool` will be empty when it's "stopped".

    public boolean isStarted() {
        return getKeys().contains(DEFAULT_THREAD_KEY);
    }

    public void startPool() {
        if (DebugConfig.debugLogging)
            Spool.logger.info("Starting pool ({}) with {} threads.", this.getName(), this.getNumThreads() + 1);
        if (this.isStarted()) throw new SpoolException("Pool already started (" + this.getName() + ")!");
        addKeyedThread(DEFAULT_THREAD_KEY, name + "-default");
    }
}
