package com.gamma.spool.concurrent.providers.gen;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderHell;

import com.gamma.spool.compat.endlessids.Compat;
import com.gamma.spool.compat.endlessids.ConcurrentChunkWrapper;
import com.gamma.spool.concurrent.ConcurrentChunk;
import com.gamma.spool.core.SpoolCompat;
import com.gamma.spool.util.concurrent.interfaces.IThreadSafe;

@SuppressWarnings("unused")
public class ConcurrentChunkProviderHell extends ChunkProviderHell implements IThreadSafe {

    public ConcurrentChunkProviderHell(World p_i2005_1_, long p_i2005_2_) {
        super(p_i2005_1_, p_i2005_2_);
    }

    // Exclusive lock. TODO: Threaded worldgen (biomes/structures)
    public synchronized void func_147419_a(int p_147419_1_, int p_147419_2_, Block[] p_147419_3_) {
        super.func_147419_a(p_147419_1_, p_147419_2_, p_147419_3_);
    }

    // Exclusive lock. TODO: Threaded worldgen (biomes/structures)
    public synchronized Chunk provideChunk(int p_73154_1_, int p_73154_2_) {
        this.hellRNG.setSeed((long) p_73154_1_ * 341873128712L + (long) p_73154_2_ * 132897987541L);
        Block[] ablock = new Block[32768];
        byte[] meta = new byte[ablock.length];
        BiomeGenBase[] abiomegenbase = this.worldObj.getWorldChunkManager()
            .loadBlockGeneratorData(null, p_73154_1_ * 16, p_73154_2_ * 16, 16, 16);
        this.func_147419_a(p_73154_1_, p_73154_2_, ablock);
        this.replaceBiomeBlocks(p_73154_1_, p_73154_2_, ablock, meta, abiomegenbase);
        this.netherCaveGenerator.func_151539_a(this, this.worldObj, p_73154_1_, p_73154_2_, ablock);
        this.genNetherBridge.func_151539_a(this, this.worldObj, p_73154_1_, p_73154_2_, ablock);
        ConcurrentChunk chunk;
        if (SpoolCompat.isEndlessIDsLoaded) {
            chunk = new ConcurrentChunkWrapper(this.worldObj, ablock, meta, p_73154_1_, p_73154_2_);
        } else {
            chunk = new ConcurrentChunk(this.worldObj, ablock, meta, p_73154_1_, p_73154_2_);
        }

        byte[] bytes = new byte[256];

        Compat.setChunkBiomes(chunk, abiomegenbase);

        chunk.resetRelightChecks();
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

    // Exclusive lock. TODO: Threaded worldgen (biomes/structures)
    public synchronized void recreateStructures(int p_82695_1_, int p_82695_2_) {
        super.recreateStructures(p_82695_1_, p_82695_2_);
    }
}
