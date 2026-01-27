package com.gamma.spool.core;

import static com.gamma.spool.core.SpoolCompat.logChange;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.gamma.gammalib.multi.MultiJavaUtil;
import com.gamma.spool.api.annotations.SkipSpoolASMChecks;
import com.gamma.spool.config.ConcurrentConfig;
import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

@LateMixin
@SkipSpoolASMChecks(SkipSpoolASMChecks.SpoolASMCheck.ALL)
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
        {
            switch (mixinName) {
                case "concurrent.MapGenBaseMixin" -> {
                    return false;
                }
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
                if (!SpoolCompat.isModLoaded(section)) {
                    logChange(
                        "MIXIN",
                        "Excluded mixin",
                        "Not loading mixin " + mixinName + " because mod " + section + " is not loaded.");
                    return false;
                } else {
                    logChange(
                        "MIXIN",
                        "Loading mixin",
                        "Loading mixin " + mixinName + " because mod " + section + " is loaded.");
                }

                if (modIDs == null) modIDs = new ObjectArrayList<>();
                modIDs.add(section);
            }
        }

        boolean isConcurrentEnabled = ConcurrentConfig.enableConcurrentWorldAccess;

        // Nonconcurrent should come first.
        // As it would turn out... `nonconcurrent` contains `concurrent`, breaking stuff!

        // Mixins that should be disabled when concurrent world access is *enabled*.
        if (mixinName.contains("nonconcurrent.")) {
            if (isLate && !isConcurrentEnabled) {
                logChange(
                    "MIXIN",
                    "Loading mixin",
                    "Loading mixin " + mixinName + " because concurrent world is disabled.");
                SpoolLogger.compatInfo("Loaded compat mixin (nonconcurrent): " + mixinName);
            } else if (isConcurrentEnabled) logChange(
                "MIXIN",
                "Excluded mixin",
                "Not loading mixin " + mixinName + " because concurrent world is enabled.");

            return !isConcurrentEnabled;
        }

        // Mixins that should be enabled when concurrent world access is *enabled*.
        if (mixinName.contains("concurrent.")) {
            if (isLate && isConcurrentEnabled) {
                logChange(
                    "MIXIN",
                    "Loading mixin",
                    "Loading mixin " + mixinName + " because concurrent world is enabled.");
                SpoolLogger.compatInfo("Loaded compat mixin (concurrent): " + mixinName);
            } else if (!isConcurrentEnabled) logChange(
                "MIXIN",
                "Excluding mixin",
                "Not loading mixin " + mixinName + " because concurrent world is disabled.");

            return isConcurrentEnabled;
        }

        // Always enabled.
        if (isLate) SpoolLogger.compatInfo("Loaded compat mixin for mod(s) " + modIDs + ": " + mixinName);
        logChange("MIXIN", "Loading mixin", "Loading " + mixinName);
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return new ArrayList<>(); // Pull from mixin config.
    }

    @Override
    public void preApply(String targetClassName, ClassNode classNode, String mixinClassName, IMixinInfo iMixinInfo) {
        logChange(mixinClassName, targetClassName, false);
    }

    @Override
    public void postApply(String targetClassName, ClassNode classNode, String mixinClassName, IMixinInfo iMixinInfo) {
        logChange(mixinClassName, targetClassName, true);
    }

    // Late mixin setup

    @Override
    public String getMixinConfig() {
        if (MultiJavaUtil.supportsVersion(21)) return "mixins.late.spool_21.json";
        if (MultiJavaUtil.hasJava17Support()) return "mixins.late.spool_17.json";
        if (MultiJavaUtil.hasJava9Support()) return "mixins.late.spool_9.json";
        return "mixins.late.spool.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        return new ArrayList<>(); // Pull from late mixin config.
    }
}
