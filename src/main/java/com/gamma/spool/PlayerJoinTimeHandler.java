package com.gamma.spool;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

import cpw.mods.fml.common.gameevent.PlayerEvent;
import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;

/**
 * Handles tracking the join and leave times of players on a server.
 * For some reason, Minecraft doesn't do this, so I have to.
 */
public class PlayerJoinTimeHandler {

    private static final Object2LongArrayMap<EntityPlayer> playerJoinTimes = new Object2LongArrayMap<>();

    public static long getJoinTime(EntityPlayer player) {
        return playerJoinTimes.getLong(player);
    }

    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        long joinTime = MinecraftServer.getSystemTimeMillis();
        playerJoinTimes.put(event.player, joinTime);
    }

    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        playerJoinTimes.removeLong(event.player);
    }
}
