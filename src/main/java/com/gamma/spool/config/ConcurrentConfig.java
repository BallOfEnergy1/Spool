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

    @Config.Comment("Enables the use of Read/Write locks on the concurrent implementations of chunk providers (specifically the ChunkProviderServer). This can lead to better performance with larger servers, though may increase overhead when it comes to smaller worlds/servers.")
    @Config.DefaultBoolean(false)
    @Config.Name("Enable Read/Write locks on chunk providers?")
    public static boolean enableRWLockChunkProvider;
}
