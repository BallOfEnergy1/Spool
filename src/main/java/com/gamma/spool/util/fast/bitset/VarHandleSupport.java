package com.gamma.spool.util.fast.bitset;

import static com.gamma.spool.util.fast.bitset.AtomicBitSet9.Cell;

import java.lang.reflect.Method;

final class VarHandleSupport {

    static final boolean AVAILABLE;
    static final Object VALUE_HANDLE;

    static final Method GET_OPAQUE;
    static final Method SET_RELEASE;
    static final Method COMPARE_AND_SET;

    static {
        Object vh = null;
        boolean ok = false;
        Method getOpaque = null;
        Method setRelease = null;
        Method compareAndSet = null;
        try {
            Class<?> vhClass = Class.forName("java.lang.invoke.VarHandle");
            Class<?> mhClass = Class.forName("java.lang.invoke.MethodHandles");
            Object lookup = mhClass.getMethod("lookup")
                .invoke(null);
            vh = lookup.getClass()
                .getMethod("findVarHandle", Class.class, String.class, Class.class)
                .invoke(lookup, Cell.class, "value", int.class);

            getOpaque = vhClass.getMethod("getOpaque", Object[].class);
            setRelease = vhClass.getMethod("setRelease", Object[].class);
            compareAndSet = vhClass.getMethod("compareAndSet", Object[].class);

            System.out.println("VarHandle initialized successfully!");
            ok = true;
        } catch (Throwable t) {
            t.printStackTrace();
        }

        VALUE_HANDLE = vh;
        AVAILABLE = ok;
        GET_OPAQUE = getOpaque;
        SET_RELEASE = setRelease;
        COMPARE_AND_SET = compareAndSet;
    }

    static int getOpaque(Cell c) {
        try {
            return (int) GET_OPAQUE.invoke(VALUE_HANDLE, new Object[] { c });
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static void setRelease(Cell c, int v) {
        try {
            Object[] methodArgs = new Object[1];
            methodArgs[0] = new Object[] { c, v };
            SET_RELEASE.invoke(VALUE_HANDLE, methodArgs);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static boolean compareAndSet(Cell c, int expected, int newValue) {
        try {
            return (boolean) COMPARE_AND_SET.invoke(VALUE_HANDLE, new Object[] { c, expected, newValue });
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }
}
