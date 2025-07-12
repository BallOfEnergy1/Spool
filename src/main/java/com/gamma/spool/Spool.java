package com.gamma.spool;

import java.util.Arrays;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.thread.ForkThreadManager;
import com.gamma.spool.thread.IThreadManager;
import com.gamma.spool.thread.KeyedPoolThreadManager;
import com.gamma.spool.thread.ManagerNames;
import com.gamma.spool.thread.ThreadManager;
import com.gamma.spool.util.caching.RegisteredCache;
import com.gamma.spool.util.distance.DistanceThreadingUtil;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ICrashCallable;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

@SuppressWarnings("unused")
@Mod(modid = Spool.MODID, version = Spool.VERSION, guiFactory = "com.gamma.spool.config.SpoolGuiConfigFactory")
@EventBusSubscriber
public class Spool {

    public static final String MODID = "spool";
    public static final String VERSION = "@VERSION@";

    public static final Object2ObjectArrayMap<ManagerNames, IThreadManager> registeredThreadManagers = new Object2ObjectArrayMap<>();

    public static final Object2ObjectArrayMap<ManagerNames, RegisteredCache> registeredCaches = new Object2ObjectArrayMap<>();

    public static boolean isHodgepodgeLoaded;
    public static boolean isGTNHLibLoaded; // Just because.

