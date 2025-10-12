package com.gamma.spool.compat.endlessids;

import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import com.falsepattern.endlessids.mixin.helpers.ChunkBiomeHook;
import com.gamma.spool.core.SpoolCompat;

public class Compat {

    public static short[] getBiomeArray(Chunk chunk) {
        return ((ChunkBiomeHook) chunk).getBiomeShortArray();
    }

    public static void setBiomeArray(Chunk chunk, short[] data) {
        ((ChunkBiomeHook) chunk).setBiomeShortArray(data);
    }

    public static void setChunkBiomes(Chunk chunk, BiomeGenBase[] abiomegenbase) {
        if (SpoolCompat.isEndlessIDsLoaded) {
            short[] ashort1 = new short[256];

            for (int k = 0; k < ashort1.length; ++k) {
                ashort1[k] = (short) abiomegenbase[k].biomeID;
            }
            setBiomeArray(chunk, ashort1);
        } else {
            byte[] abyte1 = new byte[256];

            for (int k = 0; k < abyte1.length; ++k) {
                abyte1[k] = (byte) abiomegenbase[k].biomeID;
            }
            chunk.setBiomeArray(abyte1);
        }
    }
}
