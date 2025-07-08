package com.gamma.spool.thread;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.config.ThreadManagerConfig;

/**
 * Tests for the {@link TimedOperationThreadManager} class.
 */
public class TimedOperationThreadManagerTest {

    private TimedOperationThreadManager threadManager;
    private static final String TEST_POOL_NAME = "TestTimedPool";
    private static final int TEST_THREAD_COUNT = 2;

    @Before
    public void setUp() {
        threadManager = new TimedOperationThreadManager(TEST_POOL_NAME, TEST_THREAD_COUNT);
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
        assertEquals("Pool should have correct thread count", TEST_THREAD_COUNT, threadManager.threads);
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
    public void testExecuteDelayedTask() {
        threadManager.startPool();
        AtomicBoolean taskExecuted = new AtomicBoolean(false);

        // Execute a task with a short delay
        threadManager.execute(() -> { taskExecuted.set(true); }, 100, TimeUnit.MILLISECONDS);

        // Verify task hasn't executed immediately
        assertFalse("Task should not execute immediately", taskExecuted.get());

        // Wait for the task to complete
        try {
            Thread.sleep(200); // Wait a bit longer than the delay
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();
        }

        threadManager.waitUntilAllTasksDone(false);
        assertTrue("Delayed task should have been executed after delay", taskExecuted.get());
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
    public void testMultipleDelayedTasks() {
        threadManager.startPool();
        AtomicInteger counter = new AtomicInteger(0);

        // Schedule multiple tasks with different delays
        for (int i = 0; i < 5; i++) {
            threadManager.execute(counter::incrementAndGet, 50 + (i * 10), TimeUnit.MILLISECONDS);
        }

        // Wait for all tasks to complete
        try {
            Thread.sleep(200); // Wait longer than the maximum delay
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();
        }

        threadManager.waitUntilAllTasksDone(false);
        assertEquals("All delayed tasks should have been executed", 5, counter.get());
    }

    @Test
    public void testMixedImmediateAndDelayedTasks() {
        threadManager.startPool();
        AtomicInteger immediateCounter = new AtomicInteger(0);
        AtomicInteger delayedCounter = new AtomicInteger(0);

        // Execute immediate tasks
        for (int i = 0; i < 5; i++) {
            threadManager.execute(immediateCounter::incrementAndGet);
        }

        // Execute delayed tasks
        for (int i = 0; i < 5; i++) {
            threadManager.execute(delayedCounter::incrementAndGet, 50, TimeUnit.MILLISECONDS);
        }

        // Wait for immediate tasks to complete
        threadManager.waitUntilAllTasksDone(false);
        assertEquals("All immediate tasks should have been executed", 5, immediateCounter.get());

        // Delayed tasks might not have completed yet
        int delayedCountBeforeWait = delayedCounter.get();

        // Wait for delayed tasks to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();
        }

        threadManager.waitUntilAllTasksDone(false);
        assertEquals("All delayed tasks should have been executed", 5, delayedCounter.get());
        assertTrue(
            "Some delayed tasks should have executed after waiting",
            delayedCounter.get() > delayedCountBeforeWait);
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
