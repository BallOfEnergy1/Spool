package com.gamma.spool.mixin.compat;

import java.util.Collection;
import java.util.Set;

import net.minecraft.world.NextTickListEntry;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mitchej123.hodgepodge.SimulationDistanceHelper;

import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;

@SuppressWarnings("SynchronizeOnNonFinalField") // go away, literally the entire class is shadowing a final field.
@Mixin(value = SimulationDistanceHelper.class, remap = false)
public abstract class SimulationDistanceHelperMixin_hodgepodge {

    @Shadow
    @Final
    private ObjectAVLTreeSet<NextTickListEntry> pendingTickCandidates;

    @Shadow
    private Set<NextTickListEntry> pendingTickListEntriesHashSet;

    @WrapOperation(
        method = "checkForAddedChunks",
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/objects/ObjectAVLTreeSet;addAll(Ljava/util/Collection;)Z"),
        remap = false)
    private boolean addAllAVLHandler(ObjectAVLTreeSet<NextTickListEntry> instance, Collection collection,
        Operation<Boolean> original) {
        synchronized (this.pendingTickCandidates) {
            return original.call(instance, collection);
        }
    }

    @WrapOperation(
        method = "chunkUnloaded",
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/objects/ObjectAVLTreeSet;remove(Ljava/lang/Object;)Z"),
        remap = false)
    private boolean removeAVLHandler(ObjectAVLTreeSet<NextTickListEntry> instance, Object r,
        Operation<Boolean> original) {
        synchronized (this.pendingTickCandidates) {
            return original.call(instance, r);
        }
    }

    @WrapOperation(
        method = "addTick",
        at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectAVLTreeSet;add(Ljava/lang/Object;)Z"),
        remap = false)
    private boolean addAVLHandler(ObjectAVLTreeSet<NextTickListEntry> instance, Object r, Operation<Boolean> original) {
        synchronized (this.pendingTickCandidates) {
            return original.call(instance, r);
        }
    }

    @WrapOperation(method = "tickUpdates", at = @At(value = "INVOKE", target = "Ljava/util/Set;size()I"))
    private int synchronizeTickLists(Set<NextTickListEntry> instance, Operation<Integer> original) {
        synchronized (this.pendingTickListEntriesHashSet) {
            return original.call(instance);
        }
    }
}
