package com.gamma.spool.util.distance;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.world.WorldEvent;

import com.gamma.spool.config.DistanceThreadingConfig;
import com.github.bsideup.jabel.Desugar;
import com.google.common.annotations.VisibleForTesting;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;

public class DistanceThreadingChunkUtil {

    // Utility
    static long getHashFromChunk(Chunk chunk) {
        return ChunkCoordIntPair.chunkXZ2Int(chunk.xPosition, chunk.zPosition);
    }

    static long getHashFromChunk(ChunkCoordIntPair pair) {
        return ChunkCoordIntPair.chunkXZ2Int(pair.chunkXPos, pair.chunkZPos);
    }

    // Lambda replacement.
    static int getAmountOfChunksFor(WorldServer world) {
        return ForgeChunkManager.getPersistentChunksFor(world)
            .size();
    }

    /**
     * Gets the total number of force-loaded chunks across all world servers.
     *
     * @return The total count of persistent chunks
     */
    static int getForceLoadedChunksCount() {
        int cached = DistanceThreadingUtil.cache.getAmountOfLoadedChunks();
        if (cached != -1) return cached;

        int sum = 0;
        WorldServer[] worlds = MinecraftServer.getServer().worldServers;
        if (DistanceThreadingConfig.streamParallelizationLevel >= 2) {
            // Parallelize across worlds only; inner work is constant-time lookups
            sum = Arrays.stream(worlds)
                .parallel()
                .mapToInt(DistanceThreadingChunkUtil::getAmountOfChunksFor)
                .sum();
        } else {
            for (int i = 0; i < worlds.length; i++) {
                sum += getAmountOfChunksFor(worlds[i]);
            }
        }
        DistanceThreadingUtil.cache.setAmountOfLoadedChunks(sum);
        return sum;
    }

    // Lambda replacement.
    static WorldChunkData getChunkDataFromWorld(World world) {
        return new WorldChunkData(world, getProcessedPersistentChunks(world));
    }

    static LongSet getProcessedPersistentChunks(World worldObj) {
        LongSet cached = DistanceThreadingUtil.cache.getCachedProcessedChunks(worldObj);
        if (cached != null) return cached;
        LongSet set = LongSets.synchronize(new LongOpenHashSet());
        Set<ChunkCoordIntPair> keys = ForgeChunkManager.getPersistentChunksFor(worldObj)
            .keySet();
        if (DistanceThreadingConfig.streamParallelizationLevel >= 2) {
            keys.parallelStream()
                .forEach(pair -> set.add(ChunkCoordIntPair.chunkXZ2Int(pair.chunkXPos, pair.chunkZPos)));
        } else {
            for (ChunkCoordIntPair pair : keys) {
                set.add(ChunkCoordIntPair.chunkXZ2Int(pair.chunkXPos, pair.chunkZPos));
            }
        }
        DistanceThreadingUtil.cache.setCachedProcessedChunk(worldObj, set);
        return set;
    }

    public static void onChunkForced(ForgeChunkManager.ForceChunkEvent event) {
        DistanceThreadingUtil.cache.addCachedProcessedChunk(event.ticket.world, getHashFromChunk(event.location));
        DistanceThreadingUtil.cache.changeAmountOfLoadedChunks(1);
    }

    public static void onChunkUnforced(ForgeChunkManager.UnforceChunkEvent event) {
        DistanceThreadingUtil.cache.removeCachedProcessedChunk(event.ticket.world, getHashFromChunk(event.location));
        DistanceThreadingUtil.cache.changeAmountOfLoadedChunks(-1);
    }

    public static void onWorldUnload(WorldEvent.Unload event) {
        if (DistanceThreadingUtil.isInitialized()) {
            LongSet set = DistanceThreadingUtil.cache.getCachedProcessedChunks(event.world);
            if (set == null) return;
            DistanceThreadingUtil.cache.changeAmountOfLoadedChunks(-set.size());
            DistanceThreadingUtil.cache.removeCachedProcessedChunk(event.world);
        }
    }

    public static void onWorldLoad(WorldEvent.Load event) {
        LongSet set = getProcessedPersistentChunks(event.world); // Gets us the set *and* caches it!
        DistanceThreadingUtil.cache.changeAmountOfLoadedChunks(set.size());
    }

    public static void onPlayerEnterChunk(EntityEvent.EnteringChunk event) {
        DistanceThreadingUtil.cache.invalidateCacheForWorld(event.entity.worldObj);
    }

    /**
     * Runs an operation for all force-loaded chunks across all loaded world servers by applying the given operation.
     *
     * @param operation The operation to perform on each {@link ChunkProcessingUnit}
     */
    static void forAllForceLoadedChunks(Consumer<ChunkProcessingUnit> operation) {
        WorldServer[] worlds = MinecraftServer.getServer().worldServers;
        if (DistanceThreadingConfig.streamParallelizationLevel >= 1) {
            Arrays.stream(worlds)
                .parallel()
                .forEach(world -> {
                    LongSet chunks = getProcessedPersistentChunks(world);
                    for (long hash : chunks) {
                        operation.accept(new ChunkProcessingUnit(world, hash));
                    }
                });
        } else {
            for (int i = 0; i < worlds.length; i++) {
                WorldServer world = worlds[i];
                LongSet chunks = getProcessedPersistentChunks(world);
                for (long hash : chunks) {
                    operation.accept(new ChunkProcessingUnit(world, hash));
                }
            }
        }
    }

    @VisibleForTesting
    public static int[] undoChunkHash(long chunkHash) {
        int x = (int) (chunkHash & 4294967295L);
        int z = (int) ((chunkHash >> 32) & 4294967295L);
        return new int[] { x, z };
    }

    static boolean checkNear(Chunk chunk, EntityPlayer player) {
        int limit = DistanceThreadingCommonUtil.getDistanceLimit() + 1; // Ring of unloaded chunks 1 chunk wide as
                                                                        // protection against bordering
        // chunks with different executors.
        if (chunk.xPosition < player.chunkCoordX + limit && chunk.xPosition > player.chunkCoordX - limit) {
            return chunk.zPosition < player.chunkCoordZ + limit && chunk.zPosition > player.chunkCoordZ - limit;
        }
        return false;
    }

    // Records
    // Thanks Jabel!
    @Desugar
    record WorldChunkData(World world, LongSet chunks) {}

    @Desugar
    record ChunkProcessingUnit(World world, long chunk) {}
}
