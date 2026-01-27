package com.gamma.spool.mixin.compat.hbm.concurrent;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm.handler.atmosphere.ChunkAtmosphereHandler;

@Mixin(ChunkAtmosphereHandler.class)
public abstract class ChunkAtmosphereHandlerMixin {

    @Inject(method = "tickTerraforming", at = @At("HEAD"), remap = false, cancellable = true)
    private void injectedHead(World world, CallbackInfo ci) {

        // TODO: i was doing something here i think?
    }
}
