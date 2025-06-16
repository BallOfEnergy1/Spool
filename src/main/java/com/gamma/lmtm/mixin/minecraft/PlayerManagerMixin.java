package com.gamma.lmtm.mixin.minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gamma.lmtm.accessors.EntityPlayerMPLockAccessor;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    @Mutable
    @Shadow
    private List chunkWatcherWithPlayers;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit1(CallbackInfo ci) {
        // Fuck you *ConcurrentHashMaps your HashSet*
        chunkWatcherWithPlayers = Collections.synchronizedList(new ArrayList<>());
    }

    @Inject(method = "filterChunkLoadQueue", at = @At("INVOKE"))
    private void filterChunkLoadQueueInvoke(EntityPlayerMP p_72691_1_, CallbackInfo ci) {
        if (p_72691_1_ instanceof EntityPlayerMPLockAccessor) ((EntityPlayerMPLockAccessor) p_72691_1_).lmtm$getLock()
            .lock();
    }

    @Inject(method = "filterChunkLoadQueue", at = @At(value = "INVOKE", shift = At.Shift.AFTER))
    private void filterChunkLoadQueueAfterInvoke(EntityPlayerMP p_72691_1_, CallbackInfo ci) {
        if (p_72691_1_ instanceof EntityPlayerMPLockAccessor) ((EntityPlayerMPLockAccessor) p_72691_1_).lmtm$getLock()
            .unlock();
    }

    @Inject(method = "updatePlayerPertinentChunks", at = @At("INVOKE"))
    private void updatePlayerPertinentChunksInvoke(EntityPlayerMP p_72685_1_, CallbackInfo ci) {
        if (p_72685_1_ instanceof EntityPlayerMPLockAccessor) ((EntityPlayerMPLockAccessor) p_72685_1_).lmtm$getLock()
            .lock();
    }

    @Inject(method = "updatePlayerPertinentChunks", at = @At(value = "INVOKE", shift = At.Shift.AFTER))
    private void updatePlayerPertinentChunksAfterInvoke(EntityPlayerMP p_72685_1_, CallbackInfo ci) {
        if (p_72685_1_ instanceof EntityPlayerMPLockAccessor) ((EntityPlayerMPLockAccessor) p_72685_1_).lmtm$getLock()
            .unlock();
    }

    @Shadow
    @Final
    private WorldServer theWorldServer;

    @Shadow
    private long previousTotalWorldTime;

    @Shadow
    @Final
    private List playerInstanceList;

    @Shadow
    private List players;

    @Inject(method = "updatePlayerInstances", at = @At("INVOKE"), cancellable = true)
    public void updatePlayerInstances(CallbackInfo ci) {
        long i = this.theWorldServer.getTotalWorldTime();
        int j;
        PlayerManager.PlayerInstance playerinstance;

        if (i - this.previousTotalWorldTime > 8000L) {
            this.previousTotalWorldTime = i;

            synchronized (this.playerInstanceList) {
                for (j = 0; j < this.playerInstanceList.size(); ++j) {
                    playerinstance = (PlayerManager.PlayerInstance) this.playerInstanceList.get(j);
                    playerinstance.sendChunkUpdate();
                    playerinstance.processChunk();
                }
            }
        } else {
            synchronized (this.chunkWatcherWithPlayers) {
                for (j = 0; j < this.chunkWatcherWithPlayers.size(); ++j) {
                    playerinstance = (PlayerManager.PlayerInstance) this.chunkWatcherWithPlayers.get(j);
                    playerinstance.sendChunkUpdate();
                }
            }
        }

        this.chunkWatcherWithPlayers.clear();

        if (this.players.isEmpty()) {
            WorldProvider worldprovider = this.theWorldServer.provider;

            if (!worldprovider.canRespawnHere()) {
                this.theWorldServer.theChunkProviderServer.unloadAllChunks();
            }
        }
        ci.cancel();
    }
}
