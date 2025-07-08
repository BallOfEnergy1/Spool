package com.gamma.spool.mixin.minecraft;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.block.Block;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeModContainer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gamma.spool.Spool;
import com.gamma.spool.config.ThreadManagerConfig;
import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.mixin.MinecraftLambdaOptimizedTasks;
import com.gamma.spool.thread.ManagerNames;
import com.gamma.spool.util.distance.DistanceThreadingExecutors;

import cpw.mods.fml.common.FMLLog;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

@Mixin(World.class)
public abstract class WorldMixin {

    @Shadow
    public List<EntityPlayer> playerEntities;

    @Inject(method = "<init>*", at = @At("RETURN"))
    public void init(CallbackInfo ci) {
        playerEntities = ObjectLists.synchronize(new ObjectArrayList<>());
    }

    @Inject(method = "getCollidingBoundingBoxes", at = @At(value = "HEAD"), cancellable = true)
    public void getCollidingBoundingBoxes(Entity p_72945_1_, AxisAlignedBB p_72945_2_,
        CallbackInfoReturnable<List<AxisAlignedBB>> cir) {
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
        List list = instance.getEntitiesWithinAABBExcludingEntity(p_72945_1_, p_72945_2_.expand(d0, d0, d0));

        for (int j2 = 0; j2 < list.size(); ++j2) {
            AxisAlignedBB axisalignedbb1 = ((Entity) list.get(j2)).getBoundingBox();

            if (axisalignedbb1 != null && axisalignedbb1.intersectsWith(p_72945_2_)) {
                colliding.add(axisalignedbb1);
            }

            axisalignedbb1 = p_72945_1_.getCollisionBox((Entity) list.get(j2));

            if (axisalignedbb1 != null && axisalignedbb1.intersectsWith(p_72945_2_)) {
                colliding.add(axisalignedbb1);
            }
        }

        cir.setReturnValue(colliding);
    }

    @Inject(method = "func_147461_a", at = @At(value = "HEAD"), cancellable = true)
    public void func_147461_a(AxisAlignedBB p_147461_1_, CallbackInfoReturnable<List<AxisAlignedBB>> cir) {
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

        cir.setReturnValue(colliding);
    }

    @Shadow
    protected List<Entity> unloadedEntityList;

    @Shadow
    private boolean field_147481_N;

    @Shadow
    private List<TileEntity> field_147483_b;

    @Shadow
    private List<TileEntity> addedTileEntityList;

    @Shadow
    public boolean isRemote;

    @Invoker("chunkExists")
    public abstract boolean invokeChunkExists(int x, int z);

    @Unique
    public World spool$instance;

