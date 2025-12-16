package com.gamma.spool.events;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

import cpw.mods.fml.common.gameevent.PlayerEvent;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongMaps;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;

/**
 * Handles tracking the join and leave times of players on a server.
 * For some reason, Minecraft doesn't do this, so I have to.
 */
public class PlayerJoinTimeHandler {

    private static final Int2LongMap playerJoinTimes = Int2LongMaps.synchronize(new Int2LongOpenHashMap());

    public static long getJoinTime(EntityPlayer player) {
        return playerJoinTimes.get(player.getEntityId());
    }

    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        long joinTime = MinecraftServer.getSystemTimeMillis();
        playerJoinTimes.put(event.player.getEntityId(), joinTime);
    }

    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        playerJoinTimes.remove(event.player.getEntityId());
    }
}
