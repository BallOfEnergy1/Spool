package com.gamma.spool.mixin.minecraft;

import net.minecraft.block.BlockBed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(BlockBed.class)
public abstract class BlockBedMixin {

    @WrapMethod(method = "onBlockActivated")
    private boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer player, int side, float subX,
        float subY, float subZ, Operation<Boolean> original) {
        synchronized (worldIn.playerEntities) {
            return original.call(worldIn, x, y, z, player, side, subX, subY, subZ);
        }
    }
}
