package com.gamma.spool;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gamma.spool.thread.IResizableThreadManager;
import com.gamma.spool.thread.IThreadManager;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class SpoolConfigManager {

    private boolean isInitialized = false;

    public static final Logger logger = LogManager.getLogger("Spool-Config");

    public Configuration config;

    public boolean debug;
    /**
     * <p>
     * Config boolean for enabling the experimental full-threading that Spool offers. If this is false,
     * Spool will only put different dimensions into separate threads instead of threading inside dimensions.
     * </p>
     * <p>
     * This *does* break the constant update order nature of Minecraft, that's why it's a config option.
     * </p>
     */
    public boolean enableExperimentalThreading;

    /**
     * <p>
     * Config option for changing the timeout time for a single thread in a pool.
     * </p>
     * <p>
     * Lower numbers for this value can lead to improved speeds, however, lag can cause updates to be dropped entirely.
     * <p>
     * Higher numbers for this value allow for more stable running, though at the risk of slower speeds if something
     * lags.
     * </p>
     *
     * @implNote If there are multiple threads in a pool/executor, then the *total pool timeout* should
     *           be <code>timeout / threads</code>, where <code>timeout</code> is this number and <code>threads</code>
     *           is
     *           the number of running threads in the pool.
     */
    public int globalRunningSingleThreadTimeout;

    /**
     * <p>
     * Config option for changing the timeout time for a single thread in a pool *during termination*.
     * </p>
     * <p>
     * Lower numbers for this value can lead to improved speeds, however, lag can cause updates to be dropped entirely.
     * <p>
     * Higher numbers for this value allow for more stable running, though at the risk of slower speeds if something
     * lags.
     * </p>
     *
     * @implNote If there are multiple threads in a pool/executor, then the *total pool termination timeout* should
     *           be <code>timeout / threads</code>, where <code>timeout</code> is this number and <code>threads</code>
     *           is
     *           the number of previously running threads in the pool.
     */
    public int globalTerminatingSingleThreadTimeout;

    public int entityThreads;
    public int blockThreads;

    public void onPreInit(FMLPreInitializationEvent event) {
        config = new Configuration(event.getSuggestedConfigurationFile());
        loadConfig(false);
        isInitialized = true;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onConfigChange(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (!event.modID.equals(Spool.MODID)) return;
        loadConfig(event.isWorldRunning);
    }

    public void onInit(FMLInitializationEvent event) {
        if (enableExperimentalThreading) {
            logger.warn("Spool experimental threading enabled, issues may arise!");
            Spool.startPools(event);
            logger.info("Spool completed config initialization!");
        } else {
            logger.info("Spool initialization delayed until post-init due to threading config.");
        }

        config.save();
    }

    public void loadConfig(boolean isWorldRunning) {

        config.load();

        Property temp = config.get(Configuration.CATEGORY_GENERAL, "enableExperimentalThreading", false);
        temp.comment = "Enables Spool experimental threading.";
        temp.setLanguageKey("spool.config.enableExperimentalThreading");
        enableExperimentalThreading = temp.getBoolean();

        temp = config.get(Configuration.CATEGORY_GENERAL, "globalRunningSingleThreadTimeout", 1000);
        temp.comment = "Expiring time for thread pools while running.";
        temp.setLanguageKey("spool.config.globalRunningSingleThreadTimeout");
        globalRunningSingleThreadTimeout = Math.max(temp.getInt(), 0);

        temp = config.get(Configuration.CATEGORY_GENERAL, "globalTerminatingSingleThreadTimeout", 50000);
        temp.comment = "Expiring time for thread pools while terminating.";
        temp.setLanguageKey("spool.config.globalTerminatingSingleThreadTimeout");
        globalTerminatingSingleThreadTimeout = Math.max(temp.getInt(), 0);

        temp = config.get("debug", "debugMode", true);
        temp.comment = "Enables debug logging mode.";
        temp.setLanguageKey("spool.config.debugMode");
        debug = temp.getBoolean();

        temp = config.get("experimentalThreads", "entityThreads", 4);
        temp.comment = "Number of threads to use for entity processing.";
        temp.setLanguageKey("spool.config.entityThreads");
        entityThreads = Math.max(temp.getInt(), 0);

        if (isInitialized && isWorldRunning) {
            IThreadManager manager = Spool.registeredThreadManagers.get("entityManager");
            if (manager instanceof IResizableThreadManager) ((IResizableThreadManager) manager).resize(entityThreads);
            else logger.warn("Config change requires a world reload due to manager type.");
        }

        temp = config.get("experimentalThreads", "blockThreads", 4);
        temp.comment = "Number of threads to use for block processing.";
        temp.setLanguageKey("spool.config.blockThreads");
        blockThreads = Math.max(temp.getInt(), 0);

        if (isInitialized && isWorldRunning) {
            IThreadManager manager = Spool.registeredThreadManagers.get("blockManager");
            if (manager instanceof IResizableThreadManager) ((IResizableThreadManager) manager).resize(entityThreads);
            else logger.warn("Config change requires a world reload due to manager type.");
        }
    }
}
