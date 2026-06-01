package com.gamma.spool.compat;

import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public interface IChunkCompat {

    ExtendedBlockStorage checkAndCreateSubchunk(int index);
}
