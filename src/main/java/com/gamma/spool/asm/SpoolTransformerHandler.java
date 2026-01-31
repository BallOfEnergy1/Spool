package com.gamma.spool.asm;

import com.gamma.gammalib.asm.ASMRegistry;
import com.gamma.gammalib.asm.interfaces.IHook;
import com.gamma.spool.api.annotations.SkipSpoolASMChecks;
import com.gamma.spool.asm.checks.UnsafeIterationHandler;
import com.gamma.spool.asm.transformers.AtomicNibbleArrayTransformer;
import com.gamma.spool.asm.transformers.ConcurrentAnvilChunkLoaderTransformer;
import com.gamma.spool.asm.transformers.ConcurrentChunkProviderTransformer;
import com.gamma.spool.asm.transformers.ConcurrentChunkTransformer;
import com.gamma.spool.asm.transformers.ConcurrentExtendedBlockStorageTransformer;
import com.gamma.spool.asm.transformers.EmptyChunkTransformer;
import com.gamma.spool.config.ConcurrentConfig;
import com.gamma.spool.core.Spool;

@SkipSpoolASMChecks(SkipSpoolASMChecks.SpoolASMCheck.ALL)
public class SpoolTransformerHandler implements IHook {

    public static final SpoolTransformerHandler INSTANCE = new SpoolTransformerHandler();

    public void register() {
        ASMRegistry.register(Spool.MODID, new AtomicNibbleArrayTransformer());
        ASMRegistry.register(Spool.MODID, new ConcurrentAnvilChunkLoaderTransformer());
        ASMRegistry.register(Spool.MODID, new ConcurrentChunkProviderTransformer());
        ASMRegistry.register(Spool.MODID, new ConcurrentChunkTransformer());
        ASMRegistry.register(Spool.MODID, new ConcurrentExtendedBlockStorageTransformer());
        ASMRegistry.register(Spool.MODID, new EmptyChunkTransformer());

        ASMRegistry.registerCheck(Spool.MODID, new UnsafeIterationHandler());
        ASMRegistry.registerHook(this);
    }

    @Override
    public boolean beginTransform(String name, String transformedName, byte[] basicClass) {
        if (!ConcurrentConfig.enableConcurrentWorldAccess) {
            return false;
        }

        return !transformedName.contains(SpoolNames.Targets.MIXINS) && !transformedName.contains(SpoolNames.Targets.ASM)
            && !transformedName.contains(SpoolNames.Targets.CORE);
    }
}
