package com.gamma.spool.mixin.compat.hbm_space.concurrent;

import java.util.List;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gamma.spool.concurrent.providers.ConcurrentChunkProviderServer;
import com.hbm.handler.ImpactWorldHandler;

@Mixin(ImpactWorldHandler.class)
public abstract class ImpactWorldHandlerMixin {

    @Redirect(
        method = "impactEffects",
        at = @At(
            value = "FIELD",
            opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/world/gen/ChunkProviderServer;loadedChunks:Ljava/util/List;",
            remap = false),
        remap = false)
    private static List<Chunk> redirectLoadedChunksField(ChunkProviderServer instance) {
        ConcurrentChunkProviderServer concurrentProvider = (ConcurrentChunkProviderServer) instance;
        return concurrentProvider.getChunkList();
    }
}
