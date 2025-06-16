package com.gamma.lmtm.util;

import java.util.Random;

import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.feature.WorldGenerator;

public class ThreadedBiomeEndDecorator extends ThreadedBiomeDecorator {

    protected WorldGenerator spikeGen;

    protected void genDecorations(BiomeGenBase p_150513_1_, World world, int x, int z, Random rand) {
        this.generateOres();

        if (rand.nextInt(5) == 0) {
            int i = x + rand.nextInt(16) + 8;
            int j = z + rand.nextInt(16) + 8;
            int k = world.getTopSolidOrLiquidBlock(i, j);
            this.spikeGen.generate(world, rand, i, k, j);
        }

        if (x == 0 && z == 0) {
            EntityDragon entitydragon = new EntityDragon(world);
            entitydragon.setLocationAndAngles(0.0D, 128.0D, 0.0D, rand.nextFloat() * 360.0F, 0.0F);
            world.spawnEntityInWorld(entitydragon);
        }
    }
}
