package com.gamma.spool.events;

import static com.gamma.spool.core.SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import com.gamma.spool.config.ThreadManagerConfig;
import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.core.SpoolLogger;
import com.gamma.spool.thread.IThreadManager;
import com.gamma.spool.thread.LBKeyedPoolThreadManager;
import com.gamma.spool.thread.ManagerNames;
import com.gamma.spool.thread.TimedOperationThreadManager;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

@EventBusSubscriber
public class ThreadManagerHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            checkLBPools();
            checkManagers();
        }
    }

    private static void checkLBPools() {
        // Every x ticks, update the pool (balance and such).
        if (ThreadManagerConfig.useLoadBalancingDimensionThreadManager && MinecraftServer.getServer()
            .getTickCounter() % ThreadManagerConfig.loadBalancerFrequency == 0) {
            for (IThreadManager manager : REGISTERED_THREAD_MANAGERS.values()) {
                if (!(manager instanceof LBKeyedPoolThreadManager)) continue;
                ((LBKeyedPoolThreadManager) manager).updatePool();
            }
        }
    }

    // This runs once every [while]
    // (1 pixel every sometimes lmaooo)
    // this is not enough of a hotspot to cause problems... hopefully.
    private static final Method isAIEnabledMethod = ReflectionHelper
        .findMethod(EntityLivingBase.class, null, new String[] { "isAIEnabled", "func_70650_aV" });

    private static void checkManagers() {
        // Every x ticks, update the pool (balance and such).
        if (ThreadManagerConfig.automaticallyDisableNonPerformantManagers && MinecraftServer.getServer()
            .getTickCounter() % ThreadManagerConfig.nonPerformantManagerCheckFrequency == 0) {

            for (Int2ObjectMap.Entry<IThreadManager> entry : REGISTERED_THREAD_MANAGERS.int2ObjectEntrySet()) {
                IThreadManager manager = entry.getValue();
                int managerOrdinal = entry.getIntKey();

                // Don't stop the manager controlling when managers get restarted lmao
                if (managerOrdinal == ManagerNames.THREAD_MANAGER_TIMER.ordinal()) continue;

                // Dedicated checks
                if (managerOrdinal == ManagerNames.ENTITY_AI.ordinal() && ThreadsConfig.isDistanceThreadingEnabled()) {
                    long num = 0;
                    for (WorldServer world : MinecraftServer.getServer().worldServers) {
                        for (Entity e : world.loadedEntityList) {
                            try {
                                if (e instanceof EntityLivingBase entityLivingBase
                                    && ((boolean) isAIEnabledMethod.invoke(entityLivingBase))) {
                                    num++;
                                }
                            } catch (IllegalAccessException | InvocationTargetException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }

                    // TODO: Tune this value until it's accurate (enough).
                    // Might as well set it a bit *high* for now, just to be safe.
                    if (num < 800 && !manager.isPoolDisabled()) {
                        manager.disablePool();
                        SpoolLogger.debug(
                            "Disabled manager " + ManagerNames.values()[managerOrdinal] + " due to performance.");
                    } else if (num >= 800 && manager.isPoolDisabled()) {
                        manager.enablePool();
                        SpoolLogger.debug("Enabled manager " + ManagerNames.values()[managerOrdinal] + ".");
                    }
                    continue;
                }

                // Automated checks
                long timeSaved = manager.getAvgTimeExecuting() - manager.getAvgTimeOverhead()
                    - manager.getAvgTimeWaiting();
                // If it's costing >1 ms on average, it shouldn't be running.
                if ((double) timeSaved / 1000000 < -1d) {
                    if (manager.isPoolDisabled()) {
                        manager.disablePool();
                        SpoolLogger.debug(
                            "Disabled manager " + ManagerNames.values()[managerOrdinal]
                                + " due to performance. It will be re-enabled after 10 seconds.");
                        // After 10 seconds, enable and allow to check again.
                        TimedOperationThreadManager totm = (TimedOperationThreadManager) REGISTERED_THREAD_MANAGERS
                            .get(ManagerNames.THREAD_MANAGER_TIMER.ordinal());
                        totm.execute(manager::enablePool, 10000, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
    }
}
