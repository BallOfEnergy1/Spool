package com.gamma.spool.util.concurrent.chunk;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import net.minecraft.world.chunk.Chunk;

import org.spongepowered.libraries.com.google.common.util.concurrent.Runnables;

public class ChunkFutureTask extends FutureTask<Chunk> {

    public final boolean isInstant;

    public ChunkFutureTask(Callable<Chunk> runnable) {
        super(runnable);
        isInstant = false;
    }

    public ChunkFutureTask(Chunk toReturn) {
        super(Runnables.doNothing(), toReturn);
        set(toReturn);
        isInstant = true;
    }
}
