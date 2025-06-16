package com.gamma.lmtm.mixin.minecraft;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.util.IntHashMap;
import net.minecraft.world.WorldServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gamma.lmtm.util.ConcurrentIntHashMap;

@Mixin(EntityTracker.class)
public abstract class EntityTrackerMixin {

    @Shadow
    @Mutable
    private Set trackedEntities;

    @Shadow
    @Mutable
    private IntHashMap trackedEntityIDs;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit1(CallbackInfo ci) {
        // Fuck you *ConcurrentHashMaps your HashSet*
        trackedEntities = ConcurrentHashMap.newKeySet();
        trackedEntityIDs = new ConcurrentIntHashMap();
    }

    @Shadow
    private int entityViewDistance;

    @Shadow
    private WorldServer theWorld;

    @Inject(method = "addEntityToTracker(Lnet/minecraft/entity/Entity;IIZ)V", at = @At("INVOKE"), cancellable = true)
    public void addEntityToTracker(Entity p_72785_1_, int p_72785_2_, final int p_72785_3_, boolean p_72785_4_,
        CallbackInfo ci) {
        if (p_72785_2_ > this.entityViewDistance) {
            p_72785_2_ = this.entityViewDistance;
        }

        if (!this.trackedEntityIDs.containsItem(p_72785_1_.getEntityId())) {
            EntityTrackerEntry entitytrackerentry = new EntityTrackerEntry(
                p_72785_1_,
                p_72785_2_,
                p_72785_3_,
                p_72785_4_);
            this.trackedEntities.add(entitytrackerentry);
            this.trackedEntityIDs.addKey(p_72785_1_.getEntityId(), entitytrackerentry);
            synchronized (Collections.unmodifiableList(this.theWorld.playerEntities)) {
                entitytrackerentry.sendEventsToPlayers(this.theWorld.playerEntities);
            }
        }
        ci.cancel();
    }
}
