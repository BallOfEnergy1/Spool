package com.gamma.spool.util.concurrent.chunk;

public class ChunkCoordUtil {

    public static long INT_MASK = (1L << Integer.SIZE) - 1;

    public static int getPackedX(long pos) {
        return (int) (pos & INT_MASK);
    }

    public static int getPackedZ(long pos) {
        return (int) (pos >>> 32 & INT_MASK);
    }
}
