package com.gamma.lmtm.util;

import static net.minecraftforge.event.terraingen.DecorateBiomeEvent.Decorate.EventType.*;
import static net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable.EventType.*;

import java.util.Random;

import net.minecraft.block.BlockFlower;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenBigMushroom;
import net.minecraft.world.gen.feature.WorldGenCactus;
import net.minecraft.world.gen.feature.WorldGenDeadBush;
import net.minecraft.world.gen.feature.WorldGenFlowers;
import net.minecraft.world.gen.feature.WorldGenLiquids;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraft.world.gen.feature.WorldGenPumpkin;
import net.minecraft.world.gen.feature.WorldGenReed;
import net.minecraft.world.gen.feature.WorldGenSand;
import net.minecraft.world.gen.feature.WorldGenWaterlily;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.OreGenEvent;
import net.minecraftforge.event.terraingen.TerrainGen;

public class ThreadedBiomeDecorator extends BiomeDecorator {

    protected void genStandardOre2(int x, int z, World world, Random rand, int p_76793_1_, WorldGenerator p_76793_2_,
        int p_76793_3_, int p_76793_4_) {
        for (int l = 0; l < p_76793_1_; ++l) {
            int i1 = x + rand.nextInt(16);
            int j1 = rand.nextInt(p_76793_4_) + rand.nextInt(p_76793_4_) + (p_76793_3_ - p_76793_4_);
            int k1 = z + rand.nextInt(16);
            p_76793_2_.generate(world, rand, i1, j1, k1);
        }
    }

    protected void genStandardOre1(int x, int z, World world, Random rand, int p_76795_1_, WorldGenerator p_76795_2_,
        int p_76795_3_, int p_76795_4_) {
        for (int l = 0; l < p_76795_1_; ++l) {
            int i1 = x + rand.nextInt(16);
            int j1 = rand.nextInt(p_76795_4_ - p_76795_3_) + p_76795_3_;
            int k1 = z + rand.nextInt(16);
            p_76795_2_.generate(world, rand, i1, j1, k1);
        }
    }

    @Override
    public void decorateChunk(World p_150512_1_, Random p_150512_2_, BiomeGenBase p_150512_3_, int p_150512_4_,
        int p_150512_5_) {
        synchronized (p_150512_1_) {
            this.genDecorations(p_150512_3_, p_150512_1_, p_150512_4_, p_150512_5_, p_150512_2_);
        }
    }

    public ThreadedBiomeDecorator() {
        this.sandGen = new WorldGenSand(Blocks.sand, 7);
        this.gravelAsSandGen = new WorldGenSand(Blocks.gravel, 6);
        this.dirtGen = new WorldGenMinable(Blocks.dirt, 32);
        this.gravelGen = new WorldGenMinable(Blocks.gravel, 32);
        this.coalGen = new WorldGenMinable(Blocks.coal_ore, 16);
        this.ironGen = new WorldGenMinable(Blocks.iron_ore, 8);
        this.goldGen = new WorldGenMinable(Blocks.gold_ore, 8);
        this.redstoneGen = new WorldGenMinable(Blocks.redstone_ore, 7);
        this.diamondGen = new WorldGenMinable(Blocks.diamond_ore, 7);
        this.lapisGen = new WorldGenMinable(Blocks.lapis_ore, 6);
        this.yellowFlowerGen = new WorldGenFlowers(Blocks.yellow_flower);
        this.mushroomBrownGen = new WorldGenFlowers(Blocks.brown_mushroom);
        this.mushroomRedGen = new WorldGenFlowers(Blocks.red_mushroom);
        this.bigMushroomGen = new WorldGenBigMushroom();
        this.reedGen = new WorldGenReed();
        this.cactusGen = new WorldGenCactus();
        this.waterlilyGen = new WorldGenWaterlily();
        this.flowersPerChunk = 2;
        this.grassPerChunk = 1;
        this.sandPerChunk = 1;
        this.sandPerChunk2 = 3;
        this.clayPerChunk = 1;
        this.generateLakes = true;
    }

