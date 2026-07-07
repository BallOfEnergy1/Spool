package com.gamma.spool.mixin.minecraft.imixins;

import java.util.List;

import net.minecraft.world.biome.BiomeGenBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BiomeGenBase.class)
public abstract class BiomeGenBaseMixin {

    @Redirect(
        method = "<init>(IZ)V",
        at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 11))
    private <E> boolean add(List<E> instance, E e) {
        return true;
    }
}
