package com.gamma.spool.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import com.gamma.spool.util.concurrent.interfaces.IStampedConcurrent;
import com.google.common.annotations.VisibleForTesting;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ConcurrentExtendedBlockStorage extends ExtendedBlockStorage implements IStampedConcurrent {

    public final AtomicInteger blockRefCount = new AtomicInteger(0);
    public final AtomicInteger tickRefCount = new AtomicInteger(0);

    private final StampedLock lock = new StampedLock();

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
        int l;
        long stamp = optimisticReadLock();
        try {
            l = this.blockLSBArray[p_150819_2_ << 8 | p_150819_3_ << 4 | p_150819_1_] & 255;

            if (this.blockMSBArray != null) {
                l |= this.blockMSBArray.get(p_150819_1_, p_150819_2_, p_150819_3_) << 8;
            }
        } finally {
            if ((stamp = optimisticReadUnlock(stamp)) != 0) {
                try {
                    l = this.blockLSBArray[p_150819_2_ << 8 | p_150819_3_ << 4 | p_150819_1_] & 255;

                    if (this.blockMSBArray != null) {
                        l |= this.blockMSBArray.get(p_150819_1_, p_150819_2_, p_150819_3_) << 8;
                    }
                } finally {
                    readUnlock(stamp);
                }
            }
        }

        return Block.getBlockById(l);
    }

    @VisibleForTesting
    public int getBlockIntByExtId(int p_150819_1_, int p_150819_2_, int p_150819_3_) {
        int l;
        long stamp = optimisticReadLock();
        try {
            l = this.blockLSBArray[p_150819_2_ << 8 | p_150819_3_ << 4 | p_150819_1_] & 255;

            if (this.blockMSBArray != null) {
                l |= this.blockMSBArray.get(p_150819_1_, p_150819_2_, p_150819_3_) << 8;
            }
        } finally {
            if ((stamp = optimisticReadUnlock(stamp)) != 0) {
                try {
                    l = this.blockLSBArray[p_150819_2_ << 8 | p_150819_3_ << 4 | p_150819_1_] & 255;

                    if (this.blockMSBArray != null) {
                        l |= this.blockMSBArray.get(p_150819_1_, p_150819_2_, p_150819_3_) << 8;
                    }
                } finally {
                    readUnlock(stamp);
                }
            }
        }

        return l;
    }

    @Override
    public boolean getNeedsRandomTick() {
        return this.tickRefCount.get() > 0;
    }

    @Override
    public boolean isEmpty() {
        return this.blockRefCount.get() == 0;
    }

    @Override
    public void func_150818_a(int p_150818_1_, int p_150818_2_, int p_150818_3_, Block p_150818_4_) {
        int l;
        long stamp = optimisticReadLock();
        try {
            l = this.blockLSBArray[p_150818_2_ << 8 | p_150818_3_ << 4 | p_150818_1_] & 255;
            if (this.blockMSBArray != null) {
                l |= this.blockMSBArray.get(p_150818_1_, p_150818_2_, p_150818_3_) << 8;
            }
        } finally {
            if ((stamp = optimisticReadUnlock(stamp)) != 0) {
                try {
                    l = this.blockLSBArray[p_150818_2_ << 8 | p_150818_3_ << 4 | p_150818_1_] & 255;
                    if (this.blockMSBArray != null) {
                        l |= this.blockMSBArray.get(p_150818_1_, p_150818_2_, p_150818_3_) << 8;
                    }
                } finally {
                    readUnlock(stamp);
                }
            }
        }

        Block block1 = Block.getBlockById(l);

        if (block1 != Blocks.air) {
            this.blockRefCount.decrementAndGet();

            if (block1.getTickRandomly()) {
                this.tickRefCount.decrementAndGet();
            }
        }

        if (p_150818_4_ != Blocks.air) {
            this.blockRefCount.incrementAndGet();

            if (p_150818_4_.getTickRandomly()) {
                this.tickRefCount.incrementAndGet();
            }
        }

        int i1 = Block.getIdFromBlock(p_150818_4_);

        stamp = writeLock();
        try {
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
            writeUnlock(stamp);
        }
    }

    @VisibleForTesting
    public void func_150818_a(int p_150818_1_, int p_150818_2_, int p_150818_3_, int p_150818_4_) {
        int l;
        long stamp = optimisticReadLock();
        try {
            l = this.blockLSBArray[p_150818_2_ << 8 | p_150818_3_ << 4 | p_150818_1_] & 255;
            if (this.blockMSBArray != null) {
                l |= this.blockMSBArray.get(p_150818_1_, p_150818_2_, p_150818_3_) << 8;
            }
        } finally {
            if ((stamp = optimisticReadUnlock(stamp)) != 0) {
                try {
                    l = this.blockLSBArray[p_150818_2_ << 8 | p_150818_3_ << 4 | p_150818_1_] & 255;
                    if (this.blockMSBArray != null) {
                        l |= this.blockMSBArray.get(p_150818_1_, p_150818_2_, p_150818_3_) << 8;
                    }
                } finally {
                    readUnlock(stamp);
                }
            }
        }

        if (l != 0) {
            this.blockRefCount.decrementAndGet();
        }

        if (p_150818_4_ != 0) {
            this.blockRefCount.incrementAndGet();
        }

        stamp = writeLock();
        try {
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
            writeUnlock(stamp);
        }
    }

    @Override
    public void removeInvalidBlocks() {
        int refCount = 0;
        int tickRefCount = 0;

        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                for (int k = 0; k < 16; ++k) {
                    Block block = this.getBlockByExtId(i, j, k);

                    if (block != Blocks.air) {
                        refCount++;

                        if (block.getTickRandomly()) {
                            tickRefCount++;
                        }
                    }
                }
            }
        }

        this.blockRefCount.set(refCount);
        this.tickRefCount.set(tickRefCount);
    }

    @VisibleForTesting
    public void removeInvalidBlocksInt() {

        int refCount = 0;
        int tickRefCount = 0;

        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                for (int k = 0; k < 16; ++k) {
                    int block = this.getBlockIntByExtId(i, j, k);

                    if (block != 0) {
                        refCount++;
                    }
                }
            }
        }

        this.blockRefCount.set(refCount);
        this.tickRefCount.set(tickRefCount);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void clearMSBArray() {
        long stamp = writeLock();
        try {
            this.blockMSBArray = null;
        } finally {
            writeUnlock(stamp);
        }
    }

    /**
     * Sets the array of block ID least significant bits for this ExtendedBlockStorage.
     */
    @Override
    public void setBlockLSBArray(byte[] p_76664_1_) {
        long stamp = writeLock();
        try {
            this.blockLSBArray = p_76664_1_;
        } finally {
            writeUnlock(stamp);
        }
    }

    /**
     * Sets the array of blockID most significant bits (blockMSBArray) for this ExtendedBlockStorage.
     */
    @Override
    public void setBlockMSBArray(NibbleArray p_76673_1_) {
        long stamp = writeLock();
        try {
            this.blockMSBArray = p_76673_1_;
        } finally {
            writeUnlock(stamp);
        }
    }

    /**
     * Sets the NibbleArray of block metadata (blockMetadataArray) for this ExtendedBlockStorage.
     */
    @Override
    public void setBlockMetadataArray(NibbleArray p_76668_1_) {
        long stamp = writeLock();
        try {
            this.blockMetadataArray = p_76668_1_;
        } finally {
            writeUnlock(stamp);
        }
    }

    /**
     * Sets the NibbleArray instance used for Block-light values in this particular storage block.
     */
    @Override
    public void setBlocklightArray(NibbleArray p_76659_1_) {
        long stamp = writeLock();
        try {
            this.blocklightArray = p_76659_1_;
        } finally {
            writeUnlock(stamp);
        }
    }

    /**
     * Sets the NibbleArray instance used for Sky-light values in this particular storage block.
     */
    @Override
    public void setSkylightArray(NibbleArray p_76666_1_) {
        long stamp = writeLock();
        try {
            this.skylightArray = p_76666_1_;
        } finally {
            writeUnlock(stamp);
        }
    }

    /**
     * Called by a Chunk to initialize the MSB array if getBlockMSBArray returns null. Returns the newly-created
     * NibbleArray instance.
     */
    @SideOnly(Side.CLIENT)
    @Override
    public NibbleArray createBlockMSBArray() {
        long stamp = writeLock();
        try {
            return this.blockMSBArray = new AtomicNibbleArray(this.blockLSBArray.length, 4);
        } finally {
            writeUnlock(stamp);
        }
    }
}
