package com.gamma.spool.concurrent.providers.gen;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderEnd;

import com.gamma.spool.compat.endlessids.Compat;
import com.gamma.spool.compat.endlessids.ConcurrentChunkWrapper;
import com.gamma.spool.concurrent.ConcurrentChunk;
import com.gamma.spool.core.SpoolCompat;
import com.gamma.spool.util.concurrent.interfaces.IThreadSafe;

@SuppressWarnings("unused")
public class ConcurrentChunkProviderEnd extends ChunkProviderEnd implements IThreadSafe {

    public ConcurrentChunkProviderEnd(World p_i2007_1_, long p_i2007_2_) {
        super(p_i2007_1_, p_i2007_2_);
    }

    // Exclusive lock. TODO: Threaded worldgen (biomes/structures)
    public synchronized void func_147420_a(int p_147420_1_, int p_147420_2_, Block[] p_147420_3_,
        BiomeGenBase[] p_147420_4_) {
        super.func_147420_a(p_147420_1_, p_147420_2_, p_147420_3_, p_147420_4_);
    }

    // Exclusive lock. TODO: Threaded worldgen (biomes/structures)
    public synchronized Chunk provideChunk(int p_73154_1_, int p_73154_2_) {
        this.endRNG.setSeed((long) p_73154_1_ * 341873128712L + (long) p_73154_2_ * 132897987541L);
        Block[] ablock = new Block[32768];
        byte[] meta = new byte[ablock.length];
        this.biomesForGeneration = this.endWorld.getWorldChunkManager()
            .loadBlockGeneratorData(this.biomesForGeneration, p_73154_1_ * 16, p_73154_2_ * 16, 16, 16);
        this.func_147420_a(p_73154_1_, p_73154_2_, ablock, this.biomesForGeneration);
        this.replaceBiomeBlocks(p_73154_1_, p_73154_2_, ablock, this.biomesForGeneration, meta);
        ConcurrentChunk chunk;
        if (SpoolCompat.isModLoaded("endlessids")) {
            chunk = new ConcurrentChunkWrapper(this.endWorld, ablock, meta, p_73154_1_, p_73154_2_);
        } else {
            chunk = new ConcurrentChunk(this.endWorld, ablock, meta, p_73154_1_, p_73154_2_);
        }

        Compat.setChunkBiomes(chunk, this.biomesForGeneration);

        chunk.generateSkylightMap();
        return chunk;
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
}
