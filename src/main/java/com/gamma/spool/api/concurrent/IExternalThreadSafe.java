package com.gamma.spool.api.concurrent;

/**
 * This interface can be implemented in external classes if they are
 * <i>intended to be thread-safe</i>. This is the API equivalent of
 * {@link com.gamma.spool.util.concurrent.interfaces.IThreadSafe}.
 * <p>
 * Spool can detect implementors of this interface and change
 * its execution process as needed to increase performance with
 * other mods.
 * </p>
 * <p>
 * This won't always be needed, though there are a few special
 * situations where it is very performant to do so. These
 * primarily include the modded chunk providers (for other
 * dimensions).
 * </p>
 * <p>
 * It is up to the implementing class to decide the locking
 * mechanism to use (or a lock-free architecture). As long as
 * their methods can be executed concurrently with each other
 * without issues, it can implement this interface.
 * </p>
 */
public interface IExternalThreadSafe {
    // Not really much of anything here...
    // I'll put some utility functions here eventually (maybe...)
}
