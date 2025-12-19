package com.gamma.spool.api.statistics;

/**
 * A base interface for a view-only IThreadManager. This can still be cast to
 * the normal IThreadManager interface, so care still needs to be taken with this.
 * <b>Messing with the manager's values directly can cause massive issues in Spool!</b>
 */
public interface IThreadManagerView {

    /**
     * Retrieves the name of the thread manager.
     *
     * @return the name of the thread manager as a String.
     */
    String getName();

    /**
     * Retrieves the current number of threads in the thread pool.
     *
     * @return the current number of threads in the thread pool.
     */
    int getNumThreads();

    /**
     * Retrieves the total time spent waiting for tasks to be executed in the thread pool.
     *
     * @return the total waiting time in nanoseconds.
     */
    long getTimeWaiting();

    /**
     * Retrieves the total time spent waiting for tasks to be executed in the thread pool.
     * This is a rolling average over the last 100 values.
     *
     * @return the total waiting time in nanoseconds.
     */
    long getAvgTimeWaiting();

    /**
     * Calculates and retrieves the total overhead time spent by the thread pool
     * during its operations. This could include time spent in task submission,
     * management, or any other non-execution-related activities.
     *
     * @return the overhead time in nanoseconds.
     */
    long getTimeOverhead();

    /**
     * Calculates and retrieves the total overhead time spent by the thread pool
     * during its operations. This could include time spent in task submission,
     * management, or any other non-execution-related activities.
     * This is a rolling average over the last 100 values.
     *
     * @return the overhead time in nanoseconds.
     */
    long getAvgTimeOverhead();

    /**
     * Retrieves the total time spent executing tasks in the thread pool.
     *
     * @return the total execution time in nanoseconds.
     */
    long getTimeExecuting();

    /**
     * Retrieves the total time spent executing tasks in the thread pool.
     * This is a rolling average over the last 100 values.
     *
     * @return the total execution time in nanoseconds.
     */
    long getAvgTimeExecuting();

    /**
     * Checks if the thread pool is currently initialized and started.
     *
     * @return true if the thread pool is started; false otherwise.
     */
    boolean isStarted();

    /**
     * If the thread manager's pool is currently disabled.
     *
     * @return true if the thread pool is disabled; false otherwise.
     */
    boolean isPoolDisabled();
}
