package com.gamma.spool.compat.hodgepodge;

import java.lang.reflect.Field;

import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

public class MixinReflectionPatcher {

    private static Class<?> mixinsClass;

    public static void init() {
        try {
            Class.forName("com.mitchej123.hodgepodge.core.HodgepodgeCore");
        } catch (ClassNotFoundException ignored) {
            return;
        }
        try {
            initialize();
            applyPatches();
        } catch (Exception e) {
            System.out
                .println("Failed to patch hodgepodge mixins, will probably fail if config settings arent disabled.");
        }
    }

    private static void initialize() throws Exception {
        ClassLoader loader = MixinReflectionPatcher.class.getClassLoader();
        // Initialize the Mixins enum (creates all constants)
        mixinsClass = Class.forName("com.mitchej123.hodgepodge.mixins.Mixins", true, loader);
    }

    private static void applyPatches() throws Exception {
        // Create this mod as a ITargetMod via the TargetModBuilder from gtnhmixins
        TargetModBuilder targetedMod = new TargetModBuilder().setCoreModClass("com.gamma.spool.core.SpoolCoreMod")
            .setModId("spool");

        addExcludedMod("TILE_ENTITY_RENDERER_PROFILER", targetedMod);
        addExcludedMod("ADD_SIMULATION_DISTANCE_OPTION", targetedMod);
        addExcludedMod("ADD_SIMULATION_DISTANCE_OPTION_THERMOS_FIX", targetedMod);
        addExcludedMod("OPTIMIZE_TILEENTITY_REMOVAL", targetedMod);
        addExcludedMod("SPEEDUP_NBT_COPY", targetedMod);
    }

    @SuppressWarnings("unchecked")
    private static void addExcludedMod(String mixinName, Object targetedMod) throws Exception {
        // Get the enum entry for this mixin target
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
