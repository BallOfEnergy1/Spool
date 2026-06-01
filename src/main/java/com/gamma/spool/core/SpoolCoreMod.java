package com.gamma.spool.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.launchwrapper.Launch;

import com.gamma.gammalib.config.ImplConfig;
import com.gamma.gammalib.multi.MultiJavaUtil;
import com.gamma.spool.api.annotations.SkipSpoolASMChecks;
import com.gamma.spool.asm.SpoolNames;
import com.gamma.spool.asm.SpoolTransformerHandler;
import com.gamma.spool.compat.angelica.AngelicaMixinReflectionPatcher;
import com.gamma.spool.compat.hodgepodge.HodgepodgeMixinReflectionPatcher;
import com.gamma.spool.config.APIConfig;
import com.gamma.spool.config.CompatConfig;
import com.gamma.spool.config.ConcurrentConfig;
import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.config.DistanceThreadingConfig;
import com.gamma.spool.config.ThreadManagerConfig;
import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.db.SpoolDBManager;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import com.hbm.config.GeneralConfig;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

// die
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions({ "com.gamma.spool.watchdog.", "com.gamma.spool.asm.",
    "com.gtnewhorizon.gtnhlib.asm", "it.unimi.dsi.fastutil" })
@SkipSpoolASMChecks(SkipSpoolASMChecks.SpoolASMCheck.ALL)
public class SpoolCoreMod implements IEarlyMixinLoader, IFMLLoadingPlugin {

    public static boolean isObfuscatedEnv;

    public static final ObjectList<String> earlyMixins = new ObjectArrayList<>();
    public static final ObjectList<String> lateMixins = new ObjectArrayList<>();

    static {
        // Load runtime mixin file.
        loadMixinsFromFile();

        try {
            HodgepodgeMixinReflectionPatcher.init();
            AngelicaMixinReflectionPatcher.init();

            ConfigurationManager.registerConfig(DebugConfig.class);
            ConfigurationManager.registerConfig(CompatConfig.class);
            ConfigurationManager.registerConfig(ImplConfig.class);
            ConfigurationManager.registerConfig(ThreadsConfig.class);
            ConfigurationManager.registerConfig(ThreadManagerConfig.class);
            ConfigurationManager.registerConfig(DistanceThreadingConfig.class);
            ConfigurationManager.registerConfig(ConcurrentConfig.class);
            ConfigurationManager.registerConfig(APIConfig.class);
        } catch (ConfigException e) {
            throw new RuntimeException(e);
        }

        SpoolTransformerHandler.INSTANCE.register();

        SpoolDBManager.init();

        SpoolCompat.earlyInitialization();

        // start diggin' in yo mods twin
        SpoolLogger.info("Fixing incompatible mods (early)...");
        fixOtherModsEarly();

        SpoolCompat.logChange("STAGE", "Mod lifecycle", "COREMOD");
    }

    // :ayo:
    private static void fixOtherModsEarly() {
        SpoolCompat.logChange("STAGE", "Mod fix (early) lifecycle", "BEGIN FIXES");

        // Endless IDs chunk provider super patcher is critically incompatible with Spool's replacement chunk classes.
        // This adds Spool's classes to a special interop field in the blackboard for EID to use in its patcher.
        if (SpoolCompat.isModLoadedFast(SpoolCompat.CompatibleMods.ENDLESS_IDS)) {
            SpoolCompat.logChange("STAGE", "Mod fix (early) lifecycle", "Fix Endless IDs super patcher");
            Launch.blackboard.put(
                "endlessids_spool_CLASS_Chunk_interop",
                new String[] { SpoolNames.Destinations.CONCURRENT_CHUNK,
                    SpoolNames.Destinations.CONCURRENT_CHUNK_EID });
        }

        SpoolCompat.logChange("STAGE", "Mod fix (early) lifecycle", "END FIXES");
    }

    // :ayo: x2
    static void fixOtherModsLate() {
        SpoolCompat.logChange("STAGE", "Mod fix (late) lifecycle", "BEGIN FIXES");

        // HBM's NTM packet threading is critically incompatible with Spool.
        if (SpoolCompat.isModLoadedFast(SpoolCompat.CompatibleMods.HBM)) {
            // Irrelevant due to Spool's changes.
            SpoolCompat.logChange("STAGE", "Mod fix (late) lifecycle", "Fix NTM packet threading");
            GeneralConfig.enablePacketThreading = false;
        }

        SpoolCompat.logChange("STAGE", "Mod fix (late) lifecycle", "END FIXES");
    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        isObfuscatedEnv = (boolean) data.get("runtimeDeobfuscationEnabled");
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public String getMixinConfig() {
        if (MultiJavaUtil.supportsVersion(21)) return "mixins.spool_21.early.json";
        if (MultiJavaUtil.hasJava17Support()) return "mixins.spool_17.early.json";
        if (MultiJavaUtil.hasJava9Support()) return "mixins.spool_9.early.json";
        return "mixins.spool.early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        return earlyMixins;
    }

    private static void loadMixinsFromFile() {
        InputStream is = Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream("META-INF/mixins.txt");
        if (is == null) {
            throw new IllegalStateException("Could not load mixins.txt, unable to launch.");
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            boolean early = true;
            while (true) {
                br.mark(1);
                if (br.read() == -1) break;
                br.reset();
                String line = br.readLine();
                if (line.contains("|")) {
                    early = false;
                    continue;
                }
                if (early) earlyMixins.add(line);
                else lateMixins.add(line);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read mixins.txt", e);
        }
    }
}
