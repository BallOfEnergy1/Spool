package com.gamma.spool.mixin.minecraft.imixins;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gamma.gammalib.util.concurrent.IThreadSafe;
import com.gamma.spool.core.SpoolCompat;
import com.gamma.spool.util.IChunkLockAccessor;
import com.gamma.spool.util.SidedLock;
import com.gtnewhorizons.angelica.config.AngelicaConfig;
import com.gtnewhorizons.angelica.utils.ConcurrentTileEntityMap;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

@Mixin(value = Chunk.class, priority = 1001)
public abstract class ChunkMixin_Sync implements IThreadSafe, IChunkLockAccessor {

    @Shadow
    public List<Entity>[] entityLists;

    @Shadow
    public Map<ChunkPosition, TileEntity> chunkTileEntityMap;

    @Shadow
    public World worldObj;

    @Unique
    private ReadWriteLock spool$heightMapLock;
    @Unique
    private ReadWriteLock spool$storageArraysLock;

    // interface
    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public ReadWriteLock getStorageArraysLock() {
        return spool$storageArraysLock;
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;II)V", at = @At("RETURN"))
    public void onInit(CallbackInfo ci) {
        for (int k = 0; k < this.entityLists.length; ++k) {
            this.entityLists[k] = ObjectLists.synchronize(new ObjectArrayList<>());
        }
        if (SpoolCompat.isModLoadedFast(SpoolCompat.CompatibleMods.ANGELICA) && AngelicaConfig.enableCeleritas)
            this.chunkTileEntityMap = new ConcurrentTileEntityMap();
        else this.chunkTileEntityMap = new ConcurrentHashMap<>();
        spool$heightMapLock = new SidedLock(worldObj);
        spool$storageArraysLock = new SidedLock(worldObj);
    }

    @Inject(method = "generateSkylightMap", at = @At("HEAD"))
    public void generateSkylightMapHead(CallbackInfo ci) {
        this.spool$storageArraysLock.readLock()
            .lock();
        this.spool$heightMapLock.writeLock()
            .lock();
    }

    @Inject(method = "generateSkylightMap", at = @At("RETURN"))
    public void generateSkylightMapReturn(CallbackInfo ci) {
        this.spool$heightMapLock.writeLock()
            .unlock();
        this.spool$storageArraysLock.readLock()
            .unlock();
    }

    @Inject(method = "func_150807_a", at = @At("HEAD"))
    public void func_150807_aHead(int p_150807_1_, int p_150807_2_, int p_150807_3_, Block p_150807_4_, int p_150807_5_,
        CallbackInfoReturnable<Boolean> cir) {
        this.spool$storageArraysLock.writeLock()
            .lock();
        this.spool$heightMapLock.writeLock()
            .lock();
    }

    @Inject(method = "func_150807_a", at = @At("RETURN"))
    public void func_150807_aReturn(int p_150807_1_, int p_150807_2_, int p_150807_3_, Block p_150807_4_,
        int p_150807_5_, CallbackInfoReturnable<Boolean> cir) {
        this.spool$heightMapLock.writeLock()
            .unlock();
        this.spool$storageArraysLock.writeLock()
            .unlock();
    }

    @Inject(method = "getHeightValue", at = @At("HEAD"))
    public void getHeightValueHead(int x, int z, CallbackInfoReturnable<Integer> cir) {
        this.spool$heightMapLock.readLock()
            .lock();
    }

    @Inject(method = "getHeightValue", at = @At("RETURN"))
    public void getHeightValueReturn(int x, int z, CallbackInfoReturnable<Integer> cir) {
        this.spool$heightMapLock.readLock()
            .unlock();
    }

    @Inject(method = "getTopFilledSegment", at = @At("HEAD"))
    public void getTopFilledSegmentHead(CallbackInfoReturnable<Integer> cir) {
        this.spool$storageArraysLock.readLock()
            .lock();
    }

    @Inject(method = "getTopFilledSegment", at = @At("RETURN"))
    public void getTopFilledSegmentReturn(CallbackInfoReturnable<Integer> cir) {
        this.spool$storageArraysLock.readLock()
            .unlock();
    }

    @Inject(method = "relightBlock", at = @At("HEAD"))
    public void relightBlockHead(int p_76615_1_, int p_76615_2_, int p_76615_3_, CallbackInfo ci) {
        this.spool$storageArraysLock.readLock()
            .lock();
        this.spool$heightMapLock.writeLock()
            .lock();
    }

    @Inject(method = "relightBlock", at = @At("RETURN"))
    public void relightBlockReturn(int p_76615_1_, int p_76615_2_, int p_76615_3_, CallbackInfo ci) {
        this.spool$heightMapLock.writeLock()
            .unlock();
        this.spool$storageArraysLock.readLock()
            .unlock();
    }

    @Inject(method = "getBlock", at = @At("HEAD"))
    public void getBlockHead(int p_150810_1_, int p_150810_2_, int p_150810_3_, CallbackInfoReturnable<Block> cir) {
        this.spool$storageArraysLock.readLock()
            .lock();
    }

    @Inject(method = "getBlock", at = @At("RETURN"))
    public void getBlockReturn(int p_150810_1_, int p_150810_2_, int p_150810_3_, CallbackInfoReturnable<Block> cir) {
        this.spool$storageArraysLock.readLock()
            .unlock();
    }

    @Inject(method = "getBlockMetadata", at = @At("HEAD"))
    public void getBlockMetadataHead(int p_150810_1_, int p_150810_2_, int p_150810_3_,
        CallbackInfoReturnable<Block> cir) {
        this.spool$storageArraysLock.readLock()
            .lock();
    }

