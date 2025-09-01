package com.gamma.spool.mixin.minecraft;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IntHashMap;
import net.minecraft.util.ReportedException;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.Explosion;
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

import com.gamma.spool.Spool;
import com.gamma.spool.config.ThreadManagerConfig;
import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.mixin.MinecraftLambdaOptimizedTasks;
import com.gamma.spool.thread.ManagerNames;
import com.gamma.spool.util.PendingTickList;
import com.gamma.spool.util.UnmodifiableTreeSet;
import com.gamma.spool.util.concurrent.ConcurrentIntHashMap;
import com.gamma.spool.util.distance.DistanceThreadingExecutors;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

@Mixin(WorldServer.class)
public abstract class WorldServerMixin extends World {

    @Unique
    private PendingTickList<NextTickListEntry> spool$pendingTickList;

    @Shadow
    @Mutable
    private Set<NextTickListEntry> pendingTickListEntriesHashSet;

    @Shadow
    @Mutable
    private TreeSet<NextTickListEntry> pendingTickListEntriesTreeSet;

    @Shadow
    @Mutable
    private List<NextTickListEntry> pendingTickListEntriesThisTick;

    @Shadow
    @Mutable
    private IntHashMap entityIdMap;

    public WorldServerMixin(ISaveHandler p_i45368_1_, String p_i45368_2_, WorldProvider p_i45368_3_,
        WorldSettings p_i45368_4_, Profiler p_i45368_5_) {
        super(p_i45368_1_, p_i45368_2_, p_i45368_3_, p_i45368_4_, p_i45368_5_);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {

        // Pulling this out of play.
        pendingTickListEntriesTreeSet = new UnmodifiableTreeSet<>();

        // Now introducing, this better thing!
        spool$pendingTickList = new PendingTickList<>();
        pendingTickListEntriesHashSet = spool$pendingTickList;

        pendingTickListEntriesThisTick = ObjectLists.synchronize(new ObjectArrayList<>());
        entityIdMap = new ConcurrentIntHashMap();
    }

    @Inject(method = "initialize", at = @At("HEAD"))
    private void initialize(CallbackInfo ci) {
        if (pendingTickListEntriesHashSet == null) {
            if (spool$pendingTickList == null) spool$pendingTickList = new PendingTickList<>();
            pendingTickListEntriesHashSet = spool$pendingTickList;
        }
        if (pendingTickListEntriesTreeSet == null) pendingTickListEntriesTreeSet = new UnmodifiableTreeSet<>();
        if (entityIdMap == null) entityIdMap = new ConcurrentIntHashMap();
    }

    // Lambda replacement
    @Unique
    private void spool$chunkTask(ChunkCoordIntPair pair) {
        MinecraftLambdaOptimizedTasks.chunkTask(this, pair);
    }

    @Redirect(
        method = "tick()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldServer;func_147456_g()V"))
    public void func_147456_g(WorldServer instance) {
        super.func_147456_g();

        for (final ChunkCoordIntPair chunkcoordintpair : this.activeChunkSet) {
            if (ThreadManagerConfig.useLambdaOptimization) {
                if (ThreadsConfig.isExperimentalThreadingEnabled())
                    Spool.REGISTERED_THREAD_MANAGERS.get(ManagerNames.BLOCK)
                        .execute(this::spool$chunkTask, chunkcoordintpair);
                else if (ThreadsConfig.isDistanceThreadingEnabled()) DistanceThreadingExecutors
                    .execute(this, chunkcoordintpair, this::spool$chunkTask, chunkcoordintpair);
                else spool$chunkTask(chunkcoordintpair);
            } else {
                Runnable chunkTask = () -> spool$chunkTask(chunkcoordintpair); // Effectively the same as without the
                                                                               // function.
                if (ThreadsConfig.isExperimentalThreadingEnabled())
                    Spool.REGISTERED_THREAD_MANAGERS.get(ManagerNames.BLOCK)
                        .execute(chunkTask);
                else if (ThreadsConfig.isDistanceThreadingEnabled())
                    DistanceThreadingExecutors.execute(this, chunkcoordintpair, chunkTask);
                else chunkTask.run();
            }
        }
    }

