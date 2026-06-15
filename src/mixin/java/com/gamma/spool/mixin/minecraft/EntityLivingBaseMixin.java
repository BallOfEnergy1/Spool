package com.gamma.spool.mixin.minecraft;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.core.SpoolManagerOrchestrator;
import com.gamma.spool.thread.IThreadManager;
import com.gamma.spool.thread.ManagerNames;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(EntityLivingBase.class)
public abstract class EntityLivingBaseMixin extends Entity {

    public EntityLivingBaseMixin(World worldIn) {
        super(worldIn);
    }

    @Shadow
    protected abstract void updateAITasks();

    @Shadow
    protected abstract void updateEntityActionState();

    @WrapOperation(
        method = "onLivingUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;updateAITasks()V"))
    private void wrappedUpdateAITasks(EntityLivingBase instance, Operation<Void> original) {
        if (this.worldObj.isRemote || !ThreadsConfig.isEntityAIThreadingEnabled()) {
            // Clients don't thread this.
            original.call(instance);
            return;
        }
        IThreadManager entityAIManager = SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS
            .get(ManagerNames.ENTITY_AI.ordinal());
        entityAIManager.execute(this::updateAITasks);
    }

    @WrapOperation(
        method = "onLivingUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;updateEntityActionState()V"))
    private void wrappedUpdateEntityActionState(EntityLivingBase instance, Operation<Void> original) {
        if (this.worldObj.isRemote || !ThreadsConfig.isEntityAIThreadingEnabled()) {
            // Clients don't thread this.
            original.call(instance);
            return;
        }
        IThreadManager entityAIManager = SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS
            .get(ManagerNames.ENTITY_AI.ordinal());
        entityAIManager.execute(this::updateEntityActionState);
    }
}
