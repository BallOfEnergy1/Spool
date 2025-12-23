package com.gamma.spool.util.concurrent.interfaces;

import com.gamma.spool.config.ImplConfig;
import com.gamma.spool.core.SpoolLogger;
import com.gamma.spool.unsafe.UnsafeAccessor;
import com.gamma.spool.util.fast.Java8Util;

import sun.misc.Unsafe;

/**
 * Interface representing atomic operations with enhanced performance using Unsafe.
 * <p>
 * Generally, atomics will need to use unsafe to match performance with native MC
 * classes, mostly due to the CAS looping. It is expected that classes that implement
 * this will only be initialized when a config option is enabled, mostly to prevent
 * the use of unsafe outside specific scenarios.
 */
public interface IAtomic extends IThreadSafe {

    default Unsafe getUnsafe() {
        if (!isUnsafeAvailable()) return null;
        return UnsafeAccessor.getUnsafe();
    }

    default boolean isUnsafeAvailable() {
        // Prevent ever initializing the class if the config is disabled.
        // Ensures that incompatible JVMs won't crash with this.
        if (!ImplConfig.useUnsafe) return false;

        if (UnsafeAccessor.IS_AVAILABLE) return true;
        if (UnsafeAccessor.ENABLED) {
            try {
                UnsafeAccessor.getUnsafe();
            } catch (Exception ignored) {
                UnsafeAccessor.disableUnsafe();
                SpoolLogger.error("Unsafe is unavailable, disabling...");
                return false;
            }
            return true;
        }
        return false;
    }

    default boolean isVarHandleAvailable() {
        return Java8Util.hasVarHandleSupport() && ImplConfig.useJava9Features;
    }
}