    protected void genDecorations(BiomeGenBase p_150513_1_, World world, int x, int z, Random rand) {
        MinecraftForge.EVENT_BUS.post(new DecorateBiomeEvent.Pre(world, rand, x, z));
        this.generateOres(world, x, z, rand);
        int i;
        int j;
        int k;

        boolean doGen = TerrainGen.decorate(world, rand, x, z, SAND);
        for (i = 0; doGen && i < this.sandPerChunk2; ++i) {
            j = x + rand.nextInt(16) + 8;
            k = z + rand.nextInt(16) + 8;
            this.sandGen.generate(world, rand, j, world.getTopSolidOrLiquidBlock(j, k), k);
        }

        doGen = TerrainGen.decorate(world, rand, x, z, CLAY);
        for (i = 0; doGen && i < this.clayPerChunk; ++i) {
            j = x + rand.nextInt(16) + 8;
            k = z + rand.nextInt(16) + 8;
            this.clayGen.generate(world, rand, j, world.getTopSolidOrLiquidBlock(j, k), k);
        }

        doGen = TerrainGen.decorate(world, rand, x, z, SAND_PASS2);
        for (i = 0; doGen && i < this.sandPerChunk; ++i) {
            j = x + rand.nextInt(16) + 8;
            k = z + rand.nextInt(16) + 8;
            this.gravelAsSandGen.generate(world, rand, j, world.getTopSolidOrLiquidBlock(j, k), k);
        }

        i = this.treesPerChunk;

        if (rand.nextInt(10) == 0) {
            ++i;
        }

        int l;
        int i1;

        doGen = TerrainGen.decorate(world, rand, x, z, TREE);
        for (j = 0; doGen && j < i; ++j) {
            k = x + rand.nextInt(16) + 8;
            l = z + rand.nextInt(16) + 8;
            i1 = world.getHeightValue(k, l);
            WorldGenAbstractTree worldgenabstracttree = p_150513_1_.func_150567_a(rand);
            worldgenabstracttree.setScale(1.0D, 1.0D, 1.0D);

            if (worldgenabstracttree.generate(world, rand, k, i1, l)) {
                worldgenabstracttree.func_150524_b(world, rand, k, i1, l);
            }
        }

        doGen = TerrainGen.decorate(world, rand, x, z, BIG_SHROOM);
        for (j = 0; doGen && j < this.bigMushroomsPerChunk; ++j) {
            k = x + rand.nextInt(16) + 8;
            l = z + rand.nextInt(16) + 8;
            this.bigMushroomGen.generate(world, rand, k, world.getHeightValue(k, l), l);
        }

        doGen = TerrainGen.decorate(world, rand, x, z, FLOWERS);
        for (j = 0; doGen && j < this.flowersPerChunk; ++j) {
            k = x + rand.nextInt(16) + 8;
            l = z + rand.nextInt(16) + 8;
            i1 = nextInt(world.getHeightValue(k, l) + 32, rand);
            String s = p_150513_1_.func_150572_a(rand, k, i1, l);
            BlockFlower blockflower = BlockFlower.func_149857_e(s);

            if (blockflower.getMaterial() != Material.air) {
                this.yellowFlowerGen.func_150550_a(blockflower, BlockFlower.func_149856_f(s));
                this.yellowFlowerGen.generate(world, rand, k, i1, l);
            }
        }

        doGen = TerrainGen.decorate(world, rand, x, z, GRASS);
        for (j = 0; doGen && j < this.grassPerChunk; ++j) {
            k = x + rand.nextInt(16) + 8;
            l = z + rand.nextInt(16) + 8;
            i1 = nextInt(world.getHeightValue(k, l) * 2, rand);
            WorldGenerator worldgenerator = p_150513_1_.getRandomWorldGenForGrass(rand);
            worldgenerator.generate(world, rand, k, i1, l);
        }

        doGen = TerrainGen.decorate(world, rand, x, z, DEAD_BUSH);
        for (j = 0; doGen && j < this.deadBushPerChunk; ++j) {
            k = x + rand.nextInt(16) + 8;
            l = z + rand.nextInt(16) + 8;
            i1 = nextInt(world.getHeightValue(k, l) * 2, rand);
            (new WorldGenDeadBush(Blocks.deadbush)).generate(world, rand, k, i1, l);
        }

        doGen = TerrainGen.decorate(world, rand, x, z, LILYPAD);
        for (j = 0; doGen && j < this.waterlilyPerChunk; ++j) {
            k = x + rand.nextInt(16) + 8;
            l = z + rand.nextInt(16) + 8;

            for (i1 = nextInt(world.getHeightValue(k, l) * 2, rand); i1 > 0 && world.isAirBlock(k, i1 - 1, l); --i1) {
                ;
            }

            this.waterlilyGen.generate(world, rand, k, i1, l);
        }

        doGen = TerrainGen.decorate(world, rand, x, z, SHROOM);
        for (j = 0; doGen && j < this.mushroomsPerChunk; ++j) {
            if (rand.nextInt(4) == 0) {
                k = x + rand.nextInt(16) + 8;
                l = z + rand.nextInt(16) + 8;
                i1 = world.getHeightValue(k, l);
                this.mushroomBrownGen.generate(world, rand, k, i1, l);
            }

            if (rand.nextInt(8) == 0) {
                k = x + rand.nextInt(16) + 8;
                l = z + rand.nextInt(16) + 8;
                i1 = nextInt(world.getHeightValue(k, l) * 2, rand);
                this.mushroomRedGen.generate(world, rand, k, i1, l);
            }
        }

        if (doGen && rand.nextInt(4) == 0) {
            j = x + rand.nextInt(16) + 8;
            k = z + rand.nextInt(16) + 8;
            l = nextInt(world.getHeightValue(j, k) * 2, rand);
            this.mushroomBrownGen.generate(world, rand, j, l, k);
        }

        if (doGen && rand.nextInt(8) == 0) {
            j = x + rand.nextInt(16) + 8;
            k = z + rand.nextInt(16) + 8;
            l = nextInt(world.getHeightValue(j, k) * 2, rand);
            this.mushroomRedGen.generate(world, rand, j, l, k);
        }

        doGen = TerrainGen.decorate(world, rand, x, z, REED);
        for (j = 0; doGen && j < this.reedsPerChunk; ++j) {
            k = x + rand.nextInt(16) + 8;
            l = z + rand.nextInt(16) + 8;
            i1 = nextInt(world.getHeightValue(k, l) * 2, rand);
            this.reedGen.generate(world, rand, k, i1, l);
        }

        for (j = 0; doGen && j < 10; ++j) {
            k = x + rand.nextInt(16) + 8;
            l = z + rand.nextInt(16) + 8;
            i1 = nextInt(world.getHeightValue(k, l) * 2, rand);
            this.reedGen.generate(world, rand, k, i1, l);
        }

        doGen = TerrainGen.decorate(world, rand, x, z, PUMPKIN);
        if (doGen && rand.nextInt(32) == 0) {
            j = x + rand.nextInt(16) + 8;
            k = z + rand.nextInt(16) + 8;
            l = nextInt(world.getHeightValue(j, k) * 2, rand);
            (new WorldGenPumpkin()).generate(world, rand, j, l, k);
        }

        doGen = TerrainGen.decorate(world, rand, x, z, CACTUS);
        for (j = 0; doGen && j < this.cactiPerChunk; ++j) {
            k = x + rand.nextInt(16) + 8;
            l = z + rand.nextInt(16) + 8;
            i1 = nextInt(world.getHeightValue(k, l) * 2, rand);
            this.cactusGen.generate(world, rand, k, i1, l);
        }

        doGen = TerrainGen.decorate(world, rand, x, z, LAKE);
        if (doGen && this.generateLakes) {
            for (j = 0; j < 50; ++j) {
                k = x + rand.nextInt(16) + 8;
                l = rand.nextInt(rand.nextInt(248) + 8);
                i1 = z + rand.nextInt(16) + 8;
                (new WorldGenLiquids(Blocks.flowing_water)).generate(world, rand, k, l, i1);
            }

            for (j = 0; j < 20; ++j) {
                k = x + rand.nextInt(16) + 8;
                l = rand.nextInt(rand.nextInt(rand.nextInt(240) + 8) + 8);
                i1 = z + rand.nextInt(16) + 8;
                (new WorldGenLiquids(Blocks.flowing_lava)).generate(world, rand, k, l, i1);
            }
        }

        MinecraftForge.EVENT_BUS.post(new DecorateBiomeEvent.Post(world, rand, x, z));
    }

