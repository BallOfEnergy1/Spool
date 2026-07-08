package com.gamma.spool.mixin.minecraft;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;

import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ReportedException;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraftforge.common.ForgeModContainer;

import org.spongepowered.asm.lib.Opcodes;
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

import com.gamma.spool.api.annotations.Synchronize;
import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.util.MinecraftTasks;
import com.gamma.spool.util.RWLockedList;
import com.gamma.spool.util.SidedLock;
import com.gamma.spool.util.distance.DistanceThreadingExecutors;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import cpw.mods.fml.common.FMLLog;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
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
    protected List<IWorldAccess> worldAccesses;

    @Invoker("chunkExists")
    public abstract boolean invokeChunkExists(int x, int z);

    // @Unique
    // private final AtomicInteger spool$ambientTickCountdown = new AtomicInteger(this.ambientTickCountdown);

    @Unique
    private ReadWriteLock spool$loadedEntityLock;

    @Unique
    private final ThreadLocal<ArrayList<AxisAlignedBB>> spool$AABBCache = ThreadLocal.withInitial(ArrayList::new);

    @Inject(
        method = "<init>(Lnet/minecraft/world/storage/ISaveHandler;Ljava/lang/String;Lnet/minecraft/world/WorldSettings;Lnet/minecraft/world/WorldProvider;Lnet/minecraft/profiler/Profiler;)V",
        at = @At("RETURN"))
    private void onInitServer(ISaveHandler p_i45369_1_, String p_i45369_2_, WorldSettings p_i45369_3_,
        WorldProvider p_i45369_4_, Profiler p_i45369_5_, CallbackInfo ci) {
        loadedEntityList = new RWLockedList<>(
            spool$loadedEntityLock = new SidedLock((World) (Object) this),
            new ObjectArrayList<>());

        playerEntities = new CopyOnWriteArrayList<>();
        worldAccesses = new CopyOnWriteArrayList<>();

        unloadedEntityList = ObjectLists.synchronize(new ObjectArrayList<>());
        loadedTileEntityList = ObjectLists.synchronize(new ObjectArrayList<>());
        addedTileEntityList = ObjectLists.synchronize(new ObjectArrayList<>());
        field_147483_b = ObjectLists.synchronize(new ObjectArrayList<>());
        weatherEffects = ObjectLists.synchronize(new ObjectArrayList<>());
    }

    @Redirect(
        method = { "getCollidingBoundingBoxes", "func_147461_a" },
        at = @At(
            value = "FIELD",
            opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/world/World;collidingBoundingBoxes:Ljava/util/ArrayList;"))
    private ArrayList<AxisAlignedBB> getCollidingBoundingBoxesCache(World instance) {
        return spool$AABBCache.get();
    }

    /**
     * @author BallOfEnergy01
     * @reason Add concurrency and threading for entity updates.
     */
    @Overwrite
    public void updateEntities() {
        World instance = (World) (Object) this;
        instance.theProfiler.startSection("entities");
        instance.theProfiler.startSection("global");

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
                        instance.removeEntity(entity);
                    } else {
                        throw new ReportedException(crashreport);
                    }
                }

                if (entity.isDead) {
                    weatherEffects.remove(i--);
                }
            }
        }

        instance.theProfiler.endStartSection("remove");
        loadedEntityList.removeAll(new ReferenceOpenHashSet<>(this.unloadedEntityList)); // Hodgepodge
        int j;
        int l;

        synchronized (unloadedEntityList) {
            for (i = 0; i < this.unloadedEntityList.size(); i++) {
                entity = this.unloadedEntityList.get(i);
                j = entity.chunkCoordX;
                l = entity.chunkCoordZ;

                if (entity.addedToChunk && this.invokeChunkExists(j, l)) {
                    instance.getChunkFromChunkCoords(j, l)
                        .removeEntity(entity);
                }
                instance.onEntityRemoved(entity);
            }
        }

        this.unloadedEntityList.clear();
        instance.theProfiler.endStartSection("regular");

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

                instance.theProfiler.startSection("tick");

                if (!entity.isDead) {
                    try {
                        if (!this.isRemote && ThreadsConfig.isDistanceThreadingEnabled())
                            DistanceThreadingExecutors.execute(entity, MinecraftTasks::entityTask, instance, entity);
                        else instance.updateEntity(entity);
                    } catch (Throwable throwable1) {
                        crashreport = CrashReport.makeCrashReport(throwable1, "Ticking entity");
                        crashreportcategory = crashreport.makeCategory("Entity being ticked");
                        entity.addEntityCrashInfo(crashreportcategory);

                        if (ForgeModContainer.removeErroringEntities) {
                            FMLLog.getLogger()
                                .log(org.apache.logging.log4j.Level.ERROR, crashreport.getCompleteReport());
                            instance.removeEntity(entity);
                        } else {
                            throw new ReportedException(crashreport);
                        }
                    }
                }

                instance.theProfiler.endSection();
                instance.theProfiler.startSection("remove");
                if (entity.isDead) {
                    j = entity.chunkCoordX;
                    l = entity.chunkCoordZ;

                    if (entity.addedToChunk && this.invokeChunkExists(j, l)) {
                        instance.getChunkFromChunkCoords(j, l)
                            .removeEntity(entity);
                    }

                    loadedEntityList.remove(i--);
                    instance.onEntityRemoved(entity);
                }
                instance.theProfiler.endSection();
            }
        }

        instance.theProfiler.endStartSection("pendingBlockEntitiesPre");

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
                            Chunk chunk1 = instance
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

        instance.theProfiler.endStartSection("blockEntities");
        this.field_147481_N = true;
        Iterator<TileEntity> iterator = loadedTileEntityList.iterator();

        synchronized (loadedTileEntityList) {
            while (iterator.hasNext()) {
                TileEntity tileentity = iterator.next();

                if (!tileentity.isInvalid() && tileentity.hasWorldObj()
                    && instance.blockExists(tileentity.xCoord, tileentity.yCoord, tileentity.zCoord)) {
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
                            instance.setBlockToAir(tileentity.xCoord, tileentity.yCoord, tileentity.zCoord);
                        } else {
                            throw new ReportedException(crashreport);
                        }
                    }
                }

                if (tileentity.isInvalid()) {
                    iterator.remove();

                    if (this.invokeChunkExists(tileentity.xCoord >> 4, tileentity.zCoord >> 4)) {
                        Chunk chunk = instance.getChunkFromChunkCoords(tileentity.xCoord >> 4, tileentity.zCoord >> 4);

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

        instance.theProfiler.endStartSection("pendingBlockEntitiesPost");

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
                            Chunk chunk1 = instance
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

        instance.theProfiler.endSection();
        instance.theProfiler.endSection();
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

    // @Definition(id = "ambientTickCountdown", field = "Lnet/minecraft/world/World;ambientTickCountdown:I")
    // @Expression("this.ambientTickCountdown > 0")
    // @ModifyExpressionValue(method = "setActivePlayerChunksAndCheckLight", at = @At("MIXINEXTRAS:EXPRESSION"))
    // private boolean redirectedAmbientTickCountdownCheck1(boolean original) {
    // return true;
    // }

    // @Redirect(
    // method = "setActivePlayerChunksAndCheckLight",
    // at = @At(
    // value = "FIELD",
    // target = "Lnet/minecraft/world/World;ambientTickCountdown:I",
    // opcode = Opcodes.PUTFIELD))
    // private void redirectedAmbientTickCountdownDecrement(World instance, int value) {
    // spool$ambientTickCountdown.decrementAndGet();
    // }

    // @Definition(id = "ambientTickCountdown", field = "Lnet/minecraft/world/World;ambientTickCountdown:I")
    // @Expression("this.ambientTickCountdown == 0")
    // @ModifyExpressionValue(method = "func_147467_a", at = @At("MIXINEXTRAS:EXPRESSION"))
    // private boolean redirectedAmbientTickCountdownCheck2(boolean original) {
    // int previousValue;
    // int newValue;
    // do {
    // previousValue = spool$ambientTickCountdown.get();
    // if (previousValue > 0) return false;
    // newValue = this.rand.nextInt(12000) + 6000;
    // } while (spool$ambientTickCountdown.compareAndSet(previousValue, newValue));
    // return true;
    // }

    // @Redirect(
    // method = "func_147467_a",
    // at = @At(
    // value = "FIELD",
    // target = "Lnet/minecraft/world/World;ambientTickCountdown:I",
    // opcode = Opcodes.PUTFIELD))
    // private void redirectedAmbientTickCountdownAssign(World instance, int value) {
    // // NOOP
    // }

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

    @Shadow
    @Synchronize(on = "this")
    public abstract boolean updateLightByType(EnumSkyBlock p_147463_1_, int p_147463_2_, int p_147463_3_,
        int p_147463_4_);
}
