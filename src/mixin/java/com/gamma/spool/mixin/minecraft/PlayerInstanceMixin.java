package com.gamma.spool.mixin.minecraft;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerManager;

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

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

@Mixin(PlayerManager.PlayerInstance.class)
public abstract class PlayerInstanceMixin {

    @Unique
    private static final AtomicIntegerFieldUpdater<PlayerInstanceMixin> spool$flagsYAreasToUpdateUpdater = AtomicIntegerFieldUpdater
        .newUpdater(PlayerInstanceMixin.class, "spool$flagsYAreasToUpdate");

    @Mutable
    @Shadow
    public volatile short[] locationOfBlockChange;

    @Mutable
    @Shadow
    @Final
    private List<EntityPlayerMP> playersWatchingChunk;

    @Shadow(remap = false)
    @Final
    PlayerManager this$0;

    @Unique
    private volatile int spool$flagsYAreasToUpdate;

    @Unique
    private Object2ObjectMap<EntityPlayerMP, Runnable> spool$players;

    @Unique
    private final AtomicInteger spool$numberOfTilesToUpdate = new AtomicInteger(0);

    @Unique
    private final AtomicReference<short[]> spool$locationOfBlockChangeRef = new AtomicReference<>();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        playersWatchingChunk = ObjectLists.synchronize(new ObjectArrayList<>());
        spool$players = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
        spool$locationOfBlockChangeRef.set(locationOfBlockChange);
    }

    @WrapMethod(method = { "sendChunkUpdate" })
    private void wrapped(Operation<Void> original) {
        synchronized (this) {
            original.call();
        }
    }

    /**
     * @author BallOfEnergy01
     * @reason Fix concurrency.
     */
    @Overwrite
    public void flagChunkForUpdate(int p_151253_1_, int p_151253_2_, int p_151253_3_) {
        spool$flagsYAreasToUpdateUpdater.accumulateAndGet(this, 1 << (p_151253_2_ >> 4), (old, update) -> old | update);

        short short1 = (short) (p_151253_1_ << 12 | p_151253_3_ << 8 | p_151253_2_);

        while (true) {
            int count = spool$numberOfTilesToUpdate.get();
            short[] currentArray = spool$locationOfBlockChangeRef.get();

            for (int l = 0; l < count; ++l) {
                if (currentArray[l] == short1) {
                    return;
                }
            }

            if (count >= currentArray.length) {
                short[] newArray = Arrays.copyOf(currentArray, currentArray.length << 1);
                if (!spool$locationOfBlockChangeRef.compareAndSet(currentArray, newArray)) {
                    continue;
                }
                locationOfBlockChange = newArray;
                continue;
            }

            if (spool$numberOfTilesToUpdate.compareAndSet(count, count + 1)) {
                currentArray[count] = short1;
                if (count == 0) {
                    this$0.chunkWatcherWithPlayers.add(this);
                }
                return;
            }
        }
    }

    @Redirect(
        method = { "removePlayer", "sendChunkUpdate" },
        at = @At(
            value = "FIELD",
            opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/server/management/PlayerManager$PlayerInstance;locationOfBlockChange:[S"))
    private short[] locationOfBlockChangeAccess(PlayerManager.PlayerInstance instance) {
        return spool$locationOfBlockChangeRef.get();
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

    @Redirect(
        method = { "sendChunkUpdate" },
        at = @At(
            value = "FIELD",
            opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/server/management/PlayerManager$PlayerInstance;flagsYAreasToUpdate:I"))
    private int flagYAreasToUpdateAccess(PlayerManager.PlayerInstance instance) {
        return spool$flagsYAreasToUpdateUpdater.get(this);
    }

    @Redirect(
        method = { "sendChunkUpdate" },
        at = @At(
            value = "FIELD",
            opcode = Opcodes.PUTFIELD,
            target = "Lnet/minecraft/server/management/PlayerManager$PlayerInstance;flagsYAreasToUpdate:I"))
    private void flagYAreasToUpdateUpdate(PlayerManager.PlayerInstance instance, int value) {
        spool$flagsYAreasToUpdateUpdater.set(this, value);
    }

    @SuppressWarnings("unchecked")
    @Redirect(
        method = { "removePlayer" },
        at = @At(value = "INVOKE", target = "Ljava/util/HashMap;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private <V> V playerAccess(HashMap<EntityPlayerMP, V> instance, Object key) {
        return (V) spool$players.get(key);
    }

    @SuppressWarnings({ "unchecked", "SuspiciousMethodCalls" })
    @Redirect(
        method = { "removePlayer" },
        at = @At(value = "INVOKE", target = "Ljava/util/HashMap;remove(Ljava/lang/Object;)Ljava/lang/Object;"))
    private <V> V playerRemove(HashMap<EntityPlayerMP, V> instance, Object key) {
        return (V) spool$players.remove(key);
    }

    @SuppressWarnings("unchecked")
    @Redirect(
        method = { "addPlayer" },
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/HashMap;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private <K, V> V playerPut(HashMap<K, V> instance, K key, V value) {
        return (V) spool$players.put((EntityPlayerMP) key, (Runnable) value);
    }
}
