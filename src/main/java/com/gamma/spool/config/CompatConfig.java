package com.gamma.spool.config;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = "spool", category = "Compatibility")
@Config.Comment("Spool's compatibility config.")
@Config.RequiresMcRestart
public class CompatConfig {

    @Config.Comment("If Spool's ASM checks should be run. These checks can slightly increase mod loading times, however they increase inter-mod compatibility significantly. They can, however, reveal incompatibilities with Spool's ASM system. Do not rely on this to fix all incompatibilities.")
    @Config.DefaultBoolean(true)
    @Config.Name("Enable ASM checks?")
    public static boolean enableASMChecks;

    @Config.Comment("Allows Spool to use FQCN (Fully-Qualified Class Name) checks in order to determine if some mods are installed early in the mod lifecycle. This may cause issues with some mods if they change classloaders (sudden `java.lang.ClassNotFoundException` on launch).")
    @Config.DefaultBoolean(true)
    @Config.Name("Enable FQCN compatibility checks?")
    public static boolean enableFQCNChecks;

    @Config.Comment("ModIDs that Spool should *always* register as loaded. This should only be used if Spool does not properly detect a mod that you have installed (that Spool has explicit compatibility for).")
    @Config.Name("Force-loaded modIDs")
    public static String[] forceLoadedModIDs;

    // TODO: Enable this when we have more checks.
    // @Config.Comment("The list of enabled Spool ASM checks (remove an entry to disable a check). The current supported
    // checks are: \"UNSAFE_ITERATOR\".")
    // @Config.DefaultStringList({"a", "b"})
    // @Config.Name("Enabled Spool ASM checks")
    // public static boolean dropTasksOnTimeout;
}
