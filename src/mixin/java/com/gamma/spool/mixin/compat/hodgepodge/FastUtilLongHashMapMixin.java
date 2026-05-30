package com.gamma.spool.mixin.compat.hodgepodge;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mitchej123.hodgepodge.util.FastUtilLongHashMap;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

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
    protected Long2ObjectOpenHashMap<Object> map;

    @WrapOperation(
        method = "getValueByKey",
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;get(J)Ljava/lang/Object;"))
    private <V> V getValueByKeyHandler(Long2ObjectOpenHashMap<V> instance, long k, Operation<V> original) {
        synchronized (this.map) {
            return original.call(instance, k);
        }
    }

    @WrapOperation(
        method = "getNumHashElements",
        at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;size()I"))
    private <V> int getNumHashElementsHandler(Long2ObjectOpenHashMap<V> instance, Operation<Integer> original) {
        synchronized (this.map) {
            return original.call(instance);
        }
    }

    @WrapOperation(
        method = "containsItem",
        at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;containsKey(J)Z"))
    private <V> boolean containsItemHandler(Long2ObjectOpenHashMap<V> instance, long k, Operation<Boolean> original) {
        synchronized (this.map) {
            return original.call(instance, k);
        }
    }

    @WrapOperation(
        method = "add",
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;put(JLjava/lang/Object;)Ljava/lang/Object;"))
    private <V> V addHandler(Long2ObjectOpenHashMap<V> instance, long k, V v, Operation<V> original) {
        synchronized (this.map) {
            return original.call(instance, k, v);
        }
    }

    @WrapOperation(
        method = "remove",
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;remove(J)Ljava/lang/Object;"))
    private <V> V removeHandler(Long2ObjectOpenHashMap<V> instance, long k, Operation<V> original) {
        synchronized (this.map) {
            return original.call(instance, k);
        }
    }
}
