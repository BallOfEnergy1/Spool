package com.gamma.spool.mixin.minecraft;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.util.IntHashMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gamma.gammalib.util.concurrent.ConcurrentIntHashMap;

@Mixin(EntityTracker.class)
public abstract class EntityTrackerMixin {

    @Shadow
    @Mutable
    private Set<EntityTrackerEntry> trackedEntities;

    @Shadow
    @Mutable
    private IntHashMap trackedEntityIDs;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit1(CallbackInfo ci) {
        // Fuck you *ConcurrentHashMaps your HashSet*
        trackedEntities = ConcurrentHashMap.newKeySet();
        trackedEntityIDs = new ConcurrentIntHashMap();
    }

    @Inject(
        method = "addEntityToTracker(Lnet/minecraft/entity/Entity;IIZ)V",
        at = @At(
            value = "NEW",
            target = "(Ljava/lang/String;)Ljava/lang/IllegalStateException;",
            shift = At.Shift.BEFORE),
        cancellable = true)
    private void redirectNew(Entity p_72785_1_, int p_72785_2_, int p_72785_3_, boolean p_72785_4_, CallbackInfo ci) {
        // Ignore this error.
        // FMLLog.warning("EntityTracker.addEntityToTracker attempted to add an already tracked entity " + p_72785_1_ +
        // ", cancelling operation to prevent a crash.");
        ci.cancel();
    }
}
