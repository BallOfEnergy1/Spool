package com.gamma.spool.statistics;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.DimensionManager;

import com.gamma.spool.api.statistics.IStatisticReceiver;
import com.gamma.spool.api.statistics.IThreadManagerView;
import com.gamma.spool.api.statistics.SpoolStatistic;
import com.gamma.spool.api.statistics.Statistic;
import com.gamma.spool.api.statistics.TimeAmount;
import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.core.SpoolLogger;
import com.gamma.spool.core.SpoolManagerOrchestrator;
import com.gamma.spool.thread.ManagerNames;
import com.gamma.spool.util.distance.DistanceThreadingPlayerUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

public class StatisticsManager {

    private static final Object2ObjectArrayMap<IStatisticReceiver, StatisticReceiverInfo> registered = new Object2ObjectArrayMap<>();

    public static int receiverCount() {
        return registered.size();
    }

    public static void startAll() {
        registered.values()
            .forEach(StatisticReceiverInfo::init);
    }

    public static void stopAll() {
        registered.values()
            .forEach(StatisticReceiverInfo::stop);
    }

    public static void registerReceiver(String modid, IStatisticReceiver receiver) {
        registered.put(receiver, new StatisticReceiverInfo(receiver, modid, receiver.interval()));
    }

    private static Statistic buildStatistics() {

        MinecraftServer mc = MinecraftServer.getServer();
        if (mc.isSinglePlayer() && !mc.isDedicatedServer()) {
            // Client.
            SpoolStatistic clientStatistic = new SpoolStatistic(
                -1,
                null,
                ImmutableList.copyOf(SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS.values()),
                null,
                null,
                null,
                null);
            return new Statistic(clientStatistic);
        }
        // Server.

        ImmutableMap<Integer, IThreadManagerView> dimensionThreadingManagerMap = null;

        if (ThreadsConfig.enableDimensionThreading) {
            ImmutableMap.Builder<Integer, IThreadManagerView> dimensionThreadingManagerMapBuilder = new ImmutableMap.Builder<>();

            IThreadManagerView distanceThreadingManager = SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS
                .get(ManagerNames.DIMENSION);

            for (int entry : DimensionManager.getIDs()) {
                dimensionThreadingManagerMapBuilder.put(entry, distanceThreadingManager);
            }

            dimensionThreadingManagerMap = dimensionThreadingManagerMapBuilder.build();
        }

        ImmutableMap<UUID, Integer> distanceThreadingPlayerHashcodeMap = null;

        if (ThreadsConfig.enableDistanceThreading) {
            ImmutableMap.Builder<UUID, Integer> distanceThreadingPlayerHashcodeMapBuilder = new ImmutableMap.Builder<>();

            for (EntityPlayerMP player : mc.getConfigurationManager().playerEntityList) {
                distanceThreadingPlayerHashcodeMapBuilder
                    .put(player.getUniqueID(), DistanceThreadingPlayerUtil.playerHashcode(player));
            }

            distanceThreadingPlayerHashcodeMap = distanceThreadingPlayerHashcodeMapBuilder.build();
        }

        ImmutableMap.Builder<Integer, Double> worldMSPTBuilder = new ImmutableMap.Builder<>();
        for (Map.Entry<Integer, long[]> entry : mc.worldTickTimes.entrySet()) {
            worldMSPTBuilder.put(entry.getKey(), MathHelper.average(entry.getValue()) * 1.0E-06);
        }

        ImmutableMap<Integer, Double> worldMSPT = worldMSPTBuilder.build();

        new SpoolStatistic(
            MathHelper.average(mc.tickTimeArray) * 1.0E-06,
            worldMSPT,
            ImmutableList.copyOf(SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS.values()),
            dimensionThreadingManagerMap,
            SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS.get(ManagerNames.DISTANCE),
            SpoolManagerOrchestrator.REGISTERED_CACHES.get(ManagerNames.DISTANCE)
                .getCache(),
            distanceThreadingPlayerHashcodeMap);
        return new Statistic(null);
    }

    private static double convertToMicroseconds(double amount, TimeUnit from) {
        if (from.ordinal() < TimeUnit.MICROSECONDS.ordinal()) {
            return amount / from.convert(1, TimeUnit.MICROSECONDS);
        } else {
            return amount * TimeUnit.MICROSECONDS.convert(1, from);
        }
    }

    private static class StatisticReceiverInfo {

        public final IStatisticReceiver receiver;
        public final String modid;
        public final TimeAmount interval;
        private final ScheduledExecutorService executor;
        private volatile ScheduledFuture<?> lastFuture;

        private StatisticReceiverInfo(IStatisticReceiver receiver, String modid, TimeAmount interval) {
            this.receiver = receiver;
            this.modid = modid;
            if (convertToMicroseconds(interval.getTime(), interval.getTimeUnit()) < 100.0D)
                this.interval = new TimeAmount(100, TimeUnit.MICROSECONDS);
            else this.interval = interval;
            ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("Spool-" + modid + "-Statistics-Thread")
                .setDaemon(true)
                .build();
            this.executor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        }

        public void init() {
            lastFuture = executor.scheduleWithFixedDelay(() -> {
                if (!receiver.isEnabled()) return;

                Statistic stat = buildStatistics();
                try {
                    receiver.onNewStatistics(stat);
                } catch (Throwable throwable) {
                    SpoolLogger.error("An error occurred in the statistics handler for modid " + modid, throwable);
                }
            }, 0, interval.getTime(), interval.getTimeUnit());
        }

        public void stop() {
            lastFuture.cancel(false);
        }
    }
}
