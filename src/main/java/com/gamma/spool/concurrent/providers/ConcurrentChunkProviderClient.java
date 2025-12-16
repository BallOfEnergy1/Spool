package com.gamma.spool.concurrent.providers;

import static com.gamma.spool.util.concurrent.chunk.BlobConstants.clampToGrid;

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
import com.gamma.spool.util.concurrent.chunk.DataBlob;
import com.gamma.spool.util.concurrent.interfaces.IAtomic;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@SuppressWarnings("unused")
public class ConcurrentChunkProviderClient extends ChunkProviderClient implements IAtomic {

    private final NonBlockingHashMapLong<DataBlob<Chunk>> chunkMapping = new NonBlockingHashMapLong<>();

    private DataBlob<Chunk> cachedChunkBlob;

    public ConcurrentChunkProviderClient(World p_i1184_1_) {
        super(p_i1184_1_);
    }

    /**
     * Unload chunk from ChunkProviderClient's hashmap. Called in response to a Packet50PreChunk with its mode field set
     * to false
     */
    public void unloadChunk(int x, int z) {
        Chunk chunk = this.provideChunk(x, z);

        if (!chunk.isEmpty()) {
            chunk.onChunkUnload();
        }

        long k = ChunkCoordIntPair.chunkXZ2Int(clampToGrid(x), clampToGrid(z));

        if (cachedChunkBlob != null && cachedChunkBlob.isCoordinateWithinBlob(x, z)) {
            cachedChunkBlob.removeFromBlob(x, z);
        } else {
            DataBlob<Chunk> blob = this.chunkMapping.get(k);
            if (blob != null) blob.removeFromBlob(x, z);
            cachedChunkBlob = blob;
        }

        if (cachedChunkBlob != null && cachedChunkBlob.isBlobEmpty()) chunkMapping.remove(k);
    }

    /**
     * loads or generates the chunk at the chunk location specified
     */
    public Chunk loadChunk(int x, int z) {
        ConcurrentChunk chunk;
        if (SpoolCompat.isModLoaded("endlessids")) {
            chunk = new ConcurrentChunkWrapper(this.worldObj, x, z);
        } else {
            chunk = new ConcurrentChunk(this.worldObj, x, z);
        }

        if (cachedChunkBlob != null && cachedChunkBlob.isCoordinateWithinBlob(x, z)) {
            cachedChunkBlob.addToBlob(x, z, chunk);
        } else {
            long k = ChunkCoordIntPair.chunkXZ2Int(clampToGrid(x), clampToGrid(z));
            DataBlob<Chunk> blob;
            if ((blob = chunkMapping.get(k)) == null) {
                chunkMapping.put(k, blob = new DataBlob<>(clampToGrid(x), clampToGrid(z)));
            }
            blob.addToBlob(x, z, chunk);
            cachedChunkBlob = blob;
        }

        MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load(chunk));
        chunk.isChunkLoaded.set(true);
        return chunk;
    }

    /**
     * Will return back a chunk, if it doesn't exist and its not a MP client it will generates all the blocks for the
     * specified chunk from the map seed and chunk seed
     */
    public Chunk provideChunk(int x, int z) {
        Chunk chunk = null;

        if (cachedChunkBlob != null && cachedChunkBlob.isCoordinateWithinBlob(x, z)) {
            chunk = cachedChunkBlob.getDataAtCoordinate(x, z);
        } else {
            long k = ChunkCoordIntPair.chunkXZ2Int(clampToGrid(x), clampToGrid(z));
            DataBlob<Chunk> blob = this.chunkMapping.get(k);
            if (blob != null) chunk = blob.getDataAtCoordinate(x, z);
            cachedChunkBlob = blob;
        }

        return chunk == null ? this.blankChunk : chunk;
    }

    /**
     * Unloads chunks that are marked to be unloaded. This is not guaranteed to unload every such chunk.
     */
    public boolean unloadQueuedChunks() {
        long i = System.currentTimeMillis();

        for (DataBlob<Chunk> blob : this.chunkMapping.values()) {
            for (Chunk chunk : blob.getDataInBlob()) {
                chunk.func_150804_b(System.currentTimeMillis() - i > 5L);
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
        return "MultiplayerChunkCache: " + this.chunkMapping.size();
    }

    public int getLoadedChunkCount() {
        return this.chunkMapping.size();
    }

}
