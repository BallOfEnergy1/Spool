package com.gamma.spool.mixin.minecraft.concurrent;

import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(value = S21PacketChunkData.class, priority = 999)
public abstract class S21PacketChunkDataMixin extends Packet {

    @Unique
    private static final Object spool$sync = new Object();

    @WrapMethod(method = "readPacketData")
    private void wrapped(PacketBuffer data, Operation<Void> original) {
        synchronized (spool$sync) {
            original.call(data);
        }
    }

    @WrapMethod(method = "func_149269_a")
    private static S21PacketChunkData.Extracted wrapped(Chunk p_149269_0_, boolean p_149269_1_, int p_149269_2_,
        Operation<S21PacketChunkData.Extracted> original) {
        synchronized (spool$sync) {
            return original.call(p_149269_0_, p_149269_1_, p_149269_2_);
        }
    }
}
