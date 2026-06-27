package com.gamma.spool.util;

import java.util.concurrent.locks.ReadWriteLock;

public interface IChunkLockAccessor {

    ReadWriteLock getStorageArraysLock();
}