    // Lambda replacement
    @Unique
    private void spool$blockTask(Block block, int[] location) {
        MinecraftLambdaOptimizedTasks.blockTask(this, block, location);
    }

    @Redirect(
        method = "tick()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldServer;tickUpdates(Z)Z"))
    public boolean tickUpdates(WorldServer instance, boolean p_72955_1_) {

        int i = spool$pendingTickList.size();

        if (i > 10000) {
            i = 10000;
        }

        this.theProfiler.startSection("cleaning");
        NextTickListEntry nextticklistentry;

        for (int j = 0; j < i; ++j) {
            nextticklistentry = spool$pendingTickList.first();

            if (!p_72955_1_ && nextticklistentry.scheduledTime > this.worldInfo.getWorldTotalTime()) {
                break;
            }

            spool$pendingTickList.remove(nextticklistentry);
            this.pendingTickListEntriesThisTick.add(nextticklistentry);
        }

        this.theProfiler.endSection();

        this.theProfiler.startSection("ticking");

        Iterator<NextTickListEntry> iterator = this.pendingTickListEntriesThisTick.iterator();

        // noinspection SynchronizeOnNonFinalField
        synchronized (pendingTickListEntriesThisTick) {
            while (iterator.hasNext()) {
                nextticklistentry = iterator.next();
                iterator.remove();
                byte b0 = 0;

                if (this.checkChunksExist(
                    nextticklistentry.xCoord - b0,
                    nextticklistentry.yCoord - b0,
                    nextticklistentry.zCoord - b0,
                    nextticklistentry.xCoord + b0,
                    nextticklistentry.yCoord + b0,
                    nextticklistentry.zCoord + b0)) {
                    Block block = this
                        .getBlock(nextticklistentry.xCoord, nextticklistentry.yCoord, nextticklistentry.zCoord);

                    if (block.getMaterial() != Material.air
                        && Block.isEqualTo(block, nextticklistentry.func_151351_a())) {
                        try {
                            final int[] coords = new int[] { nextticklistentry.xCoord, nextticklistentry.yCoord,
                                nextticklistentry.zCoord };
                            if (ThreadManagerConfig.useLambdaOptimization) {
                                if (ThreadsConfig.isExperimentalThreadingEnabled())
                                    Spool.REGISTERED_THREAD_MANAGERS.get(ManagerNames.BLOCK)
                                        .execute(this::spool$blockTask, block, coords);
                                else if (ThreadsConfig.isDistanceThreadingEnabled()) DistanceThreadingExecutors
                                    .execute(this, coords[0], coords[2], this::spool$blockTask, false, block, coords);
                                else spool$blockTask(block, coords);
                            } else {
                                Runnable task = () -> block
                                    .updateTick(this, coords[0], coords[1], coords[2], this.rand);
                                if (ThreadsConfig.isExperimentalThreadingEnabled())
                                    Spool.REGISTERED_THREAD_MANAGERS.get(ManagerNames.BLOCK)
                                        .execute(task);
                                else if (ThreadsConfig.isDistanceThreadingEnabled())
                                    DistanceThreadingExecutors.execute(this, coords[0], coords[2], task, false);
                                else task.run();
                            }
                        } catch (Throwable throwable1) {
                            CrashReport crashreport = CrashReport
                                .makeCrashReport(throwable1, "Exception while ticking a block");
                            CrashReportCategory crashreportcategory = crashreport.makeCategory("Block being ticked");
                            int k;

                            try {
                                k = this.getBlockMetadata(
                                    nextticklistentry.xCoord,
                                    nextticklistentry.yCoord,
                                    nextticklistentry.zCoord);
                            } catch (Throwable throwable) {
                                k = -1;
                            }

                            CrashReportCategory.func_147153_a(
                                crashreportcategory,
                                nextticklistentry.xCoord,
                                nextticklistentry.yCoord,
                                nextticklistentry.zCoord,
                                block,
                                k);
                            throw new ReportedException(crashreport);
                        }
                    }
                } else {
                    this.scheduleBlockUpdate(
                        nextticklistentry.xCoord,
                        nextticklistentry.yCoord,
                        nextticklistentry.zCoord,
                        nextticklistentry.func_151351_a(),
                        0);
                }
            }
        }

        this.theProfiler.endSection();
        this.pendingTickListEntriesThisTick.clear();
        return !spool$pendingTickList.isEmpty();
    }

    @Unique
    private final ObjectArrayList<NextTickListEntry> spool$toRemove = new ObjectArrayList<>(); // Purely for
                                                                                               // reusability.

    /**
     * @author BallOfEnergy01
     * @reason Literally just making this better/faster/thread safe.
     */
    @Overwrite
    public List<NextTickListEntry> getPendingBlockUpdates(Chunk p_72920_1_, boolean p_72920_2_) {
        ObjectArrayList<NextTickListEntry> arraylist = new ObjectArrayList<>();
        ChunkCoordIntPair chunkcoordintpair = p_72920_1_.getChunkCoordIntPair();
        int i = (chunkcoordintpair.chunkXPos << 4) - 2;
        int j = i + 16 + 2;
        int k = (chunkcoordintpair.chunkZPos << 4) - 2;
        int l = k + 16 + 2;

        synchronized (spool$pendingTickList) {
            for (NextTickListEntry nextTickListEntry : spool$pendingTickList) {
                if (nextTickListEntry.xCoord >= i && nextTickListEntry.xCoord < j
                    && nextTickListEntry.zCoord >= k
                    && nextTickListEntry.zCoord < l) {
                    if (p_72920_2_) {
                        spool$toRemove.add(nextTickListEntry);
                    }

                    arraylist.add(nextTickListEntry);
                }
            }
        }

        ObjectList<NextTickListEntry> array;
        synchronized (this.pendingTickListEntriesThisTick) {
            array = new ObjectArrayList<>(this.pendingTickListEntriesThisTick);
        }

        for (NextTickListEntry nextticklistentry : array) {
            if (nextticklistentry.xCoord >= i && nextticklistentry.xCoord < j
                && nextticklistentry.zCoord >= k
                && nextticklistentry.zCoord < l) {
                if (p_72920_2_) {
                    spool$toRemove.add(nextticklistentry);
                }

                arraylist.add(nextticklistentry);
            }
        }

        spool$toRemove.forEach(spool$pendingTickList::remove);
        spool$toRemove.forEach(pendingTickListEntriesThisTick::remove);
        spool$toRemove.clear();

        return arraylist;
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @WrapOperation(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/management/PlayerManager;updatePlayerInstances()V"))
    public void updatePlayerInstances(final PlayerManager instance, Operation<Void> original) {
        synchronized (instance) {
            original.call(instance);
        }
    }

    /**
     * @author BallOfEnergy
     * @reason Chunk concurrency.
     */
    @Overwrite
    public List<TileEntity> func_147486_a(int p_147486_1_, int p_147486_2_, int p_147486_3_, int p_147486_4_,
        int p_147486_5_, int p_147486_6_) {
        ArrayList<TileEntity> arraylist = new ArrayList<>();

        for (int x = (p_147486_1_ >> 4); x <= (p_147486_4_ >> 4); x++) {
            for (int z = (p_147486_3_ >> 4); z <= (p_147486_6_ >> 4); z++) {
                Chunk chunk = getChunkFromChunkCoords(x, z);
                if (chunk != null) {
                    synchronized (chunk.chunkTileEntityMap) {
                        for (TileEntity obj : chunk.chunkTileEntityMap.values()) {
                            TileEntity entity = obj;
                            if (!entity.isInvalid()) {
                                if (entity.xCoord >= p_147486_1_ && entity.yCoord >= p_147486_2_
                                    && entity.zCoord >= p_147486_3_
                                    && entity.xCoord <= p_147486_4_
                                    && entity.yCoord <= p_147486_5_
                                    && entity.zCoord <= p_147486_6_) {
                                    arraylist.add(entity);
                                }
                            }
                        }
                    }
                }
            }
        }

        return arraylist;
    }

    // Pretty much all of this is just security/compatibility, since 90% of it has already been mixin'd in this class.
    // Though, if something else happens to edit these tables, it'll be crapped.
    // Ergo, it's better off to just leave this here...

    // Just ignore any adds.
    @SuppressWarnings("SameReturnValue")
    @Redirect(
        method = { "scheduleBlockUpdateWithPriority", "func_147446_b" },
        at = @At(value = "INVOKE", target = "Ljava/util/TreeSet;add(Ljava/lang/Object;)Z"))
    public boolean addToTreeSet(TreeSet<Object> instance, Object e) {
        return true;
    }

    // And removes...
    @SuppressWarnings("SameReturnValue")
    @Redirect(
        method = "tickUpdates",
        at = @At(value = "INVOKE", target = "Ljava/util/TreeSet;remove(Ljava/lang/Object;)Z"))
    public boolean removeFromTreeSet(TreeSet<Object> instance, Object e) {
        return true;
    }

    // Redirect size calls.
    @Redirect(method = "tickUpdates", at = @At(value = "INVOKE", target = "Ljava/util/TreeSet;size()I"))
    public int sizeOfTreeSet(TreeSet<Object> instance) {
        return spool$pendingTickList.size();
    }

    // Redirect first calls.
    @Redirect(
        method = "tickUpdates",
        at = @At(value = "INVOKE", target = "Ljava/util/TreeSet;first()Ljava/lang/Object;"))
    public Object firstFromTreeSet(TreeSet<Object> instance) {
        return spool$pendingTickList.first();
    }

    // Redirect isEmpty calls.
    @Redirect(method = "tickUpdates", at = @At(value = "INVOKE", target = "Ljava/util/TreeSet;isEmpty()Z"))
    public boolean isTreeSetEmpty(TreeSet<Object> instance) {
        return spool$pendingTickList.isEmpty();
    }

    @WrapMethod(method = "updateAllPlayersSleepingFlag")
    private void updateAllPlayersSleepingFlag(Operation<Void> original) {
        synchronized (playerEntities) {
            original.call();
        }
    }

    @WrapMethod(method = "wakeAllPlayers")
    private void wakeAllPlayers(Operation<Void> original) {
        synchronized (playerEntities) {
            original.call();
        }
    }

    @WrapMethod(method = "areAllPlayersAsleep")
    private boolean areAllPlayersAsleep(Operation<Boolean> original) {
        synchronized (playerEntities) {
            return original.call();
        }
    }

    @WrapMethod(method = "newExplosion")
    private Explosion newExplosion(Entity p_72885_1_, double p_72885_2_, double p_72885_4_, double p_72885_6_,
        float p_72885_8_, boolean p_72885_9_, boolean p_72885_10_, Operation<Explosion> original) {
        synchronized (playerEntities) {
            return original.call(p_72885_1_, p_72885_2_, p_72885_4_, p_72885_6_, p_72885_8_, p_72885_9_, p_72885_10_);
        }
    }

    @WrapMethod(method = "func_147487_a")
    private void func_147487_a(String p_147487_1_, double p_147487_2_, double p_147487_4_, double p_147487_6_,
        int p_147487_8_, double p_147487_9_, double p_147487_11_, double p_147487_13_, double p_147487_15_,
        Operation<Void> original) {
        synchronized (playerEntities) {
            original.call(
                p_147487_1_,
                p_147487_2_,
                p_147487_4_,
                p_147487_6_,
                p_147487_8_,
                p_147487_9_,
                p_147487_11_,
                p_147487_13_,
                p_147487_15_);
        }
    }
}
