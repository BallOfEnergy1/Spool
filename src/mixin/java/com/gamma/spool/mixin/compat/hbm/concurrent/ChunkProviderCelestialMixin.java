package com.gamma.spool.mixin.compat.hbm.concurrent;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gamma.spool.compat.IChunkCompat;
import com.hbm.dim.ChunkProviderCelestial;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(ChunkProviderCelestial.class)
public abstract class ChunkProviderCelestialMixin {

    @Definition(
        id = "getBlockStorageArray",
        method = "Lnet/minecraft/world/chunk/Chunk;getBlockStorageArray()[Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;")
    @Definition(id = "index", local = @Local(type = int.class, name = "index"))
    @Definition(id = "chunk", local = @Local(type = Chunk.class, name = "chunk"))
    @Expression("chunk.getBlockStorageArray()[index]")
    @ModifyExpressionValue(method = "provideChunk", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
    private static ExtendedBlockStorage check(ExtendedBlockStorage original, @Local(name = "chunk") Chunk chunk,
        @Local(name = "index") int index) {
        return ((IChunkCompat) chunk).checkAndCreateSubchunk(index);
    }
}
