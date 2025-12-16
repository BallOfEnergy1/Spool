package com.gamma.spool.mixin.minecraft;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    private int entityId;
    @Shadow
    private static int nextEntityID;
    @Unique
    private static final AtomicInteger spool$nextEntityId = new AtomicInteger(0);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(World worldIn, CallbackInfo ci) {
        // We can't trust the ID given to us originally, as it could be incorrect.
        entityId = spool$nextEntityId.getAndIncrement();

        // This is not the best way of doing this, but with the circumstances it's probably fine.
        // No other mods should be using this, so it shouldn't make a big difference in the first place.
        // Even if they are using it, I doubt they're changing the value, so this is totally fine.
        nextEntityID = spool$nextEntityId.get();
    }

    /**
     * @author BallOfEnergy01
     * @reason Change to use atomic integer for concurrency.
     */
    @Overwrite(remap = false)
    public final void resetEntityId() {
        entityId = spool$nextEntityId.getAndIncrement();
        nextEntityID = spool$nextEntityId.get();
    }
}
