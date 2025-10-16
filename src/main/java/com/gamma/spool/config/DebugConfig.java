package com.gamma.spool.config;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = "spool", category = "Debug")
@Config.Comment("Spool's debug config.")
public class DebugConfig {

    @Config.Comment("Enables debug (F3 menu).")
    @Config.DefaultBoolean(false)
    @Config.Name("Enable debug mode (F3 menu)?")
    public static boolean debug;

    @Config.Comment("Enables debug (console logging).")
    @Config.DefaultBoolean(false)
    @Config.Name("Enable debug mode (console logging)?")
    public static boolean debugLogging;

    @Config.Comment("Enables compatibility debug (console logging).")
    @Config.DefaultBoolean(false)
    @Config.Name("Enable compatibility debug mode (console logging)?")
    public static boolean compatLogging;

    @Config.Comment("Enables full compatibility debug (complete file logging).")
    @Config.DefaultBoolean(false)
    @Config.Name("Enable full compatibility debug mode (complete file logging)?")
    public static boolean fullCompatLogging;

    @Config.Comment("Enables ASM debug (console logging).")
    @Config.DefaultBoolean(false)
    @Config.Name("Enable ASM debug mode (console logging)?")
    public static boolean logASM;
}
