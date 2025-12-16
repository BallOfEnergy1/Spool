package com.gamma.spool.core;

import java.util.Arrays;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import com.gamma.spool.Tags;
import com.gamma.spool.api.SpoolAPI;
import com.gamma.spool.api.annotations.SkipSpoolASMChecks;
import com.gamma.spool.commands.CommandSpool;
import com.gamma.spool.config.APIConfig;
import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.config.ThreadManagerConfig;
import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.db.SpoolDBManager;
import com.gamma.spool.events.PlayerJoinTimeHandler;
import com.gamma.spool.gui.GuiHandler;
import com.gamma.spool.statistics.StatisticsManager;
import com.gamma.spool.thread.IThreadManager;
import com.gamma.spool.thread.KeyedPoolThreadManager;
import com.gamma.spool.thread.LBKeyedPoolThreadManager;
import com.gamma.spool.thread.ManagerNames;
import com.gamma.spool.util.distance.DistanceThreadingUtil;
import com.gamma.spool.watchdog.Watchdog;
import com.google.common.collect.ImmutableList;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ICrashCallable;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.ints.IntSet;

@SuppressWarnings("unused")
@Mod(
    modid = Spool.MODID,
    version = Tags.VERSION,
    dependencies = "required-after:gtnhmixins@[2.0.1,);" + "required-after:unimixins@[0.0.20,);"
        + "required-after:gtnhlib@[0.6.21,);",
    guiFactory = "com.gamma.spool.config.SpoolGuiConfigFactory")
@EventBusSubscriber
@SkipSpoolASMChecks(SkipSpoolASMChecks.SpoolASMCheck.ALL)
public class Spool {

    public static final String MODID = "spool";
    public static final String VERSION = Tags.VERSION;

    @Mod.Instance(MODID)
    public static Spool instance;

