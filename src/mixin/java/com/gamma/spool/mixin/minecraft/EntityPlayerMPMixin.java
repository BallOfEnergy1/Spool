package com.gamma.spool.mixin.minecraft;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.ChunkCoordIntPair;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

@Mixin(EntityPlayerMP.class)
public abstract class EntityPlayerMPMixin {

    @Shadow
    @Final
    @Mutable
    public List<ChunkCoordIntPair> loadedChunks;

    @Shadow
    @Final
    @Mutable
    public List<Integer> destroyedItemsNetCache;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        loadedChunks = ObjectLists.synchronize(new ObjectArrayList<>());
        destroyedItemsNetCache = IntLists.synchronize(new IntArrayList());
    }

    // Because of a quirk in Mixins, this cannot easily be replaced.
    @WrapMethod(method = "onUpdate")
    private void onUpdate(Operation<Void> original) {
        synchronized (this.loadedChunks) {
            original.call();
        }
    }
}
