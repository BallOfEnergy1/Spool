package com.gamma.spool.mixin.minecraft;

import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(SoundHandler.class)
public abstract class SoundHandlerMixin {

    @WrapOperation(
        method = "update",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/SoundManager;updateAllSounds()V"))
    private void wrapUpdateAllSounds(SoundManager instance, Operation<Void> original) {
        synchronized (instance.sndHandler) {
            original.call(instance);
        }
    }
}
