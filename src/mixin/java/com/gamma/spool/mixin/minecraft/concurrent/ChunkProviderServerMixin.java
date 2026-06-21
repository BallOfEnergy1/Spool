package com.gamma.spool.mixin.minecraft.concurrent;

import java.util.List;
import java.util.Set;

import net.minecraft.util.LongHashMap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gamma.spool.util.NonBlockingMCLongHashMap;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

@Mixin(ChunkProviderServer.class)
public abstract class ChunkProviderServerMixin {

    @Shadow
    public IChunkProvider currentChunkProvider;

    @Shadow
    public LongHashMap loadedChunkHashMap;

    @Shadow
    public List<Chunk> loadedChunks;

    @Shadow
    private Set<Long> chunksToUnload;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        this.loadedChunkHashMap = new NonBlockingMCLongHashMap<Chunk>();
        this.loadedChunks = ObjectLists.synchronize(new ObjectArrayList<>());
        this.chunksToUnload = LongSets.synchronize(new LongOpenHashSet());
    }

    @WrapMethod(method = "unloadQueuedChunks")
    private boolean wrappedUnloadQueuedChunks(Operation<Boolean> original) {
        // synchronize on top of the built-in sync for Hodgepodge's sake.
        synchronized (this.chunksToUnload) {
            return original.call();
        }
    }

    @WrapMethod(method = "originalLoadChunk", remap = false)
    private Chunk wrappedOriginalLoadChunk(int x, int z, Operation<Chunk> original) {
        synchronized (this.currentChunkProvider) {
            return original.call(x, z);
        }
    }

    @WrapMethod(method = "populate")
    private void wrappedPopulate(IChunkProvider provider, int x, int z, Operation<Void> original) {
        synchronized (this.currentChunkProvider) {
            original.call(provider, x, z);
        }
    }
}
