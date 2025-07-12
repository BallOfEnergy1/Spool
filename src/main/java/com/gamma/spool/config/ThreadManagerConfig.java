package com.gamma.spool.config;

import com.gtnewhorizon.gtnhlib.config.Config;

@SuppressWarnings("unused")
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
    @Config.DefaultBoolean(true)
    @Config.Name("Use lambda optimization?")
    public static boolean useLambdaOptimization;

    @Config.Comment("If the managers should use an enhanced method of profiling in order to provide more information to Minecraft's built-in profiler about threads. This is only required for extra info when using the `/debug` command, or when other mods use the server's profiler to get statistics.")
    @Config.DefaultBoolean(false)
    @Config.Name("Use better task profiling?")
    public static boolean betterTaskProfiling;

    @Config.Comment("If updates should be able to roll over into the thread sleep time. Works better for a few heavy ticks every few ticks, but does next to nothing for constant load. Can cause instabilities with mods that expect all updates to be finished before the FML Post-tick event.")
    @Config.DefaultBoolean(false)
    @Config.Name("Allow processing during sleep?")
    public static boolean allowProcessingDuringSleep;
}
