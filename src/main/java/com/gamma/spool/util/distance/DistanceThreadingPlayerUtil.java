package com.gamma.spool.util.distance;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import com.gamma.spool.events.PlayerJoinTimeHandler;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DistanceThreadingPlayerUtil {

    @SuppressWarnings("SameParameterValue")
    static DistanceThreadingUtil.Nearby getNearestPlayers(EntityPlayer player, boolean ignoreLimit) {
        return getNearestPlayers(player, ignoreLimit, null);
    }

    static DistanceThreadingUtil.Nearby getNearestPlayers(EntityPlayer player, boolean ignoreLimit,
        EntityPlayer precalculatedClosest) {
        DistanceThreadingUtil.Nearby cachedResult = DistanceThreadingUtil.cache
            .getCachedNearestPlayer(player.worldObj, playerHashcode(player));
        if (cachedResult != null) if (cachedResult.usedIgnoreLimit() == ignoreLimit) return cachedResult;
        List<EntityPlayer> allPlayersInWorld = new ObjectArrayList<>(player.worldObj.playerEntities);
        allPlayersInWorld.remove(player);
        if (allPlayersInWorld.isEmpty()) {
            DistanceThreadingUtil.Nearby nearby = new DistanceThreadingUtil.Nearby(
                null,
                new ObjectArrayList<>(),
                ignoreLimit);
            DistanceThreadingUtil.cache.setCachedNearestPlayer(player.worldObj, playerHashcode(player), nearby);
            return nearby;
        }
        EntityPlayer closest = precalculatedClosest;
        boolean shouldCalculateClosest = closest == null;
        List<EntityPlayer> allClose = new ObjectArrayList<>();
        double distance = 0D;
        // noinspection ForLoopReplaceableByForEach
        for (int i = 0, allPlayersInWorldSize = allPlayersInWorld.size(); i < allPlayersInWorldSize; i++) {
            EntityPlayer otherPlayer = allPlayersInWorld.get(i);
            if (ignoreLimit || checkNear(player, otherPlayer)) {
                if (shouldCalculateClosest) {
                    double newDist = otherPlayer.getDistanceSqToEntity(player);
                    if (distance < newDist) {
                        closest = otherPlayer;
                        distance = newDist;
                    }
                }
                allClose.add(otherPlayer);
            }
        }
        DistanceThreadingUtil.Nearby nearby = new DistanceThreadingUtil.Nearby(closest, allClose, ignoreLimit);
        DistanceThreadingUtil.cache.setCachedNearestPlayer(player.worldObj, playerHashcode(player), nearby);
        return nearby;
    }

    static DistanceThreadingUtil.Nearby getNearestPlayers(Chunk chunk, boolean ignoreLimit) {
        DistanceThreadingUtil.Nearby cachedResult = DistanceThreadingUtil.cache
            .getCachedNearestChunk(chunk.worldObj, ChunkCoordIntPair.chunkXZ2Int(chunk.xPosition, chunk.zPosition));
        if (cachedResult != null) if (cachedResult.usedIgnoreLimit() == ignoreLimit) return cachedResult;

        List<EntityPlayer> allPlayersInWorld = chunk.worldObj.playerEntities;
        if (allPlayersInWorld.isEmpty()) {
            DistanceThreadingUtil.Nearby nearby = new DistanceThreadingUtil.Nearby(null, null, ignoreLimit);
            DistanceThreadingUtil.cache.setCachedNearestChunk(
                chunk.worldObj,
                ChunkCoordIntPair.chunkXZ2Int(chunk.xPosition, chunk.zPosition),
                nearby);
            return nearby;
        }
        EntityPlayer closest = null;
        double distance = 0D;
        // noinspection ForLoopReplaceableByForEach
        for (int i = 0, allPlayersInWorldSize = allPlayersInWorld.size(); i < allPlayersInWorldSize; i++) {
            EntityPlayer otherPlayer = allPlayersInWorld.get(i);
            if (ignoreLimit || DistanceThreadingChunkUtil.checkNear(chunk, otherPlayer)) {
                double newDist = otherPlayer
                    .getDistanceSq(chunk.xPosition * 16 + 8, otherPlayer.posY, chunk.zPosition * 16 + 8);
                if (distance < newDist) {
                    closest = otherPlayer;
                    distance = newDist;
                }

            }
        }
        // this is horrifying :heart:
        // This basically allows the target player *and* the closest player to be the same player, which isn't intended
        // to happen,
        // but it's how the code is structured downstream.
        if (closest == null) {// Not really much we can do here. There are no close players.
            DistanceThreadingUtil.Nearby nearby = new DistanceThreadingUtil.Nearby(null, null, ignoreLimit);
            DistanceThreadingUtil.cache.setCachedNearestChunk(
                chunk.worldObj,
                ChunkCoordIntPair.chunkXZ2Int(chunk.xPosition, chunk.zPosition),
                nearby);
            return nearby;
        }
        DistanceThreadingUtil.Nearby nearby = getNearestPlayers(closest, ignoreLimit, closest);
        DistanceThreadingUtil.cache.setCachedNearestChunk(
            chunk.worldObj,
            ChunkCoordIntPair.chunkXZ2Int(chunk.xPosition, chunk.zPosition),
            nearby);
        return nearby;
    }

    static boolean checkNear(EntityPlayer player, EntityPlayer otherPlayer) {
        int limit = DistanceThreadingCommonUtil.getDistanceLimit() + 1; // Ring of unloaded chunks 1 chunk wide as
                                                                        // protection against bordering
        // chunks with different executors.
        if (otherPlayer.chunkCoordX < player.chunkCoordX + limit
            && otherPlayer.chunkCoordX > player.chunkCoordX - limit) {
            return otherPlayer.chunkCoordZ < player.chunkCoordZ + limit
                && otherPlayer.chunkCoordZ > player.chunkCoordZ - limit;
        }
        return false;
    }

    public static int playerHashcode(EntityPlayer player) {
        int uuidCode = player.getUniqueID()
            .hashCode();
        World world = player.getEntityWorld();
        if (world.isRemote) return uuidCode;
        long timeJoined = PlayerJoinTimeHandler.getJoinTime(player);
        // Not sure how to check the validity of this algorithm...
        return (int) (uuidCode ^ (timeJoined >>> 32) ^ timeJoined);
    }

    static EntityPlayer getPrioritized(List<EntityPlayer> players, EntityPlayer self) {
        EntityPlayer prioritized = self;
        // noinspection ForLoopReplaceableByForEach
        for (int i = 0, playersSize = players.size(); i < playersSize; i++) {
            EntityPlayer player = players.get(i);
            if (prioritized == null) prioritized = player;
            else if (PlayerJoinTimeHandler.getJoinTime(player) < PlayerJoinTimeHandler.getJoinTime(prioritized))
                prioritized = player;
        }
        return prioritized;
    }
}
