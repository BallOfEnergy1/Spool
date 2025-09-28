package com.gamma.spool.mixin.compat.hodgepodge;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mitchej123.hodgepodge.util.FastUtilLongHashMap;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

// I'll likely end up removing this later,
// however, I'm not entirely sure if I even still need this.
// I'm pretty sure I do, though it gets slow fast.
// I'll figure that out later.
// TODO: figure that out

@SuppressWarnings("SynchronizeOnNonFinalField") // go away, literally the entire class is shadowing a final field.
@Mixin(value = FastUtilLongHashMap.class, remap = false)
public abstract class FastUtilLongHashMapMixin {

    @Shadow
    @Final
    private Long2ObjectMap<Object> map;

    @WrapOperation(
        method = "getValueByKey",
        at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;"),
        remap = false)
    private Object getValueByKeyHandler(Long2ObjectMap<?> instance, long l, Operation<Object> original) {
        synchronized (this.map) {
            return original.call(instance, l);
        }
    }

    @WrapOperation(
        method = "getNumHashElements",
        at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;size()I"),
        remap = false)
    private int getNumHashElementsHandler(Long2ObjectMap<?> instance, Operation<Integer> original) {
        synchronized (this.map) {
            return original.call(instance);
        }
    }

    @WrapOperation(
        method = "containsItem",
        at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;containsKey(J)Z"),
        remap = false)
    private boolean containsItemHandler(Long2ObjectMap<?> instance, long l, Operation<Boolean> original) {
        synchronized (this.map) {
            return original.call(instance, l);
        }
    }

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
