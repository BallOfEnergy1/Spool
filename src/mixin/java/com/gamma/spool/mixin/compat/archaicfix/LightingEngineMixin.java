package com.gamma.spool.mixin.compat.archaicfix;

import java.util.concurrent.locks.ReentrantLock;

import org.embeddedt.archaicfix.lighting.world.lighting.LightingEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

// The usefulness of this class is debatable, especially since Phosphor is disabled
// by us.
@Mixin(LightingEngine.class)
public abstract class LightingEngineMixin {

    @Shadow(remap = false)
    @Final
    private ReentrantLock lock;

    /**
     * @author BallOfEnergy
     * @reason Don't warn, just lock.
     */
    @Overwrite(remap = false)
    private void acquireLock() {
        lock.lock();
    }
}
