package com.gamma.spool.config;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = "spool", category = "implementation")
@Config.Comment("Spool's implementation config.")
public class ImplConfig {

    @Config.Comment("Enables Spool to use Java 8 Unsafe (if compatibility is detected) to improve performance under thread contention (volatiles, fencing, etc.).")
    @Config.DefaultBoolean(true)
    @Config.Name("Use Java 8 Unsafe?")
    public static boolean useUnsafe;

    @Config.Comment("Enables Spool to use Java 9+ features if compatibility is detected (LWJGL3ify). This option is currently unused and is meant for future additions.")
    @Config.DefaultBoolean(true)
    @Config.Name("Use Java 9+ features?")
    public static boolean useJava9Features;

    @Config.Comment("If Spool should prioritize memory efficiency over speed, sometimes sacrificing performance for a large reduction in RAM usage by using \"compact\" implementations.")
    @Config.DefaultBoolean(false)
    @Config.Name("Use \"compact\" implementations?")
    public static boolean useCompactImpls;
}
