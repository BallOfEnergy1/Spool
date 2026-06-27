package com.gamma.spool.mixin.minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.IntHashMap;
import net.minecraft.util.ReportedException;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.ISaveHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gamma.gammalib.util.concurrent.ConcurrentIntHashMap;
import com.gamma.spool.async.ImmediateUpdatesAsync;
import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.core.SpoolCompat;
import com.gamma.spool.core.SpoolLogger;
import com.gamma.spool.util.LockHelper;
import com.gamma.spool.util.MinecraftTasks;
import com.gamma.spool.util.PendingTickList;
import com.gamma.spool.util.RWLockedSet;
import com.gamma.spool.util.UnmodifiableTreeSet;
import com.gamma.spool.util.distance.DistanceThreadingExecutors;
import com.mitchej123.hodgepodge.config.FixesConfig;
import com.mitchej123.hodgepodge.hax.LongChunkCoordIntPairSet;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

@Mixin(value = WorldServer.class)
public abstract class WorldServerMixin extends World {

    @Shadow
    @Mutable
    private Set<NextTickListEntry> pendingTickListEntriesHashSet;

    @Shadow
    @Mutable
    private List<NextTickListEntry> pendingTickListEntriesThisTick;

    @Shadow
    private TreeSet<NextTickListEntry> pendingTickListEntriesTreeSet;

    @Shadow
    @Mutable
    private IntHashMap entityIdMap;

    @Shadow
    public abstract void scheduleBlockUpdate(int p_147464_1_, int p_147464_2_, int p_147464_3_, Block p_147464_4_,
        int p_147464_5_);

    @Unique
    private PendingTickList spool$pendingTickList;

    @Unique
    private ThreadLocal<Integer> spool$currentBlockUpdateRecursiveCallsPerThread = ThreadLocal.withInitial(() -> 0);

    public WorldServerMixin(ISaveHandler p_i45368_1_, String p_i45368_2_, WorldProvider p_i45368_3_,
        WorldSettings p_i45368_4_, Profiler p_i45368_5_) {
        super(p_i45368_1_, p_i45368_2_, p_i45368_3_, p_i45368_4_, p_i45368_5_);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {

        // Pulling this out of play.
        pendingTickListEntriesTreeSet = new UnmodifiableTreeSet<>();

        // Now introducing, this better thing!
        spool$pendingTickList = new PendingTickList();
        pendingTickListEntriesHashSet = spool$pendingTickList;

        pendingTickListEntriesThisTick = ObjectLists.synchronize(new ObjectArrayList<>());
        entityIdMap = new ConcurrentIntHashMap();
    }

    @Inject(method = "initialize", at = @At("RETURN"))
    private void initialize(CallbackInfo ci) {
        if (pendingTickListEntriesHashSet == null) {
            if (spool$pendingTickList == null) spool$pendingTickList = new PendingTickList();
            pendingTickListEntriesHashSet = spool$pendingTickList;
        }
        if (pendingTickListEntriesTreeSet == null) pendingTickListEntriesTreeSet = new UnmodifiableTreeSet<>();
        if (entityIdMap == null) entityIdMap = new ConcurrentIntHashMap();
    }

    @Redirect(
        method = "tick()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldServer;func_147456_g()V"))
    public void func_147456_g(WorldServer instance) {
        super.func_147456_g();

        LockHelper.readLockActiveChunkSet(this.provider.dimensionId);
        try {
            Iterator<ChunkCoordIntPair> iterator;
            if (SpoolCompat.isModLoadedFast(SpoolCompat.CompatibleMods.HODGEPODGE)
                && FixesConfig.fixTooManyAllocationsChunkPositionIntPair)
                iterator = ((LongChunkCoordIntPairSet) ((RWLockedSet<ChunkCoordIntPair>) activeChunkSet).getWrapped())
                    .unsafeIterator();
            else iterator = activeChunkSet.iterator();
            while (iterator.hasNext()) {
                ChunkCoordIntPair chunkcoordintpair = iterator.next();
                MinecraftTasks.executeChunkTask(this, chunkcoordintpair);
            }
        } finally {
            LockHelper.readUnlockActiveChunkSet(this.provider.dimensionId);
        }
    }

