package com.gamma.spool.watchdog;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gamma.gammalib.graphical.DeadlockMessage;
import com.gamma.spool.config.ThreadManagerConfig;
import com.gamma.spool.core.SpoolManagerOrchestrator;
import com.gamma.spool.thread.IThreadManager;

import cpw.mods.fml.common.FMLCommonHandler;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class Watchdog extends Thread {

    public static final Logger logger = LogManager.getLogger("Spool-Watchdog");
    public static final IntList watchedThreadManagers = new IntArrayList();

    @Override
    public synchronized void start() {
        this.setDaemon(true);
        this.setName("Spool-Watchdog");
        super.start();
        logger.info("Watchdog started.");
    }

    @Override
    public void run() {
        while (true) {
            try {
                long start = System.currentTimeMillis();

                ThreadMXBean tmx = ManagementFactory.getThreadMXBean();

                // Find deadlocked threads.
                long[] ids = tmx.findDeadlockedThreads();
                if (ids != null) foundDeadlocks(tmx.getThreadInfo(ids, true, true));

                // Check for thread manager deadlocks.
                if (ThreadManagerConfig.enableThreadManagerWatchdog) {
                    for (Int2ObjectMap.Entry<IThreadManager> entry : SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS
                        .int2ObjectEntrySet()) {
                        IThreadManager manager = entry.getValue();
                        Optional<Throwable> throwable = manager.getPendingExceptionIfAny();
                        if (throwable.isPresent()) {
                            logger
                                .warn("Thread manager {} has a pending exception.", manager.getName(), throwable.get());
                            if (watchedThreadManagers.contains(entry.getIntKey())) {
                                logger.fatal("This thread manager has had multiple errors occur.");
                                terminate();
                                continue;
                            }
                            logger.warn(
                                "The Watchdog will attempt to restart this thread manager. If a second failure is detected, the game will crash!");
                            try {
                                manager.terminatePool();
                                manager.startPool();
                            } catch (Throwable e) {
                                logger.fatal("Failed to restart thread manager.", e);
                                terminate();
                            }
                            watchedThreadManagers.add(entry.getIntKey());
                        }
                    }
                }

                Thread.sleep(
                    Math.max(0, ThreadManagerConfig.spoolWatchdogFrequency - (System.currentTimeMillis() - start)));
            } catch (InterruptedException e) {
                logger.warn("Watchdog thread interrupted.");
                break;
            } catch (Throwable throwable) {
                logger.error("Watchdog thread encountered an error!", throwable);
                break;
            }
        }
        logger.error("Spool Watchdog thread will not be restarted.");
    }

    private void foundDeadlocks(ThreadInfo[] threads) {
        final BigErrorBuilder builder = new BigErrorBuilder("Deadlock detected!");
        builder.append("The following threads are deadlocked:");
        for (ThreadInfo thread : threads) {
            builder.append("  Thread %s (%d)", thread.getThreadName(), thread.getThreadId());

            // Stacktrace
            builder.append("    Stacktrace: ");
            StackTraceElement[] elements = Arrays.stream(thread.getStackTrace())
                .limit(9)
                .toArray(StackTraceElement[]::new);
            for (StackTraceElement element : elements) {
                String string = element.getClassName();

                string += "." + element.getMethodName()
                    + "("
                    + (element.isNativeMethod() ? "Native Method)"
                        : (element.getFileName() != null && element.getLineNumber() >= 0
                            ? element.getFileName() + ":" + element.getLineNumber() + ")"
                            : (element.getFileName() != null ? element.getFileName() + ")" : "Unknown Source)")));

                builder.append("      %s", string);
            }

            // Locked synchronizers
            builder.append("    Locked synchronizers: ");
            for (LockInfo locks : thread.getLockedSynchronizers()) {
                builder.append("      %s", locks.toString());
            }

            // Locked monitors
            builder.append("    Locked monitors: ");
            for (LockInfo locks : thread.getLockedMonitors()) {
                builder.append("      %s", locks.toString());
            }

            // Blocked by
            builder.append("    Blocked by: ");
            if (thread.getLockInfo() == null) {
                builder.append(
                    "      %s",
                    thread.getLockInfo()
                        .toString());
            } else {
                builder.append("      None");
            }
        }

        builder.finalizeAndPost();

        DeadlockMessage.showError(
            "Spool - Fatal Error",
            "Spool has detected a severe deadlock and is unable to recover the instance.\nSee logs for more details.");

        terminate(true);
    }

    /**
     * Class for building large error blocks like the ones that Forge (FMLLog) offers (but with more content).
     */
    private static class BigErrorBuilder {

        /**
         * The builder for the large error.
         */
        private final StringBuilder builder = new StringBuilder();

        public BigErrorBuilder(String title) {
            builder.append("****************************************");
            builder.append("\n* ")
                .append(title)
                .append("\n* ");
        }

        /**
         * Appends a line to the error block.
         */
        public BigErrorBuilder append(String message) {
            builder.append("\n* ")
                .append(message);
            return this;
        }

        /**
         * Appends a line with params to the error block.
         */
        public BigErrorBuilder append(String message, Object... args) {
            builder.append("\n* ")
                .append(String.format(message, args));
            return this;
        }

        /**
         * Finalizes the large error block and sends it to the console via the logger.
         */
        public void finalizeAndPost() {
            builder.append("\n****************************************");

            for (String line : builder.toString()
                .split("\n")) {
                logger.error(line);
            }
        }
    }

    private static void terminate() {
        terminate(false);
    }

    private static void terminate(boolean hardExit) {
        logger.fatal("Watchdog terminating instance.");

        FMLCommonHandler.instance()
            .exitJava(1, hardExit);
    }
}
