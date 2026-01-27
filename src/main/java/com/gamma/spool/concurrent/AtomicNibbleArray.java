package com.gamma.spool.concurrent;

import static com.gamma.gammalib.unsafe.UnsafeConstants.BYTE_ARRAY_BASE_OFFSET;

import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.world.chunk.NibbleArray;

import com.gamma.gammalib.util.concurrent.IAtomic;
import com.google.common.annotations.VisibleForTesting;

import sun.misc.Unsafe;

@SuppressWarnings("unused")
public class AtomicNibbleArray extends NibbleArray implements IAtomic {

    public final AtomicReference<byte[]> concurrentData = new AtomicReference<>();

    private final Unsafe U = this.getUnsafe();

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

        if (isUnsafeAvailable()) {
            byte[] array = concurrentData.get();
            int value = U.getIntVolatile(array, BYTE_ARRAY_BASE_OFFSET + intIndex);
            byte targetByte = (byte) (value >> (internalOffset << 3));
            return (targetByte >> shift) & 0xF;
        } else {
            byte[] array = concurrentData.get();
            synchronized (array) {
                // Read the byte containing our nibble
                byte b = array[byteIndex];
                // Extract the nibble (either high or low 4 bits)
                return (b >> shift) & 0xF;
            }
        }
    }

    @Override
    public void set(int x, int y, int z, int value) {
        final int nibbleIndex = y << this.depthBitsPlusFour | z << this.depthBits | x;
        final int byteIndex = nibbleIndex >> 1;
        final int intIndex = byteIndex & ~3;
        final int internalOffset = byteIndex & 3;
        final int shift = (nibbleIndex & 1) << 2;

        if (isUnsafeAvailable()) {
            byte[] array = concurrentData.get();
            while (true) {
                int currentInt = U.getInt(array, BYTE_ARRAY_BASE_OFFSET + intIndex);
                byte currentByte = (byte) (currentInt >> (internalOffset << 3));
                byte newByte = (byte) ((currentByte & ~(0xF << shift)) | ((value & 0xF) << shift));

                int newInt = currentInt & ~(0xFF << (internalOffset << 3))
                    | ((newByte & 0xFF) << (internalOffset << 3));

                if (U.compareAndSwapInt(array, BYTE_ARRAY_BASE_OFFSET + intIndex, currentInt, newInt)) {
                    U.storeFence();
                    return;
                }
            }
        } else {
            byte[] array = concurrentData.get();
            synchronized (array) {
                // Get the current byte
                byte currentByte = array[byteIndex];
                // Clear the target nibble (either high or low 4 bits)
                byte clearedByte = (byte) (currentByte & ~(0xF << shift));
                // Set the new nibble value
                byte newByte = (byte) (clearedByte | ((value & 0xF) << shift));
                // Write it back
                array[byteIndex] = newByte;
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

        if (isUnsafeAvailable()) {
            byte[] array = concurrentData.get();
            while (true) {
                int currentInt = U.getInt(array, BYTE_ARRAY_BASE_OFFSET + intIndex);
                byte currentByte = (byte) (currentInt >> (internalOffset << 3));
                int oldNibble = (currentByte >> shift) & 0xF;
                int newNibble = (oldNibble + 1) & 0xF;

                byte newByte = (byte) ((currentByte & ~(0xF << shift)) | (newNibble << shift));
                int newInt = currentInt & ~(0xFF << (internalOffset << 3))
                    | ((newByte & 0xFF) << (internalOffset << 3));

                if (U.compareAndSwapInt(array, BYTE_ARRAY_BASE_OFFSET + intIndex, currentInt, newInt)) {
                    U.storeFence();
                    return newNibble;
                }
            }
        } else {
            byte[] array = concurrentData.get();
            synchronized (array) {
                // Get the current byte
                byte currentByte = array[byteIndex];
                // Extract the current nibble
                int oldNibble = (currentByte >> shift) & 0xF;
                // Calculate new nibble value (wrap around at 16)
                int newNibble = (oldNibble + 1) & 0xF;
                // Clear the old nibble
                byte clearedByte = (byte) (currentByte & ~(0xF << shift));
                // Set the new nibble value
                array[byteIndex] = (byte) (clearedByte | (newNibble << shift));
                // Return the new value
                return newNibble;
            }
        }
    }
}
