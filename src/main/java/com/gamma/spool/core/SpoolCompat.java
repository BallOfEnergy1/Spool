package com.gamma.spool.core;

import com.gamma.spool.api.annotations.SkipSpoolASMChecks;
import com.gamma.spool.config.CompatConfig;
import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.db.SpoolDBManager;

import cpw.mods.fml.common.Loader;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

@SkipSpoolASMChecks(SkipSpoolASMChecks.SpoolASMCheck.ALL)
public class SpoolCompat {

    public static final ObjectOpenHashSet<Mod> compatSet = new ObjectOpenHashSet<>();

    public static boolean isObfuscated;

    public static boolean isEarlyCompatReady = false;
    public static boolean isCompatReady = false;

    /**
     * Early initialization occurs at the beginning of the coremod initialization.
     */
    public static void earlyInitialization() {
        if (isEarlyCompatReady) return;

        SpoolLogger.compatInfo("Loading early compat...");

        if (CompatConfig.forceLoadedModIDs.length != 0) {
            SpoolLogger.compatInfo("Found {} force-loaded compat IDs:", CompatConfig.forceLoadedModIDs.length);
            for (int idx = 0; idx < CompatConfig.forceLoadedModIDs.length; idx++) {
                compatSet.add(new Mod(CompatConfig.forceLoadedModIDs[idx]));
            }
        }

        checkIsModLoadedFQCN("endlessids", "com.falsepattern.endlessids.asm.EndlessIDsCore");

        // Order matters here; NTM Space should be prioritized due to it being an FQCN-based compat.
        // checkIsModLoadedFQCN("hbm", SpecialModVersions.NTM_SPACE, "com.hbm.util.AstronomyUtil");

        SpoolLogger.compatInfo("Early compat loaded ({} mods)!", compatSet.size());

        if (!compatSet.isEmpty()) {
            SpoolLogger.compatInfo("Loaded early compat:");
            for (Mod mod : compatSet) SpoolLogger.compatInfo("  - " + mod.toString());
        }

        logChange("COMPAT", "Mod compat lifecycle", String.format("Early compat loaded: %s", compatSet));

        isEarlyCompatReady = true;
    }

    /**
     * Happens during mod initialization (phase-2), after mod construction.
     * Mod classes are safe to use here, just be careful with cyclic dependencies since mixins use
     * these compats.
     */
    public static void checkLoadedMods() {
        if (isCompatReady) return;
        SpoolLogger.compatInfo("Loading compat...");

        checkIsModLoaded("hodgepodge");

        checkIsModLoaded("chunkapi");

        if (!isModLoaded("hbm", SpecialModVersions.NTM_SPACE)) checkIsModLoaded("hbm");

        isObfuscated = SpoolCoreMod.isObfuscatedEnv;

        SpoolLogger.compatInfo("Loaded compat ({} mods)!", compatSet.size());

        if (!compatSet.isEmpty()) {
            SpoolLogger.compatInfo("Loaded compat:");
            for (Mod mod : compatSet) SpoolLogger.compatInfo("  - " + mod.toString());
        }

        logChange("COMPAT", "Mod compat lifecycle", String.format("Standard compat loaded: %s", compatSet));

        isCompatReady = true;
    }

    private static void checkIsModLoadedFQCN(String modID, String fqcn) {
        if (!CompatConfig.enableFQCNChecks) return;
        try {
            SpoolLogger.compatInfo("Checking for mod {} (presence of fqcn {}).", modID.toLowerCase(), fqcn);
            // Disallow initialization of the class.
            Class.forName(
                fqcn,
                false,
                Thread.currentThread()
                    .getContextClassLoader());
            compatSet.add(new Mod(modID.toLowerCase()));
        } catch (Throwable ignored) {}
    }

    private static void checkIsModLoadedFQCN(String modID, SpecialModVersions ver, String fqcn) {
        if (!CompatConfig.enableFQCNChecks) return;
        try {
            SpoolLogger
                .compatInfo("Checking for mod {} ({}) (presence of fqcn {}).", modID.toLowerCase(), ver.name(), fqcn);
            // Disallow initialization of the class.
            Class.forName(
                fqcn,
                false,
                Thread.currentThread()
                    .getContextClassLoader());
            compatSet.add(new Mod(modID.toLowerCase(), ver));
        } catch (Throwable ignored) {}
    }

    private static void checkIsModLoaded(String modID) {
        if (Loader.isModLoaded(modID.toLowerCase())) compatSet.add(new Mod(modID.toLowerCase()));
    }

    private static void checkIsModLoaded(String modID, SpecialModVersions ver) {
        if (Loader.isModLoaded(modID.toLowerCase())) compatSet.add(new Mod(modID.toLowerCase(), ver));
    }

    public static boolean isModLoaded(String modID) {
        for (Mod mod : compatSet) if (mod.modID.equals(modID.toLowerCase()) && mod.ver == null) return true;
        return false;
    }

    public static boolean isModLoaded(String modID, SpecialModVersions ver) {
        for (Mod mod : compatSet) if (mod.modID.equals(modID.toLowerCase()) && mod.ver.equals(ver)) return true;
        return false;
    }

    public static class Mod {

        String modID;
        SpecialModVersions ver;

        public Mod(String modID) {
            this(modID, null);
        }

        public Mod(String modID, SpecialModVersions ver) {
            this.modID = modID;
            this.ver = ver;
        }

        @Override
        public String toString() {
            return String.format("%s%s", this.modID, this.ver == null ? "" : (" (" + this.ver.name() + ")"));
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Mod other)) return false;

            if (modID.equals(other.modID)) {
                if (ver == null && other.ver == null) return true;
                else if (ver != null && other.ver != null) return ver.equals(other.ver);
            }
            return false;
        }
    }

    /**
     * Some mods, of course, use the same modID for their forks.
     * Spool's compat doesn't like this, so this entire system is in place to allow for differentiating
     * mods of the same modID.
     */
    public enum SpecialModVersions {
        NTM_SPACE
    }

    // More compat stuff, this is for logging all the adjusted fields and such.

    public static void logChange(String type, String target, String owner, String in, String newTarget,
        String newOwner) {
        if (!DebugConfig.fullCompatLogging) return;
        logToSDB(
            "ASM",
            "ASM " + type + " transformation",
            String.format("%s (owned by %s) in %s -> %s (owned by %s)", target, owner, in, newTarget, newOwner));
    }

    public static void logChange(String type, String cause, String description) {
        if (!DebugConfig.fullCompatLogging) return;
        logToSDB(type, cause, description);
    }

    public static void logChange(String mixinName, String targetClass, boolean post) {
        if (!DebugConfig.fullCompatLogging) return;
        logToSDB(
            "MIXIN",
            "Mixin lifecycle",
            String.format("%s mixin %s to class %s", (post ? "Applied" : "Applying"), mixinName, targetClass));
    }

    public static void logToSDB(String type, String cause, String desc) {
        SpoolDBManager.log(type, cause, desc);
    }
}
