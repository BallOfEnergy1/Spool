package com.gamma.spool.mixin.compat.hbm.concurrent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.ChunkCoordIntPair;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm.handler.radiation.ChunkRadiationHandlerPRISM;

@Mixin(value = ChunkRadiationHandlerPRISM.RadPerWorld.class, remap = false)
public abstract class RadPerWorldMixin {

    @Shadow
    public Map<ChunkCoordIntPair, ChunkRadiationHandlerPRISM.SubChunk[]> radiation;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void onInit(CallbackInfo ci) {
        radiation = new ConcurrentHashMap<>();
    }
}
