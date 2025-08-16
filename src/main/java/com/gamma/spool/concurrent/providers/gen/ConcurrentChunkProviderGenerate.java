package com.gamma.spool.concurrent.providers.gen;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderGenerate;

import com.gamma.spool.util.concurrent.interfaces.IThreadSafe;

@SuppressWarnings("unused")
public class ConcurrentChunkProviderGenerate extends ChunkProviderGenerate implements IThreadSafe {

    public ConcurrentChunkProviderGenerate(World p_i2006_1_, long p_i2006_2_, boolean p_i2006_4_) {
        super(p_i2006_1_, p_i2006_2_, p_i2006_4_);
    }

    // Exclusive lock. TODO: Threaded worldgen (biomes/structures)
    public synchronized void func_147424_a(int p_147419_1_, int p_147419_2_, Block[] p_147419_3_) {
        super.func_147424_a(p_147419_1_, p_147419_2_, p_147419_3_);
    }

    // Exclusive lock. TODO: Threaded worldgen (biomes/structures)
    public synchronized Chunk provideChunk(int p_73154_1_, int p_73154_2_) {
        return super.provideChunk(p_73154_1_, p_73154_2_);
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
