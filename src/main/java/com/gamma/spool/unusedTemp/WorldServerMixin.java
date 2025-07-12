package com.gamma.spool.unusedTemp;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.init.Blocks;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.util.IntHashMap;
import net.minecraft.util.ReportedException;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
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
import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.thread.ManagerNames;
import com.gamma.spool.util.ConcurrentIntHashMap;
import com.gamma.spool.util.PendingTickList;
import com.gamma.spool.util.UnmodifiableTreeSet;
import com.gamma.spool.util.distance.DistanceThreadingExecutors;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mitchej123.hodgepodge.util.ChunkPosUtil;

import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

// Partially taken from Hodgepodge and implemented here for compatibility.
@SuppressWarnings({ "UnusedMixin", "unused" })
@Mixin(value = WorldServer.class, priority = 1001)
public abstract class WorldServerMixin extends World implements ISimulationDistanceWorld {

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

    @Unique
    private final Map<Long, Boolean> hodgepodge$processChunk = new Object2BooleanOpenHashMap<>();

    @Unique
    private final ExtendedBlockStorage[] hodgepodge$emptyBlockStorage = new ExtendedBlockStorage[0];

    @Inject(method = "<init>", at = @At("TAIL"))
    private void hodgepodge$initSimulationHelper(MinecraftServer p_i45284_1_, ISaveHandler p_i45284_2_,
        String p_i45284_3_, int p_i45284_4_, WorldSettings p_i45284_5_, Profiler p_i45284_6_, CallbackInfo ci) {
        SimulationDistanceHelper helper = hodgepodge$getSimulationDistanceHelper();
        helper.setServerVariables(pendingTickListEntriesHashSet, this::chunkExists);
    }

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
        loadedEntityList = ObjectLists.synchronize(new ObjectArrayList<>());
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

