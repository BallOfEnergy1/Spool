package com.gamma.spool.mixin.compat.hbm.concurrent;

import net.minecraftforge.event.terraingen.OreGenEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm.main.ModEventHandler;

@Mixin(ModEventHandler.class)
public abstract class ModEventHandlerMixin {

    @Inject(method = "onGenerateOre", at = @At(value = "HEAD"), remap = false, cancellable = true, require = 0)
    private void injectedHead(OreGenEvent.GenerateMinable event, CallbackInfo ci) {
        if (event == null) {
            ci.cancel();
            return;
        }

        if (event.world == null || event.generator == null) {
            ci.cancel();
            return;
        }

        if (event.world.provider == null) {
            ci.cancel();
        }
    }
}
