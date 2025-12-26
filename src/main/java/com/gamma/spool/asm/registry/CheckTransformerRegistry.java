package com.gamma.spool.asm.registry;

import java.util.List;

import com.gamma.spool.asm.checks.UnsafeIterationHandler;
import com.gamma.spool.asm.interfaces.ICheckTransformer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CheckTransformerRegistry {

    private static final List<ICheckTransformer> transformers;

    static {
        transformers = ObjectArrayList.of(new UnsafeIterationHandler());
    }

    public static List<ICheckTransformer> getRegistered() {
        return transformers;
    }

    public static boolean inRegistry(ICheckTransformer obj) {
        return transformers.contains(obj);
    }

    public static int length() {
        return transformers.size();
    }
}
