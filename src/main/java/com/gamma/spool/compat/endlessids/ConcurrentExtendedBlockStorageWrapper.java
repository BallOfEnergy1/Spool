/*
 * EndlessIDs
 * Copyright (C) 2022-2025 FalsePattern, The MEGA Team
 * All Rights Reserved
 * The above copyright notice, this permission notice and the words "MEGA"
 * shall be included in all copies or substantial portions of the Software.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.gamma.spool.compat.endlessids;

import static com.falsepattern.endlessids.constants.ExtendedConstants.blocksPerSubChunk;

import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.chunk.NibbleArray;

import com.falsepattern.endlessids.EndlessIDs;
import com.falsepattern.endlessids.Hooks;
import com.falsepattern.endlessids.Tags;
import com.falsepattern.endlessids.config.GeneralConfig;
import com.falsepattern.endlessids.mixin.helpers.SubChunkBlockHook;
import com.gamma.spool.concurrent.AtomicNibbleArray;
import com.gamma.spool.concurrent.ConcurrentExtendedBlockStorage;

public class ConcurrentExtendedBlockStorageWrapper extends ConcurrentExtendedBlockStorage implements SubChunkBlockHook {

    private final AtomicReference<AtomicNibbleArray> b2High = new AtomicReference<>();
    private final AtomicReference<byte[]> b3 = new AtomicReference<>();
    private final AtomicReference<AtomicNibbleArray> m1High = new AtomicReference<>();
    private final AtomicReference<byte[]> m2 = new AtomicReference<>();

    public ConcurrentExtendedBlockStorageWrapper(int p_i1997_1_, boolean p_i1997_2_) {
        super(p_i1997_1_, p_i1997_2_);
    }

    public int eid$getID(int x, int y, int z) {
        int index = y << 8 | z << 4 | x;
        int id = blockLSBArray.get()[index] & 0xFF;
        if (blockMSBArray.get() != null) {
            id |= blockMSBArray.get()
                .get(x, y, z) << 8;
            if (b2High.get() != null) {
                id |= b2High.get()
                    .get(x, y, z) << 12;
                if (b3.get() != null) {
                    id |= (b3.get()[index] & 0xFF) << 16;
                }
            }
        }
        return id;
    }

    public void eid$setID(int x, int y, int z, int id) {
        int index = y << 8 | z << 4 | x;
        blockLSBArray.get()[index] = (byte) (id & 0xFF);
        if (id > 0xFF) {
            if (blockMSBArray.get() == null) {
                eid$createB2Low();
            }
            if (id > 0xFFF) {
                if (b2High.get() == null) {
                    eid$createB2High();
                }
                if (id > 0xFFFF && b3.get() == null) {
                    eid$createB3();
                }
            }
        }
        if (blockMSBArray.get() != null) {
            blockMSBArray.get()
                .set(x, y, z, (id >>> 8) & 0xF);
            if (b2High.get() != null) {
                b2High.get()
                    .set(x, y, z, (id >>> 12) & 0xF);
                if (b3.get() != null) {
                    b3.get()[index] = (byte) ((id >>> 16) & 0xFF);
                }
            }
        }
    }

    /**
     * @author FalsePattern
     */
    @Override
    public Block getBlockByExtId(int x, int y, int z) {
        return Block.getBlockById(eid$getID(x, y, z));
    }

    /**
     * @author FalsePattern
     */
    @Override
    public void func_150818_a(int x, int y, int z, Block newBlock) {
        Block oldBlock = this.getBlockByExtId(x, y, z);
        if (oldBlock != Blocks.air) {
            this.blockRefCount.decrementAndGet();
            if (oldBlock.getTickRandomly()) {
                this.tickRefCount.decrementAndGet();
            }
        }

        if (newBlock != Blocks.air) {
            this.blockRefCount.incrementAndGet();
            if (newBlock.getTickRandomly()) {
                this.tickRefCount.incrementAndGet();
            }
        }

        int blockID = Hooks.getIdFromBlockWithCheck(newBlock, oldBlock);
        eid$setID(x, y, z, blockID);
    }

    /**
     * @author FalsePattern
     */
    @Override
    public int getExtBlockMetadata(int x, int y, int z) {
        int meta = blockMetadataArray.get()
            .get(x, y, z);
        if (m1High.get() != null) {
            meta |= m1High.get()
                .get(x, y, z) << 4;
            if (m2.get() != null) {
                meta |= (m2.get()[(y << 8) | (z << 4) | x] & 0xFF) << 8;
            }
        }
        return meta;
    }

    /**
     * @author FalsePattern
     */
    @Override
    public void setExtBlockMetadata(int x, int y, int z, int meta) {
        blockMetadataArray.get()
            .set(x, y, z, meta & 0xF);
        if (meta > 0xF) {
            if (m1High.get() == null) {
                eid$createM1High();
            }
            if (meta > 0xFF && m2.get() == null) {
                eid$createM2();
            }
        }
        if (m1High.get() != null) {
            m1High.get()
                .set(x, y, z, (meta >>> 4) & 0xF);
            if (m2.get() != null) {
                m2.get()[(y << 8) | (z << 4) | x] = (byte) ((meta >>> 8) & 0xFF);
            }
        }
    }

    @Override
    public int eid$getMetadata(int x, int y, int z) {
        return getExtBlockMetadata(x, y, z);
    }

    @Override
    public void eid$setMetadata(int x, int y, int z, int id) {
        setExtBlockMetadata(x, y, z, id);
    }

    @Override
    public void removeInvalidBlocks() {
        int refCount = 0;
        int tickRefCount = 0;

        for (int x = 0; x < 16; ++x) {
            for (int y = 0; y < 16; ++y) {
                for (int z = 0; z < 16; ++z) {
                    Block block = this.getBlockByExtId(x, y, z);
                    if (block == null && GeneralConfig.removeInvalidBlocks) {
                        this.func_150818_a(x, y, z, Blocks.air);
                        block = Blocks.air;
                    }

                    if (block != Blocks.air && block != null) {
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

    private UnsupportedOperationException emergencyCrash() {
        String crashMSG = "A mod that is incompatible with " + Tags.MODNAME
            + " has tried to access the block array of a"
            + " chunk like in vanilla! Crashing in fear of potential world corruption!\n"
            + "Please report this issue on https://github.com/GTMEGA/EndlessIDs ASAP!";
        EndlessIDs.LOG.fatal(crashMSG);
        return new UnsupportedOperationException(crashMSG);
    }

    @Override
    public byte[] eid$getB1() {
        return blockLSBArray.get();
    }

    @Override
    public void eid$setB1(byte[] data) {
        blockLSBArray.set(data);
    }

    @Override
    public NibbleArray eid$getB2Low() {
        return blockMSBArray.get();
    }

    @Override
    public void eid$setB2Low(NibbleArray data) {
        blockMSBArray.set((AtomicNibbleArray) data);
    }

    @Override
    public NibbleArray eid$createB2Low() {
        AtomicNibbleArray arr = new AtomicNibbleArray(blocksPerSubChunk, 4);
        blockMSBArray.set(arr);
        return arr;
    }

    @Override
    public NibbleArray eid$getB2High() {
        return b2High.get();
    }

    @Override
    public void eid$setB2High(NibbleArray data) {
        b2High.set((AtomicNibbleArray) data);
    }

    @Override
    public NibbleArray eid$createB2High() {
        AtomicNibbleArray arr = new AtomicNibbleArray(blocksPerSubChunk, 4);
        b2High.set(arr);
        return arr;
    }

    @Override
    public byte[] eid$getB3() {
        return b3.get();
    }

    @Override
    public void eid$setB3(byte[] data) {
        b3.set(data);
    }

    @Override
    public byte[] eid$createB3() {
        byte[] arr = new byte[blocksPerSubChunk];
        b3.set(arr);
        return arr;
    }

    @Override
    public NibbleArray eid$getM1Low() {
        return blockMetadataArray.get();
    }

    @Override
    public void eid$setM1Low(NibbleArray m1Low) {
        blockMetadataArray.set((AtomicNibbleArray) m1Low);
    }

    @Override
    public NibbleArray eid$getM1High() {
        return m1High.get();
    }

    @Override
    public void eid$setM1High(NibbleArray m1High) {
        this.m1High.set((AtomicNibbleArray) m1High);
    }

    @Override
    public NibbleArray eid$createM1High() {
        AtomicNibbleArray arr = new AtomicNibbleArray(blocksPerSubChunk, 4);
        m1High.set(arr);
        return arr;
    }

    @Override
    public byte[] eid$getM2() {
        return m2.get();
    }

    @Override
    public void eid$setM2(byte[] m2) {
        this.m2.set(m2);
    }

    @Override
    public byte[] eid$createM2() {
        byte[] arr = new byte[blocksPerSubChunk];
        m2.set(arr);
        return arr;
    }

    @Override
    public int eid$getBlockMask() {
        if (blockMSBArray.get() == null) {
            return 0b00;
        }
        if (b2High.get() == null) {
            return 0b01;
        }
        if (b3.get() == null) {
            return 0b10;
        }
        return 0b11;
    }

    @Override
    public int eid$getMetadataMask() {
        if (m1High.get() == null) {
            return 0b01;
        }
        if (m2.get() == null) {
            return 0b10;
        }
        return 0b11;
    }

    @Override
    public NibbleArray getBlockMSBArray() {
        throw emergencyCrash();
    }

    @Override
    public NibbleArray createBlockMSBArray() {
        throw emergencyCrash();
    }

    @Override
    public void clearMSBArray() {
        throw emergencyCrash();
    }

    @Override
    public void setBlockMSBArray(NibbleArray p_76673_1_) {
        throw emergencyCrash();
    }

    @Override
    public void setBlockLSBArray(byte[] p_76664_1_) {
        throw emergencyCrash();
    }

    @Override
    public byte[] getBlockLSBArray() {
        throw emergencyCrash();
    }

    @Override
    public void setBlockMetadataArray(NibbleArray arr) {
        throw emergencyCrash();
    }

    @Override
    public NibbleArray getMetadataArray() {
        throw emergencyCrash();
    }
}
