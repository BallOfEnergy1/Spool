package com.gamma.spool.mixin.minecraft.accessors;

import net.minecraft.entity.EntityLivingBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntityLivingBase.class)
public interface EntityLivingBaseAccessor {

    @Invoker("isAIEnabled")
    boolean spool$isAIEnabled();
}
