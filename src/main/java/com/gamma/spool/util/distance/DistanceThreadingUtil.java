package com.gamma.spool.util.distance;

import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import org.jctools.maps.NonBlockingHashSet;

import com.gamma.spool.config.DistanceThreadingConfig;
import com.gamma.spool.core.SpoolLogger;
import com.gamma.spool.thread.IThreadManager;
import com.gamma.spool.thread.KeyedPoolThreadManager;
import com.github.bsideup.jabel.Desugar;
import com.google.common.annotations.VisibleForTesting;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class DistanceThreadingUtil {

    static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock(true);

    static KeyedPoolThreadManager keyedPool;

    public static final DistanceThreadingCache cache = new DistanceThreadingCache();

    public static KeyedPoolThreadManager getKeyedPool() {
        return keyedPool;
    }

    public static boolean isInitialized() {
        return keyedPool != null;
    }

    public static void init(IThreadManager keyedPool) {
        LOCK.writeLock()
            .lock();
        if (DistanceThreadingUtil.keyedPool != null)
            throw new IllegalStateException("DistanceThreadingUtil is already initialized!");

        if (!(keyedPool instanceof KeyedPoolThreadManager))
            throw new IllegalArgumentException("KeyedPoolThreadManager is required for DistanceThreadingUtil!");
        DistanceThreadingUtil.keyedPool = (KeyedPoolThreadManager) keyedPool;
        keyedPool.startPoolIfNeeded();
        cache.invalidate();
        SpoolLogger.info("Initialized DistanceThreadingUtil.");
        LOCK.writeLock()
            .unlock();
    }

    public static void teardown() {
        LOCK.writeLock()
            .lock();
        if (DistanceThreadingUtil.keyedPool == null)
            throw new IllegalStateException("DistanceThreadingUtil was not initialized!");

        keyedPool.terminatePool();
        DistanceThreadingUtil.keyedPool = null;
        playerExecutorMap.clear();
        chunkExecutorMap.clear();
        cache.invalidate();
        SpoolLogger.info("Terminated DistanceThreadingUtil.");
        LOCK.writeLock()
            .unlock();
    }

    // Holds all players and their executor mappings.
    private static final Object2IntMap<EntityPlayer> playerExecutorMap = Object2IntMaps
        .synchronize(new Object2IntOpenHashMap<>());

    public static Object2IntMap<EntityPlayer> getPlayerExecutorMap() {
        return playerExecutorMap;
    }

    // Holds all forced chunks and their executor mappings.
    private static final Long2IntMap chunkExecutorMap = Long2IntMaps.synchronize(new Long2IntOpenHashMap());

    public static Long2IntMap getChunkExecutorMap() {
        return chunkExecutorMap;
    }

    public static void onClientLeave(EntityPlayer player) {
        cache.invalidate();
        final int executor = playerExecutorMap.getInt(player);

        IntStream stream;
        if (DistanceThreadingConfig.parallelizeStreams) stream = chunkExecutorMap.values()
            .intParallelStream();
        else stream = chunkExecutorMap.values()
            .intStream();

        stream.filter(e -> e != executor)
            .forEach(chunkExecutorMap::remove);

        if (playerExecutorMap.remove(player, executor)) {
            if (!playerExecutorMap.containsValue(executor)) keyedPool.removeKeyedThread(executor);
        }
    }

    public static int getThread(EntityPlayer player) {
        if (player == null) throw new NullPointerException("Player cannot be null.");
        return getThread(player, DistanceThreadingPlayerUtil.getNearestPlayers(player, false));
    }

    private static boolean shouldCheckChunkInstability = false;

    public static int getThread(Chunk chunk) {
        shouldCheckChunkInstability = true;
        if (chunk == null) throw new NullPointerException("Chunk cannot be null.");

        long pairHash = DistanceThreadingChunkUtil.getHashFromChunk(chunk);

        if (!DistanceThreadingChunkUtil.getChunkDataFromWorld(chunk.worldObj)
            .chunks()
            .contains(pairHash)) {
            Nearby nearbyPlayers = DistanceThreadingPlayerUtil.getNearestPlayers(chunk, false);
            // Chunk is not force-loaded.
            int returnValue;
            if (nearbyPlayers.nearest == null) {
                // This chunk shouldn't even be loaded at all.
                // Execute it on the default thread.
                returnValue = KeyedPoolThreadManager.MAIN_THREAD_KEY;
            } else returnValue = getThread(nearbyPlayers);
            // Just incase this was ever force loaded in the past, we can remove it.
            chunkExecutorMap.remove(pairHash, returnValue);
            return returnValue;
        }

        Nearby nearbyPlayers = DistanceThreadingPlayerUtil.getNearestPlayers(chunk, true);

        // Chunk is force-loaded.
        boolean containsKey = chunkExecutorMap.containsKey(pairHash);
        if (containsKey) {
            // Executor exists.
            if (nearbyPlayers.nearest == null) {
                // If there is no nearest player, remove from the map and execute on the main thread.
                chunkExecutorMap.remove(pairHash);
                return KeyedPoolThreadManager.MAIN_THREAD_KEY;
            }
            // Executor exists and there's a nearest player.
            int executor = chunkExecutorMap.get(pairHash);
            int hash = DistanceThreadingPlayerUtil.playerHashcode(nearbyPlayers.nearest);
            if (hash != executor) {
                // Chunk executor is not the nearest player's executor.
                int nearestPlayerExecutor = playerExecutorMap.getInt(nearbyPlayers.nearest);
                chunkExecutorMap.put(pairHash, nearestPlayerExecutor);
                return nearestPlayerExecutor;
            }
            // Chunk executor is the nearest player's executor.
            return executor;
        }

        // If the executor does not exist.
        // Normally, an executor should always exist unless the chunk is newly ticketed/force-loaded.
        if (nearbyPlayers.nearest == null)
            // If there *is no nearest player*, this only happens when no players are online.
            return KeyedPoolThreadManager.MAIN_THREAD_KEY;

        if (playerExecutorMap.containsKey(nearbyPlayers.nearest)) {
            // Assign to the nearest player.
            chunkExecutorMap.put(pairHash, playerExecutorMap.getInt(nearbyPlayers.nearest));
            return synchronizeExecutorToAdjacentChunks(chunk, pairHash);
        }

        // This isn't going well so far...
        // The nearest player doesn't have an executor.
        SpoolLogger.warn("Nearest player does not have an executor for chunk {}.", chunk);
        SpoolLogger.warn("Checking for executor map instability...");
        checkForPlayerInstability();
        if (playerExecutorMap.containsKey(nearbyPlayers.nearest)) {
            // Assign to the nearest player (post rebuild).
            chunkExecutorMap.put(pairHash, playerExecutorMap.getInt(nearbyPlayers.nearest));
            return synchronizeExecutorToAdjacentChunks(chunk, pairHash);
        } else {
            // At this point, it's too far gone. We've tried rebuilding the map, and it hasn't worked, hinting
            // at a massive underlying issue.
            throw new IllegalStateException("Player executor map does not contain nearest player, even after rebuild!");
        }
    }

    private static boolean shouldCheckPlayerInstability = false;

    public static void onTick() {
        if (shouldCheckPlayerInstability) {
            checkForPlayerInstability();
            shouldCheckPlayerInstability = false;
        }
        if (shouldCheckChunkInstability) {
            checkForChunkInstability();
            shouldCheckChunkInstability = false;
        }
    }

    private static int getThread(Nearby nearby) {
        nearby.nearby.remove(nearby.nearest);
        return getThread(nearby.nearest, nearby);
    }

    private static int getThread(EntityPlayer player, Nearby nearestPlayers) {
        shouldCheckPlayerInstability = true;
        boolean containsKey = playerExecutorMap.containsKey(player);
        if (containsKey && nearestPlayers.nearby.isEmpty()) {
            // Executor exists and no players are within range.
            int executor = playerExecutorMap.getInt(player);
            int hash = DistanceThreadingPlayerUtil.playerHashcode(player);
            if (hash != executor) {
                // Player executor is not its own.
                keyedPool.addKeyedThread(hash, "Distance-Executor-" + hash);
                playerExecutorMap.put(player, hash);
                return hash;
            }
            return executor;
        }

        if (containsKey)
            // Executor exists, but there are players within range.
            return synchronizePlayersToPlayerExecutor(nearestPlayers.nearby, player);

        if (nearestPlayers.nearby.isEmpty()) {
            // Executor does not exist for this player and no players are in range.
            // Create a new thread in the pool.
            int hash = DistanceThreadingPlayerUtil.playerHashcode(player);
            keyedPool.addKeyedThread(hash, "Distance-Executor-" + hash);
            playerExecutorMap.put(player, hash);
            return hash;
        }

        // If the executor does not exist for this player and there are players in range.
        return synchronizePlayersToPlayerExecutor(nearestPlayers.nearby, player);
    }

    private static void synchronizePlayersToPlayerExecutorWithPrioritized(List<EntityPlayer> nearestPlayers,
        EntityPlayer prioritized) {
        int prioritizedExecutor = playerExecutorMap.getInt(prioritized);
        for (EntityPlayer nearbyPlayer : nearestPlayers) {
            if (prioritizedExecutor != playerExecutorMap.getInt(nearbyPlayer))
                // If nearby players do not have the same executor,
                // Assign the nearby players to the prioritized player's executor.
                playerExecutorMap.put(nearbyPlayer, prioritizedExecutor);
        }
    }

    private static int synchronizePlayersToPlayerExecutor(List<EntityPlayer> nearestPlayers,
        EntityPlayer currentPlayer) {
        EntityPlayer prioritized = DistanceThreadingPlayerUtil.getPrioritized(nearestPlayers, currentPlayer); // include
                                                                                                              // self
        int prioritizedExecutor;
        if (!playerExecutorMap.containsKey(prioritized)) {
            prioritizedExecutor = DistanceThreadingPlayerUtil.playerHashcode(prioritized);
            keyedPool.addKeyedThread(prioritizedExecutor, "Distance-Executor-" + prioritizedExecutor);
            playerExecutorMap.put(prioritized, prioritizedExecutor);
        } else prioritizedExecutor = playerExecutorMap.getInt(prioritized);

        // noinspection ForLoopReplaceableByForEach
        for (int i = 0, nearestPlayersSize = nearestPlayers.size(); i < nearestPlayersSize; i++) {
            EntityPlayer nearbyPlayer = nearestPlayers.get(i);
            int executor = playerExecutorMap.getInt(nearbyPlayer);
            if (prioritizedExecutor != executor) {
                // If nearby players do not have the same executor...
                // Remove that executor from the pool.
                keyedPool.removeKeyedThread(executor);
                // Assign the nearby players to the prioritized player's executor.
                playerExecutorMap.put(nearbyPlayer, prioritizedExecutor);
            }
        }
        playerExecutorMap.putIfAbsent(currentPlayer, prioritizedExecutor);
        return prioritizedExecutor; // Now synchronized across all the players.
    }

    private static int synchronizeExecutorToAdjacentChunks(Chunk chunk, long chunkHash) {
        int chunkExecutor;
        if (chunkExecutorMap.containsKey(chunkHash)) chunkExecutor = chunkExecutorMap.get(chunkHash);
        else {
            Nearby nearbyPlayers = DistanceThreadingPlayerUtil.getNearestPlayers(chunk, false);
            chunkExecutor = playerExecutorMap.getInt(nearbyPlayers.nearest);
        }

        chunkExecutorMap.put(chunkHash, chunkExecutor);
        // lord have mercy.
        int count = floodFillForceLoadedChunks(chunk.worldObj, chunkHash);
        if (count > 1) SpoolLogger.debug("DistanceThreadingUtil flood-filled {} persistent chunks.", count);
        return chunkExecutor; // Now synchronized across all the players.
    }

    private static final int[][] CARDINAL_OFFSETS = { { 0, 1 }, { 1, 0 }, { 0, -1 }, { -1, 0 } };

    private static int floodFillForceLoadedChunks(final World worldObj, final long chunkHash) {
        return floodFillForceLoadedChunks(DistanceThreadingChunkUtil.getProcessedPersistentChunks(worldObj), chunkHash);
    }

    @VisibleForTesting
    public static int floodFillForceLoadedChunks(final NonBlockingHashSet<Long> chunks, final long chunkHash) {
        int seedExecutor = chunkExecutorMap.get(chunkHash); // Seed's executor
        LongSet visited = new LongOpenHashSet(); // Visited chunks cache
        visited.add(chunkHash); // Initial seed chunk
        int initialSize = chunkExecutorMap.size(); // Initial executor map size

        LongArrayFIFOQueue taskQueue = new LongArrayFIFOQueue(); // Task queue.
        taskQueue.enqueue(chunkHash); // Seed's hash.
        // Setup done.

        // If it didn't process anything new, we know we're done.
        while (!taskQueue.isEmpty()) {
            long targetHash = taskQueue.dequeueLong();
            for (int[] offset : CARDINAL_OFFSETS) {
                long adjacentHash = DistanceThreadingCommonUtil.addToChunkHash(targetHash, offset[0], offset[1]);
                // Skip if we've already processed this chunk
                if (!visited.add(adjacentHash)) continue;
                // Skip if the chunk isn't persistent
                if (chunks.contains(adjacentHash)) {
                    chunkExecutorMap.put(adjacentHash, seedExecutor);
                    taskQueue.enqueue(adjacentHash);
                }
            }
        }
        int numProcessedTotal = chunkExecutorMap.size() - initialSize;
        return numProcessedTotal + 1; // Seed chunk.
    }

    private static void checkForPlayerInstability() {
        if (playerExecutorMap.size() != MinecraftServer.getServer()
            .getCurrentPlayerCount()) {
            SpoolLogger.error("Player executor map does not match player count!");
            SpoolLogger.error("Player executor map size: {}", playerExecutorMap.size());
            SpoolLogger.error(
                "Player list size: {}",
                MinecraftServer.getServer()
                    .getCurrentPlayerCount());
            if (DistanceThreadingConfig.resolveConflicts) {
                SpoolLogger.error("Conflict resolution enabled, attempting to rebuild the player executor map...");

                // Step 1: Restart the pool.
                keyedPool.terminatePool();
                keyedPool.startPoolIfNeeded();

                // Step 2: Reconstruct the map from scratch.
                if (rebuildPlayerMap()) {
                    SpoolLogger.error("Player executor map rebuilt failed.");
                    SpoolLogger.error("Player executor map size (after rebuild): {}", playerExecutorMap.size());
                    SpoolLogger.error(
                        "Player list size: {}",
                        MinecraftServer.getServer()
                            .getCurrentPlayerCount());
                    throw new IllegalStateException(
                        "Player executor map does not match player count (failed rebuild)!");
                }
                SpoolLogger.warn("Player executor map rebuilt successfully!");
            } else throw new IllegalStateException("Player executor map does not match player count!");
        }
    }

    private static void checkForChunkInstability() {
        if (MinecraftServer.getServer()
            .getCurrentPlayerCount() == 0) {
            // Make sure that nothing remains.
            // If nobody is online, they will all be removed in the future when they tick anyway.
            chunkExecutorMap.clear();
            return;
        }
        int intendedSize = DistanceThreadingChunkUtil.getForceLoadedChunksCount();
        if (chunkExecutorMap.size() != intendedSize) {
            SpoolLogger.error("Chunk executor map does not match force-loaded chunk count!");
            SpoolLogger.error("Chunk executor map size: {}", chunkExecutorMap.size());
            SpoolLogger.error("Force-loaded chunk count: {}", intendedSize);
            if (DistanceThreadingConfig.resolveConflicts) {
                SpoolLogger.error("Conflict resolution enabled, attempting to rebuild the chunk executor map...");

                // Step 1: Restart the pool.
                keyedPool.terminatePool();
                keyedPool.startPoolIfNeeded();

                // Step 2: Reconstruct the map from scratch.
                if (rebuildChunkMap()) {
                    SpoolLogger.error("Chunk executor map rebuild failed.");
                    SpoolLogger.error("Chunk executor map size (after rebuild): {}", chunkExecutorMap.size());
                    SpoolLogger.error("Force-loaded chunk count: {}", intendedSize);
                    throw new IllegalStateException(
                        "Chunk executor map does not match intended chunk count (failed rebuild)!");
                }
                SpoolLogger.warn("Chunk executor map rebuilt successfully!");
            } else throw new IllegalStateException("Chunk executor map does not match intended chunk count!");
        }
    }

    public static boolean rebuildPlayerMap() {
        // Remove any erroneous data from the executor map.
        playerExecutorMap.clear();

        for (EntityPlayer player : MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList) {
            if (playerExecutorMap.containsKey(player)) continue;
            Nearby list = DistanceThreadingPlayerUtil.getNearestPlayers(player, false);
            if (list.nearby.isEmpty()) {
                int hash = DistanceThreadingPlayerUtil.playerHashcode(player);
                playerExecutorMap.put(player, hash);
                keyedPool.addKeyedThread(hash, "Distance-Executor-" + hash);
            } else {
                EntityPlayer prioritizedPlayer = DistanceThreadingPlayerUtil.getPrioritized(list.nearby, player);
                int hash = DistanceThreadingPlayerUtil.playerHashcode(prioritizedPlayer);
                playerExecutorMap.put(prioritizedPlayer, hash);
                keyedPool.addKeyedThread(hash, "Distance-Executor-" + hash);
                synchronizePlayersToPlayerExecutorWithPrioritized(list.nearby, prioritizedPlayer);
            }
        }
        return playerExecutorMap.size() != MinecraftServer.getServer()
            .getCurrentPlayerCount();
    }

    public static boolean rebuildChunkMap() {
        // Remove any erroneous data from the executor map.
        chunkExecutorMap.clear();

        DistanceThreadingChunkUtil.forAllForceLoadedChunks(chunkProcessingUnit -> {
            World world = chunkProcessingUnit.world();
            int[] coords = DistanceThreadingChunkUtil.undoChunkHash(chunkProcessingUnit.chunk()); // x/y
            Chunk chunk = world.getChunkFromChunkCoords(coords[0], coords[1]);

            Nearby nearby = DistanceThreadingPlayerUtil.getNearestPlayers(chunk, false);
            if (nearby.nearby.isEmpty()) {
                // There is a chunk that is force-loaded while also no players are nearby.
                // Execute this on the main thread.
                chunkExecutorMap.put(chunkProcessingUnit.chunk(), KeyedPoolThreadManager.MAIN_THREAD_KEY);
            } else {
                if (nearby.nearest != null) {
                    int hash = DistanceThreadingPlayerUtil.playerHashcode(nearby.nearest);
                    chunkExecutorMap.put(chunkProcessingUnit.chunk(), hash);
                    synchronizeExecutorToAdjacentChunks(chunk, chunkProcessingUnit.chunk());
                }
            }
        });
        if (MinecraftServer.getServer()
            .getCurrentPlayerCount() == 0) return true;
        int intendedSize = DistanceThreadingChunkUtil.getForceLoadedChunksCount();
        return chunkExecutorMap.size() != intendedSize;
    }

    // Records
    // Thanks Jabel!
    @Desugar
    record WorldChunkData(World world, NonBlockingHashSet<Long> chunks) {

        public Stream<ChunkProcessingUnit> getChunkUnitStream() {
            return this.chunks()
                .stream()
                .map(this::createChunkUnit);
        }

        // Lambda replacement.
        private ChunkProcessingUnit createChunkUnit(long intPair) {
            return new ChunkProcessingUnit(this.world(), intPair);
        }
    }

    @Desugar
    record ChunkProcessingUnit(World world, long chunk) {}

    @Desugar
    public record Nearby(EntityPlayer nearest, List<EntityPlayer> nearby, boolean usedIgnoreLimit) {}
}
