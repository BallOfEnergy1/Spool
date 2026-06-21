package com.gamma.spool.mixin.minecraft.concurrent;

import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkProviderEnd;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gamma.spool.async.ImmediateFallAsync;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(ChunkProviderEnd.class)
public abstract class ChunkProviderEndMixin {

    @Shadow
    public World endWorld;

    @Redirect(
        method = "populate",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/block/BlockFalling;fallInstantly:Z",
            opcode = Opcodes.PUTSTATIC))
    private void redirectFallInstantly(boolean value, @Local(argsOnly = true, ordinal = 0) int x,
        @Local(argsOnly = true, ordinal = 1) int z) {
        if (value) ImmediateFallAsync.addImmediateFall(endWorld, x >> 4, z >> 4);
        else ImmediateFallAsync.removeImmediateFall(endWorld, x >> 4, z >> 4);
    }
}
