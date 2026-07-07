package com.gamma.spool.mixin.minecraft;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gamma.gammalib.util.concurrent.IThreadSafe;
import com.gamma.spool.api.annotations.Synchronize;
import com.gamma.spool.async.EntityLoadingAsync;
import com.gamma.spool.core.SpoolCompat;
import com.gtnewhorizons.angelica.config.AngelicaConfig;
import com.gtnewhorizons.angelica.utils.ConcurrentTileEntityMap;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

@Mixin(value = Chunk.class, priority = 1001)
public abstract class ChunkMixin implements IThreadSafe {

    @Shadow
    public List<Entity>[] entityLists;

    @Shadow
    public Map<ChunkPosition, TileEntity> chunkTileEntityMap;

    @Shadow
    @Final
    public int xPosition;

    @Shadow
    @Final
    public int zPosition;

    @Shadow
    public World worldObj;

    @Inject(method = "<init>(Lnet/minecraft/world/World;II)V", at = @At("RETURN"))
    public void onInit(CallbackInfo ci) {
        for (int k = 0; k < this.entityLists.length; ++k) {
            this.entityLists[k] = ObjectLists.synchronize(new ObjectArrayList<>());
        }
        if (SpoolCompat.isModLoadedFast(SpoolCompat.CompatibleMods.ANGELICA) && AngelicaConfig.enableCeleritas)
            this.chunkTileEntityMap = new ConcurrentTileEntityMap();
        else this.chunkTileEntityMap = new ConcurrentHashMap<>();
    }

    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void injectedAddEntity(Entity p_76612_1_, CallbackInfo ci) {
        int x = MathHelper.floor_double(p_76612_1_.posX / 16.0D);
        int z = MathHelper.floor_double(p_76612_1_.posZ / 16.0D);
        if (x != this.xPosition || z != this.zPosition) {
            IChunkProvider provider = this.worldObj.getChunkProvider();
            if (provider.chunkExists(x, z)) provider.provideChunk(x, z)
                .addEntity(p_76612_1_);
            else EntityLoadingAsync.addPendingEntity(p_76612_1_, x, z);
            ci.cancel();
        }
    }

    @Unique
    @Synchronize(
        on = { "Lnet/minecraft/world/chunk/Chunk;heightMap:[I",
            "Lnet/minecraft/world/chunk/Chunk;precipitationHeightMap:[I", })
    public abstract boolean func_150807_a(int p_150807_1_, int p_150807_2_, int p_150807_3_, Block p_150807_4_,
        int p_150807_5_);

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Unique
    @Synchronize(
        on = { "Lnet/minecraft/world/chunk/Chunk;heightMap:[I",
            "Lnet/minecraft/world/chunk/Chunk;precipitationHeightMap:[I" })
    public abstract void generateSkylightMap();

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Unique
    @Synchronize(on = "Lnet/minecraft/world/chunk/Chunk;heightMap:[I")
    public abstract int getHeightValue(int x, int z);

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Unique
    @Synchronize(on = "Lnet/minecraft/world/chunk/Chunk;heightMap:[I")
    public abstract boolean canBlockSeeTheSky(int x, int y, int z);

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Unique
    @Synchronize(on = "Lnet/minecraft/world/chunk/Chunk;precipitationHeightMap:[I")
    public abstract int getPrecipitationHeight(int x, int y);
}