    @Inject(method = "getBlockMetadata", at = @At("RETURN"))
    public void getBlockMetadataReturn(int p_150810_1_, int p_150810_2_, int p_150810_3_,
        CallbackInfoReturnable<Block> cir) {
        this.spool$storageArraysLock.readLock()
            .unlock();
    }

    @Inject(method = "setBlockMetadata", at = @At("HEAD"))
    public void setBlockMetadataHead(int p_76589_1_, int p_76589_2_, int p_76589_3_, int p_76589_4_,
        CallbackInfoReturnable<Boolean> cir) {
        this.spool$storageArraysLock.writeLock()
            .lock();
    }

    @Inject(method = "setBlockMetadata", at = @At("RETURN"))
    public void setBlockMetadataReturn(int p_76589_1_, int p_76589_2_, int p_76589_3_, int p_76589_4_,
        CallbackInfoReturnable<Boolean> cir) {
        this.spool$storageArraysLock.writeLock()
            .unlock();
    }

    @Inject(method = "getSavedLightValue", at = @At("HEAD"))
    public void getSavedLightValueHead(EnumSkyBlock p_76614_1_, int p_76614_2_, int p_76614_3_, int p_76614_4_,
        CallbackInfoReturnable<Integer> cir) {
        this.spool$storageArraysLock.readLock()
            .lock();
    }

    @Inject(method = "getSavedLightValue", at = @At("RETURN"))
    public void getSavedLightValueReturn(EnumSkyBlock p_76614_1_, int p_76614_2_, int p_76614_3_, int p_76614_4_,
        CallbackInfoReturnable<Integer> cir) {
        this.spool$storageArraysLock.readLock()
            .unlock();
    }

    @Inject(method = "setLightValue", at = @At("HEAD"))
    public void setLightValueHead(EnumSkyBlock p_76633_1_, int p_76633_2_, int p_76633_3_, int p_76633_4_,
        int p_76633_5_, CallbackInfo ci) {
        this.spool$storageArraysLock.writeLock()
            .lock();
    }

    @Inject(method = "setLightValue", at = @At("RETURN"))
    public void setLightValueReturn(EnumSkyBlock p_76633_1_, int p_76633_2_, int p_76633_3_, int p_76633_4_,
        int p_76633_5_, CallbackInfo ci) {
        this.spool$storageArraysLock.writeLock()
            .unlock();
    }

    @Inject(method = "getBlockLightValue", at = @At("HEAD"))
    public void getBlockLightValueHead(int p_76629_1_, int p_76629_2_, int p_76629_3_, int p_76629_4_,
        CallbackInfoReturnable<Integer> cir) {
        this.spool$storageArraysLock.readLock()
            .lock();
    }

    @Inject(method = "getBlockLightValue", at = @At("RETURN"))
    public void getBlockLightValueReturn(int p_76629_1_, int p_76629_2_, int p_76629_3_, int p_76629_4_,
        CallbackInfoReturnable<Integer> cir) {
        this.spool$storageArraysLock.readLock()
            .unlock();
    }

    @Inject(method = "canBlockSeeTheSky", at = @At("HEAD"))
    public void canBlockSeeTheSkyHead(int p_76619_1_, int p_76619_2_, int p_76619_3_,
        CallbackInfoReturnable<Boolean> cir) {
        this.spool$heightMapLock.readLock()
            .lock();
    }

    @Inject(method = "canBlockSeeTheSky", at = @At("RETURN"))
    public void canBlockSeeTheSkyReturn(int p_76619_1_, int p_76619_2_, int p_76619_3_,
        CallbackInfoReturnable<Boolean> cir) {
        this.spool$heightMapLock.readLock()
            .unlock();
    }

    @Inject(method = "getPrecipitationHeight", at = @At("HEAD"))
    public void getPrecipitationHeightHead(int p_76626_1_, int p_76626_2_, CallbackInfoReturnable<Integer> cir) {
        this.spool$heightMapLock.writeLock()
            .lock();
    }

    @Inject(method = "getPrecipitationHeight", at = @At("RETURN"))
    public void getPrecipitationHeightReturn(int p_76626_1_, int p_76626_2_, CallbackInfoReturnable<Integer> cir) {
        this.spool$heightMapLock.writeLock()
            .unlock();
    }

    @Inject(method = "getAreLevelsEmpty", at = @At("HEAD"))
    public void getAreLevelsEmptyHead(int p_76626_1_, int p_76626_2_, CallbackInfoReturnable<Integer> cir) {
        this.spool$storageArraysLock.readLock()
            .lock();
    }

    @Inject(method = "getAreLevelsEmpty", at = @At("RETURN"))
    public void getAreLevelsEmptyReturn(int p_76626_1_, int p_76626_2_, CallbackInfoReturnable<Integer> cir) {
        this.spool$storageArraysLock.readLock()
            .unlock();
    }

    @Inject(method = "setStorageArrays", at = @At("HEAD"))
    public void setStorageArraysHead(ExtendedBlockStorage[] p_76602_1_, CallbackInfo ci) {
        this.spool$storageArraysLock.writeLock()
            .lock();
    }

    @Inject(method = "setStorageArrays", at = @At("RETURN"))
    public void setStorageArraysReturn(ExtendedBlockStorage[] p_76602_1_, CallbackInfo ci) {
        this.spool$storageArraysLock.writeLock()
            .unlock();
    }

    @Inject(method = "enqueueRelightChecks", at = @At("HEAD"))
    public void enqueueRelightChecksHead(CallbackInfo ci) {
        this.spool$storageArraysLock.readLock()
            .lock();
    }

    @Inject(method = "enqueueRelightChecks", at = @At("RETURN"))
    public void enqueueRelightChecksReturn(CallbackInfo ci) {
        this.spool$storageArraysLock.readLock()
            .unlock();
    }
}
