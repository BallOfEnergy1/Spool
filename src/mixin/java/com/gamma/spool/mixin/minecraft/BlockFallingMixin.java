package com.gamma.spool.mixin.minecraft;

import net.minecraft.block.BlockFalling;
import net.minecraft.world.World;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gamma.spool.async.ImmediateFallAsync;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(BlockFalling.class)
public abstract class BlockFallingMixin {

    @Redirect(
        method = "func_149830_m",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/block/BlockFalling;fallInstantly:Z",
            opcode = Opcodes.GETSTATIC))
    private boolean redirectFallInstantly(@Local(argsOnly = true) World world,
        @Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 2) int z) {
        return ImmediateFallAsync.isLocationImmediateFall(world, x >> 4, z >> 4);
    }
}
