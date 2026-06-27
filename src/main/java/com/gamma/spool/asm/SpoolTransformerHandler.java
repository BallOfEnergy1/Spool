package com.gamma.spool.asm;

import com.gamma.gammalib.asm.ASMRegistry;
import com.gamma.gammalib.asm.interfaces.IHook;
import com.gamma.spool.api.annotations.SkipSpoolASMChecks;
import com.gamma.spool.asm.checks.UnsafeIterationHandler;
import com.gamma.spool.core.Spool;

@SkipSpoolASMChecks(SkipSpoolASMChecks.SpoolASMCheck.ALL)
public class SpoolTransformerHandler implements IHook {

    public static final SpoolTransformerHandler INSTANCE = new SpoolTransformerHandler();

    public void register() {
        ASMRegistry.registerCheck(Spool.MODID, new UnsafeIterationHandler());
        ASMRegistry.registerHook(this);
    }

    @Override
    public boolean beginTransform(String name, String transformedName, byte[] basicClass) {
        return !transformedName.contains(SpoolNames.Targets.MIXINS) && !transformedName.contains(SpoolNames.Targets.ASM)
            && !transformedName.contains(SpoolNames.Targets.CORE);
    }
}
