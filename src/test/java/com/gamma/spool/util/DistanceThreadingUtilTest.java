package com.gamma.spool.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.minecraft.world.ChunkCoordIntPair;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.config.DistanceThreadingConfig;
import com.gamma.spool.config.ThreadManagerConfig;
import com.gamma.spool.thread.KeyedPoolThreadManager;
import com.gamma.spool.util.distance.DistanceThreadingChunkUtil;
import com.gamma.spool.util.distance.DistanceThreadingUtil;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * Tests for the {@link DistanceThreadingUtil} class.
 */
public class DistanceThreadingUtilTest {

    private KeyedPoolThreadManager keyedPool;
    private static final String TEST_POOL_NAME = "TestDistanceThreadingPool";
    private static final int TEST_THREAD_LIMIT = 8;

    @Before
    public void setUp() {
        ThreadManagerConfig.globalRunningSingleThreadTimeout = 500;
        ThreadManagerConfig.globalTerminatingSingleThreadTimeout = 2000;
        DebugConfig.debug = true;
        DebugConfig.debugLogging = true;

        // Set up KeyedPoolThreadManager
        keyedPool = new KeyedPoolThreadManager(TEST_POOL_NAME, TEST_THREAD_LIMIT);
    }

    @After
    public void tearDown() {
        if (keyedPool.isStarted()) {
            keyedPool.terminatePool();
        }
        if (DistanceThreadingUtil.isInitialized()) DistanceThreadingUtil.teardown();
    }

    /**
     * Test that initialization properly sets the keyedPool, and that termination properly clears all values.
     */
    @Test
    public void testInitAndTeardown() {

        // Initialize DistanceThreadingUtil with our test pool
        DistanceThreadingUtil.init(keyedPool);

        assertTrue("DistanceThreadingUtil should be initialized", DistanceThreadingUtil.isInitialized());

        // Verify that the pool was set correctly
        assertEquals("KeyedPool should be set correctly", keyedPool, DistanceThreadingUtil.getKeyedPool());

        DistanceThreadingUtil.teardown();

        assertFalse("DistanceThreadingUtil should not be initialized", DistanceThreadingUtil.isInitialized());
        assertNull("KeyedPool should be null", DistanceThreadingUtil.getKeyedPool());
        assertNotNull("Player executor map should be initialized", DistanceThreadingUtil.getPlayerExecutorMap());
        assertNotNull("Chunk executor map should be initialized", DistanceThreadingUtil.getChunkExecutorMap());
        assertEquals(
            "Player executor map should be empty",
            0,
            DistanceThreadingUtil.getPlayerExecutorMap()
                .size());
        assertEquals(
            "Chunk executor map should be empty",
            0,
            DistanceThreadingUtil.getChunkExecutorMap()
                .size());
    }

    /**
     * Test that the static buckets are initialized properly.
     */
    @Test
    public void testMapsInitialization() {
        // Initialize DistanceThreadingUtil with our test pool
        DistanceThreadingUtil.init(keyedPool);

        // Verify that the buckets are initialized
        assertNotNull("Player executor map should be initialized", DistanceThreadingUtil.getPlayerExecutorMap());
        assertNotNull("Chunk executor map should be initialized", DistanceThreadingUtil.getChunkExecutorMap());

        // Verify that the buckets are empty at the start
        assertEquals(
            "Player executor map should be empty",
            0,
            DistanceThreadingUtil.getPlayerExecutorMap()
                .size());
        assertEquals(
            "Chunk executor map should be empty",
            0,
            DistanceThreadingUtil.getChunkExecutorMap()
                .size());
    }

