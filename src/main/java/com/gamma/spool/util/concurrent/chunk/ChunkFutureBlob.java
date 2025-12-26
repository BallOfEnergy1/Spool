package com.gamma.spool.util.concurrent.chunk;

import net.minecraft.world.chunk.Chunk;

public class ChunkFutureBlob extends DataBlob<ChunkFutureTask> {

    public ChunkFutureBlob(int centerX, int centerZ) {
        super(centerX, centerZ);
    }

    public boolean isCoordinateWithinBlob(long hash) {
        return isCoordinateWithinBlob(ChunkCoordUtil.getPackedX(hash), ChunkCoordUtil.getPackedZ(hash));
    }

    public ChunkFutureTask getDataAtCoordinate(long hash) {
        return getDataAtCoordinate(ChunkCoordUtil.getPackedX(hash), ChunkCoordUtil.getPackedZ(hash));
    }

    public void addToBlob(int x, int z, Chunk data) {
        super.addToBlob(x, z, new ChunkFutureTask(data));
    }

    public void removeFromBlob(long hash) {
        removeFromBlob(ChunkCoordUtil.getPackedX(hash), ChunkCoordUtil.getPackedZ(hash));
    }

    public ChunkFutureTask[] getChunksInBlob() {
        return getDataInBlob().toArray(new ChunkFutureTask[0]);
    }
}
