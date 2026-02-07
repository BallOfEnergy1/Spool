package com.gamma.spool.util;

import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.gameevent.TickEvent;

public class BusLatchRegistry {

    public static void init() {
        new BusLatchBuilder().addPrerequisite(WorldEvent.Load.class)
            .setDependent(TickEvent.WorldTickEvent.class)
            .build();
    }
}
