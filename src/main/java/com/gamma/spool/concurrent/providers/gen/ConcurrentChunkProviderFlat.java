package com.gamma.spool.concurrent.providers.gen;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkProviderFlat;
import net.minecraft.world.gen.structure.MapGenStructure;

import com.gamma.spool.concurrent.ConcurrentChunk;
import com.gamma.spool.concurrent.ConcurrentExtendedBlockStorage;
import com.gamma.spool.util.concurrent.interfaces.IThreadSafe;

public class ConcurrentChunkProviderFlat extends ChunkProviderFlat implements IThreadSafe {

    public ConcurrentChunkProviderFlat(World p_i2004_1_, long p_i2004_2_, boolean p_i2004_4_, String p_i2004_5_) {
        super(p_i2004_1_, p_i2004_2_, p_i2004_4_, p_i2004_5_);
    }

    public Chunk provideChunk(int p_73154_1_, int p_73154_2_) {
        ConcurrentChunk chunk = new ConcurrentChunk(this.worldObj, p_73154_1_, p_73154_2_);
        int l;

        for (int k = 0; k < this.cachedBlockIDs.length; ++k) {
            Block block = this.cachedBlockIDs[k];

            if (block != null) {
                l = k >> 4;

                ExtendedBlockStorage[] storages = chunk.getBlockStorageArray();

                ExtendedBlockStorage extendedblockstorage = storages[l];

                if (extendedblockstorage == null) {
                    extendedblockstorage = new ConcurrentExtendedBlockStorage(k, !this.worldObj.provider.hasNoSky);
                    storages[l] = extendedblockstorage;
                    chunk.setStorageArrays(storages);
                }

                for (int i1 = 0; i1 < 16; ++i1) {
                    for (int j1 = 0; j1 < 16; ++j1) {
                        extendedblockstorage.func_150818_a(i1, k & 15, j1, block);
                        extendedblockstorage.setExtBlockMetadata(i1, k & 15, j1, this.cachedBlockMetadata[k]);
                    }
                }
            }
        }

        chunk.generateSkylightMap();
        // Exclusive lock. TODO: Threaded worldgen (biomes/structures)
        synchronized (this) {
            BiomeGenBase[] abiomegenbase = this.worldObj.getWorldChunkManager()
                .loadBlockGeneratorData(null, p_73154_1_ * 16, p_73154_2_ * 16, 16, 16);
            byte[] abyte = chunk.getBiomeArray();

            for (l = 0; l < abyte.length; ++l) {
                abyte[l] = (byte) abiomegenbase[l].biomeID;
            }

            for (Object mapGen : this.structureGenerators) {
                ((MapGenStructure) mapGen).func_151539_a(this, this.worldObj, p_73154_1_, p_73154_2_, null);
            }
        }

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
