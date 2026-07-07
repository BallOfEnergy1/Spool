package com.gamma.spool.compat.chunkapi;

import java.lang.reflect.Field;

import com.gamma.spool.core.SpoolLogger;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

/**
 * Mixin patcher for ChunkAPI compatibility (older versions which had explicit Spool compatibility).
 *
 * @author Mika Hensel (malteeez), BallOfEnergy01
 */
public class ChunkAPIMixinReflectionPatcher {

    private static Class<?> mixinsClass;

    public static void init() {
        try {
            Class.forName("com.falsepattern.chunk.internal.core.CoreLoadingPlugin");
        } catch (ClassNotFoundException ignored) {
            return;
        }

        try {
            initialize();
            if (applyPatches()) SpoolLogger.info("Successfully patched ChunkAPI mixins.");
        } catch (Exception e) {
            System.out.println("Failed to patch ChunkAPI mixins.");
        }
    }

    private static void initialize() throws Exception {
        ClassLoader loader = ChunkAPIMixinReflectionPatcher.class.getClassLoader();
        // Initialize the Mixins enum (creates all constants)
        mixinsClass = Class.forName("com.falsepattern.chunk.internal.mixin.plugin.Mixin", true, loader);
    }

    private static boolean applyPatches() throws Exception {

        // Check to see if the version is already new enough to where we don't have to make this patch.
        try {
            // noinspection unchecked,rawtypes
            Enum.valueOf((Class<Enum>) mixinsClass, "Core_NoSpool");
        } catch (Exception ignored) {
            return false;
        }

        // Get the enum entry for this mixin target
        @SuppressWarnings({ "rawtypes", "unchecked" })
        Object mixin = Enum.valueOf((Class<Enum>) mixinsClass, "CommonCore");

        // Allow accessing the builder field for the enum entries
        Field builderField = mixinsClass.getDeclaredField("builder");
        builderField.setAccessible(true);
        MixinBuilder builder = (MixinBuilder) builderField.get(mixin);

        builder.addCommonMixins("common.base.AnvilChunkLoaderMixin");
        builder.addClientMixins("client.vanilla.ChunkMixin");

        return true;
    }
}
