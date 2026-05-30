package com.gamma.spool.mixin.minecraft.concurrent;

import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenHellLava;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gamma.spool.concurrent.async.ImmediateUpdatesAsync;

@Mixin(WorldGenHellLava.class)
public abstract class WorldGenHellLavaMixin {

    @Redirect(
        method = "generate",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/World;scheduledUpdatesAreImmediate:Z",
            opcode = Opcodes.PUTFIELD))
    private void redirectScheduledUpdatesAreImmediate(World instance, boolean value) {
        ImmediateUpdatesAsync.setImmediateUpdate(value);
    }
}
