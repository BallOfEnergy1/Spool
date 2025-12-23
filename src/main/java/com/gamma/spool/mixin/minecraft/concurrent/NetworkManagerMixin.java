package com.gamma.spool.mixin.minecraft.concurrent;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import io.netty.util.concurrent.GenericFutureListener;

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

    @WrapMethod(method = "dispatchPacket")
    public void dispatchPacketWrapped(Packet inPacket, GenericFutureListener[] futureListeners,
        Operation<Void> original) {
        synchronized (this) {
            original.call(inPacket, futureListeners);
        }
    }
}
