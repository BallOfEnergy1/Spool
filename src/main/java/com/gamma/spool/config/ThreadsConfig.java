package com.gamma.spool.config;

import com.gtnewhorizon.gtnhlib.config.Config;

@SuppressWarnings("unused")
@Config(modid = "spool")
@Config.RequiresMcRestart
@Config.Comment("Spool's general threading config. This holds settings about threading types and numbers of threads.")
public class ThreadsConfig {

    @Config.Comment("Enables Spool experimental threading. This will disable distance threading if both are enabled.")
    @Config.DefaultBoolean(false)
    @Config.Name("Enable experimental threading?")
    public static boolean enableExperimentalThreading;

    @Config.Comment("Enables Spool's distance-based threading options. This is only really effective for servers where players are spread out large distances.")
    @Config.DefaultBoolean(false)
    @Config.Name("Enable distance-based threading?")
    public static boolean enableDistanceThreading;

    @Config.Comment("Enables Spool's dimension-based threading options. This is the simplest and most stable form of threading.")
    @Config.DefaultBoolean(true)
    @Config.Name("Enable dimension-based threading?")
    public static boolean enableDimensionThreading;

    @Config.Comment("Enables loading/generating chunks in a separate thread. This can be useful when loading large amounts of chunks, though it can potentially cause world-generation issues. This option currently may decrease performance.")
    @Config.DefaultBoolean(false)
    @Config.Name("Enable threaded chunk loading?")
    public static boolean enableThreadedChunkLoading;

    @Config.Comment("Number of threads to use for entity processing.")
    @Config.DefaultInt(4)
    @Config.Name("# Entity threads")
    @Config.RangeInt(min = 1, max = 16)
    public static int entityThreads;

    @Config.Comment("Number of threads to use for block processing.")
    @Config.DefaultInt(4)
    @Config.Name("# Block threads")
    @Config.RangeInt(min = 1, max = 16)
    public static int blockThreads;

    @Config.Comment("Maximum number of threads to use for distance-based threading (only used when distance-based threading is enabled).")
    @Config.DefaultInt(8)
    @Config.Name("# Distance-based threads")
    @Config.RangeInt(min = 1, max = 64)
    public static int distanceMaxThreads;

    @Config.Comment("Maximum number of threads to use for dimension processing (only used when experimental threading is disabled).")
    @Config.DefaultInt(4)
    @Config.Name("# Dimension threads")
    @Config.RangeInt(min = 1, max = 64)
    public static int dimensionMaxThreads;

    @Config.Comment("Number of threads to use for loading/generating chunks.")
    @Config.DefaultInt(1)
    @Config.Name("# Chunk loading threads")
    @Config.RangeInt(min = 1, max = 8)
    public static int chunkLoadingThreads;

    @Config.Ignore
    // Disables distance threading if something doesn't like it.
    public static boolean forceDisableDistanceThreading;

    public static boolean isExperimentalThreadingEnabled() {
        return enableExperimentalThreading && entityThreads >= 1 && blockThreads >= 1;
    }

    public static boolean shouldDistanceThreadingBeEnabled() {
        return enableDistanceThreading && !enableExperimentalThreading && distanceMaxThreads >= 1;
    }

    public static boolean shouldDistanceThreadingBeDisabled() {
        return enableDistanceThreading && (enableExperimentalThreading || distanceMaxThreads < 1);
    }

    public static boolean isDistanceThreadingEnabled() {
        return enableDistanceThreading && (!forceDisableDistanceThreading && !shouldDistanceThreadingBeDisabled());
    }

    public static boolean isDimensionThreadingEnabled() {
        return enableDimensionThreading && dimensionMaxThreads >= 1;
    }

    public static boolean isThreadedChunkLoadingEnabled() {
        return enableThreadedChunkLoading && chunkLoadingThreads >= 1;
    }
}
