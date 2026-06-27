package com.gamma.spool.mixin.minecraft;

import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gamma.spool.util.IChunkLockAccessor;

@Mixin(value = S21PacketChunkData.class, priority = 999)
public abstract class S21PacketChunkDataMixin extends Packet {

    @Inject(method = "func_149269_a", at = @At("HEAD"))
    private static void func_149269_aHead(Chunk p_149269_0_, boolean p_149269_1_, int p_149269_2_,
        CallbackInfoReturnable<S21PacketChunkData.Extracted> cir) {
        ((IChunkLockAccessor) p_149269_0_).getStorageArraysLock()
            .readLock()
            .lock();
    }

    @Inject(method = "func_149269_a", at = @At("RETURN"))
    private static void func_149269_aReturn(Chunk p_149269_0_, boolean p_149269_1_, int p_149269_2_,
        CallbackInfoReturnable<S21PacketChunkData.Extracted> cir) {
        ((IChunkLockAccessor) p_149269_0_).getStorageArraysLock()
            .readLock()
            .unlock();
    }
}
