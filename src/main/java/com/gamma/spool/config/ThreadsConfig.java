package com.gamma.spool.config;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = "spool", category = "threads")
@Config.RequiresMcRestart
public class ThreadsConfig {

    @Config.Comment("Enables Spool experimental threading.")
    @Config.DefaultBoolean(false)
    @Config.Name("Enable experimental threading?")
    public static boolean enableExperimentalThreading;

    @Config.Comment("Maximum number of threads to use for dimension processing (only used when experimental threading is disabled).")
    @Config.DefaultInt(4)
    @Config.Name("# Dimension threads")
    public static int dimensionMaxThreads;

    @Config.Comment("Number of threads to use for entity processing.")
    @Config.DefaultInt(4)
    @Config.Name("# Entity threads")
    public static int entityThreads;

    @Config.Comment("Number of threads to use for block processing.")
    @Config.DefaultInt(4)
    @Config.Name("# Block threads")
    public static int blockThreads;
}