    @Redirect(
        method = "tick()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldServer;func_147456_g()V"))
    public void func_147456_g(WorldServer instance) {
        super.func_147456_g();
        AtomicInteger i = new AtomicInteger();
        AtomicInteger j = new AtomicInteger();

        for (ChunkCoordIntPair chunkcoordintpair : this.activeChunkSet) {
            SimulationDistanceHelper helper = hodgepodge$getSimulationDistanceHelper();
            boolean shouldProcess = helper.shouldProcessTick(chunkcoordintpair.chunkXPos, chunkcoordintpair.chunkZPos);
            if (shouldProcess) hodgepodge$processChunk
                .put(ChunkPosUtil.toLong(chunkcoordintpair.chunkXPos, chunkcoordintpair.chunkZPos), true);
            Runnable chunkTask = () -> {
                int k = chunkcoordintpair.chunkXPos * 16;
                int l = chunkcoordintpair.chunkZPos * 16;
                Chunk chunk = this.getChunkFromChunkCoords(chunkcoordintpair.chunkXPos, chunkcoordintpair.chunkZPos);
                this.func_147467_a(k, l, chunk);
                chunk.func_150804_b(false);

                int i1;
                int j1;
                int k1;
                int l1;

                if (hodgepodge$processChunk.get(ChunkPosUtil.toLong(chunk.xPosition, chunk.zPosition))
                    && provider.canDoLightning(chunk)
                    && this.rand.nextInt(100000) == 0
                    && this.isRaining()
                    && this.isThundering()) {
                    this.updateLCG = this.updateLCG * 3 + 1013904223;
                    i1 = this.updateLCG >> 2;
                    j1 = k + (i1 & 15);
                    k1 = l + (i1 >> 8 & 15);
                    l1 = this.getPrecipitationHeight(j1, k1);

                    if (this.canLightningStrikeAt(j1, l1, k1)) {
                        this.addWeatherEffect(new EntityLightningBolt(this, j1, l1, k1));
                    }
                }

                if (hodgepodge$processChunk.get(ChunkPosUtil.toLong(chunk.xPosition, chunk.zPosition))
                    && provider.canDoRainSnowIce(chunk)
                    && this.rand.nextInt(16) == 0) {
                    this.updateLCG = this.updateLCG * 3 + 1013904223;
                    i1 = this.updateLCG >> 2;
                    j1 = i1 & 15;
                    k1 = i1 >> 8 & 15;
                    l1 = this.getPrecipitationHeight(j1 + k, k1 + l);

                    if (this.isBlockFreezableNaturally(j1 + k, l1 - 1, k1 + l)) {
                        this.setBlock(j1 + k, l1 - 1, k1 + l, Blocks.ice);
                    }

                    if (this.isRaining() && this.func_147478_e(j1 + k, l1, k1 + l, true)) {
                        this.setBlock(j1 + k, l1, k1 + l, Blocks.snow_layer);
                    }

                    if (this.isRaining()) {
                        BiomeGenBase biomegenbase = this.getBiomeGenForCoords(j1 + k, k1 + l);

                        if (biomegenbase.canSpawnLightningBolt()) {
                            this.getBlock(j1 + k, l1 - 1, k1 + l)
                                .fillWithRain(this, j1 + k, l1 - 1, k1 + l);
                        }
                    }
                }

                ExtendedBlockStorage[] aextendedblockstorage;
                if (hodgepodge$processChunk.get(ChunkPosUtil.toLong(chunk.xPosition, chunk.zPosition)))
                    aextendedblockstorage = chunk.getBlockStorageArray();
                else aextendedblockstorage = hodgepodge$emptyBlockStorage;

                j1 = aextendedblockstorage.length;

                for (k1 = 0; k1 < j1; ++k1) {
                    ExtendedBlockStorage extendedblockstorage = aextendedblockstorage[k1];

                    if (extendedblockstorage != null && extendedblockstorage.getNeedsRandomTick()) {
                        for (int i3 = 0; i3 < 3; ++i3) {
                            Runnable blockTask = () -> {
                                this.updateLCG = this.updateLCG * 3 + 1013904223;
                                int i2 = this.updateLCG >> 2;
                                int j2 = i2 & 15;
                                int k2 = i2 >> 8 & 15;
                                int l2 = i2 >> 16 & 15;
                                j.incrementAndGet();
                                Block block = extendedblockstorage.getBlockByExtId(j2, l2, k2);

                                if (block.getTickRandomly()) {
                                    i.incrementAndGet();
                                    block.updateTick(
                                        this,
                                        j2 + k,
                                        l2 + extendedblockstorage.getYLocation(),
                                        k2 + l,
                                        this.rand);

                                }
                            };
                            if (ThreadsConfig.isExperimentalThreadingEnabled())
                                Spool.registeredThreadManagers.get(ManagerNames.BLOCK)
                                    .execute(blockTask);
                            // NOTE:
                            // Distance threading will not work on this level, as it's nested inside
                            // chunk-level threading.
                            else blockTask.run();
                        }
                    }
                }
            };
            if (ThreadsConfig.isExperimentalThreadingEnabled()) Spool.registeredThreadManagers.get(ManagerNames.BLOCK)
                .execute(chunkTask);
            else if (ThreadsConfig.isDistanceThreadingEnabled())
                DistanceThreadingExecutors.execute(this, chunkcoordintpair, chunkTask);
        }
    }

    @Redirect(
        method = "tick()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldServer;tickUpdates(Z)Z"))
    public boolean tickUpdates(WorldServer instance, boolean p_72955_1_) {

        if (!Spool.isHodgepodgeLoaded) {
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
        } else {
            SimulationDistanceHelper helper = hodgepodge$getSimulationDistanceHelper();
            helper.tickUpdates(p_72955_1_, this.pendingTickListEntriesThisTick);
        }

        this.theProfiler.startSection("ticking");

        Iterator<NextTickListEntry> iterator = this.pendingTickListEntriesThisTick.iterator();

        // noinspection SynchronizeOnNonFinalField
        synchronized (pendingTickListEntriesThisTick) {
            while (iterator.hasNext()) {
                NextTickListEntry nextticklistentry = iterator.next();
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
                            Runnable task = () -> block.updateTick(
                                this,
                                nextticklistentry.xCoord,
                                nextticklistentry.yCoord,
                                nextticklistentry.zCoord,
                                this.rand);
                            if (ThreadsConfig.isExperimentalThreadingEnabled())
                                Spool.registeredThreadManagers.get(ManagerNames.BLOCK)
                                    .execute(task);
                            else if (ThreadsConfig.isDistanceThreadingEnabled()) DistanceThreadingExecutors
                                .execute(this, nextticklistentry.xCoord, nextticklistentry.zCoord, task, false);
                            else task.run();
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

        for (NextTickListEntry nextticklistentry : ObjectLists
            .synchronize(new ObjectArrayList<>(this.pendingTickListEntriesThisTick))) {
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
     * Handle adding ticks
     */
    @WrapOperation(
        method = "scheduleBlockUpdateWithPriority",
        at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"))
    private boolean hodgepodge$addTick1(Set<NextTickListEntry> instance, Object e, Operation<Boolean> original) {
        SimulationDistanceHelper helper = hodgepodge$getSimulationDistanceHelper();
        helper.addTick((NextTickListEntry) e);
        return original.call(instance, e);
    }

    /**
     * Handle adding ticks
     */
    @WrapOperation(
        method = "func_147446_b",
        at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"))
    private boolean hodgepodge$addTick2(Set<NextTickListEntry> instance, Object e, Operation<Boolean> original) {
        SimulationDistanceHelper helper = hodgepodge$getSimulationDistanceHelper();
        helper.addTick((NextTickListEntry) e);
        return original.call(instance, e);
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
}
