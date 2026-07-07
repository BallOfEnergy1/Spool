package com.gamma.spool.events;

import net.minecraftforge.event.world.ChunkEvent;

import com.gamma.spool.async.EntityLoadingAsync;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

@EventBusSubscriber
public class EntityLoadingAsyncHandler {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChunkLoad(ChunkEvent.Load event) {
        EntityLoadingAsync.onChunkLoad(event.world, event.getChunk());
    }
}
