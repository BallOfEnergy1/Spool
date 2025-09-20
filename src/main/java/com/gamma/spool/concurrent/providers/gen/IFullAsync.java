package com.gamma.spool.concurrent.providers.gen;

import net.minecraft.world.chunk.Chunk;

import com.gamma.spool.util.concurrent.interfaces.IThreadSafe;

public interface IFullAsync extends IThreadSafe {

    /**
     * Fully async version of `provideChunk()`.
     * 
     * @param x X-Coordinate of chunk to provide.
     * @param y Y-Coordinate of chunk to provide.
     * @return The provided chunk.
     */
    Chunk provideChunkAsync(final int x, final int y);
}
