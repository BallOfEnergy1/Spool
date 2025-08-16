package com.gamma.spool.concurrent;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.world.chunk.NibbleArray;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the {@link AtomicNibbleArray} class.
 */
public class AtomicNibbleArrayTest {

    private static final int CUBE_SIDE_LENGTH = 15;
    private static final int ARRAY_SIZE = (CUBE_SIDE_LENGTH + 1) * (CUBE_SIDE_LENGTH + 1) * (CUBE_SIDE_LENGTH + 1);
    private static final int DEPTH = 4;

    private static final int NUM_THREADS = 8;
    private static final int OPERATIONS_PER_THREAD = 1000;

    private static final int SPEED_TEST_INSTANCE_COUNT = 4096;

    private AtomicNibbleArray nibbleArray;
    private ExecutorService executorService;

    @Before
    public void setUp() {
        nibbleArray = new AtomicNibbleArray(ARRAY_SIZE, DEPTH);
        executorService = Executors.newFixedThreadPool(NUM_THREADS);

        // UnsafeAccessor.enableUnsafe();
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

        // UnsafeAccessor.disableUnsafe();
    }

    /**
     * Test that the constructor properly initializes the array.
     */
    @Test
    public void testConstructor() {
        System.out.println("testConstructor:");
        assertNotNull("ConcurrentData should not be null", nibbleArray.concurrentData);
        assertEquals(
            "ConcurrentData should have the correct length",
            (ARRAY_SIZE >> 1 + (ARRAY_SIZE & 1)),
            nibbleArray.concurrentData.get().length);
    }

    /**
     * Test the constructor that takes a byte array.
     */
    @Test
    public void testByteArrayConstructor() {
        System.out.println("testByteArrayConstructor:");
        byte[] byteArray = new byte[ARRAY_SIZE >> 1];
        Random random = new Random();

        // Fill the byte array with random values
        for (int idx = 0; idx < ARRAY_SIZE >> 1; idx++) {
            byteArray[idx] = (byte) (random.nextInt(16)); // Lower nibbles
            byteArray[idx] |= (byte) ((random.nextInt(16) << 4)); // Upper nibbles
        }

        AtomicNibbleArray arrayFromBytes = new AtomicNibbleArray(byteArray, DEPTH);

        NibbleArray original = new NibbleArray(byteArray, DEPTH);

        assertNotNull("ConcurrentData should not be null", arrayFromBytes.concurrentData);

        // Test that the values were properly stored
        for (int idx = 0; idx < ARRAY_SIZE; idx++) {
            int expectedValue;
            if ((idx & 1) == 0) {
                expectedValue = byteArray[idx >> 1] & 0xF;
            } else {
                expectedValue = (byteArray[idx >> 1] & 0xF0) >> 4;
            }
            int x = (idx & 0b000000001111);
            int z = (idx & 0b000011110000) >> 4;
            int y = (idx & 0b111100000000) >> 8;
            int actualValue = arrayFromBytes.get(x, y, z);
            assertEquals(
                "Value at position (" + x + "," + y + "," + z + ") should match expected value",
                expectedValue,
                actualValue);
            int standardValue = original.get(x, y, z);
            assertEquals(
                "Value at position (" + x + "," + y + "," + z + ") should match with normal NibbleArray",
                standardValue,
                actualValue);
        }
    }

