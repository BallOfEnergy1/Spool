package com.gamma.spool.concurrent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.command.IEntitySelector;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.world.ChunkEvent;

import com.gamma.spool.util.concurrent.IConcurrent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ConcurrentChunk extends Chunk implements IConcurrent {

    public final AtomicBoolean isModified = new AtomicBoolean(false);

    public final AtomicBoolean hasEntities = new AtomicBoolean(false);

    public final AtomicBoolean isChunkLoaded = new AtomicBoolean(false);

    public final AtomicInteger queuedLightChecks = new AtomicInteger(0);

    public final StampedLock lock = new StampedLock();

    @Override
    public StampedLock getLock() {
        return lock;
    }

    public ConcurrentChunk(World p_i1995_1_, int p_i1995_2_, int p_i1995_3_) {
        super(p_i1995_1_, p_i1995_2_, p_i1995_3_);
        this.queuedLightChecks.set(4096);
        writeLock();
        try {
            this.storageArrays = new ConcurrentExtendedBlockStorage[16];
        } finally {
            writeUnlock();
        }
    }

    public ConcurrentChunk(World p_i45446_1_, Block[] p_i45446_2_, int p_i45446_3_, int p_i45446_4_) {
        this(p_i45446_1_, p_i45446_3_, p_i45446_4_);
        writeLock();
        try {
            int k = p_i45446_2_.length / 256;
            boolean flag = !p_i45446_1_.provider.hasNoSky;

            for (int l = 0; l < 16; ++l) {
                for (int i1 = 0; i1 < 16; ++i1) {
                    for (int j1 = 0; j1 < k; ++j1) {
                        Block block = p_i45446_2_[l << 11 | i1 << 7 | j1];

                        if (block != null && block.getMaterial() != Material.air) {
                            int k1 = j1 >> 4;

                            if (this.storageArrays[k1] == null) {
                                this.storageArrays[k1] = new ConcurrentExtendedBlockStorage(k1 << 4, flag);
                            }

                            this.storageArrays[k1].func_150818_a(l, j1 & 15, i1, block);
                        }
                    }
                }
            }
        } finally {
            writeUnlock();
        }
    }

    public ConcurrentChunk(World p_i45447_1_, Block[] p_i45447_2_, byte[] p_i45447_3_, int p_i45447_4_,
        int p_i45447_5_) {
        this(p_i45447_1_, p_i45447_4_, p_i45447_5_);
        writeLock();
        try {
            int k = p_i45447_2_.length / 256;
            boolean flag = !p_i45447_1_.provider.hasNoSky;

            for (int l = 0; l < 16; ++l) {
                for (int i1 = 0; i1 < 16; ++i1) {
                    for (int j1 = 0; j1 < k; ++j1) {
                        int k1 = l * k * 16 | i1 * k | j1;
                        Block block = p_i45447_2_[k1];

                        if (block != null && block != Blocks.air) {
                            int l1 = j1 >> 4;

                            if (this.storageArrays[l1] == null) {
                                this.storageArrays[l1] = new ConcurrentExtendedBlockStorage(l1 << 4, flag);
                            }

                            this.storageArrays[l1].func_150818_a(l, j1 & 15, i1, block);
                            this.storageArrays[l1].setExtBlockMetadata(l, j1 & 15, i1, p_i45447_3_[k1]);
                        }
                    }
                }
            }
        } finally {
            writeUnlock();
        }
    }

    /**
     * Checks whether the chunk is at the X/Z location specified
     */
    public boolean isAtLocation(int x, int z) {
        readLock();
        try {
            return x == this.xPosition && z == this.zPosition;
        } finally {
            readUnlock();
        }
    }

    /**
     * Returns the value in the height map at this x, z coordinate in the chunk
     */
    public int getHeightValue(int x, int z) {
        readLock();
        try {
            return this.heightMap[z << 4 | x];
        } finally {
            readUnlock();
        }
    }

    /**
     * Returns the topmost ExtendedBlockStorage instance for this Chunk that actually contains a block.
     */
    public int getTopFilledSegment() {
        readLock();
        try {
            for (int i = this.storageArrays.length - 1; i >= 0; --i) {
                if (this.storageArrays[i] != null) {
                    return this.storageArrays[i].getYLocation();
                }
            }
            return 0;
        } finally {
            readUnlock();
        }
    }

    /**
     * Returns the ExtendedBlockStorage array for this Chunk.
     */
    public ExtendedBlockStorage[] getBlockStorageArray() {
        readLock();
        try {
            return this.storageArrays;
        } finally {
            readUnlock();
        }
    }

    /**
     * Generates the height map for a chunk from scratch
     */
    @SideOnly(Side.CLIENT)
    public void generateHeightMap() {
        writeLock();
        try {
            int i = this.getTopFilledSegment();
            this.heightMapMinimum = Integer.MAX_VALUE;

            for (int j = 0; j < 16; ++j) {
                int k = 0;

                while (k < 16) {
                    this.precipitationHeightMap[j + (k << 4)] = -999;
                    int l = i + 16 - 1;

                    while (true) {
                        if (l > 0) {
                            if (func_150808_b(j, l - 1, k) == 0) {
                                --l;
                                continue;
                            }

                            this.heightMap[k << 4 | j] = l;

                            if (l < this.heightMapMinimum) {
                                this.heightMapMinimum = l;
                            }
                        }

                        ++k;
                        break;
                    }
                }
            }

            this.isModified.set(true);
        } finally {
            writeUnlock();
        }
    }

    /**
     * Generates the initial skylight map for the chunk upon generation or load.
     */
    public void generateSkylightMap() {
        writeLock();
        try {
            int i = this.getTopFilledSegment();
            this.heightMapMinimum = Integer.MAX_VALUE;

            for (int j = 0; j < 16; ++j) {
                int k = 0;

                while (k < 16) {
                    this.precipitationHeightMap[j + (k << 4)] = -999;
                    int l = i + 16 - 1;

                    while (true) {
                        if (l > 0) {
                            if (this.func_150808_b(j, l - 1, k) == 0) {
                                --l;
                                continue;
                            }

                            this.heightMap[k << 4 | j] = l;

                            if (l < this.heightMapMinimum) {
                                this.heightMapMinimum = l;
                            }
                        }

                        if (!this.worldObj.provider.hasNoSky) {
                            l = 15;
                            int i1 = i + 16 - 1;

                            do {
                                int j1 = this.func_150808_b(j, i1, k);

                                if (j1 == 0 && l != 15) {
                                    j1 = 1;
                                }

                                l -= j1;

                                if (l > 0) {
                                    ExtendedBlockStorage extendedblockstorage = this.storageArrays[i1 >> 4];

                                    if (extendedblockstorage != null) {
                                        extendedblockstorage.setExtSkylightValue(j, i1 & 15, k, l);
                                        this.worldObj
                                            .func_147479_m((this.xPosition << 4) + j, i1, (this.zPosition << 4) + k);
                                    }
                                }

                                --i1;
                            } while (i1 > 0 && l > 0);
                        }

                        ++k;
                        break;
                    }
                }
            }

            this.isModified.set(true);
        } finally {
            writeUnlock();
        }
    }

    /**
     * Propagates a given sky-visible block's light value downward and upward to neighboring blocks as necessary.
     */
    private void propagateSkylightOcclusion(int p_76595_1_, int p_76595_2_) {
        writeLock();
        try {
            this.updateSkylightColumns[p_76595_1_ + p_76595_2_ * 16] = true;
            this.isGapLightingUpdated = true;
        } finally {
            writeUnlock();
        }
    }

    private void recheckGaps(boolean p_150803_1_) {
        this.worldObj.theProfiler.startSection("recheckGaps");
        writeLock();
        try {
            if (this.worldObj.doChunksNearChunkExist(this.xPosition * 16 + 8, 0, this.zPosition * 16 + 8, 16)) {
                for (int i = 0; i < 16; ++i) {
                    for (int j = 0; j < 16; ++j) {
                        if (this.updateSkylightColumns[i + j * 16]) {
                            this.updateSkylightColumns[i + j * 16] = false;
                            int k = this.getHeightValue(i, j);
                            int l = this.xPosition * 16 + i;
                            int i1 = this.zPosition * 16 + j;
                            int j1 = this.worldObj.getChunkHeightMapMinimum(l - 1, i1);
                            int k1 = this.worldObj.getChunkHeightMapMinimum(l + 1, i1);
                            int l1 = this.worldObj.getChunkHeightMapMinimum(l, i1 - 1);
                            int i2 = this.worldObj.getChunkHeightMapMinimum(l, i1 + 1);

                            if (k1 < j1) {
                                j1 = k1;
                            }

                            if (l1 < j1) {
                                j1 = l1;
                            }

                            if (i2 < j1) {
                                j1 = i2;
                            }

                            this.checkSkylightNeighborHeight(l, i1, j1);
                            this.checkSkylightNeighborHeight(l - 1, i1, k);
                            this.checkSkylightNeighborHeight(l + 1, i1, k);
                            this.checkSkylightNeighborHeight(l, i1 - 1, k);
                            this.checkSkylightNeighborHeight(l, i1 + 1, k);

                            if (p_150803_1_) {
                                this.worldObj.theProfiler.endSection();
                                return;
                            }
                        }
                    }
                }
                this.isGapLightingUpdated = false;
            }

            this.worldObj.theProfiler.endSection();
        } finally {
            writeUnlock();
        }
    }

    /**
     * Checks the height of a block next to a sky-visible block and schedules a lighting update as necessary.
     */
    private void checkSkylightNeighborHeight(int p_76599_1_, int p_76599_2_, int p_76599_3_) {
        writeLock();
        try {
            int l = this.worldObj.getHeightValue(p_76599_1_, p_76599_2_);

            if (l > p_76599_3_) {
                this.updateSkylightNeighborHeight(p_76599_1_, p_76599_2_, p_76599_3_, l + 1);
            } else if (l < p_76599_3_) {
                this.updateSkylightNeighborHeight(p_76599_1_, p_76599_2_, l, p_76599_3_ + 1);
            }
        } finally {
            writeUnlock();
        }
    }

    private void updateSkylightNeighborHeight(int p_76609_1_, int p_76609_2_, int p_76609_3_, int p_76609_4_) {
        writeLock();
        try {
            if (p_76609_4_ > p_76609_3_ && this.worldObj.doChunksNearChunkExist(p_76609_1_, 0, p_76609_2_, 16)) {
                for (int i1 = p_76609_3_; i1 < p_76609_4_; ++i1) {
                    this.worldObj.updateLightByType(EnumSkyBlock.Sky, p_76609_1_, i1, p_76609_2_);
                }

                this.isModified.set(true);
            }
        } finally {
            writeUnlock();
        }
    }

    /**
     * Initiates the recalculation of both the block-light and sky-light for a given block inside a chunk.
     */
    private void relightBlock(int p_76615_1_, int p_76615_2_, int p_76615_3_) {
        writeLock();
        try {
            int l = this.heightMap[p_76615_3_ << 4 | p_76615_1_] & 255;

            int i1 = Math.max(p_76615_2_, l);

            while (i1 > 0 && this.func_150808_b(p_76615_1_, i1 - 1, p_76615_3_) == 0) {
                --i1;
            }

            if (i1 != l) {
                this.worldObj
                    .markBlocksDirtyVertical(p_76615_1_ + this.xPosition * 16, p_76615_3_ + this.zPosition * 16, i1, l);
                this.heightMap[p_76615_3_ << 4 | p_76615_1_] = i1;
                int j1 = this.xPosition * 16 + p_76615_1_;
                int k1 = this.zPosition * 16 + p_76615_3_;
                int l1;
                int i2;

                if (!this.worldObj.provider.hasNoSky) {
                    ExtendedBlockStorage extendedblockstorage;
                    if (i1 < l) {
                        for (l1 = i1; l1 < l; ++l1) {
                            extendedblockstorage = this.storageArrays[l1 >> 4];

                            if (extendedblockstorage != null) {
                                extendedblockstorage.setExtSkylightValue(p_76615_1_, l1 & 15, p_76615_3_, 15);
                                this.worldObj.func_147479_m(
                                    (this.xPosition << 4) + p_76615_1_,
                                    l1,
                                    (this.zPosition << 4) + p_76615_3_);
                            }
                        }
                    } else {
                        for (l1 = l; l1 < i1; ++l1) {
                            extendedblockstorage = this.storageArrays[l1 >> 4];

                            if (extendedblockstorage != null) {
                                extendedblockstorage.setExtSkylightValue(p_76615_1_, l1 & 15, p_76615_3_, 0);
                                this.worldObj.func_147479_m(
                                    (this.xPosition << 4) + p_76615_1_,
                                    l1,
                                    (this.zPosition << 4) + p_76615_3_);
                            }
                        }

                    }

                    l1 = 15;

                    while (i1 > 0 && l1 > 0) {
                        --i1;
                        i2 = this.func_150808_b(p_76615_1_, i1, p_76615_3_);

                        if (i2 == 0) {
                            i2 = 1;
                        }

                        l1 -= i2;

                        if (l1 < 0) {
                            l1 = 0;
                        }

                        ExtendedBlockStorage extendedblockstorage1 = this.storageArrays[i1 >> 4];

                        if (extendedblockstorage1 != null) {
                            extendedblockstorage1.setExtSkylightValue(p_76615_1_, i1 & 15, p_76615_3_, l1);
                        }
                    }
                }

                l1 = this.heightMap[p_76615_3_ << 4 | p_76615_1_];
                i2 = l;
                int j2 = l1;

                if (l1 < l) {
                    i2 = l1;
                    j2 = l;
                }

                if (l1 < this.heightMapMinimum) {
                    this.heightMapMinimum = l1;
                }

                if (!this.worldObj.provider.hasNoSky) {
                    this.updateSkylightNeighborHeight(j1 - 1, k1, i2, j2);
                    this.updateSkylightNeighborHeight(j1 + 1, k1, i2, j2);
                    this.updateSkylightNeighborHeight(j1, k1 - 1, i2, j2);
                    this.updateSkylightNeighborHeight(j1, k1 + 1, i2, j2);
                    this.updateSkylightNeighborHeight(j1, k1, i2, j2);
                }

                this.isModified.set(true);
            }
        } finally {
            writeUnlock();
        }
    }

    public int func_150808_b(int p_150808_1_, int p_150808_2_, int p_150808_3_) {
        readLock();
        try {
            int x = (xPosition << 4) + p_150808_1_;
            int z = (zPosition << 4) + p_150808_3_;
            return this.getBlock(p_150808_1_, p_150808_2_, p_150808_3_)
                .getLightOpacity(worldObj, x, p_150808_2_, z);
        } finally {
            readUnlock();
        }
    }

    /**
     * Returns the block corresponding to the given coordinates inside a chunk.
     */
    public Block getBlock(final int p_150810_1_, final int p_150810_2_, final int p_150810_3_) {
        readLock();
        try {
            Block block = Blocks.air;

            if (p_150810_2_ >> 4 < this.storageArrays.length) {
                ExtendedBlockStorage extendedblockstorage = this.storageArrays[p_150810_2_ >> 4];

                if (extendedblockstorage != null) {
                    try {
                        block = extendedblockstorage.getBlockByExtId(p_150810_1_, p_150810_2_ & 15, p_150810_3_);
                    } catch (Throwable throwable) {
                        CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Getting block");
                        CrashReportCategory crashreportcategory = crashreport.makeCategory("Block being got");
                        crashreportcategory.addCrashSectionCallable(
                            "Location",
                            () -> CrashReportCategory.getLocationInfo(p_150810_1_, p_150810_2_, p_150810_3_));
                        throw new ReportedException(crashreport);
                    }
                }
            }
            return block;
        } finally {
            readUnlock();
        }
    }

    /**
     * Return the metadata corresponding to the given coordinates inside a chunk.
     */
    public int getBlockMetadata(int p_76628_1_, int p_76628_2_, int p_76628_3_) {
        readLock();
        try {
            if (p_76628_2_ >> 4 >= this.storageArrays.length) {
                return 0;
            } else {
                ExtendedBlockStorage extendedblockstorage = this.storageArrays[p_76628_2_ >> 4];
                return extendedblockstorage != null
                    ? extendedblockstorage.getExtBlockMetadata(p_76628_1_, p_76628_2_ & 15, p_76628_3_)
                    : 0;
            }
        } finally {
            readUnlock();
        }
    }

    public boolean func_150807_a(int p_150807_1_, int p_150807_2_, int p_150807_3_, Block p_150807_4_,
        int p_150807_5_) {
        writeLock();
        try {
            int i1 = p_150807_3_ << 4 | p_150807_1_;

            if (p_150807_2_ >= this.precipitationHeightMap[i1] - 1) {
                this.precipitationHeightMap[i1] = -999;
            }

            int j1 = this.heightMap[i1];
            Block block1 = this.getBlock(p_150807_1_, p_150807_2_, p_150807_3_);
            int k1 = this.getBlockMetadata(p_150807_1_, p_150807_2_, p_150807_3_);

            if (block1 == p_150807_4_ && k1 == p_150807_5_) {
                return false;
            } else {
                ExtendedBlockStorage extendedblockstorage = this.storageArrays[p_150807_2_ >> 4];
                boolean flag = false;

                if (extendedblockstorage == null) {
                    if (p_150807_4_ == Blocks.air) {
                        return false;
                    }

                    extendedblockstorage = this.storageArrays[p_150807_2_ >> 4] = new ConcurrentExtendedBlockStorage(
                        p_150807_2_ >> 4 << 4,
                        !this.worldObj.provider.hasNoSky);
                    flag = p_150807_2_ >= j1;
                }

                int l1 = this.xPosition * 16 + p_150807_1_;
                int i2 = this.zPosition * 16 + p_150807_3_;

                int k2 = block1.getLightOpacity(this.worldObj, l1, p_150807_2_, i2);

                if (!this.worldObj.isRemote) {
                    block1.onBlockPreDestroy(this.worldObj, l1, p_150807_2_, i2, k1);
                }

                extendedblockstorage.func_150818_a(p_150807_1_, p_150807_2_ & 15, p_150807_3_, p_150807_4_);
                extendedblockstorage.setExtBlockMetadata(p_150807_1_, p_150807_2_ & 15, p_150807_3_, p_150807_5_); // This
                                                                                                                   // line
                if (!this.worldObj.isRemote) {
                    block1.breakBlock(this.worldObj, l1, p_150807_2_, i2, block1, k1);
                    // After breakBlock a phantom TE might have been created with incorrect meta. This attempts to kill
                    // that phantom TE so the normal one can be create properly later
                    TileEntity te = this.getTileEntityUnsafe(p_150807_1_ & 0x0F, p_150807_2_, p_150807_3_ & 0x0F);
                    if (te != null && te.shouldRefresh(
                        block1,
                        getBlock(p_150807_1_ & 0x0F, p_150807_2_, p_150807_3_ & 0x0F),
                        k1,
                        getBlockMetadata(p_150807_1_ & 0x0F, p_150807_2_, p_150807_3_ & 0x0F),
                        worldObj,
                        l1,
                        p_150807_2_,
                        i2)) {
                        this.removeTileEntity(p_150807_1_ & 0x0F, p_150807_2_, p_150807_3_ & 0x0F);
                    }
                } else if (block1.hasTileEntity(k1)) {
                    TileEntity te = this.getTileEntityUnsafe(p_150807_1_ & 0x0F, p_150807_2_, p_150807_3_ & 0x0F);
                    if (te != null
                        && te.shouldRefresh(block1, p_150807_4_, k1, p_150807_5_, worldObj, l1, p_150807_2_, i2)) {
                        this.worldObj.removeTileEntity(l1, p_150807_2_, i2);
                    }
                }

                if (extendedblockstorage.getBlockByExtId(p_150807_1_, p_150807_2_ & 15, p_150807_3_) != p_150807_4_) {
                    return false;
                } else {
                    extendedblockstorage.setExtBlockMetadata(p_150807_1_, p_150807_2_ & 15, p_150807_3_, p_150807_5_);

                    if (flag) {
                        this.generateSkylightMap();
                    } else {
                        int j2 = p_150807_4_.getLightOpacity(this.worldObj, l1, p_150807_2_, i2);

                        if (j2 > 0) {
                            if (p_150807_2_ >= j1) {
                                this.relightBlock(p_150807_1_, p_150807_2_ + 1, p_150807_3_);
                            }
                        } else if (p_150807_2_ == j1 - 1) {
                            this.relightBlock(p_150807_1_, p_150807_2_, p_150807_3_);
                        }

                        if (j2 != k2 && (j2 < k2
                            || this.getSavedLightValue(EnumSkyBlock.Sky, p_150807_1_, p_150807_2_, p_150807_3_) > 0
                            || this.getSavedLightValue(EnumSkyBlock.Block, p_150807_1_, p_150807_2_, p_150807_3_)
                                > 0)) {
                            this.propagateSkylightOcclusion(p_150807_1_, p_150807_3_);
                        }
                    }

                    TileEntity tileentity;

                    if (!this.worldObj.isRemote) {
                        p_150807_4_.onBlockAdded(this.worldObj, l1, p_150807_2_, i2);
                    }

                    if (p_150807_4_.hasTileEntity(p_150807_5_)) {
                        tileentity = this.func_150806_e(p_150807_1_, p_150807_2_, p_150807_3_);

                        if (tileentity != null) {
                            tileentity.updateContainingBlockInfo();
                            tileentity.blockMetadata = p_150807_5_;
                        }
                    }

                    this.isModified.set(true);
                    return true;
                }
            }
        } finally {
            writeUnlock();
        }
    }

    /**
     * Set the metadata of a block in the chunk
     */
    public boolean setBlockMetadata(int p_76589_1_, int p_76589_2_, int p_76589_3_, int p_76589_4_) {
        writeLock();
        try {
            ExtendedBlockStorage extendedblockstorage = this.storageArrays[p_76589_2_ >> 4];

            if (extendedblockstorage == null) {
                return false;
            } else {
                int i1 = extendedblockstorage.getExtBlockMetadata(p_76589_1_, p_76589_2_ & 15, p_76589_3_);

                if (i1 == p_76589_4_) {
                    return false;
                } else {
                    this.isModified.set(true);
                    extendedblockstorage.setExtBlockMetadata(p_76589_1_, p_76589_2_ & 15, p_76589_3_, p_76589_4_);

                    if (extendedblockstorage.getBlockByExtId(p_76589_1_, p_76589_2_ & 15, p_76589_3_)
                        .hasTileEntity(p_76589_4_)) {
                        TileEntity tileentity = this.func_150806_e(p_76589_1_, p_76589_2_, p_76589_3_);

                        if (tileentity != null) {
                            tileentity.updateContainingBlockInfo();
                            tileentity.blockMetadata = p_76589_4_;
                        }
                    }
                    return true;
                }
            }
        } finally {
            writeUnlock();
        }
    }

    /**
     * Gets the amount of light saved in this block (doesn't adjust for daylight)
     */
    public int getSavedLightValue(EnumSkyBlock p_76614_1_, int p_76614_2_, int p_76614_3_, int p_76614_4_) {
        readLock();
        try {
            ExtendedBlockStorage extendedblockstorage = this.storageArrays[p_76614_3_ >> 4];
            return extendedblockstorage == null
                ? (this.canBlockSeeTheSky(p_76614_2_, p_76614_3_, p_76614_4_) ? p_76614_1_.defaultLightValue : 0)
                : (p_76614_1_ == EnumSkyBlock.Sky
                    ? (this.worldObj.provider.hasNoSky ? 0
                        : extendedblockstorage.getExtSkylightValue(p_76614_2_, p_76614_3_ & 15, p_76614_4_))
                    : (p_76614_1_ == EnumSkyBlock.Block
                        ? extendedblockstorage.getExtBlocklightValue(p_76614_2_, p_76614_3_ & 15, p_76614_4_)
                        : p_76614_1_.defaultLightValue));
        } finally {
            readUnlock();
        }
    }

    /**
     * Sets the light value at the coordinate. If enumskyblock is set to sky it sets it in the skylightmap and if its a
     * block then into the blocklightmap. Args enumSkyBlock, x, y, z, lightValue
     */
    public void setLightValue(EnumSkyBlock p_76633_1_, int p_76633_2_, int p_76633_3_, int p_76633_4_, int p_76633_5_) {
        writeLock();
        try {
            ExtendedBlockStorage extendedblockstorage = this.storageArrays[p_76633_3_ >> 4];

            if (extendedblockstorage == null) {
                extendedblockstorage = this.storageArrays[p_76633_3_ >> 4] = new ConcurrentExtendedBlockStorage(
                    p_76633_3_ >> 4 << 4,
                    !this.worldObj.provider.hasNoSky);
                this.generateSkylightMap();
            }

            this.isModified.set(true);

            if (p_76633_1_ == EnumSkyBlock.Sky) {
                if (!this.worldObj.provider.hasNoSky) {
                    extendedblockstorage.setExtSkylightValue(p_76633_2_, p_76633_3_ & 15, p_76633_4_, p_76633_5_);
                }
            } else if (p_76633_1_ == EnumSkyBlock.Block) {
                extendedblockstorage.setExtBlocklightValue(p_76633_2_, p_76633_3_ & 15, p_76633_4_, p_76633_5_);
            }
        } finally {
            writeUnlock();
        }
    }

    /**
     * Gets the amount of light on a block taking into account sunlight
     */
    public int getBlockLightValue(int p_76629_1_, int p_76629_2_, int p_76629_3_, int p_76629_4_) {
        readLock();
        try {
            ExtendedBlockStorage extendedblockstorage = this.storageArrays[p_76629_2_ >> 4];

            if (extendedblockstorage == null) {
                return !this.worldObj.provider.hasNoSky && p_76629_4_ < EnumSkyBlock.Sky.defaultLightValue
                    ? EnumSkyBlock.Sky.defaultLightValue - p_76629_4_
                    : 0;
            } else {
                int i1 = this.worldObj.provider.hasNoSky ? 0
                    : extendedblockstorage.getExtSkylightValue(p_76629_1_, p_76629_2_ & 15, p_76629_3_);

                if (i1 > 0) {
                    isLit = true;
                }

                i1 -= p_76629_4_;
                int j1 = extendedblockstorage.getExtBlocklightValue(p_76629_1_, p_76629_2_ & 15, p_76629_3_);

                if (j1 > i1) {
                    i1 = j1;
                }

                return i1;
            }
        } finally {
            readUnlock();
        }
    }

    /**
     * Adds an entity to the chunk. Args: entity
     */
    public void addEntity(Entity p_76612_1_) {
        writeLock();
        try {
            this.hasEntities.set(true);
            int i = MathHelper.floor_double(p_76612_1_.posX / 16.0D);
            int j = MathHelper.floor_double(p_76612_1_.posZ / 16.0D);

            if (i != this.xPosition || j != this.zPosition) {
                logger.warn(
                    "Wrong location! " + p_76612_1_
                        + " (at "
                        + i
                        + ", "
                        + j
                        + " instead of "
                        + this.xPosition
                        + ", "
                        + this.zPosition
                        + ")");
                Thread.dumpStack();
            }

            int k = MathHelper.floor_double(p_76612_1_.posY / 16.0D);

            if (k < 0) {
                k = 0;
            }

            if (k >= this.entityLists.length) {
                k = this.entityLists.length - 1;
            }

            MinecraftForge.EVENT_BUS.post(
                new EntityEvent.EnteringChunk(
                    p_76612_1_,
                    this.xPosition,
                    this.zPosition,
                    p_76612_1_.chunkCoordX,
                    p_76612_1_.chunkCoordZ));
            p_76612_1_.addedToChunk = true;
            p_76612_1_.chunkCoordX = this.xPosition;
            p_76612_1_.chunkCoordY = k;
            p_76612_1_.chunkCoordZ = this.zPosition;
            this.entityLists[k].add(p_76612_1_);
        } finally {
            writeUnlock();
        }
    }

    /**
     * removes entity using its y chunk coordinate as its index
     */
    public void removeEntity(Entity p_76622_1_) {
        this.removeEntityAtIndex(p_76622_1_, p_76622_1_.chunkCoordY);
    }

    /**
     * Removes entity at the specified index from the entity array.
     */
    public void removeEntityAtIndex(Entity p_76608_1_, int p_76608_2_) {
        if (p_76608_2_ < 0) {
            p_76608_2_ = 0;
        }

        writeLock();
        try {
            if (p_76608_2_ >= this.entityLists.length) {
                p_76608_2_ = this.entityLists.length - 1;
            }

            this.entityLists[p_76608_2_].remove(p_76608_1_);
        } finally {
            writeUnlock();
        }
    }

    /**
     * Returns whether is not a block above this one blocking sight to the sky (done via checking against the heightmap)
     */
    public boolean canBlockSeeTheSky(int p_76619_1_, int p_76619_2_, int p_76619_3_) {
        readLock();
        try {
            return p_76619_2_ >= this.heightMap[p_76619_3_ << 4 | p_76619_1_];
        } finally {
            readUnlock();
        }
    }

    public TileEntity func_150806_e(int p_150806_1_, int p_150806_2_, int p_150806_3_) {
        writeLock();
        try {
            ChunkPosition chunkposition = new ChunkPosition(p_150806_1_, p_150806_2_, p_150806_3_);
            TileEntity tileentity = this.chunkTileEntityMap.get(chunkposition);

            if (tileentity != null && tileentity.isInvalid()) {
                chunkTileEntityMap.remove(chunkposition);
                tileentity = null;
            }

            if (tileentity == null) {
                Block block = this.getBlock(p_150806_1_, p_150806_2_, p_150806_3_);
                int meta = this.getBlockMetadata(p_150806_1_, p_150806_2_, p_150806_3_);

                if (!block.hasTileEntity(meta)) {
                    return null;
                }

                tileentity = block.createTileEntity(worldObj, meta);
                this.worldObj.setTileEntity(
                    this.xPosition * 16 + p_150806_1_,
                    p_150806_2_,
                    this.zPosition * 16 + p_150806_3_,
                    tileentity);
            }

            return tileentity;
        } finally {
            writeUnlock();
        }
    }

    public void addTileEntity(TileEntity p_150813_1_) {
        writeLock();
        try {
            int i = p_150813_1_.xCoord - this.xPosition * 16;
            int j = p_150813_1_.yCoord;
            int k = p_150813_1_.zCoord - this.zPosition * 16;
            this.func_150812_a(i, j, k, p_150813_1_);

            if (this.isChunkLoaded.get()) {
                this.worldObj.addTileEntity(p_150813_1_);
            }

        } finally {
            writeUnlock();
        }
    }

    public void func_150812_a(int p_150812_1_, int p_150812_2_, int p_150812_3_, TileEntity p_150812_4_) {
        writeLock();
        try {
            ChunkPosition chunkposition = new ChunkPosition(p_150812_1_, p_150812_2_, p_150812_3_);
            p_150812_4_.setWorldObj(this.worldObj);
            p_150812_4_.xCoord = this.xPosition * 16 + p_150812_1_;
            p_150812_4_.yCoord = p_150812_2_;
            p_150812_4_.zCoord = this.zPosition * 16 + p_150812_3_;

            int metadata = getBlockMetadata(p_150812_1_, p_150812_2_, p_150812_3_);
            if (this.getBlock(p_150812_1_, p_150812_2_, p_150812_3_)
                .hasTileEntity(metadata)) {
                if (this.chunkTileEntityMap.containsKey(chunkposition)) {
                    this.chunkTileEntityMap.get(chunkposition)
                        .invalidate();
                }

                p_150812_4_.validate();
                this.chunkTileEntityMap.put(chunkposition, p_150812_4_);
            }
        } finally {
            writeUnlock();
        }
    }

    public void removeTileEntity(int p_150805_1_, int p_150805_2_, int p_150805_3_) {
        writeLock();
        try {
            ChunkPosition chunkposition = new ChunkPosition(p_150805_1_, p_150805_2_, p_150805_3_);

            if (this.isChunkLoaded.get()) {
                TileEntity tileentity;
                tileentity = this.chunkTileEntityMap.remove(chunkposition);

                if (tileentity != null) {
                    tileentity.invalidate();
                }
            }
        } finally {
            writeUnlock();
        }
    }

    /**
     * Called when this Chunk is loaded by the ChunkProvider
     */
    public void onChunkLoad() {
        this.isChunkLoaded.set(true);
        readLock();
        try {
            this.worldObj.func_147448_a(this.chunkTileEntityMap.values());

            for (List<Entity> entityList : this.entityLists) {

                for (Entity entity : entityList) {
                    entity.onChunkLoad();
                }

                this.worldObj.addLoadedEntities(entityList);
            }
            MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load(this));
        } finally {
            readUnlock();
        }
    }

    /**
     * Called when this Chunk is unloaded by the ChunkProvider
     */
    public void onChunkUnload() {
        this.isChunkLoaded.set(false);
        readLock();
        try {

            for (TileEntity tileentity : this.chunkTileEntityMap.values()) {
                this.worldObj.func_147457_a(tileentity);
            }

            for (List<Entity> entityList : this.entityLists) {
                this.worldObj.unloadEntities(entityList);
            }
            MinecraftForge.EVENT_BUS.post(new ChunkEvent.Unload(this));
        } finally {
            readUnlock();
        }
    }

    /**
     * Sets the isModified flag for this Chunk
     */
    public void setChunkModified() {
        this.isModified.set(true);
    }

    /**
     * Fills the given list of all entities that intersect within the given bounding box that aren't the passed entity
     * Args: entity, aabb, listToFill
     */
    public void getEntitiesWithinAABBForEntity(Entity p_76588_1_, AxisAlignedBB p_76588_2_,
        List<net.minecraft.entity.Entity> p_76588_3_, IEntitySelector p_76588_4_) {
        readLock();
        try {
            int i = MathHelper.floor_double((p_76588_2_.minY - World.MAX_ENTITY_RADIUS) / 16.0D);
            int j = MathHelper.floor_double((p_76588_2_.maxY + World.MAX_ENTITY_RADIUS) / 16.0D);
            i = MathHelper.clamp_int(i, 0, this.entityLists.length - 1);
            j = MathHelper.clamp_int(j, 0, this.entityLists.length - 1);

            for (int k = i; k <= j; ++k) {
                List<Entity> list1 = this.entityLists[k];

                for (Entity entity1 : list1) {
                    if (entity1 != p_76588_1_ && entity1.boundingBox.intersectsWith(p_76588_2_)
                        && (p_76588_4_ == null || p_76588_4_.isEntityApplicable(entity1))) {
                        p_76588_3_.add(entity1);
                        Entity[] aentity = entity1.getParts();

                        if (aentity != null) {
                            for (Entity entity : aentity) {
                                entity1 = entity;

                                if (entity1 != p_76588_1_ && entity1.boundingBox.intersectsWith(p_76588_2_)
                                    && (p_76588_4_ == null || p_76588_4_.isEntityApplicable(entity1))) {
                                    p_76588_3_.add(entity1);
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            readUnlock();
        }
    }

    /**
     * Gets all entities that can be assigned to the specified class. Args: entityClass, aabb, listToFill
     */
    public <T> void getEntitiesOfTypeWithinAAAB(Class<T> p_76618_1_, AxisAlignedBB p_76618_2_, List<T> p_76618_3_,
        IEntitySelector p_76618_4_) {
        readLock();
        try {
            int i = MathHelper.floor_double((p_76618_2_.minY - World.MAX_ENTITY_RADIUS) / 16.0D);
            int j = MathHelper.floor_double((p_76618_2_.maxY + World.MAX_ENTITY_RADIUS) / 16.0D);
            i = MathHelper.clamp_int(i, 0, this.entityLists.length - 1);
            j = MathHelper.clamp_int(j, 0, this.entityLists.length - 1);

            for (int k = i; k <= j; ++k) {
                List<Entity> list1 = this.entityLists[k];

                for (Entity entity : list1) {
                    if (p_76618_1_.isAssignableFrom(entity.getClass()) && entity.boundingBox.intersectsWith(p_76618_2_)
                        && (p_76618_4_ == null || p_76618_4_.isEntityApplicable(entity))) {
                        p_76618_3_.add((T) entity);
                    }
                }
            }
        } finally {
            readUnlock();
        }
    }

    /**
     * Returns true if this Chunk needs to be saved
     */
    public boolean needsSaving(boolean p_76601_1_) {
        readLock();
        try {
            if (p_76601_1_) {
                if (this.hasEntities.get() && this.worldObj.getTotalWorldTime() != this.lastSaveTime
                    || this.isModified.get()) {
                    return true;
                }
            } else if (this.hasEntities.get() && this.worldObj.getTotalWorldTime() >= this.lastSaveTime + 600L) {
                return true;
            }
            return this.isModified.get();
        } finally {
            readUnlock();
        }
    }

    public Random getRandomWithSeed(long p_76617_1_) {
        readLock();
        try {
            return new Random(
                this.worldObj.getSeed() + ((long) this.xPosition * this.xPosition * 4987142)
                    + (this.xPosition * 5947611L)
                    + ((long) this.zPosition * this.zPosition) * 4392871L
                    + (this.zPosition * 389711L) ^ p_76617_1_);
        } finally {
            readUnlock();
        }
    }

    public void populateChunk(IChunkProvider p_76624_1_, IChunkProvider p_76624_2_, int p_76624_3_, int p_76624_4_) {
        writeLock();
        try {
            if (!this.isTerrainPopulated && p_76624_1_.chunkExists(p_76624_3_ + 1, p_76624_4_ + 1)
                && p_76624_1_.chunkExists(p_76624_3_, p_76624_4_ + 1)
                && p_76624_1_.chunkExists(p_76624_3_ + 1, p_76624_4_)) {
                p_76624_1_.populate(p_76624_2_, p_76624_3_, p_76624_4_);
            }

            if (p_76624_1_.chunkExists(p_76624_3_ - 1, p_76624_4_)
                && !p_76624_1_.provideChunk(p_76624_3_ - 1, p_76624_4_).isTerrainPopulated
                && p_76624_1_.chunkExists(p_76624_3_ - 1, p_76624_4_ + 1)
                && p_76624_1_.chunkExists(p_76624_3_, p_76624_4_ + 1)
                && p_76624_1_.chunkExists(p_76624_3_ - 1, p_76624_4_ + 1)) {
                p_76624_1_.populate(p_76624_2_, p_76624_3_ - 1, p_76624_4_);
            }

            if (p_76624_1_.chunkExists(p_76624_3_, p_76624_4_ - 1)
                && !p_76624_1_.provideChunk(p_76624_3_, p_76624_4_ - 1).isTerrainPopulated
                && p_76624_1_.chunkExists(p_76624_3_ + 1, p_76624_4_ - 1)
                && p_76624_1_.chunkExists(p_76624_3_ + 1, p_76624_4_ - 1)
                && p_76624_1_.chunkExists(p_76624_3_ + 1, p_76624_4_)) {
                p_76624_1_.populate(p_76624_2_, p_76624_3_, p_76624_4_ - 1);
            }

            if (p_76624_1_.chunkExists(p_76624_3_ - 1, p_76624_4_ - 1)
                && !p_76624_1_.provideChunk(p_76624_3_ - 1, p_76624_4_ - 1).isTerrainPopulated
                && p_76624_1_.chunkExists(p_76624_3_, p_76624_4_ - 1)
                && p_76624_1_.chunkExists(p_76624_3_ - 1, p_76624_4_)) {
                p_76624_1_.populate(p_76624_2_, p_76624_3_ - 1, p_76624_4_ - 1);
            }
        } finally {
            writeUnlock();
        }
    }

    /**
     * Gets the height to which rain/snow will fall. Calculates it if not already stored.
     */
    public int getPrecipitationHeight(int p_76626_1_, int p_76626_2_) {
        writeLock();
        try {
            int k = p_76626_1_ | p_76626_2_ << 4;
            int l = this.precipitationHeightMap[k];

            if (l == -999) {
                int i1 = this.getTopFilledSegment() + 15;
                l = -1;

                while (i1 > 0 && l == -1) {
                    Block block = this.getBlock(p_76626_1_, i1, p_76626_2_);
                    Material material = block.getMaterial();

                    if (!material.blocksMovement() && !material.isLiquid()) {
                        --i1;
                    } else {
                        l = i1 + 1;
                    }
                }

                this.precipitationHeightMap[k] = l;
            }
            return l;
        } finally {
            writeUnlock();
        }
    }

    public void func_150804_b(boolean p_150804_1_) {
        writeLock();
        try {
            if (this.isGapLightingUpdated && !this.worldObj.provider.hasNoSky && !p_150804_1_) {
                this.recheckGaps(this.worldObj.isRemote);
            }

            this.field_150815_m = true;

            if (!this.isLightPopulated && this.isTerrainPopulated) {
                this.func_150809_p();
            }
        } finally {
            writeUnlock();
        }
    }

    public boolean func_150802_k() {
        readLock();
        try {
            return this.field_150815_m && this.isTerrainPopulated && this.isLightPopulated;
        } finally {
            readUnlock();
        }
    }

    /**
     * Gets a ChunkCoordIntPair representing the Chunk's position.
     */
    public ChunkCoordIntPair getChunkCoordIntPair() {
        readLock();
        try {
            return new ChunkCoordIntPair(this.xPosition, this.zPosition);
        } finally {
            readUnlock();
        }
    }

    /**
     * Returns whether the ExtendedBlockStorages containing levels (in blocks) from arg 1 to arg 2 are fully empty
     * (true) or not (false).
     */
    public boolean getAreLevelsEmpty(int p_76606_1_, int p_76606_2_) {
        readLock();
        try {
            if (p_76606_1_ < 0) {
                p_76606_1_ = 0;
            }

            if (p_76606_2_ >= 256) {
                p_76606_2_ = 255;
            }

            for (int k = p_76606_1_; k <= p_76606_2_; k += 16) {
                ExtendedBlockStorage extendedblockstorage = this.storageArrays[k >> 4];

                if (extendedblockstorage != null && !extendedblockstorage.isEmpty()) {
                    return false;
                }
            }
            return true;
        } finally {
            readUnlock();
        }
    }

    public void setStorageArrays(ExtendedBlockStorage[] p_76602_1_) {
        writeLock();
        try {
            this.storageArrays = p_76602_1_;
        } finally {
            writeUnlock();
        }
    }

    /**
     * Initialize this chunk with new binary data
     */
    @SideOnly(Side.CLIENT)
    public void fillChunk(byte[] p_76607_1_, int p_76607_2_, int p_76607_3_, boolean p_76607_4_) {
        writeLock();
        try {
            Iterator<TileEntity> iterator = chunkTileEntityMap.values()
                .iterator();
            while (iterator.hasNext()) {
                TileEntity tileEntity = iterator.next();
                tileEntity.updateContainingBlockInfo();
                tileEntity.getBlockMetadata();
                tileEntity.getBlockType();
            }

            int k = 0;
            boolean flag1 = !this.worldObj.provider.hasNoSky;
            int l;

            for (l = 0; l < this.storageArrays.length; ++l) {
                if ((p_76607_2_ & 1 << l) != 0) {
                    if (this.storageArrays[l] == null) {
                        this.storageArrays[l] = new ConcurrentExtendedBlockStorage(l << 4, flag1);
                    }

                    byte[] abyte1 = this.storageArrays[l].getBlockLSBArray();
                    System.arraycopy(p_76607_1_, k, abyte1, 0, abyte1.length);
                    k += abyte1.length;
                } else if (p_76607_4_ && this.storageArrays[l] != null) {
                    this.storageArrays[l] = null;
                }
            }

            NibbleArray nibblearray;

            for (l = 0; l < this.storageArrays.length; ++l) {
                if ((p_76607_2_ & 1 << l) != 0 && this.storageArrays[l] != null) {
                    nibblearray = this.storageArrays[l].getMetadataArray();
                    System.arraycopy(p_76607_1_, k, nibblearray.data, 0, nibblearray.data.length);
                    k += nibblearray.data.length;
                }
            }

            for (l = 0; l < this.storageArrays.length; ++l) {
                if ((p_76607_2_ & 1 << l) != 0 && this.storageArrays[l] != null) {
                    nibblearray = this.storageArrays[l].getBlocklightArray();
                    System.arraycopy(p_76607_1_, k, nibblearray.data, 0, nibblearray.data.length);
                    k += nibblearray.data.length;
                }
            }

            if (flag1) {
                for (l = 0; l < this.storageArrays.length; ++l) {
                    if ((p_76607_2_ & 1 << l) != 0 && this.storageArrays[l] != null) {
                        nibblearray = this.storageArrays[l].getSkylightArray();
                        System.arraycopy(p_76607_1_, k, nibblearray.data, 0, nibblearray.data.length);
                        k += nibblearray.data.length;
                    }
                }
            }

            for (l = 0; l < this.storageArrays.length; ++l) {
                if ((p_76607_3_ & 1 << l) != 0) {
                    if (this.storageArrays[l] == null) {
                        k += 2048;
                    } else {
                        nibblearray = this.storageArrays[l].getBlockMSBArray();

                        if (nibblearray == null) {
                            nibblearray = this.storageArrays[l].createBlockMSBArray();
                        }

                        System.arraycopy(p_76607_1_, k, nibblearray.data, 0, nibblearray.data.length);
                        k += nibblearray.data.length;
                    }
                } else if (p_76607_4_ && this.storageArrays[l] != null
                    && this.storageArrays[l].getBlockMSBArray() != null) {
                        this.storageArrays[l].clearMSBArray();
                    }
            }

            if (p_76607_4_) {
                System.arraycopy(p_76607_1_, k, this.blockBiomeArray, 0, this.blockBiomeArray.length);
            }

            for (l = 0; l < this.storageArrays.length; ++l) {
                if (this.storageArrays[l] != null && (p_76607_2_ & 1 << l) != 0) {
                    this.storageArrays[l].removeInvalidBlocks();
                }
            }

            this.isLightPopulated = true;
            this.isTerrainPopulated = true;
            this.generateHeightMap();
            List<TileEntity> invalidList = new ArrayList<>();
            iterator = this.chunkTileEntityMap.values()
                .iterator();

            while (iterator.hasNext()) {
                TileEntity tileentity = iterator.next();
                int x = tileentity.xCoord & 15;
                int y = tileentity.yCoord;
                int z = tileentity.zCoord & 15;
                Block block = tileentity.getBlockType();
                if ((block != getBlock(x, y, z) || tileentity.blockMetadata != this.getBlockMetadata(x, y, z))
                    && tileentity.shouldRefresh(
                        block,
                        getBlock(x, y, z),
                        tileentity.blockMetadata,
                        this.getBlockMetadata(x, y, z),
                        worldObj,
                        x,
                        y,
                        z)) {
                    invalidList.add(tileentity);
                }
                tileentity.updateContainingBlockInfo();
            }

            for (TileEntity te : invalidList) {
                te.invalidate();
            }
        } finally {
            writeUnlock();
        }
    }

    /**
     * This method retrieves the biome at a set of coordinates
     */
    public BiomeGenBase getBiomeGenForWorldCoords(int p_76591_1_, int p_76591_2_, WorldChunkManager p_76591_3_) {
        writeLock();
        try {
            int k = this.blockBiomeArray[p_76591_2_ << 4 | p_76591_1_] & 255;

            if (k == 255) {
                BiomeGenBase biomegenbase = p_76591_3_
                    .getBiomeGenAt((this.xPosition << 4) + p_76591_1_, (this.zPosition << 4) + p_76591_2_);
                k = biomegenbase.biomeID;
                this.blockBiomeArray[p_76591_2_ << 4 | p_76591_1_] = (byte) (k & 255);
            }
            return BiomeGenBase.getBiome(k) == null ? BiomeGenBase.plains : BiomeGenBase.getBiome(k);
        } finally {
            writeUnlock();
        }
    }

    /**
     * Returns an array containing a 16x16 mapping on the X/Z of block positions in this Chunk to biome IDs.
     */
    public byte[] getBiomeArray() {
        readLock();
        try {
            return this.blockBiomeArray;
        } finally {
            readUnlock();
        }
    }

    /**
     * Accepts a 256-entry array that contains a 16x16 mapping on the X/Z plane of block positions in this Chunk to
     * biome IDs.
     */
    public void setBiomeArray(byte[] p_76616_1_) {
        writeLock();
        try {
            this.blockBiomeArray = p_76616_1_;
        } finally {
            writeUnlock();
        }
    }

    /**
     * Resets the relight check index to 0 for this Chunk.
     */
    public void resetRelightChecks() {
        this.queuedLightChecks.set(0);
    }

    /**
     * Called once-per-chunk-per-tick, and advances the round-robin relight check index per-storage-block by up to 8
     * blocks at a time. In a worst-case scenario, can potentially take up to 1.6 seconds, calculated via
     * (4096/(8*16))/20, to re-check all blocks in a chunk, which could explain both lagging light updates in certain
     * cases as well as Nether relight
     */
    public void enqueueRelightChecks() {
        writeLock();
        try {
            for (int i = 0; i < 8; ++i) {
                if (this.queuedLightChecks.get() >= 4096) {
                    return;
                }

                int j = this.queuedLightChecks.get() % 16;
                int k = this.queuedLightChecks.get() / 16 % 16;
                int l = this.queuedLightChecks.get() / 256;
                this.queuedLightChecks.incrementAndGet();
                int i1 = (this.xPosition << 4) + k;
                int j1 = (this.zPosition << 4) + l;

                for (int k1 = 0; k1 < 16; ++k1) {
                    int l1 = (j << 4) + k1;

                    if (this.storageArrays[j] == null && (k1 == 0 || k1 == 15 || k == 0 || k == 15 || l == 0 || l == 15)
                        || this.storageArrays[j] != null && this.storageArrays[j].getBlockByExtId(k, k1, l)
                            .getMaterial() == Material.air) {
                        if (this.worldObj.getBlock(i1, l1 - 1, j1)
                            .getLightValue() > 0) {
                            this.worldObj.func_147451_t(i1, l1 - 1, j1);
                        }

                        if (this.worldObj.getBlock(i1, l1 + 1, j1)
                            .getLightValue() > 0) {
                            this.worldObj.func_147451_t(i1, l1 + 1, j1);
                        }

                        if (this.worldObj.getBlock(i1 - 1, l1, j1)
                            .getLightValue() > 0) {
                            this.worldObj.func_147451_t(i1 - 1, l1, j1);
                        }

                        if (this.worldObj.getBlock(i1 + 1, l1, j1)
                            .getLightValue() > 0) {
                            this.worldObj.func_147451_t(i1 + 1, l1, j1);
                        }

                        if (this.worldObj.getBlock(i1, l1, j1 - 1)
                            .getLightValue() > 0) {
                            this.worldObj.func_147451_t(i1, l1, j1 - 1);
                        }

                        if (this.worldObj.getBlock(i1, l1, j1 + 1)
                            .getLightValue() > 0) {
                            this.worldObj.func_147451_t(i1, l1, j1 + 1);
                        }

                        this.worldObj.func_147451_t(i1, l1, j1);
                    }
                }
            }
        } finally {
            writeUnlock();
        }
    }

    public void func_150809_p() {
        writeLock();
        try {
            this.isTerrainPopulated = true;
            this.isLightPopulated = true;

            if (!this.worldObj.provider.hasNoSky) {
                if (this.worldObj.checkChunksExist(
                    this.xPosition * 16 - 1,
                    0,
                    this.zPosition * 16 - 1,
                    this.xPosition * 16 + 1,
                    63,
                    this.zPosition * 16 + 1)) {
                    for (int i = 0; i < 16; ++i) {
                        for (int j = 0; j < 16; ++j) {
                            if (!this.func_150811_f(i, j)) {
                                this.isLightPopulated = false;
                                break;
                            }
                        }
                    }

                    if (this.isLightPopulated) {
                        Chunk chunk = this.worldObj
                            .getChunkFromBlockCoords(this.xPosition * 16 - 1, this.zPosition * 16);
                        chunk.func_150801_a(3);
                        chunk = this.worldObj.getChunkFromBlockCoords(this.xPosition * 16 + 16, this.zPosition * 16);
                        chunk.func_150801_a(1);
                        chunk = this.worldObj.getChunkFromBlockCoords(this.xPosition * 16, this.zPosition * 16 - 1);
                        chunk.func_150801_a(0);
                        chunk = this.worldObj.getChunkFromBlockCoords(this.xPosition * 16, this.zPosition * 16 + 16);
                        chunk.func_150801_a(2);
                    }
                } else {
                    this.isLightPopulated = false;
                }
            }
        } finally {
            writeUnlock();
        }
    }

    public void func_150801_a(int p_150801_1_) {
        if (this.isTerrainPopulated) {
            int j;

            if (p_150801_1_ == 3) {
                for (j = 0; j < 16; ++j) {
                    this.func_150811_f(15, j);
                }
            } else if (p_150801_1_ == 1) {
                for (j = 0; j < 16; ++j) {
                    this.func_150811_f(0, j);
                }
            } else if (p_150801_1_ == 0) {
                for (j = 0; j < 16; ++j) {
                    this.func_150811_f(j, 15);
                }
            } else if (p_150801_1_ == 2) {
                for (j = 0; j < 16; ++j) {
                    this.func_150811_f(j, 0);
                }
            }
        }
    }

    public boolean func_150811_f(int p_150811_1_, int p_150811_2_) {
        writeLock();
        try {
            int k = this.getTopFilledSegment();
            boolean flag = false;
            boolean flag1 = false;
            int l;

            for (l = k + 16 - 1; l > 63 || l > 0 && !flag1; --l) {
                int i1 = this.func_150808_b(p_150811_1_, l, p_150811_2_);

                if (i1 == 255 && l < 63) {
                    flag1 = true;
                }

                if (!flag && i1 > 0) {
                    flag = true;
                } else if (flag && i1 == 0
                    && !this.worldObj
                        .func_147451_t(this.xPosition * 16 + p_150811_1_, l, this.zPosition * 16 + p_150811_2_)) {
                            return false;
                        }
            }

            for (; l > 0; --l) {
                if (this.getBlock(p_150811_1_, l, p_150811_2_)
                    .getLightValue() > 0) {
                    this.worldObj
                        .func_147451_t(this.xPosition * 16 + p_150811_1_, l, this.zPosition * 16 + p_150811_2_);
                }
            }

            return true;
        } finally {
            writeUnlock();
        }
    }

    /**
     * Retrieves the tile entity, WITHOUT creating it.
     * Good for checking if it exists.
     *
     * @param x X-Coordinate
     * @param y Y-Coordinate
     * @param z Z-Coordinate
     * @return The tile entity at the specified location, if it exists and is valid.
     */
    public TileEntity getTileEntityUnsafe(int x, int y, int z) {
        writeLock();
        try {
            ChunkPosition chunkposition = new ChunkPosition(x, y, z);
            TileEntity tileentity = this.chunkTileEntityMap.get(chunkposition);

            if (tileentity != null && tileentity.isInvalid()) {
                chunkTileEntityMap.remove(chunkposition);
                tileentity = null;
            }
            return tileentity;
        } finally {
            writeUnlock();
        }
    }

    /**
     * Removes the tile entity at the specified position, only if it's
     * marked as invalid.
     *
     * @param x X-Coordinate
     * @param y Y-Coordinate
     * @param z Z-Coordinate
     */
    public void removeInvalidTileEntity(int x, int y, int z) {
        writeLock();
        try {
            ChunkPosition position = new ChunkPosition(x, y, z);
            if (isChunkLoaded.get()) {
                TileEntity entity = chunkTileEntityMap.get(position);
                if (entity != null && entity.isInvalid()) {
                    chunkTileEntityMap.remove(position);
                }
            }
        } finally {
            writeUnlock();
        }
    }
}
