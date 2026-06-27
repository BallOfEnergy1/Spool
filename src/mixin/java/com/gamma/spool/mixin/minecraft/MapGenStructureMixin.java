package com.gamma.spool.mixin.minecraft;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.StructureStart;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gamma.spool.util.RWLockedLong2ObjectMap;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

@Mixin(value = MapGenStructure.class, priority = 1001)
public abstract class MapGenStructureMixin {

    @Unique
    protected final ReadWriteLock spool$structureMapLock = new ReentrantReadWriteLock(true);

    @SuppressWarnings("unused")
    @Shadow
    protected Map<Long, StructureStart> structureMap = new RWLockedLong2ObjectMap<>(
        spool$structureMapLock,
        new Long2ObjectOpenHashMap<>());

    @Inject(method = "generateStructuresInChunk", at = @At("HEAD"))
    private void generateStructuresInChunkHead(World p_75051_1_, Random p_75051_2_, int p_75051_3_, int p_75051_4_,
        CallbackInfoReturnable<Boolean> cir) {
        spool$structureMapLock.readLock()
            .lock();
    }

    @Inject(method = "generateStructuresInChunk", at = @At("RETURN"))
    private void generateStructuresInChunkReturn(World p_75051_1_, Random p_75051_2_, int p_75051_3_, int p_75051_4_,
        CallbackInfoReturnable<Boolean> cir) {
        spool$structureMapLock.readLock()
            .unlock();
    }

    @Inject(method = "func_143028_c", at = @At("HEAD"))
    private void func_143028_cHead(int p_143028_1_, int p_143028_2_, int p_143028_3_,
        CallbackInfoReturnable<StructureStart> cir) {
        spool$structureMapLock.readLock()
            .lock();
    }

    @Inject(method = "func_143028_c", at = @At("RETURN"))
    private void func_143028_cReturn(int p_143028_1_, int p_143028_2_, int p_143028_3_,
        CallbackInfoReturnable<StructureStart> cir) {
        spool$structureMapLock.readLock()
            .unlock();
    }

    @Inject(method = "func_142038_b", at = @At("HEAD"))
    private void func_142038_bHead(int p_142038_1_, int p_142038_2_, int p_142038_3_,
        CallbackInfoReturnable<Boolean> cir) {
        spool$structureMapLock.readLock()
            .lock();
    }

    @Inject(method = "func_142038_b", at = @At("RETURN"))
    private void func_142038_bReturn(int p_142038_1_, int p_142038_2_, int p_142038_3_,
        CallbackInfoReturnable<Boolean> cir) {
        spool$structureMapLock.readLock()
            .unlock();
    }

    @Inject(method = "func_151545_a", at = @At("HEAD"))
    private void func_151545_aHead(World p_151545_1_, int p_151545_2_, int p_151545_3_, int p_151545_4_,
        CallbackInfoReturnable<ChunkPosition> cir) {
        spool$structureMapLock.readLock()
            .lock();
    }

    @Inject(method = "func_151545_a", at = @At("RETURN"))
    private void func_151545_aReturn(World p_151545_1_, int p_151545_2_, int p_151545_3_, int p_151545_4_,
        CallbackInfoReturnable<ChunkPosition> cir) {
        spool$structureMapLock.readLock()
            .unlock();
    }
}
