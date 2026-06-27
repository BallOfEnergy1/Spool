package com.gamma.spool.mixin.minecraft;

import java.util.HashMap;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.SpawnerAnimals;
import net.minecraft.world.WorldServer;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

@Mixin(SpawnerAnimals.class)
public abstract class SpawnerAnimalsMixin {

    @Redirect(
        method = "findChunksForSpawning",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/WorldServer;playerEntities:Ljava/util/List;",
            opcode = Opcodes.GETFIELD,
            ordinal = 0))
    private List<EntityPlayer> redirectedGetField1(WorldServer instance,
        @Share("list") LocalRef<List<EntityPlayer>> list) {
        List<EntityPlayer> newList = new ObjectArrayList<>(instance.playerEntities);
        list.set(newList);
        return newList;
    }

    @Redirect(
        method = "findChunksForSpawning",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/WorldServer;playerEntities:Ljava/util/List;",
            opcode = Opcodes.GETFIELD,
            ordinal = 1))
    private List<EntityPlayer> redirectedGetField2(WorldServer instance,
        @Share("list") LocalRef<List<EntityPlayer>> list) {
        return list.get();
    }

    @Redirect(method = "findChunksForSpawning", at = @At(value = "INVOKE", target = "Ljava/util/HashMap;clear()V"))
    private void redirectedClear(HashMap<?, ?> instance,
        @Share("eligibleChunks") LocalRef<HashMap<?, ?>> eligibleChunks) {
        eligibleChunks.set(new HashMap<>());
    }

    @Redirect(
        method = "findChunksForSpawning",
        slice = @Slice(from = @At(value = "INVOKE", target = "Ljava/util/HashMap;clear()V")),
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/SpawnerAnimals;eligibleChunksForSpawning:Ljava/util/HashMap;",
            opcode = Opcodes.GETFIELD))
    private HashMap<?, ?> redirectedFieldAccess(SpawnerAnimals instance,
        @Share("eligibleChunks") LocalRef<HashMap<?, ?>> eligibleChunks) {
        return eligibleChunks.get();
    }
}
