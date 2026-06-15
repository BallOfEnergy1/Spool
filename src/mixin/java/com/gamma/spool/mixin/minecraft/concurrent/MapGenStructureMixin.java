package com.gamma.spool.mixin.minecraft.concurrent;

import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.StructureStart;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(value = MapGenStructure.class)
public abstract class MapGenStructureMixin {

    @Shadow
    protected Map<Long, StructureStart> structureMap;

    @WrapMethod(method = "func_151538_a")
    private void wrapped(World worldIn, int p_151538_2_, int p_151538_3_, int p_151538_4_, int p_151538_5_,
        Block[] p_151538_6_, Operation<Void> original) {
        synchronized (this.structureMap) {
            original.call(worldIn, p_151538_2_, p_151538_3_, p_151538_4_, p_151538_5_, p_151538_6_);
        }
    }

    @WrapMethod(method = "generateStructuresInChunk")
    private boolean wrapped(World p_75051_1_, Random p_75051_2_, int p_75051_3_, int p_75051_4_,
        Operation<Boolean> original) {
        synchronized (this.structureMap) {
            return original.call(p_75051_1_, p_75051_2_, p_75051_3_, p_75051_4_);
        }
    }

    @WrapMethod(method = "func_143028_c")
    private StructureStart wrapped_143028_c(int p_143028_1_, int p_143028_2_, int p_143028_3_,
        Operation<StructureStart> original) {
        synchronized (this.structureMap) {
            return original.call(p_143028_1_, p_143028_2_, p_143028_3_);
        }
    }

    @WrapMethod(method = "func_142038_b")
    private boolean wrapped(int p_142038_1_, int p_142038_2_, int p_142038_3_, Operation<Boolean> original) {
        synchronized (this.structureMap) {
            return original.call(p_142038_1_, p_142038_2_, p_142038_3_);
        }
    }

    @WrapMethod(method = "func_151545_a")
    private ChunkPosition wrapped_151545_a(World p_151545_1_, int p_151545_2_, int p_151545_3_, int p_151545_4_,
        Operation<ChunkPosition> original) {
        synchronized (this.structureMap) {
            return original.call(p_151545_1_, p_151545_2_, p_151545_3_, p_151545_4_);
        }
    }

    @WrapMethod(method = "func_143027_a")
    private void wrapped(World world, Operation<Void> original) {
        synchronized (this.structureMap) {
            original.call(world);
        }
    }

}
