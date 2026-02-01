package com.gamma.spool.mixin.compat.archaicfix.concurrent;

import java.util.concurrent.locks.ReentrantLock;

import org.embeddedt.archaicfix.lighting.world.lighting.LightingEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LightingEngine.class)
public abstract class LightingEngineMixin {

    @Shadow(remap = false)
    @Final
    private ReentrantLock lock;

    // TODO: Make thread-safe and thread this.

    /**
     * @author BallOfEnergy
     * @reason Don't warn, just lock.
     */
    @Overwrite(remap = false)
    private void acquireLock() {
        lock.lock();
    }
}
