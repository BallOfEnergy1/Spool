package com.gamma.spool.mixin.minecraft;

import net.minecraft.village.VillageSiege;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(VillageSiege.class)
public abstract class VillageSiegeMixin {

    @Shadow
    private World worldObj;

    @WrapMethod(method = "func_75529_b")
    private boolean func_75529_b(Operation<Boolean> original) {
        synchronized (worldObj.playerEntities) {
            return original.call();
        }
    }
}
