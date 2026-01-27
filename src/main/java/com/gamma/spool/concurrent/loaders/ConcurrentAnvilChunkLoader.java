package com.gamma.spool.concurrent.loaders;

import java.io.File;

import net.minecraft.world.chunk.storage.AnvilChunkLoader;

import com.gamma.gammalib.util.concurrent.IThreadSafe;

@SuppressWarnings("unused")
public class ConcurrentAnvilChunkLoader extends AnvilChunkLoader implements IThreadSafe {

    public ConcurrentAnvilChunkLoader(File p_i2003_1_) {
        super(p_i2003_1_);
    }
}
