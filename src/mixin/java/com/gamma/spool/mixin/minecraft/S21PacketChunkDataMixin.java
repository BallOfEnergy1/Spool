package com.gamma.spool.mixin.minecraft;

import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalLongRef;

@Mixin(value = S21PacketChunkData.class, priority = 999)
public abstract class S21PacketChunkDataMixin extends Packet {

    @WrapMethod(method = "func_149269_a")
    private static S21PacketChunkData.Extracted func_149269_aWrapped(Chunk p_149269_0_, boolean p_149269_1_,
        int p_149269_2_, Operation<S21PacketChunkData.Extracted> original) {
        synchronized (p_149269_0_.getBlockStorageArray()) {
            return original.call(p_149269_0_, p_149269_1_, p_149269_2_);
        }
    }
}
