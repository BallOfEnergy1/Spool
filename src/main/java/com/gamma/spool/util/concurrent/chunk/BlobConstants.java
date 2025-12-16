package com.gamma.spool.util.concurrent.chunk;

import net.minecraft.world.ChunkCoordIntPair;

import com.gamma.spool.config.ConcurrentConfig;
import com.gamma.spool.core.SpoolLogger;
import com.mitchej123.hodgepodge.util.ChunkPosUtil;

public class BlobConstants {

    public static final int RADIUS = ConcurrentConfig.chunkBlobbingRadius;
    public static final int DIAMETER = (RADIUS * 2) + 1;
    public static final int AREA = DIAMETER * DIAMETER;

    static {
        SpoolLogger.info("Chunk blob radius: " + RADIUS);
        SpoolLogger.info("Chunk blob diameter: " + DIAMETER);
        SpoolLogger.info("Chunk blob area: " + AREA);
    }

    public static int clampToGrid(int num) {
        return Math.round((float) num / BlobConstants.DIAMETER) * BlobConstants.DIAMETER;
    }

    public static long clampToGrid(long hash) {
        int x = ChunkPosUtil.getPackedX(hash);
        int z = ChunkPosUtil.getPackedZ(hash);
        return ChunkCoordIntPair.chunkXZ2Int(
            Math.round((float) x / BlobConstants.DIAMETER) * BlobConstants.DIAMETER,
            Math.round((float) z / BlobConstants.DIAMETER) * BlobConstants.DIAMETER);
    }
}
