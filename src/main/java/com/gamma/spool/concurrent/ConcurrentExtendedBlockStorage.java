package com.gamma.spool.concurrent;

import java.util.concurrent.locks.StampedLock;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import com.gamma.spool.util.concurrent.IConcurrent;
import com.google.common.annotations.VisibleForTesting;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ConcurrentExtendedBlockStorage extends ExtendedBlockStorage implements IConcurrent {

    public final StampedLock lock = new StampedLock();

    @Override
    public StampedLock getLock() {
        return lock;
    }

    public ConcurrentExtendedBlockStorage(int p_i1997_1_, boolean p_i1997_2_) {
        super(p_i1997_1_, p_i1997_2_);
        this.blockMetadataArray = new AtomicNibbleArray(this.blockLSBArray.length, 4);
        this.blocklightArray = new AtomicNibbleArray(this.blockLSBArray.length, 4);

        if (p_i1997_2_) {
            this.skylightArray = new AtomicNibbleArray(this.blockLSBArray.length, 4);
        }
    }

    /**
     * Returns the block for a location in a chunk, with the extended ID merged from a byte array and a NibbleArray to
     * form a full 12-bit block ID.
     */
    @Override
    public Block getBlockByExtId(int p_150819_1_, int p_150819_2_, int p_150819_3_) {
        readLock();
        try {
            int l = this.blockLSBArray[p_150819_2_ << 8 | p_150819_3_ << 4 | p_150819_1_] & 255;

            if (this.blockMSBArray != null) {
                l |= this.blockMSBArray.get(p_150819_1_, p_150819_2_, p_150819_3_) << 8;
            }
            return Block.getBlockById(l);
        } finally {
            readUnlock();
        }
    }

    @VisibleForTesting
    public int getBlockIntByExtId(int p_150819_1_, int p_150819_2_, int p_150819_3_) {
        readLock();
        try {
            int l = this.blockLSBArray[p_150819_2_ << 8 | p_150819_3_ << 4 | p_150819_1_] & 255;

            if (this.blockMSBArray != null) {
                l |= this.blockMSBArray.get(p_150819_1_, p_150819_2_, p_150819_3_) << 8;
            }
            return l;
        } finally {
            readUnlock();
        }
    }

    @Override
    public boolean getNeedsRandomTick() {
        readLock();
        try {
            return this.tickRefCount > 0;
        } finally {
            readUnlock();
        }
    }

    @Override
    public int getYLocation() {
        readLock();
        try {
            return this.yBase;
        } finally {
            readUnlock();
        }
    }

    @Override
    public boolean isEmpty() {
        readLock();
        try {
            return this.blockRefCount == 0;
        } finally {
            readUnlock();
        }
    }

    @Override
    public void func_150818_a(int p_150818_1_, int p_150818_2_, int p_150818_3_, Block p_150818_4_) {
        writeLock();
        try {
            int l = this.blockLSBArray[p_150818_2_ << 8 | p_150818_3_ << 4 | p_150818_1_] & 255;

            if (this.blockMSBArray != null) {
                l |= this.blockMSBArray.get(p_150818_1_, p_150818_2_, p_150818_3_) << 8;
            }

            Block block1 = Block.getBlockById(l);

            if (block1 != Blocks.air) {
                --this.blockRefCount;

                if (block1.getTickRandomly()) {
                    --this.tickRefCount;
                }
            }

            if (p_150818_4_ != Blocks.air) {
                ++this.blockRefCount;

                if (p_150818_4_.getTickRandomly()) {
                    ++this.tickRefCount;
                }
            }

            int i1 = Block.getIdFromBlock(p_150818_4_);
            this.blockLSBArray[p_150818_2_ << 8 | p_150818_3_ << 4 | p_150818_1_] = (byte) (i1 & 255);

            if (i1 > 255) {
                if (this.blockMSBArray == null) {
                    this.blockMSBArray = new AtomicNibbleArray(this.blockLSBArray.length, 4);
                }

                this.blockMSBArray.set(p_150818_1_, p_150818_2_, p_150818_3_, (i1 & 3840) >> 8);
            } else if (this.blockMSBArray != null) {
                this.blockMSBArray.set(p_150818_1_, p_150818_2_, p_150818_3_, 0);
            }
        } finally {
            writeUnlock();
        }
    }

    @VisibleForTesting
    public void func_150818_a(int p_150818_1_, int p_150818_2_, int p_150818_3_, int p_150818_4_) {
        writeLock();
        try {
            int l = this.blockLSBArray[p_150818_2_ << 8 | p_150818_3_ << 4 | p_150818_1_] & 255;

            if (this.blockMSBArray != null) {
                l |= this.blockMSBArray.get(p_150818_1_, p_150818_2_, p_150818_3_) << 8;
            }

            if (l != 0) {
                --this.blockRefCount;
            }

            if (p_150818_4_ != 0) {
                ++this.blockRefCount;
            }

            this.blockLSBArray[p_150818_2_ << 8 | p_150818_3_ << 4 | p_150818_1_] = (byte) (p_150818_4_ & 255);

            if (p_150818_4_ > 255) {
                if (this.blockMSBArray == null) {
                    this.blockMSBArray = new AtomicNibbleArray(this.blockLSBArray.length, 4);
                }

                this.blockMSBArray.set(p_150818_1_, p_150818_2_, p_150818_3_, (p_150818_4_ & 3840) >> 8);
            } else if (this.blockMSBArray != null) {
                this.blockMSBArray.set(p_150818_1_, p_150818_2_, p_150818_3_, 0);
            }
        } finally {
            writeUnlock();
        }
    }

    /**
     * Returns the metadata associated with the block at the given coordinates in this ExtendedBlockStorage.
     */
    @Override
    public int getExtBlockMetadata(int p_76665_1_, int p_76665_2_, int p_76665_3_) {
        readLock();
        try {
            return this.blockMetadataArray.get(p_76665_1_, p_76665_2_, p_76665_3_);
        } finally {
            readUnlock();
        }
    }

    /**
     * Sets the metadata of the Block at the given coordinates in this ExtendedBlockStorage to the given metadata.
     */
    @Override
    public void setExtBlockMetadata(int p_76654_1_, int p_76654_2_, int p_76654_3_, int p_76654_4_) {
        writeLock();
        try {
            this.blockMetadataArray.set(p_76654_1_, p_76654_2_, p_76654_3_, p_76654_4_);
        } finally {
            writeUnlock();
        }
    }

    /**
     * Sets the saved Sky-light value in the extended block storage structure.
     */
    @Override
    public void setExtSkylightValue(int p_76657_1_, int p_76657_2_, int p_76657_3_, int p_76657_4_) {
        writeLock();
        try {
            this.skylightArray.set(p_76657_1_, p_76657_2_, p_76657_3_, p_76657_4_);
        } finally {
            writeUnlock();
        }
    }

    /**
     * Gets the saved Sky-light value in the extended block storage structure.
     */
    @Override
    public int getExtSkylightValue(int p_76670_1_, int p_76670_2_, int p_76670_3_) {
        readLock();
        try {
            return this.skylightArray.get(p_76670_1_, p_76670_2_, p_76670_3_);
        } finally {
            readUnlock();
        }
    }

    /**
     * Sets the saved Block-light value in the extended block storage structure.
     */
    @Override
    public void setExtBlocklightValue(int p_76677_1_, int p_76677_2_, int p_76677_3_, int p_76677_4_) {
        writeLock();
        try {
            this.blocklightArray.set(p_76677_1_, p_76677_2_, p_76677_3_, p_76677_4_);
        } finally {
            writeUnlock();
        }
    }

    /**
     * Gets the saved Block-light value in the extended block storage structure.
     */
    @Override
    public int getExtBlocklightValue(int p_76674_1_, int p_76674_2_, int p_76674_3_) {
        readLock();
        try {
            return this.blocklightArray.get(p_76674_1_, p_76674_2_, p_76674_3_);
        } finally {
            readUnlock();
        }
    }

    @Override
    public void removeInvalidBlocks() {
        writeLock();
        try {
            this.blockRefCount = 0;
            this.tickRefCount = 0;

            for (int i = 0; i < 16; ++i) {
                for (int j = 0; j < 16; ++j) {
                    for (int k = 0; k < 16; ++k) {
                        Block block = this.getBlockByExtId(i, j, k);

                        if (block != Blocks.air) {
                            ++this.blockRefCount;

                            if (block.getTickRandomly()) {
                                ++this.tickRefCount;
                            }
                        }
                    }
                }
            }
        } finally {
            writeUnlock();
        }
    }

    @VisibleForTesting
    public void removeInvalidBlocksInt() {
        writeLock();
        try {
            this.blockRefCount = 0;
            this.tickRefCount = 0;

            for (int i = 0; i < 16; ++i) {
                for (int j = 0; j < 16; ++j) {
                    for (int k = 0; k < 16; ++k) {
                        int block = this.getBlockIntByExtId(i, j, k);

                        if (block != 0) {
                            ++this.blockRefCount;
                        }
                    }
                }
            }
        } finally {
            writeUnlock();
        }
    }

    @Override
    public byte[] getBlockLSBArray() {
        readLock();
        try {
            return this.blockLSBArray;
        } finally {
            readUnlock();
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void clearMSBArray() {
        writeLock();
        try {
            this.blockMSBArray = null;
        } finally {
            writeUnlock();
        }
    }

    /**
     * Sets the array of block ID least significant bits for this ExtendedBlockStorage.
     */
    @Override
    public void setBlockLSBArray(byte[] p_76664_1_) {
        writeLock();
        try {
            this.blockLSBArray = p_76664_1_;
        } finally {
            writeUnlock();
        }
    }

    /**
     * Sets the array of blockID most significant bits (blockMSBArray) for this ExtendedBlockStorage.
     */
    @Override
    public void setBlockMSBArray(NibbleArray p_76673_1_) {
        writeLock();
        try {
            this.blockMSBArray = p_76673_1_;
        } finally {
            writeUnlock();
        }
    }

    /**
     * Sets the NibbleArray of block metadata (blockMetadataArray) for this ExtendedBlockStorage.
     */
    @Override
    public void setBlockMetadataArray(NibbleArray p_76668_1_) {
        writeLock();
        try {
            this.blockMetadataArray = p_76668_1_;
        } finally {
            writeUnlock();
        }
    }

    /**
     * Sets the NibbleArray instance used for Block-light values in this particular storage block.
     */
    @Override
    public void setBlocklightArray(NibbleArray p_76659_1_) {
        writeLock();
        try {
            this.blocklightArray = p_76659_1_;
        } finally {
            writeUnlock();
        }
    }

    /**
     * Sets the NibbleArray instance used for Sky-light values in this particular storage block.
     */
    @Override
    public void setSkylightArray(NibbleArray p_76666_1_) {
        writeLock();
        try {
            this.skylightArray = p_76666_1_;
        } finally {
            writeUnlock();
        }
    }

    /**
     * Called by a Chunk to initialize the MSB array if getBlockMSBArray returns null. Returns the newly-created
     * NibbleArray instance.
     */
    @SideOnly(Side.CLIENT)
    @Override
    public NibbleArray createBlockMSBArray() {
        writeLock();
        try {
            return this.blockMSBArray = new AtomicNibbleArray(this.blockLSBArray.length, 4);
        } finally {
            writeUnlock();
        }
    }
}
