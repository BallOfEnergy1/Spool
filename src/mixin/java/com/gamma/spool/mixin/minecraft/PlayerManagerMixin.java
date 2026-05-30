package com.gamma.spool.mixin.minecraft;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.util.LongHashMap;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gamma.spool.util.ConcurrentLongHashMap;
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

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit1(CallbackInfo ci) {
        playerInstances = new ConcurrentLongHashMap();
        chunkWatcherWithPlayers = ObjectLists.synchronize(new ObjectArrayList<>());
        playerInstanceList = ObjectLists.synchronize(new ObjectArrayList<>());
        players = ObjectLists.synchronize(new ObjectArrayList<>());
    }

    @WrapMethod(method = "updatePlayerInstances")
    public void wrappedUpdatePlayerInstances(Operation<Void> original) {
        synchronized (this) {
            original.call();
        }
    }

}
