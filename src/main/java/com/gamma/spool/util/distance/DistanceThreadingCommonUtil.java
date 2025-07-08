package com.gamma.spool.util.distance;

import net.minecraft.server.MinecraftServer;

import com.gamma.spool.config.DistanceThreadingConfig;

public class DistanceThreadingCommonUtil {

    static int getDistanceLimit() {
        int limit = MinecraftServer.getServer()
            .getConfigurationManager()
            .getViewDistance() * 2;
        int configLimit = DistanceThreadingConfig.threadChunkDistance;
        if (limit > configLimit && configLimit != 0) throw new IllegalArgumentException(
            "View distance is too high for threadChunkDistance! " + limit + " > " + configLimit);
        else if (configLimit == 0) configLimit = limit;
        return configLimit;
    }

    public static long addToChunkHash(long hash, int x, int z) {
        return hash + ((long) x & 4294967295L | ((long) z & 4294967295L) << 32);
    }
}
