package com.gamma.spool.core;

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

        if (ThreadsConfig.isExperimentalThreadingEnabled()) {

            SpoolLogger.warn("Spool experimental threading enabled, issues may arise!");

            REGISTERED_THREAD_MANAGERS.put(
                ManagerNames.ENTITY.ordinal(),
                new ForkThreadManager(ManagerNames.ENTITY.getName(), ThreadsConfig.entityThreads));
            SpoolLogger.info("Entity manager initialized.");

            REGISTERED_THREAD_MANAGERS.put(
                ManagerNames.BLOCK.ordinal(),
                new ForkThreadManager(ManagerNames.BLOCK.getName(), ThreadsConfig.blockThreads));
            SpoolLogger.info("Block manager initialized.");

        }

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

        if (ThreadsConfig.isThreadedChunkLoadingEnabled()) {
            REGISTERED_THREAD_MANAGERS.put(
                ManagerNames.CHUNK_LOAD.ordinal(),
                new ForkThreadManager(ManagerNames.CHUNK_LOAD.getName(), ThreadsConfig.chunkLoadingThreads));
            SpoolLogger.info("Chunk loading manager initialized.");
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
}
