package com.gamma.spool.mixin.minecraft;

import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.biome.BiomeGenBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gamma.spool.util.ThreadedBiomeDecorator;

@Mixin(BiomeGenBase.class)
public abstract class BiomeGenBaseMixin {

    @Inject(method = "createBiomeDecorator", at = @At("RETURN"), cancellable = true)
    private void injected(CallbackInfoReturnable<BiomeDecorator> cir) {
        BiomeGenBase instance = (BiomeGenBase) (Object) this;
        cir.setReturnValue(instance.getModdedBiomeDecorator(new ThreadedBiomeDecorator()));
    }
}