    /**
     * Test the getByteArray method.
     */
    @Test
    public void testGetByteArray() {
        System.out.println("testGetByteArray:");

        NibbleArray original = new NibbleArray(ARRAY_SIZE, DEPTH);

        // Set some values in the array
        for (int idx = 0; idx < ARRAY_SIZE; idx++) {
            int x = (idx & 0b000000001111);
            int z = (idx & 0b000011110000) >> 4;
            int y = (idx & 0b111100000000) >> 8;
            nibbleArray.set(x, y, z, (x + y + z) & 15); // Values 0-15
            original.set(x, y, z, (x + y + z) & 15);
        }

        byte[] byteArray = nibbleArray.getByteArray();

        assertNotNull("Byte array should not be null", byteArray);

        // The byte array length should match the concurrentData length
        assertEquals("Byte array length should match", nibbleArray.concurrentData.get().length, byteArray.length);

        for (int idx = 0; idx < (byteArray.length << 1); idx++) {
            byte b = byteArray[idx >> 1];
            int x = (idx & 0b000000001111);
            int z = (idx & 0b000011110000) >> 4;
            int y = (idx & 0b111100000000) >> 8;
            assertEquals(
                "Nibble at position " + idx + " should match expected value",
                (x + y + z) & 15,
                ((idx & 1) == 0 ? b & 0xF : ((b & 0xF0) >> 4)));
        }

        assertArrayEquals("Output byte array should equal normal output", original.data, byteArray);
    }

    @Test
    public void testRandomGetByteArray() {
        System.out.println("testRandomGetByteArray:");

        NibbleArray original = new NibbleArray(ARRAY_SIZE, DEPTH);

        Random random = new Random(42);

        // Set values in the array
        for (int idx = 0; idx < (ARRAY_SIZE << 12); idx++) {
            int x = random.nextInt(16);
            int z = random.nextInt(16);
            int y = random.nextInt(16);
            int value = (x + y + z) % 16; // Values 0-15
            nibbleArray.set(x, y, z, value);
            original.set(x, y, z, value);
        }

        System.out.println("RandomGetByteArray ran " + (ARRAY_SIZE << 12) + " random writes.");

        byte[] byteArray = nibbleArray.getByteArray();

        assertNotNull("Byte array should not be null", byteArray);

        // The byte array length should match the concurrentData length
        assertEquals("Byte array length should match", nibbleArray.concurrentData.get().length, byteArray.length);

        // Verify the values
        for (int idx = 0; idx < (byteArray.length << 14); idx++) {
            int x = random.nextInt(16);
            int z = random.nextInt(16);
            int y = random.nextInt(16);
            byte b = byteArray[(y << (DEPTH + 4) | z << DEPTH | x) >> 1];
            assertEquals(
                "Nibble at position " + idx + " should match expected value",
                nibbleArray.get(x, y, z),
                ((x & 1) == 0 ? b & 0xF : ((b & 0xF0) >> 4)));
        }

        System.out.println("RandomGetByteArray ran " + (byteArray.length << 14) + " random read checks.");

        assertArrayEquals("Output byte array should equal normal output", original.data, byteArray);
    }

    /**
     * Test the get and set methods for accuracy.
     */
    @Test
    public void testGetAndSet() {
        System.out.println("testGetAndSet:");

        NibbleArray original = new NibbleArray(ARRAY_SIZE, DEPTH);

        // Set values in the array
        for (int idx = 0; idx < ARRAY_SIZE; idx++) {
            int x = (idx & 0b000000001111);
            int z = (idx & 0b000011110000) >> 4;
            int y = (idx & 0b111100000000) >> 8;
            int value = (x + y + z) % 16; // Values 0-15
            nibbleArray.set(x, y, z, value);
            original.set(x, y, z, value);
        }

        // Verify the values
        for (int idx = 0; idx < ARRAY_SIZE; idx++) {
            int x = (idx & 0b000000001111);
            int z = (idx & 0b000011110000) >> 4;
            int y = (idx & 0b111100000000) >> 8;
            int expectedValue = (x + y + z) % 16;
            int actualValue = nibbleArray.get(x, y, z);
            assertEquals(
                "Value at position (" + x + "," + y + "," + z + ") should match with expected byte array",
                expectedValue,
                actualValue);
            int originalArrayValue = original.get(x, y, z);
            assertEquals(
                "Value at position (" + x + "," + y + "," + z + ") should match with original NibbleArray",
                originalArrayValue,
                actualValue);
        }
    }

