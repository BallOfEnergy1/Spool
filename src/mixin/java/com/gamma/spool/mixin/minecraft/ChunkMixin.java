package com.gamma.spool.mixin.minecraft;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gamma.gammalib.util.concurrent.IThreadSafe;
import com.gamma.spool.core.SpoolCompat;
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

@Mixin(Chunk.class)
public abstract class ChunkMixin implements IThreadSafe {

    @Shadow
    public List<Entity>[] entityLists;

    @Shadow
    public Map<ChunkPosition, TileEntity> chunkTileEntityMap;

    @Shadow
    public int[] heightMap;

    @Shadow
    public ExtendedBlockStorage[] storageArrays;

    // Because EID is amazing, they set `blockBiomeArray` to `null` in favor of their own.
    // We can just use a sync object instead.
    @Unique
    private final Object spool$biomeSync = new Object();

    @Inject(method = "<init>(Lnet/minecraft/world/World;II)V", at = @At("RETURN"))
    public void onInit(CallbackInfo ci) {
        for (int k = 0; k < this.entityLists.length; ++k) {
            this.entityLists[k] = ObjectLists.synchronize(new ObjectArrayList<>());
        }
        if (SpoolCompat.isModLoadedFast(SpoolCompat.CompatibleMods.ANGELICA) && AngelicaConfig.enableCeleritas)
            this.chunkTileEntityMap = new ConcurrentTileEntityMap();
        else this.chunkTileEntityMap = new ConcurrentHashMap<>();
    }

    @WrapMethod(method = "generateSkylightMap")
    private void wrappedGenerateSkylightMap(Operation<Void> original) {
        synchronized (this.heightMap) {
            original.call();
        }
    }

    @Definition(
        id = "storageArrays",
        field = "Lnet/minecraft/world/chunk/Chunk;storageArrays:[Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;")
    @Definition(id = "i1", local = @Local(type = int.class, name = "i1"))
    @Expression("this.storageArrays[i1 >> 4]")
    @WrapOperation(method = "generateSkylightMap", at = @At("MIXINEXTRAS:EXPRESSION"))
    private ExtendedBlockStorage wrappedGenerateSkylightMapInner(ExtendedBlockStorage[] array, int index,
        Operation<ExtendedBlockStorage> original) {
        synchronized (this.storageArrays) {
            return original.call(array, index);
        }
    }

    // Insanely expensive call that ***needs*** to be profiled further.
    @WrapMethod(method = "func_150807_a")
    private boolean wrappedFunc_150807_a(int p_150807_1_, int p_150807_2_, int p_150807_3_, Block p_150807_4_,
        int p_150807_5_, Operation<Boolean> original) {
        synchronized (this.heightMap) {
            synchronized (this.storageArrays) {
                return original.call(p_150807_1_, p_150807_2_, p_150807_3_, p_150807_4_, p_150807_5_);
            }
        }
    }

    @WrapMethod(method = "getEntitiesWithinAABBForEntity")
    private void wrappedGetEntitiesWithinAABBForEntity(Entity p_76588_1_, AxisAlignedBB p_76588_2_,
        List<Entity> p_76588_3_, IEntitySelector p_76588_4_, Operation<Void> original) {
        synchronized (this.entityLists) {
            original.call(p_76588_1_, p_76588_2_, p_76588_3_, p_76588_4_);
        }
    }

    @WrapMethod(method = "getHeightValue")
    public int wrappedGetHeightValue(int x, int z, Operation<Integer> original) {
        synchronized (this.heightMap) {
            return original.call(x, z);
        }
    }

    @WrapMethod(method = "getTopFilledSegment")
    public int wrappedGetTopFilledSegment(Operation<Integer> original) {
        synchronized (this.storageArrays) {
            return original.call();
        }
    }

    // this also might really need to be profiled.
    @WrapMethod(method = "relightBlock")
    private void wrappedRelightBlock(int p_76615_1_, int p_76615_2_, int p_76615_3_, Operation<Void> original) {
        synchronized (this.heightMap) {
            original.call(p_76615_1_, p_76615_2_, p_76615_3_);
        }
    }

    @Definition(
        id = "storageArrays",
        field = "Lnet/minecraft/world/chunk/Chunk;storageArrays:[Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;")
    @Expression("this.storageArrays[?]")
    @WrapOperation(method = "relightBlock", at = @At("MIXINEXTRAS:EXPRESSION"))
    private ExtendedBlockStorage wrappedRelightBlockInner(ExtendedBlockStorage[] array, int index,
        Operation<ExtendedBlockStorage> original) {
        synchronized (this.storageArrays) {
            return original.call(array, index);
        }
    }

    @WrapMethod(method = "getBlock")
    public Block wrappedGetBlock(int p_150810_1_, int p_150810_2_, int p_150810_3_, Operation<Block> original) {
        synchronized (this.storageArrays) {
            return original.call(p_150810_1_, p_150810_2_, p_150810_3_);
        }
    }

    @WrapMethod(method = "getBlockMetadata")
    public int wrappedGetBlockMetadata(int p_76628_1_, int p_76628_2_, int p_76628_3_, Operation<Integer> original) {
        synchronized (this.storageArrays) {
            return original.call(p_76628_1_, p_76628_2_, p_76628_3_);
        }
    }

