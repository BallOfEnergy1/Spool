package com.gamma.spool.mixin.minecraft;

import net.minecraft.world.SpawnerAnimals;
import net.minecraft.world.WorldServer;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(SpawnerAnimals.class)
public abstract class SpawnerAnimalsMixin {

    @WrapMethod(method = "findChunksForSpawning")
    private int findChunksForSpawning(WorldServer p_77192_1_, boolean p_77192_2_, boolean p_77192_3_,
        boolean p_77192_4_, Operation<Integer> original) {
        synchronized (p_77192_1_.playerEntities) {
            return original.call(p_77192_1_, p_77192_2_, p_77192_3_, p_77192_4_);
        }
    }
}
