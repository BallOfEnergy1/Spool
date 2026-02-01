package com.gamma.spool.mixin.minecraft.concurrent;

import java.util.concurrent.locks.ReentrantLock;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This class makes `Block`'s `onNeighborBlockChange` method thread-safe to
 * align with Spool's world concurrency.
 */
@Mixin(Block.class)
public abstract class BlockMixin {

    @Unique
    private final ReentrantLock spool$blockLock = new ReentrantLock(true); // Maintain block update order.

    @Inject(method = "onNeighborBlockChange", at = @At(value = "HEAD"))
    public void onNeighborBlockChangeInjectHead(World worldIn, int x, int y, int z, Block neighbor, CallbackInfo ci) {
        spool$blockLock.lock();
    }

    @Inject(method = "onNeighborBlockChange", at = @At(value = "RETURN"))
    public void onNeighborBlockChangeInjectReturn(World worldIn, int x, int y, int z, Block neighbor, CallbackInfo ci) {
        spool$blockLock.unlock();
    }
}
