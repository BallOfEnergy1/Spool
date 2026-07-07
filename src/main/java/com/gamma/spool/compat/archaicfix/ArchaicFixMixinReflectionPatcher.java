package com.gamma.spool.compat.archaicfix;

import java.lang.reflect.Field;

import com.gamma.spool.core.SpoolLogger;
import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

/**
 * Mixin patcher for ArchaicFix compatibility.
 *
 * @author Mika Hensel (malteeez), BallOfEnergy01
 */
public class ArchaicFixMixinReflectionPatcher {

    private static Class<?> mixinsClass;

    public static void init() {
        try {
            Class.forName("org.embeddedt.archaicfix.ArchaicCore");
        } catch (ClassNotFoundException ignored) {
            return;
        }

        try {
            initialize();
            if (applyPatches()) SpoolLogger.info("Successfully patched ArchaicFix mixins.");
        } catch (Exception e) {
            System.out.println("Failed to patch ArchaicFix mixins.");
        }
    }

    private static void initialize() throws Exception {
        ClassLoader loader = ArchaicFixMixinReflectionPatcher.class.getClassLoader();
        // Initialize the Mixins enum (creates all constants)
        mixinsClass = Class.forName("org.embeddedt.archaicfix.asm.Mixin", true, loader);
    }

    private static boolean applyPatches() throws Exception {

        // Create this mod as a ITargetMod via the TargetModBuilder from gtnhmixins
        TargetModBuilder targetedMod = new TargetModBuilder().setCoreModClass("com.gamma.spool.core.SpoolCoreMod")
            .setModId("spool");

        addExcludedMod("WORLD_UPDATE_ENTITIES", targetedMod);
        // Entirely disable phosphor in favor of a more modern lighting engine such as LUMI.
        addExcludedMod("PHOSPHOR", targetedMod);
        addExcludedMod("PHOSPHOR_NO_CHUNKAPI", targetedMod);
        addExcludedMod("PHOSPHOR_FASTCRAFT", targetedMod);

        return true;
    }

    @SuppressWarnings("unchecked")
    private static void addExcludedMod(String mixinName, Object targetedMod) throws Exception {
        // Get the enum entry for this mixin target
        @SuppressWarnings("rawtypes")
        Object mixin = Enum.valueOf((Class<Enum>) mixinsClass, mixinName);

        // Allow accessing the builder field for the enum entries
        Field builderField = mixinsClass.getDeclaredField("builder");
        builderField.setAccessible(true);
        Object builder = builderField.get(mixin);

        // Just use the existing MixinBuilder for the entry from <clinit> and modify the excludedMods in AbstractBuilder
        // via the usual method.
        // In this case we do not care about the return value (the same reference for chaining)
        ((MixinBuilder) builder).addExcludedMod((ITargetMod) targetedMod);
    }
}
