package com.gamma.spool.mixin.minecraft;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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

    @Redirect(
        method = "onLivingUpdate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/profiler/Profiler;startSection(Ljava/lang/String;)V",
            ordinal = 1))
    private void wrapped1(Profiler instance, String name) {
        // NOOP
    }

    @Redirect(
        method = "onLivingUpdate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/profiler/Profiler;startSection(Ljava/lang/String;)V",
            ordinal = 2))
    private void wrapped2(Profiler instance, String name) {
        // NOOP
    }

    @Shadow
    protected abstract void updateAITasks();

    @Shadow
    protected abstract void updateEntityActionState();

    @Unique
    private static void spool$newAITask(EntityLivingBase instance) {
        instance.worldObj.theProfiler.startSection("newAiInner");
        ((EntityLivingBaseMixin) ((Object) instance)).updateAITasks();
        instance.worldObj.theProfiler.endSection();
    }

    @Unique
    private static void spool$oldAITask(EntityLivingBase instance) {
        instance.worldObj.theProfiler.startSection("oldAiInner");
        ((EntityLivingBaseMixin) ((Object) instance)).updateEntityActionState();
        instance.worldObj.theProfiler.endSection();
    }

    @WrapOperation(
        method = "onLivingUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;updateAITasks()V"))
    private void wrappedUpdateAITasks(EntityLivingBase instance, Operation<Void> original) {
        this.worldObj.theProfiler.startSection("newAIExecute");
        if (this.worldObj.isRemote || !ThreadsConfig.isEntityAIThreadingEnabled()) {
            // Clients don't thread this.
            original.call(instance);
            return;
        }
        IThreadManager entityAIManager = SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS
            .get(ManagerNames.ENTITY_AI.ordinal());
        entityAIManager.execute(EntityLivingBaseMixin::spool$newAITask, instance);
    }

    @WrapOperation(
        method = "onLivingUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;updateEntityActionState()V"))
    private void wrappedUpdateEntityActionState(EntityLivingBase instance, Operation<Void> original) {
        this.worldObj.theProfiler.startSection("oldAIExecute");
        if (this.worldObj.isRemote || !ThreadsConfig.isEntityAIThreadingEnabled()) {
            // Clients don't thread this.
            original.call(instance);
            return;
        }
        IThreadManager entityAIManager = SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS
            .get(ManagerNames.ENTITY_AI.ordinal());
        entityAIManager.execute(EntityLivingBaseMixin::spool$oldAITask, instance);
    }
}
