package com.gamma.spool;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gamma.spool.thread.ForkThreadManager;
import com.gamma.spool.thread.IResizableThreadManager;
import com.gamma.spool.thread.IThreadManager;
import com.gamma.spool.thread.KeyedPoolThreadManager;
import com.gamma.spool.thread.ThreadManager;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ICrashCallable;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLStateEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

@Mod(modid = Spool.MODID, version = Spool.VERSION)
public class Spool {

    public static final String MODID = "spool";
    public static final String VERSION = "@VERSION@";

    public static final Logger logger = LogManager.getLogger("Spool");

    public static final Object2ObjectArrayMap<String, IThreadManager> registeredThreadManagers = new Object2ObjectArrayMap<>();

    public static boolean isHodgepodgeLoaded;

    public static final SpoolConfigManager configManager = new SpoolConfigManager();

    @EventHandler
    public static void preInit(FMLPreInitializationEvent event) {

        logger.info("Hello world!");

        isHodgepodgeLoaded = Loader.isModLoaded("hodgepodge");

        configManager.onPreInit(event);

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

                        builder.append("\n\t\t\tResizable?: ");
                        builder.append(manager instanceof IResizableThreadManager);

                        builder.append("\n\t\t\tPool active: ");
                        builder.append(manager.isStarted());

                        builder.append("\n\t\t\tThread count: ");
                        builder.append(manager.getNumThreads());
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

        logger.info("Spool beginning initialization...");
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(configManager);

        configManager.onInit(event);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        startPools(event);
    }

    public static void startPools(FMLStateEvent state) {
        if (state instanceof FMLPreInitializationEvent) {
            if (configManager.enableExperimentalThreading) {
                Spool.registeredThreadManagers
                    .put("entityManager", new ForkThreadManager("entityManager", configManager.entityThreads));
                Spool.logger.info(">Entity manager initialized.");

                Spool.registeredThreadManagers
                    .put("blockManager", new ForkThreadManager("blockManager", configManager.blockThreads));
                Spool.logger.info(">Block manager initialized.");
            }
        } else if (state instanceof FMLPostInitializationEvent) {
            if (!configManager.enableExperimentalThreading) {
                logger.info("Continuing Spool initialization...");

                KeyedPoolThreadManager dimensionManager = getDimensionManager();

                registeredThreadManagers.put("dimensionManager", dimensionManager);

                logger.info("Spool post-initialization complete.");
            }
        }
    }

    private static KeyedPoolThreadManager getDimensionManager() {
        KeyedPoolThreadManager dimensionManager = new KeyedPoolThreadManager("dimensionManager");

        // Okay, listen, this is a bunch of BS.
        // Yes, technically this `getStaticDimensionIDs()` function is not a *public API*
        // HOWEVER, it just so happens to do exactly what I need it to, which is the fact that it gives me the exact
        // number
        // of dimensions that will be loaded on server start. The pool will dynamically be resized as worlds are loaded,
        // however,
        // it's really just better if it's assigned to a proper value beforehand.
        for (int i : DimensionManager.getStaticDimensionIDs())
            dimensionManager.addKeyedThread(i, "Dimension-" + i + "-Thread");
        return dimensionManager;
    }

    @EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        registeredThreadManagers.values()
            .forEach((manager) -> manager.waitUntilAllTasksDone(false));
        logger.info("Stopping Spool processing threads to conserve system resources.");
        registeredThreadManagers.values()
            .forEach(IThreadManager::terminatePool);
        logger.info("Spool threads terminated.");
    }

    @EventHandler
    public void serverStarted(FMLServerStartingEvent event) {
        logger.info("Starting Spool threads...");
        registeredThreadManagers.values()
            .forEach(IThreadManager::startPoolIfNeeded);
        logger.info("Spool threads started successfully.");
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onDebugScreen(RenderGameOverlayEvent.Text event) {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.showDebugInfo) {
            event.right.add("");
            if (mc.isSingleplayer()) {
                event.right.add("Spool Stats");
                for (IThreadManager manager : registeredThreadManagers.values()) {
                    event.right.add("Pool: " + manager.getName());
                    event.right.add(
                        "Manager class: " + manager.getClass()
                            .getSimpleName());
                    event.right.add("Resizable?: " + (manager instanceof IResizableThreadManager));
                    event.right.add(String.format("Number of threads: %d", manager.getNumThreads()));
                    if (!configManager.debug)
                        event.right.add("Additional information unavailable (debugging inactive).");
                    else {
                        event.right
                            .add(String.format("Time spent in thread: %.2fms", manager.getTimeExecuting() / 1000000d));
                        event.right.add(
                            String.format("Overhead spent on thread: %.2fms", manager.getTimeOverhead() / 1000000d));
                        event.right.add(
                            String.format("Time spent waiting on thread: %.2fms", manager.getTimeWaiting() / 1000000d));
                        event.right.add(
                            String.format(
                                "Total time saved by thread: %.2fms",
                                manager.getTimeExecuting() - manager.getTimeOverhead()
                                    - manager.getTimeWaiting() / 1000000d));
                    }
                    if (manager instanceof ThreadManager) {
                        event.right
                            .add(String.format(" Futures queue size: %d", ((ThreadManager) manager).futuresSize));
                        event.right
                            .add(String.format(" Overflow queue size: %d", ((ThreadManager) manager).overflowSize));
                    }
                }
            } else {
                event.right.add("Spool unable to show information; MP server detected.");
            }
        }
    }
}
