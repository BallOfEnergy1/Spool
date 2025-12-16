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

    @Config.Comment("Radius in which chunks will be \"blobbed\" together. This can increase performance with large amounts of chunk accesses in an area, but can increase memory usage with larger values.")
    @Config.DefaultInt(5)
    @Config.RangeInt(min = 1, max = 512)
    @Config.Name("Chunk blobbing radius")
    public static int chunkBlobbingRadius;
}
