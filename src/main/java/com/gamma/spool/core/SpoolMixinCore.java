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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

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

        List<String> modIDs = null;

        if (isLate) {
            String[] sections = mixinName.split("\\.");
            for (int idx = 0; idx < sections.length - 1 /* Skip last element; mixin name. */; idx++) {
                String section = sections[idx];
                if (section.equals("concurrent") || section.equals("nonconcurrent")) {
                    break;
                }
                // Do not load if one of the target mods is not loaded.
                if (!Loader.isModLoaded(section)) return false;

                if (modIDs == null) modIDs = new ObjectArrayList<>();
                modIDs.add(section);
            }
        }

        boolean isConcurrentEnabled = ConcurrentConfig.enableConcurrentWorldAccess;

        // Mixins that should be enabled when concurrent world access is *enabled*.
        if (mixinName.startsWith("concurrent.")) {
            if (isLate && isConcurrentEnabled)
                SpoolLogger.compatInfo("Loaded compat mixin (concurrent) for mod(s) " + modIDs + ": " + mixinName);
            return isConcurrentEnabled;
        }

        // Mixins that should be disabled when concurrent world access is *enabled*.
        if (mixinName.startsWith("nonconcurrent.")) {
            if (isLate && !isConcurrentEnabled)
                SpoolLogger.compatInfo("Loaded compat mixin (nonconcurrent) for mod(s) " + modIDs + ": " + mixinName);

            return !isConcurrentEnabled;
        }

        // Always enabled.
        if (isLate) SpoolLogger.compatInfo("Loaded compat mixin for mod(s) " + modIDs + ": " + mixinName);
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
