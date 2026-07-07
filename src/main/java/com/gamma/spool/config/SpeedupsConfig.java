package com.gamma.spool.config;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = "spool", category = "Speedups")
@Config.Comment("Spool's speedups config. These may change game features for additional speedups.")
@Config.RequiresMcRestart
public class SpeedupsConfig {

    @Config.Comment("Whether or not squids should be allowed to spawn in the world. This can greatly speed up servers, as squids take up a relatively large amount of time.")
    @Config.DefaultBoolean(false)
    @Config.Name("Nuke squids?")
    public static boolean nukeSquids;
}
