package com.gamma.spool.config;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = "spool", category = "Distance Threading")
@Config.Comment("Spool's distance threading config. This config is only used when the distance threading option is enabled.")
public class DistanceThreadingConfig {

    @Config.Comment("Distance (in chunks) between two players before being separated into different threads."
        + " Set to 0 to use view distance. If this is less than the view distance while set, Spool will crash!")
    @Config.DefaultInt(0)
    @Config.Name("Chunk distance limit")
    @Config.RangeInt(min = 0)
    public static int threadChunkDistance;

    @Config.Comment("If the manager should resolve executor conflicts (potentially unstable but will not crash), or if it should crash upon conflicts.")
    @Config.DefaultBoolean(true)
    @Config.Name("Resolve conflicts?")
    public static boolean resolveConflicts;

    @Config.Comment("How aggressively the manager should parallelize streams for forced chunks (good for large servers, may introduce overhead for smaller servers). A value of 0 disables stream parallelization entirely.")
    @Config.DefaultInt(0)
    @Config.Name("Stream parallelization level")
    @Config.RangeInt(min = 0, max = 2)
    public static int streamParallelizationLevel;
}