    protected void generateOres(World world, int x, int z, Random rand) {
        MinecraftForge.ORE_GEN_BUS.post(new OreGenEvent.Pre(world, rand, x, z));
        if (TerrainGen.generateOre(world, rand, dirtGen, x, z, DIRT))
            this.genStandardOre1(x, z, world, rand, 20, this.dirtGen, 0, 256);
        if (TerrainGen.generateOre(world, rand, gravelGen, x, z, GRAVEL))
            this.genStandardOre1(x, z, world, rand, 10, this.gravelGen, 0, 256);
        if (TerrainGen.generateOre(world, rand, coalGen, x, z, COAL))
            this.genStandardOre1(x, z, world, rand, 20, this.coalGen, 0, 128);
        if (TerrainGen.generateOre(world, rand, ironGen, x, z, IRON))
            this.genStandardOre1(x, z, world, rand, 20, this.ironGen, 0, 64);
        if (TerrainGen.generateOre(world, rand, goldGen, x, z, GOLD))
            this.genStandardOre1(x, z, world, rand, 2, this.goldGen, 0, 32);
        if (TerrainGen.generateOre(world, rand, redstoneGen, x, z, REDSTONE))
            this.genStandardOre1(x, z, world, rand, 8, this.redstoneGen, 0, 16);
        if (TerrainGen.generateOre(world, rand, diamondGen, x, z, DIAMOND))
            this.genStandardOre1(x, z, world, rand, 1, this.diamondGen, 0, 16);
        if (TerrainGen.generateOre(world, rand, lapisGen, x, z, LAPIS))
            this.genStandardOre2(x, z, world, rand, 1, this.lapisGen, 16, 16);
        MinecraftForge.ORE_GEN_BUS.post(new OreGenEvent.Post(world, rand, x, z));
    }

    private int nextInt(int i, Random rand) {
        if (i <= 1) return 0;
        return rand.nextInt(i);
    }
}