    @EventHandler
    public static void preInit(FMLPreInitializationEvent event) {

        SpoolLogger.info("Hello world!");

        isHodgepodgeLoaded = Loader.isModLoaded("hodgepodge");

        isGTNHLibLoaded = Loader.isModLoaded("gtnhlib");

        FMLCommonHandler.instance()
            .registerCrashCallable(new ICrashCallable() {

                public String call() {
                    StringBuilder builder = new StringBuilder(
                        "!! Crashes may be caused by Spool's incompatibility with other mods !!");
                    for (IThreadManager manager : registeredThreadManagers.values()) {
                        builder.append("\n\t\t");
                        builder.append(manager.getName());

                        builder.append("\n\t\t\tPool manager: ");
                        builder.append(
                            manager.getClass()
                                .getSimpleName());

                        if (ThreadsConfig.isDistanceThreadingEnabled()
                            && manager == DistanceThreadingUtil.getKeyedPool())
                            builder.append("\n\t\t\tPool is linked to DistanceThreadingUtil");

                        builder.append("\n\t\t\tPool active: ");
                        builder.append(manager.isStarted());

                        builder.append("\n\t\t\tThread count: ");
                        builder.append(manager.getNumThreads());

                        if (manager instanceof KeyedPoolThreadManager) {
                            builder.append("\n\t\t\tUsed keys: ");
                            IntSet set = ((KeyedPoolThreadManager) manager).getKeys();
                            builder.append(Arrays.toString(set.toArray()));

                            builder.append("\n\t\t\tRemapped keys: ");
                            set = ((KeyedPoolThreadManager) manager).getMappedKeys();
                            builder.append(Arrays.toString(set.toArray()));
                        }
                    }

                    return builder.toString();
                }

                public String getLabel() {
                    return "Spool Info";
                }
            });
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {

        SpoolLogger.info("Spool beginning initialization...");

        if (ThreadsConfig.shouldDistanceThreadingBeDisabled()) {
            SpoolLogger.logger
                .warn("Distance threading option has been disabled, experimental threading already enabled!");
            ThreadsConfig.forceDisableDistanceThreading = true;
        }

        SpoolLogger.info("Spool experimental threading enabled: " + ThreadsConfig.isExperimentalThreadingEnabled());
        SpoolLogger.info("Spool distance threading enabled: " + ThreadsConfig.isDistanceThreadingEnabled());
        SpoolLogger.info("Spool dimension threading enabled: " + ThreadsConfig.isDimensionThreadingEnabled());

        Spool.startPools();
        SpoolLogger.info("Spool initialization complete.");
    }

    public static void startPools() {
        if (ThreadsConfig.isExperimentalThreadingEnabled()) {

            SpoolLogger.warn("Spool experimental threading enabled, issues may arise!");

            Spool.registeredThreadManagers.put(
                ManagerNames.ENTITY,
                new ForkThreadManager(ManagerNames.ENTITY.getName(), ThreadsConfig.entityThreads));
            SpoolLogger.logger.info(">Entity manager initialized.");

            Spool.registeredThreadManagers.put(
                ManagerNames.BLOCK,
                new ForkThreadManager(ManagerNames.BLOCK.getName(), ThreadsConfig.blockThreads));
            SpoolLogger.logger.info(">Block manager initialized.");

        }

        startDistanceManager();

        if (ThreadsConfig.isDimensionThreadingEnabled()) {

            KeyedPoolThreadManager dimensionManager = new KeyedPoolThreadManager(
                ManagerNames.DIMENSION.getName(),
                ThreadsConfig.dimensionMaxThreads);
            dimensionManager.addKeyedThread(0, "Dimension-" + 0 + "-Thread");

            registeredThreadManagers.put(ManagerNames.DIMENSION, dimensionManager);
            SpoolLogger.logger.info(">Dimension manager initialized.");
        }
    }

    public static void startDistanceManager() {
        if (ThreadsConfig.isDistanceThreadingEnabled()) {

            KeyedPoolThreadManager pool = new KeyedPoolThreadManager(
                ManagerNames.DISTANCE.getName(),
                ThreadsConfig.distanceMaxThreads);

            Spool.registeredThreadManagers.put(ManagerNames.DISTANCE, pool);
            registeredCaches.put(ManagerNames.DISTANCE, new RegisteredCache(DistanceThreadingUtil.cache));

            SpoolLogger.logger.info(">Distance manager initialized.");
        }
    }

    @EventHandler
    public void serverStarted(FMLServerStartingEvent event) {
        SpoolLogger.info("Starting Spool threads...");

        registeredThreadManagers.values()
            .forEach(IThreadManager::startPoolIfNeeded);

        if (ThreadsConfig.isDistanceThreadingEnabled()) {
            if (event.getServer()
                .isSinglePlayer()) {
                SpoolLogger.info("Singleplayer detected, tearing down DistanceThreadingUtil if initialized...");
                if (DistanceThreadingUtil.isInitialized()) {
                    DistanceThreadingUtil.teardown();
                }

                registeredCaches.remove(ManagerNames.DISTANCE);
                registeredThreadManagers.remove(ManagerNames.DISTANCE);
                ThreadsConfig.forceDisableDistanceThreading = true;
            } else {
                DistanceThreadingUtil.init(registeredThreadManagers.get(ManagerNames.DISTANCE));
            }
        }

        SpoolLogger.info("Spool threads started successfully.");
    }

    @EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        SpoolLogger.info("Stopping Spool processing threads to conserve system resources.");
        registeredThreadManagers.values()
            .forEach(IThreadManager::terminatePool);

        if (ThreadsConfig.isDistanceThreadingEnabled()) DistanceThreadingUtil.teardown();

        if (ThreadsConfig.forceDisableDistanceThreading && ThreadsConfig.shouldDistanceThreadingBeEnabled()) {
            // If it was disabled due to single-player.
            SpoolLogger.info("Disabled distance threading override.");
            ThreadsConfig.forceDisableDistanceThreading = false;
            startDistanceManager();
        }

        SpoolLogger.info("Spool threads terminated.");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPreServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && ThreadsConfig.isDistanceThreadingEnabled())
            DistanceThreadingUtil.onTick(); // Check for instability at the start of the tick.
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientJoin(FMLNetworkEvent.ServerConnectionFromClientEvent event) {
        MinecraftServer mc = MinecraftServer.getServer();
        if (mc.isSinglePlayer() && !mc.isDedicatedServer()) {
            // We're inside a client...
            if (!((IntegratedServer) mc).getPublic()) return; // Not multiplayer.

            // This at some point became a LAN server that someone is joining.
            // Distance threading is only disabled *because of single-player*
            // (and because of experimental threading), so it should be re-enabled here.
            if (ThreadsConfig.shouldDistanceThreadingBeEnabled() && ThreadsConfig.forceDisableDistanceThreading) {
                ThreadsConfig.forceDisableDistanceThreading = false;
                SpoolLogger.info("Distance threading re-enabled due to LAN server presence.");

                startDistanceManager();

                SpoolLogger.info("Initializing DistanceThreadingUtil...");
                DistanceThreadingUtil.init(registeredThreadManagers.get(ManagerNames.DISTANCE));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerJoinTimeHandler.onPlayerLeave(event);
        MinecraftServer mc = MinecraftServer.getServer();
        if (ThreadsConfig.isDistanceThreadingEnabled()) {

            DistanceThreadingUtil.onClientLeave(event.player);

            if (mc.isSinglePlayer() && mc.getCurrentPlayerCount() > 1) {
                SpoolLogger.info("Singleplayer detected, tearing down DistanceThreadingUtil if initialized...");
                if (DistanceThreadingUtil.isInitialized()) {
                    DistanceThreadingUtil.teardown();
                }

                registeredCaches.remove(ManagerNames.DISTANCE);
                registeredThreadManagers.remove(ManagerNames.DISTANCE);
                ThreadsConfig.forceDisableDistanceThreading = true;
            } else if (mc.getCurrentPlayerCount() != 1) {
                DistanceThreadingUtil.init(registeredThreadManagers.get(ManagerNames.DISTANCE));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerJoinTimeHandler.onPlayerJoin(event);
        if (ThreadsConfig.isDistanceThreadingEnabled() && DistanceThreadingUtil.isInitialized()) {
            SpoolLogger.debug("Building player and chunk executor maps...");
            DistanceThreadingUtil.rebuildPlayerMap();
            DistanceThreadingUtil.rebuildChunkMap();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDebugScreen(RenderGameOverlayEvent.Text event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!mc.gameSettings.showDebugInfo) return;

        event.right.add("");

        if (!mc.isIntegratedServerRunning()) {
            // This detects if this is a client connecting to a server.
            // This also works for LAN servers (hopefully)!
            event.right.add("Spool unable to show information; MP server detected.");
            return;
        }

        event.right.add("Spool Stats");
        event.right.add("Experimental threading: " + ThreadsConfig.isExperimentalThreadingEnabled());
        event.right.add("Distance threading: " + ThreadsConfig.isDistanceThreadingEnabled());
        event.right.add("Dimension threading: " + ThreadsConfig.isDimensionThreadingEnabled());
        for (IThreadManager manager : registeredThreadManagers.values()) {
            event.right.add("");
            event.right.add("Pool: " + manager.getName());
            event.right.add(
                "Manager class: " + manager.getClass()
                    .getSimpleName());
            event.right.add(String.format("Number of threads: %d", manager.getNumThreads()));
            if (!DebugConfig.debug) event.right.add("Additional information unavailable (debugging inactive).");
            else {
                event.right.add(String.format("Time spent in thread: %.2fms", manager.getTimeExecuting() / 1000000d));
                event.right
                    .add(String.format("Overhead spent on thread: %.2fms", manager.getTimeOverhead() / 1000000d));
                event.right
                    .add(String.format("Time spent waiting on thread: %.2fms", manager.getTimeWaiting() / 1000000d));
                event.right.add(
                    String.format(
                        "Total time saved by thread: %.2fms",
                        (manager.getTimeExecuting() - manager.getTimeOverhead() - manager.getTimeWaiting())
                            / 1000000d));

                if (manager instanceof ThreadManager) {
                    event.right.add(String.format("Futures queue size: %d", ((ThreadManager) manager).futuresSize));
                    event.right.add(String.format("Overflow queue size: %d", ((ThreadManager) manager).overflowSize));
                }

                if (manager instanceof KeyedPoolThreadManager) {
                    event.right.add(
                        String.format(
                            "Used keys: %s",
                            Arrays.toString(
                                ((KeyedPoolThreadManager) manager).getKeys()
                                    .toArray())));

                    event.right.add(
                        String.format(
                            "Remapped keys: %s",
                            Arrays.toString(
                                ((KeyedPoolThreadManager) manager).getMappedKeys()
                                    .toArray())));
                    if (ThreadsConfig.isDistanceThreadingEnabled() && manager == DistanceThreadingUtil.getKeyedPool()) {
                        event.right.add("Pool is linked to DistanceThreadingUtil");
                    }
                }
            }
        }

        for (Map.Entry<ManagerNames, RegisteredCache> cache : registeredCaches.entrySet()) {
            event.right.add("");
            RegisteredCache registeredCache = cache.getValue();
            event.right.add(
                "Cache name: " + registeredCache.getCache()
                    .getNameForDebug());
            event.right.add(
                "Attached to: " + cache.getKey()
                    .getName());

            if (SpoolCoreMod.OBJECT_DEBUG) {
                event.right.add(
                    String.format(
                        "Spool cache calculated size: %.2fMB (%d Bytes)",
                        (double) registeredCache.getCachedSize() / 1000000d,
                        registeredCache.getCachedSize()));
            } else event.right.add("Unable to compute cache size.");
        }
    }
}
