package com.gamma.spool.compat.angelica;

import java.lang.reflect.Field;
import java.util.List;

import com.gamma.spool.core.SpoolLogger;
import com.gtnewhorizon.gtnhmixins.builders.AbstractBuilder;
import com.gtnewhorizons.angelica.mixins.Mixins;

/**
 * Mixin patcher for Angelica compatibility.
 *
 * @author BallOfEnergy01, Mika Hensel (malteeez)
 */
public class AngelicaMixinReflectionPatcher {

    private static Class<?> mixinsClass;

    public static void init() {
        try {
            Class.forName("com.gtnewhorizons.angelica.AngelicaMod");
        } catch (ClassNotFoundException ignored) {
            return;
        }
        try {
            initialize();
            applyPatches();
        } catch (Exception e) {
            SpoolLogger
                .error("Failed to patch angelica mixins, will probably fail if config settings arent disabled.", e);
        }
    }

    private static void initialize() throws Exception {
        ClassLoader loader = AngelicaMixinReflectionPatcher.class.getClassLoader();
        // Initialize the Mixins enum (creates all constants)
        mixinsClass = Class.forName("com.gtnewhorizons.angelica.mixins.Mixins", true, loader);
    }

    private static void applyPatches() throws Exception {

        // Get the enum entry for this mixin target
        Object mixin = Enum.valueOf((Class<Enum>) mixinsClass, "CELERITAS");

        Field clientClassesField = AbstractBuilder.class.getDeclaredField("clientClasses");
        clientClassesField.setAccessible(true);
        List<String> clientMixins = (List<String>) clientClassesField.get(((Mixins) (mixin)).getBuilder());

        if (clientMixins.remove("celeritas.terrain.MixinChunk"))
            SpoolLogger.info("Successfully patched Celeritas mixins.");
        else SpoolLogger.error("Failed to patch Celeritas mixins, things may not work as expected!");
    }
}
