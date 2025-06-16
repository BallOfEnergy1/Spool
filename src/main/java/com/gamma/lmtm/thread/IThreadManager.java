package com.gamma.lmtm.thread;

public interface IThreadManager {

    int getNumThreads();

    long getTimeWaiting();

    long getTimeOverhead();

    long getTimeExecuting();

    void startPool();

    void terminatePool();

    boolean isStarted();

    void startPoolIfNeeded();

    void execute(Runnable task);

    void waitUntilAllTasksDone(boolean timeout);

    default void waitUntilAllTasksDone() {
        this.waitUntilAllTasksDone(true);
    }
}
