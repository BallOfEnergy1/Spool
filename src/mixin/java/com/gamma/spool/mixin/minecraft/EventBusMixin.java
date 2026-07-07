package com.gamma.spool.mixin.minecraft;

import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gamma.spool.util.BusLatch;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mixin(value = EventBus.class, remap = false)
public abstract class EventBusMixin {

    @Inject(method = "post", at = @At("HEAD"))
    public void postHead(Event event, CallbackInfoReturnable<Boolean> cir, @Share("world") LocalRef<World> worldRef) {
        World world = null;
        if (event instanceof WorldEvent we) {
            world = we.world;
            // these already skip on clients (world.isRemote)
            if (we instanceof WorldEvent.Load) BusLatch.onWorldLoad(world);
            else if (we instanceof WorldEvent.Unload) BusLatch.onWorldUnload(world);
        } else if (event instanceof TickEvent.WorldTickEvent wte) world = wte.world;
        if (world == null) return;
        if (world.isRemote) return; // skip on clients
        worldRef.set(world);
        BusLatch.preEvent(event.getClass(), world.provider.dimensionId);
    }

    @Inject(method = "post", at = @At("RETURN"))
    public void postReturn(Event event, CallbackInfoReturnable<Boolean> cir, @Share("world") LocalRef<World> worldRef) {
        World world = worldRef.get();
        if (world == null || world.isRemote) return; // skip on clients
        BusLatch.postEvent(event.getClass(), world.provider.dimensionId);
    }
}
