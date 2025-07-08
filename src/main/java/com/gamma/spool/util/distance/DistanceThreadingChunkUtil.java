package com.gamma.spool.util.distance;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeChunkManager;

import com.gamma.spool.config.DistanceThreadingConfig;
import com.google.common.annotations.VisibleForTesting;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class DistanceThreadingChunkUtil {

    // Lambda replacement.
    static long getHashFromPair(ChunkCoordIntPair pair) {
        return ChunkCoordIntPair.chunkXZ2Int(pair.chunkXPos, pair.chunkZPos);
    }

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
        int cached = DistanceThreadingUtil.cache.amountLoadedChunks.getItem();
        if (cached != -1) return cached;

        IntStream stream = Arrays.stream(MinecraftServer.getServer().worldServers)
            .mapToInt(DistanceThreadingChunkUtil::getAmountOfChunksFor);
        int sum = (DistanceThreadingConfig.parallelizeStreams ? stream.parallel() : stream).sum();
        DistanceThreadingUtil.cache.amountLoadedChunks.setItem(sum);
        return sum;
    }

    // Lambda replacement.
    static DistanceThreadingUtil.WorldChunkData getChunkDataFromWorld(World world) {
        return new DistanceThreadingUtil.WorldChunkData(world, getProcessedPersistentChunks(world));
    }

    static LongOpenHashSet getProcessedPersistentChunks(World worldObj) {
        LongOpenHashSet cached = DistanceThreadingUtil.cache.processedChunks.getItem();
        if (cached != null) return cached;
        LongOpenHashSet set = LongOpenHashSet.toSet(
            (DistanceThreadingConfig.parallelizeStreams // Parallelism check.
                ? ForgeChunkManager.getPersistentChunksFor(worldObj)
                    .keySet()
                    .parallelStream() // Parallel
                : ForgeChunkManager.getPersistentChunksFor(worldObj)
                    .keySet()
                    .stream() // Sequential
            ).mapToLong(DistanceThreadingChunkUtil::getHashFromPair));
        DistanceThreadingUtil.cache.processedChunks.setItem(set);
        return set;
    }

    /**
     * Runs an operation for all force-loaded chunks across all loaded world servers by applying the given operation.
     *
     * @param operation The operation to perform on each {@link ChunkCoordIntPair}
     */
    static void forAllForceLoadedChunks(Consumer<DistanceThreadingUtil.ChunkProcessingUnit> operation) {
        // Phase 1: Collect each world into the stream
        Stream<WorldServer> worldDataStream = DistanceThreadingConfig.parallelizeStreams
            ? Arrays.stream(MinecraftServer.getServer().worldServers)
                .parallel()
            : Arrays.stream(MinecraftServer.getServer().worldServers);

        worldDataStream
            // Phase 2: Collect chunk data for each world (optionally parallel).
            .map(DistanceThreadingChunkUtil::getChunkDataFromWorld)

            // Phase 3: Move individual chunks out of the WorldChunkData object and into the stream (optionally
            // parallel).
            .flatMap((DistanceThreadingUtil.WorldChunkData::getChunkUnitStream))

            // Phase 4: Run operation for every chunk (optionally parallel).
            .forEach(operation);
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
