package com.gamma.spool.mixin.minecraft;

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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    @Final
    @Mutable
    @Shadow
    private List<PlayerManager.PlayerInstance> chunkWatcherWithPlayers;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit1(CallbackInfo ci) {
        playerInstanceList = ObjectLists.synchronize(new ObjectArrayList<>());
        chunkWatcherWithPlayers = ObjectLists.synchronize(new ObjectArrayList<>());
    }

    @Shadow
    @Final
    private WorldServer theWorldServer;

    @Shadow
    private long previousTotalWorldTime;

    @Mutable
    @Shadow
    @Final
    private List<PlayerManager.PlayerInstance> playerInstanceList;

    @Final
    @Shadow
    private List<EntityPlayerMP> players;

    // TODO: Proper injecting here.
    @Inject(method = "updatePlayerInstances", at = @At("HEAD"), cancellable = true)
    public void updatePlayerInstances(CallbackInfo ci) {
        long i = this.theWorldServer.getTotalWorldTime();
        int j;
        PlayerManager.PlayerInstance playerinstance;

        if (i - this.previousTotalWorldTime > 8000L) {
            this.previousTotalWorldTime = i;

            // noinspection SynchronizeOnNonFinalField
            synchronized (playerInstanceList) {
                for (j = 0; j < playerInstanceList.size(); ++j) {
                    playerinstance = playerInstanceList.get(j);
                    // synchronized (spool$lockObject) {
                    playerinstance.sendChunkUpdate();
                    // }
                    playerinstance.processChunk();
                }
            }
        } else {
            // noinspection SynchronizeOnNonFinalField
            synchronized (chunkWatcherWithPlayers) {
                for (j = 0; j < chunkWatcherWithPlayers.size(); ++j) {
                    playerinstance = chunkWatcherWithPlayers.get(j);
                    // synchronized (spool$lockObject) {
                    playerinstance.sendChunkUpdate();
                    // }
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
