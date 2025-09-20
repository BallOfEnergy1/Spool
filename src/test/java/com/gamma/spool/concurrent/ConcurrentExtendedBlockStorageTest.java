package com.gamma.spool.concurrent;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the {@link ConcurrentExtendedBlockStorage} class.
 * This class tests concurrency, speed, and data accuracy of the ConcurrentExtendedBlockStorage.
 */
public class ConcurrentExtendedBlockStorageTest {

    private static final int NUM_THREADS = 8;
    private static final int OPERATIONS_PER_THREAD = 1000;
    private static final int SPEED_TEST_INSTANCE_COUNT = 16;

    private ConcurrentExtendedBlockStorage concurrentStorage;
    private ExtendedBlockStorage standardStorage;
    private ExecutorService executorService;

    @Before
    public void setUp() {
        // Initialize with Y-base 0 and skylight enabled
        concurrentStorage = new ConcurrentExtendedBlockStorage(0, true);
        standardStorage = new ExtendedBlockStorage(0, true);
        executorService = Executors.newFixedThreadPool(NUM_THREADS);
    }

    @After
    public void tearDown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread()
                .interrupt();
        }
    }

    /**
     * Test that the constructor properly initializes the storage.
     */
    @Test
    public void testConstructor() {
        assertNotNull("ConcurrentExtendedBlockStorage should not be null", concurrentStorage);
        assertEquals("Y-base should be 0", 0, concurrentStorage.getYLocation());
        assertTrue("Storage should be empty initially", concurrentStorage.isEmpty());
    }

    /**
     * Test the get and set methods for blocks.
     */
    @Test
    public void testBlockGetAndSet() {
        // Set blocks in a pattern

        assertTrue("isEmpty should be true before adding blocks", concurrentStorage.isEmpty());

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    // Use different blocks based on coordinates
                    int block = (x + y + z) % 3 == 0 ? 0 : (x + y + z) % 3 == 1 ? 1 : 2;

                    concurrentStorage.func_150818_a(x, y, z, block);
                }
            }
        }

        assertFalse("isEmpty should be false after adding blocks", concurrentStorage.isEmpty());

        // Verify blocks
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    int expectedBlock = (x + y + z) % 3 == 0 ? 0 : (x + y + z) % 3 == 1 ? 1 : 2;

                    int actualBlock = concurrentStorage.getBlockIntByExtId(x, y, z);

                    assertEquals(
                        "Block at position (" + x + "," + y + "," + z + ") should match expected block",
                        expectedBlock,
                        actualBlock);
                }
            }
        }
    }

    /**
     * Test the get and set methods for metadata.
     */
    @Test
    public void testMetadataGetAndSet() {
        // Set metadata in a pattern
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    int metadata = (x + y + z) % 16; // Values 0-15

                    concurrentStorage.setExtBlockMetadata(x, y, z, metadata);
                    standardStorage.setExtBlockMetadata(x, y, z, metadata);
                }
            }
        }

        // Verify metadata
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    int expectedMetadata = (x + y + z) % 16;

                    int actualMetadata = concurrentStorage.getExtBlockMetadata(x, y, z);
                    int standardMetadata = standardStorage.getExtBlockMetadata(x, y, z);

                    assertEquals(
                        "Metadata at position (" + x + "," + y + "," + z + ") should match expected value",
                        expectedMetadata,
                        actualMetadata);
                    assertEquals(
                        "Metadata at position (" + x + "," + y + "," + z + ") should match standard storage",
                        standardMetadata,
                        actualMetadata);
                }
            }
        }
    }

    /**
     * Test the get and set methods for block light.
     */
    @Test
    public void testBlockLightGetAndSet() {
        // Set block light in a pattern
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    int lightValue = (x + y + z) % 16; // Values 0-15

                    concurrentStorage.setExtBlocklightValue(x, y, z, lightValue);
                    standardStorage.setExtBlocklightValue(x, y, z, lightValue);
                }
            }
        }

        // Verify block light
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    int expectedLight = (x + y + z) % 16;

                    int actualLight = concurrentStorage.getExtBlocklightValue(x, y, z);
                    int standardLight = standardStorage.getExtBlocklightValue(x, y, z);

                    assertEquals(
                        "Block light at position (" + x + "," + y + "," + z + ") should match expected value",
                        expectedLight,
                        actualLight);
                    assertEquals(
                        "Block light at position (" + x + "," + y + "," + z + ") should match standard storage",
                        standardLight,
                        actualLight);
                }
            }
        }
    }

    /**
     * Test the get and set methods for sky light.
     */
    @Test
    public void testSkyLightGetAndSet() {
        // Set sky light in a pattern
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    int lightValue = (x + y + z) % 16; // Values 0-15

                    concurrentStorage.setExtSkylightValue(x, y, z, lightValue);
                    standardStorage.setExtSkylightValue(x, y, z, lightValue);
                }
            }
        }

        // Verify sky light
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    int expectedLight = (x + y + z) % 16;

                    int actualLight = concurrentStorage.getExtSkylightValue(x, y, z);
                    int standardLight = standardStorage.getExtSkylightValue(x, y, z);

                    assertEquals(
                        "Sky light at position (" + x + "," + y + "," + z + ") should match expected value",
                        expectedLight,
                        actualLight);
                    assertEquals(
                        "Sky light at position (" + x + "," + y + "," + z + ") should match standard storage",
                        standardLight,
                        actualLight);
                }
            }
        }
    }

    /**
     * Test concurrent access to the storage.
     */
    @Test
    public void testConcurrentAccess() throws InterruptedException {
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);
        final AtomicBoolean hasErrors = new AtomicBoolean(false);
        final List<String> errors = new ArrayList<>();

        // Create threads that will concurrently access the storage
        for (int t = 0; t < NUM_THREADS; t++) {
            final int threadId = t;
            executorService.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    Random random = new Random(threadId); // Different seed for each thread

                    // Perform operations
                    for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                        int x = random.nextInt(16);
                        int y = random.nextInt(16);
                        int z = random.nextInt(16);

                        // Randomly choose an operation
                        int operation = random.nextInt(4);

                        switch (operation) {
                            case 0: // Set block
                                int block = random.nextBoolean() ? 0 : 1;
                                concurrentStorage.func_150818_a(x, y, z, block);
                                break;
                            case 1: // Set metadata
                                int metadata = random.nextInt(16);
                                concurrentStorage.setExtBlockMetadata(x, y, z, metadata);
                                break;
                            case 2: // Set block light
                                int blockLight = random.nextInt(16);
                                concurrentStorage.setExtBlocklightValue(x, y, z, blockLight);
                                break;
                            case 3: // Set sky light
                                int skyLight = random.nextInt(16);
                                concurrentStorage.setExtSkylightValue(x, y, z, skyLight);
                                break;
                        }

                        // Verify that we can read values without exceptions
                        try {
                            concurrentStorage.getBlockByExtId(x, y, z);
                            concurrentStorage.getExtBlockMetadata(x, y, z);
                            concurrentStorage.getExtBlocklightValue(x, y, z);
                            concurrentStorage.getExtSkylightValue(x, y, z);
                        } catch (Exception e) {
                            hasErrors.set(true);
                            errors.add("Thread " + threadId + " exception during read: " + e);
                        }
                    }
                } catch (Exception e) {
                    hasErrors.set(true);
                    errors.add("Thread " + threadId + " exception: " + e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads
        startLatch.countDown();

        // Wait for all threads to complete
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        assertTrue("All threads should complete in time", completed);

        // Check for errors
        if (hasErrors.get()) {
            StringBuilder errorMessage = new StringBuilder("Concurrent access errors:\n");
            for (String error : errors) {
                errorMessage.append(error)
                    .append("\n");
            }
            fail(errorMessage.toString());
        }
    }

    /**
     * Test the speed of block operations compared to standard storage.
     */
    @Test
    public void testBlockOperationSpeed() {
        ConcurrentExtendedBlockStorage[] concurrentStorages = new ConcurrentExtendedBlockStorage[SPEED_TEST_INSTANCE_COUNT];

        // Initialize arrays
        for (int i = 0; i < SPEED_TEST_INSTANCE_COUNT; i++) {
            concurrentStorages[i] = new ConcurrentExtendedBlockStorage(0, true);
        }

        // Test concurrent storage write speed
        long concurrentWriteStart = System.nanoTime();
        for (ConcurrentExtendedBlockStorage storage : concurrentStorages) {
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        int block = (x + y + z) % 3 == 0 ? 0 : (x + y + z) % 3 == 1 ? 1 : 2;
                        storage.func_150818_a(x, y, z, block);
                    }
                }
            }
        }
        long concurrentWriteTime = System.nanoTime() - concurrentWriteStart;

        // Test concurrent storage read speed
        long concurrentReadStart = System.nanoTime();
        for (ConcurrentExtendedBlockStorage storage : concurrentStorages) {
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        storage.getBlockIntByExtId(x, y, z);
                    }
                }
            }
        }
        long concurrentReadTime = System.nanoTime() - concurrentReadStart;

        // Print results
        System.out.println("Block operation speed test results:");
        System.out.println("Concurrent write time: " + concurrentWriteTime / 1000000 + "ms");
        System.out.println("Concurrent read time: " + concurrentReadTime / 1000000 + "ms");

        // We don't assert on the actual times since they can vary by environment,
        // but we log them for analysis
    }

    /**
     * Test the speed of metadata operations compared to standard storage.
     */
    @Test
    public void testMetadataOperationSpeed() {
        ConcurrentExtendedBlockStorage[] concurrentStorages = new ConcurrentExtendedBlockStorage[SPEED_TEST_INSTANCE_COUNT];
        ExtendedBlockStorage[] standardStorages = new ExtendedBlockStorage[SPEED_TEST_INSTANCE_COUNT];

        // Initialize arrays
        for (int i = 0; i < SPEED_TEST_INSTANCE_COUNT; i++) {
            concurrentStorages[i] = new ConcurrentExtendedBlockStorage(0, true);
            standardStorages[i] = new ExtendedBlockStorage(0, true);
        }

        // Test concurrent storage write speed
        long concurrentWriteStart = System.nanoTime();
        for (ConcurrentExtendedBlockStorage storage : concurrentStorages) {
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        int metadata = (x + y + z) % 16;
                        storage.setExtBlockMetadata(x, y, z, metadata);
                    }
                }
            }
        }
        long concurrentWriteTime = System.nanoTime() - concurrentWriteStart;

        // Test standard storage write speed
        long standardWriteStart = System.nanoTime();
        for (ExtendedBlockStorage storage : standardStorages) {
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        int metadata = (x + y + z) % 16;
                        storage.setExtBlockMetadata(x, y, z, metadata);
                    }
                }
            }
        }
        long standardWriteTime = System.nanoTime() - standardWriteStart;

        // Test concurrent storage read speed
        long concurrentReadStart = System.nanoTime();
        for (ConcurrentExtendedBlockStorage storage : concurrentStorages) {
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        storage.getExtBlockMetadata(x, y, z);
                    }
                }
            }
        }
        long concurrentReadTime = System.nanoTime() - concurrentReadStart;

        // Test standard storage read speed
        long standardReadStart = System.nanoTime();
        for (ExtendedBlockStorage storage : standardStorages) {
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        storage.getExtBlockMetadata(x, y, z);
                    }
                }
            }
        }
        long standardReadTime = System.nanoTime() - standardReadStart;

        // Print results
        System.out.println("Metadata operation speed test results:");
        System.out.println("Concurrent write time: " + concurrentWriteTime / 1000000 + "ms");
        System.out.println("Standard write time: " + standardWriteTime / 1000000 + "ms");
        System.out.println("Concurrent read time: " + concurrentReadTime / 1000000 + "ms");
        System.out.println("Standard read time: " + standardReadTime / 1000000 + "ms");
    }

    /**
     * Test the removeInvalidBlocks method.
     */
    @Test
    public void testRemoveInvalidBlocks() {
        // Create a fresh instance
        concurrentStorage = new ConcurrentExtendedBlockStorage(0, true);

        // Set some blocks
        for (int x = 0; x < 16; x += 4) {
            for (int y = 0; y < 16; y += 4) {
                for (int z = 0; z < 16; z += 4) {
                    concurrentStorage.func_150818_a(x, y, z, 1);
                }
            }
        }

        // Count blocks
        int blockCount = 0;
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    int block = concurrentStorage.getBlockIntByExtId(x, y, z);
                    if (block != 0) {
                        blockCount++;
                    }
                }
            }
        }

        // Call removeInvalidBlocks
        concurrentStorage.removeInvalidBlocksInt();

        // Verify blockRefCount matches our count
        assertEquals("blockRefCount should match manual count", blockCount, concurrentStorage.blockRefCount.get());

        // Verify isEmpty works correctly
        if (blockCount > 0) {
            assertFalse("isEmpty should return false when blocks exist", concurrentStorage.isEmpty());
        } else {
            assertTrue("isEmpty should return true when no blocks exist", concurrentStorage.isEmpty());
        }

        // Clear all blocks
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    concurrentStorage.func_150818_a(x, y, z, 0);
                }
            }
        }

        // Call removeInvalidBlocks again
        concurrentStorage.removeInvalidBlocksInt();

        // Verify blockRefCount is 0
        assertEquals("blockRefCount should be 0 after clearing all blocks", 0, concurrentStorage.blockRefCount.get());

        // Verify isEmpty works correctly
        assertTrue("isEmpty should return true when no blocks exist", concurrentStorage.isEmpty());
    }
}
