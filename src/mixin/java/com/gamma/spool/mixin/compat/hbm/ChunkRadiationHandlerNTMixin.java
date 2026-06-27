package com.gamma.spool.mixin.compat.hbm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm.handler.radiation.ChunkRadiationHandlerNT;

@Mixin(value = ChunkRadiationHandlerNT.class, remap = false)
public abstract class ChunkRadiationHandlerNTMixin {

    @Shadow
    private static Map<World, ChunkRadiationHandlerNT.WorldRadiationData> worldMap;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void onInit(CallbackInfo ci) {
        worldMap = new ConcurrentHashMap<>();
    }
}
