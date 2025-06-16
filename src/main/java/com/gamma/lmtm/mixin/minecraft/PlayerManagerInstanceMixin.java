package com.gamma.lmtm.mixin.minecraft;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gamma.lmtm.accessors.EntityPlayerMPLockAccessor;

@Mixin(PlayerManager.PlayerInstance.class)
public abstract class PlayerManagerInstanceMixin {

    @Inject(method = "addPlayer", at = @At("INVOKE"))
    private void addPlayerInvoke(EntityPlayerMP p_72691_1_, CallbackInfo ci) {
        if (p_72691_1_ instanceof EntityPlayerMPLockAccessor) ((EntityPlayerMPLockAccessor) p_72691_1_).lmtm$getLock()
            .lock();
    }

    @Inject(method = "addPlayer", at = @At(value = "INVOKE", shift = At.Shift.AFTER))
    private void addPlayerAfterInvoke(EntityPlayerMP p_72691_1_, CallbackInfo ci) {
        if (p_72691_1_ instanceof EntityPlayerMPLockAccessor) ((EntityPlayerMPLockAccessor) p_72691_1_).lmtm$getLock()
            .unlock();
    }

    @Inject(method = "removePlayer", at = @At("INVOKE"))
    private void removePlayerInvoke(EntityPlayerMP p_72691_1_, CallbackInfo ci) {
        if (p_72691_1_ instanceof EntityPlayerMPLockAccessor) ((EntityPlayerMPLockAccessor) p_72691_1_).lmtm$getLock()
            .lock();
    }

    @Inject(method = "removePlayer", at = @At(value = "INVOKE", shift = At.Shift.AFTER))
    private void removePlayerAfterInvoke(EntityPlayerMP p_72691_1_, CallbackInfo ci) {
        if (p_72691_1_ instanceof EntityPlayerMPLockAccessor) ((EntityPlayerMPLockAccessor) p_72691_1_).lmtm$getLock()
            .unlock();
    }
}
