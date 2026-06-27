package com.gamma.spool.mixin.minecraft;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Ported from GerverMod (and made thread-safe) to speed up frequent dimension manager accesses.
@Mixin(value = DimensionManager.class, remap = false)
public abstract class DimensionManagerSpeedupMixin {

    @Unique
    private static final AtomicReference<WorldServer[]> spool$worldCache = new AtomicReference<>();
    @Unique
    private static final AtomicReference<Integer[]> spool$idCache = new AtomicReference<>();

    @Inject(method = "getWorlds", at = @At("HEAD"), cancellable = true)
    private static void getWorldsHead(CallbackInfoReturnable<WorldServer[]> cir) {
        synchronized (spool$worldCache) {
            WorldServer[] value = spool$worldCache.get();
            if (value != null) cir.setReturnValue(value);
        }
    }

    @Redirect(
        method = "getWorlds",
        at = @At(value = "INVOKE", target = "Ljava/util/Collection;toArray([Ljava/lang/Object;)[Ljava/lang/Object;"))
    private static <T> T[] getWorldsOnArray(Collection<WorldServer> instance, T[] ts) {
        synchronized (spool$worldCache) {
            T[] arr = instance.toArray(ts);
            spool$worldCache.set((WorldServer[]) arr);
            return arr;
        }
    }

    @Inject(method = "getIDs()[Ljava/lang/Integer;", at = @At("HEAD"), cancellable = true)
    private static void getIDsHead(CallbackInfoReturnable<Integer[]> cir) {
        synchronized (spool$idCache) {
            Integer[] value = spool$idCache.get();
            if (value != null) cir.setReturnValue(value);
        }
    }

    @Redirect(
        method = "getIDs()[Ljava/lang/Integer;",
        at = @At(value = "INVOKE", target = "Ljava/util/Set;toArray([Ljava/lang/Object;)[Ljava/lang/Object;"))
    private static <T> T[] getIDsOnArray(Set<Integer> instance, T[] ts) {
        synchronized (spool$idCache) {
            T[] arr = instance.toArray(ts);
            spool$idCache.set((Integer[]) arr);
            return arr;
        }
    }

    @Inject(method = "setWorld", at = @At("HEAD"))
    private static void injected(CallbackInfo ci) {
        spool$worldCache.set(null);
        spool$idCache.set(null);
    }
}
