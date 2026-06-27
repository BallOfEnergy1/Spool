package com.gamma.spool.mixin.minecraft;

import java.util.List;

import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityTrackerEntry.class)
public abstract class EntityTrackerEntryMixin {

    @Shadow
    public abstract void tryStartWachingThis(EntityPlayerMP p_73117_1_);

    /**
     * @author BallOfEnergy01
     * @reason Use iterator; safer with a COW list.
     */
    @Overwrite
    public void sendEventsToPlayers(List<EntityPlayer> entityPlayers) {
        for (EntityPlayer entityPlayer : entityPlayers) {
            this.tryStartWachingThis((EntityPlayerMP) entityPlayer);
        }
    }
}
