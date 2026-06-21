package com.gamma.spool.mixin.compat.hbm.concurrent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.World;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm.handler.radiation.ChunkRadiationHandlerSimple;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(value = ChunkRadiationHandlerSimple.class, remap = false)
public abstract class ChunkRadiationHandlerSimpleMixin {

    @Shadow
    private Map<World, ChunkRadiationHandlerSimple.SimpleRadiationPerWorld> perWorld;

    @WrapMethod(method = "getRadiation")
    private float wrappedGetRadiation(World world, int x, int y, int z, Operation<Float> original) {
        synchronized (this) {
            return original.call(world, x, y, z);
        }
    }

    @WrapMethod(method = "setRadiation")
    private void wrappedSetRadiation(World world, int x, int y, int z, float rad, Operation<Void> original) {
        synchronized (this) {
            original.call(world, x, y, z, rad);
        }
    }

    @WrapMethod(method = "updateSystem")
    private void wrappedUpdateSystem(Operation<Void> original) {
        synchronized (this) {
            original.call();
        }
    }

    @WrapMethod(method = "clearSystem")
    private void wrappedClearSystem(World world, Operation<Void> original) {
        synchronized (this) {
            original.call(world);
        }
    }

    @WrapMethod(method = "receiveChunkLoad")
    private void wrappedChunkLoad(ChunkDataEvent.Load event, Operation<Void> original) {
        synchronized (this) {
            original.call(event);
        }
    }

    @WrapMethod(method = "receiveChunkSave")
    private void wrappedChunkSave(ChunkDataEvent.Save event, Operation<Void> original) {
        synchronized (this) {
            original.call(event);
        }
    }

    @WrapMethod(method = "receiveChunkUnload")
    private void wrappedChunkUnload(ChunkEvent.Unload event, Operation<Void> original) {
        synchronized (this) {
            original.call(event);
        }
    }

    @WrapMethod(method = "handleWorldDestruction")
    private void wrappedHandleWorldDestruction(Operation<Void> original) {
        synchronized (this) {
            original.call();
        }
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void onInit(CallbackInfo ci) {
        perWorld = new ConcurrentHashMap<>();
    }
}
