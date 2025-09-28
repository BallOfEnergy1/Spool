package com.gamma.spool.core;

import cpw.mods.fml.common.Loader;

public class SpoolCompat {

    public static boolean isHodgepodgeLoaded;

    public static boolean isChunkAPILoaded;
    public static boolean isEndlessIDsLoaded;

    public static boolean isHBMLoaded;

    public static boolean isObfuscated;

    public static boolean isCompatReady = false;

    public static int numLoadedCompats;

    public static void checkLoadedMods() {
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
