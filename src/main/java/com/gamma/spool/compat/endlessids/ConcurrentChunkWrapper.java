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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;

import com.falsepattern.endlessids.EndlessIDs;
import com.falsepattern.endlessids.Tags;
import com.falsepattern.endlessids.constants.ExtendedConstants;
import com.falsepattern.endlessids.mixin.helpers.ChunkBiomeHook;
import com.gamma.spool.concurrent.ConcurrentChunk;

// Most of this file is directly from EndlessIDs (with some tweaks).
// Critical for compat between EndlessIDs and Spool.
public class ConcurrentChunkWrapper extends ConcurrentChunk implements ChunkBiomeHook {

    private final AtomicReference<short[]> eid$blockBiomeShortArray = new AtomicReference<>(new short[16 * 16]);

    public ConcurrentChunkWrapper(World p_i1995_1_, int p_i1995_2_, int p_i1995_3_) {
        super(p_i1995_1_, p_i1995_2_, p_i1995_3_);
        blockBiomeArray.set(null);
        Arrays.fill(eid$blockBiomeShortArray.get(), (short) -1);
    }

    public ConcurrentChunkWrapper(World p_i45446_1_, Block[] p_i45446_2_, int p_i45446_3_, int p_i45446_4_) {
        super(p_i45446_1_, p_i45446_2_, p_i45446_3_, p_i45446_4_);
        blockBiomeArray.set(null);
        Arrays.fill(eid$blockBiomeShortArray.get(), (short) -1);
    }

    public ConcurrentChunkWrapper(World p_i45447_1_, Block[] p_i45447_2_, byte[] p_i45447_3_, int p_i45447_4_,
        int p_i45447_5_) {
        super(p_i45447_1_, p_i45447_2_, p_i45447_3_, p_i45447_4_, p_i45447_5_);
        blockBiomeArray.set(null);
        Arrays.fill(eid$blockBiomeShortArray.get(), (short) -1);
    }

    private void eid$emergencyCrash() {
        String crashMSG = "A mod that is incompatible with " + Tags.MODNAME
            + " has tried to access the biome array of a"
            + " chunk like in vanilla! Crashing in fear of potential world corruption!\n"
            + "Please report this issue on https://github.com/GTMEGA/EndlessIDs ASAP!";
        EndlessIDs.LOG.fatal(crashMSG);
        throw new UnsupportedOperationException(crashMSG);
    }

    @Override
    public void setBiomeArray(byte[] p_76616_1_) {
        eid$emergencyCrash();
    }

    @Override
    public byte[] getBiomeArray() {
        eid$emergencyCrash();
        return null;
    }

    /**
     * @author FalsePattern
     */
    @Override
    public BiomeGenBase getBiomeGenForWorldCoords(int x, int z, WorldChunkManager manager) {
        short[] array = this.eid$blockBiomeShortArray.get();
        int index = z << 4 | x;
        int id = array[index] & ExtendedConstants.biomeIDMask;

        if (id == ExtendedConstants.biomeIDNull) {
            // Source:
            // https://github.com/embeddedt/ArchaicFix/blob/3d1392f4db5a7221534a6f9b00c0c36a49d9be59/src/main/java/org/embeddedt/archaicfix/mixins/common/core/MixinChunk.java#L52
            if (this.worldObj.isRemote) {
                return BiomeGenBase.ocean;
            }
            BiomeGenBase gen = manager.getBiomeGenAt((this.xPosition << 4) + x, (this.zPosition << 4) + z);
            id = gen.biomeID;

            short[] oldArray;
            short[] newArray;
            do {
                oldArray = this.eid$blockBiomeShortArray.get();
                newArray = oldArray.clone();
                newArray[index] = (byte) (id & 255);
            } while (!this.eid$blockBiomeShortArray.compareAndSet(oldArray, newArray));
        }

        return BiomeGenBase.getBiome(id) == null ? BiomeGenBase.plains : BiomeGenBase.getBiome(id);
    }

    @Override
    public short[] getBiomeShortArray() {
        return eid$blockBiomeShortArray.get();
    }

    @Override
    public void setBiomeShortArray(short[] data) {
        eid$blockBiomeShortArray.set(data);
    }
}
