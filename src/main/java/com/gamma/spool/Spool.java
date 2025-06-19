package com.gamma.spool;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gamma.spool.thread.ForkThreadManager;
import com.gamma.spool.thread.IThreadManager;
import com.gamma.spool.thread.ThreadManager;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ICrashCallable;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

@Mod(modid = Spool.MODID, version = Spool.VERSION)
public class Spool {

    public static final String MODID = "spool";
    public static final String VERSION = "@VERSION@";

    public static final Logger logger = LogManager.getLogger("Spool");

    public static final Object2ObjectArrayMap<String, IThreadManager> registeredThreadManagers = new Object2ObjectArrayMap<>();

    public static Configuration config;

    public final static boolean debug = false;

    @EventHandler
    public static void preInit(FMLPreInitializationEvent event) {

        logger.info("Hello world!");

        config = new Configuration(event.getSuggestedConfigurationFile());

        config.load();

        /*
         * This issue has since been fixed with the priority change in `MinecraftServerMixin`.
         * boolean crashOnHodgepodge = config.get(Configuration.CATEGORY_GENERAL, "crashOnHodgepodge", true)
         * .getBoolean();
         * if (Loader.isModLoaded("hodgepodge") && crashOnHodgepodge) {
         * throw new IllegalStateException(
         * "Hodgepodge has been detected on the instance."
         * +
         * " Due to both Hodgepodge and Spool using mixins to fiddle inside of Minecraft classes, this crash is here to prevent crashes due to incompatibilities between the two."
         * + " This crash can be disabled in the config by setting 'crashOnHodgepodge' to false.");
         * }
         */

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

        int threads = config.get("threads", "entityThreads", 4)
            .getInt();
        registeredThreadManagers.put("entityManager", new ForkThreadManager("entityManager", threads));
        logger.info(">Entity manager initialized.");

        threads = config.get("threads", "blockThreads", 4)
            .getInt();
        registeredThreadManagers.put("blockManager", new ForkThreadManager("blockManager", threads));
        logger.info(">Block manager initialized.");


        MinecraftForge.EVENT_BUS.register(this);

        config.save();
        logger.info("Spool completed initialization!");
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
                    event.right.add(String.format("Number of threads: %d", manager.getNumThreads()));
                    if (!debug) event.right.add("Additional information unavailable (debugging inactive).");
                    else {
                        event.right
                            .add(String.format("Time saved in thread: %.2fms", manager.getTimeExecuting() / 1000000d));
                        event.right.add(
                            String.format("Overhead spent on thread: %.2fms", manager.getTimeOverhead() / 1000000d));
                        event.right.add(
                            String.format("Time spent waiting on thread: %.2fms", manager.getTimeWaiting() / 1000000d));
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
