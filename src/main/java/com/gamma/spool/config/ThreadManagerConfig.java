package com.gamma.spool.config;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = "spool", category = "threadManagers")
public class ThreadManagerConfig {

    @Config.Comment("Expiring time for thread pools while running.")
    @Config.DefaultInt(2000)
    @Config.Name("Running single thread timeout (ms)")
    public static int globalRunningSingleThreadTimeout;

    @Config.Comment("Expiring time for thread pools while terminating.")
    @Config.DefaultInt(50000)
    @Config.Name("Terminating single thread timeout (ms)")
    public static int globalTerminatingSingleThreadTimeout;

    @Config.Comment("If updates should be dropped when a pool times out.")
    @Config.DefaultBoolean(false)
    @Config.Name("Drop tasks on timeout?")
    public static boolean dropTasksOnTimeout;
}
