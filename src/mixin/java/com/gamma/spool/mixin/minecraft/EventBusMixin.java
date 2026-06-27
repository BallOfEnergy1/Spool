package com.gamma.spool.mixin.minecraft;

import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gamma.spool.util.BusLatch;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mixin(value = EventBus.class, remap = false)
public abstract class EventBusMixin {

    @Inject(method = "post", at = @At("HEAD"))
    public void postHead(Event event, CallbackInfoReturnable<Boolean> cir, @Share("dim") LocalIntRef dim) {
        World world = null;
        if (event instanceof WorldEvent we) {
            world = we.world;
            if (we instanceof WorldEvent.Load) BusLatch.onWorldLoad(world.provider.dimensionId);
            else if (we instanceof WorldEvent.Unload) BusLatch.onWorldUnload(world.provider.dimensionId);
        } else if (event instanceof TickEvent.WorldTickEvent wte) world = wte.world;
        if (world == null) return;
        int tempDim;
        dim.set(tempDim = world.provider.dimensionId);
        BusLatch.preEvent(event.getClass(), tempDim);
    }

    @Inject(method = "post", at = @At("RETURN"))
    public void postReturn(Event event, CallbackInfoReturnable<Boolean> cir, @Share("dim") LocalIntRef dim) {
        BusLatch.postEvent(event.getClass(), dim.get());
    }
}
