package com.gamma.spool.mixin.minecraft;

import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

// Ported from GerverMod (and made thread-safe) to speed up frequent dimension manager accesses.
@Mixin(value = DimensionManager.class, remap = false)
public abstract class DimensionManagerSpeedupMixin {

    @Unique
    private static final AtomicReference<WorldServer[]> spool$worldCache = new AtomicReference<>();
    @Unique
    private static final AtomicReference<Integer[]> spool$idCache = new AtomicReference<>();

    @WrapMethod(method = "getWorlds")
    private static WorldServer[] getWorlds(Operation<WorldServer[]> original) {
        WorldServer[] previousValue;
        WorldServer[] newValue;
        do {
            previousValue = spool$worldCache.get();
            if (previousValue != null) return previousValue;
            newValue = original.call();
        } while (spool$worldCache.compareAndSet(previousValue, newValue));
        return spool$worldCache.get();
    }

    @WrapMethod(method = "getIDs()[Ljava/lang/Integer;")
    private static Integer[] getIDs(Operation<Integer[]> original) {
        Integer[] previousValue;
        Integer[] newValue;
        do {
            previousValue = spool$idCache.get();
            if (previousValue != null) return previousValue;
            newValue = original.call();
        } while (spool$idCache.compareAndSet(previousValue, newValue));
        return spool$idCache.get();
    }

    @Inject(method = "setWorld", at = @At("HEAD"))
    private static void injected(CallbackInfo ci) {
        spool$worldCache.set(null);
        spool$idCache.set(null);
    }
}
