package com.gamma.spool.mixin.minecraft;

import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.StructureStart;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(value = MapGenStructure.class, priority = 1001)
public abstract class MapGenStructureMixin {

    // Until hodgepodge fixes their stuff (accepts my pr lmao), this has to be unused.
    // @Unique
    // protected final ReadWriteLock spool$structureMapLock = new ReentrantReadWriteLock(true);
    // @SuppressWarnings("unused")
    // @Shadow
    // protected Map<Long, StructureStart> structureMap = new RWLockedLong2ObjectMap<>(
    // spool$structureMapLock,
    // new Long2ObjectOpenHashMap<>());
    // we now need to manually synchronize on every use of the map
    // so thats fucking annoying,,, yay.

    @Shadow
    protected Map<Long, StructureStart> structureMap;

    @WrapMethod(method = "func_151538_a")
    private void func_151538_aWrapped(World worldIn, int p_151538_2_, int p_151538_3_, int p_151538_4_, int p_151538_5_,
        Block[] p_151538_6_, Operation<Void> original) {
        synchronized (structureMap) {
            original.call(worldIn, p_151538_2_, p_151538_3_, p_151538_4_, p_151538_5_, p_151538_6_);
        }
    }

    @WrapMethod(method = "func_143027_a")
    private void func_143027_aWrapped(World p_143027_1_, Operation<Void> original) {
        synchronized (structureMap) {
            original.call(p_143027_1_);
        }
    }

    @WrapMethod(method = "generateStructuresInChunk")
    private boolean generateStructuresInChunkWrapped(World p_75051_1_, Random p_75051_2_, int p_75051_3_,
        int p_75051_4_, Operation<Boolean> original) {
        synchronized (structureMap) {
            return original.call(p_75051_1_, p_75051_2_, p_75051_3_, p_75051_4_);
        }
    }

    @WrapMethod(method = "func_143028_c")
    private StructureStart func_143028_cWrapped(int p_143028_1_, int p_143028_2_, int p_143028_3_,
        Operation<StructureStart> original) {
        synchronized (structureMap) {
            return original.call(p_143028_1_, p_143028_2_, p_143028_3_);
        }
    }

    @WrapMethod(method = "func_142038_b")
    private boolean func_142038_bWrapped(int p_143028_1_, int p_143028_2_, int p_143028_3_,
        Operation<Boolean> original) {
        synchronized (structureMap) {
            return original.call(p_143028_1_, p_143028_2_, p_143028_3_);
        }
    }

    @WrapMethod(method = "func_151545_a")
    private ChunkPosition func_151545_aWrapped(World p_151545_1_, int p_151545_2_, int p_151545_3_, int p_151545_4_,
        Operation<ChunkPosition> original) {
        synchronized (structureMap) {
            return original.call(p_151545_1_, p_151545_2_, p_151545_3_, p_151545_4_);
        }
    }

    // end annoying shit

    // @Inject(method = "generateStructuresInChunk", at = @At("HEAD"))
    // private void generateStructuresInChunkHead(World p_75051_1_, Random p_75051_2_, int p_75051_3_, int p_75051_4_,
    // CallbackInfoReturnable<Boolean> cir) {
    // spool$structureMapLock.readLock()
    // .lock();
    // }

    // @Inject(method = "generateStructuresInChunk", at = @At("RETURN"))
    // private void generateStructuresInChunkReturn(World p_75051_1_, Random p_75051_2_, int p_75051_3_, int p_75051_4_,
    // CallbackInfoReturnable<Boolean> cir) {
    // spool$structureMapLock.readLock()
    // .unlock();
    // }

    // @Inject(method = "func_143028_c", at = @At("HEAD"))
    // private void func_143028_cHead(int p_143028_1_, int p_143028_2_, int p_143028_3_,
    // CallbackInfoReturnable<StructureStart> cir) {
    // spool$structureMapLock.readLock()
    // .lock();
    // }

    // @Inject(method = "func_143028_c", at = @At("RETURN"))
    // private void func_143028_cReturn(int p_143028_1_, int p_143028_2_, int p_143028_3_,
    // CallbackInfoReturnable<StructureStart> cir) {
    // spool$structureMapLock.readLock()
    // .unlock();
    // }

    // @Inject(method = "func_142038_b", at = @At("HEAD"))
    // private void func_142038_bHead(int p_142038_1_, int p_142038_2_, int p_142038_3_,
    // CallbackInfoReturnable<Boolean> cir) {
    // spool$structureMapLock.readLock()
    // .lock();
    // }

    // @Inject(method = "func_142038_b", at = @At("RETURN"))
    // private void func_142038_bReturn(int p_142038_1_, int p_142038_2_, int p_142038_3_,
    // CallbackInfoReturnable<Boolean> cir) {
    // spool$structureMapLock.readLock()
    // .unlock();
    // }

    // @Inject(method = "func_151545_a", at = @At("HEAD"))
    // private void func_151545_aHead(World p_151545_1_, int p_151545_2_, int p_151545_3_, int p_151545_4_,
    // CallbackInfoReturnable<ChunkPosition> cir) {
    // spool$structureMapLock.readLock()
    // .lock();
    // }

    // @Inject(method = "func_151545_a", at = @At("RETURN"))
    // private void func_151545_aReturn(World p_151545_1_, int p_151545_2_, int p_151545_3_, int p_151545_4_,
    // CallbackInfoReturnable<ChunkPosition> cir) {
    // spool$structureMapLock.readLock()
    // .unlock();
    // }
}
