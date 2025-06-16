package com.gamma.lmtm;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gamma.lmtm.thread.ForkThreadManager;
import com.gamma.lmtm.thread.IThreadManager;

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

@Mod(modid = LMTM.MODID, version = LMTM.VERSION)
public class LMTM {

    public static final String MODID = "lmtm";
    public static final String VERSION = "@VERSION@";

    public static Logger logger = LogManager.getLogger("LMTM");

    public static IThreadManager entityManager;
    public static IThreadManager blockManager;

    public static Configuration config;

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
         * " Due to both Hodgepodge and LMTM using mixins to fiddle inside of Minecraft classes, this crash is here to prevent crashes due to incompatibilities between the two."
         * + " This crash can be disabled in the config by setting 'crashOnHodgepodge' to false.");
         * }
         */

        FMLCommonHandler.instance()
            .registerCrashCallable(new ICrashCallable() {

                public String call() {
                    return "!! Crashes may be caused by LMTM's incompatibility with other mods !!"
                        + "\n\t\tBlock Pool: "
                        + "\n\t\t\tPool manager: "
                        + LMTM.blockManager.getClass()
                            .getSimpleName()
                        + "\n\t\t\tPool active: "
                        + LMTM.blockManager.isStarted()
                        + "\n\t\t\tThread count: "
                        + LMTM.blockManager.getNumThreads()
                        + "\n\t\tEntity Pool: "
                        + "\n\t\t\tPool manager: "
                        + LMTM.entityManager.getClass()
                            .getSimpleName()
                        + "\n\t\t\tPool active: "
                        + LMTM.entityManager.isStarted()
                        + "\n\t\t\tThread count: "
                        + LMTM.entityManager.getNumThreads();
                }

                public String getLabel() {
                    return "LMTM Info";
                }
            });
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("LMTM beginning initialization...");

        int threads = config.get("threads", "entityThreads", 4)
            .getInt();
        entityManager = new ForkThreadManager("entityManager", threads);
        logger.info(">Entity manager initialized.");

        threads = config.get("threads", "blockThreads", 4)
            .getInt();
        blockManager = new ForkThreadManager("blockManager", threads);
        logger.info(">Block manager initialized.");

        MinecraftForge.EVENT_BUS.register(this);

        config.save();
        logger.info("LMTM completed initialization!");
    }

    @EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        LMTM.blockManager.waitUntilAllTasksDone(false);
        LMTM.entityManager.waitUntilAllTasksDone(false);
        logger.info("Stopping LMTM processing threads to conserve system resources.");
        entityManager.terminatePool();
        blockManager.terminatePool();
        logger.info("LMTM threads terminated.");
    }

    @EventHandler
    public void serverStarted(FMLServerStartingEvent event) {
        logger.info("Starting LMTM threads...");
        entityManager.startPoolIfNeeded();
        blockManager.startPoolIfNeeded();
        logger.info("LMTM threads started successfully.");
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onDebugScreen(RenderGameOverlayEvent.Text event) {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.showDebugInfo) {
            event.right.add("");
            event.right.add("LMTM Stats");
            event.right.add("Block thread active: " + LMTM.blockManager.isStarted());
            if (LMTM.blockManager.isStarted()) {
                event.right.add(String.format("  Number of threads: %d", LMTM.blockManager.getNumThreads()));
                event.right.add(
                    String.format("  Time saved in thread: %.2fms", LMTM.blockManager.getTimeExecuting() / 1000000d));
                event.right.add(
                    String
                        .format("  Overhead spent on thread: %.2fms", LMTM.blockManager.getTimeOverhead() / 1000000d));
                event.right.add(
                    String.format(
                        "  Time spent waiting on thread: %.2fms",
                        LMTM.blockManager.getTimeWaiting() / 1000000d));
                // event.right.add(String.format(" Futures queue size: %d", LMTM.blockManager.futuresSize));
                // event.right.add(String.format(" Overflow queue size: %d", LMTM.blockManager.overflowSize));
            }
            event.right.add("Entity thread active: " + LMTM.entityManager.isStarted());
            if (LMTM.entityManager.isStarted()) {
                event.right.add(String.format("  Number of threads: %d", LMTM.entityManager.getNumThreads()));
                event.right.add(
                    String.format("  Time saved in thread: %.2fms", LMTM.entityManager.getTimeExecuting() / 1000000d));
                event.right.add(
                    String
                        .format("  Overhead spent on thread: %.2fms", LMTM.entityManager.getTimeOverhead() / 1000000d));
                event.right.add(
                    String.format(
                        "  Time spent waiting on thread: %.2fms",
                        LMTM.entityManager.getTimeWaiting() / 1000000d));
                // event.right.add(String.format(" Futures queue size: %d", LMTM.entityManager.futuresSize));
                // event.right.add(String.format(" Overflow queue size: %d", LMTM.entityManager.overflowSize));
            }
        }
    }
}
