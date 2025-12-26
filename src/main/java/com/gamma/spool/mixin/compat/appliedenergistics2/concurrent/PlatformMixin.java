package com.gamma.spool.mixin.compat.appliedenergistics2.concurrent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import appeng.util.Platform;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

@Mixin(Platform.class)
// TODO: Maybe make this fix overwrite `getEffectiveSide` for a more universal fix?
public abstract class PlatformMixin {

    @Redirect(
        method = "isClient",
        at = @At(
            value = "INVOKE",
            target = "Lcpw/mods/fml/common/FMLCommonHandler;getEffectiveSide()Lcpw/mods/fml/relauncher/Side;",
            remap = false),
        remap = false)
    private static Side getEffectiveSide(FMLCommonHandler instance) {
        Thread thread = Thread.currentThread();
        if (thread.getName()
            .contains("Server thread")) {
            return Side.SERVER;
        } else if (thread.getName()
            .toLowerCase()
            .contains("spool")) {
                return Side.SERVER;
            }
        return Side.CLIENT;
    }
}
