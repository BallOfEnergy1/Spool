package com.gamma.spool.thread;

/**
 * Interface that extends {@link IThreadManager} to provide functionality for resizing
 * the thread pool dynamically. Any class extending this should ensure that:
 * <ol>
 * <li>Tasks will *never* be dropped while resizing.</li>
 * <li>The *manager* should not be blocked from accepting new tasks while resizing.</li>
 * </ol>
 */
public interface IResizableThreadManager extends IThreadManager {

    /**
     * Safely resizes the thread manager's pool without dropping any tasks.
     *
     * @param threads Number of threads to resize the pool to.
     */
    void resize(int threads);
}
