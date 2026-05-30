package com.gamma.spool.concurrent.async;

public class ImmediateUpdatesAsync {

    private static final ThreadLocal<Boolean> isImmediateUpdate = ThreadLocal.withInitial(() -> false);

    public static boolean isLocationImmediateUpdate() {
        return isImmediateUpdate.get();
    }

    public static void setImmediateUpdate(boolean value) {
        isImmediateUpdate.set(value);
    }
}
