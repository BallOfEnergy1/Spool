package com.gamma.spool.mixin.minecraft;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import net.minecraft.command.ICommandSender;
import net.minecraft.crash.CrashReport;
import net.minecraft.network.NetworkSystem;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.profiler.IPlayerUsage;
import net.minecraft.profiler.PlayerUsageSnooper;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.gui.IUpdatePlayerListBox;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
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

import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.config.ThreadManagerConfig;
import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.core.SpoolManagerOrchestrator;
import com.gamma.spool.mixin.MinecraftLambdaOptimizedTasks;
import com.gamma.spool.thread.IThreadManager;
import com.gamma.spool.thread.KeyedPoolThreadManager;
import com.gamma.spool.thread.LBKeyedPoolThreadManager;
import com.gamma.spool.thread.ManagerNames;
import com.gamma.spool.util.caching.RegisteredCache;
import com.gamma.spool.util.concurrent.AsyncProfiler;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.FMLCommonHandler;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrays;

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

    @Shadow
    private boolean startProfiling;
    @Shadow
    private long field_147142_T;
    @Shadow
    @Final
    private ServerStatusResponse field_147147_p;

    @Shadow
    public abstract int getMaxPlayers();

    @Shadow
    public abstract int getCurrentPlayerCount();

    @Shadow
    @Final
    private Random field_147146_q;

    @Shadow
    protected abstract void saveAllWorlds(@SuppressWarnings("SameParameterValue") boolean dontLog);

    @Shadow
    @Final
    public long[] tickTimeArray;
    @Shadow
    @Final
    private PlayerUsageSnooper usageSnooper;

    @Invoker("func_147137_ag")
    public abstract NetworkSystem invoke_func_147137_ag();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        theProfiler = new AsyncProfiler(); // New async profiler.
    }

    @Unique
    public void spool$dimensionTask(int id) {
        MinecraftLambdaOptimizedTasks.dimensionTask((MinecraftServer) (Object) this, id);
    }

    @Unique
    private void spool$finishTick() {
        this.theProfiler.startSection("spoolFinishTick");
        this.theProfiler.startSection("spoolWaiting");
        SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS.values()
            .forEach(IThreadManager::waitUntilAllTasksDone);
        this.theProfiler.endStartSection("spoolClearCaches");
        if (ThreadsConfig.isDistanceThreadingEnabled()) {
            RegisteredCache cache = SpoolManagerOrchestrator.REGISTERED_CACHES.get(ManagerNames.DISTANCE);
            cache.updateCachedSize();
            cache.getCache()
                .invalidate();
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
        try {

            // This is here purely because the `jobs` profiler section is never actually ended.
            this.theProfiler.endSection();

            if (ThreadManagerConfig.allowProcessingDuringSleep) spool$finishTick();

            this.theProfiler.startSection("connection");
            this.invoke_func_147137_ag()
                .networkTick();

            this.theProfiler.endSection();
            this.theProfiler.endSection();
            this.theProfiler.startSection("levels");
            net.minecraftforge.common.chunkio.ChunkIOExecutor.tick();
            int i;

            Integer[] ids = DimensionManager.getIDs(this.tickCounter % 200 == 0);

            if (ThreadsConfig.isDimensionThreadingEnabled()) {
                this.theProfiler.startSection("spoolDimensionUpdating");
                long time = 0;
                if (DebugConfig.debug) time = System.nanoTime();
                KeyedPoolThreadManager dimensionManager = (KeyedPoolThreadManager) SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS
                    .get(ManagerNames.DIMENSION);
                IntSet set = dimensionManager.getAllKeys();
                // Maybe not the best way... works though~~~
                // There aren't too many dimension IDs at any point in time, so it shouldn't introduce too much
                // overhead.

                for (int id : ids) {
                    if (!set.contains(id)) {
                        dimensionManager.addKeyedThread(id, "Dimension " + id + "-Thread");

                        if (dimensionManager instanceof LBKeyedPoolThreadManager) {
                            ((LBKeyedPoolThreadManager) dimensionManager).setLoadFunction(id, () -> {
                                WorldServer worldObj = DimensionManager.getWorld(id);
                                // TODO: Fine tune this.
                                return (worldObj.playerEntities.size() / 10d)
                                    + (ForgeChunkManager.getPersistentChunksFor(worldObj)
                                        .size() / 50d)
                                    + (worldObj.loadedTileEntityList.size() / 80d)
                                    + (worldObj.loadedEntityList.size() / 100d);
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

                // Normally wouldn't do this in a mixin, but I do want to see how the dimension threading overhead is.
                if (DebugConfig.debug) dimensionManager.timeOverhead = System.nanoTime() - time;
                this.theProfiler.endSection();

                for (final int id : ids) {
                    this.theProfiler.startSection("spoolSubmitDimensionTask");
                    if (ThreadManagerConfig.useLambdaOptimization) {
                        dimensionManager.execute(id, this::spool$dimensionTask, id);
                    } else {
                        Runnable task = () -> spool$dimensionTask(id);
                        dimensionManager.execute(id, task);
                    }
                    this.theProfiler.endSection();
                }
            } else {
                for (final int id : ids) {
                    this.theProfiler.startSection("spoolUpdateDimension");
                    if (ThreadManagerConfig.useLambdaOptimization) spool$dimensionTask(id);
                    else {
                        Runnable task = () -> spool$dimensionTask(id);
                        task.run();
                    }
                    this.theProfiler.endSection();
                }
            }

            this.theProfiler.endStartSection("dim_unloading");
            DimensionManager.unloadWorlds(worldTickTimes);
            this.theProfiler.endStartSection("players");
            this.serverConfigManager.sendPlayerInfoToAllPlayers();
            this.theProfiler.endStartSection("tickables");

            for (i = 0; i < this.tickables.size(); ++i) {
                this.tickables.get(i)
                    .update();
            }

            this.theProfiler.endSection();
        } catch (ConcurrentModificationException e) {
            spool$SpoolCrash(e);
        }
    }

    @Unique
    private final Random spool$statusRandom = new Random();

    /**
     * @author BallOfEnergy01
     * @reason Reimplemented to allow for concurrency and threading.
     */
    @Overwrite
    public void tick() {
        long i = System.nanoTime();
        if (this.startProfiling) {
            this.startProfiling = false;
            this.theProfiler.profilingEnabled = true;
            this.theProfiler.clearProfiling();
        }

        this.theProfiler.startSection("root");
        this.theProfiler.startSection("preTick");
        FMLCommonHandler.instance()
            .onPreServerTick();
        this.theProfiler.endSection();
        ++this.tickCounter;

        this.updateTimeLightAndEntities();

        if (i - this.field_147142_T >= 5000000000L) { // 5 seconds.
            this.field_147142_T = i;
            this.field_147147_p.func_151319_a(
                new ServerStatusResponse.PlayerCountData(this.getMaxPlayers(), this.getCurrentPlayerCount()));
            GameProfile[] agameprofile = new GameProfile[Math.min(this.getCurrentPlayerCount(), 12)];
            int j = MathHelper
                .getRandomIntegerInRange(this.field_147146_q, 0, this.getCurrentPlayerCount() - agameprofile.length);

            for (int k = 0; k < agameprofile.length; ++k) {
                agameprofile[k] = this.serverConfigManager.playerEntityList.get(j + k)
                    .getGameProfile();
            }

            ObjectArrays.shuffle(agameprofile, spool$statusRandom);
            this.field_147147_p.func_151318_b()
                .func_151330_a(agameprofile);
        }

        if (this.tickCounter % 900 == 0 && this.tickCounter != 0) {
            this.theProfiler.startSection("save");
            this.serverConfigManager.saveAllPlayerData();
            this.saveAllWorlds(true);
            this.theProfiler.endSection();
        }

        if (!ThreadManagerConfig.allowProcessingDuringSleep) {
            spool$finishTick(); // Finish this tick before tick times are tallied.
        }

        this.theProfiler.startSection("tallying");
        this.tickTimeArray[this.tickCounter % 100] = System.nanoTime() - i;
        this.theProfiler.endStartSection("snooper");

        if (!this.usageSnooper.isSnooperRunning() && this.tickCounter > 100) {
            this.usageSnooper.startSnooper();
        }

        if (this.tickCounter % 6000 == 0) {
            this.usageSnooper.addMemoryStatsToSnooper();
        }

        this.theProfiler.endStartSection("postTick");
        FMLCommonHandler.instance()
            .onPostServerTick();
        this.theProfiler.endSection();
        this.theProfiler.endSection();
    }

    @Unique
    public void spool$SpoolCrash(Throwable exception) {
        CrashReport report = CrashReport.makeCrashReport(exception, "Spool Concurrency Error");
        throw new ReportedException(report);
    }
}
