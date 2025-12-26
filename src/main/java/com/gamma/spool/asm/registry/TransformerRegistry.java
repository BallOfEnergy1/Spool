package com.gamma.spool.asm.registry;

import java.util.List;

import com.gamma.spool.asm.interfaces.ITransformer;
import com.gamma.spool.asm.transformers.AtomicNibbleArrayTransformer;
import com.gamma.spool.asm.transformers.ConcurrentAnvilChunkLoaderTransformer;
import com.gamma.spool.asm.transformers.ConcurrentChunkProviderTransformer;
import com.gamma.spool.asm.transformers.ConcurrentChunkTransformer;
import com.gamma.spool.asm.transformers.ConcurrentExtendedBlockStorageTransformer;
import com.gamma.spool.asm.transformers.EmptyChunkTransformer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TransformerRegistry {

    private static final List<ITransformer> transformers;

    static {
        transformers = ObjectArrayList.of(
            new AtomicNibbleArrayTransformer(),
            new ConcurrentAnvilChunkLoaderTransformer(),
            new ConcurrentChunkProviderTransformer(),
            new ConcurrentChunkTransformer(),
            new ConcurrentExtendedBlockStorageTransformer(),
            new EmptyChunkTransformer());
    }

    public static List<ITransformer> getRegistered() {
        return transformers;
    }

    public static boolean inRegistry(ITransformer obj) {
        return transformers.contains(obj);
    }

    public static int length() {
        return transformers.size();
    }
}
