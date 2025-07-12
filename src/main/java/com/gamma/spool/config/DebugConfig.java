package com.gamma.spool.config;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = "spool", category = "Debug")
@Config.Comment("Spool's debug config.")
public class DebugConfig {

    @Config.Comment("Enables debug mode (F3 menu).")
    @Config.DefaultBoolean(false)
    @Config.Name("Enable debug mode (F3 menu)?")
    public static boolean debug;

    @Config.Comment("Enables debug mode (console logging).")
    @Config.DefaultBoolean(false)
    @Config.Name("Enable debug mode (console logging)?")
    public static boolean debugLogging;
}