    /**
     * Test the get and set methods with random indexes for accuracy.
     */
    @Test
    public void testRandomGetAndSet() {
        System.out.println("testRandomGetAndSet:");

        NibbleArray original = new NibbleArray(ARRAY_SIZE, DEPTH);

        Random random = new Random(42);

        // Set values in the array
        for (int idx = 0; idx < ARRAY_SIZE << 12; idx++) {
            int x = random.nextInt(16);
            int z = random.nextInt(16);
            int y = random.nextInt(16);
            int value = (x + y + z) % 16; // Values 0-15
            if (nibbleArray.get(x, y, z) != 0 || original.get(x, y, z) != 0) {
                continue; // Skip already-assigned values.
            }
            nibbleArray.set(x, y, z, value);
            original.set(x, y, z, value);
        }

        System.out.println("RandomGetAndSet attempted " + (ARRAY_SIZE << 12) + " random writes.");

        random = new Random(42);

        // Verify the values
        for (int idx = 0; idx < (ARRAY_SIZE << 14); idx++) {
            int x = random.nextInt(16);
            int z = random.nextInt(16);
            int y = random.nextInt(16);
            int expectedValue = (x + y + z) % 16;
            int actualValue = nibbleArray.get(x, y, z);
            assertEquals(
                "Value at position (" + x + "," + y + "," + z + ") should match with expected byte array",
                expectedValue,
                actualValue);
            int originalArrayValue = original.get(x, y, z);
            assertEquals(
                "Value at position (" + x + "," + y + "," + z + ") should match with original NibbleArray",
                originalArrayValue,
                actualValue);
        }

        System.out.println("RandomGetAndSet ran " + (ARRAY_SIZE << 14) + " random read checks.");
    }

    /**
     * Test concurrent access to the array.
     */
    @Test
    public void testConcurrentAccess() throws InterruptedException {
        System.out.println("testConcurrentAccess:");
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);
        final AtomicBoolean hasErrors = new AtomicBoolean(false);
        final List<String> errors = new ArrayList<>();

