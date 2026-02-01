package com.gamma.spool.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gamma.gammalib.config.ImplConfig;
import com.gamma.gammalib.multi.MultiJavaUtil;
import com.gamma.gammalib.unsafe.UnsafeAccessor;
import com.gamma.spool.api.annotations.SkipSpoolASMChecks;
import com.gamma.spool.asm.SpoolTransformerHandler;
import com.gamma.spool.compat.hodgepodge.MixinReflectionPatcher;
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

        loadMixinsFromFile();

        try {
            MixinReflectionPatcher.init();

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

        boolean isUnsafeDeprecated = MultiJavaUtil.supportsVersion(23);
        if (ImplConfig.useUnsafe && !isUnsafeDeprecated) UnsafeAccessor.init();

        SpoolLogger.info("================== Available Java Features =================");
        SpoolLogger.info("\tDetected Java version: " + MultiJavaUtil.getVersion());
        SpoolLogger
            .info("\tJava 8 Unsafe: Enabled: " + ImplConfig.useUnsafe + "; Available: " + UnsafeAccessor.IS_AVAILABLE);
        if (isUnsafeDeprecated) SpoolLogger.warn("\tJava Unsafe is deprecated as of Java 23 and will not be used!");
        SpoolLogger.info(
            "\tJava >= 9: Enabled: " + ImplConfig.useJava9Features + "; Supported: " + MultiJavaUtil.hasJava9Support());
        SpoolLogger.info(
            "\tJava >= 17: Enabled: " + ImplConfig.useJava17Features
                + "; Supported: "
                + MultiJavaUtil.hasJava17Support());
        SpoolLogger.info(
            "\tJava >= 25: Enabled: " + ImplConfig.useJava25Features
                + "; Supported: "
                + MultiJavaUtil.hasJava25Support());
        SpoolLogger.info("\tCompact impls: Enabled: " + ImplConfig.useCompactImpls);
        SpoolLogger.info("============================================================");

        SpoolDBManager.init();

        SpoolCompat.earlyInitialization();

        SpoolCompat.logChange("STAGE", "Mod lifecycle", "COREMOD");
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
        isObfuscatedEnv = !(boolean) data.get("runtimeDeobfuscationEnabled");
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
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
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
