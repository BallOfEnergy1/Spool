package com.gamma.spool.mixin.minecraft;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(EntityDragon.class)
public abstract class EntityDragonMixin extends EntityLiving {

    public EntityDragonMixin(World p_i1595_1_) {
        super(p_i1595_1_);
    }

    @WrapMethod(method = "setNewTarget")
    private void setNewTarget(Operation<Void> original) {
        synchronized (worldObj.playerEntities) {
            original.call();
        }
    }
}
