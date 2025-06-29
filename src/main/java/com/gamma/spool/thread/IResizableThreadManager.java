package com.gamma.spool.thread;

public interface IResizableThreadManager extends IThreadManager {

    /**
     * Safely resizes the thread manager's pool without dropping any tasks.
     * 
     * @param threads Number of threads to resize the pool to.
     */
    void resize(int threads);
}
