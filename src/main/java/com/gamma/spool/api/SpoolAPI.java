package com.gamma.spool.api;

import com.gamma.spool.Spool;

/*
 * TODO: The below.
 * This API should allow other mods to:
 * 1. Toggle features inside Spool.
 * 2. View some internal statistics for Spool performance measurements.
 * 3. View the current state of Spool's initialization.
 * 4. View certain statistics about Spool's caches.
 */
@SuppressWarnings("unused")
public class SpoolAPI {

    public static boolean isInitialized() {
        return Spool.isInitialized;
    }

}
