package com.gamma.spool.concurrent.providers;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;

import com.gamma.spool.concurrent.ConcurrentChunk;
import com.gamma.spool.config.ConcurrentConfig;
import com.gamma.spool.util.concurrent.interfaces.IConcurrent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

@SideOnly(Side.CLIENT)
@SuppressWarnings("unused")
public class ConcurrentChunkProviderClient extends ChunkProviderClient implements IConcurrent {

    private final Long2ObjectMap<Chunk> chunkMapping;
    private final boolean enableRWLock;

    private final ReentrantReadWriteLock lock;

    @Override
    public ReentrantReadWriteLock getLock() {
        return lock;
    }

    public ConcurrentChunkProviderClient(World p_i1184_1_) {
        super(p_i1184_1_);
        if (ConcurrentConfig.enableRWLockChunkProvider) {
            enableRWLock = true;
            chunkMapping = new Long2ObjectOpenHashMap<>();
            lock = new ReentrantReadWriteLock();
        } else {
            enableRWLock = false;
            chunkMapping = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());
            lock = null;
        }
    }

    /**
     * Unload chunk from ChunkProviderClient's hashmap. Called in response to a Packet50PreChunk with its mode field set
     * to false
     */
    public void unloadChunk(int p_73234_1_, int p_73234_2_) {
        Chunk chunk = this.provideChunk(p_73234_1_, p_73234_2_);

        if (!chunk.isEmpty()) {
            chunk.onChunkUnload();
        }

        if (enableRWLock) {
            writeLock();
            try {
                this.chunkMapping.remove(ChunkCoordIntPair.chunkXZ2Int(p_73234_1_, p_73234_2_));
            } finally {
                writeUnlock();
            }
        } else {
            this.chunkMapping.remove(ChunkCoordIntPair.chunkXZ2Int(p_73234_1_, p_73234_2_));
        }
    }

    /**
     * loads or generates the chunk at the chunk location specified
     */
    public Chunk loadChunk(int p_73158_1_, int p_73158_2_) {
        ConcurrentChunk chunk = new ConcurrentChunk(this.worldObj, p_73158_1_, p_73158_2_);
        if (enableRWLock) {
            writeLock();
            try {
                this.chunkMapping.put(ChunkCoordIntPair.chunkXZ2Int(p_73158_1_, p_73158_2_), chunk);
            } finally {
                writeUnlock();
            }
        } else {
            this.chunkMapping.put(ChunkCoordIntPair.chunkXZ2Int(p_73158_1_, p_73158_2_), chunk);
        }
        MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load(chunk));
        chunk.isChunkLoaded.set(true);
        return chunk;
    }

    /**
     * Will return back a chunk, if it doesn't exist and its not a MP client it will generates all the blocks for the
     * specified chunk from the map seed and chunk seed
     */
    public Chunk provideChunk(int p_73154_1_, int p_73154_2_) {
        Chunk chunk;
        if (enableRWLock) {
            readLock();
            try {
                chunk = this.chunkMapping.get(ChunkCoordIntPair.chunkXZ2Int(p_73154_1_, p_73154_2_));
            } finally {
                readUnlock();
            }
        } else {
            chunk = this.chunkMapping.get(ChunkCoordIntPair.chunkXZ2Int(p_73154_1_, p_73154_2_));
        }
        return chunk == null ? this.blankChunk : chunk;
    }

    /**
     * Unloads chunks that are marked to be unloaded. This is not guaranteed to unload every such chunk.
     */
    public boolean unloadQueuedChunks() {
        long i = System.currentTimeMillis();

        if (enableRWLock) {
            readLock();
            try {
                for (Chunk chunk : this.chunkMapping.values()) {
                    chunk.func_150804_b(System.currentTimeMillis() - i > 5L);
                }
            } finally {
                readUnlock();
            }
        } else {
            synchronized (chunkMapping) {
                for (Chunk chunk : this.chunkMapping.values()) {
                    chunk.func_150804_b(System.currentTimeMillis() - i > 5L);
                }
            }
        }

        if (System.currentTimeMillis() - i > 100L) {
            logger.info("Warning: Clientside chunk ticking took {} ms", System.currentTimeMillis() - i);
        }

        return false;
    }

    /**
     * Converts the instance data to a readable string.
     */
    public String makeString() {
        if (enableRWLock) {
            readLock();
            try {
                return "MultiplayerChunkCache: " + this.chunkMapping.size();
            } finally {
                readUnlock();
            }
        } else {
            return "MultiplayerChunkCache: " + this.chunkMapping.size();
        }
    }

    public int getLoadedChunkCount() {
        if (enableRWLock) {
            readLock();
            try {
                return this.chunkMapping.size();
            } finally {
                readUnlock();
            }
        } else {
            return this.chunkMapping.size();
        }
    }
}
