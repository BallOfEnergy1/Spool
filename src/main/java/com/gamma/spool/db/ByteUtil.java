package com.gamma.spool.db;

public class ByteUtil {

    public static byte[] bytesFromShort(short s) {
        return new byte[] { (byte) (s & 0xFF), (byte) ((s >> 8) & 0xFF) };
    }

    public static short shortFromBytes(byte[] bytes) {
        return (short) (bytes[0] | (bytes[1] << 8));
    }

    public static byte[] bytesFromInt(int i) {
        return new byte[] { (byte) (i & 0xFF), (byte) ((i >> 8) & 0xFF), (byte) ((i >> 16) & 0xFF),
            (byte) ((i >> 24) & 0xFF) };
    }

    public static int intFromBytes(byte[] bytes) {
        return bytes[0] | (bytes[1] << 8) | (bytes[2] << 16) | (bytes[3] << 24);
    }

    public static byte[] bytesFromLong(long l) {
        return new byte[] { (byte) (l & 0xFF), (byte) ((l >> 8) & 0xFF), (byte) ((l >> 16) & 0xFF),
            (byte) ((l >> 24) & 0xFF), (byte) ((l >> 32) & 0xFF), (byte) ((l >> 40) & 0xFF), (byte) ((l >> 48) & 0xFF),
            (byte) ((l >> 56) & 0xFF) };
    }

    public static long longFromBytes(byte[] bytes) {
        return bytes[0] | (bytes[1] << 8)
            | (bytes[2] << 16)
            | (bytes[3] << 24)
            | ((long) bytes[4] << 32)
            | ((long) bytes[5] << 40)
            | ((long) bytes[6] << 48)
            | ((long) bytes[7] << 56);
    }
}
