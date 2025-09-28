package com.gamma.spool.mixin.minecraft.nonconcurrent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

@Mixin(ChunkProviderServer.class)
public abstract class ChunkProviderServerMixin {

    @Shadow(remap = false)
    @Mutable
    private Set<Long> loadingChunks;

    @Shadow
    @Mutable
    public List<Chunk> loadedChunks;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        loadingChunks = ConcurrentHashMap.newKeySet();
        loadedChunks = ObjectLists.synchronize(new ObjectArrayList<>());
        // loadedChunkHashMap = new ConcurrentLongHashMap();
    }

    @WrapOperation(
        method = "loadChunk(IILjava/lang/Runnable;)Lnet/minecraft/world/chunk/Chunk;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/ChunkProviderServer;originalLoadChunk(II)Lnet/minecraft/world/chunk/Chunk;",
            remap = false),
        remap = false)
    public Chunk wrappedOriginalLoadChunk(ChunkProviderServer instance, int crashreportcategory, int throwable,
        Operation<Chunk> original) {
        synchronized (this) {
            return original.call(instance, crashreportcategory, throwable);
        }
    }
}
