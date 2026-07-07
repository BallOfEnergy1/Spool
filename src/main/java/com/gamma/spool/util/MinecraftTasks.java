package com.gamma.spool.util;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.block.Block;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ReportedException;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.DimensionManager;

import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.util.distance.DistanceThreadingExecutors;
import com.github.bsideup.jabel.Desugar;

import cpw.mods.fml.common.FMLCommonHandler;

public class MinecraftTasks {

    public static void entityTask(World that, Entity entity) {
        that.updateEntity(entity);
    }

    public static void executeChunkTask(World that, ChunkCoordIntPair chunkcoordintpair) {
        if (ThreadsConfig.isDistanceThreadingEnabled()) DistanceThreadingExecutors
            .execute(that, chunkcoordintpair, MinecraftTasks::chunkTask, that, chunkcoordintpair);
        else MinecraftTasks.chunkTask(that, chunkcoordintpair);
    }

    public static void chunkTask(World that, ChunkCoordIntPair pair) {
        int updateLCG = ThreadLocalRandom.current()
            .nextInt();
        int baseX = pair.chunkXPos << 4;
        int baseZ = pair.chunkZPos << 4;
        Chunk chunk = that.getChunkFromChunkCoords(pair.chunkXPos, pair.chunkZPos);
        that.func_147467_a(baseX, baseZ, chunk);
        chunk.func_150804_b(false);

        boolean isRaining = that.isRaining();

        if (isRaining && that.isThundering() && that.provider.canDoLightning(chunk) && that.rand.nextInt(100000) == 0) {
            updateLCG = updateLCG * 3 + 1013904223;
            int temp1 = updateLCG >> 2;
            int xCoord = baseX + (temp1 & 15);
            int zCoord = baseZ + (temp1 >> 8 & 15);
            int yCoord = that.getPrecipitationHeight(xCoord, zCoord);

            if (that.canLightningStrikeAt(xCoord, yCoord, zCoord)) {
                that.addWeatherEffect(new EntityLightningBolt(that, xCoord, yCoord, zCoord));
            }
        }

        if (that.provider.canDoRainSnowIce(chunk) && that.rand.nextInt(16) == 0) {
            updateLCG = updateLCG * 3 + 1013904223;
            int temp1 = updateLCG >> 2;
            int xCoord = baseX + (temp1 & 15);
            int zCoord = baseZ + (temp1 >> 8 & 15);
            int yCoord = that.getPrecipitationHeight(xCoord, zCoord);
            int yCoordMinusOne = yCoord - 1;

            if (that.isBlockFreezableNaturally(xCoord, yCoordMinusOne, zCoord)) {
                that.setBlock(xCoord, yCoordMinusOne, zCoord, Blocks.ice);
            }

            if (isRaining && that.func_147478_e(xCoord, yCoord, zCoord, true)) {
                that.setBlock(xCoord, yCoord, zCoord, Blocks.snow_layer);
            }

            if (isRaining) {
                BiomeGenBase biomegenbase = that.getBiomeGenForCoords(xCoord, zCoord);

                if (biomegenbase.canSpawnLightningBolt()) {
                    that.getBlock(xCoord, yCoordMinusOne, zCoord)
                        .fillWithRain(that, xCoord, yCoordMinusOne, zCoord);
                }
            }
        }

        for (ExtendedBlockStorage extendedblockstorage : chunk.getBlockStorageArray()) {
            if (extendedblockstorage != null && extendedblockstorage.getNeedsRandomTick()) {
                for (int i3 = 0; i3 < 3; ++i3) {
                    updateLCG = updateLCG * 3 + 1013904223;
                    int temp1 = updateLCG >> 2;
                    int xCoord = temp1 & 15;
                    int zCoord = temp1 >> 8 & 15;
                    int yCoord = temp1 >> 16 & 15;
                    Block block = extendedblockstorage.getBlockByExtId(xCoord, yCoord, zCoord);

                    if (block.getTickRandomly()) {
                        block.updateTick(
                            that,
                            xCoord + baseX,
                            yCoord + extendedblockstorage.getYLocation(),
                            zCoord + baseZ,
                            that.rand);
                    }
                }
            }
        }
    }

    @Desugar
    public record BlockTaskUnit(Block block, int x, int y, int z) {}

    public static void blockTask(World that, BlockTaskUnit unit) {
        unit.block.updateTick(that, unit.x, unit.y, unit.z, that.rand);
    }

    public static void dimensionTask(MinecraftServer that, int id) {
        long j = System.nanoTime();

        if (id == 0 || that.getAllowNether()) {
            WorldServer worldserver = DimensionManager.getWorld(id);
            that.theProfiler.startSection(
                worldserver.getWorldInfo()
                    .getWorldName());
            that.theProfiler.startSection("pools");
            that.theProfiler.endSection();
            that.theProfiler.startSection("tracker");
            worldserver.getEntityTracker()
                .updateTrackedEntities();
            that.theProfiler.endSection();

            if (that.getTickCounter() % 20 == 0) {
                that.theProfiler.startSection("timeSync");
                that.getConfigurationManager()
                    .sendPacketToAllPlayersInDimension(
                        new S03PacketTimeUpdate(
                            worldserver.getTotalWorldTime(),
                            worldserver.getWorldTime(),
                            worldserver.getGameRules()
                                .getGameRuleBooleanValue("doDaylightCycle")),
                        worldserver.provider.dimensionId);
                that.theProfiler.endSection();
            }

            that.theProfiler.startSection("tick");
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
            that.theProfiler.endSection();
            that.theProfiler.endSection();
        }

        that.worldTickTimes.get(id)[that.getTickCounter() % 100] = System.nanoTime() - j;
    }
}
