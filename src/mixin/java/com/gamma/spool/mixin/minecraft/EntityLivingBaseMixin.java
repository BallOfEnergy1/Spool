package com.gamma.spool.mixin.minecraft;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.core.SpoolManagerOrchestrator;
import com.gamma.spool.thread.ManagerNames;

@Mixin(EntityLivingBase.class)
public abstract class EntityLivingBaseMixin extends Entity {

    public EntityLivingBaseMixin(World worldIn) {
        super(worldIn);
    }

    @Shadow
    protected abstract void updateAITasks();

    @Shadow
    protected abstract void updateEntityActionState();

    @Redirect(
        method = "onLivingUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;updateAITasks()V"))
    private void redirectUpdateAITasks(EntityLivingBase instance) {
        if (this.worldObj.isRemote || !ThreadsConfig.isEntityAIThreadingEnabled()) {
            // Clients don't thread this.
            updateAITasks();
            return;
        }
        SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS.get(ManagerNames.ENTITY_AI)
            .execute(this::updateAITasks);
    }

    @Redirect(
        method = "onLivingUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;updateEntityActionState()V"))
    private void redirectUpdateEntityActionState(EntityLivingBase instance) {
        if (this.worldObj.isRemote || !ThreadsConfig.isEntityAIThreadingEnabled()) {
            // Clients don't thread this.
            updateEntityActionState();
            return;
        }
        SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS.get(ManagerNames.ENTITY_AI)
            .execute(this::updateEntityActionState);
    }
}
