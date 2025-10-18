package com.gamma.spool.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import net.minecraft.launchwrapper.Launch;

import com.gamma.spool.config.DebugConfig;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import cpw.mods.fml.common.Loader;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

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

        checkIsModLoadedFQCN("endlessIDs", "com.falsepattern.endlessids.asm.EndlessIDsCore");

        // Order matters here; NTM Space should be prioritized due to it being an FQCN-based compat.
        checkIsModLoadedFQCN("hbm", SpecialModVersions.NTM_SPACE, "com.hbm.util.AstronomyUtil");

        SpoolLogger.compatInfo("Early compat loaded ({} mods)!", compatSet.size());

        if (!compatSet.isEmpty()) {
            SpoolLogger.compatInfo("Loaded early compat:");
            for (Mod mod : compatSet) SpoolLogger.compatInfo("  - " + mod.toString());
        }

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

        isCompatReady = true;
    }

    private static void checkIsModLoadedFQCN(String modID, String fqcn) {
        try {
            SpoolLogger.compatInfo("Checking for mod {} (presence of fqcn {}).", modID, fqcn);
            // Disallow initialization of the class.
            Class.forName(
                fqcn,
                false,
                Thread.currentThread()
                    .getContextClassLoader());
            compatSet.add(new Mod(modID));
        } catch (Throwable ignored) {}
    }

    private static void checkIsModLoadedFQCN(String modID, SpecialModVersions ver, String fqcn) {
        try {
            SpoolLogger.compatInfo("Checking for mod {} ({}) (presence of fqcn {}).", modID, ver.name(), fqcn);
            // Disallow initialization of the class.
            Class.forName(
                fqcn,
                false,
                Thread.currentThread()
                    .getContextClassLoader());
            compatSet.add(new Mod(modID, ver));
        } catch (Throwable ignored) {}
    }

    private static void checkIsModLoaded(String modID) {
        if (Loader.isModLoaded(modID)) compatSet.add(new Mod(modID));
    }

    private static void checkIsModLoaded(String modID, SpecialModVersions ver) {
        if (Loader.isModLoaded(modID)) compatSet.add(new Mod(modID, ver));
    }

    public static boolean isModLoaded(String modID) {
        for (Mod mod : compatSet) if (mod.modID.equals(modID) && mod.ver == null) return true;

        return false;
    }

    public static boolean isModLoaded(String modID, SpecialModVersions ver) {
        for (Mod mod : compatSet) if (mod.modID.equals(modID) && mod.ver.equals(ver)) return true;

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

    private static final ReentrantLock lock = new ReentrantLock(true);
    private static final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder().setNameFormat("Spool-IO")
            .build());

    private static final File outputFile;
    private static final Writer outputFileWriter;

    static {
        if (DebugConfig.fullCompatLogging) {
            outputFile = new File(Launch.minecraftHome, "spool/spool-compat.log");
            outputFile.getParentFile()
                .mkdirs();
            if (outputFile.exists()) outputFile.delete();
            try {
                outputFileWriter = new FileWriter(outputFile);
            } catch (IOException e) {
                throw new RuntimeException(e); // TODO: Make this more resilient.
            }
        } else {
            outputFile = null;
            outputFileWriter = null;
        }
    }

    public static void logChange(String type, String target, String owner, String in, String newTarget,
        String newOwner) {
        if (!DebugConfig.fullCompatLogging) return;
        String textOut = String.format(
            "%s - %s (owned by %s) in %s -> %s (owned by %s)\n",
            type.toUpperCase(),
            target,
            owner,
            in,
            newTarget,
            newOwner);
        logChange(textOut);
    }

    public static void logChange(String mixinName, String targetClass, boolean post) {
        if (!DebugConfig.fullCompatLogging) return;
        String textOut = String
            .format("MIXIN - %s mixin %s to class %s\n", (post ? "Applied" : "Applying"), mixinName, targetClass);
        logChange(textOut);
    }

    public static void logChange(String textOut) {
        if (!DebugConfig.fullCompatLogging) return;
        lock.lock();
        try {
            ioExecutor.execute(() -> {
                try {
                    outputFileWriter.append(textOut);
                    outputFileWriter.flush();
                } catch (IOException e) {
                    SpoolLogger.compatInfo("Cannot log compat debug!", e);
                }
            });
        } finally {
            lock.unlock();
        }
    }
}
