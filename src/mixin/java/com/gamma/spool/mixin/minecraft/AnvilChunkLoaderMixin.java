package com.gamma.spool.mixin.minecraft;

import java.util.List;

import net.minecraft.world.chunk.storage.AnvilChunkLoader;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

@Mixin(AnvilChunkLoader.class)
public abstract class AnvilChunkLoaderMixin {

    @Redirect(
        method = "writeChunkToNBT",
        at = @At(
            value = "FIELD",
            args = "array=get",
            opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/world/chunk/Chunk;entityLists:[Ljava/util/List;"))
    private List<?> redirectWriteField(List<?>[] array, int index) {
        return new ObjectArrayList<>(array);
    }
}
