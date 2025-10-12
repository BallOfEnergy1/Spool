package com.gamma.spool.concurrent.providers;

import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;

import org.jctools.maps.NonBlockingHashMapLong;

import com.gamma.spool.compat.endlessids.ConcurrentChunkWrapper;
import com.gamma.spool.concurrent.ConcurrentChunk;
import com.gamma.spool.core.SpoolCompat;
import com.gamma.spool.util.concurrent.interfaces.IAtomic;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@SuppressWarnings("unused")
public class ConcurrentChunkProviderClient extends ChunkProviderClient implements IAtomic {

    private final NonBlockingHashMapLong<Chunk> chunkMapping = new NonBlockingHashMapLong<>();

    public ConcurrentChunkProviderClient(World p_i1184_1_) {
        super(p_i1184_1_);
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

        this.chunkMapping.remove(ChunkCoordIntPair.chunkXZ2Int(p_73234_1_, p_73234_2_));
    }

    /**
     * loads or generates the chunk at the chunk location specified
     */
    public Chunk loadChunk(int p_73158_1_, int p_73158_2_) {
        ConcurrentChunk chunk;
        if (SpoolCompat.isEndlessIDsLoaded) {
            chunk = new ConcurrentChunkWrapper(this.worldObj, p_73158_1_, p_73158_2_);
        } else {
            chunk = new ConcurrentChunk(this.worldObj, p_73158_1_, p_73158_2_);
        }

        this.chunkMapping.put(ChunkCoordIntPair.chunkXZ2Int(p_73158_1_, p_73158_2_), chunk);

        MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load(chunk));
        chunk.isChunkLoaded.set(true);
        return chunk;
    }

    /**
     * Will return back a chunk, if it doesn't exist and its not a MP client it will generates all the blocks for the
     * specified chunk from the map seed and chunk seed
     */
    public Chunk provideChunk(int p_73154_1_, int p_73154_2_) {
        Chunk chunk = this.chunkMapping.get(ChunkCoordIntPair.chunkXZ2Int(p_73154_1_, p_73154_2_));
        return chunk == null ? this.blankChunk : chunk;
    }

    /**
     * Unloads chunks that are marked to be unloaded. This is not guaranteed to unload every such chunk.
     */
    public boolean unloadQueuedChunks() {
        long i = System.currentTimeMillis();

        for (Chunk chunk : this.chunkMapping.values()) {
            chunk.func_150804_b(System.currentTimeMillis() - i > 5L);
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
        return "MultiplayerChunkCache: " + this.chunkMapping.size();
    }

    public int getLoadedChunkCount() {
        return this.chunkMapping.size();
    }
}
