package com.gamma.lmtm.mixin.minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.block.Block;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.init.Blocks;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.IntHashMap;
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

import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gamma.lmtm.LMTM;
import com.gamma.lmtm.util.ConcurrentIntHashMap;
import com.gamma.lmtm.util.ConcurrentTreeSet;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(value = WorldServer.class, priority = 999)
public abstract class WorldServerMixin extends World {

    @Shadow
    @Mutable
    private Set pendingTickListEntriesHashSet;
    @Shadow
    @Mutable
    private TreeSet pendingTickListEntriesTreeSet;
    @Shadow
    @Mutable
    private List pendingTickListEntriesThisTick;
    @Shadow
    @Mutable
    private IntHashMap entityIdMap;
    @Final
    @Shadow
    private static Logger logger;

    public WorldServerMixin(ISaveHandler p_i45368_1_, String p_i45368_2_, WorldProvider p_i45368_3_,
        WorldSettings p_i45368_4_, Profiler p_i45368_5_) {
        super(p_i45368_1_, p_i45368_2_, p_i45368_3_, p_i45368_4_, p_i45368_5_);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        pendingTickListEntriesHashSet = ConcurrentHashMap.newKeySet();
        pendingTickListEntriesTreeSet = new ConcurrentTreeSet<>();
        pendingTickListEntriesThisTick = Collections.synchronizedList(new ArrayList<>());
        loadedEntityList = Collections.synchronizedList(new ArrayList<>());
        entityIdMap = new ConcurrentIntHashMap();
    }

    @Inject(method = "initialize", at = @At("HEAD"))
    private void initialize(CallbackInfo ci) {
        if (pendingTickListEntriesHashSet == null) pendingTickListEntriesHashSet = ConcurrentHashMap.newKeySet();
        if (pendingTickListEntriesTreeSet == null) pendingTickListEntriesTreeSet = new ConcurrentTreeSet<>();
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

                if (provider.canDoLightning(chunk) && this.rand.nextInt(100000) == 0
                    && this.isRaining()
                    && this.isThundering()) {
                    this.updateLCG = this.updateLCG * 3 + 1013904223;
                    i1 = this.updateLCG >> 2;
                    j1 = k + (i1 & 15);
                    k1 = l + (i1 >> 8 & 15);
                    l1 = this.getPrecipitationHeight(j1, k1);

                    if (this.canLightningStrikeAt(j1, l1, k1)) {
                        this.addWeatherEffect(new EntityLightningBolt(this, (double) j1, (double) l1, (double) k1));
                    }
                }

                if (provider.canDoRainSnowIce(chunk) && this.rand.nextInt(16) == 0) {
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

                ExtendedBlockStorage[] aextendedblockstorage = chunk.getBlockStorageArray();
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
                            LMTM.blockManager.execute(blockTask);
                        }
                    }
                }
            };
            LMTM.blockManager.execute(chunkTask);
        }
    }

    @WrapOperation(
        method = "tick()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldServer;tickUpdates(Z)Z"))
    public boolean tickUpdates(WorldServer instance, boolean p_72955_1_, Operation<Boolean> original) {
        synchronized (pendingTickListEntriesThisTick) {
            return original.call(instance, p_72955_1_);
        }
    }

    @Inject(method = "getPendingBlockUpdates", at = @At("INVOKE"), cancellable = true)
    public void getPendingBlockUpdates(Chunk p_72920_1_, boolean p_72920_2_,
        CallbackInfoReturnable<List<NextTickListEntry>> cir) {
        ArrayList arraylist = null;
        ChunkCoordIntPair chunkcoordintpair = p_72920_1_.getChunkCoordIntPair();
        int i = (chunkcoordintpair.chunkXPos << 4) - 2;
        int j = i + 16 + 2;
        int k = (chunkcoordintpair.chunkZPos << 4) - 2;
        int l = k + 16 + 2;

        for (int i1 = 0; i1 < 2; ++i1) {
            if (i1 == 0) {
                Iterator iterator = this.pendingTickListEntriesTreeSet.iterator();
                synchronized (this.pendingTickListEntriesTreeSet) {
                    while (iterator.hasNext()) {
                        NextTickListEntry nextticklistentry = (NextTickListEntry) iterator.next();

                        if (nextticklistentry.xCoord >= i && nextticklistentry.xCoord < j
                            && nextticklistentry.zCoord >= k
                            && nextticklistentry.zCoord < l) {
                            if (p_72920_2_) {
                                this.pendingTickListEntriesHashSet.remove(nextticklistentry);
                                iterator.remove();
                            }

                            if (arraylist == null) {
                                arraylist = new ArrayList();
                            }

                            arraylist.add(nextticklistentry);
                        }
                    }
                }
            } else {
                Iterator iterator = this.pendingTickListEntriesThisTick.iterator();

                if (!this.pendingTickListEntriesThisTick.isEmpty()) {
                    logger.debug("toBeTicked = " + this.pendingTickListEntriesThisTick.size());
                }

                synchronized (Collections.unmodifiableList(this.pendingTickListEntriesThisTick)) {
                    while (iterator.hasNext()) {
                        NextTickListEntry nextticklistentry = (NextTickListEntry) iterator.next();

                        if (nextticklistentry.xCoord >= i && nextticklistentry.xCoord < j
                            && nextticklistentry.zCoord >= k
                            && nextticklistentry.zCoord < l) {
                            if (p_72920_2_) {
                                this.pendingTickListEntriesHashSet.remove(nextticklistentry);
                                iterator.remove();
                            }

                            if (arraylist == null) {
                                arraylist = new ArrayList();
                            }

                            arraylist.add(nextticklistentry);
                        }
                    }
                }
            }
        }

        cir.setReturnValue(arraylist);
    }
}
