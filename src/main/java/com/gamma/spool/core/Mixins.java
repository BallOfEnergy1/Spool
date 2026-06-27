package com.gamma.spool.core;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    CHUNK_SYNC(new MixinBuilder("Synchronize Chunk methods").addClientMixins(convert("ChunkMixin_Sync"))
        .addExcludedMod(TargetedMod.SUPERNOVA)
        .setPhase(Phase.EARLY)),
    CHUNK_SYNC_SUPERNOVA(
        new MixinBuilder("Synchronize Chunk methods (Supernova)").addClientMixins(convert("ChunkMixin_Sync_Supernova"))
            .addRequiredMod(TargetedMod.SUPERNOVA)
            .setPhase(Phase.EARLY));

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }

    private static String convert(String original) {
        return "imixins." + original;
    }
}
