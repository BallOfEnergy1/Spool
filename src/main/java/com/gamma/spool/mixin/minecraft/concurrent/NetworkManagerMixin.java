package com.gamma.spool.mixin.minecraft.concurrent;

import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

@Mixin(NetworkManager.class)
public abstract class NetworkManagerMixin {

    // @WrapOperation(
    // method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V",
    // at = @At(
    // value = "INVOKE",
    // target = "Lnet/minecraft/network/Packet;processPacket(Lnet/minecraft/network/INetHandler;)V"
    // )
    // )
    // public void channelRead0Wrapped(Packet instance, INetHandler iNetHandler, Operation<Void> original) {
    // synchronized (this) {
    // original.call(instance, iNetHandler);
    // }
    // }

    @WrapOperation(
        method = "dispatchPacket",
        at = @At(
            value = "INVOKE",
            target = "Lio/netty/channel/Channel;writeAndFlush(Ljava/lang/Object;)Lio/netty/channel/ChannelFuture;",
            ordinal = 0,
            remap = false))
    public ChannelFuture dispatchPacketWrapped(Channel instance, Object o, Operation<ChannelFuture> original) {
        synchronized (this) {
            return original.call(instance, o);
        }
    }

    @WrapOperation(
        method = "processReceivedPackets",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/INetHandler;onNetworkTick()V"))
    public void processReceivedPacketsTickWrapped(INetHandler instance, Operation<Void> original) {
        synchronized (this) {
            original.call(instance);
        }
    }

    @WrapOperation(
        method = "processReceivedPackets",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/Packet;processPacket(Lnet/minecraft/network/INetHandler;)V"))
    public void processReceivedPacketsProcessWrapped(Packet instance, INetHandler iNetHandler,
        Operation<Void> original) {
        synchronized (this) {
            original.call(instance, iNetHandler);
        }
    }
}
