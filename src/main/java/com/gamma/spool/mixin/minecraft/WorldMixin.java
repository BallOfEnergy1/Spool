package com.gamma.spool.mixin.minecraft;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraftforge.common.ForgeModContainer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gamma.spool.Spool;
import com.gamma.spool.config.ThreadManagerConfig;
import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.mixin.MinecraftLambdaOptimizedTasks;
import com.gamma.spool.thread.ManagerNames;
import com.gamma.spool.util.distance.DistanceThreadingExecutors;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

@Mixin(World.class)
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
    protected List<IWorldAccess> worldAccesses;

    @SideOnly(Side.CLIENT)
    @Inject(
        method = "<init>(Lnet/minecraft/world/storage/ISaveHandler;Ljava/lang/String;Lnet/minecraft/world/WorldProvider;Lnet/minecraft/world/WorldSettings;Lnet/minecraft/profiler/Profiler;)V",
        at = @At("RETURN"))
    private void onInit(ISaveHandler p_i45368_1_, String p_i45368_2_, WorldProvider p_i45368_3_,
        WorldSettings p_i45368_4_, Profiler p_i45368_5_, CallbackInfo ci) {

        loadedEntityList = ObjectLists.synchronize(new ObjectArrayList<>());
        unloadedEntityList = ObjectLists.synchronize(new ObjectArrayList<>());
        loadedTileEntityList = ObjectLists.synchronize(new ObjectArrayList<>());
        addedTileEntityList = ObjectLists.synchronize(new ObjectArrayList<>());
        field_147483_b = ObjectLists.synchronize(new ObjectArrayList<>());
        playerEntities = ObjectLists.synchronize(new ObjectArrayList<>());
        weatherEffects = ObjectLists.synchronize(new ObjectArrayList<>());
        worldAccesses = ObjectLists.synchronize(new ObjectArrayList<>());
    }

    @Inject(
        method = "<init>(Lnet/minecraft/world/storage/ISaveHandler;Ljava/lang/String;Lnet/minecraft/world/WorldSettings;Lnet/minecraft/world/WorldProvider;Lnet/minecraft/profiler/Profiler;)V",
        at = @At("RETURN"))
    private void onInit(ISaveHandler p_i45369_1_, String p_i45369_2_, WorldSettings p_i45369_3_,
        WorldProvider p_i45369_4_, Profiler p_i45369_5_, CallbackInfo ci) {

        loadedEntityList = ObjectLists.synchronize(new ObjectArrayList<>());
        unloadedEntityList = ObjectLists.synchronize(new ObjectArrayList<>());
        loadedTileEntityList = ObjectLists.synchronize(new ObjectArrayList<>());
        addedTileEntityList = ObjectLists.synchronize(new ObjectArrayList<>());
        field_147483_b = ObjectLists.synchronize(new ObjectArrayList<>());
        playerEntities = ObjectLists.synchronize(new ObjectArrayList<>());
        weatherEffects = ObjectLists.synchronize(new ObjectArrayList<>());
        worldAccesses = ObjectLists.synchronize(new ObjectArrayList<>());
    }

    /**
     * @author BallOfEnergy
     * @reason Ensure that the colliding bounding boxes aren't conflicting.
     */
    @Overwrite
    public List<AxisAlignedBB> getCollidingBoundingBoxes(Entity p_72945_1_, AxisAlignedBB p_72945_2_) {
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

        return colliding;
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

    @Shadow
    private boolean field_147481_N;

    @Shadow
    public boolean isRemote;

    @Shadow
    public abstract Chunk getChunkFromChunkCoords(int p_72964_1_, int p_72964_2_);

    @Shadow
    public abstract void func_147453_f(int x, int yPos, int z, Block blockIn);

    @Shadow
    public abstract Block getBlock(int p_147439_1_, int p_147439_2_, int p_147439_3_);

    @Invoker("chunkExists")
    public abstract boolean invokeChunkExists(int x, int z);

    @Unique
    private World spool$instance;

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

        int i;
        Entity entity;
        CrashReport crashreport;
        CrashReportCategory crashreportcategory;

        synchronized (weatherEffects) {
            for (i = 0; i < spool$instance.weatherEffects.size(); i++) {
                entity = spool$instance.weatherEffects.get(i);

                try {
                    ++entity.ticksExisted;
                    // No lambda optimization needed here, already method reference!
                    if (!this.isRemote && ThreadsConfig.isExperimentalThreadingEnabled())
                        Spool.REGISTERED_THREAD_MANAGERS.get(ManagerNames.ENTITY)
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
                    spool$instance.weatherEffects.remove(i--);
                }
            }
        }

        spool$instance.theProfiler.endStartSection("remove");
        spool$instance.loadedEntityList.removeAll(new ReferenceOpenHashSet<>(this.unloadedEntityList)); // Hodgepodge
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
            }
        }

        synchronized (unloadedEntityList) { // TODO: Consider compacting this?
            for (i = 0; i < this.unloadedEntityList.size(); i++) {
                spool$instance.onEntityRemoved(this.unloadedEntityList.get(i));
            }
        }

        this.unloadedEntityList.clear();
        spool$instance.theProfiler.endStartSection("regular");

        synchronized (spool$instance.loadedEntityList) {
            for (i = 0; i < spool$instance.loadedEntityList.size(); i++) {
                entity = spool$instance.loadedEntityList.get(i);

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
                                Spool.REGISTERED_THREAD_MANAGERS.get(ManagerNames.ENTITY)
                                    .execute(this::spool$entityTask, entity);
                            else if (!this.isRemote && ThreadsConfig.isDistanceThreadingEnabled())
                                DistanceThreadingExecutors.execute(entity, this::spool$entityTask, entity);
                            else spool$entityTask(entity);
                        } else {
                            final Entity finalEntity1 = entity;
                            if (!this.isRemote && ThreadsConfig.isExperimentalThreadingEnabled())
                                Spool.REGISTERED_THREAD_MANAGERS.get(ManagerNames.ENTITY)
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
                    j = entity.chunkCoordX;
                    l = entity.chunkCoordZ;

                    if (entity.addedToChunk && this.invokeChunkExists(j, l)) {
                        spool$instance.getChunkFromChunkCoords(j, l)
                            .removeEntity(entity);
                    }

                    spool$instance.loadedEntityList.remove(i--);
                    spool$instance.onEntityRemoved(entity);
                }
                spool$instance.theProfiler.endSection();
            }
        }

        spool$instance.theProfiler.endStartSection("blockEntities");
        this.field_147481_N = true;
        Iterator<TileEntity> iterator = spool$instance.loadedTileEntityList.iterator();

        synchronized (spool$instance.loadedTileEntityList) {
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

        if (!this.field_147483_b.isEmpty()) {
            synchronized (field_147483_b) {
                for (TileEntity tile : this.field_147483_b) {
                    tile.onChunkUnload();
                }
            }

            spool$instance.loadedTileEntityList.removeAll(new ReferenceOpenHashSet<>(this.field_147483_b)); // Hodgepodge
            this.field_147483_b.clear();
        }

        this.field_147481_N = false;

        spool$instance.theProfiler.endStartSection("pendingBlockEntities");

        if (!this.addedTileEntityList.isEmpty()) {
            synchronized (addedTileEntityList) {
                for (TileEntity tileentity1 : this.addedTileEntityList) {
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
            }

            this.addedTileEntityList.clear();
        }

        spool$instance.theProfiler.endSection();
        spool$instance.theProfiler.endSection();
    }

    @WrapMethod(method = "setTileEntity")
    private void spool$setTileEntity(int x, int y, int z, TileEntity tileEntityIn, Operation<Void> original) {
        synchronized (addedTileEntityList) {
            original.call(x, y, z, tileEntityIn);
        }
    }

    @WrapMethod(method = "getTileEntity")
    private TileEntity getTileEntity(int x, int y, int z, Operation<TileEntity> original) {
        synchronized (addedTileEntityList) {
            return original.call(x, y, z);
        }
    }

    /**
     * @author BallOfEnergy
     * @reason Replaced to ensure concurrency.
     */
    @Overwrite
    @SideOnly(Side.CLIENT)
    public List<Entity> getLoadedEntityList() {
        return new ObjectArrayList<>(this.loadedEntityList);
    }

    @WrapMethod(method = "countEntities(Ljava/lang/Class;)I")
    private int countEntities(Class<? extends Entity> p_72907_1_, Operation<Integer> original) {
        synchronized (loadedTileEntityList) {
            return original.call(p_72907_1_);
        }
    }

    @WrapMethod(method = "countEntities(Lnet/minecraft/entity/EnumCreatureType;Z)I", remap = false)
    private int countEntities(EnumCreatureType type, boolean forSpawnCount, Operation<Integer> original) {
        synchronized (loadedTileEntityList) {
            return original.call(type, forSpawnCount);
        }
    }

    @WrapMethod(method = "markBlockForUpdate")
    private void markBlockForUpdate(int p_147471_1_, int p_147471_2_, int p_147471_3_, Operation<Void> original) {
        synchronized (worldAccesses) {
            original.call(p_147471_1_, p_147471_2_, p_147471_3_);
        }
    }

    @WrapMethod(method = "markBlockRangeForRenderUpdate")
    private void markBlockRangeForRenderUpdate(int p_147458_1_, int p_147458_2_, int p_147458_3_, int p_147458_4_,
        int p_147458_5_, int p_147458_6_, Operation<Void> original) {
        synchronized (worldAccesses) {
            original.call(p_147458_1_, p_147458_2_, p_147458_3_, p_147458_4_, p_147458_5_, p_147458_6_);
        }
    }

    @WrapMethod(method = "setLightValue")
    private void setLightValue(EnumSkyBlock p_72915_1_, int p_72915_2_, int p_72915_3_, int p_72915_4_, int p_72915_5_,
        Operation<Void> original) {
        synchronized (worldAccesses) {
            original.call(p_72915_1_, p_72915_2_, p_72915_3_, p_72915_4_, p_72915_5_);
        }
    }

    @WrapMethod(method = "func_147479_m")
    private void setLightValue(int p_147479_1_, int p_147479_2_, int p_147479_3_, Operation<Void> original) {
        synchronized (worldAccesses) {
            original.call(p_147479_1_, p_147479_2_, p_147479_3_);
        }
    }

    @WrapMethod(method = "playSoundAtEntity")
    private void playSoundAtEntity(Entity p_72956_1_, String p_72956_2_, float p_72956_3_, float p_72956_4_,
        Operation<Void> original) {
        synchronized (worldAccesses) {
            original.call(p_72956_1_, p_72956_2_, p_72956_3_, p_72956_4_);
        }
    }

    @WrapMethod(method = "playSoundToNearExcept")
    private void playSoundToNearExcept(EntityPlayer p_85173_1_, String p_85173_2_, float p_85173_3_, float p_85173_4_,
        Operation<Void> original) {
        synchronized (worldAccesses) {
            original.call(p_85173_1_, p_85173_2_, p_85173_3_, p_85173_4_);
        }
    }

    @WrapMethod(method = "playSoundEffect")
    private void playSoundEffect(double x, double y, double z, String soundName, float volume, float pitch,
        Operation<Void> original) {
        synchronized (worldAccesses) {
            original.call(x, y, z, soundName, volume, pitch);
        }
    }

    @WrapMethod(method = "playRecord")
    private void playRecord(String recordName, int x, int y, int z, Operation<Void> original) {
        synchronized (worldAccesses) {
            original.call(recordName, x, y, z);
        }
    }

    @WrapMethod(method = "spawnParticle")
    private void spawnParticle(String particleName, double x, double y, double z, double velocityX, double velocityY,
        double velocityZ, Operation<Void> original) {
        synchronized (worldAccesses) {
            original.call(particleName, x, y, z, velocityX, velocityY, velocityZ);
        }
    }

    @WrapMethod(method = "onEntityAdded")
    private void onEntityAdded(Entity p_72923_1_, Operation<Void> original) {
        synchronized (worldAccesses) {
            original.call(p_72923_1_);
        }
    }

    @WrapMethod(method = "onEntityRemoved")
    private void onEntityRemoved(Entity p_72923_1_, Operation<Void> original) {
        synchronized (worldAccesses) {
            original.call(p_72923_1_);
        }
    }

    @WrapMethod(method = "playBroadcastSound")
    private void playBroadcastSound(int p_82739_1_, int p_82739_2_, int p_82739_3_, int p_82739_4_, int p_82739_5_,
        Operation<Void> original) {
        synchronized (worldAccesses) {
            original.call(p_82739_1_, p_82739_2_, p_82739_3_, p_82739_4_, p_82739_5_);
        }
    }

    @WrapMethod(method = "playAuxSFXAtEntity")
    private void playAuxSFXAtEntity(EntityPlayer player, int p_72889_2_, int x, int y, int z, int p_72889_6_,
        Operation<Void> original) {
        synchronized (worldAccesses) {
            original.call(player, p_72889_2_, x, y, z, p_72889_6_);
        }
    }

    @WrapMethod(method = "destroyBlockInWorldPartially")
    private void destroyBlockInWorldPartially(int p_147443_1_, int x, int y, int z, int blockDamage,
        Operation<Void> original) {
        synchronized (worldAccesses) {
            original.call(p_147443_1_, x, y, z, blockDamage);
        }
    }

    @WrapMethod(method = "func_147450_X")
    private void func_147450_X(Operation<Void> original) {
        synchronized (worldAccesses) {
            original.call();
        }
    }

    @WrapMethod(method = "func_152378_a")
    private EntityPlayer func_152378_a(UUID uuid, Operation<EntityPlayer> original) {
        synchronized (playerEntities) {
            return original.call(uuid);
        }
    }

    @WrapMethod(method = "getPlayerEntityByName")
    private EntityPlayer getPlayerEntityByName(String name, Operation<EntityPlayer> original) {
        synchronized (playerEntities) {
            return original.call(name);
        }
    }

    @WrapMethod(method = "getClosestVulnerablePlayer")
    private EntityPlayer getClosestVulnerablePlayer(double p_72846_1_, double p_72846_3_, double p_72846_5_,
        double p_72846_7_, Operation<EntityPlayer> original) {
        synchronized (playerEntities) {
            return original.call(p_72846_1_, p_72846_3_, p_72846_5_, p_72846_7_);
        }
    }

    @WrapMethod(method = "setActivePlayerChunksAndCheckLight")
    private void setActivePlayerChunksAndCheckLight(Operation<Void> original) {
        synchronized (playerEntities) {
            original.call();
        }
    }
}
