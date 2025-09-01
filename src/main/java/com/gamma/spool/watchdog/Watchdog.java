package com.gamma.spool.watchdog;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gamma.spool.graphical.SpoolMessage;

import cpw.mods.fml.common.FMLCommonHandler;

public class Watchdog extends Thread {

    public static final Logger logger = LogManager.getLogger("Spool-Watchdog");
    public static final long FREQUENCY = 1_000; // Once every 1_000 millis; Once per second.

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

                Thread.sleep(Math.max(0, FREQUENCY - (System.currentTimeMillis() - start)));
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

        SpoolMessage.showError(
            "Spool - Fatal Error",
            "Spool has detected a severe deadlock and is unable to recover.\nSee logs for more details.");

        logger.fatal("Watchdog terminating instance.");

        FMLCommonHandler.instance()
            .exitJava(1, false);
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
}
