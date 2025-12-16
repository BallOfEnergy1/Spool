package com.gamma.spool.util.concurrent.chunk;

import net.minecraft.world.chunk.Chunk;

import com.mitchej123.hodgepodge.util.ChunkPosUtil;

public class ChunkFutureBlob extends DataBlob<ChunkFutureTask> {

    public ChunkFutureBlob(int centerX, int centerZ) {
        super(centerX, centerZ);
    }

    public boolean isCoordinateWithinBlob(long hash) {
        return isCoordinateWithinBlob(ChunkPosUtil.getPackedX(hash), ChunkPosUtil.getPackedZ(hash));
    }

    public ChunkFutureTask getDataAtCoordinate(long hash) {
        return getDataAtCoordinate(ChunkPosUtil.getPackedX(hash), ChunkPosUtil.getPackedZ(hash));
    }

    public void addToBlob(int x, int z, Chunk data) {
        super.addToBlob(x, z, new ChunkFutureTask(data));
    }

    public void removeFromBlob(long hash) {
        removeFromBlob(ChunkPosUtil.getPackedX(hash), ChunkPosUtil.getPackedZ(hash));
    }

    public ChunkFutureTask[] getChunksInBlob() {
        return getDataInBlob().toArray(new ChunkFutureTask[0]);
    }
}
