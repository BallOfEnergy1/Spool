package com.gamma.spool.mixin.minecraft;

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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gamma.spool.Spool;
import com.gamma.spool.thread.IThreadManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import cpw.mods.fml.common.FMLCommonHandler;

@Mixin(value = MinecraftServer.class, priority = 1001) // Fix compatibility for hodgepodge racing to inject.
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

    @Inject(method = "updateTimeLightAndEntities", at = @At(value = "HEAD"), cancellable = true)
    public void updateTimeLightAndEntities(CallbackInfo ci) {
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
            for (int x = 0; x < ids.length; x++) {
                int id = ids[x];
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
        ci.cancel();
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
