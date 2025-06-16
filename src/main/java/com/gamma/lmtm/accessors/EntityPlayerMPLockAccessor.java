package com.gamma.lmtm.accessors;

import java.util.concurrent.locks.ReentrantLock;

public interface EntityPlayerMPLockAccessor {

    ReentrantLock lmtm$getLock();
}
