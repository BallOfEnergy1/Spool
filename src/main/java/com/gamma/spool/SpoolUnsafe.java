package com.gamma.spool;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

// Forgive me.
// I want the performance.
public class SpoolUnsafe {

    public static boolean ENABLED = true; // TODO: config
    public static boolean IS_AVAILABLE = false;

    public static long BYTE_ARRAY_BASE_OFFSET;

    private static Unsafe U = null;

    public static Unsafe getUnsafe() {
        if (ENABLED && !IS_AVAILABLE) {
            try {
                Field f = Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                U = (Unsafe) f.get(null);
                BYTE_ARRAY_BASE_OFFSET = U.arrayBaseOffset(byte[].class);
                IS_AVAILABLE = true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (!ENABLED || !IS_AVAILABLE) {
            throw new IllegalStateException("Unsafe is not available!");
        }

        return U;
    }

    public static void disableUnsafe() {
        ENABLED = false;
        if (IS_AVAILABLE) U = null;
    }

    public static void enableUnsafe() {
        ENABLED = true;
    }
}
