package com.gamma.spool.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.gamma.spool.config.ConcurrentConfig;
import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import cpw.mods.fml.common.Loader;

@LateMixin
public class SpoolMixinCore implements IMixinConfigPlugin, ILateMixinLoader {

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return "";
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Class name preprocessing
        String mixinName = mixinClassName.replace("com.gamma.spool.mixin.", "");

        boolean isLate = false;
        if (mixinName.startsWith("compat.")) {
            isLate = true;
            mixinName = mixinName.replace("compat.", "");
        }

        mixinName = mixinName.replace("minecraft.", "");
        // Preprocessing end

        // Force disabled mixins (testing and such).
        switch (mixinName) {
            case "concurrent.MapGenBaseMixin" -> {
                return false;
            }
        }

        String modID = null;

        if (isLate) {
            modID = mixinName.substring(0, mixinName.indexOf("."));
            // Do not load if the target mod is not loaded.
            if (!Loader.isModLoaded(modID)) return false;
        }

        boolean isConcurrentEnabled = ConcurrentConfig.enableConcurrentWorldAccess;

        // Mixins that should be enabled when concurrent world access is *enabled*.
        if (mixinName.startsWith("concurrent.")) {
            if (isLate && isConcurrentEnabled)
                SpoolLogger.compatInfo("Loaded compat mixin (concurrent) for mod " + modID + ": " + mixinName);

            return isConcurrentEnabled;
        }

        // Mixins that should be disabled when concurrent world access is *enabled*.
        if (mixinName.startsWith("nonconcurrent.")) {
            if (isLate && !isConcurrentEnabled)
                SpoolLogger.compatInfo("Loaded compat mixin (nonconcurrent) for mod " + modID + ": " + mixinName);

            return !isConcurrentEnabled;
        }

        // Always enabled.
        if (isLate) SpoolLogger.compatInfo("Loaded compat mixin for mod " + modID + ": " + mixinName);
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return new ArrayList<>(); // Pull from mixin config.
    }

    @Override
    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {}

    @Override
    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {}

    // Late mixin setup

    @Override
    public String getMixinConfig() {
        return "mixins.late.spool.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        return new ArrayList<>(); // Pull from late mixin config.
    }
}
