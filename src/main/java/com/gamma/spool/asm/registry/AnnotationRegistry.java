package com.gamma.spool.asm.registry;

import com.gamma.gammalib.asm.util.AnnotationDescriptor;
import com.gamma.spool.api.annotations.SkipSpoolASMChecks;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class AnnotationRegistry {

    private static final Object2ObjectOpenHashMap<String, AnnotationDescriptor> annotations = new Object2ObjectOpenHashMap<>();

    static {
        annotations.put(
            "com.hbm.blocks.generic.BlockGlyphidSpawner$TileEntityGlpyhidSpawner",
            new AnnotationDescriptor(
                SkipSpoolASMChecks.class,
                "value",
                SkipSpoolASMChecks.SpoolASMCheck.UNSAFE_ITERATION));
    }

    public static AnnotationDescriptor getValue(String key) {
        return annotations.get(key);
    }

    public static boolean inRegistry(String key) {
        return annotations.containsKey(key);
    }

    public static int length() {
        return annotations.size();
    }
}
