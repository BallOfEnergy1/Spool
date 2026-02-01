package com.gamma.spool.mixin.compat.hbm.concurrent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm.handler.radiation.ChunkRadiationHandlerPRISM;

@Mixin(ChunkRadiationHandlerPRISM.class)
public abstract class ChunkRadiationHandlerPRISMMixin {

    @Shadow(remap = false)
    @Mutable
    @Final
    public static Map<ChunkCoordIntPair, ChunkRadiationHandlerPRISM.SubChunk[]> newAdditions;
    @Shadow(remap = false)
    public Map<World, ChunkRadiationHandlerPRISM.RadPerWorld> perWorld;

    @Inject(method = "<init>", at = @At(value = "RETURN", remap = false), remap = false)
    private void onInit(CallbackInfo ci) {
        perWorld = new ConcurrentHashMap<>();
        newAdditions = new ConcurrentHashMap<>();
    }

    @Mixin(ChunkRadiationHandlerPRISM.RadPerWorld.class)
    public abstract static class RadPerWorldMixin {

        @Shadow(remap = false)
        public Map<ChunkCoordIntPair, ChunkRadiationHandlerPRISM.SubChunk[]> radiation;

        @Inject(method = "<init>", at = @At(value = "RETURN", remap = false), remap = false)
        private void onInit(CallbackInfo ci) {
            radiation = new ConcurrentHashMap<>();
        }
    }
}
