package com.gamma.spool.mixin.compat.hbm.concurrent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm.handler.radiation.ChunkRadiationHandlerSimple;

@Mixin(ChunkRadiationHandlerSimple.class)
public abstract class ChunkRadiationHandlerSimpleMixin {

    @Shadow(remap = false)
    private Map<World, ChunkRadiationHandlerSimple.SimpleRadiationPerWorld> perWorld;

    @Inject(method = "<init>", at = @At(value = "RETURN", remap = false), remap = false)
    private void onInit(CallbackInfo ci) {
        perWorld = new ConcurrentHashMap<>();
    }

    @Mixin(ChunkRadiationHandlerSimple.SimpleRadiationPerWorld.class)
    public abstract static class SimpleRadiationPerWorldMixin {

        @Shadow(remap = false)
        public Map<ChunkCoordIntPair, Float> radiation;

        @Inject(method = "<init>", at = @At(value = "RETURN", remap = false), remap = false)
        private void onInit(CallbackInfo ci) {
            radiation = new ConcurrentHashMap<>();
        }
    }
}
