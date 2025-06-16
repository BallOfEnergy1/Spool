package com.gamma.lmtm.mixin.minecraft;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.ChunkProviderGenerate;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.ChunkProviderEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cpw.mods.fml.common.eventhandler.Event;

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

    @Inject(method = "replaceBlocksForBiome", at = @At("INVOKE"), cancellable = true)
    public void replaceBlocksForBiome(int p_147422_1_, int p_147422_2_, Block[] p_147422_3_, byte[] p_147422_4_,
        BiomeGenBase[] p_147422_5_, CallbackInfo ci) {
        ChunkProviderGenerate thisObject = (ChunkProviderGenerate) (Object) this;
        synchronized (this) {
            ChunkProviderEvent.ReplaceBiomeBlocks event = new ChunkProviderEvent.ReplaceBiomeBlocks(
                thisObject,
                p_147422_1_,
                p_147422_2_,
                p_147422_3_,
                p_147422_4_,
                p_147422_5_,
                this.worldObj);
            MinecraftForge.EVENT_BUS.post(event);
            if (event.getResult() == Event.Result.DENY) return;

            double d0 = 0.03125D;
            this.stoneNoise = this.field_147430_m.func_151599_a(
                this.stoneNoise,
                (double) (p_147422_1_ * 16),
                (double) (p_147422_2_ * 16),
                16,
                16,
                d0 * 2.0D,
                d0 * 2.0D,
                1.0D);

            for (int k = 0; k < 16; ++k) {
                for (int l = 0; l < 16; ++l) {
                    BiomeGenBase biomegenbase = p_147422_5_[l + k * 16];
                    biomegenbase.genTerrainBlocks(
                        this.worldObj,
                        this.rand,
                        p_147422_3_,
                        p_147422_4_,
                        p_147422_1_ * 16 + k,
                        p_147422_2_ * 16 + l,
                        this.stoneNoise[l + k * 16]);
                }
            }
        }
        ci.cancel();
    }
}
