package com.gamma.lmtm.mixin.minecraft;

import net.minecraft.world.biome.BiomeGenEnd;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gamma.lmtm.util.ThreadedBiomeEndDecorator;

@Mixin(BiomeGenEnd.class)
public abstract class BiomeGenEndMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        BiomeGenEnd instance = (BiomeGenEnd) (Object) this;
        instance.theBiomeDecorator = new ThreadedBiomeEndDecorator();
    }
}