    /**
     * Test the floodFillForceLoadedChunks method with an empty chunks set in sequential mode.
     */
    @Test
    public void testFloodFillEmptyChunksSequential() {
        // Initialize DistanceThreadingUtil with our test pool
        DistanceThreadingUtil.init(keyedPool);

        // Set to sequential mode
        DistanceThreadingConfig.streamParallelizationLevel = 0;

        // Create an empty chunks set
        LongSet chunks = new LongArraySet();

        // Create a seed chunk hash
        long seedChunkHash = ChunkCoordIntPair.chunkXZ2Int(0, 0);

        // Add the seed chunk to the executor map
        DistanceThreadingUtil.getChunkExecutorMap()
            .put(seedChunkHash, 1);

        // Call the method
        int numProcessed = DistanceThreadingUtil.floodFillForceLoadedChunks(chunks, seedChunkHash);

        // Verify that only the seed chunk was processed
        assertEquals("Only the seed chunk should be processed", 1, numProcessed);
    }

    /**
     * Test the floodFillForceLoadedChunks method with an empty chunks set in parallel mode.
     */
    @Test
    public void testFloodFillEmptyChunksParallel() {
        // Initialize DistanceThreadingUtil with our test pool
        DistanceThreadingUtil.init(keyedPool);

        // Set to parallel mode
        DistanceThreadingConfig.streamParallelizationLevel = 2;

        // Create an empty chunks set
        LongSet chunks = new LongArraySet();

        // Create a seed chunk hash
        long seedChunkHash = ChunkCoordIntPair.chunkXZ2Int(0, 0);

        // Add the seed chunk to the executor map
        DistanceThreadingUtil.getChunkExecutorMap()
            .put(seedChunkHash, 1);

        // Call the test-specific method
        int numProcessed = DistanceThreadingUtil.floodFillForceLoadedChunks(chunks, seedChunkHash);

        // Verify that only the seed chunk was processed
        assertEquals("Only the seed chunk should be processed", 1, numProcessed);
    }

    /**
     * Test the floodFillForceLoadedChunks method with a single chunk in sequential mode.
     */
    @Test
    public void testFloodFillSingleChunkSequential() {
        // Initialize DistanceThreadingUtil with our test pool
        DistanceThreadingUtil.init(keyedPool);

        // Set to sequential mode
        DistanceThreadingConfig.streamParallelizationLevel = 0;

        // Create a chunks set with a single chunk
        LongSet chunks = new LongArraySet();
        long chunkHash = ChunkCoordIntPair.chunkXZ2Int(0, 0);
        chunks.add(chunkHash);

        // Add the seed chunk to the executor map
        DistanceThreadingUtil.getChunkExecutorMap()
            .put(chunkHash, 1);

        // Call the method
        int numProcessed = DistanceThreadingUtil.floodFillForceLoadedChunks(chunks, chunkHash);

        // Verify that only the seed chunk was processed
        assertEquals("Only the seed chunk should be processed", 1, numProcessed);
    }

    /**
     * Test the floodFillForceLoadedChunks method with a single chunk in parallel mode.
     */
    @Test
    public void testFloodFillSingleChunkParallel() {
        // Initialize DistanceThreadingUtil with our test pool
        DistanceThreadingUtil.init(keyedPool);

        // Set to parallel mode
        DistanceThreadingConfig.streamParallelizationLevel = 2;

        // Create a chunks set with a single chunk
        LongSet chunks = new LongArraySet();
        long chunkHash = ChunkCoordIntPair.chunkXZ2Int(0, 0);
        chunks.add(chunkHash);

        // Add the seed chunk to the executor map
        DistanceThreadingUtil.getChunkExecutorMap()
            .put(chunkHash, 1);

        // Call the method
        int numProcessed = DistanceThreadingUtil.floodFillForceLoadedChunks(chunks, chunkHash);

        // Verify that only the seed chunk was processed
        assertEquals("Only the seed chunk should be processed", 1, numProcessed);
    }

