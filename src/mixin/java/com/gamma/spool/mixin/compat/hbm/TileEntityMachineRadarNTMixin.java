package com.gamma.spool.mixin.compat.hbm;

import java.util.Iterator;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.hbm.tileentity.machine.TileEntityMachineRadarNT;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

@Mixin(value = TileEntityMachineRadarNT.class, remap = false)
public abstract class TileEntityMachineRadarNTMixin {

    @Redirect(
        method = "updateSystem",
        at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", ordinal = 0))
    private static Iterator<?> updateSystem(List<?> instance) {
        // world.loadedEntityList
        synchronized (instance) {
            return new ObjectArrayList<>(instance).iterator();
        }
    }
}
