package com.gamma.spool.mixin.minecraft.nonconcurrent;

import net.minecraft.block.Block;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.ChunkProviderGenerate;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(ChunkProviderGenerate.class)
public abstract class ChunkProviderGenerateMixin {

    @WrapMethod(method = "replaceBlocksForBiome")
    public void spool$replaceBlocksForBiome(int p_147422_1_, int p_147422_2_, Block[] p_147422_3_, byte[] p_147422_4_,
        BiomeGenBase[] p_147422_5_, Operation<Void> original) {
        synchronized (this) {
            original.call(p_147422_1_, p_147422_2_, p_147422_3_, p_147422_4_, p_147422_5_);
        }
    }
}
