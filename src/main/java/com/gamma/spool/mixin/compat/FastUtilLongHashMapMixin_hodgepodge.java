package com.gamma.spool.mixin.compat;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mitchej123.hodgepodge.util.FastUtilLongHashMap;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

// This is my first time doing a mixin like this so don't laugh at me
// this is way beyond my skill level, but I'm managing :pray:

@SuppressWarnings("SynchronizeOnNonFinalField") // go away, literally the entire class is shadowing a final field.
@Mixin(value = FastUtilLongHashMap.class, remap = false)
public abstract class FastUtilLongHashMapMixin_hodgepodge {

    @Shadow
    @Final
    private Long2ObjectMap<Object> map;

    @WrapOperation(
        method = "add",
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;put(JLjava/lang/Object;)Ljava/lang/Object;"),
        remap = false)
    private Object addHandler(Long2ObjectMap<?> instance, long l, Object o, Operation<Object> original) {
        synchronized (this.map) {
            return original.call(instance, l, o);
        }
    }

    @WrapOperation(
        method = "remove",
        at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;remove(J)Ljava/lang/Object;"),
        remap = false)
    private Object removeHandler(Long2ObjectMap<?> instance, long l, Operation<Object> original) {
        synchronized (this.map) {
            return original.call(instance, l);
        }
    }

    // Ok so here's the problem...
    // Synchronized iterators.
    // Iterators can't be internally synchronized (as far as I know), so we have to externally synchronize them.
}
