package com.gamma.spool.mixin.minecraft.concurrent;

import static com.gamma.spool.util.concurrent.chunk.BlobConstants.*;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.chunkio.ChunkIOProvider;
import net.minecraftforge.common.chunkio.QueuedChunk;
import net.minecraftforge.event.world.ChunkDataEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gamma.spool.concurrent.providers.ConcurrentChunkProviderServer;
import com.gamma.spool.util.concurrent.chunk.ChunkFutureBlob;

@Mixin(ChunkIOProvider.class)
public abstract class ChunkIOProviderMixin {

    // Originally this was intended to be a synchronous call, so now I have to fix this BS :(
    @Inject(method = "callStage2*", at = @At("HEAD"), remap = false, cancellable = true)
    public void callStage2Inject(QueuedChunk queuedChunk, Chunk chunk, CallbackInfo ci) throws RuntimeException {
        if (chunk == null) {
            // If the chunk loading failed just do it synchronously (may generate)
            queuedChunk.provider.originalLoadChunk(queuedChunk.x, queuedChunk.z);
            return;
        }

        queuedChunk.loader.loadEntities(queuedChunk.world, queuedChunk.compound.getCompoundTag("Level"), chunk);
        MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Load(chunk, queuedChunk.compound)); // Don't call
        // ChunkDataEvent.Load
        // async
        chunk.lastSaveTime = queuedChunk.provider.worldObj.getTotalWorldTime();

        long k = ChunkCoordIntPair.chunkXZ2Int(clampToGrid(queuedChunk.x), clampToGrid(queuedChunk.z));
        ConcurrentChunkProviderServer castedProvider = ((ConcurrentChunkProviderServer) queuedChunk.provider);

        ChunkFutureBlob blob;
        if ((blob = castedProvider.concurrentLoadedChunkHashMap.get(k)) == null) {
            castedProvider.concurrentLoadedChunkHashMap
                .put(k, blob = new ChunkFutureBlob(clampToGrid(queuedChunk.x), clampToGrid(queuedChunk.z)));
        }
        blob.addToBlob(queuedChunk.x, queuedChunk.z, chunk);
        castedProvider.invalidateCache();

        chunk.onChunkLoad();

        if (queuedChunk.provider.currentChunkProvider != null) {
            queuedChunk.provider.currentChunkProvider.recreateStructures(queuedChunk.x, queuedChunk.z);
        }

        chunk.populateChunk(queuedChunk.provider, queuedChunk.provider, queuedChunk.x, queuedChunk.z);
        ci.cancel();
    }
}
