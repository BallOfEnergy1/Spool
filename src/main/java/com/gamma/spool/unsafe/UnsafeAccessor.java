package com.gamma.spool.unsafe;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

// Forgive me.
// I want the performance.
public class UnsafeAccessor {

    public static boolean ENABLED = false;
    public static boolean IS_AVAILABLE = false;

    private static Unsafe U = null;

    public static void init() {
        enableUnsafe();
        getUnsafe();
    }

    public static Unsafe getUnsafe() {
        if (ENABLED && !IS_AVAILABLE) {
            try {
                Field field = Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                U = (Unsafe) field.get(null);
                UnsafeConstants.BYTE_ARRAY_BASE_OFFSET = U.arrayBaseOffset(byte[].class);
                UnsafeConstants.SHORT_ARRAY_BASE_OFFSET = U.arrayBaseOffset(short[].class);
                UnsafeConstants.INT_ARRAY_BASE_OFFSET = U.arrayBaseOffset(int[].class);
                UnsafeConstants.LONG_ARRAY_BASE_OFFSET = U.arrayBaseOffset(long[].class);
                UnsafeConstants.SHORT_ARRAY_INDEX_SCALE = U.arrayIndexScale(short[].class);
                UnsafeConstants.INT_ARRAY_INDEX_SCALE = U.arrayIndexScale(int[].class);
                UnsafeConstants.LONG_ARRAY_INDEX_SCALE = U.arrayIndexScale(long[].class);
                IS_AVAILABLE = true;
            } catch (Exception e) {
                throw new IllegalStateException(
                    "Unsafe is not available: " + e.getClass()
                        .getName());
            }
        }

        return U;
    }

    public static void disableUnsafe() {
        ENABLED = false;
        if (IS_AVAILABLE) U = null;
        IS_AVAILABLE = false;
    }

    public static void enableUnsafe() {
        ENABLED = true;
    }
}
