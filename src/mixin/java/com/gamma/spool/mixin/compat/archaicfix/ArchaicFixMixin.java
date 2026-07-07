package com.gamma.spool.mixin.compat.archaicfix;

import org.embeddedt.archaicfix.ArchaicFix;
import org.embeddedt.archaicfix.config.ArchaicConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

// sorry, just gotta disable you!
@Mixin(value = ArchaicFix.class, remap = false)
public abstract class ArchaicFixMixin {

    @Inject(method = "preinit", at = @At("HEAD"))
    private void injectedPreInitHead(FMLPreInitializationEvent event, CallbackInfo ci) {
        ArchaicConfig.enablePhosphor = false; // Disable Phosphor
    }
}
