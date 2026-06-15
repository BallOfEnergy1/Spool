package com.gamma.spool.core;

import java.util.concurrent.Executor;

import com.gamma.spool.api.annotations.SkipSpoolASMChecks;
import com.gamma.spool.config.ThreadManagerConfig;
import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.thread.ForkThreadManager;
import com.gamma.spool.thread.IThreadManager;
import com.gamma.spool.thread.KeyedPoolThreadManager;
import com.gamma.spool.thread.LBKeyedPoolThreadManager;
import com.gamma.spool.thread.ManagerNames;
import com.gamma.spool.thread.TimedOperationThreadManager;
import com.gamma.spool.util.caching.RegisteredCache;
import com.gamma.spool.util.distance.DistanceThreadingUtil;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;

@SkipSpoolASMChecks(SkipSpoolASMChecks.SpoolASMCheck.ALL)
public class SpoolManagerOrchestrator {

    public static final Int2ObjectArrayMap<IThreadManager> REGISTERED_THREAD_MANAGERS = new Int2ObjectArrayMap<>();

    public static final Int2ObjectArrayMap<RegisteredCache> REGISTERED_CACHES = new Int2ObjectArrayMap<>();

    static void startPools() {

        REGISTERED_THREAD_MANAGERS.put(
            ManagerNames.THREAD_MANAGER_TIMER.ordinal(),
            new TimedOperationThreadManager(ManagerNames.THREAD_MANAGER_TIMER.getName(), 1));
        SpoolLogger.info("Thread manager timer initialized.");

        startDistanceManager();

        if (ThreadsConfig.isDimensionThreadingEnabled()) {

            KeyedPoolThreadManager dimensionManager;
            if (ThreadManagerConfig.useLoadBalancingDimensionThreadManager)
                dimensionManager = new LBKeyedPoolThreadManager(
                    ManagerNames.DIMENSION.getName(),
                    ThreadsConfig.dimensionMaxThreads);
            else dimensionManager = new KeyedPoolThreadManager(
                ManagerNames.DIMENSION.getName(),
                ThreadsConfig.dimensionMaxThreads);

            REGISTERED_THREAD_MANAGERS.put(ManagerNames.DIMENSION.ordinal(), dimensionManager);
            SpoolLogger.info("Dimension manager initialized.");
        }

        if (ThreadsConfig.isEntityAIThreadingEnabled()) {
            REGISTERED_THREAD_MANAGERS.put(
                ManagerNames.ENTITY_AI.ordinal(),
                new ForkThreadManager(ManagerNames.ENTITY_AI.getName(), ThreadsConfig.entityAIMaxThreads));
            SpoolLogger.info("Entity AI manager initialized.");
        }
    }

    public static void startDistanceManager() {
        if (ThreadsConfig.isDistanceThreadingEnabled()) {

            KeyedPoolThreadManager pool = new KeyedPoolThreadManager(
                ManagerNames.DISTANCE.getName(),
                ThreadsConfig.distanceMaxThreads);

            REGISTERED_THREAD_MANAGERS.put(ManagerNames.DISTANCE.ordinal(), pool);
            REGISTERED_CACHES.put(ManagerNames.DISTANCE.ordinal(), new RegisteredCache(DistanceThreadingUtil.cache));

            SpoolLogger.info("Distance manager initialized.");
        }
    }

    public static Executor getProperExecutor(boolean toThread, ManagerNames name) {
        if (!toThread) {
            return Runnable::run;
        }
        return SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS.get(name.ordinal());
    }
}
