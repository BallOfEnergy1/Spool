package com.gamma.spool.mixin.minecraft;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Hashtable;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.crash.CrashReport;
import net.minecraft.network.NetworkSystem;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.profiler.IPlayerUsage;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.gui.IUpdatePlayerListBox;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.ReportedException;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;

import com.gamma.spool.Spool;
import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.thread.IThreadManager;
import com.gamma.spool.thread.KeyedPoolThreadManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import cpw.mods.fml.common.FMLCommonHandler;
import it.unimi.dsi.fastutil.ints.IntSet;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements ICommandSender, Runnable, IPlayerUsage {

    @Shadow
    private int tickCounter;
    @Final
    @Shadow
    public Profiler theProfiler;
    @Shadow
    private ServerConfigurationManager serverConfigManager;
    @Shadow(remap = false)
    public Hashtable<Integer, long[]> worldTickTimes;
    @Final
    @Shadow
    private List tickables;

    @Invoker("getAllowNether")
    public abstract boolean invokeGetAllowNether();

    @Invoker("func_147137_ag")
    public abstract NetworkSystem invoke_func_147137_ag();

    /**
     * @author BallOfEnergy01
     * @reason Reimplemented to allow for concurrency and threading.
     */
    @Overwrite
    public void updateTimeLightAndEntities() {
        try {
            Spool.registeredThreadManagers.values()
                .forEach(IThreadManager::waitUntilAllTasksDone);

            this.theProfiler.startSection("connection");
            this.invoke_func_147137_ag()
                .networkTick();
            this.theProfiler.endSection();

            this.theProfiler.startSection("levels");
            net.minecraftforge.common.chunkio.ChunkIOExecutor.tick();
            int i;

            Integer[] ids = DimensionManager.getIDs(this.tickCounter % 200 == 0);

            long time = 0;
            if (DebugConfig.debug) time = System.nanoTime();
            KeyedPoolThreadManager dimensionManager = (KeyedPoolThreadManager) Spool.registeredThreadManagers
                .get("dimensionManager");
            IntSet set = dimensionManager.getKeys();
            // Maybe not the best way... works though~~~
            // There aren't too many dimension IDs at any point in time, so it shouldn't introduce too much overhead.
            for (int id : ids) {
                if (!set.contains(id)) dimensionManager.addKeyedThread(id, "Dimension " + id + "-Thread");
            }
            for (int id : set) {
                if (set.contains(id) && !Arrays.asList(ids)
                    .contains(id)) dimensionManager.removeKeyedThread(id);
            }

            // Normally wouldn't do this in a mixin, but I do want to see how the dimension threading overhead is.
            if (DebugConfig.debug) dimensionManager.timeOverhead = System.nanoTime() - time;

            for (int id : ids) {

                Runnable task = () -> {
                    long j = System.nanoTime();

                    if (id == 0 || this.invokeGetAllowNether()) {
                        WorldServer worldserver = DimensionManager.getWorld(id);
                        this.theProfiler.startSection(
                            worldserver.getWorldInfo()
                                .getWorldName());
                        this.theProfiler.startSection("pools");
                        this.theProfiler.endSection();
                        this.theProfiler.startSection("tracker");
                        worldserver.getEntityTracker()
                            .updateTrackedEntities();
                        this.theProfiler.endSection();

                        if (this.tickCounter % 20 == 0) {
                            this.theProfiler.startSection("timeSync");
                            this.serverConfigManager.sendPacketToAllPlayersInDimension(
                                new S03PacketTimeUpdate(
                                    worldserver.getTotalWorldTime(),
                                    worldserver.getWorldTime(),
                                    worldserver.getGameRules()
                                        .getGameRuleBooleanValue("doDaylightCycle")),
                                worldserver.provider.dimensionId);
                            this.theProfiler.endSection();
                        }

                        this.theProfiler.startSection("tick");
                        FMLCommonHandler.instance()
                            .onPreWorldTick(worldserver);
                        CrashReport crashreport;

                        try {
                            worldserver.tick();
                        } catch (Throwable throwable1) {
                            crashreport = CrashReport.makeCrashReport(throwable1, "Exception ticking world");
                            worldserver.addWorldInfoToCrashReport(crashreport);
                            throw new ReportedException(crashreport);
                        }

                        try {
                            worldserver.updateEntities();
                        } catch (Throwable throwable) {
                            crashreport = CrashReport.makeCrashReport(throwable, "Exception ticking world entities");
                            worldserver.addWorldInfoToCrashReport(crashreport);
                            throw new ReportedException(crashreport);
                        }

                        FMLCommonHandler.instance()
                            .onPostWorldTick(worldserver);
                        this.theProfiler.endSection();
                        this.theProfiler.endSection();
                    }

                    worldTickTimes.get(id)[this.tickCounter % 100] = System.nanoTime() - j;
                };

                if (!ThreadsConfig.enableExperimentalThreading) dimensionManager.execute(id, task);
                else task.run();
            }

            this.theProfiler.endStartSection("dim_unloading");
            DimensionManager.unloadWorlds(worldTickTimes);
            this.theProfiler.endStartSection("players");
            this.serverConfigManager.sendPlayerInfoToAllPlayers();
            this.theProfiler.endStartSection("tickables");

            for (i = 0; i < this.tickables.size(); ++i) {
                ((IUpdatePlayerListBox) this.tickables.get(i)).update();
            }

            this.theProfiler.endSection();
            this.theProfiler.endSection();
        } catch (ConcurrentModificationException e) {
            spool$SpoolCrash(e);
        }
    }

    @WrapOperation(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;saveAllWorlds(Z)V"))
    private void injected(MinecraftServer instance, boolean dontLog, Operation<Void> original) {
        if (this.tickCounter != 0) { // Don't save if it's right after the world load.
            original.call(instance, dontLog);
        }
    }

    @Unique
    public void spool$SpoolCrash(Throwable exception) {
        CrashReport report = CrashReport.makeCrashReport(exception, "Spool Concurrency Error");
        throw new ReportedException(report);
    }
}
