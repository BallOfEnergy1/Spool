package com.gamma.spool.mixin.minecraft;

import net.minecraft.world.gen.layer.IntCache;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gamma.spool.config.ConcurrentConfig;
import com.gamma.spool.core.SpoolCompat;

// Apply after Hodgepodge.
@Mixin(value = IntCache.class, priority = 1001)
public abstract class IntCacheMixin {

    @Unique
    private static final boolean spool$REMOVED = ConcurrentConfig.removeIntCache;

    /**
     * @author BallOfEnergy01
     * @reason Nuke IntCache for thread-safety.
     */
    @Inject(method = "resetIntCache", at = @At("HEAD"), cancellable = true)
    private static void resetIntCache(CallbackInfo ci) {
        if (spool$REMOVED && !SpoolCompat.isModLoadedFast(SpoolCompat.CompatibleMods.HODGEPODGE)) ci.cancel();
    }

    /**
     * @author BallOfEnergy01
     * @reason Nuke IntCache for thread-safety.
     */
    @Inject(method = "getIntCache", at = @At("HEAD"), cancellable = true)
    private static void getIntCache(int p_76445_0_, CallbackInfoReturnable<int[]> cir) {
        if (spool$REMOVED && !SpoolCompat.isModLoadedFast(SpoolCompat.CompatibleMods.HODGEPODGE))
            cir.setReturnValue(new int[p_76445_0_]);
    }
}
