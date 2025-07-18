package com.gamma.spool.concurrent;

import static com.gamma.spool.SpoolUnsafe.BYTE_ARRAY_BASE_OFFSET;

import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.world.chunk.NibbleArray;

import com.gamma.spool.SpoolUnsafe;
import com.google.common.annotations.VisibleForTesting;

import sun.misc.Unsafe;

@SuppressWarnings("unused")
public class AtomicNibbleArray extends NibbleArray {

    public final AtomicReference<byte[]> concurrentData = new AtomicReference<>();

    private static final Unsafe U = SpoolUnsafe.getUnsafe();

    public byte[] getByteArray() {

        return concurrentData.get();
    }

    public AtomicNibbleArray(int length, int depth) {
        super(length, depth);
        data = null;
        concurrentData.set(new byte[length >> 1]);
    }

    public AtomicNibbleArray(byte[] array, int depth) {
        super(array, depth);
        data = null;
        concurrentData.set(array);
    }

    @Override
    public int get(int x, int y, int z) {

        final int nibbleIndex = y << this.depthBitsPlusFour | z << this.depthBits | x;
        final int byteIndex = nibbleIndex >> 1;
        final int intIndex = byteIndex & ~3;
        final int internalOffset = byteIndex & 3;
        final int shift = (nibbleIndex & 1) << 2;

        byte[] array = concurrentData.get();
        int value = U.getIntVolatile(array, BYTE_ARRAY_BASE_OFFSET + intIndex);
        byte targetByte = (byte) (value >> (internalOffset << 3));
        return (targetByte >> shift) & 0xF;
    }

    @Override
    public void set(int x, int y, int z, int value) {
        final int nibbleIndex = y << this.depthBitsPlusFour | z << this.depthBits | x;
        final int byteIndex = nibbleIndex >> 1;
        final int intIndex = byteIndex & ~3;
        final int internalOffset = byteIndex & 3;
        final int shift = (nibbleIndex & 1) << 2;

        byte[] array = concurrentData.get();
        while (true) {
            int currentInt = U.getInt(array, BYTE_ARRAY_BASE_OFFSET + intIndex);
            byte currentByte = (byte) (currentInt >> (internalOffset << 3));
            byte newByte = (byte) ((currentByte & ~(0xF << shift)) | ((value & 0xF) << shift));

            int newInt = currentInt & ~(0xFF << (internalOffset << 3)) | ((newByte & 0xFF) << (internalOffset << 3));

            if (U.compareAndSwapInt(array, BYTE_ARRAY_BASE_OFFSET + intIndex, currentInt, newInt)) {
                U.storeFence();
                return;
            }
        }
    }

    @VisibleForTesting
    public int incrementAndGet(int x, int y, int z) {
        final int nibbleIndex = y << this.depthBitsPlusFour | z << this.depthBits | x;
        final int byteIndex = nibbleIndex >> 1;
        final int intIndex = byteIndex & ~3;
        final int internalOffset = byteIndex & 3;
        final int shift = (nibbleIndex & 1) << 2;

        byte[] array = concurrentData.get();
        while (true) {
            int currentInt = U.getInt(array, BYTE_ARRAY_BASE_OFFSET + intIndex);
            byte currentByte = (byte) (currentInt >> (internalOffset << 3));
            int oldNibble = (currentByte >> shift) & 0xF;
            int newNibble = (oldNibble + 1) & 0xF;

            byte newByte = (byte) ((currentByte & ~(0xF << shift)) | (newNibble << shift));
            int newInt = currentInt & ~(0xFF << (internalOffset << 3)) | ((newByte & 0xFF) << (internalOffset << 3));

            if (U.compareAndSwapInt(array, BYTE_ARRAY_BASE_OFFSET + intIndex, currentInt, newInt)) {
                U.storeFence();
                return newNibble;
            }
        }
    }
}
