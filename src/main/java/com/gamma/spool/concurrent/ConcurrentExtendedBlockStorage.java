package com.gamma.spool.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import com.gamma.spool.util.concurrent.interfaces.IAtomic;
import com.google.common.annotations.VisibleForTesting;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ConcurrentExtendedBlockStorage extends ExtendedBlockStorage implements IAtomic {

    public final AtomicInteger blockRefCount = new AtomicInteger(0);
    public final AtomicInteger tickRefCount = new AtomicInteger(0);

    public final AtomicReference<byte[]> blockLSBArray;

    public final AtomicReference<AtomicNibbleArray> blockMSBArray = new AtomicReference<>();

    public final AtomicReference<AtomicNibbleArray> blockMetadataArray = new AtomicReference<>();
    public final AtomicReference<AtomicNibbleArray> blocklightArray = new AtomicReference<>();
    public final AtomicReference<AtomicNibbleArray> skylightArray = new AtomicReference<>();

    public ConcurrentExtendedBlockStorage(int p_i1997_1_, boolean p_i1997_2_) {
        super(p_i1997_1_, p_i1997_2_);

        blockLSBArray = new AtomicReference<>(new byte[4096]);

        this.blockMetadataArray.set(new AtomicNibbleArray(4096, 4));
        this.blocklightArray.set(new AtomicNibbleArray(4096, 4));

        if (p_i1997_2_) {
            this.skylightArray.set(new AtomicNibbleArray(4096, 4));
        }
    }

    /**
     * Returns the block for a location in a chunk, with the extended ID merged from a byte array and a NibbleArray to
     * form a full 12-bit block ID.
     */
    @Override
    public Block getBlockByExtId(int p_150819_1_, int p_150819_2_, int p_150819_3_) {
        int l;

        l = this.blockLSBArray.get()[p_150819_2_ << 8 | p_150819_3_ << 4 | p_150819_1_] & 255;

        if (this.blockMSBArray.get() != null) {
            l |= this.blockMSBArray.get()
                .get(p_150819_1_, p_150819_2_, p_150819_3_) << 8;
        }

        return Block.getBlockById(l);
    }

    @VisibleForTesting
    public int getBlockIntByExtId(int p_150819_1_, int p_150819_2_, int p_150819_3_) {
        int l;

        l = this.blockLSBArray.get()[p_150819_2_ << 8 | p_150819_3_ << 4 | p_150819_1_] & 255;

        if (this.blockMSBArray.get() != null) {
            l |= this.blockMSBArray.get()
                .get(p_150819_1_, p_150819_2_, p_150819_3_) << 8;
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

        l = this.blockLSBArray.get()[p_150818_2_ << 8 | p_150818_3_ << 4 | p_150818_1_] & 255;
        if (this.blockMSBArray.get() != null) {
            l |= this.blockMSBArray.get()
                .get(p_150818_1_, p_150818_2_, p_150818_3_) << 8;
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

        this.blockLSBArray.get()[p_150818_2_ << 8 | p_150818_3_ << 4 | p_150818_1_] = (byte) (i1 & 255);

        if (i1 > 255) {
            if (this.blockMSBArray.get() == null) {
                this.blockMSBArray.set(new AtomicNibbleArray(this.blockLSBArray.get().length, 4));
            }

            this.blockMSBArray.get()
                .set(p_150818_1_, p_150818_2_, p_150818_3_, (i1 & 3840) >> 8);
        } else if (this.blockMSBArray.get() != null) {
            this.blockMSBArray.get()
                .set(p_150818_1_, p_150818_2_, p_150818_3_, 0);
        }
    }

    @VisibleForTesting
    public void func_150818_a(int p_150818_1_, int p_150818_2_, int p_150818_3_, int p_150818_4_) {
        int l;
        l = this.blockLSBArray.get()[p_150818_2_ << 8 | p_150818_3_ << 4 | p_150818_1_] & 255;
        if (this.blockMSBArray.get() != null) {
            l |= this.blockMSBArray.get()
                .get(p_150818_1_, p_150818_2_, p_150818_3_) << 8;
        }

        if (l != 0) {
            this.blockRefCount.decrementAndGet();
        }

        if (p_150818_4_ != 0) {
            this.blockRefCount.incrementAndGet();
        }

        this.blockLSBArray.get()[p_150818_2_ << 8 | p_150818_3_ << 4 | p_150818_1_] = (byte) (p_150818_4_ & 255);

        if (p_150818_4_ > 255) {
            if (this.blockMSBArray.get() == null) {
                this.blockMSBArray.set(new AtomicNibbleArray(this.blockLSBArray.get().length, 4));
            }

            this.blockMSBArray.get()
                .set(p_150818_1_, p_150818_2_, p_150818_3_, (p_150818_4_ & 3840) >> 8);
        } else if (this.blockMSBArray.get() != null) {
            this.blockMSBArray.get()
                .set(p_150818_1_, p_150818_2_, p_150818_3_, 0);
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
        this.blockMSBArray.set(null);
    }

    /**
     * Sets the array of block ID least significant bits for this ExtendedBlockStorage.
     */
    @Override
    public void setBlockLSBArray(byte[] p_76664_1_) {
        this.blockLSBArray.set(p_76664_1_);
    }

    /**
     * Sets the array of blockID most significant bits (blockMSBArray) for this ExtendedBlockStorage.
     */
    @Override
    public void setBlockMSBArray(NibbleArray p_76673_1_) {
        this.blockMSBArray.set((AtomicNibbleArray) p_76673_1_);
    }

    /**
     * Sets the NibbleArray of block metadata (blockMetadataArray) for this ExtendedBlockStorage.
     */
    @Override
    public void setBlockMetadataArray(NibbleArray p_76668_1_) {
        this.blockMetadataArray.set((AtomicNibbleArray) p_76668_1_);
    }

    /**
     * Sets the NibbleArray instance used for Block-light values in this particular storage block.
     */
    @Override
    public void setBlocklightArray(NibbleArray p_76659_1_) {
        this.blocklightArray.set((AtomicNibbleArray) p_76659_1_);
    }

    /**
     * Sets the NibbleArray instance used for Sky-light values in this particular storage block.
     */
    @Override
    public void setSkylightArray(NibbleArray p_76666_1_) {
        this.skylightArray.set((AtomicNibbleArray) p_76666_1_);
    }

    /**
     * Called by a Chunk to initialize the MSB array if getBlockMSBArray returns null. Returns the newly-created
     * NibbleArray instance.
     */
    @SideOnly(Side.CLIENT)
    @Override
    public NibbleArray createBlockMSBArray() {
        this.blockMSBArray.set(new AtomicNibbleArray(this.blockLSBArray.get().length, 4));
        return this.blockMSBArray.get();
    }

    @Override
    public NibbleArray getBlocklightArray() {
        return this.blocklightArray.get();
    }

    @Override
    public byte[] getBlockLSBArray() {
        return this.blockLSBArray.get();
    }

    @Override
    public NibbleArray getBlockMSBArray() {
        return this.blockMSBArray.get();
    }

    @Override
    public int getExtBlocklightValue(int p_76674_1_, int p_76674_2_, int p_76674_3_) {
        return this.blocklightArray.get()
            .get(p_76674_1_, p_76674_2_, p_76674_3_);
    }

    @Override
    public int getExtBlockMetadata(int p_76665_1_, int p_76665_2_, int p_76665_3_) {
        return this.blockMetadataArray.get()
            .get(p_76665_1_, p_76665_2_, p_76665_3_);
    }

    @Override
    public int getExtSkylightValue(int p_76670_1_, int p_76670_2_, int p_76670_3_) {
        return this.skylightArray.get()
            .get(p_76670_1_, p_76670_2_, p_76670_3_);
    }

    @Override
    public NibbleArray getMetadataArray() {
        return this.blockMetadataArray.get();
    }

    @Override
    public NibbleArray getSkylightArray() {
        return this.skylightArray.get();
    }

    @Override
    public void setExtBlocklightValue(int p_76677_1_, int p_76677_2_, int p_76677_3_, int p_76677_4_) {
        this.blocklightArray.get()
            .set(p_76677_1_, p_76677_2_, p_76677_3_, p_76677_4_);
    }

    @Override
    public void setExtBlockMetadata(int p_76654_1_, int p_76654_2_, int p_76654_3_, int p_76654_4_) {
        this.blockMetadataArray.get()
            .set(p_76654_1_, p_76654_2_, p_76654_3_, p_76654_4_);
    }

    @Override
    public void setExtSkylightValue(int p_76657_1_, int p_76657_2_, int p_76657_3_, int p_76657_4_) {
        this.skylightArray.get()
            .set(p_76657_1_, p_76657_2_, p_76657_3_, p_76657_4_);
    }
}
