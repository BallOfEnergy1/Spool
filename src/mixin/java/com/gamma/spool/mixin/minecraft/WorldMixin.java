package com.gamma.spool.mixin.minecraft;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;

import net.minecraft.block.Block;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraftforge.common.ForgeModContainer;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.core.SpoolCompat;
import com.gamma.spool.util.LockHelper;
import com.gamma.spool.util.MinecraftTasks;
import com.gamma.spool.util.RWLockedList;
import com.gamma.spool.util.SidedLock;
import com.gamma.spool.util.distance.DistanceThreadingExecutors;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mitchej123.hodgepodge.config.FixesConfig;
import com.mitchej123.hodgepodge.hax.LongChunkCoordIntPairSet;

import cpw.mods.fml.common.FMLLog;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

@Mixin(value = World.class, priority = 1001)
public abstract class WorldMixin {

    @Shadow
    public List<Entity> loadedEntityList;

    @Shadow
    protected List<Entity> unloadedEntityList;

    @Shadow
    public List<TileEntity> loadedTileEntityList;

    @Shadow
    private List<TileEntity> addedTileEntityList;

    @Shadow
    private List<TileEntity> field_147483_b; // removedTileEntityList

    @Shadow
    public List<EntityPlayer> playerEntities;

    @Shadow
    public List<Entity> weatherEffects;

    @Shadow
    public boolean field_147481_N;

    @Shadow
    public boolean isRemote;

    @Shadow
    private int ambientTickCountdown;

    @Shadow
    public Random rand;

    @Shadow
    protected Set<ChunkCoordIntPair> activeChunkSet;

    @Shadow
    @Final
    public WorldProvider provider;

    @Invoker("chunkExists")
    public abstract boolean invokeChunkExists(int x, int z);

    @Unique
    private World spool$instance;

    @Unique
    private final AtomicInteger spool$ambientTickCountdown = new AtomicInteger(this.ambientTickCountdown);

    // Purely to support adding/removing world accessors dynamically during runtime.
    // If I didn't want to support this, I'd just make the list immutable and be done.
    @Unique
    private final ReadWriteLock spool$worldAccessRW = new SidedLock((World) (Object) this);

    @SuppressWarnings("DataFlowIssue")
    @Unique
    private final ReadWriteLock spool$loadedEntityLock = new SidedLock((World) (Object) this);

    @Inject(
        method = "<init>(Lnet/minecraft/world/storage/ISaveHandler;Ljava/lang/String;Lnet/minecraft/world/WorldSettings;Lnet/minecraft/world/WorldProvider;Lnet/minecraft/profiler/Profiler;)V",
        at = @At("RETURN"))
    private void onInitServer(ISaveHandler p_i45369_1_, String p_i45369_2_, WorldSettings p_i45369_3_,
        WorldProvider p_i45369_4_, Profiler p_i45369_5_, CallbackInfo ci) {
        loadedEntityList = new RWLockedList<>(spool$loadedEntityLock, new ObjectArrayList<>());
        if (SpoolCompat.isModLoadedFast(SpoolCompat.CompatibleMods.HODGEPODGE)
            && FixesConfig.fixTooManyAllocationsChunkPositionIntPair)
            activeChunkSet = LockHelper
                .createRWLockedActiveChunkSet(provider.dimensionId, new LongChunkCoordIntPairSet());
        else activeChunkSet = LockHelper.createRWLockedActiveChunkSet(provider.dimensionId, new ObjectOpenHashSet<>());

        playerEntities = new CopyOnWriteArrayList<>();

        unloadedEntityList = ObjectLists.synchronize(new ObjectArrayList<>());
        loadedTileEntityList = ObjectLists.synchronize(new ObjectArrayList<>());
        addedTileEntityList = ObjectLists.synchronize(new ObjectArrayList<>());
        field_147483_b = ObjectLists.synchronize(new ObjectArrayList<>());
        weatherEffects = ObjectLists.synchronize(new ObjectArrayList<>());
    }

