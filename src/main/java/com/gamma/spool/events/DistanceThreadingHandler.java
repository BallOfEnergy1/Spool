package com.gamma.spool.events;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.world.WorldEvent;

import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.core.SpoolLogger;
import com.gamma.spool.core.SpoolManagerOrchestrator;
import com.gamma.spool.thread.ManagerNames;
import com.gamma.spool.util.distance.DistanceThreadingChunkUtil;
import com.gamma.spool.util.distance.DistanceThreadingUtil;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

@EventBusSubscriber
public class DistanceThreadingHandler {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPreServerTick(TickEvent.ServerTickEvent event) {
        // Check for instability at the start of the tick.
        if (event.phase == TickEvent.Phase.START && ThreadsConfig.isDistanceThreadingEnabled())
            DistanceThreadingUtil.onTick();
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

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
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
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (ThreadsConfig.isDistanceThreadingEnabled() && DistanceThreadingUtil.isInitialized()) {
            SpoolLogger.info("Rebuilding player and chunk executor buckets; reason: Player join...");
            DistanceThreadingUtil.rebuildPlayerMap();
            DistanceThreadingUtil.rebuildChunkMap();
        }
    }

    // TODO: Probably find a better way than just rebuilding the entire map every time.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (ThreadsConfig.isDistanceThreadingEnabled() && DistanceThreadingUtil.isInitialized()) {
            SpoolLogger.info("Rebuilding player and chunk executor buckets; reason: Player changed dimensions...");
            DistanceThreadingUtil.rebuildPlayerMap();
            DistanceThreadingUtil.rebuildChunkMap();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChunkForced(ForgeChunkManager.ForceChunkEvent event) {
        DistanceThreadingChunkUtil.onChunkForced(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChunkUnforced(ForgeChunkManager.UnforceChunkEvent event) {
        DistanceThreadingChunkUtil.onChunkUnforced(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onWorldUnloaded(WorldEvent.Unload event) {
        DistanceThreadingChunkUtil.onWorldUnload(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onWorldLoaded(WorldEvent.Load event) {
        DistanceThreadingChunkUtil.onWorldLoad(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerEnterChunk(EntityEvent.EnteringChunk event) {
        if (event.entity instanceof EntityPlayer) DistanceThreadingChunkUtil.onPlayerEnterChunk(event);
    }
}
