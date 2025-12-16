package com.gamma.spool.config;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = "spool", category = "Debug")
@Config.Comment("Spool's debug config.")
public class DebugConfig {

    @Config.Comment("Enables debug (`/spool stats` (GUI) menu).")
    @Config.DefaultBoolean(false)
    @Config.Name("Enable debug mode (GUI)?")
    public static boolean debug;

    @Config.Comment("Enables debug (console logging).")
    @Config.DefaultBoolean(false)
    @Config.Name("Enable debug mode (console logging)?")
    public static boolean debugLogging;

    @Config.Comment("Enables compatibility debug (console logging).")
    @Config.DefaultBoolean(false)
    @Config.Name("Enable compatibility debug mode (console logging)?")
    public static boolean compatLogging;

    @Config.Comment("Enables full compatibility debug (complete SDB logging).")
    @Config.DefaultBoolean(false)
    @Config.Name("Enable full compatibility debug mode (complete SDB logging)?")
    public static boolean fullCompatLogging;

    @Config.Comment("Allows SDB socket connections on `localhost:7655`.")
    @Config.DefaultBoolean(false)
    @Config.Name("Allow SDB connections?")
    public static boolean allowSDBConnections;

    @Config.Comment("Maximum amount of connections to the SDB server before connections are denied.")
    @Config.DefaultInt(2)
    @Config.RangeInt(min = 1, max = 16)
    @Config.Name("Max # of SDB connections")
    public static int maxSDBConnections;

    @Config.Comment("Enables ASM debug (console logging).")
    @Config.DefaultBoolean(false)
    @Config.Name("Enable ASM debug mode (console logging)?")
    public static boolean logASM;
}
