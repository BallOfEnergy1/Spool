package com.gamma.spool.core;

import cpw.mods.fml.common.Loader;

public class SpoolCompat {

    public static boolean isHodgepodgeLoaded;

    public static boolean isChunkAPILoaded;
    public static boolean isEndlessIDsLoaded;

    public static boolean isHBMLoaded;

    public static boolean isObfuscated;

    public static boolean isEarlyCompatReady = false;
    public static boolean isCompatReady = false;

    public static int numLoadedCompats;

    public static void earlyInitialization() {
        if (isEarlyCompatReady) return;

        SpoolLogger.compatInfo("Loading early compat...");

        try {
            Class.forName("com.falsepattern.endlessids.asm.EndlessIDsCore");
            isEndlessIDsLoaded = true;
        } catch (Throwable e) {
            isEndlessIDsLoaded = false;
        }

        SpoolLogger.compatInfo("Early compat loaded!");

        isEarlyCompatReady = true;
    }

    public static void checkLoadedMods() {
        if (isCompatReady) return;
        SpoolLogger.compatInfo("Loading compat...");

        isHodgepodgeLoaded = checkIsModLoaded("hodgepodge");

        isChunkAPILoaded = checkIsModLoaded("chunkapi");
        isEndlessIDsLoaded = checkIsModLoaded("endlessids");

        isHBMLoaded = checkIsModLoaded("hbm");

        isObfuscated = SpoolCoreMod.isObfuscatedEnv;

        SpoolLogger.compatInfo("Loaded compat ({} mods)!", numLoadedCompats);
        isCompatReady = true;
    }

    private static boolean checkIsModLoaded(String... modIDs) {
        for (String modID : modIDs) {
            if (Loader.isModLoaded(modID)) {
                numLoadedCompats++;
                return true;
            }
        }
        return false;
    }
}
