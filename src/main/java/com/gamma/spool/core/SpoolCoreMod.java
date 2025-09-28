package com.gamma.spool.core;

import java.lang.instrument.Instrumentation;
import java.util.Collection;
import java.util.Map;

import net.bytebuddy.agent.ByteBuddyAgent;

import com.gamma.spool.config.APIConfig;
import com.gamma.spool.config.ConcurrentConfig;
import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.config.DistanceThreadingConfig;
import com.gamma.spool.config.ThreadManagerConfig;
import com.gamma.spool.config.ThreadsConfig;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

// die
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions({ "com.gamma.spool.watchdog.", "com.gamma.spool.asm." })
public class SpoolCoreMod implements IFMLLoadingPlugin {

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
            ConfigurationManager.registerConfig(DebugConfig.class);
            ConfigurationManager.registerConfig(ThreadsConfig.class);
            ConfigurationManager.registerConfig(ThreadManagerConfig.class);
            ConfigurationManager.registerConfig(DistanceThreadingConfig.class);
            ConfigurationManager.registerConfig(ConcurrentConfig.class);
            ConfigurationManager.registerConfig(APIConfig.class);

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
}
