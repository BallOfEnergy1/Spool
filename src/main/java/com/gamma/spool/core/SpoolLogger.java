package com.gamma.spool.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.thread.ManagerNames;

@SuppressWarnings("unused")
public class SpoolLogger {

    public static final Logger logger = LogManager.getLogger("Spool");
    private static final ConcurrentHashMap<String, Long> lastLogTime = new ConcurrentHashMap<>();
    private static final long DEFAULT_RATE_LIMIT = TimeUnit.SECONDS.toMillis(1);

    public SpoolLogger() {}

    public static void debug(String message, Object... args) {
        if (DebugConfig.debugLogging) logger.info(message, args);
    }

    public static void debugWarn(String message, Object... args) {
        if (DebugConfig.debugLogging) logger.info(message, args);
    }

    public static <T> void compatInfo(String message, Object... args) {
        logger.info("[Compat]: " + message, args);
    }

    public static <T> void compatFatal(String message, Object... args) {
        logger.fatal("[Compat]: " + message, args);
    }

    public static <T> void asmInfo(T that, String message, Object... args) {
        if (DebugConfig.logASM) logger.info(
            "[" + that.getClass()
                .getSimpleName() + "]: " + message,
            args);
    }

    public static void info(String message, Object... args) {
        logger.info(message, args);
    }

    public static void warn(String message, Object... args) {
        logger.warn(message, args);
    }

    public static void error(String message, Object... args) {
        logger.error(message, args);
    }

    public static boolean debugRateLimited(String message, Object... args) {
        if (DebugConfig.debugLogging) {
            boolean canLog = canLog(message);
            if (canLog) logger.debug(message, args);
            return canLog;
        }
        return false;
    }

    public static boolean infoRateLimited(String message, Object... args) {
        boolean canLog = canLog(message);
        if (canLog) logger.info(message, args);
        return canLog;
    }

    public static boolean warnRateLimited(String message, Object... args) {
        boolean canLog = canLog(message);
        if (canLog) logger.warn(message, args);
        return canLog;
    }

    public static boolean warnRateLimited(String message, String managerName, Object... args) {
        boolean canLog = canLog(message + managerName);
        if (canLog) logger.warn(message, managerName, args);
        return canLog;
    }

    public static boolean warnRateLimited(String message, ManagerNames managerName, Object... args) {
        return warnRateLimited(message, managerName.getName(), args);
    }

    public static boolean errorRateLimited(String message, Object... args) {
        boolean canLog = canLog(message);
        if (canLog) logger.error(message, args);
        return canLog;
    }

    private static boolean canLog(String message) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastLogTime.get(message);

        if (lastTime == null || currentTime - lastTime >= DEFAULT_RATE_LIMIT) {
            lastLogTime.put(message, currentTime);
            return true;
        }
        return false;
    }

}
