package com.gamma.spool.mixin.minecraft;

import java.util.Set;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.ISaveHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;

@Mixin(WorldClient.class)
public abstract class WorldClientMixin extends World {

    public WorldClientMixin(ISaveHandler p_i45368_1_, String p_i45368_2_, WorldProvider p_i45368_3_,
        WorldSettings p_i45368_4_, Profiler p_i45368_5_) {
        super(p_i45368_1_, p_i45368_2_, p_i45368_3_, p_i45368_4_, p_i45368_5_);
    }

    @Shadow
    private Set<Entity> entitySpawnQueue;

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void onInit(NetHandlerPlayClient p_i45063_1_, WorldSettings p_i45063_2_, int p_i45063_3_,
        EnumDifficulty p_i45063_4_, Profiler p_i45063_5_, CallbackInfo ci) {
        entitySpawnQueue = ObjectSets.synchronize(new ObjectOpenHashSet<>());
    }

    @WrapMethod(method = "tick")
    private void tick(Operation<Void> original) {
        synchronized (entitySpawnQueue) {
            original.call();
        }
    }

    // TODO: This is a really bad idea. Figure out a better way to do this.
    @WrapMethod(method = "removeAllEntities")
    private void removeAllEntities(Operation<Void> original) {
        synchronized (loadedEntityList) {
            synchronized (unloadedEntityList) {
                original.call();
            }
        }
    }
}
