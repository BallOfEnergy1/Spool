package com.gamma.spool.concurrent;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.BitSet;
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

import com.gamma.spool.util.fast.bitset.BitSetFactory;
import com.gamma.spool.util.fast.bitset.FastAtomicBitSet;

public class AtomicBitSetTest {

    private static final int CUBE_SIDE_LENGTH = 15;
    private static final int ARRAY_SIZE = (CUBE_SIDE_LENGTH + 1) * (CUBE_SIDE_LENGTH + 1) * (CUBE_SIDE_LENGTH + 1);

    private static final int NUM_THREADS = 8;
    private static final int OPERATIONS_PER_THREAD = 1000;

    private static final int SPEED_TEST_INSTANCE_COUNT = 4096;

    private FastAtomicBitSet bitSet;
    private ExecutorService executorService;

    @Before
    public void setUp() {
        bitSet = BitSetFactory.create(ARRAY_SIZE);
        executorService = Executors.newFixedThreadPool(NUM_THREADS);
        // ImplConfig.useCompactImpls = true;
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
    public void testGetBitSet() {
        System.out.println("testGetBitSet:");

        boolean[] original = new boolean[ARRAY_SIZE];

        // Set some values in the array
        for (int idx = 0; idx < ARRAY_SIZE; idx++) {
            if (idx % 2 == 0) bitSet.set(idx);
            original[idx] = (idx % 2) == 0;
        }

        for (int idx = 0; idx < ARRAY_SIZE; idx++) {
            assertEquals("Bit at index " + idx + " should match expected value", original[idx], bitSet.get(idx));
        }
    }

    @Test
    public void testRandomGetBitSet() {
        System.out.println("testRandomGetBitSet:");

        boolean[] original = new boolean[ARRAY_SIZE];

        Random random = new Random(42);

        // Set values in the array
        for (int idx = 0; idx < (ARRAY_SIZE << 12); idx++) {
            int arrayIdx = random.nextInt(ARRAY_SIZE);
            boolean value = arrayIdx % 2 == 0;
            if (value) {
                bitSet.set(arrayIdx);
            }
            original[arrayIdx] = value;
        }

        System.out.println("RandomGetBitSet ran " + (ARRAY_SIZE << 12) + " random writes.");

        // Verify the values
        for (int idx = 0; idx < (ARRAY_SIZE << 14); idx++) {
            int x = random.nextInt(16);
            int z = random.nextInt(16);
            int y = random.nextInt(16);
            boolean b = original[(y << 8) | (z << 4) | x];
            assertEquals(
                "Bit at index " + idx + " should match expected value",
                bitSet.get((y << 8) | (z << 4) | x),
                b);
        }

        System.out.println("RandomGetBitSet ran " + (ARRAY_SIZE << 14) + " random read checks.");
    }

    /**
     * Test the get and set methods for accuracy.
     */
    @Test
    public void testGetAndSet() {
        System.out.println("testGetAndSet:");

        boolean[] original = new boolean[ARRAY_SIZE];

        // Set values in the array
        for (int idx = 0; idx < ARRAY_SIZE; idx++) {
            boolean value = idx % 2 == 0;
            if (value) bitSet.set(idx);
            original[idx] = value;
        }

        // Verify the values
        for (int idx = 0; idx < ARRAY_SIZE; idx++) {
            boolean expectedValue = idx % 2 == 0;
            boolean actualValue = bitSet.get(idx);
            assertEquals(
                "Value at index " + idx + " should match with expected boolean array",
                expectedValue,
                actualValue);
        }
    }

    /**
     * Test the get and set methods with random indexes for accuracy.
     */
    @Test
    public void testRandomGetAndSet() {
        System.out.println("testRandomGetAndSet:");

        boolean[] original = new boolean[ARRAY_SIZE];

        Random random = new Random(42);

        // Set values in the array
        for (int idx = 0; idx < ARRAY_SIZE << 12; idx++) {
            int x = random.nextInt(16);
            int z = random.nextInt(16);
            int y = random.nextInt(16);
            int index = (y << 8) | (z << 4) | x;
            boolean value = index % 2 == 0;
            if (bitSet.get(index) || original[index]) {
                continue; // Skip already-assigned values.
            }
            if (value) bitSet.set(index);
            original[index] = value;
        }

        System.out.println("RandomGetAndSet attempted " + (ARRAY_SIZE << 12) + " random writes.");

        random = new Random(42);

        // Verify the values
        for (int idx = 0; idx < (ARRAY_SIZE << 14); idx++) {
            int x = random.nextInt(16);
            int z = random.nextInt(16);
            int y = random.nextInt(16);
            int index = (y << 8) | (z << 4) | x;
            boolean expectedValue = index % 2 == 0;
            boolean actualValue = bitSet.get(index);
            assertEquals(
                "Bit at index " + index + " should match with expected boolean array",
                expectedValue,
                actualValue);
            boolean originalArrayValue = original[index];
            assertEquals(
                "Bit at index " + index + " should match with original boolean array",
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
                        boolean value = random.nextBoolean();

                        // Set the value
                        if (value) bitSet.set((y << 8) | (z << 4) | x);
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
        final boolean initialValue = true;

        // Set initial value
        bitSet.set((y << 8) | (z << 4) | x);

        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);

        // Each thread will flip the value
        for (int t = 0; t < NUM_THREADS; t++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    for (int i = 0; i < 10; i++) {
                        // Atomic flip
                        bitSet.flip((y << 8) | (z << 4) | x);

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

        boolean expectedFinalValue = initialValue;
        for (int i = 0; i < NUM_THREADS * 10; i++) {
            expectedFinalValue = !expectedFinalValue;
        }

        boolean actualFinalValue = bitSet.get((y << 8) | (z << 4) | x);

        assertEquals(
            "Final value should match expected after concurrent modifications",
            expectedFinalValue,
            actualFinalValue);
    }

    /**
     * Tests the sequential read performance.
     */
    @Test
    public void testBitSetReadSpeedSequential() {
        System.out.println("testBitSetReadSpeedSequential:");

        FastAtomicBitSet[] bitSetArray = new FastAtomicBitSet[SPEED_TEST_INSTANCE_COUNT];
        Random random = new Random();

        System.out.println("Filling bit sets for testing...");

        for (int i = 0; i < bitSetArray.length; i++) {
            FastAtomicBitSet set;
            bitSetArray[i] = set = BitSetFactory.create(ARRAY_SIZE);

            // Fill the bit set with random values
            for (int idx = 0; idx < ARRAY_SIZE; idx++) {
                set.set(idx, random.nextBoolean());
            }

        }

        System.out.println("Beginning test!");

        long nanoTime = System.nanoTime();
        // Previous tests already confirmed that they're correct.
        for (FastAtomicBitSet atomicBitSet : bitSetArray) {
            for (int idx = 0; idx < ARRAY_SIZE; idx++) {
                atomicBitSet.get(idx);
            }
        }
        long nanoseconds = System.nanoTime() - nanoTime;

        System.out.println(
            "Time to get " + ARRAY_SIZE * SPEED_TEST_INSTANCE_COUNT
                + " bits (in "
                + SPEED_TEST_INSTANCE_COUNT
                + " sets) (sequential): "
                + nanoseconds / 1000
                + "μs ("
                + nanoseconds / 1000000
                + "ms)");
    }

    /**
     * Tests the sequential write performance.
     */
    @Test
    public void testBitSetWriteSpeedSequential() {
        System.out.println("testBitSetWriteSpeedSequential:");

        FastAtomicBitSet[] bitSetArray = new FastAtomicBitSet[SPEED_TEST_INSTANCE_COUNT];
        Random random = new Random();

        System.out.println("Filling bit sets for testing...");

        for (int i = 0; i < bitSetArray.length; i++) {
            FastAtomicBitSet set;
            bitSetArray[i] = set = BitSetFactory.create(ARRAY_SIZE);

            // Fill the bit set with random values
            for (int idx = 0; idx < ARRAY_SIZE; idx++) {
                set.set(idx, random.nextBoolean());
            }
        }

        System.out.println("Beginning test!");

        long nanoTime = System.nanoTime();
        // Previous tests already confirmed that they're correct.
        for (FastAtomicBitSet atomicBitSet : bitSetArray) {
            for (int idx = 0; idx < ARRAY_SIZE; idx++) {
                atomicBitSet.set(idx, false);
            }
        }
        long nanoseconds = System.nanoTime() - nanoTime;

        System.out.println(
            "Time to write " + ARRAY_SIZE * SPEED_TEST_INSTANCE_COUNT
                + " bits (in "
                + SPEED_TEST_INSTANCE_COUNT
                + " sets) (sequential): "
                + nanoseconds / 1000
                + "μs ("
                + nanoseconds / 1000000
                + "ms)");
    }

    /**
     * Tests the sequential read performance (original BitSet).
     */
    @Test
    public void testNormalBitSetReadSpeedSequential() {
        System.out.println("testNormalBitSetReadSpeedSequential:");

        java.util.BitSet[] bitSetArray = new BitSet[SPEED_TEST_INSTANCE_COUNT];
        Random random = new Random();

        System.out.println("Filling bit sets for testing...");

        for (int i = 0; i < bitSetArray.length; i++) {
            BitSet set;
            bitSetArray[i] = set = new BitSet(ARRAY_SIZE);

            // Fill the bit set with random values
            for (int idx = 0; idx < ARRAY_SIZE; idx++) {
                set.set(idx, random.nextBoolean());
            }
        }

        System.out.println("Beginning test!");

        long nanoTime = System.nanoTime();
        // Previous tests already confirmed that they're correct.
        for (BitSet atomicBitSet : bitSetArray) {
            for (int idx = 0; idx < ARRAY_SIZE; idx++) {
                atomicBitSet.get(idx);
            }
        }
        long nanoseconds = System.nanoTime() - nanoTime;

        System.out.println(
            "Time to get " + ARRAY_SIZE * SPEED_TEST_INSTANCE_COUNT
                + " bits (in "
                + SPEED_TEST_INSTANCE_COUNT
                + " ORIGINAL sets) (sequential): "
                + nanoseconds / 1000
                + "μs ("
                + nanoseconds / 1000000
                + "ms)");
    }

    /**
     * Tests the sequential write performance (original BitSet).
     */
    @Test
    public void testNormalBitSetWriteSpeedSequential() {
        System.out.println("testNormalBitSetWriteSpeedSequential:");

        BitSet[] bitSetArray = new BitSet[SPEED_TEST_INSTANCE_COUNT];
        Random random = new Random();

        System.out.println("Filling bit sets for testing...");

        for (int i = 0; i < bitSetArray.length; i++) {
            BitSet set;
            bitSetArray[i] = set = new BitSet(ARRAY_SIZE);

            // Fill the bit set with random values
            for (int idx = 0; idx < ARRAY_SIZE; idx++) {
                set.set(idx, random.nextBoolean());
            }
        }

        System.out.println("Beginning test!");

        long nanoTime = System.nanoTime();
        // Previous tests already confirmed that they're correct.
        for (BitSet atomicBitSet : bitSetArray) {
            for (int idx = 0; idx < ARRAY_SIZE; idx++) {
                atomicBitSet.set(idx, false);
            }
        }
        long nanoseconds = System.nanoTime() - nanoTime;

        System.out.println(
            "Time to write " + ARRAY_SIZE * SPEED_TEST_INSTANCE_COUNT
                + " bits (in "
                + SPEED_TEST_INSTANCE_COUNT
                + " ORIGINAL sets) (sequential): "
                + nanoseconds / 1000
                + "μs ("
                + nanoseconds / 1000000
                + "ms)");
    }

    /**
     * Tests the parallel read performance.
     */
    @Test
    public void testBitSetReadSpeedParallel() throws InterruptedException {
        System.out.println("testBitSetReadSpeedParallel:");

        FastAtomicBitSet[] bitSetArray = new FastAtomicBitSet[SPEED_TEST_INSTANCE_COUNT];
        Random random = new Random();

        System.out.println("Filling bit sets for testing...");

        for (int i = 0; i < bitSetArray.length; i++) {
            FastAtomicBitSet set;
            bitSetArray[i] = set = BitSetFactory.create(ARRAY_SIZE);

            // Fill the bit set with random values
            for (int idx = 0; idx < ARRAY_SIZE; idx++) {
                set.set(idx, random.nextBoolean());
            }
        }

        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(SPEED_TEST_INSTANCE_COUNT);

        System.out.println("Adding parallel tasks...");

        for (int t = 0; t < SPEED_TEST_INSTANCE_COUNT; t++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    for (int idx = 0; idx < ARRAY_SIZE; idx++) {
                        bitSetArray[idx].get(idx);
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
                + " bits (in "
                + SPEED_TEST_INSTANCE_COUNT
                + " sets) (parallel): "
                + nanoseconds / 1000
                + "μs ("
                + nanoseconds / 1000000
                + "ms)");
    }

    /**
     * Tests the parallel write performance.
     */
    @Test
    public void testBitSetWriteSpeedParallel() throws InterruptedException {
        System.out.println("testBitSetWriteSpeedParallel:");

        FastAtomicBitSet[] bitSetArray = new FastAtomicBitSet[SPEED_TEST_INSTANCE_COUNT];
        Random random = new Random();

        System.out.println("Filling bit sets for testing...");

        for (int i = 0; i < bitSetArray.length; i++) {
            FastAtomicBitSet set;
            bitSetArray[i] = set = BitSetFactory.create(ARRAY_SIZE);

            // Fill the bit set with random values
            for (int idx = 0; idx < ARRAY_SIZE; idx++) {
                set.set(idx, random.nextBoolean());
            }
        }

        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(SPEED_TEST_INSTANCE_COUNT);

        System.out.println("Adding parallel tasks...");

        for (int t = 0; t < SPEED_TEST_INSTANCE_COUNT; t++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    for (int idx = 0; idx < ARRAY_SIZE; idx++) {
                        bitSetArray[idx].set(idx, false);
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
                + " bits (in "
                + SPEED_TEST_INSTANCE_COUNT
                + " sets) (parallel): "
                + nanoseconds / 1000
                + "μs ("
                + nanoseconds / 1000000
                + "ms)");
    }

    /**
     * Tests the parallel read performance (original BitSet).
     */
    @Test
    public void testNormalBitSetReadSpeedParallel() throws InterruptedException {
        System.out.println("testNormalBitSetReadSpeedParallel:");

        BitSet[] bitSetArray = new BitSet[SPEED_TEST_INSTANCE_COUNT];
        Random random = new Random();

        System.out.println("Filling bit sets for testing...");

        for (int i = 0; i < bitSetArray.length; i++) {
            BitSet set;
            bitSetArray[i] = set = new BitSet(ARRAY_SIZE);

            // Fill the bit set with random values
            for (int idx = 0; idx < ARRAY_SIZE; idx++) {
                set.set(idx, random.nextBoolean());
            }
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
                        synchronized (sync) {
                            bitSetArray[idx].get(idx);
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
                + " bits (in "
                + SPEED_TEST_INSTANCE_COUNT
                + " ORIGINAL sets) (parallel): "
                + nanoseconds / 1000
                + "μs ("
                + nanoseconds / 1000000
                + "ms)");
    }

    /**
     * Tests the parallel write performance (original BitSet).
     */
    @Test
    public void testNormalBitSetWriteSpeedParallel() throws InterruptedException {
        System.out.println("testNormalBitSetWriteSpeedParallel:");

        BitSet[] bitSetArray = new BitSet[SPEED_TEST_INSTANCE_COUNT];
        Random random = new Random();

        System.out.println("Filling bit sets for testing...");

        for (int i = 0; i < bitSetArray.length; i++) {
            BitSet set;
            bitSetArray[i] = set = new BitSet(ARRAY_SIZE);

            // Fill the bit set with random values
            for (int idx = 0; idx < ARRAY_SIZE; idx++) {
                set.set(idx, random.nextBoolean());
            }
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
                        synchronized (sync) {
                            bitSetArray[idx].set(idx, false);
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
                + " bits (in "
                + SPEED_TEST_INSTANCE_COUNT
                + " ORIGINAL sets) (parallel): "
                + nanoseconds / 1000
                + "μs ("
                + nanoseconds / 1000000
                + "ms)");
    }
}
