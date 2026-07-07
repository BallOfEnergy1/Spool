package com.gamma.spool.mixin.minecraft;

import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;

@Mixin(EntitySquid.class)
public abstract class EntitySquidMixin extends EntityWaterMob {

    public EntitySquidMixin(World p_i1695_1_) {
        super(p_i1695_1_);
    }

    // Squid pitch is only ever used on the client.
    @Definition(id = "atan2", method = "Ljava/lang/Math;atan2(DD)D")
    @Definition(id = "motionY", field = "Lnet/minecraft/entity/passive/EntitySquid;motionY:D")
    @Expression("atan2(?, this.motionY)")
    @Redirect(method = "onLivingUpdate", at = @At("MIXINEXTRAS:EXPRESSION"))
    private double redirectedSquidPitch(double y, double x) {
        if (!this.worldObj.isRemote) {
            return 0;
        } else {
            return Math.atan2(y, x);
        }
    }
}
