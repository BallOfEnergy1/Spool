package com.gamma.spool.mixin.minecraft.imixins;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.spongepowered.asm.mixin.Dynamic;
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
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

@Mixin(value = Chunk.class, priority = 1001)
public abstract class ChunkMixin_Sync_Supernova implements IThreadSafe, IChunkLockAccessor {

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

    @WrapMethod(method = "generateSkylightMap")
    public void generateSkylightMapHead(Operation<Void> original) {
        this.spool$heightMapLock.writeLock()
            .lock();
        try {
            original.call();
        } finally {
            this.spool$heightMapLock.writeLock()
                .unlock();
        }
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

    @WrapOperation(
        method = "func_150807_a",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;relightBlock(III)V"))
    public void relightBlockWrapped(Chunk instance, int x, int y, int z, Operation<Void> original) {
        this.spool$heightMapLock.writeLock()
            .lock();
        try {
            original.call(instance, x, y, z);
        } finally {
            this.spool$heightMapLock.writeLock()
                .unlock();
        }
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

    @Definition(id = "section", local = @Local(name = "section", type = ExtendedBlockStorage.class))
    @Definition(
        id = "storageArrays",
        field = "Lnet/minecraft/world/chunk/Chunk;storageArrays:[Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;")
    @Definition(id = "y", local = @Local(name = "y", type = int.class))
    @Expression("section = this.storageArrays[y >> 4]")
    @Dynamic("Injected by Supernova")
    @Inject(
        method = "supernova$fillVanillaSkyForColumn",
        at = @At(value = "MIXINEXTRAS:EXPRESSION", shift = At.Shift.BY, by = -3))
    private void supernova$fillVanillaSkyForColumnBefore(CallbackInfo ci) {
        this.spool$storageArraysLock.readLock()
            .lock();
    }

    @Definition(id = "section", local = @Local(name = "section", type = ExtendedBlockStorage.class))
    @Definition(
        id = "storageArrays",
        field = "Lnet/minecraft/world/chunk/Chunk;storageArrays:[Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;")
    @Definition(id = "y", local = @Local(name = "y", type = int.class))
    @Expression("section = this.storageArrays[y >> 4]")
    @Dynamic("Injected by Supernova")
    @Inject(
        method = "supernova$fillVanillaSkyForColumn",
        at = @At(value = "MIXINEXTRAS:EXPRESSION", shift = At.Shift.AFTER))
    private void supernova$fillVanillaSkyForColumnAfter(CallbackInfo ci) {
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
}
