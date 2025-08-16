package com.gamma.spool.mixin.minecraft;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.EntityLivingBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(RenderGlobal.class)
public abstract class RenderGlobalMixin {

    @Shadow
    private WorldClient theWorld;

    @WrapMethod(method = "renderEntities")
    private void renderEntities(EntityLivingBase p_147589_1_, ICamera p_147589_2_, float p_147589_3_,
        Operation<Void> original) {
        synchronized (theWorld.loadedEntityList) {
            original.call(p_147589_1_, p_147589_2_, p_147589_3_);
        }
    }
}
