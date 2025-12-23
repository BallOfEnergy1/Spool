package com.gamma.spool.util.fast;

public class Java8Util {

    public static boolean hasVarHandleSupport() {
        // TODO: Sort out this stupid thing.
        // I would love to have Java 11 VarHandle work, but I can't seem to get it to work without building with
        // >8 JDK, which breaks Spool's compatibility. VarHandle's methods are natives, which breaks shit.

        // try {
        // Class.forName("java.lang.invoke.VarHandle");
        // return true;
        // } catch (Throwable e) {
        // return false;
        // }
        return false;
    }
}
