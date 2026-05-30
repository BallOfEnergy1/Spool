package com.gamma.spool.mixin.minecraft.concurrent;

import net.minecraft.world.chunk.NibbleArray;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.gamma.gammalib.multi.factory.NibbleArrayFactory;
import com.gamma.gammalib.multi.nibblearray.FastAtomicNibbleArray;

@Mixin(NibbleArray.class)
public abstract class NibbleArrayMixin {

    @Shadow
    public byte[] data;

    @Unique
    private FastAtomicNibbleArray spool$wrapped;

    /**
     * @author BallOfEnergy01
     * @reason Concurrency.
     */
    @Overwrite
    public int get(int x, int y, int z) {
        if (spool$wrapped == null) spool$wrapped = NibbleArrayFactory.wrap(data);
        return spool$wrapped.get(x, y, z);
    }

    /**
     * @author BallOfEnergy01
     * @reason Concurrency.
     */
    @Overwrite
    public void set(int x, int y, int z, int value) {
        if (spool$wrapped == null) spool$wrapped = NibbleArrayFactory.wrap(data);
        spool$wrapped.set(x, y, z, value);
    }
}
