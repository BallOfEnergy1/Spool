package com.gamma.spool.config;

import com.gtnewhorizon.gtnhlib.config.Config;

@SuppressWarnings("unused")
@Config(modid = "spool", category = "Concurrency")
@Config.Comment("Spool's world concurrency config.")
@Config.RequiresMcRestart
public class ConcurrentConfig {

    @Config.Comment("Enables world concurrency. This option can break a LOT of mods that attempt to modify/mixin core Minecraft classes, though this option greatly increases performance and allows for threaded/concurrent world access.")
    @Config.DefaultBoolean(true)
    @Config.Name("Enable concurrent world?")
    public static boolean enableConcurrentWorldAccess;
}