    @EventHandler
    public static void preInit(FMLPreInitializationEvent event) {

        SpoolCompat.logChange("STAGE", "Mod lifecycle", "PREINIT");

        SpoolLogger.info("Hello world!");

        SpoolManagerOrchestrator.early();

        SpoolCompat.checkLoadedMods();

        FMLCommonHandler.instance()
            .registerCrashCallable(new ICrashCallable() {

                public String call() {
                    StringBuilder builder = new StringBuilder(
                        "!! Crashes may be caused by Spool's incompatibility with other mods !!");
                    for (IThreadManager manager : SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS.values()) {
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
                            if (manager instanceof LBKeyedPoolThreadManager) {
                                builder.append("\n\t\t\tAll keys: [");
                                for (int i : ((LBKeyedPoolThreadManager) manager).getAllKeys()) {
                                    builder.append(i);
                                    builder.append(":");
                                    builder.append(
                                        String.format(
                                            "%.2f",
                                            ((LBKeyedPoolThreadManager) manager).getLoadFactorForKey(i)));
                                    builder.append(", ");
                                }
                                builder.delete(builder.length() - 2, builder.length());
                                builder.append("]");
                            }

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

        SpoolCompat.logChange("STAGE", "Mod lifecycle", "INIT");

        SpoolLogger.info("Spool beginning initialization...");

        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new GuiHandler());

        if (ThreadsConfig.shouldDistanceThreadingBeDisabled()) {
            SpoolLogger.warn("Distance threading option has been disabled, experimental threading already enabled!");
            ThreadsConfig.forceDisableDistanceThreading = true;
        }

        SpoolLogger.info("Spool experimental threading enabled: " + ThreadsConfig.isExperimentalThreadingEnabled());
        SpoolLogger.info("Spool distance threading enabled: " + ThreadsConfig.isDistanceThreadingEnabled());
        SpoolLogger.info("Spool dimension threading enabled: " + ThreadsConfig.isDimensionThreadingEnabled());
        SpoolLogger.info("Spool chunk threading enabled: " + ThreadsConfig.isThreadedChunkLoadingEnabled());

        SpoolManagerOrchestrator.startPools();

        SpoolLogger.info("Setting up SpoolAPI...");
        setupAPI();
        if (ThreadManagerConfig.enableSpoolWatchdog) {
            SpoolLogger.info("Starting Spool Watchdog...");
            SpoolManagerOrchestrator.watchdogThread.start();
            SpoolLogger.info("Watchdog started!");
        }
        SpoolLogger.info("Spool initialization complete.");
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (!event.modID.equals(MODID)) return;

        IThreadManager manager = SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS
            .get(ManagerNames.DIMENSION.ordinal());

        // Handle the `useLoadBalancingDimensionThreadManager` config option.
        if (manager instanceof LBKeyedPoolThreadManager
            && !ThreadManagerConfig.useLoadBalancingDimensionThreadManager) {
            // Replace it before terminating the old pool to ensure old tasks get completed.
            SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS.put(
                ManagerNames.DIMENSION.ordinal(),
                new KeyedPoolThreadManager(manager.getName(), manager.getNumThreads()));
            manager.terminatePool();

            SpoolLogger.info("Replaced load-balancing dimension thread manager with standard thread manager.");
            SpoolLogger.info("Lag spike may occur; dimension pool will need to be rebuilt.");

        } else if (!(manager instanceof LBKeyedPoolThreadManager)
            && ThreadManagerConfig.useLoadBalancingDimensionThreadManager) {
                // Replace it before terminating the old pool to ensure old tasks get completed.
                SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS.put(
                    ManagerNames.DIMENSION.ordinal(),
                    new LBKeyedPoolThreadManager(manager.getName(), manager.getNumThreads()));
                manager.terminatePool();

                SpoolLogger.info("Replaced standard dimension thread manager with load-balancing thread manager.");
                SpoolLogger.info("Lag spike may occur; dimension pool will need to be rebuilt.");
            }

        // Handle the `enableSpoolWatchdog` config option.
        if (SpoolManagerOrchestrator.watchdogThread.isAlive() && !ThreadManagerConfig.enableSpoolWatchdog) {
            SpoolLogger.info("Stopping Spool Watchdog...");
            SpoolManagerOrchestrator.watchdogThread.interrupt();
            SpoolLogger.info("Watchdog stopped!");
        } else if (!SpoolManagerOrchestrator.watchdogThread.isAlive() && ThreadManagerConfig.enableSpoolWatchdog) {
            SpoolLogger.info("Starting Spool Watchdog...");
            if (SpoolManagerOrchestrator.watchdogThread.isInterrupted())
                SpoolManagerOrchestrator.watchdogThread = new Watchdog();
            SpoolManagerOrchestrator.watchdogThread.start();
            SpoolLogger.info("Watchdog started!");
        }

        // Handle the SDB config options.
        if (DebugConfig.fullCompatLogging && !SpoolDBManager.isRunning) {
            SpoolDBManager.init();
        } else if (!DebugConfig.fullCompatLogging && SpoolDBManager.isRunning) {
            SpoolDBManager.teardown();
        }

        if (DebugConfig.allowSDBConnections) SpoolDBManager.allowConnections();
        else SpoolDBManager.stopConnections();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIMCEvent(FMLInterModComms.IMCEvent event) {
        SpoolIMC.handleIMC(event);
    }

    public static void setupAPI() {
        SpoolAPI.statisticGatheringAllowed = APIConfig.statisticGatheringAllowed;

        SpoolAPI.isInitialized = true;
    }

    @EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        SpoolLogger.info("Starting Spool threads...");

        SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS.values()
            .forEach(IThreadManager::startPoolIfNeeded);
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {

        event.registerServerCommand(new CommandSpool());

        if (ThreadsConfig.isDistanceThreadingEnabled()) {
            if (event.getServer()
                .isSinglePlayer()) {
                SpoolLogger.info("Singleplayer detected, tearing down DistanceThreadingUtil if initialized...");
                if (DistanceThreadingUtil.isInitialized()) {
                    DistanceThreadingUtil.teardown();
                }

                SpoolManagerOrchestrator.REGISTERED_CACHES.remove(ManagerNames.DISTANCE.ordinal());
                SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS.remove(ManagerNames.DISTANCE.ordinal());
                ThreadsConfig.forceDisableDistanceThreading = true;
            } else {
                DistanceThreadingUtil
                    .init(SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS.get(ManagerNames.DISTANCE.ordinal()));
            }
        }

        SpoolLogger.info("Spool threads started successfully.");

        if (APIConfig.statisticGatheringAllowed) {
            SpoolLogger.info("Spool starting statistics handlers...");
            StatisticsManager.startAll();
            SpoolLogger.info("Spool statistics handlers started successfully.");
        }
    }

    @EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {

        if (APIConfig.statisticGatheringAllowed) {
            SpoolLogger.info("Spool stopping statistics handlers...");
            StatisticsManager.stopAll();
            SpoolLogger.info("Spool statistics handlers stopped successfully.");
        }

        SpoolLogger.info("Stopping Spool processing threads to conserve system resources.");
        SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS.values()
            .forEach(IThreadManager::terminatePool);

        if (ThreadsConfig.isDistanceThreadingEnabled()) DistanceThreadingUtil.teardown();

        if (ThreadsConfig.forceDisableDistanceThreading && ThreadsConfig.shouldDistanceThreadingBeEnabled()) {
            // If it was disabled due to single-player.
            SpoolLogger.info("Disabled distance threading override.");
            ThreadsConfig.forceDisableDistanceThreading = false;
            SpoolManagerOrchestrator.startDistanceManager();
        }

        SpoolLogger.info("Spool threads terminated.");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPreServerTick(TickEvent.ServerTickEvent event) {
        // IMC
        ImmutableList<FMLInterModComms.IMCMessage> messages = FMLInterModComms.fetchRuntimeMessages(MODID);
        SpoolIMC.handleIMC(messages);

        if (event.phase == TickEvent.Phase.START) SpoolManagerOrchestrator.onPreTick(
            MinecraftServer.getServer()
                .getTickCounter());
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

                SpoolManagerOrchestrator.startDistanceManager();

                SpoolLogger.info("Initializing DistanceThreadingUtil...");
                DistanceThreadingUtil
                    .init(SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS.get(ManagerNames.DISTANCE.ordinal()));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerJoinTimeHandler.onPlayerLeave(event);
        MinecraftServer mc = MinecraftServer.getServer();
        if (ThreadsConfig.isDistanceThreadingEnabled()) {

            DistanceThreadingUtil.onClientLeave(event.player);

            if (!mc.isDedicatedServer() && (mc.getCurrentPlayerCount() - 1) == 1) {
                SpoolLogger.info("Singleplayer detected, tearing down DistanceThreadingUtil if initialized...");
                if (DistanceThreadingUtil.isInitialized()) {
                    DistanceThreadingUtil.teardown();
                }

                SpoolManagerOrchestrator.REGISTERED_CACHES.remove(ManagerNames.DISTANCE.ordinal());
                SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS.remove(ManagerNames.DISTANCE.ordinal());
                ThreadsConfig.forceDisableDistanceThreading = true;
            }
        }
    }

    // TODO: Probably find a better way than just rebuilding the entire map every time.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerJoinTimeHandler.onPlayerJoin(event);
        if (ThreadsConfig.isDistanceThreadingEnabled() && DistanceThreadingUtil.isInitialized()) {
            SpoolLogger.info("Rebuilding player and chunk executor buckets; reason: Player join...");
            DistanceThreadingUtil.rebuildPlayerMap();
            DistanceThreadingUtil.rebuildChunkMap();
        }
    }

    // TODO: Probably find a better way than just rebuilding the entire map every time.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (ThreadsConfig.isDistanceThreadingEnabled() && DistanceThreadingUtil.isInitialized()) {
            SpoolLogger.info("Rebuilding player and chunk executor buckets; reason: Player changed dimensions...");
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
        event.right.add("Entity AI threading: " + ThreadsConfig.isEntityAIThreadingEnabled());
        event.right.add("Chunk threading: " + ThreadsConfig.isThreadedChunkLoadingEnabled());
    }

    @NetworkCheckHandler
    public boolean checkModList(Map<String, String> versions, Side side) {
        return true;
    }
}
