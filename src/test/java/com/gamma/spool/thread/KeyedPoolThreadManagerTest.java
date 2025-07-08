package com.gamma.spool.thread;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.config.ThreadManagerConfig;

/**
 * Tests for the {@link KeyedPoolThreadManager} class.
 */
public class KeyedPoolThreadManagerTest {

    private KeyedPoolThreadManager threadManager;
    private static final String TEST_POOL_NAME = "TestKeyedPool";
    private static final int TEST_THREAD_LIMIT = 3;
    private static final int TEST_THREAD_KEY_1 = 1;
    private static final int TEST_THREAD_KEY_2 = 2;

    @Before
    public void setUp() {
        threadManager = new KeyedPoolThreadManager(TEST_POOL_NAME, TEST_THREAD_LIMIT);
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
        assertTrue(
            "Pool should contain default thread key",
            threadManager.getKeys()
                .contains(KeyedPoolThreadManager.DEFAULT_THREAD_KEY));
    }

    @Test
    public void testTerminatePool() {
        threadManager.startPool();
        assertTrue("Pool should be started", threadManager.isStarted());
        threadManager.terminatePool();
        assertFalse("Pool should be terminated", threadManager.isStarted());
        assertEquals("Pool should have no threads after termination", 0, threadManager.getNumThreads());
    }

    @Test
    public void testAddKeyedThread() {
        threadManager.startPool();
        int initialThreadCount = threadManager.getNumThreads();

        threadManager.addKeyedThread(TEST_THREAD_KEY_1, "TestThread1");
        assertEquals(
            "Thread count should increase after adding a thread",
            initialThreadCount + 1,
            threadManager.getNumThreads());
        assertTrue(
            "Pool should contain the added thread key",
            threadManager.getKeys()
                .contains(TEST_THREAD_KEY_1));
    }

    @Test
    public void testRemoveKeyedThread() {
        threadManager.startPool();
        threadManager.addKeyedThread(TEST_THREAD_KEY_1, "TestThread1");
        int threadCountBeforeRemoval = threadManager.getNumThreads();

        threadManager.removeKeyedThread(TEST_THREAD_KEY_1);
        assertEquals(
            "Thread count should decrease after removing a thread",
            threadCountBeforeRemoval - 1,
            threadManager.getNumThreads());
        assertFalse(
            "Pool should not contain the removed thread key",
            threadManager.getKeys()
                .contains(TEST_THREAD_KEY_1));
    }

    @Test
    public void testExecuteTaskOnDefaultThread() {
        threadManager.startPool();
        AtomicBoolean taskExecuted = new AtomicBoolean(false);

        threadManager.execute(() -> { taskExecuted.set(true); });

        threadManager.waitUntilAllTasksDone(false);
        assertTrue("Task should have been executed on default thread", taskExecuted.get());
    }

    @Test
    public void testExecuteTaskOnKeyedThread() {
        threadManager.startPool();
        threadManager.addKeyedThread(TEST_THREAD_KEY_1, "TestThread1");
        AtomicBoolean taskExecuted = new AtomicBoolean(false);

        threadManager.execute(TEST_THREAD_KEY_1, () -> { taskExecuted.set(true); });

        threadManager.waitUntilAllTasksDone(false);
        assertTrue("Task should have been executed on keyed thread", taskExecuted.get());
    }

    @Test
    public void testThreadLimitEnforcement() {
        // Create a manager with a small thread limit
        KeyedPoolThreadManager limitedManager = new KeyedPoolThreadManager("LimitedPool", 1);
        limitedManager.startPool(); // This adds the default thread

        // Try to add more threads than the limit
        limitedManager.addKeyedThread(1, "Thread1");
        limitedManager.addKeyedThread(2, "Thread2");

        // Only the default thread and one additional thread should be created
        assertEquals("Thread count should be limited", 2, limitedManager.getNumThreads());

        // Clean up
        limitedManager.terminatePool();
    }
}
