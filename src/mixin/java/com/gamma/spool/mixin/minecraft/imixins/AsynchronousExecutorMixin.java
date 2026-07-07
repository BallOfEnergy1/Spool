package com.gamma.spool.mixin.minecraft.imixins;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import net.minecraftforge.common.util.AsynchronousExecutor;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// This is only used in the `ChunkIOExecutor`. If it's used anywhere else by any mods, that's their issue. Fuck them.
@Mixin(value = AsynchronousExecutor.class, remap = false)
public abstract class AsynchronousExecutorMixin {

    @Mutable
    @Shadow
    @Final
    ThreadPoolExecutor pool;

    @Unique
    private final ExecutorService spool$service = Executors.newThreadPerTaskExecutor(
        Thread.ofVirtual()
            .name("Virtual Chunk I/O Executor Thread")
            .factory());

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(AsynchronousExecutor.CallBackProvider<?, ?, ?, ?> provider, int coreSize, CallbackInfo ci) {
        this.pool = null;
    }

    @Redirect(
        method = "add",
        at = @At(value = "INVOKE", target = "Ljava/util/concurrent/ThreadPoolExecutor;execute(Ljava/lang/Runnable;)V"))
    private void redirectExecute(ThreadPoolExecutor instance, Runnable command) {
        spool$service.execute(command);
    }

    /**
     * @author BallOfEnergy01
     * @reason Replace with virtual threads.
     */
    @Overwrite
    public void setActiveThreads(int count) {
        // NOOP
    }
}
