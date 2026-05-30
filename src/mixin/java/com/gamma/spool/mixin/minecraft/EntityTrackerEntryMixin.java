package com.gamma.spool.mixin.minecraft;

import java.util.List;

import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(EntityTrackerEntry.class)
public abstract class EntityTrackerEntryMixin {

    @WrapOperation(
        method = "sendLocationToAllClients",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/EntityTrackerEntry;sendEventsToPlayers(Ljava/util/List;)V"))
    private void sendEventsToPlayers(EntityTrackerEntry instance, List<EntityPlayer> entityPlayers,
        Operation<Void> original) {
        synchronized (entityPlayers) {
            original.call(instance, entityPlayers);
        }
    }
}
