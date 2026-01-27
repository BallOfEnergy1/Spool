package com.gamma.spool.asm;

import com.gamma.gammalib.asm.ASMRegistry;
import com.gamma.spool.api.annotations.SkipSpoolASMChecks;
import com.gamma.spool.asm.checks.UnsafeIterationHandler;
import com.gamma.spool.asm.transformers.AtomicNibbleArrayTransformer;
import com.gamma.spool.asm.transformers.ConcurrentAnvilChunkLoaderTransformer;
import com.gamma.spool.asm.transformers.ConcurrentChunkProviderTransformer;
import com.gamma.spool.asm.transformers.ConcurrentChunkTransformer;
import com.gamma.spool.asm.transformers.ConcurrentExtendedBlockStorageTransformer;
import com.gamma.spool.asm.transformers.EmptyChunkTransformer;
import com.gamma.spool.core.Spool;

@SuppressWarnings("unused")
@SkipSpoolASMChecks(SkipSpoolASMChecks.SpoolASMCheck.ALL)
public class SpoolTransformerHandler {

    public void register() {
        ASMRegistry.register(Spool.MODID, new AtomicNibbleArrayTransformer());
        ASMRegistry.register(Spool.MODID, new ConcurrentAnvilChunkLoaderTransformer());
        ASMRegistry.register(Spool.MODID, new ConcurrentChunkProviderTransformer());
        ASMRegistry.register(Spool.MODID, new ConcurrentChunkTransformer());
        ASMRegistry.register(Spool.MODID, new ConcurrentExtendedBlockStorageTransformer());
        ASMRegistry.register(Spool.MODID, new EmptyChunkTransformer());

        ASMRegistry.registerCheck(Spool.MODID, new UnsafeIterationHandler());
    }
}
