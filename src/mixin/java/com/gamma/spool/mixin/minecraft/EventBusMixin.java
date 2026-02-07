package com.gamma.spool.mixin.minecraft;

import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;

import org.spongepowered.asm.mixin.Mixin;

import com.gamma.spool.util.BusLatch;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mixin(EventBus.class)
public abstract class EventBusMixin {

    @WrapMethod(method = "post", remap = false)
    public boolean postWrapped(Event event, Operation<Boolean> original) {
        World world = null;
        int dim;
        if (event instanceof WorldEvent we) {
            world = we.world;
            if (we instanceof WorldEvent.Load) BusLatch.onWorldLoad(world.provider.dimensionId);
            else if (we instanceof WorldEvent.Unload) BusLatch.onWorldUnload(world.provider.dimensionId);
        } else if (event instanceof TickEvent.WorldTickEvent wte) world = wte.world;
        if (world == null) return original.call(event);
        dim = world.provider.dimensionId;
        BusLatch.preEvent(event.getClass(), dim);
        try {
            return original.call(event);
        } finally {
            BusLatch.postEvent(event.getClass(), dim);
        }
    }
}
