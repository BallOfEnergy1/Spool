package com.gamma.spool.config;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = "spool", category = "Thread Managers")
@Config.Comment("Spool's thread manager config. This changes elements about the internal thread managers (globally across all threading methods).")
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

    @Config.Comment("If the managers should optimize lambdas (pulling scoped values) using Consumers. Sometimes increases performance, sometimes doesn't.")
    @Config.DefaultBoolean(false)
    @Config.Name("Use lambda optimization?")
    public static boolean useLambdaOptimization;
}
