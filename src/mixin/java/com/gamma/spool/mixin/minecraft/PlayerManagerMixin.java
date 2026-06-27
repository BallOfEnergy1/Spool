package com.gamma.spool.mixin.minecraft;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.util.LongHashMap;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gamma.spool.util.ConcurrentLongHashMap;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    @Final
    @Mutable
    @Shadow
    public List<PlayerManager.PlayerInstance> chunkWatcherWithPlayers;

    @Mutable
    @Shadow
    @Final
    private List<PlayerManager.PlayerInstance> playerInstanceList;

    @Mutable
    @Final
    @Shadow
    private List<EntityPlayerMP> players;

    @Mutable
    @Shadow
    @Final
    private LongHashMap playerInstances;

    @Unique
    private final Lock spool$updateInstancesLock = new ReentrantLock(true);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit1(CallbackInfo ci) {
        playerInstances = new ConcurrentLongHashMap();
        chunkWatcherWithPlayers = ObjectLists.synchronize(new ObjectArrayList<>());
        playerInstanceList = ObjectLists.synchronize(new ObjectArrayList<>());
        players = ObjectLists.synchronize(new ObjectArrayList<>());
    }

    @Inject(method = "updatePlayerInstances", at = @At("HEAD"))
    public void updatePlayerInstancesHead(CallbackInfo ci) {
        spool$updateInstancesLock.lock();
    }

    @Inject(method = "updatePlayerInstances", at = @At("RETURN"))
    public void updatePlayerInstancesReturn(CallbackInfo ci) {
        spool$updateInstancesLock.unlock();
    }
}
