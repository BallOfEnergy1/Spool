package com.gamma.spool.thread;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.gamma.spool.api.statistics.IThreadManagerView;

/**
 * Interface for managing a thread pool and executing tasks. Provides methods to monitor
 * and control the execution of tasks and the lifecycle of the thread pool.
 */
public interface IThreadManager extends IThreadManagerView, Executor {

    /**
     * Initializes and starts the thread pool. The thread pool is configured with a fixed number
     * of threads.
     * This method ensures that the pool is properly started.
     * <p>
     * If the thread pool is already initialized, this method should throw a {@link IllegalStateException}.
     * <p>
     * It is essential to call this method before submitting any tasks to the thread pool to
     * ensure that the pool is properly initialized.
     *
     * @throws IllegalStateException if the pool has already been started.
     */
    void startPool();

    /**
     * Terminates the thread pool and releases its resources.
     * This method attempts to finish all tasks within the thread pool, then terminates the pool.
     * If the pool fails to terminate within the time specified in the config, it will forcefully shut down,
     * possibly dropping tasks.
     * <p>
     * Upon termination, the reference to the thread pool is cleared, effectively rendering the
     * thread manager unusable until it is restarted.
     *
     * @throws RuntimeException if the termination process is interrupted.
     */
    void terminatePool();

    /**
     * Executes the given task using the thread pool managed by the thread manager.
     *
     * @param task the task to be executed
     * @throws NullPointerException if the task is null.
     */
    @SuppressWarnings("NullableProblems")
    void execute(Runnable task);

    /**
     * Executes the given task using the thread pool managed by the thread manager.
     *
     * @param func the task to be executed
     * @param arg  The argument to be given to the consumer.
     * @throws NullPointerException if the task is null.
     */
    <A> void execute(Consumer<A> func, A arg);

    /**
     * Executes the given task using the thread pool managed by the thread manager.
     *
     * @param func the task to be executed
     * @param arg1 The first argument to be given to the consumer.
     * @param arg2 The second argument to be given to the consumer.
     * @throws NullPointerException if the task is null.
     */
    <A, B> void execute(BiConsumer<A, B> func, A arg1, B arg2);

    /**
     * Waits until all currently submitted tasks in the thread pool are completed.
     *
     * @param timeout a boolean indicating whether the running or terminating
     *                timeout should be applied while waiting for tasks to complete.
     *                If {@code false}, the termination time config option will be used
     *                for the timeout time instead of the running time.
     */
    void waitUntilAllTasksDone(boolean timeout);

    /**
     * Disables the thread manager's pool and forces all tasks to run on the current thread.
     */
    void disablePool();

    /**
     * Enables the thread manager's pool and allows tasks to be run on pool threads.
     */
    void enablePool();

    /**
     * Waits until all currently submitted tasks in the thread pool are completed.
     * This method invokes {@link #waitUntilAllTasksDone(boolean)} with the default behavior
     * of applying the running timeout configuration.
     */
    default void waitUntilAllTasksDone() {
        this.waitUntilAllTasksDone(true);
    }

    /**
     * Ensures that the thread pool is started, initializing it if necessary.
     * This method checks whether the thread pool is already started by invoking {@link #isStarted()}.
     * If the thread pool is not running, it will be initialized and started by calling {@link #startPool()}.
     */
    default void startPoolIfNeeded() {
        if (!this.isStarted()) this.startPool();
    }

    default void cancelFuture(Future<?> future) {
        future.cancel(false);
    }
}