        // Create threads that will concurrently access the array
        for (int t = 0; t < NUM_THREADS; t++) {
            final int threadId = t;
            executorService.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    Random random = new Random(threadId); // Different seed for each thread

                    // Perform operations
                    for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                        int x = random.nextInt(CUBE_SIDE_LENGTH + 1);
                        int y = random.nextInt(CUBE_SIDE_LENGTH + 1);
                        int z = random.nextInt(CUBE_SIDE_LENGTH + 1);
                        int value = random.nextInt(16); // Values 0-15

                        // Set the value
                        nibbleArray.set(x, y, z, value);

                        // Get the value and verify it's either our value or another thread's value
                        int retrievedValue = nibbleArray.get(x, y, z);

                        // The retrieved value should be between 0 and 15
                        if (retrievedValue < 0 || retrievedValue > 15) {
                            hasErrors.set(true);
                            errors.add(
                                "Thread " + threadId
                                    + " got invalid value "
                                    + retrievedValue
                                    + " at position ("
                                    + x
                                    + ","
                                    + y
                                    + ","
                                    + z
                                    + ")");
                        }
                    }
                } catch (Exception e) {
                    hasErrors.set(true);
                    errors.add("Thread " + threadId + " exception: " + e.getMessage());
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
            assertTrue(errorMessage.toString(), false);
        }
    }

    /**
     * Test that concurrent modifications are atomic.
     */
    @Test
    public void testAtomicModifications() throws InterruptedException {
        System.out.println("testAtomicModifications:");
        final int x = 1, y = 1, z = 1;
        final int initialValue = 7;

        // Set initial value
        nibbleArray.set(x, y, z, initialValue);

        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);

        // Each thread will increment the value by 1, but we'll wrap around at 16
        for (int t = 0; t < NUM_THREADS; t++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    for (int i = 0; i < 10; i++) {
                        // Atomic increment
                        nibbleArray.incrementAndGet(x, y, z);

                        // Small delay to increase chance of race conditions
                        Thread.yield();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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

        // Final value should be (initialValue + NUM_THREADS * 10) % 16
        int expectedFinalValue = (initialValue + (NUM_THREADS * 10)) % 16;
        int actualFinalValue = nibbleArray.get(x, y, z);

        assertEquals(
            "Final value should match expected after concurrent modifications",
            expectedFinalValue,
            actualFinalValue);
    }

    /**
     * Tests the sequential read performance.
     */
    @Test
    public void testNibbleReadSpeedSequential() {
        System.out.println("testNibbleReadSpeedSequential:");

        AtomicNibbleArray[] arrayFromBytes = new AtomicNibbleArray[SPEED_TEST_INSTANCE_COUNT];
        Random random = new Random();

        System.out.println("Filling nibble arrays for testing...");

        for (int i = 0; i < arrayFromBytes.length; i++) {
            byte[] byteArray = new byte[ARRAY_SIZE >> 1];

            // Fill the byte array with random values
            for (int idx = 0; idx < ARRAY_SIZE >> 1; idx++) {
                byteArray[idx] = (byte) (random.nextInt(16)); // Lower nibbles
                byteArray[idx] |= (byte) (random.nextInt(16) << 4); // Upper nibbles
            }

            arrayFromBytes[i] = new AtomicNibbleArray(byteArray, DEPTH);
        }

        System.out.println("Beginning test!");

        long nanoTime = System.nanoTime();
        // Previous tests already confirmed that they're correct.
        for (AtomicNibbleArray arrayFromByte : arrayFromBytes) {
            for (int idx = 0; idx < ARRAY_SIZE; idx++) {
                int x = (idx & 0b000000001111);
                int z = (idx & 0b000011110000) >> 4;
                int y = (idx & 0b111100000000) >> 8;
                arrayFromByte.get(x, y, z);
            }
        }
        long nanoseconds = System.nanoTime() - nanoTime;

        System.out.println(
            "Time to get " + ARRAY_SIZE * SPEED_TEST_INSTANCE_COUNT
                + " nibbles (in "
                + SPEED_TEST_INSTANCE_COUNT
                + " arrays) (sequential): "
                + nanoseconds / 1000
                + "μs ("
                + nanoseconds / 1000000
                + "ms)");
    }

    /**
     * Tests the sequential write performance.
     */
    @Test
    public void testNibbleWriteSpeedSequential() {
        System.out.println("testNibbleWriteSpeedSequential:");

        AtomicNibbleArray[] arrayFromBytes = new AtomicNibbleArray[SPEED_TEST_INSTANCE_COUNT];
        Random random = new Random();

        System.out.println("Filling nibble arrays for testing...");

        for (int i = 0; i < arrayFromBytes.length; i++) {
            byte[] byteArray = new byte[ARRAY_SIZE >> 1];

            // Fill the byte array with random values
            for (int idx = 0; idx < ARRAY_SIZE >> 1; idx++) {
                byteArray[idx] = (byte) (random.nextInt(16)); // Lower nibbles
                byteArray[idx] |= (byte) (random.nextInt(16) << 4); // Upper nibbles
            }

            arrayFromBytes[i] = new AtomicNibbleArray(byteArray, DEPTH);
        }

        System.out.println("Beginning test!");

        long nanoTime = System.nanoTime();
        // Previous tests already confirmed that they're correct.
        for (AtomicNibbleArray arrayFromByte : arrayFromBytes) {
            for (int idx = 0; idx < ARRAY_SIZE; idx++) {
                int x = (idx & 0b000000001111);
                int z = (idx & 0b000011110000) >> 4;
                int y = (idx & 0b111100000000) >> 8;
                arrayFromByte.set(x, y, z, 0);
            }
        }
        long nanoseconds = System.nanoTime() - nanoTime;

        System.out.println(
            "Time to write " + ARRAY_SIZE * SPEED_TEST_INSTANCE_COUNT
                + " nibbles (in "
                + SPEED_TEST_INSTANCE_COUNT
                + " arrays) (sequential): "
                + nanoseconds / 1000
                + "μs ("
                + nanoseconds / 1000000
                + "ms)");
    }

    /**
     * Tests the sequential read performance (original NibbleArray).
     */
    @Test
    public void testNormalNibbleReadSpeedSequential() {
        System.out.println("testNormalNibbleReadSpeedSequential:");

        NibbleArray[] arrayFromBytes = new NibbleArray[SPEED_TEST_INSTANCE_COUNT];
        Random random = new Random();

        System.out.println("Filling nibble arrays for testing...");

        for (int i = 0; i < arrayFromBytes.length; i++) {
            byte[] byteArray = new byte[ARRAY_SIZE >> 1];

            // Fill the byte array with random values
            for (int idx = 0; idx < ARRAY_SIZE >> 1; idx++) {
                byteArray[idx] = (byte) (random.nextInt(16)); // Lower nibbles
                byteArray[idx] |= (byte) (random.nextInt(16) << 4); // Upper nibbles
            }

            arrayFromBytes[i] = new NibbleArray(byteArray, DEPTH);
        }

        System.out.println("Beginning test!");

        long nanoTime = System.nanoTime();
        // Previous tests already confirmed that they're correct.
        for (NibbleArray arrayFromByte : arrayFromBytes) {
            for (int idx = 0; idx < ARRAY_SIZE; idx++) {
                int x = (idx & 0b000000001111);
                int z = (idx & 0b000011110000) >> 4;
                int y = (idx & 0b111100000000) >> 8;
                arrayFromByte.get(x, y, z);
            }
        }
        long nanoseconds = System.nanoTime() - nanoTime;

        System.out.println(
            "Time to get " + ARRAY_SIZE * SPEED_TEST_INSTANCE_COUNT
                + " nibbles (in "
                + SPEED_TEST_INSTANCE_COUNT
                + " ORIGINAL arrays) (sequential): "
                + nanoseconds / 1000
                + "μs ("
                + nanoseconds / 1000000
                + "ms)");
    }

    /**
     * Tests the sequential write performance (original NibbleArray).
     */
    @Test
    public void testNormalNibbleWriteSpeedSequential() {
        System.out.println("testNormalNibbleWriteSpeedSequential:");

        NibbleArray[] arrayFromBytes = new NibbleArray[SPEED_TEST_INSTANCE_COUNT];
        Random random = new Random();

        System.out.println("Filling nibble arrays for testing...");

        for (int i = 0; i < arrayFromBytes.length; i++) {
            byte[] byteArray = new byte[ARRAY_SIZE >> 1];

            // Fill the byte array with random values
            for (int idx = 0; idx < ARRAY_SIZE >> 1; idx++) {
                byteArray[idx] = (byte) (random.nextInt(16)); // Lower nibbles
                byteArray[idx] |= (byte) (random.nextInt(16) << 4); // Upper nibbles
            }

            arrayFromBytes[i] = new NibbleArray(byteArray, DEPTH);
        }

        System.out.println("Beginning test!");

        long nanoTime = System.nanoTime();
        // Previous tests already confirmed that they're correct.
        for (NibbleArray arrayFromByte : arrayFromBytes) {
            for (int idx = 0; idx < ARRAY_SIZE; idx++) {
                int x = (idx & 0b000000001111);
                int z = (idx & 0b000011110000) >> 4;
                int y = (idx & 0b111100000000) >> 8;
                arrayFromByte.set(x, y, z, 0);
            }
        }
        long nanoseconds = System.nanoTime() - nanoTime;

        System.out.println(
            "Time to write " + ARRAY_SIZE * SPEED_TEST_INSTANCE_COUNT
                + " nibbles (in "
                + SPEED_TEST_INSTANCE_COUNT
                + " ORIGINAL arrays) (sequential): "
                + nanoseconds / 1000
                + "μs ("
                + nanoseconds / 1000000
                + "ms)");
    }

    /**
     * Tests the parallel read performance.
     */
    @Test
    public void testNibbleReadSpeedParallel() throws InterruptedException {
        System.out.println("testNibbleReadSpeedParallel:");

        AtomicNibbleArray[] arrayFromBytes = new AtomicNibbleArray[SPEED_TEST_INSTANCE_COUNT];
        Random random = new Random();

        System.out.println("Filling nibble arrays for testing...");

        for (int i = 0; i < arrayFromBytes.length; i++) {
            byte[] byteArray = new byte[ARRAY_SIZE >> 1];

            // Fill the byte array with random values
            for (int idx = 0; idx < ARRAY_SIZE >> 1; idx++) {
                byteArray[idx] = (byte) (random.nextInt(16)); // Lower nibbles
                byteArray[idx] |= (byte) (random.nextInt(16) << 4); // Upper nibbles
            }

            arrayFromBytes[i] = new AtomicNibbleArray(byteArray, DEPTH);
        }

        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(SPEED_TEST_INSTANCE_COUNT);

        System.out.println("Adding parallel tasks...");

        for (int t = 0; t < SPEED_TEST_INSTANCE_COUNT; t++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    for (int idx = 0; idx < ARRAY_SIZE; idx++) {
                        int x = (idx & 0b000000001111);
                        int z = (idx & 0b000011110000) >> 4;
                        int y = (idx & 0b111100000000) >> 8;
                        arrayFromBytes[idx].get(x, y, z);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        System.out.println("Beginning test!");

        long nanoTime = System.nanoTime();

        // Start all threads
        startLatch.countDown();

        // Wait for all threads.
        doneLatch.await();

        long nanoseconds = System.nanoTime() - nanoTime;

        System.out.println(
            "Time to get " + ARRAY_SIZE * SPEED_TEST_INSTANCE_COUNT
                + " nibbles (in "
                + SPEED_TEST_INSTANCE_COUNT
                + " arrays) (parallel): "
                + nanoseconds / 1000
                + "μs ("
                + nanoseconds / 1000000
                + "ms)");
    }

    /**
     * Tests the parallel write performance.
     */
    @Test
    public void testNibbleWriteSpeedParallel() throws InterruptedException {
        System.out.println("testNibbleWriteSpeedParallel:");

        AtomicNibbleArray[] arrayFromBytes = new AtomicNibbleArray[SPEED_TEST_INSTANCE_COUNT];
        Random random = new Random();

        System.out.println("Filling nibble arrays for testing...");

        for (int i = 0; i < arrayFromBytes.length; i++) {
            byte[] byteArray = new byte[ARRAY_SIZE >> 1];

            // Fill the byte array with random values
            for (int idx = 0; idx < ARRAY_SIZE >> 1; idx++) {
                byteArray[idx] = (byte) (random.nextInt(16)); // Lower nibbles
                byteArray[idx] |= (byte) (random.nextInt(16) << 4); // Upper nibbles
            }

            arrayFromBytes[i] = new AtomicNibbleArray(byteArray, DEPTH);
        }

        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(SPEED_TEST_INSTANCE_COUNT);

        System.out.println("Adding parallel tasks...");

        for (int t = 0; t < SPEED_TEST_INSTANCE_COUNT; t++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    for (int idx = 0; idx < ARRAY_SIZE; idx++) {
                        int x = (idx & 0b000000001111);
                        int z = (idx & 0b000011110000) >> 4;
                        int y = (idx & 0b111100000000) >> 8;
                        arrayFromBytes[idx].set(x, y, z, 0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        System.out.println("Beginning test!");

        long nanoTime = System.nanoTime();

        // Start all threads
        startLatch.countDown();

        // Wait for all threads.
        doneLatch.await();

        long nanoseconds = System.nanoTime() - nanoTime;

        System.out.println(
            "Time to write " + ARRAY_SIZE * SPEED_TEST_INSTANCE_COUNT
                + " nibbles (in "
                + SPEED_TEST_INSTANCE_COUNT
                + " arrays) (parallel): "
                + nanoseconds / 1000
                + "μs ("
                + nanoseconds / 1000000
                + "ms)");
    }

    /**
     * Tests the parallel read performance (original NibbleArray).
     */
    @Test
    public void testNormalNibbleReadSpeedParallel() throws InterruptedException {
        System.out.println("testNormalNibbleReadSpeedParallel:");

        NibbleArray[] arrayFromBytes = new NibbleArray[SPEED_TEST_INSTANCE_COUNT];
        Random random = new Random();

        System.out.println("Filling nibble arrays for testing...");

        for (int i = 0; i < arrayFromBytes.length; i++) {
            byte[] byteArray = new byte[ARRAY_SIZE >> 1];

            // Fill the byte array with random values
            for (int idx = 0; idx < ARRAY_SIZE >> 1; idx++) {
                byteArray[idx] = (byte) (random.nextInt(16)); // Lower nibbles
                byteArray[idx] |= (byte) (random.nextInt(16) << 4); // Upper nibbles
            }

            arrayFromBytes[i] = new NibbleArray(byteArray, DEPTH);
        }

        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(SPEED_TEST_INSTANCE_COUNT);

        Object sync = new Object();

        System.out.println("Adding parallel tasks...");

        for (int t = 0; t < SPEED_TEST_INSTANCE_COUNT; t++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    for (int idx = 0; idx < ARRAY_SIZE; idx++) {
                        int x = (idx & 0b000000001111);
                        int z = (idx & 0b000011110000) >> 4;
                        int y = (idx & 0b111100000000) >> 8;
                        synchronized (sync) {
                            arrayFromBytes[idx].get(x, y, z);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        System.out.println("Beginning test!");

        long nanoTime = System.nanoTime();

        // Start all threads
        startLatch.countDown();

        // Wait for all threads.
        doneLatch.await();

        long nanoseconds = System.nanoTime() - nanoTime;

        System.out.println(
            "Time to get " + ARRAY_SIZE * SPEED_TEST_INSTANCE_COUNT
                + " nibbles (in "
                + SPEED_TEST_INSTANCE_COUNT
                + " ORIGINAL arrays) (parallel): "
                + nanoseconds / 1000
                + "μs ("
                + nanoseconds / 1000000
                + "ms)");
    }

    /**
     * Tests the parallel write performance (original NibbleArray).
     */
    @Test
    public void testNormalNibbleWriteSpeedParallel() throws InterruptedException {
        System.out.println("testNormalNibbleWriteSpeedParallel:");

        NibbleArray[] arrayFromBytes = new NibbleArray[SPEED_TEST_INSTANCE_COUNT];
        Random random = new Random();

        System.out.println("Filling nibble arrays for testing...");

        for (int i = 0; i < arrayFromBytes.length; i++) {
            byte[] byteArray = new byte[ARRAY_SIZE >> 1];

            // Fill the byte array with random values
            for (int idx = 0; idx < ARRAY_SIZE >> 1; idx++) {
                byteArray[idx] = (byte) (random.nextInt(16)); // Lower nibbles
                byteArray[idx] |= (byte) (random.nextInt(16) << 4); // Upper nibbles
            }

            arrayFromBytes[i] = new NibbleArray(byteArray, DEPTH);
        }

        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(SPEED_TEST_INSTANCE_COUNT);

        Object sync = new Object();

        System.out.println("Adding parallel tasks...");

        for (int t = 0; t < SPEED_TEST_INSTANCE_COUNT; t++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    for (int idx = 0; idx < ARRAY_SIZE; idx++) {
                        int x = (idx & 0b000000001111);
                        int z = (idx & 0b000011110000) >> 4;
                        int y = (idx & 0b111100000000) >> 8;
                        synchronized (sync) {
                            arrayFromBytes[idx].set(x, y, z, 0);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        System.out.println("Beginning test!");

        long nanoTime = System.nanoTime();

        // Start all threads
        startLatch.countDown();

        // Wait for all threads.
        doneLatch.await();

        long nanoseconds = System.nanoTime() - nanoTime;

        System.out.println(
            "Time to write " + ARRAY_SIZE * SPEED_TEST_INSTANCE_COUNT
                + " nibbles (in "
                + SPEED_TEST_INSTANCE_COUNT
                + " ORIGINAL arrays) (parallel): "
                + nanoseconds / 1000
                + "μs ("
                + nanoseconds / 1000000
                + "ms)");
    }
}
