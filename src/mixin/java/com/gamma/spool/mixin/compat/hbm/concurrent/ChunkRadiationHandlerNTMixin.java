package com.gamma.spool.mixin.compat.hbm.concurrent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm.handler.radiation.ChunkRadiationHandlerNT;
import com.hbm.util.fauxpointtwelve.BlockPos;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;

@Mixin(ChunkRadiationHandlerNT.class)
public abstract class ChunkRadiationHandlerNTMixin {

    @Shadow(remap = false)
    private static Map<World, ChunkRadiationHandlerNT.WorldRadiationData> worldMap;

    @Inject(method = "<init>", at = @At(value = "RETURN", remap = false), remap = false)
    private void onInit(CallbackInfo ci) {
        worldMap = new ConcurrentHashMap<>();
    }

    @Mixin(ChunkRadiationHandlerNT.WorldRadiationData.class)
    public abstract static class WorldRadiationDataMixin {

        @Shadow(remap = false)
        public Map<ChunkCoordIntPair, ChunkRadiationHandlerNT.ChunkRadiationStorage> data;

        @Shadow(remap = false)
        public Set<ChunkRadiationHandlerNT.RadPocket> activePockets;
        @Shadow(remap = false)
        private Set<BlockPos> dirtyChunks2;
        @Shadow(remap = false)
        private Set<BlockPos> dirtyChunks;

        @Inject(method = "<init>", at = @At(value = "RETURN", remap = false), remap = false)
        private void onInit(CallbackInfo ci) {
            data = new ConcurrentHashMap<>();
            activePockets = ObjectSets.synchronize(new ObjectLinkedOpenHashSet<>());
            dirtyChunks2 = ObjectSets.synchronize(new ObjectLinkedOpenHashSet<>());
            dirtyChunks = ObjectSets.synchronize(new ObjectLinkedOpenHashSet<>());
        }
    }
}