    @Unique
    public void spool$entityTask(Entity entity) {
        MinecraftLambdaOptimizedTasks.entityTask(spool$instance, entity);
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
        AtomicInteger i = new AtomicInteger();
        Entity entity;
        CrashReport crashreport;
        CrashReportCategory crashreportcategory;

        for (i.set(0); i.get() < spool$instance.weatherEffects.size(); i.incrementAndGet()) {
            entity = spool$instance.weatherEffects.get(i.get());

            try {
                ++entity.ticksExisted;
                // No lambda optimization needed here, already method reference!
                if (!this.isRemote && ThreadsConfig.isExperimentalThreadingEnabled())
                    Spool.registeredThreadManagers.get(ManagerNames.ENTITY)
                        .execute(entity::onUpdate);
                else if (!this.isRemote && ThreadsConfig.isDistanceThreadingEnabled())
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
                spool$instance.weatherEffects.remove(i.getAndDecrement());
            }
        }

        spool$instance.theProfiler.endStartSection("remove");
        spool$instance.loadedEntityList.removeAll(new ReferenceOpenHashSet<>(this.unloadedEntityList)); // Hodgepodge
        AtomicInteger j = new AtomicInteger();
        AtomicInteger l = new AtomicInteger();

        for (i.set(0); i.get() < this.unloadedEntityList.size(); i.incrementAndGet()) {
            entity = this.unloadedEntityList.get(i.get());
            j.set(entity.chunkCoordX);
            l.set(entity.chunkCoordZ);

            if (entity.addedToChunk && this.invokeChunkExists(j.get(), l.get())) {
                spool$instance.getChunkFromChunkCoords(j.get(), l.get())
                    .removeEntity(entity);
            }
        }

        for (i.set(0); i.get() < this.unloadedEntityList.size(); i.incrementAndGet()) {
            spool$instance.onEntityRemoved(this.unloadedEntityList.get(i.get()));
        }
        this.unloadedEntityList.clear();
        spool$instance.theProfiler.endStartSection("regular");

        synchronized (spool$instance.loadedEntityList) {
            for (i.set(0); i.get() < spool$instance.loadedEntityList.size(); i.incrementAndGet()) {
                entity = spool$instance.loadedEntityList.get(i.get());

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
                        if (ThreadManagerConfig.useLambdaOptimization) {
                            if (!this.isRemote && ThreadsConfig.isExperimentalThreadingEnabled())
                                Spool.registeredThreadManagers.get(ManagerNames.ENTITY)
                                    .execute(this::spool$entityTask, entity);
                            else if (!this.isRemote && ThreadsConfig.isDistanceThreadingEnabled())
                                DistanceThreadingExecutors.execute(entity, this::spool$entityTask, entity);
                            else spool$entityTask(entity);
                        } else {
                            final Entity finalEntity1 = entity;
                            if (!this.isRemote && ThreadsConfig.isExperimentalThreadingEnabled())
                                Spool.registeredThreadManagers.get(ManagerNames.ENTITY)
                                    .execute(() -> spool$instance.updateEntity(finalEntity1));
                            else if (!this.isRemote && ThreadsConfig.isDistanceThreadingEnabled())
                                DistanceThreadingExecutors
                                    .execute(entity, () -> spool$instance.updateEntity(finalEntity1));
                            else spool$instance.updateEntity(finalEntity1);
                        }
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
                    j.set(entity.chunkCoordX);
                    l.set(entity.chunkCoordZ);

                    if (entity.addedToChunk && this.invokeChunkExists(j.get(), l.get())) {
                        spool$instance.getChunkFromChunkCoords(j.get(), l.get())
                            .removeEntity(entity);
                    }

                    spool$instance.loadedEntityList.remove(i.getAndDecrement());
                    spool$instance.onEntityRemoved(entity);
                }
            }
        }

        spool$instance.theProfiler.endStartSection("blockEntities");
        this.field_147481_N = true;
        Iterator iterator = spool$instance.loadedTileEntityList.iterator();

        while (iterator.hasNext()) {
            TileEntity tileentity = (TileEntity) iterator.next();

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
                        chunk
                            .removeInvalidTileEntity(tileentity.xCoord & 15, tileentity.yCoord, tileentity.zCoord & 15);
                    }
                }
            }
        }

        if (!this.field_147483_b.isEmpty()) {
            for (Object tile : this.field_147483_b) {
                ((TileEntity) tile).onChunkUnload();
            }
            spool$instance.loadedTileEntityList.removeAll(new ReferenceOpenHashSet<>(this.field_147483_b)); // Hodgepodge
            this.field_147483_b.clear();
        }

        this.field_147481_N = false;

        spool$instance.theProfiler.endStartSection("pendingBlockEntities");

        if (!this.addedTileEntityList.isEmpty()) {
            for (int k = 0; k < this.addedTileEntityList.size(); ++k) {
                TileEntity tileentity1 = this.addedTileEntityList.get(k);

                if (!tileentity1.isInvalid()) {
                    if (!spool$instance.loadedTileEntityList.contains(tileentity1)) {
                        spool$instance.loadedTileEntityList.add(tileentity1);
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

        spool$instance.theProfiler.endSection();
        spool$instance.theProfiler.endSection();
    }
}
