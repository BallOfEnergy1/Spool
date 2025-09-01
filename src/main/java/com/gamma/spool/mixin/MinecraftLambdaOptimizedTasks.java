package com.gamma.spool.mixin;

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

import com.gamma.spool.Spool;
import com.gamma.spool.config.ThreadManagerConfig;
import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.thread.ManagerNames;
import com.github.bsideup.jabel.Desugar;

import cpw.mods.fml.common.FMLCommonHandler;

public class MinecraftLambdaOptimizedTasks {

    public static void entityTask(World that, Entity entity) {
        that.updateEntity(entity);
    }

    public static void chunkTask(World that, ChunkCoordIntPair pair) {
        int k = pair.chunkXPos << 4;
        int l = pair.chunkZPos << 4;
        Chunk chunk = that.getChunkFromChunkCoords(pair.chunkXPos, pair.chunkZPos);
        that.func_147467_a(k, l, chunk);
        chunk.func_150804_b(false);

        int i1;
        int j1;
        int k1;
        int l1;

        if (that.provider.canDoLightning(chunk) && that.rand.nextInt(100000) == 0
            && that.isRaining()
            && that.isThundering()) {
            that.updateLCG = that.updateLCG * 3 + 1013904223;
            i1 = that.updateLCG >> 2;
            j1 = k + (i1 & 15);
            k1 = l + (i1 >> 8 & 15);
            l1 = that.getPrecipitationHeight(j1, k1);

            if (that.canLightningStrikeAt(j1, l1, k1)) {
                that.addWeatherEffect(new EntityLightningBolt(that, j1, l1, k1));
            }
        }

        if (that.provider.canDoRainSnowIce(chunk) && that.rand.nextInt(16) == 0) {
            that.updateLCG = that.updateLCG * 3 + 1013904223;
            i1 = that.updateLCG >> 2;
            j1 = i1 & 15;
            k1 = i1 >> 8 & 15;
            l1 = that.getPrecipitationHeight(j1 + k, k1 + l);

            if (that.isBlockFreezableNaturally(j1 + k, l1 - 1, k1 + l)) {
                that.setBlock(j1 + k, l1 - 1, k1 + l, Blocks.ice);
            }

            if (that.isRaining() && that.func_147478_e(j1 + k, l1, k1 + l, true)) {
                that.setBlock(j1 + k, l1, k1 + l, Blocks.snow_layer);
            }

            if (that.isRaining()) {
                BiomeGenBase biomegenbase = that.getBiomeGenForCoords(j1 + k, k1 + l);

                if (biomegenbase.canSpawnLightningBolt()) {
                    that.getBlock(j1 + k, l1 - 1, k1 + l)
                        .fillWithRain(that, j1 + k, l1 - 1, k1 + l);
                }
            }
        }

        ExtendedBlockStorage[] aextendedblockstorage;
        aextendedblockstorage = chunk.getBlockStorageArray();

        j1 = aextendedblockstorage.length;

        for (k1 = 0; k1 < j1; ++k1) {
            ExtendedBlockStorage extendedblockstorage = aextendedblockstorage[k1];

            if (extendedblockstorage != null && extendedblockstorage.getNeedsRandomTick()) {
                for (int i3 = 0; i3 < 3; ++i3) {
                    that.updateLCG = that.updateLCG * 3 + 1013904223;
                    int i2 = that.updateLCG >> 2;
                    int j2 = i2 & 15;
                    int k2 = i2 >> 8 & 15;
                    int l2 = i2 >> 16 & 15;
                    Block block = extendedblockstorage.getBlockByExtId(j2, l2, k2);

                    if (block.getTickRandomly()) { // No idea why I chose to add the task outside this check...
                        final int[] coords = new int[] { j2 + k, l2 + extendedblockstorage.getYLocation(), k2 + l };
                        if (ThreadManagerConfig.useLambdaOptimization) {
                            if (ThreadsConfig.isExperimentalThreadingEnabled())
                                Spool.REGISTERED_THREAD_MANAGERS.get(ManagerNames.BLOCK)
                                    .execute(
                                        MinecraftLambdaOptimizedTasks::blockTask,
                                        that,
                                        new BlockTaskUnit(block, coords)); // sad...
                            else blockTask(that, block, coords);
                        } else {
                            Runnable blockTask = () -> block
                                .updateTick(that, coords[0], coords[1], coords[2], that.rand);
                            if (ThreadsConfig.isExperimentalThreadingEnabled())
                                Spool.REGISTERED_THREAD_MANAGERS.get(ManagerNames.BLOCK)
                                    .execute(blockTask);
                            // NOTE:
                            // Distance threading will not work on this level, as it's nested inside
                            // chunk-level threading.
                            else blockTask.run();
                        }
                    }
                }
            }
        }
    }

    @Desugar
    private record BlockTaskUnit(Block block, int[] location) {}

    public static void blockTask(World world, Block block, int[] location) {
        block.updateTick(world, location[0], location[1], location[2], world.rand);
    }

    private static void blockTask(World that, BlockTaskUnit unit) {
        unit.block.updateTick(that, unit.location[0], unit.location[1], unit.location[2], that.rand);
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
