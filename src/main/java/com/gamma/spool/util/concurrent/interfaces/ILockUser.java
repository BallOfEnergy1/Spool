package com.gamma.spool.util.concurrent.interfaces;

public interface ILockUser extends IThreadSafe {

    void readLock();

    void readUnlock();

    void writeLock();

    void writeUnlock();
}
