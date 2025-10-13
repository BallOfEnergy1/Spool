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

import org.jctools.maps.NonBlockingHashSet;

import com.gamma.spool.config.DistanceThreadingConfig;
import com.google.common.annotations.VisibleForTesting;

public class DistanceThreadingChunkUtil {

    // Utility
    static long getHashFromChunk(Chunk chunk) {
        return ChunkCoordIntPair.chunkXZ2Int(chunk.xPosition, chunk.zPosition);
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
        if (DistanceThreadingConfig.parallelizeStreams) {
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
        DistanceThreadingUtil.cache.amountLoadedChunks.setItem(sum);
        return sum;
    }

    // Lambda replacement.
    static DistanceThreadingUtil.WorldChunkData getChunkDataFromWorld(World world) {
        return new DistanceThreadingUtil.WorldChunkData(world, getProcessedPersistentChunks(world));
    }

    static NonBlockingHashSet<Long> getProcessedPersistentChunks(World worldObj) {
        NonBlockingHashSet<Long> cached = DistanceThreadingUtil.cache.getCachedProcessedChunk(worldObj);
        if (cached != null) return cached;
        NonBlockingHashSet<Long> set = new NonBlockingHashSet<>();
        // Build directly into the non-blocking set to avoid intermediate allocations/boxing churn
        Set<ChunkCoordIntPair> keys = ForgeChunkManager.getPersistentChunksFor(worldObj)
            .keySet();
        if (DistanceThreadingConfig.parallelizeStreams) {
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

    /**
     * Runs an operation for all force-loaded chunks across all loaded world servers by applying the given operation.
     *
     * @param operation The operation to perform on each {@link ChunkCoordIntPair}
     */
    static void forAllForceLoadedChunks(Consumer<DistanceThreadingUtil.ChunkProcessingUnit> operation) {
        WorldServer[] worlds = MinecraftServer.getServer().worldServers;
        if (DistanceThreadingConfig.parallelizeStreams) {
            Arrays.stream(worlds)
                .parallel()
                .forEach(world -> {
                    NonBlockingHashSet<Long> chunks = getProcessedPersistentChunks(world);
                    for (Long hash : chunks) {
                        operation.accept(new DistanceThreadingUtil.ChunkProcessingUnit(world, hash));
                    }
                });
        } else {
            for (int i = 0; i < worlds.length; i++) {
                WorldServer world = worlds[i];
                NonBlockingHashSet<Long> chunks = getProcessedPersistentChunks(world);
                for (Long hash : chunks) {
                    operation.accept(new DistanceThreadingUtil.ChunkProcessingUnit(world, hash));
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
}