    /**
     * @author BallOfEnergy01
     * @reason Concurrency.
     */
    @Overwrite
    public boolean tickUpdates(boolean p_72955_1_) {

        int i = spool$pendingTickList.size();

        if (i > 1000) {
            i = 1000;
        }

        this.theProfiler.startSection("cleaning");
        NextTickListEntry entry;

        for (int j = 0; j < i; ++j) {
            entry = spool$pendingTickList.first();

            if (!p_72955_1_ && entry.scheduledTime > this.worldInfo.getWorldTotalTime()) {
                break;
            }

            spool$pendingTickList.remove(entry);
            this.pendingTickListEntriesThisTick.add(entry);
        }

        this.theProfiler.endSection();

        this.theProfiler.startSection("ticking");

        Iterator<NextTickListEntry> iterator = this.pendingTickListEntriesThisTick.iterator();

        // noinspection SynchronizeOnNonFinalField
        synchronized (pendingTickListEntriesThisTick) {
            while (iterator.hasNext()) {
                entry = iterator.next();
                iterator.remove();
                byte b0 = 0;

                if (this.checkChunksExist(
                    entry.xCoord - b0,
                    entry.yCoord - b0,
                    entry.zCoord - b0,
                    entry.xCoord + b0,
                    entry.yCoord + b0,
                    entry.zCoord + b0)) {
                    Block block = this.getBlock(entry.xCoord, entry.yCoord, entry.zCoord);

                    if (block.getMaterial() != Material.air && Block.isEqualTo(block, entry.func_151351_a())) {
                        try {
                            final int x = entry.xCoord;
                            final int y = entry.yCoord;
                            final int z = entry.zCoord;
                            if (ThreadsConfig.isDistanceThreadingEnabled()) DistanceThreadingExecutors.execute(
                                this,
                                x,
                                z,
                                MinecraftTasks::blockTask,
                                false,
                                this,
                                new MinecraftTasks.BlockTaskUnit(block, x, y, z));
                            else block.updateTick(this, x, y, z, this.rand);
                        } catch (Throwable throwable1) {
                            CrashReport crashreport = CrashReport
                                .makeCrashReport(throwable1, "Exception while ticking a block");
                            CrashReportCategory crashreportcategory = crashreport.makeCategory("Block being ticked");
                            int k;

                            try {
                                k = this.getBlockMetadata(entry.xCoord, entry.yCoord, entry.zCoord);
                            } catch (Throwable throwable) {
                                k = -1;
                            }

                            CrashReportCategory
                                .func_147153_a(crashreportcategory, entry.xCoord, entry.yCoord, entry.zCoord, block, k);
                            throw new ReportedException(crashreport);
                        }
                    }
                } else {
                    this.scheduleBlockUpdate(entry.xCoord, entry.yCoord, entry.zCoord, entry.func_151351_a(), 0);
                }
            }
        }

        this.theProfiler.endSection();
        this.pendingTickListEntriesThisTick.clear();
        return !spool$pendingTickList.isEmpty();
    }

