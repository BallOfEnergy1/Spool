package com.gamma.spool.core;

import java.lang.instrument.Instrumentation;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.bytebuddy.agent.ByteBuddyAgent;

import com.gamma.gammalib.config.ImplConfig;
import com.gamma.gammalib.multi.MultiJavaUtil;
import com.gamma.gammalib.unsafe.UnsafeAccessor;
import com.gamma.spool.api.annotations.SkipSpoolASMChecks;
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

// die
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions({ "com.gamma.spool.watchdog.", "com.gamma.spool.asm." })
@SkipSpoolASMChecks(SkipSpoolASMChecks.SpoolASMCheck.ALL)
public class SpoolCoreMod implements IEarlyMixinLoader, IFMLLoadingPlugin {

    private static Instrumentation instrumentation;

    public static boolean isObfuscatedEnv;

    public static long getRecursiveObjectSize(Object o) {
        if (!OBJECT_DEBUG) return -1;
        if (o == null) return 0;
        long size = 0;

        if (o instanceof Collection<?>) {
            for (Object obj : ((Collection<?>) o)) {
                // This doesn't work great for objects containing lists;
                // it only works if the object itself is a list.
                size += getRecursiveObjectSize(obj);
            }
        } else if (o instanceof Map<?, ?>) {
            for (Map.Entry<?, ?> obj : ((Map<?, ?>) o).entrySet()) {
                // This doesn't work great for objects containing lists;
                // it only works if the object itself is a list.
                size += getRecursiveObjectSize(obj.getKey());
                size += getRecursiveObjectSize(obj.getValue());
            }
        }

        return size + instrumentation.getObjectSize(o); // include class overhead
    }

    public static final boolean OBJECT_DEBUG = false;

    static {
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

            boolean isUnsafeDeprecated = MultiJavaUtil.supportsVersion(23);
            if (ImplConfig.useUnsafe && !isUnsafeDeprecated) UnsafeAccessor.init();

            SpoolLogger.info("================== Available Java Features =================");
            SpoolLogger.info("\tDetected Java version: " + MultiJavaUtil.getVersion());
            SpoolLogger.info(
                "\tJava 8 Unsafe: Enabled: " + ImplConfig.useUnsafe + "; Available: " + UnsafeAccessor.IS_AVAILABLE);
            if (isUnsafeDeprecated) SpoolLogger.warn("\tJava Unsafe is deprecated as of Java 23 and will not be used!");
            SpoolLogger.info(
                "\tJava >= 9: Enabled: " + ImplConfig.useJava9Features
                    + "; Supported: "
                    + MultiJavaUtil.hasJava9Support());
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

            if (OBJECT_DEBUG) {
                // Debug code that allows us to dynamically load the instrumentation agent.
                // This should always be disabled unless you *really* **really** need it.
                SpoolLogger.warn("!!!Object debug enabled!!!");
                try {
                    instrumentation = ByteBuddyAgent.install();
                    SpoolLogger.warn("Successfully loaded instrumentation agent!");
                    SpoolLogger.warn(
                        "Instrumentation test: `new Object()` is " + instrumentation.getObjectSize(new Object())
                            + " bytes.");
                } catch (Exception ignored) {}
            }
        } catch (ConfigException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[] { "com.gamma.spool.asm.SpoolTransformerHandler" };
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
        if (MultiJavaUtil.supportsVersion(21)) return "mixins.spool_21.json";
        if (MultiJavaUtil.hasJava17Support()) return "mixins.spool_17.json";
        if (MultiJavaUtil.hasJava9Support()) return "mixins.spool_9.json";
        return "mixins.spool.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        return List.of();
    }
}
