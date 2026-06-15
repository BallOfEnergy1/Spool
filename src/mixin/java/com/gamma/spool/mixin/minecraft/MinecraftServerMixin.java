package com.gamma.spool.mixin.minecraft;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.network.NetworkSystem;
import net.minecraft.profiler.IPlayerUsage;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.gui.IUpdatePlayerListBox;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gamma.spool.config.ThreadManagerConfig;
import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.core.SpoolManagerOrchestrator;
import com.gamma.spool.thread.IThreadManager;
import com.gamma.spool.thread.KeyedPoolThreadManager;
import com.gamma.spool.thread.LBKeyedPoolThreadManager;
import com.gamma.spool.thread.ManagerNames;
import com.gamma.spool.util.MinecraftTasks;
import com.gamma.spool.util.caching.RegisteredCache;
import com.gamma.spool.util.concurrent.AsyncProfiler;
import com.gamma.spool.util.distance.DistanceThreadingUtil;

import it.unimi.dsi.fastutil.ints.IntSet;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements ICommandSender, Runnable, IPlayerUsage {

    @Shadow
    private int tickCounter;
    @Shadow
    @Final
    @Mutable
    public Profiler theProfiler;
    @Shadow
    private ServerConfigurationManager serverConfigManager;
    @Shadow(remap = false)
    public Hashtable<Integer, long[]> worldTickTimes;
    @Final
    @Shadow
    private List<IUpdatePlayerListBox> tickables;

    @Invoker("func_147137_ag")
    public abstract NetworkSystem invoke_func_147137_ag();

    @Unique
    private MinecraftServer spool$instance;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        theProfiler = new AsyncProfiler(); // New async profiler.
    }

    @Unique
    private void spool$finishTick() {
        this.theProfiler.startSection("spoolFinishTick");
        this.theProfiler.startSection("spoolWaiting");
        SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS.values()
            .forEach(IThreadManager::waitUntilAllTasksDone);
        this.theProfiler.endStartSection("spoolClearCaches");
        if (ThreadsConfig.isDistanceThreadingEnabled() && DistanceThreadingUtil.isInitialized()) {
            RegisteredCache cache = SpoolManagerOrchestrator.REGISTERED_CACHES.get(ManagerNames.DISTANCE.ordinal());
            cache.updateCachedSize();
            cache.getCache()
                .invalidate(true);
        }
        this.theProfiler.endSection();
        this.theProfiler.endSection();
    }

    /**
     * @author BallOfEnergy01
     * @reason Reimplemented to allow for concurrency and threading.
     */
    @Overwrite
    public void updateTimeLightAndEntities() {
        // This is here purely because the `jobs` profiler section is never actually ended.
        // I don't remember this. I'm afraid to remove it because it might break things.
        this.theProfiler.endSection();

        if (ThreadManagerConfig.allowProcessingDuringSleep) spool$finishTick();

        this.theProfiler.startSection("levels");
        net.minecraftforge.common.chunkio.ChunkIOExecutor.tick();
        int i;

        Integer[] ids = DimensionManager.getIDs(this.tickCounter % 200 == 0);

        if (spool$instance == null) {
            // noinspection DataFlowIssue fuck you
            spool$instance = (MinecraftServer) (Object) this;
        }

        if (ThreadsConfig.isDimensionThreadingEnabled()) {
            KeyedPoolThreadManager dimensionManager = (KeyedPoolThreadManager) SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS
                .get(ManagerNames.DIMENSION.ordinal());
            IntSet set = dimensionManager.getAllKeys();
            // Maybe not the best way... works though~~~
            // There aren't too many dimension IDs at any point in time, so it shouldn't introduce too much
            // overhead.

            for (int id : ids) {
                if (!set.contains(id)) {
                    dimensionManager.addKeyedThread(id, "Dimension " + id + "-Thread");

                    if (dimensionManager instanceof LBKeyedPoolThreadManager) {
                        // Precalculate to avoid rare lockups on crash.
                        WorldServer worldObj = DimensionManager.getWorld(id);
                        int playerCount = worldObj.playerEntities.size();
                        int chunkCount = ForgeChunkManager.getPersistentChunksFor(worldObj)
                            .size();
                        int loadedTECount = worldObj.loadedTileEntityList.size();
                        int loadedEntityCount = worldObj.loadedEntityList.size();

                        ((LBKeyedPoolThreadManager) dimensionManager).setLoadFunction(id, () -> {
                            // TODO: Fine tune this.
                            return (playerCount / 10d) + (chunkCount / 50d)
                                + (loadedTECount / 80d)
                                + (loadedEntityCount / 100d);
                        });
                    }
                }
            }

            List<Integer> idList = Arrays.asList(ids);

            for (int id : set) {
                if (!idList.contains(id)) {
                    dimensionManager.removeKeyedThread(id);
                }
            }

            for (final int id : ids) {
                dimensionManager.execute(id, MinecraftTasks::dimensionTask, spool$instance, id);
            }
        } else {
            for (final int id : ids) {
                MinecraftTasks.dimensionTask(spool$instance, id);
            }
        }

        this.theProfiler.endStartSection("dim_unloading");
        DimensionManager.unloadWorlds(worldTickTimes);
        this.theProfiler.endStartSection("connection");
        this.invoke_func_147137_ag()
            .networkTick();
        this.theProfiler.endStartSection("players");
        this.serverConfigManager.sendPlayerInfoToAllPlayers();
        this.theProfiler.endStartSection("tickables");

        for (i = 0; i < this.tickables.size(); ++i) {
            this.tickables.get(i)
                .update();
        }

        this.theProfiler.endSection();
    }

    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/profiler/Profiler;startSection(Ljava/lang/String;)V",
            shift = At.Shift.AFTER,
            ordinal = 2))
    private void injectedTick(CallbackInfo ci) {
        if (!ThreadManagerConfig.allowProcessingDuringSleep) {
            spool$finishTick(); // Finish this tick before tick times are tallied.
        }
    }
}
