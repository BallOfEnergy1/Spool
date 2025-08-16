package com.gamma.spool.mixin.minecraft;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.ChunkProviderGenerate;
import net.minecraft.world.gen.NoiseGeneratorPerlin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(ChunkProviderGenerate.class)
public abstract class ChunkProviderGenerateMixin {

    @Shadow
    private double[] stoneNoise;

    @Shadow
    private NoiseGeneratorPerlin field_147430_m;

    @Shadow
    private World worldObj;

    @Shadow
    private Random rand;

    @WrapMethod(method = "replaceBlocksForBiome")
    public void lmtm$replaceBlocksForBiome(int p_147422_1_, int p_147422_2_, Block[] p_147422_3_, byte[] p_147422_4_,
        BiomeGenBase[] p_147422_5_, Operation<Void> original) {
        synchronized (this) {
            original.call(p_147422_1_, p_147422_2_, p_147422_3_, p_147422_4_, p_147422_5_);
        }
    }
}
