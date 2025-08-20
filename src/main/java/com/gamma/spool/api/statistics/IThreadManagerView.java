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
     * Calculates and retrieves the total overhead time spent by the thread pool
     * during its operations. This could include time spent in task submission,
     * management, or any other non-execution-related activities.
     *
     * @return the overhead time in nanoseconds.
     */
    long getTimeOverhead();

    /**
     * Retrieves the total time spent executing tasks in the thread pool.
     *
     * @return the total execution time in nanoseconds.
     */
    long getTimeExecuting();

    /**
     * Checks if the thread pool is currently initialized and started.
     *
     * @return true if the thread pool is started; false otherwise.
     */
    boolean isStarted();
}