    /**
     * Test the floodFillForceLoadedChunks method with multiple connected chunks in sequential mode.
     */
    @Test
    public void testFloodFillConnectedChunksSequential() {
        // Initialize DistanceThreadingUtil with our test pool
        DistanceThreadingUtil.init(keyedPool);

        // Set to sequential mode
        DistanceThreadingConfig.streamParallelizationLevel = 0;

        // Create a chunk set with connected chunks
        LongSet chunks = new LongArraySet();
        long seedChunkHash = ChunkCoordIntPair.chunkXZ2Int(0, 0);
        chunks.add(seedChunkHash);

        // Add adjacent chunks in all four cardinal directions
        chunks.add(ChunkCoordIntPair.chunkXZ2Int(1, 0)); // East
        chunks.add(ChunkCoordIntPair.chunkXZ2Int(0, 1)); // North
        chunks.add(ChunkCoordIntPair.chunkXZ2Int(-1, 0)); // West
        chunks.add(ChunkCoordIntPair.chunkXZ2Int(0, -1)); // South

        // Add the seed chunk to the executor map
        DistanceThreadingUtil.getChunkExecutorMap()
            .put(seedChunkHash, 1);

        // Call the method
        int numProcessed = DistanceThreadingUtil.floodFillForceLoadedChunks(chunks, seedChunkHash);

        // Verify that all 5 chunks were processed
        assertEquals("All 5 chunks should be processed", 5, numProcessed);

        // Verify that all chunks have the same executor
        assertEquals(
            "East chunk should have the same executor",
            1,
            DistanceThreadingUtil.getChunkExecutorMap()
                .get(ChunkCoordIntPair.chunkXZ2Int(1, 0)));
        assertEquals(
            "North chunk should have the same executor",
            1,
            DistanceThreadingUtil.getChunkExecutorMap()
                .get(ChunkCoordIntPair.chunkXZ2Int(0, 1)));
        assertEquals(
            "West chunk should have the same executor",
            1,
            DistanceThreadingUtil.getChunkExecutorMap()
                .get(ChunkCoordIntPair.chunkXZ2Int(-1, 0)));
        assertEquals(
            "South chunk should have the same executor",
            1,
            DistanceThreadingUtil.getChunkExecutorMap()
                .get(ChunkCoordIntPair.chunkXZ2Int(0, -1)));
    }

    /**
     * Test the floodFillForceLoadedChunks method with multiple connected chunks in parallel mode.
     */
    @Test
    public void testFloodFillConnectedChunksParallel() {
        // Initialize DistanceThreadingUtil with our test pool
        DistanceThreadingUtil.init(keyedPool);

        // Set to parallel mode
        DistanceThreadingConfig.streamParallelizationLevel = 2;

        // Create a chunks set with connected chunks
        LongSet chunks = new LongArraySet();
        long seedChunkHash = ChunkCoordIntPair.chunkXZ2Int(0, 0);
        chunks.add(seedChunkHash);

        // Add adjacent chunks in all four cardinal directions
        chunks.add(ChunkCoordIntPair.chunkXZ2Int(1, 0)); // East
        chunks.add(ChunkCoordIntPair.chunkXZ2Int(0, 1)); // North
        chunks.add(ChunkCoordIntPair.chunkXZ2Int(-1, 0)); // West
        chunks.add(ChunkCoordIntPair.chunkXZ2Int(0, -1)); // South

        // Add the seed chunk to the executor map
        DistanceThreadingUtil.getChunkExecutorMap()
            .put(seedChunkHash, 1);

        // Call the method
        int numProcessed = DistanceThreadingUtil.floodFillForceLoadedChunks(chunks, seedChunkHash);

        // Verify that all 5 chunks were processed
        assertEquals("All 5 chunks should be processed", 5, numProcessed);

        // Verify that all chunks have the same executor
        assertEquals(
            "East chunk should have the same executor",
            1,
            DistanceThreadingUtil.getChunkExecutorMap()
                .get(ChunkCoordIntPair.chunkXZ2Int(1, 0)));
        assertEquals(
            "North chunk should have the same executor",
            1,
            DistanceThreadingUtil.getChunkExecutorMap()
                .get(ChunkCoordIntPair.chunkXZ2Int(0, 1)));
        assertEquals(
            "West chunk should have the same executor",
            1,
            DistanceThreadingUtil.getChunkExecutorMap()
                .get(ChunkCoordIntPair.chunkXZ2Int(-1, 0)));
        assertEquals(
            "South chunk should have the same executor",
            1,
            DistanceThreadingUtil.getChunkExecutorMap()
                .get(ChunkCoordIntPair.chunkXZ2Int(0, -1)));
    }

