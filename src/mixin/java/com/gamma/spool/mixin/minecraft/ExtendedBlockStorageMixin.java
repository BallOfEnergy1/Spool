package com.gamma.spool.mixin.minecraft;

import net.minecraft.block.Block;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.gamma.spool.api.annotations.Synchronize;

@Mixin(value = ExtendedBlockStorage.class, priority = 1001)
public abstract class ExtendedBlockStorageMixin {

    @Shadow
    @Synchronize(on = "Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;blockLSBArray:[B")
    public abstract Block getBlockByExtId(int p_150819_1_, int p_150819_2_, int p_150819_3_);

    @Shadow
    @Synchronize(on = "Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;blockLSBArray:[B")
    public abstract void func_150818_a(int p_150818_1_, int p_150818_2_, int p_150818_3_, Block p_150818_4_);

    @Shadow
    @Synchronize(on = "Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;blockLSBArray:[B")
    public abstract boolean getNeedsRandomTick();

    @Shadow
    @Synchronize(on = "Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;blockLSBArray:[B")
    public abstract boolean isEmpty();
}
