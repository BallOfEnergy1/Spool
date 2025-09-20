package com.gamma.spool.concurrent.providers.gen;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderGenerate;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.MapGenCaves;
import net.minecraft.world.gen.MapGenRavine;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.MapGenScatteredFeature;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.ChunkProviderEvent;

import com.gamma.spool.concurrent.ConcurrentChunk;

import cpw.mods.fml.common.eventhandler.Event;

@SuppressWarnings("unused")
public class ConcurrentChunkProviderGenerate extends ChunkProviderGenerate implements IFullAsync {

    public ConcurrentChunkProviderGenerate(World p_i2006_1_, long p_i2006_2_, boolean p_i2006_4_) {
        super(p_i2006_1_, p_i2006_2_, p_i2006_4_);
    }

    // Exclusive lock. TODO: Threaded worldgen (biomes/structures)
    public synchronized void func_147424_a(int p_147419_1_, int p_147419_2_, Block[] p_147419_3_) {
        super.func_147424_a(p_147419_1_, p_147419_2_, p_147419_3_);
    }

    public synchronized Chunk provideChunk(int p_73154_1_, int p_73154_2_) {
        // Yes, this is technically inefficient.
        // Sue me, I'm lazy.
        Chunk chunk = super.provideChunk(p_73154_1_, p_73154_2_);

        byte[] bytes = new byte[256];

        for (int k = 0; k < bytes.length; ++k) {
            bytes[k] = (byte) this.biomesForGeneration[k].biomeID;
        }

        chunk.setBiomeArray(bytes);
        return chunk;
    }

    public Chunk provideChunkAsync(final int x, final int y) {

        // BEGIN PROVIDE CHUNK

        Random rand = new Random((long) x * 341873128712L + (long) y * 132897987541L);
        Block[] ablock = new Block[65536];
        byte[] abyte = new byte[65536];
        this.func_147424_a(x, y, ablock);

        BiomeGenBase[] biomesForGeneration;

        synchronized (this.worldObj.getWorldChunkManager()) {
            // TODO: Check if this is safe at all???
            biomesForGeneration = this.worldObj.getWorldChunkManager()
                .loadBlockGeneratorData(null, x * 16, y * 16, 16, 16);
        }

        this.replaceBlocksForBiomeAsync(x, y, ablock, abyte, biomesForGeneration, rand);
        MapGenBase caveGenerator = new MapGenCaves();
        caveGenerator.func_151539_a(this, this.worldObj, x, y, ablock);
        MapGenBase ravineGenerator = new MapGenRavine();
        ravineGenerator.func_151539_a(this, this.worldObj, x, y, ablock);

        if (this.mapFeaturesEnabled) {
            MapGenMineshaft mineshaftGenerator = new MapGenMineshaft();
            mineshaftGenerator.func_151539_a(this, this.worldObj, x, y, ablock);
            MapGenVillage villageGenerator = new MapGenVillage();
            villageGenerator.func_151539_a(this, this.worldObj, x, y, ablock);
            MapGenStronghold strongholdGenerator = new MapGenStronghold();
            strongholdGenerator.func_151539_a(this, this.worldObj, x, y, ablock);
            MapGenScatteredFeature scatteredFeatureGenerator = new MapGenScatteredFeature();
            scatteredFeatureGenerator.func_151539_a(this, this.worldObj, x, y, ablock);
        }

        ConcurrentChunk chunk = new ConcurrentChunk(this.worldObj, ablock, abyte, x, y);
        byte[] abyte1 = chunk.getBiomeArray();

        for (int k = 0; k < abyte1.length; ++k) {
            abyte1[k] = (byte) biomesForGeneration[k].biomeID;
        }

        chunk.generateSkylightMap();

        // END PROVIDE CHUNK

        return chunk;
    }

    /**
     * Async-compatible version of `replaceBlocksForBiome()`
     * 
     * @param x           X-Coordinate of chunk.
     * @param y           Y-Coordinate of chunk.
     * @param p_147422_3_ Block array to fill.
     * @param p_147422_4_ Metadata array to fill (?)
     * @param p_147422_5_ Array of biomes to replace for.
     * @param rand        Random object for this chunk.
     */
    public void replaceBlocksForBiomeAsync(int x, int y, Block[] p_147422_3_, byte[] p_147422_4_,
        BiomeGenBase[] p_147422_5_, Random rand) {
        ChunkProviderEvent.ReplaceBiomeBlocks event = new ChunkProviderEvent.ReplaceBiomeBlocks(
            this,
            x,
            y,
            p_147422_3_,
            p_147422_4_,
            p_147422_5_,
            this.worldObj);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.getResult() == Event.Result.DENY) return;

        double d0 = 0.03125D;
        double[] stoneNoise = this.field_147430_m
            .func_151599_a(null, x * 16, y * 16, 16, 16, d0 * 2.0D, d0 * 2.0D, 1.0D);

        for (int k = 0; k < 16; ++k) {
            for (int l = 0; l < 16; ++l) {
                BiomeGenBase biomegenbase = p_147422_5_[l + k * 16];
                biomegenbase.genTerrainBlocks(
                    this.worldObj,
                    rand,
                    p_147422_3_,
                    p_147422_4_,
                    x * 16 + k,
                    y * 16 + l,
                    stoneNoise[l + k * 16]);
            }
        }
    }

    // Exclusive lock. TODO: Threaded worldgen (biomes/structures)
    public synchronized void populate(IChunkProvider p_73153_1_, int p_73153_2_, int p_73153_3_) {
        super.populate(p_73153_1_, p_73153_2_, p_73153_3_);
    }

    // Exclusive lock. TODO: Threaded worldgen (biomes/structures)
    public synchronized List<BiomeGenBase.SpawnListEntry> getPossibleCreatures(EnumCreatureType p_73155_1_,
        int p_73155_2_, int p_73155_3_, int p_73155_4_) {
        return super.getPossibleCreatures(p_73155_1_, p_73155_2_, p_73155_3_, p_73155_4_);
    }

    // Exclusive lock. TODO: Threaded worldgen (biomes/structures)
    public synchronized ChunkPosition func_147416_a(World p_147416_1_, String p_147416_2_, int p_147416_3_,
        int p_147416_4_, int p_147416_5_) {
        return super.func_147416_a(p_147416_1_, p_147416_2_, p_147416_3_, p_147416_4_, p_147416_5_);
    }

    // Exclusive lock. TODO: Threaded worldgen (biomes/structures)
    public synchronized void recreateStructures(int p_82695_1_, int p_82695_2_) {
        super.recreateStructures(p_82695_1_, p_82695_2_);
    }
}
