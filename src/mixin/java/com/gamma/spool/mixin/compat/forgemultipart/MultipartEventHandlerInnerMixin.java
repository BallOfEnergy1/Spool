package com.gamma.spool.mixin.compat.forgemultipart;

import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import codechicken.multipart.handler.MultipartEventHandler$;
import codechicken.multipart.handler.MultipartSPH;
import scala.collection.Seq;

@Mixin(MultipartEventHandler$.class)
public abstract class MultipartEventHandlerInnerMixin {

    @WrapOperation(
        method = "serverTick",
        at = @At(
            value = "INVOKE",
            target = "Lcodechicken/multipart/handler/MultipartSPH$;onTickEnd(Lscala/collection/Seq;)V",
            remap = false),
        remap = false)
    private static void serverTickWrap(MultipartSPH instance, Seq<?> seq, Operation<Void> original) {
        synchronized (MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList) {
            original.call(instance, seq);
        }
    }
}