    /**
     * Test the floodFillForceLoadedChunks method with disconnected chunks in sequential mode.
     */
    @Test
    public void testFloodFillDisconnectedChunksSequential() {
        // Initialize DistanceThreadingUtil with our test pool
        DistanceThreadingUtil.init(keyedPool);

        // Set to sequential mode
        DistanceThreadingConfig.streamParallelizationLevel = 0;

        // Create a chunks set with disconnected chunks
        LongSet chunks = new LongArraySet();
        long seedChunkHash = ChunkCoordIntPair.chunkXZ2Int(0, 0);
        chunks.add(seedChunkHash);

        // Add disconnected chunks (not adjacent to the seed chunk)
        chunks.add(ChunkCoordIntPair.chunkXZ2Int(2, 0)); // 2 chunks east
        chunks.add(ChunkCoordIntPair.chunkXZ2Int(0, 2)); // 2 chunks north

        // Add the seed chunk to the executor map
        DistanceThreadingUtil.getChunkExecutorMap()
            .put(seedChunkHash, 1);

        // Call the method
        int numProcessed = DistanceThreadingUtil.floodFillForceLoadedChunks(chunks, seedChunkHash);

        // Verify that only the seed chunk was processed (disconnected chunks shouldn't be processed)
        assertEquals("Only the seed chunk should be processed", 1, numProcessed);

        // Verify that disconnected chunks don't have the executor
        assertFalse(
            "Disconnected chunk should not have an executor",
            DistanceThreadingUtil.getChunkExecutorMap()
                .containsKey(ChunkCoordIntPair.chunkXZ2Int(2, 0)));
        assertFalse(
            "Disconnected chunk should not have an executor",
            DistanceThreadingUtil.getChunkExecutorMap()
                .containsKey(ChunkCoordIntPair.chunkXZ2Int(0, 2)));
    }

    /**
     * Test the floodFillForceLoadedChunks method with disconnected chunks in parallel mode.
     */
    @Test
    public void testFloodFillDisconnectedChunksParallel() {
        // Initialize DistanceThreadingUtil with our test pool
        DistanceThreadingUtil.init(keyedPool);

        // Set to parallel mode
        DistanceThreadingConfig.streamParallelizationLevel = 2;

        // Create a chunks set with disconnected chunks
        LongSet chunks = new LongArraySet();
        long seedChunkHash = ChunkCoordIntPair.chunkXZ2Int(0, 0);
        chunks.add(seedChunkHash);

        // Add disconnected chunks (not adjacent to the seed chunk)
        chunks.add(ChunkCoordIntPair.chunkXZ2Int(2, 0)); // 2 chunks east
        chunks.add(ChunkCoordIntPair.chunkXZ2Int(0, 2)); // 2 chunks north

        // Add the seed chunk to the executor map
        DistanceThreadingUtil.getChunkExecutorMap()
            .put(seedChunkHash, 1);

        // Call the method
        int numProcessed = DistanceThreadingUtil.floodFillForceLoadedChunks(chunks, seedChunkHash);

        // Verify that only the seed chunk was processed (disconnected chunks shouldn't be processed)
        assertEquals("Only the seed chunk should be processed", 1, numProcessed);

        // Verify that disconnected chunks don't have the executor
        assertFalse(
            "Disconnected chunk should not have an executor",
            DistanceThreadingUtil.getChunkExecutorMap()
                .containsKey(ChunkCoordIntPair.chunkXZ2Int(2, 0)));
        assertFalse(
            "Disconnected chunk should not have an executor",
            DistanceThreadingUtil.getChunkExecutorMap()
                .containsKey(ChunkCoordIntPair.chunkXZ2Int(0, 2)));
    }

    public static final int RADIUS = 10;

