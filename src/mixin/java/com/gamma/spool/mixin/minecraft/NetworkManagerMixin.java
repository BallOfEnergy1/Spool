package com.gamma.spool.mixin.minecraft;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.util.concurrent.GenericFutureListener;

@Mixin(NetworkManager.class)
public abstract class NetworkManagerMixin {

    @Unique
    private final Lock spool$dispatchPacketLock = new ReentrantLock(true);

    @Inject(method = "dispatchPacket", at = @At("HEAD"))
    private void dispatchPacketHead(Packet inPacket, GenericFutureListener<?>[] futureListeners, CallbackInfo ci) {
        spool$dispatchPacketLock.lock();
    }

    @Inject(method = "dispatchPacket", at = @At("RETURN"))
    private void dispatchPacketReturn(Packet inPacket, GenericFutureListener<?>[] futureListeners, CallbackInfo ci) {
        spool$dispatchPacketLock.unlock();
    }
}
