package com.gamma.spool.mixin.minecraft.concurrent;

import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gamma.spool.concurrent.ConcurrentExtendedBlockStorage;

@Mixin(S21PacketChunkData.class)
public abstract class S21PacketChunkDataMixin {

    @Redirect(
        method = "func_149269_a",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;getBlockLSBArray()[B"))
    private static byte[] redirected(ExtendedBlockStorage instance) {
        return ((ConcurrentExtendedBlockStorage) instance).getBlockLSBArraySafe();
    }
}
