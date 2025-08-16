package com.gamma.spool.mixin.minecraft;

import java.util.List;

import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(EntityTrackerEntry.class)
public abstract class EntityTrackerEntryMixin {

    @WrapMethod(method = "sendEventsToPlayers")
    private void sendEventsToPlayers(List<EntityPlayer> p_73125_1_, Operation<Void> original) {
        synchronized (p_73125_1_) {
            original.call(p_73125_1_);
        }
    }
}
