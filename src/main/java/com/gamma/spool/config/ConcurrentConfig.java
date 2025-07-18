package com.gamma.spool.config;

import com.gtnewhorizon.gtnhlib.config.Config;

@SuppressWarnings("unused")
@Config(modid = "spool", category = "Concurrency")
@Config.RequiresMcRestart
@Config.Comment("Spool's world concurrency config.")
public class ConcurrentConfig {

    @Config.Comment("Enables world concurrency. This option can break a LOT of mods that attempt to modify/mixin core Minecraft classes, though this option greatly increases performance and allows for threaded/concurrent world access.")
    @Config.DefaultBoolean(false)
    @Config.Name("Enable concurrent world?")
    public static boolean enableConcurrentWorldAccess;
}