    /**
     * Test the floodFillForceLoadedChunks method with multiple connected chunks in sequential mode.
     */
    @Test
    public void testFloodFillManyConnectedChunksSequential() {
        // Initialize DistanceThreadingUtil with our test pool
        DistanceThreadingUtil.init(keyedPool);

        // Create a chunks set with connected chunks
        LongSet chunks = new LongArraySet();
        long seedChunkHash = ChunkCoordIntPair.chunkXZ2Int(0, 0);
        chunks.add(seedChunkHash);

        for (int x = -RADIUS; x < RADIUS; x++) {
            for (int y = -RADIUS; y < RADIUS; y++) {
                chunks.add(ChunkCoordIntPair.chunkXZ2Int(x, y));
            }
        }

        // Add the seed chunk to the executor map
        DistanceThreadingUtil.getChunkExecutorMap()
            .put(seedChunkHash, 1);

        // Call the method
        long nanoTime = System.nanoTime();
        int numProcessed = DistanceThreadingUtil.floodFillForceLoadedChunks(chunks, seedChunkHash);
        long nanoseconds = System.nanoTime() - nanoTime;

        // Verify that all 5 chunks were processed
        assertEquals("All " + chunks.size() + " chunks should be processed", chunks.size(), numProcessed);

        // Verify that all chunks have the same executor
        for (Long2IntMap.Entry entry : DistanceThreadingUtil.getChunkExecutorMap()
            .long2IntEntrySet()) {
            long hash = entry.getLongKey();
            int[] coords = DistanceThreadingChunkUtil.undoChunkHash(hash);
            assertEquals(
                "Chunk should have the same executor: (" + coords[0] + ", " + coords[1] + ")",
                1,
                entry.getIntValue());
        }

        System.out.println(
            "Time to process " + chunks.size()
                + " chunks (sequential): "
                + nanoseconds / 1000
                + "μs ("
                + nanoseconds / 1000000
                + "ms)");
    }

    /**
     * Test the floodFillForceLoadedChunks method with multiple connected chunks in parallel mode.
     */
    @Test
    public void testFloodFillManyConnectedChunksParallel() {
        // Initialize DistanceThreadingUtil with our test pool
        DistanceThreadingUtil.init(keyedPool);

        // Set to parallel mode
        DistanceThreadingConfig.streamParallelizationLevel = 2;

        // Create a chunks set with connected chunks
        LongSet chunks = new LongArraySet();
        long seedChunkHash = ChunkCoordIntPair.chunkXZ2Int(0, 0);
        chunks.add(seedChunkHash);

        for (int x = -RADIUS; x < RADIUS; x++) {
            for (int y = -RADIUS; y < RADIUS; y++) {
                chunks.add(ChunkCoordIntPair.chunkXZ2Int(x, y));
            }
        }

        // Add the seed chunk to the executor map
        DistanceThreadingUtil.getChunkExecutorMap()
            .put(seedChunkHash, 1);

        // Call the method
        long nanoTime = System.nanoTime();
        int numProcessed = DistanceThreadingUtil.floodFillForceLoadedChunks(chunks, seedChunkHash);
        long nanoseconds = System.nanoTime() - nanoTime;

        // Verify that all 5 chunks were processed
        assertEquals("All " + chunks.size() + " chunks should be processed", chunks.size(), numProcessed);

        // Verify that all chunks have the same executor
        for (Long2IntMap.Entry entry : DistanceThreadingUtil.getChunkExecutorMap()
            .long2IntEntrySet()) {
            long hash = entry.getLongKey();
            int[] coords = DistanceThreadingChunkUtil.undoChunkHash(hash);
            assertEquals(
                "Chunk should have the same executor: (" + coords[0] + ", " + coords[1] + ")",
                1,
                entry.getIntValue());
        }

        System.out.println(
            "Time to process " + chunks.size()
                + " chunks (parallel): "
                + nanoseconds / 1000
                + "μs ("
                + nanoseconds / 1000000
                + "ms)");
    }
}
