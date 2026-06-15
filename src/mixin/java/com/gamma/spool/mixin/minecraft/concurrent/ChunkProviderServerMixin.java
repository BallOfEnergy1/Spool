package com.gamma.spool.mixin.minecraft.concurrent;

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

@Mixin(ChunkProviderServer.class)
public abstract class ChunkProviderServerMixin {

    @Shadow
    public IChunkProvider currentChunkProvider;

    @Shadow
    public LongHashMap loadedChunkHashMap;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        this.loadedChunkHashMap = new NonBlockingMCLongHashMap();
    }

    @WrapMethod(method = "originalLoadChunk", remap = false)
    private Chunk wrapped(int x, int z, Operation<Chunk> original) {
        synchronized (this.currentChunkProvider) {
            return original.call(x, z);
        }
    }

    @WrapMethod(method = "populate")
    private void wrapped(IChunkProvider provider, int x, int z, Operation<Void> original) {
        synchronized (this.currentChunkProvider) {
            original.call(provider, x, z);
        }
    }
}
