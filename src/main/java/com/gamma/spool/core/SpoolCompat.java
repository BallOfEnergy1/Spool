package com.gamma.spool.core;

import java.util.EnumSet;

import com.gamma.spool.api.annotations.SkipSpoolASMChecks;
import com.gamma.spool.config.CompatConfig;
import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.db.SpoolDBManager;

import cpw.mods.fml.common.Loader;

@SkipSpoolASMChecks(SkipSpoolASMChecks.SpoolASMCheck.ALL)
public class SpoolCompat {

    public enum CompatibleMods {

        ENDLESS_IDS("endlessids", "com.falsepattern.endlessids.asm.EndlessIDsCore"),
        CHUNK_API("chunkapi", "com.falsepattern.chunk.internal.ChunkAPI"),
        ANGELICA("angelica", "com.gtnewhorizons.angelica.AngelicaMod"),
        HODGEPODGE("hodgepodge", "com.mitchej123.hodgepodge.core.HodgepodgeCore"),
        SUPERNOVA("supernova", "com.mitchej123.supernova.core.SupernovaCore"),
        ARCHAICFIX("archaicfix", "org.embeddedt.archaicfix.ArchaicCore"),
        HBM("hbm", "com.hbm.main.MainRegistry", true);

        public final String modID;
        public final String FQCN;
        public final boolean noCoremod;

        CompatibleMods(String modID) {
            this(modID, null, false);
        }

        CompatibleMods(String modID, String FQCN) {
            this(modID, FQCN, false);
        }

        CompatibleMods(String modID, String FQCN, boolean noCoremod) {
            this.modID = modID;
            this.FQCN = FQCN;
            this.noCoremod = noCoremod;
        }

        public static CompatibleMods findMod(String modID) {
            for (CompatibleMods mod : values()) {
                if (mod.modID.equals(modID)) {
                    return mod;
                }
            }
            return null;
        }

        public boolean isEarly() {
            return FQCN != null;
        }

        @Override
        public String toString() {
            return String.format("%s%s", modID, isEarly() ? " (early; FQCN: " + FQCN + ")" : "");
        }
    }

    public static final EnumSet<CompatibleMods> compatSet = EnumSet.noneOf(CompatibleMods.class);

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
                addMod(CompatConfig.forceLoadedModIDs[idx]);
            }
        }

        for (CompatibleMods mod : CompatibleMods.values()) {
            if (mod.isEarly()) checkIsModLoaded(mod);
        }

        SpoolLogger.compatInfo("Early compat loaded ({} mods)!", compatSet.size());

        if (!compatSet.isEmpty()) {
            SpoolLogger.compatInfo("Loaded early compat:");
            for (CompatibleMods mod : compatSet) SpoolLogger.compatInfo("  - " + mod.toString());
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

        // Yay, automated systems!
        for (String modID : Loader.instance()
            .getIndexedModList()
            .keySet()) {
            // Avoid duplicate mods in case we loaded something in early initialization.
            if (!isModLoaded(modID)) addMod(modID);
        }

        isObfuscated = SpoolCoreMod.isObfuscatedEnv;

        SpoolLogger.compatInfo("Loaded compat ({} mods)!", compatSet.size());

        if (!compatSet.isEmpty()) {
            SpoolLogger.compatInfo("Loaded compat:");
            for (CompatibleMods mod : compatSet) SpoolLogger.compatInfo("  - " + mod.toString());
        }

        logChange("COMPAT", "Mod compat lifecycle", String.format("Standard compat loaded: %s", compatSet));

        isCompatReady = true;
    }

    private static void checkIsModLoaded(CompatibleMods mod) {
        if (mod.isEarly()) {
            if (!CompatConfig.enableFQCNChecks) return;
            try {
                ClassLoader classLoader = Thread.currentThread()
                    .getContextClassLoader();
                if (mod.noCoremod) {
                    SpoolLogger.compatInfo(
                        "Checking for mod {} (presence of game-class fqcn {}).",
                        mod.modID.toLowerCase(),
                        mod.FQCN);
                    String resourcePath = mod.FQCN.replace('.', '/') + ".class";
                    // check if the resource exists without loading the class into the classloader at all
                    // we can't load it into the classloader at all since we'd be initializing a game-class (crash later
                    // on)
                    if (classLoader.getResource(resourcePath) != null) {
                        addMod(mod.modID.toLowerCase());
                    }
                } else {
                    SpoolLogger.compatInfo(
                        "Checking for mod {} (presence of coremod fqcn {}).",
                        mod.modID.toLowerCase(),
                        mod.FQCN);
                    // Disallow initialization of the class.
                    Class.forName(mod.FQCN, false, classLoader);
                    addMod(mod.modID.toLowerCase());
                }
            } catch (Throwable ignored) {}
        } else if (Loader.isModLoaded(mod.modID.toLowerCase()) || Loader.isModLoaded(mod.modID)) {
            addMod(mod.modID.toLowerCase());
        }
    }

    private static void addMod(String modID) {
        CompatibleMods mod = CompatibleMods.findMod(modID);
        if (mod != null) compatSet.add(mod);
    }

    /**
     * Quickly checks if a mod is loaded into Spool's compat store.
     * This requires that the mod is loaded into Spool's compat store manually
     * via the {@link CompatibleMods} enum.
     *
     * @param mod The mod enum to check for.
     * @return Whether the mod is loaded.
     */
    public static boolean isModLoadedFast(CompatibleMods mod) {
        return compatSet.contains(mod);
    }

    /**
     * Checks if a mod is loaded into Spool's compat store.
     *
     * @param modID The modID to check for.
     * @return Whether the mod is loaded.
     */
    public static boolean isModLoaded(String modID) {
        for (CompatibleMods mod : compatSet) if (mod.modID.equals(modID)) return true;
        return false;
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
