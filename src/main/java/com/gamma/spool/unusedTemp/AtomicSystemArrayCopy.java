package com.gamma.spool.unusedTemp;

/**
 * A really cool function me and another guy made.
 * Quite sad that it never got used, so I want to save it here.
 * 
 * @author khaddra
 */
public class AtomicSystemArrayCopy {
    // public void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {
    // if (!(src instanceof byte[] srcArray)) {
    // throw new ArrayStoreException("Source is not a byte array");
    // }
    //
    // byte[] currentArray = concurrentData.get();
    //
    // int alignedStart = (destPos + 3) & ~3;
    // int alignedEnd = (destPos + length) & ~3;
    // int alignedLength = alignedEnd - alignedStart;
    //
    // int remainingStart = alignedStart - destPos;
    // int remainingEnd = (destPos + length) - alignedEnd;
    //
    // // special case
    // if (alignedLength < 0) {
    // int mask = ((1 << ((4 - remainingStart) * 8)) - 1) | -(1 << (remainingEnd * 8));
    // long offset = BYTE_ARRAY_BASE_OFFSET + (alignedStart - 4);
    // while (true) {
    // int oldDest = U.getInt(currentArray, offset);
    // byte[] srcValues = new byte[4];
    // // hacky method that should work with any source array
    // System.arraycopy(srcArray, srcPos, srcValues, 4 - remainingStart, length);
    // int newDest = (U.getInt(srcValues, BYTE_ARRAY_BASE_OFFSET) & ~mask) | (oldDest & mask);
    // if (U.compareAndSwapInt(currentArray, offset, oldDest, newDest)) {
    // break;
    // }
    // }
    // return;
    // }
    //
    // if (remainingStart != 0) {
    // int mask = ((1 << ((4 - remainingStart) * 8)) - 1);
    // long offset = BYTE_ARRAY_BASE_OFFSET + (alignedStart - 4);
    // while (true) {
    // int oldDest = U.getInt(currentArray, offset);
    // byte[] srcValues = new byte[4];
    // // hacky method that should work with any source array
    // System.arraycopy(srcArray, srcPos, srcValues, 4 - remainingStart, remainingStart);
    // int newDest = (U.getInt(srcValues, BYTE_ARRAY_BASE_OFFSET) & ~mask) | (oldDest & mask);
    // if (U.compareAndSwapInt(currentArray, offset, oldDest, newDest)) {
    // break;
    // }
    // }
    // }
    //
    // if (alignedLength != 0) {
    // System.arraycopy(srcArray, srcPos + remainingStart, currentArray, alignedStart, alignedLength);
    // }
    //
    // if (remainingEnd != 0) {
    // int mask = -(1 << (remainingEnd * 8));
    // long offset = BYTE_ARRAY_BASE_OFFSET + alignedEnd;
    // while (true) {
    // int oldDest = U.getInt(currentArray, offset);
    // byte[] srcValues = new byte[4];
    // // hacky method that should work with any source array
    // System.arraycopy(srcArray, srcPos + length - remainingEnd, srcValues, 0, remainingEnd);
    // int newDest = ((U.getInt(srcValues, BYTE_ARRAY_BASE_OFFSET) & ~mask) | (oldDest & mask));
    // if (U.compareAndSwapInt(currentArray, offset, oldDest, newDest)) {
    // break;
    // }
    // }
    // }
    // }
}
