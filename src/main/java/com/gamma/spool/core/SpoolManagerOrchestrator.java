package com.gamma.spool.core;

import com.gamma.spool.config.ThreadManagerConfig;
import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.thread.ForkThreadManager;
import com.gamma.spool.thread.IThreadManager;
import com.gamma.spool.thread.KeyedPoolThreadManager;
import com.gamma.spool.thread.LBKeyedPoolThreadManager;
import com.gamma.spool.thread.ManagerNames;
import com.gamma.spool.util.caching.RegisteredCache;
import com.gamma.spool.util.distance.DistanceThreadingUtil;
import com.gamma.spool.watchdog.Watchdog;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

public class SpoolManagerOrchestrator {

    public static final Object2ObjectArrayMap<ManagerNames, IThreadManager> REGISTERED_THREAD_MANAGERS = new Object2ObjectArrayMap<>();

    public static final Object2ObjectArrayMap<ManagerNames, RegisteredCache> REGISTERED_CACHES = new Object2ObjectArrayMap<>();

    static Watchdog watchdogThread = new Watchdog();

    static void startPools() {
        if (ThreadsConfig.isExperimentalThreadingEnabled()) {

            SpoolLogger.warn("Spool experimental threading enabled, issues may arise!");

            REGISTERED_THREAD_MANAGERS.put(
                ManagerNames.ENTITY,
                new ForkThreadManager(ManagerNames.ENTITY.getName(), ThreadsConfig.entityThreads));
            SpoolLogger.info("Entity manager initialized.");

            REGISTERED_THREAD_MANAGERS.put(
                ManagerNames.BLOCK,
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

            REGISTERED_THREAD_MANAGERS.put(ManagerNames.DIMENSION, dimensionManager);
            SpoolLogger.info("Dimension manager initialized.");
        }

        if (ThreadsConfig.isThreadedChunkLoadingEnabled()) {
            REGISTERED_THREAD_MANAGERS.put(
                ManagerNames.CHUNK_LOAD,
                new ForkThreadManager(ManagerNames.CHUNK_LOAD.getName(), ThreadsConfig.chunkLoadingThreads));
            SpoolLogger.info("Chunk loading manager initialized.");
        }
    }

    static void startDistanceManager() {
        if (ThreadsConfig.isDistanceThreadingEnabled()) {

            KeyedPoolThreadManager pool = new KeyedPoolThreadManager(
                ManagerNames.DISTANCE.getName(),
                ThreadsConfig.distanceMaxThreads);

            REGISTERED_THREAD_MANAGERS.put(ManagerNames.DISTANCE, pool);
            REGISTERED_CACHES.put(ManagerNames.DISTANCE, new RegisteredCache(DistanceThreadingUtil.cache));

            SpoolLogger.info("Distance manager initialized.");
        }
    }

    static void onPreTick(long tickCounter) {
        if (ThreadsConfig.isDistanceThreadingEnabled()) DistanceThreadingUtil.onTick(); // Check for instability at the
                                                                                        // start of the tick.

        // Every 5 ticks, update the pool (balance and such).
        if (ThreadManagerConfig.useLoadBalancingDimensionThreadManager
            && tickCounter % ThreadManagerConfig.loadBalancerFrequency == 0) {
            for (IThreadManager manager : REGISTERED_THREAD_MANAGERS.values()) {
                if (!(manager instanceof LBKeyedPoolThreadManager)) continue;
                ((LBKeyedPoolThreadManager) manager).updatePool();
            }
        }
    }
}
