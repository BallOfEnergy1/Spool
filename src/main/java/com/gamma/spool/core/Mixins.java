package com.gamma.spool.core;

import javax.annotation.Nonnull;

import com.gamma.gammalib.core.GammaLibConfig;
import com.gamma.gammalib.multi.MultiJavaUtil;
import com.gamma.spool.config.SpeedupsConfig;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    SQUID_NUKE(new MixinBuilder("Nuke squids").addCommonMixins(convert("BiomeGenBaseMixin"))
        .setApplyIf(() -> SpeedupsConfig.nukeSquids)
        .setPhase(Phase.EARLY)),
    LOOM_OPTIMIZATIONS(new MixinBuilder("Optimize threaded features using virtual threads")
        .addCommonMixins(convert("AsynchronousExecutorMixin"))
        .setApplyIf(() -> GammaLibConfig.useJava25Features && MultiJavaUtil.supportsVersion(21))
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
