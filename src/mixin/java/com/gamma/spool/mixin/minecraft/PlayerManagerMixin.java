package com.gamma.spool.mixin.minecraft;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    @Final
    @Mutable
    @Shadow
    public List<PlayerManager.PlayerInstance> chunkWatcherWithPlayers;

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

    @Mixin(PlayerManager.PlayerInstance.class)
    public abstract static class PlayerInstanceMixin {

        @Shadow
        public short[] locationOfBlockChange;

        @Shadow(remap = false)
        @Final
        PlayerManager this$0;
        @Shadow
        private int flagsYAreasToUpdate;
        @Unique
        private final AtomicInteger spool$numberOfTilesToUpdate = new AtomicInteger(0);
        @Unique
        private final Object spool$sync = new Object();

        @WrapMethod(method = { "sendChunkUpdate" })
        private void wrapped(Operation<Void> original) {
            synchronized (spool$sync) {
                original.call();
            }
        }

        /**
         * @author BallOfEnergy01
         * @reason Fix concurrency.
         */
        @Overwrite
        public void flagChunkForUpdate(int p_151253_1_, int p_151253_2_, int p_151253_3_) {
            synchronized (spool$sync) {
                if (spool$numberOfTilesToUpdate.get() == 0) {
                    this$0.chunkWatcherWithPlayers.add(this);
                }

                this.flagsYAreasToUpdate |= 1 << (p_151253_2_ >> 4);

                // if (spool$numberOfTilesToUpdate < 64) //Forge; Cache everything, so always run
                {
                    short short1 = (short) (p_151253_1_ << 12 | p_151253_3_ << 8 | p_151253_2_);

                    for (int l = 0; l < spool$numberOfTilesToUpdate.get(); ++l) {
                        if (this.locationOfBlockChange[l] == short1) {
                            return;
                        }
                    }

                    if (spool$numberOfTilesToUpdate.get() == locationOfBlockChange.length) {
                        locationOfBlockChange = Arrays.copyOf(locationOfBlockChange, locationOfBlockChange.length << 1);
                    }
                    this.locationOfBlockChange[spool$numberOfTilesToUpdate.getAndIncrement()] = short1;
                }
            }
        }

        @Redirect(
            method = { "removePlayer", "sendChunkUpdate" },
            at = @At(
                value = "FIELD",
                opcode = Opcodes.GETFIELD,
                target = "Lnet/minecraft/server/management/PlayerManager$PlayerInstance;numberOfTilesToUpdate:I"))
        private int numberOfTilesAccess(PlayerManager.PlayerInstance instance) {
            return spool$numberOfTilesToUpdate.get();
        }

        @Redirect(
            method = { "removePlayer", "sendChunkUpdate" },
            at = @At(
                value = "FIELD",
                opcode = Opcodes.PUTFIELD,
                target = "Lnet/minecraft/server/management/PlayerManager$PlayerInstance;numberOfTilesToUpdate:I"))
        private void numberOfTilesUpdate(PlayerManager.PlayerInstance instance, int value) {
            spool$numberOfTilesToUpdate.set(value);
        }
    }
}