    /**
     * @author BallOfEnergy
     * @reason Ensure that the colliding bounding boxes aren't conflicting.
     *         TODO: FIX THIS IT'S SO BAD OH MY GOD
     */
    @Inject(method = "getCollidingBoundingBoxes", at = @At("HEAD"), cancellable = true)
    public void getCollidingBoundingBoxes(Entity p_72945_1_, AxisAlignedBB p_72945_2_,
        CallbackInfoReturnable<List<AxisAlignedBB>> cir) {
        if (p_72945_1_ instanceof EntityItem) {
            cir.setReturnValue(Collections.emptyList());
            return;
        }
        World instance = (World) (Object) this;
        List<AxisAlignedBB> colliding = new ObjectArrayList<>();
        int i = MathHelper.floor_double(p_72945_2_.minX);
        int j = MathHelper.floor_double(p_72945_2_.maxX + 1.0D);
        int k = MathHelper.floor_double(p_72945_2_.minY);
        int l = MathHelper.floor_double(p_72945_2_.maxY + 1.0D);
        int i1 = MathHelper.floor_double(p_72945_2_.minZ);
        int j1 = MathHelper.floor_double(p_72945_2_.maxZ + 1.0D);

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = i1; l1 < j1; ++l1) {
                if (instance.blockExists(k1, 64, l1)) {
                    for (int i2 = k - 1; i2 < l; ++i2) {
                        Block block;

                        if (k1 >= -30000000 && k1 < 30000000 && l1 >= -30000000 && l1 < 30000000) {
                            block = instance.getBlock(k1, i2, l1);
                        } else {
                            block = Blocks.stone;
                        }

                        block.addCollisionBoxesToList(instance, k1, i2, l1, p_72945_2_, colliding, p_72945_1_);
                    }
                }
            }
        }

        double d0 = 0.25D;
        List<Entity> list = instance.getEntitiesWithinAABBExcludingEntity(p_72945_1_, p_72945_2_.expand(d0, d0, d0));

        for (Entity entity : list) {
            AxisAlignedBB axisalignedbb1 = entity.getBoundingBox();

            if (axisalignedbb1 != null && axisalignedbb1.intersectsWith(p_72945_2_)) {
                colliding.add(axisalignedbb1);
            }

            axisalignedbb1 = p_72945_1_.getCollisionBox(entity);

            if (axisalignedbb1 != null && axisalignedbb1.intersectsWith(p_72945_2_)) {
                colliding.add(axisalignedbb1);
            }
        }

        cir.setReturnValue(colliding);
    }

    /**
     * @author BallOfEnergy
     * @reason Ensure that the colliding bounding boxes aren't conflicting.
     */
    @Overwrite
    public List<AxisAlignedBB> func_147461_a(AxisAlignedBB p_147461_1_) {
        World instance = (World) (Object) this;
        List<AxisAlignedBB> colliding = new ObjectArrayList<>();
        int i = MathHelper.floor_double(p_147461_1_.minX);
        int j = MathHelper.floor_double(p_147461_1_.maxX + 1.0D);
        int k = MathHelper.floor_double(p_147461_1_.minY);
        int l = MathHelper.floor_double(p_147461_1_.maxY + 1.0D);
        int i1 = MathHelper.floor_double(p_147461_1_.minZ);
        int j1 = MathHelper.floor_double(p_147461_1_.maxZ + 1.0D);

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = i1; l1 < j1; ++l1) {
                if (instance.blockExists(k1, 64, l1)) {
                    for (int i2 = k - 1; i2 < l; ++i2) {
                        Block block;

                        if (k1 >= -30000000 && k1 < 30000000 && l1 >= -30000000 && l1 < 30000000) {
                            block = instance.getBlock(k1, i2, l1);
                        } else {
                            block = Blocks.bedrock;
                        }

                        block.addCollisionBoxesToList(instance, k1, i2, l1, p_147461_1_, colliding, null);
                    }
                }
            }
        }

        return colliding;
    }

    /**
     * @author BallOfEnergy01
     * @reason Add concurrency and threading for entity updates.
     */
    @Overwrite
    public void updateEntities() {
        if (spool$instance == null) spool$instance = (World) (Object) this;
        spool$instance.theProfiler.startSection("entities");
        spool$instance.theProfiler.startSection("global");

        int i;
        Entity entity;
        CrashReport crashreport;
        CrashReportCategory crashreportcategory;

        synchronized (weatherEffects) {
            for (i = 0; i < weatherEffects.size(); i++) {
                entity = weatherEffects.get(i);

                try {
                    ++entity.ticksExisted;
                    if (!this.isRemote && ThreadsConfig.isDistanceThreadingEnabled())
                        DistanceThreadingExecutors.execute(entity, entity::onUpdate);
                    else entity.onUpdate();
                } catch (Throwable throwable2) {
                    crashreport = CrashReport.makeCrashReport(throwable2, "Ticking entity");
                    crashreportcategory = crashreport.makeCategory("Entity being ticked");

                    if (entity == null) {
                        crashreportcategory.addCrashSection("Entity", "~~NULL~~");
                    } else {
                        entity.addEntityCrashInfo(crashreportcategory);
                    }

                    if (ForgeModContainer.removeErroringEntities) {
                        FMLLog.getLogger()
                            .log(org.apache.logging.log4j.Level.ERROR, crashreport.getCompleteReport());
                        spool$instance.removeEntity(entity);
                    } else {
                        throw new ReportedException(crashreport);
                    }
                }

                if (entity.isDead) {
                    weatherEffects.remove(i--);
                }
            }
        }

        spool$instance.theProfiler.endStartSection("remove");
        loadedEntityList.removeAll(new ReferenceOpenHashSet<>(this.unloadedEntityList)); // Hodgepodge
        int j;
        int l;

        synchronized (unloadedEntityList) {
            for (i = 0; i < this.unloadedEntityList.size(); i++) {
                entity = this.unloadedEntityList.get(i);
                j = entity.chunkCoordX;
                l = entity.chunkCoordZ;

                if (entity.addedToChunk && this.invokeChunkExists(j, l)) {
                    spool$instance.getChunkFromChunkCoords(j, l)
                        .removeEntity(entity);
                }
                spool$instance.onEntityRemoved(entity);
            }
        }

        this.unloadedEntityList.clear();
        spool$instance.theProfiler.endStartSection("regular");

        synchronized (loadedEntityList) {
            for (i = 0; i < loadedEntityList.size(); i++) {
                entity = loadedEntityList.get(i);

                if (entity.ridingEntity != null) {
                    if (!entity.ridingEntity.isDead && entity.ridingEntity.riddenByEntity == entity) {
                        continue;
                    }

                    entity.ridingEntity.riddenByEntity = null;
                    entity.ridingEntity = null;
                }

                spool$instance.theProfiler.startSection("tick");

                if (!entity.isDead) {
                    try {
                        if (!this.isRemote && ThreadsConfig.isDistanceThreadingEnabled()) DistanceThreadingExecutors
                            .execute(entity, MinecraftTasks::entityTask, spool$instance, entity);
                        else spool$instance.updateEntity(entity);
                    } catch (Throwable throwable1) {
                        crashreport = CrashReport.makeCrashReport(throwable1, "Ticking entity");
                        crashreportcategory = crashreport.makeCategory("Entity being ticked");
                        entity.addEntityCrashInfo(crashreportcategory);

                        if (ForgeModContainer.removeErroringEntities) {
                            FMLLog.getLogger()
                                .log(org.apache.logging.log4j.Level.ERROR, crashreport.getCompleteReport());
                            spool$instance.removeEntity(entity);
                        } else {
                            throw new ReportedException(crashreport);
                        }
                    }
                }

                spool$instance.theProfiler.endSection();
                spool$instance.theProfiler.startSection("remove");
                if (entity.isDead) {
                    j = entity.chunkCoordX;
                    l = entity.chunkCoordZ;

                    if (entity.addedToChunk && this.invokeChunkExists(j, l)) {
                        spool$instance.getChunkFromChunkCoords(j, l)
                            .removeEntity(entity);
                    }

                    loadedEntityList.remove(i--);
                    spool$instance.onEntityRemoved(entity);
                }
                spool$instance.theProfiler.endSection();
            }
        }

        spool$instance.theProfiler.endStartSection("pendingBlockEntitiesPre");

        // noinspection SynchronizeOnNonFinalField
        synchronized (addedTileEntityList) {
            if (!this.addedTileEntityList.isEmpty()) {
                for (TileEntity tileentity1 : this.addedTileEntityList) {
                    if (!tileentity1.isInvalid()) {
                        if (!loadedTileEntityList.contains(tileentity1)) {
                            loadedTileEntityList.add(tileentity1);
                        }
                    } else {
                        if (this.invokeChunkExists(tileentity1.xCoord >> 4, tileentity1.zCoord >> 4)) {
                            Chunk chunk1 = spool$instance
                                .getChunkFromChunkCoords(tileentity1.xCoord >> 4, tileentity1.zCoord >> 4);

                            if (chunk1 != null) {
                                chunk1.removeInvalidTileEntity(
                                    tileentity1.xCoord & 15,
                                    tileentity1.yCoord,
                                    tileentity1.zCoord & 15);
                            }
                        }
                    }
                }

                this.addedTileEntityList.clear();
            }
        }

        spool$instance.theProfiler.endStartSection("blockEntities");
        this.field_147481_N = true;
        Iterator<TileEntity> iterator = loadedTileEntityList.iterator();

        synchronized (loadedTileEntityList) {
            while (iterator.hasNext()) {
                TileEntity tileentity = iterator.next();

                if (!tileentity.isInvalid() && tileentity.hasWorldObj()
                    && spool$instance.blockExists(tileentity.xCoord, tileentity.yCoord, tileentity.zCoord)) {
                    try {
                        // No lambda optimization needed here, already method reference!
                        if (!this.isRemote && ThreadsConfig.isDistanceThreadingEnabled())
                            DistanceThreadingExecutors.execute(tileentity, tileentity::updateEntity);
                        else tileentity.updateEntity();
                    } catch (Throwable throwable) {
                        crashreport = CrashReport.makeCrashReport(throwable, "Ticking block entity");
                        crashreportcategory = crashreport.makeCategory("Block entity being ticked");
                        tileentity.func_145828_a(crashreportcategory);
                        if (ForgeModContainer.removeErroringTileEntities) {
                            FMLLog.getLogger()
                                .log(org.apache.logging.log4j.Level.ERROR, crashreport.getCompleteReport());
                            tileentity.invalidate();
                            spool$instance.setBlockToAir(tileentity.xCoord, tileentity.yCoord, tileentity.zCoord);
                        } else {
                            throw new ReportedException(crashreport);
                        }
                    }
                }

                if (tileentity.isInvalid()) {
                    iterator.remove();

                    if (this.invokeChunkExists(tileentity.xCoord >> 4, tileentity.zCoord >> 4)) {
                        Chunk chunk = spool$instance
                            .getChunkFromChunkCoords(tileentity.xCoord >> 4, tileentity.zCoord >> 4);

                        if (chunk != null) {
                            chunk.removeInvalidTileEntity(
                                tileentity.xCoord & 15,
                                tileentity.yCoord,
                                tileentity.zCoord & 15);
                        }
                    }
                }
            }
        }

        spool$instance.theProfiler.endStartSection("pendingBlockEntitiesPost");

        // noinspection SynchronizeOnNonFinalField
        synchronized (addedTileEntityList) {
            if (!this.addedTileEntityList.isEmpty()) {
                for (TileEntity tileentity1 : this.addedTileEntityList) {
                    if (!tileentity1.isInvalid()) {
                        if (!loadedTileEntityList.contains(tileentity1)) {
                            loadedTileEntityList.add(tileentity1);
                        }
                    } else {
                        if (this.invokeChunkExists(tileentity1.xCoord >> 4, tileentity1.zCoord >> 4)) {
                            Chunk chunk1 = spool$instance
                                .getChunkFromChunkCoords(tileentity1.xCoord >> 4, tileentity1.zCoord >> 4);

                            if (chunk1 != null) {
                                chunk1.removeInvalidTileEntity(
                                    tileentity1.xCoord & 15,
                                    tileentity1.yCoord,
                                    tileentity1.zCoord & 15);
                            }
                        }
                    }
                }

                this.addedTileEntityList.clear();
            }
        }

        synchronized (field_147483_b) {
            if (!this.field_147483_b.isEmpty()) {
                for (TileEntity tile : this.field_147483_b) {
                    tile.onChunkUnload();
                }

                loadedTileEntityList.removeAll(new ReferenceOpenHashSet<>(this.field_147483_b)); // Hodgepodge
                this.field_147483_b.clear();
            }
        }

        this.field_147481_N = false;

        spool$instance.theProfiler.endSection();
        spool$instance.theProfiler.endSection();
    }

    @WrapMethod(method = "setTileEntity")
    private void setTileEntityWrapped(int x, int y, int z, TileEntity tileEntityIn, Operation<Void> original) {
        synchronized (addedTileEntityList) {
            original.call(x, y, z, tileEntityIn);
        }
    }

    // @Inject(method = "setTileEntity", at = @At(value = "INVOKE", target =
    // "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    // private void setTileEntityHead(int x, int y, int z, TileEntity tileEntityIn, CallbackInfo ci) {
    // spool$addedTileEntityLock.readLock()
    // .lock();
    // }

    // @Inject(method = "setTileEntity", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
    // ordinal = 0, shift = At.Shift.AFTER))
    // private void setTileEntityReturn(int x, int y, int z, TileEntity tileEntityIn, CallbackInfo ci) {
    // spool$addedTileEntityLock.readLock()
    // .unlock();
    // }

    @WrapMethod(method = "getTileEntity")
    private TileEntity getTileEntityWrapped(int x, int y, int z, Operation<TileEntity> original) {
        synchronized (addedTileEntityList) {
            return original.call(x, y, z);
        }
    }

    // @Inject(method = "getTileEntity", at = @At("HEAD"))
    // private void getTileEntityHead(int x, int y, int z, CallbackInfoReturnable<TileEntity> cir) {
    // spool$addedTileEntityLock.readLock()
    // .lock();
    // }

    // @Inject(method = "getTileEntity", at = @At("RETURN"))
    // private void getTileEntityReturn(int x, int y, int z, CallbackInfoReturnable<TileEntity> cir) {
    // spool$addedTileEntityLock.readLock()
    // .unlock();
    // }

    @Definition(id = "ambientTickCountdown", field = "Lnet/minecraft/world/World;ambientTickCountdown:I")
    @Expression("this.ambientTickCountdown > 0")
    @ModifyExpressionValue(method = "setActivePlayerChunksAndCheckLight", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean redirectedAmbientTickCountdownCheck1(boolean original) {
        return true;
    }

    @Redirect(
        method = "setActivePlayerChunksAndCheckLight",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/World;ambientTickCountdown:I",
            opcode = Opcodes.PUTFIELD))
    private void redirectedAmbientTickCountdownDecrement(World instance, int value) {
        spool$ambientTickCountdown.decrementAndGet();
    }

    @Definition(id = "ambientTickCountdown", field = "Lnet/minecraft/world/World;ambientTickCountdown:I")
    @Expression("this.ambientTickCountdown == 0")
    @ModifyExpressionValue(method = "func_147467_a", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean redirectedAmbientTickCountdownCheck2(boolean original) {
        int previousValue;
        int newValue;
        do {
            previousValue = spool$ambientTickCountdown.get();
            if (previousValue > 0) return false;
            newValue = this.rand.nextInt(12000) + 6000;
        } while (spool$ambientTickCountdown.compareAndSet(previousValue, newValue));
        return true;
    }

    @Redirect(
        method = "func_147467_a",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/World;ambientTickCountdown:I",
            opcode = Opcodes.PUTFIELD))
    private void redirectedAmbientTickCountdownAssign(World instance, int value) {
        // NOOP
    }

    @Inject(method = "countEntities(Ljava/lang/Class;)I", at = @At("HEAD"))
    private void countEntitiesHead(Class<? extends Entity> p_72907_1_, CallbackInfoReturnable<Integer> cir) {
        spool$loadedEntityLock.readLock()
            .lock();
    }

    @Inject(method = "countEntities(Ljava/lang/Class;)I", at = @At("RETURN"))
    private void countEntitiesReturn(Class<? extends Entity> p_72907_1_, CallbackInfoReturnable<Integer> cir) {
        spool$loadedEntityLock.readLock()
            .unlock();
    }

    @Inject(method = "countEntities(Lnet/minecraft/entity/EnumCreatureType;Z)I", at = @At("HEAD"), remap = false)
    private void countEntitiesHead(EnumCreatureType type, boolean forSpawnCount, CallbackInfoReturnable<Integer> cir) {
        spool$loadedEntityLock.readLock()
            .lock();
    }

    @Inject(method = "countEntities(Lnet/minecraft/entity/EnumCreatureType;Z)I", at = @At("RETURN"), remap = false)
    private void countEntitiesReturn(EnumCreatureType type, boolean forSpawnCount,
        CallbackInfoReturnable<Integer> cir) {
        spool$loadedEntityLock.readLock()
            .unlock();
    }

    @Inject(method = "markBlockForUpdate", at = @At("HEAD"))
    private void markBlockForUpdateHead(int p_147471_1_, int p_147471_2_, int p_147471_3_, CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .lock();
    }

    @Inject(method = "markBlockForUpdate", at = @At("RETURN"))
    private void markBlockForUpdateReturn(int p_147471_1_, int p_147471_2_, int p_147471_3_, CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .unlock();
    }

    @Inject(method = "markBlockRangeForRenderUpdate", at = @At("HEAD"))
    private void markBlockRangeForRenderUpdateHead(int p_147458_1_, int p_147458_2_, int p_147458_3_, int p_147458_4_,
        int p_147458_5_, int p_147458_6_, CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .lock();
    }

    @Inject(method = "markBlockRangeForRenderUpdate", at = @At("RETURN"))
    private void markBlockRangeForRenderUpdateReturn(int p_147458_1_, int p_147458_2_, int p_147458_3_, int p_147458_4_,
        int p_147458_5_, int p_147458_6_, CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .unlock();
    }

    @Inject(method = "setLightValue", at = @At("HEAD"))
    private void setLightValueHead(EnumSkyBlock p_72915_1_, int p_72915_2_, int p_72915_3_, int p_72915_4_,
        int p_72915_5_, CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .lock();
    }

    @Inject(method = "setLightValue", at = @At("RETURN"))
    private void setLightValueReturn(EnumSkyBlock p_72915_1_, int p_72915_2_, int p_72915_3_, int p_72915_4_,
        int p_72915_5_, CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .unlock();
    }

    @Inject(method = "func_147479_m", at = @At("HEAD"))
    private void func_147479_mHead(int p_147479_1_, int p_147479_2_, int p_147479_3_, CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .lock();
    }

    @Inject(method = "func_147479_m", at = @At("RETURN"))
    private void func_147479_mReturn(int p_147479_1_, int p_147479_2_, int p_147479_3_, CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .unlock();
    }

    @Inject(method = "playSoundAtEntity", at = @At("HEAD"))
    private void playSoundAtEntityHead(Entity p_72956_1_, String p_72956_2_, float p_72956_3_, float p_72956_4_,
        CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .lock();
    }

    @Inject(method = "playSoundAtEntity", at = @At("RETURN"))
    private void playSoundAtEntityReturn(Entity p_72956_1_, String p_72956_2_, float p_72956_3_, float p_72956_4_,
        CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .unlock();
    }

    @Inject(method = "playSoundToNearExcept", at = @At("HEAD"))
    private void playSoundToNearExceptHead(EntityPlayer p_85173_1_, String p_85173_2_, float p_85173_3_,
        float p_85173_4_, CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .lock();
    }

    @Inject(method = "playSoundToNearExcept", at = @At("RETURN"))
    private void playSoundToNearExceptReturn(EntityPlayer p_85173_1_, String p_85173_2_, float p_85173_3_,
        float p_85173_4_, CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .unlock();
    }

    @Inject(method = "playSoundEffect", at = @At("HEAD"))
    private void playSoundEffectHead(double x, double y, double z, String soundName, float volume, float pitch,
        CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .lock();
    }

    @Inject(method = "playSoundEffect", at = @At("RETURN"))
    private void playSoundEffectReturn(double x, double y, double z, String soundName, float volume, float pitch,
        CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .unlock();
    }

    @Inject(method = "playRecord", at = @At("HEAD"))
    private void playRecordHead(String recordName, int x, int y, int z, CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .lock();
    }

    @Inject(method = "playRecord", at = @At("RETURN"))
    private void playRecordReturn(String recordName, int x, int y, int z, CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .unlock();
    }

    @Inject(method = "spawnParticle", at = @At("HEAD"))
    private void spawnParticleHead(String particleName, double x, double y, double z, double velocityX,
        double velocityY, double velocityZ, CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .lock();
    }

    @Inject(method = "spawnParticle", at = @At("RETURN"))
    private void spawnParticleReturn(String particleName, double x, double y, double z, double velocityX,
        double velocityY, double velocityZ, CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .unlock();
    }

    @Inject(method = "onEntityAdded", at = @At("HEAD"))
    private void onEntityAddedHead(Entity p_72923_1_, CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .lock();
    }

    @Inject(method = "onEntityAdded", at = @At("RETURN"))
    private void onEntityAddedReturn(Entity p_72923_1_, CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .unlock();
    }

    @Inject(method = "onEntityRemoved", at = @At("HEAD"))
    private void onEntityRemovedHead(Entity p_72923_1_, CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .lock();
    }

    @Inject(method = "onEntityRemoved", at = @At("RETURN"))
    private void onEntityRemovedReturn(Entity p_72923_1_, CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .unlock();
    }

    @Inject(method = "playBroadcastSound", at = @At("HEAD"))
    private void playBroadcastSoundHead(int p_82739_1_, int p_82739_2_, int p_82739_3_, int p_82739_4_, int p_82739_5_,
        CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .lock();
    }

    @Inject(method = "playBroadcastSound", at = @At("RETURN"))
    private void playBroadcastSoundReturn(int p_82739_1_, int p_82739_2_, int p_82739_3_, int p_82739_4_,
        int p_82739_5_, CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .unlock();
    }

    @Inject(method = "playAuxSFXAtEntity", at = @At("HEAD"))
    private void playAuxSFXAtEntityHead(EntityPlayer player, int p_72889_2_, int x, int y, int z, int p_72889_6_,
        CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .lock();
    }

    @Inject(method = "playAuxSFXAtEntity", at = @At("RETURN"))
    private void playAuxSFXAtEntityReturn(EntityPlayer player, int p_72889_2_, int x, int y, int z, int p_72889_6_,
        CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .unlock();
    }

    @Inject(method = "destroyBlockInWorldPartially", at = @At("HEAD"))
    private void destroyBlockInWorldPartiallyHead(int p_147443_1_, int x, int y, int z, int blockDamage,
        CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .lock();
    }

    @Inject(method = "destroyBlockInWorldPartially", at = @At("RETURN"))
    private void destroyBlockInWorldPartiallyReturn(int p_147443_1_, int x, int y, int z, int blockDamage,
        CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .unlock();
    }

    @Inject(method = "func_147450_X", at = @At("HEAD"))
    private void func_147450_XHead(CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .lock();
    }

    @Inject(method = "func_147450_X", at = @At("RETURN"))
    private void func_147450_XReturn(CallbackInfo ci) {
        spool$worldAccessRW.readLock()
            .unlock();
    }

    @Inject(method = "addWorldAccess", at = @At("HEAD"))
    private void addWorldAccessHead(CallbackInfo ci) {
        spool$worldAccessRW.writeLock()
            .lock();
    }

    @Inject(method = "addWorldAccess", at = @At("RETURN"))
    private void addWorldAccessReturn(CallbackInfo ci) {
        spool$worldAccessRW.writeLock()
            .unlock();
    }

    @Inject(method = "removeWorldAccess", at = @At("HEAD"))
    private void removeWorldAccessHead(CallbackInfo ci) {
        spool$worldAccessRW.writeLock()
            .lock();
    }

    @Inject(method = "removeWorldAccess", at = @At("RETURN"))
    private void removeWorldAccessReturn(CallbackInfo ci) {
        spool$worldAccessRW.writeLock()
            .unlock();
    }

    @Inject(method = "setActivePlayerChunksAndCheckLight", at = @At("HEAD"))
    private void setActivePlayerChunksAndCheckLightHead(CallbackInfo ci) {
        LockHelper.writeLockActiveChunkSet(provider.dimensionId);
    }

    @Inject(method = "setActivePlayerChunksAndCheckLight", at = @At("RETURN"))
    private void setActivePlayerChunksAndCheckLightReturn(CallbackInfo ci) {
        LockHelper.writeUnlockActiveChunkSet(provider.dimensionId);
    }

    @WrapMethod(method = "updateLightByType")
    private boolean updateLightByTypeWrapped(EnumSkyBlock p_147463_1_, int p_147463_2_, int p_147463_3_,
        int p_147463_4_, Operation<Boolean> original) {
        synchronized (this) {
            return original.call(p_147463_1_, p_147463_2_, p_147463_3_, p_147463_4_);
        }
    }
}
