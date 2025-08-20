package com.gamma.spool.config;

import com.gtnewhorizon.gtnhlib.config.Config;

@SuppressWarnings("unused")
@Config(modid = "spool", category = "API")
@Config.Comment("SpoolAPI config.")
public class APIConfig {

    @Config.Comment("Enables other mods to collect statistics about Spool for performance metrics. This can sometimes reduce performance, however it's unlikely to reduce it by much.")
    @Config.DefaultBoolean(true)
    @Config.Name("Allow statistics gathering (other mods)?")
    public static boolean statisticGatheringAllowed;
}
