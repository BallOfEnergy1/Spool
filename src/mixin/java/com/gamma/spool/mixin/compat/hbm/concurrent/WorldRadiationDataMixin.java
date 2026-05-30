package com.gamma.spool.mixin.compat.hbm.concurrent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.ChunkCoordIntPair;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm.handler.radiation.ChunkRadiationHandlerNT;
import com.hbm.util.fauxpointtwelve.BlockPos;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;

@Mixin(value = ChunkRadiationHandlerNT.WorldRadiationData.class, remap = false)
public abstract class WorldRadiationDataMixin {

    @Shadow
    public Map<ChunkCoordIntPair, ChunkRadiationHandlerNT.ChunkRadiationStorage> data;

    @Shadow
    public Set<ChunkRadiationHandlerNT.RadPocket> activePockets;
    @Shadow
    private Set<BlockPos> dirtyChunks2;
    @Shadow
    private Set<BlockPos> dirtyChunks;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void onInit(CallbackInfo ci) {
        data = new ConcurrentHashMap<>();
        activePockets = ObjectSets.synchronize(new ObjectLinkedOpenHashSet<>());
        dirtyChunks2 = ObjectSets.synchronize(new ObjectLinkedOpenHashSet<>());
        dirtyChunks = ObjectSets.synchronize(new ObjectLinkedOpenHashSet<>());
    }
}