    /**
     * @author mitchej123
     * @reason Spatial index lookup
     */
    @Overwrite
    public List<NextTickListEntry> getPendingBlockUpdates(Chunk chunk, boolean remove) {
        final var tickIndex = spool$pendingTickList.hodgepodge$getTickIndex();
        ObjectArrayList<NextTickListEntry> result = null;
        final int chunkX = chunk.xPosition;
        final int chunkZ = chunk.zPosition;
        final int minX = (chunkX << 4) - 2;
        final int maxX = minX + 18;
        final int minZ = (chunkZ << 4) - 2;
        final int maxZ = minZ + 18;

        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                final long key = ChunkCoordIntPair.chunkXZ2Int(chunkX + dx, chunkZ + dz);
                final ObjectOpenHashSet<NextTickListEntry> bucket = tickIndex.get(key);
                if (bucket == null) continue;

                final Iterator<NextTickListEntry> it = bucket.iterator();
                while (it.hasNext()) {
                    final NextTickListEntry entry = it.next();

                    if (!spool$pendingTickList.contains(entry)) {
                        it.remove();
                        continue;
                    }

                    if (entry.xCoord >= minX && entry.xCoord < maxX && entry.zCoord >= minZ && entry.zCoord < maxZ) {
                        if (remove) {
                            spool$pendingTickList.removeRaw(entry);
                            it.remove();
                        }
                        if (result == null) result = new ObjectArrayList<>();
                        result.add(entry);
                    }
                }
                if (bucket.isEmpty()) tickIndex.remove(key);
            }
        }

        // Vanilla's second loop: scan pendingTickListEntriesThisTick
        for (NextTickListEntry entry : pendingTickListEntriesThisTick) {
            if (entry.xCoord >= minX && entry.xCoord < maxX && entry.zCoord >= minZ && entry.zCoord < maxZ) {
                if (result == null) result = new ObjectArrayList<>();
                result.add(entry);
            }
        }

        if (result != null) //noinspection unchecked
            Collections.sort(result);
        return result;
    }

    /**
     * @author BallOfEnergy01
     * @reason Concurrency.
     */
    @Overwrite
    public void scheduleBlockUpdateWithPriority(int x, int y, int z, Block block, int p_147454_5_, int p_147454_6_) {

        // Early initialization shenanigans, this will (at maximum) be run once per world.
        if (spool$pendingTickList == null) {
            spool$pendingTickList = new PendingTickList();
            pendingTickListEntriesHashSet = spool$pendingTickList;
        }

        boolean recursiveUpdateFix = SpoolCompat.isModLoadedFast(SpoolCompat.CompatibleMods.HODGEPODGE)
            && FixesConfig.limitRecursiveBlockUpdateDepth > 0;

        if (recursiveUpdateFix) {
            if (spool$currentBlockUpdateRecursiveCallsPerThread == null) {
                spool$currentBlockUpdateRecursiveCallsPerThread = ThreadLocal.withInitial(() -> 0);
            }
            // SHENANIGANS END
            // ------------------------------ RECURSIVE UPDATE FIX ------------------------------
            int numRecursiveCalls = spool$currentBlockUpdateRecursiveCallsPerThread.get();
            if (numRecursiveCalls >= FixesConfig.limitRecursiveBlockUpdateDepth) {
                final StackOverflowError error = new StackOverflowError(
                    String.format(
                        "Too many recursive block updates (%d) at world %d, block %s (%d, %d, %d) - aborting further block updates",
                        numRecursiveCalls,
                        this.provider.dimensionId,
                        block,
                        x,
                        y,
                        z));
                SpoolLogger.error(error.getMessage(), error);
                int newValue = numRecursiveCalls - 1;
                if (newValue <= 0) spool$currentBlockUpdateRecursiveCallsPerThread.remove();
                else spool$currentBlockUpdateRecursiveCallsPerThread.set(newValue);
                return;
            }
            numRecursiveCalls++;
            spool$currentBlockUpdateRecursiveCallsPerThread.set(numRecursiveCalls);
            // ------------------------------ RECURSIVE UPDATE FIX ------------------------------
        }

        NextTickListEntry entry = new NextTickListEntry(x, y, z, block);
        byte b0 = 0;

        if (ImmediateUpdatesAsync.isLocationImmediateUpdate() && block.getMaterial() != Material.air) {
            if (block.func_149698_L()) {
                b0 = 8;

                if (this.checkChunksExist(
                    entry.xCoord - b0,
                    entry.yCoord - b0,
                    entry.zCoord - b0,
                    entry.xCoord + b0,
                    entry.yCoord + b0,
                    entry.zCoord + b0)) {
                    Block block1 = this.getBlock(entry.xCoord, entry.yCoord, entry.zCoord);

                    if (block1.getMaterial() != Material.air && block1 == entry.func_151351_a()) {
                        block1.updateTick(this, entry.xCoord, entry.yCoord, entry.zCoord, this.rand);
                    }
                }

                if (recursiveUpdateFix) {
                    // ------------------------------ RECURSIVE UPDATE FIX ------------------------------
                    int newValue = spool$currentBlockUpdateRecursiveCallsPerThread.get() - 1;
                    if (newValue <= 0) spool$currentBlockUpdateRecursiveCallsPerThread.remove();
                    else spool$currentBlockUpdateRecursiveCallsPerThread.set(newValue);
                    return;
                    // ------------------------------ RECURSIVE UPDATE FIX ------------------------------
                }
            }

            p_147454_5_ = 1;
        }

        if (this.checkChunksExist(x - b0, y - b0, z - b0, x + b0, y + b0, z + b0)) {
            if (block.getMaterial() != Material.air) {
                entry.setScheduledTime((long) p_147454_5_ + this.worldInfo.getWorldTotalTime());
                entry.setPriority(p_147454_6_);
            }

            if (!this.spool$pendingTickList.contains(entry)) {
                this.spool$pendingTickList.add(entry);
            }
        }
        if (recursiveUpdateFix) {
            // ------------------------------ RECURSIVE UPDATE FIX ------------------------------
            int newValue = spool$currentBlockUpdateRecursiveCallsPerThread.get() - 1;
            if (newValue <= 0) spool$currentBlockUpdateRecursiveCallsPerThread.remove();
            else spool$currentBlockUpdateRecursiveCallsPerThread.set(newValue);
            // ------------------------------ RECURSIVE UPDATE FIX ------------------------------
        }
    }

    /**
     * @author BallOfEnergy01
     * @reason Concurrency.
     */
    @Overwrite
    public void func_147446_b(int x, int y, int z, Block p_147446_4_, int p_147446_5_, int p_147446_6_) {
        NextTickListEntry entry = new NextTickListEntry(x, y, z, p_147446_4_);
        entry.setPriority(p_147446_6_);

        if (p_147446_4_.getMaterial() != Material.air) {
            entry.setScheduledTime((long) p_147446_5_ + this.worldInfo.getWorldTotalTime());
        }

        if (!this.spool$pendingTickList.contains(entry)) {
            this.spool$pendingTickList.add(entry);
        }
    }
}
