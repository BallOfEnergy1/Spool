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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gamma.spool.util.concurrent.AtomicByteArray;

public class AtomicByteArrayTest {

    private static final int CUBE_SIDE_LENGTH = 15;
    private static final int ARRAY_SIZE = (CUBE_SIDE_LENGTH + 1) * (CUBE_SIDE_LENGTH + 1) * (CUBE_SIDE_LENGTH + 1);

    private static final int NUM_THREADS = 8;
    private static final int OPERATIONS_PER_THREAD = 1000;

    private AtomicByteArray byteArray;
    private ExecutorService executorService;

    @Before
    public void setUp() {
        byteArray = new AtomicByteArray(ARRAY_SIZE);
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

    @Test
    public void testGetByteArray() {
        System.out.println("testGetByteArray:");

        byte[] original = new byte[ARRAY_SIZE];

        // Set some values in the array
        for (int idx = 0; idx < ARRAY_SIZE; idx++) {
            int x = (idx & 0b000000001111);
            int z = (idx & 0b000011110000) >> 4;
            int y = (idx & 0b111100000000) >> 8;
            byteArray.set(idx, (byte) ((x + y + z) & 15)); // Values 0-15
            original[idx] = (byte) ((x + y + z) & 15);
        }

        byte[] byteArray = this.byteArray.getCopy();

        assertNotNull("Byte array should not be null", byteArray);

        // The byte array length should match the concurrentData length
        assertEquals("Byte array length should match", this.byteArray.length(), byteArray.length);

        for (int idx = 0; idx < byteArray.length; idx++) {
            byte b = byteArray[idx];
            int x = (idx & 0b000000001111);
            int z = (idx & 0b000011110000) >> 4;
            int y = (idx & 0b111100000000) >> 8;
            assertEquals("Byte at index " + idx + " should match expected value", (x + y + z) & 15, b);
        }

        assertArrayEquals("Output byte array should equal normal output", original, byteArray);
    }

    @Test
    public void testRandomGetByteArray() {
        System.out.println("testRandomGetByteArray:");

        byte[] original = new byte[ARRAY_SIZE];

        Random random = new Random(42);

        // Set values in the array
        for (int idx = 0; idx < (ARRAY_SIZE << 12); idx++) {
            int x = random.nextInt(16);
            int z = random.nextInt(16);
            int y = random.nextInt(16);
            int value = (x + y + z) % 16; // Values 0-15
            byteArray.set((x << 8) | (y << 4) | z, (byte) value);
            original[(x << 8) | (y << 4) | z] = (byte) value;
        }

        System.out.println("RandomGetByteArray ran " + (ARRAY_SIZE << 12) + " random writes.");

        byte[] byteArray = this.byteArray.getCopy();

        assertNotNull("Byte array should not be null", byteArray);

        // The byte array length should match the concurrentData length
        assertEquals("Byte array length should match", this.byteArray.length(), byteArray.length);

        // Verify the values
        for (int idx = 0; idx < (byteArray.length << 14); idx++) {
            int x = random.nextInt(16);
            int z = random.nextInt(16);
            int y = random.nextInt(16);
            byte b = byteArray[(y << 8) | (z << 4) | x];
            assertEquals(
                "Byte at index " + idx + " should match expected value",
                this.byteArray.get((y << 8) | (z << 4) | x),
                b);
        }

        System.out.println("RandomGetByteArray ran " + (byteArray.length << 14) + " random read checks.");

        assertArrayEquals("Output byte array should equal normal output", original, byteArray);
    }

    /**
     * Test the get and set methods for accuracy.
     */
    @Test
    public void testGetAndSet() {
        System.out.println("testGetAndSet:");

        byte[] original = new byte[ARRAY_SIZE];

        // Set values in the array
        for (int idx = 0; idx < ARRAY_SIZE; idx++) {
            int x = (idx & 0b000000001111);
            int z = (idx & 0b000011110000) >> 4;
            int y = (idx & 0b111100000000) >> 8;
            int value = (x + y + z) % 16; // Values 0-15
            byteArray.set(idx, (byte) value);
            original[idx] = (byte) value;
        }

        // Verify the values
        for (int idx = 0; idx < ARRAY_SIZE; idx++) {
            int x = (idx & 0b000000001111);
            int z = (idx & 0b000011110000) >> 4;
            int y = (idx & 0b111100000000) >> 8;
            int expectedValue = (x + y + z) % 16;
            int actualValue = byteArray.get(idx);
            assertEquals(
                "Value at index " + idx + " should match with expected byte array",
                expectedValue,
                actualValue);
            int originalArrayValue = original[idx];
            assertEquals(
                "Value at index " + idx + " should match with original NibbleArray",
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

        byte[] original = new byte[ARRAY_SIZE];

        Random random = new Random(42);

        // Set values in the array
        for (int idx = 0; idx < ARRAY_SIZE << 12; idx++) {
            int x = random.nextInt(16);
            int z = random.nextInt(16);
            int y = random.nextInt(16);
            int value = (x + y + z) % 256; // Values 0-256
            int index = (y << 8) | (z << 4) | x;
            if (byteArray.get(index) != 0 || original[index] != 0) {
                continue; // Skip already-assigned values.
            }
            byteArray.set(index, (byte) value);
            original[index] = (byte) value;
        }

        System.out.println("RandomGetAndSet attempted " + (ARRAY_SIZE << 12) + " random writes.");

        random = new Random(42);

        // Verify the values
        for (int idx = 0; idx < (ARRAY_SIZE << 14); idx++) {
            int x = random.nextInt(16);
            int z = random.nextInt(16);
            int y = random.nextInt(16);
            int expectedValue = (x + y + z) % 256;
            int actualValue = byteArray.get((y << 8) | (z << 4) | x);
            assertEquals(
                "Value at index " + ((y << 8) | (z << 4) | x) + " should match with expected byte array",
                expectedValue,
                actualValue);
            int originalArrayValue = original[(y << 8) | (z << 4) | x];
            assertEquals(
                "Value at index " + ((y << 8) | (z << 4) | x) + " should match with original NibbleArray",
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
                        byteArray.set((y << 8) | (z << 4) | x, (byte) value);

                        // Get the value and verify it's either our value or another thread's value
                        int retrievedValue = byteArray.get((y << 8) | (z << 4) | x);

                        // The retrieved value should be between 0 and 15
                        if (retrievedValue < 0 || retrievedValue > 15) {
                            hasErrors.set(true);
                            errors.add(
                                "Thread " + threadId
                                    + " got invalid value "
                                    + retrievedValue
                                    + " at index "
                                    + ((y << 8) | (z << 4) | x)
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
        byteArray.set((y << 8) | (z << 4) | x, (byte) initialValue);

        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);

        // Each thread will increment the value by 1, but we'll wrap around at 16
        for (int t = 0; t < NUM_THREADS; t++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    for (int i = 0; i < 10; i++) {
                        // Atomic increment
                        byteArray.incrementAndGet((y << 8) | (z << 4) | x);

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

        // Final value should be (initialValue + NUM_THREADS * 10) % 256
        int expectedFinalValue = (initialValue + (NUM_THREADS * 10)) % 256;
        int actualFinalValue = byteArray.get((y << 8) | (z << 4) | x);

        assertEquals(
            "Final value should match expected after concurrent modifications",
            expectedFinalValue,
            actualFinalValue);
    }
}
