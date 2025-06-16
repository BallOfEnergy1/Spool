package com.gamma.lmtm.accessors;

import java.util.concurrent.locks.ReentrantLock;

public interface ChunkProviderServerLockAccessor {

    ReentrantLock lmtm$getLock();
}
