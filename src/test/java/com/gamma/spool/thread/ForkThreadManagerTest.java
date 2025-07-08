package com.gamma.spool.thread;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.config.ThreadManagerConfig;

/**
 * Tests for the {@link ForkThreadManager} class.
 */
public class ForkThreadManagerTest {

    private ForkThreadManager threadManager;
    private static final String TEST_POOL_NAME = "TestForkPool";
    private static final int TEST_THREAD_COUNT = 2;

    @Before
    public void setUp() {
        threadManager = new ForkThreadManager(TEST_POOL_NAME, TEST_THREAD_COUNT);
        ThreadManagerConfig.globalRunningSingleThreadTimeout = 500;
        ThreadManagerConfig.globalTerminatingSingleThreadTimeout = 2000;
        DebugConfig.debug = true;
        DebugConfig.debugLogging = true;
    }

    @After
    public void tearDown() {
        if (threadManager.isStarted()) {
            threadManager.terminatePool();
        }
    }

    @Test
    public void testStartPool() {
        assertFalse("Pool should not be started initially", threadManager.isStarted());
        threadManager.startPool();
        assertTrue("Pool should be started after startPool()", threadManager.isStarted());
        assertEquals("Pool should have correct name", TEST_POOL_NAME, threadManager.getName());
    }

    @Test
    public void testTerminatePool() {
        threadManager.startPool();
        assertTrue("Pool should be started", threadManager.isStarted());
        threadManager.terminatePool();
        assertFalse("Pool should be terminated", threadManager.isStarted());
    }

    @Test
    public void testExecuteTask() {
        threadManager.startPool();
        AtomicBoolean taskExecuted = new AtomicBoolean(false);

        threadManager.execute(() -> { taskExecuted.set(true); });

        threadManager.waitUntilAllTasksDone(false);
        assertTrue("Task should have been executed", taskExecuted.get());
    }

    @Test
    public void testMultipleTasks() {
        threadManager.startPool();
        AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < 10; i++) {
            threadManager.execute(counter::incrementAndGet);
        }

        threadManager.waitUntilAllTasksDone(false);
        assertEquals("All tasks should have been executed", 10, counter.get());
    }

    @Test
    public void testParallelExecution() {
        threadManager.startPool();

        // Create a latch to synchronize the start of tasks
        CountDownLatch startLatch = new CountDownLatch(1);

        // Create a latch to wait for all tasks to reach a certain point
        CountDownLatch executionLatch = new CountDownLatch(TEST_THREAD_COUNT);

        // Counter to track how many tasks are executing simultaneously
        AtomicInteger concurrentTasks = new AtomicInteger(0);
        AtomicInteger maxConcurrentTasks = new AtomicInteger(0);

        // Create tasks that will increment the counter, wait, and then decrement
        for (int i = 0; i < TEST_THREAD_COUNT; i++) {
            threadManager.execute(() -> {
                try {
                    // Wait for the signal to start
                    startLatch.await();

                    // Increment the counter and update max if needed
                    int current = concurrentTasks.incrementAndGet();
                    int max;
                    do {
                        max = maxConcurrentTasks.get();
                        if (current <= max) break;
                    } while (!maxConcurrentTasks.compareAndSet(max, current));

                    // Signal that this task has reached the execution point
                    executionLatch.countDown();

                    // Wait for all tasks to reach this point
                    executionLatch.await(1, TimeUnit.SECONDS);

                    // Decrement the counter
                    concurrentTasks.decrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread()
                        .interrupt();
                }
            });
        }

        // Signal all tasks to start
        startLatch.countDown();

        // Wait for all tasks to complete
        threadManager.waitUntilAllTasksDone(false);

        // Verify that tasks executed in parallel
        assertEquals("Maximum concurrent tasks should match thread count", TEST_THREAD_COUNT, maxConcurrentTasks.get());
    }

    @Test
    public void testPerformanceMetrics() {
        threadManager.startPool();

        // Execute a task that takes some time
        threadManager.execute(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread()
                    .interrupt();
            }
        });

        threadManager.waitUntilAllTasksDone(false);

        // Verify that time metrics are being tracked
        assertTrue("Execution time should be tracked", threadManager.getTimeExecuting() > 0);
        assertTrue("Waiting time should be tracked", threadManager.getTimeWaiting() > 0);
    }
}