    @WrapMethod(method = "setBlockMetadata")
    public boolean wrappedSetBlockMetadata(int p_76589_1_, int p_76589_2_, int p_76589_3_, int p_76589_4_,
        Operation<Boolean> original) {
        synchronized (this.storageArrays) {
            return original.call(p_76589_1_, p_76589_2_, p_76589_3_, p_76589_4_);
        }
    }

    @WrapMethod(method = "getSavedLightValue")
    public int wrappedGetSavedLightValue(EnumSkyBlock p_76614_1_, int p_76614_2_, int p_76614_3_, int p_76614_4_,
        Operation<Integer> original) {
        synchronized (this.storageArrays) {
            return original.call(p_76614_1_, p_76614_2_, p_76614_3_, p_76614_4_);
        }
    }

    @WrapMethod(method = "setLightValue")
    public void wrappedSetLightValue(EnumSkyBlock p_76633_1_, int p_76633_2_, int p_76633_3_, int p_76633_4_,
        int p_76633_5_, Operation<Void> original) {
        synchronized (this.storageArrays) {
            original.call(p_76633_1_, p_76633_2_, p_76633_3_, p_76633_4_, p_76633_5_);
        }
    }

    @WrapMethod(method = "getBlockLightValue")
    public int wrappedGetBlockLightValue(int p_76629_1_, int p_76629_2_, int p_76629_3_, int p_76629_4_,
        Operation<Integer> original) {
        synchronized (this.storageArrays) {
            return original.call(p_76629_1_, p_76629_2_, p_76629_3_, p_76629_4_);
        }
    }

    @WrapMethod(method = "canBlockSeeTheSky")
    public boolean wrappedCanBlockSeeTheSky(int p_76619_1_, int p_76619_2_, int p_76619_3_,
        Operation<Boolean> original) {
        synchronized (this.heightMap) {
            return original.call(p_76619_1_, p_76619_2_, p_76619_3_);
        }
    }

    @WrapMethod(method = "needsSaving")
    public synchronized boolean wrappedNeedsSaving(boolean p_76601_1_, Operation<Boolean> original) {
        return original.call(p_76601_1_);
    }

    @WrapMethod(method = "populateChunk")
    public synchronized void wrappedPopulateChunk(IChunkProvider p_76624_1_, IChunkProvider p_76624_2_, int p_76624_3_,
        int p_76624_4_, Operation<Void> original) {
        original.call(p_76624_1_, p_76624_2_, p_76624_3_, p_76624_4_);
    }

    @WrapMethod(method = "getPrecipitationHeight")
    public int wrappedGetPrecipitationHeight(int p_76626_1_, int p_76626_2_, Operation<Integer> original) {
        synchronized (this.heightMap) {
            return original.call(p_76626_1_, p_76626_2_);
        }
    }

    @WrapMethod(method = "func_150804_b")
    public synchronized void wrappedFunc_150804_b(boolean p_150804_1_, Operation<Void> original) {
        original.call(p_150804_1_);
    }

    @WrapMethod(method = "func_150802_k")
    public synchronized boolean wrappedFunc_150802_k(Operation<Boolean> original) {
        return original.call();
    }

    @WrapMethod(method = "getAreLevelsEmpty")
    public boolean wrappedGetAreLevelsEmpty(int p_76606_1_, int p_76606_2_, Operation<Boolean> original) {
        synchronized (this.storageArrays) {
            return original.call(p_76606_1_, p_76606_2_);
        }
    }

    @WrapMethod(method = "setStorageArrays")
    public void wrappedSetStorageArrays(ExtendedBlockStorage[] p_76602_1_, Operation<Void> original) {
        synchronized (this.storageArrays) {
            original.call((Object) p_76602_1_);
        }
    }

    @WrapMethod(method = "enqueueRelightChecks")
    public void wrappedEnqueueRelightChecks(Operation<Void> original) {
        synchronized (this.storageArrays) {
            original.call();
        }
    }

    @WrapMethod(method = "func_150809_p")
    public synchronized void wrappedFunc_150809_p(Operation<Void> original) {
        original.call();
    }

    @WrapMethod(method = "getBiomeGenForWorldCoords")
    public BiomeGenBase wrappedGetBiomeGenForWorldCoords(int p_76591_1_, int p_76591_2_, WorldChunkManager p_76591_3_,
        Operation<BiomeGenBase> original) {
        synchronized (this.spool$biomeSync) {
            return original.call(p_76591_1_, p_76591_2_, p_76591_3_);
        }
    }

    @WrapMethod(method = "getBiomeArray")
    public byte[] wrappedGetBiomeArray(Operation<byte[]> original) {
        synchronized (this.spool$biomeSync) {
            return original.call();
        }
    }

    @WrapMethod(method = "getBiomeShortArray", require = 0, remap = false)
    @Dynamic("Created by EID during their ChunkMixin")
    public short[] wrappedGetBiomeShortArray(Operation<short[]> original) {
        synchronized (this.spool$biomeSync) {
            return original.call();
        }
    }
}
