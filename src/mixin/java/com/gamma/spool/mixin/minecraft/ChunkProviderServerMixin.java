package com.gamma.spool.mixin.minecraft;

import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.minecraft.util.LongHashMap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gamma.spool.util.NonBlockingMCLongHashMap;
import com.gamma.spool.util.RWLockedLongSet;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

// Apply after Hodgepodge's mixins
@Mixin(value = ChunkProviderServer.class, priority = 1001)
public abstract class ChunkProviderServerMixin {

    @Shadow
    public LongHashMap loadedChunkHashMap;

    @Shadow
    public List<Chunk> loadedChunks;

    @Shadow
    private Set<Long> chunksToUnload;

    @Unique
    private final ReadWriteLock spool$chunksToUnloadLock = new ReentrantReadWriteLock(true);

    @Unique
    private final Lock spool$generationLock = new ReentrantLock(true);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        this.loadedChunkHashMap = new NonBlockingMCLongHashMap<Chunk>();
        this.loadedChunks = ObjectLists.synchronize(new ObjectArrayList<>());
        this.chunksToUnload = new RWLockedLongSet(spool$chunksToUnloadLock, new LongOpenHashSet());
    }

    @Inject(method = "unloadQueuedChunks", at = @At("HEAD"))
    private void unloadQueuedChunksHead(CallbackInfoReturnable<Boolean> cir) {
        spool$chunksToUnloadLock.writeLock()
            .lock();
    }

    @Inject(method = "unloadQueuedChunks", at = @At("RETURN"))
    private void unloadQueuedChunksReturn(CallbackInfoReturnable<Boolean> cir) {
        spool$chunksToUnloadLock.writeLock()
            .unlock();
    }

    @Inject(method = "originalLoadChunk", at = @At("HEAD"), remap = false)
    private void originalLoadChunkHead(CallbackInfoReturnable<Boolean> cir) {
        spool$generationLock.lock();
    }

    @Inject(method = "originalLoadChunk", at = @At("RETURN"), remap = false)
    private void originalLoadChunkReturn(CallbackInfoReturnable<Boolean> cir) {
        spool$generationLock.unlock();
    }

    @Inject(method = "populate", at = @At("HEAD"))
    private void populateHead(IChunkProvider p_73153_1_, int p_73153_2_, int p_73153_3_, CallbackInfo ci) {
        spool$generationLock.lock();
    }

    @Inject(method = "populate", at = @At("RETURN"))
    private void populateReturn(IChunkProvider p_73153_1_, int p_73153_2_, int p_73153_3_, CallbackInfo ci) {
        spool$generationLock.unlock();
    }
}
