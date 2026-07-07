package com.gamma.spool.core;

import static com.gamma.spool.core.SpoolCompat.logChange;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.gamma.gammalib.multi.MultiJavaUtil;
import com.gamma.spool.api.annotations.SkipSpoolASMChecks;
import com.gamma.spool.asm.SynchronizationInjectionHelper;
import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;

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

        List<String> modIDs = null;

        if (isLate) {
            String[] sections = mixinName.split("\\.");
            for (int idx = 0; idx < sections.length - 1 /* Skip last element; mixin name. */; idx++) {
                String section = sections[idx];
                if (section.equals("concurrent") || section.equals("nonconcurrent")) {
                    break;
                }

                // Do not load if one of the target mods is not loaded.
                if (SpoolCompat.isModLoaded(section)) {
                    logChange(
                        "MIXIN",
                        "Loading mixin",
                        "Loading mixin " + mixinName + " because mod " + section + " is loaded.");
                } else {
                    logChange(
                        "MIXIN",
                        "Excluded mixin",
                        "Not loading mixin " + mixinName + " because mod " + section + " is not loaded.");
                    SpoolLogger.compatInfo("Not loading mixin: " + mixinName);
                    return false;
                }

                if (modIDs == null) modIDs = new ObjectArrayList<>();
                modIDs.add(section);
            }
        }

        if (!isLate) {
            logChange("MIXIN", "Loading mixin", "Loading mixin " + mixinName + ".");
        }

        SpoolLogger.compatInfo("Loaded mixin: " + mixinName);

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {

        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode classNode, String mixinClassName, IMixinInfo mixinInfo) {
        logChange(mixinClassName, targetClassName, false);
    }

    @Override
    public void postApply(String targetClassName, ClassNode classNode, String mixinClassName, IMixinInfo mixinInfo) {
        logChange(mixinClassName, targetClassName, true);
        SynchronizationInjectionHelper.onMixinPostApply(classNode, mixinInfo);
    }

    // Late mixin setup

    @Override
    public String getMixinConfig() {
        if (MultiJavaUtil.supportsVersion(21)) return "mixins.spool_21.late.json";
        if (MultiJavaUtil.hasJava17Support()) return "mixins.spool_17.late.json";
        if (MultiJavaUtil.hasJava9Support()) return "mixins.spool_9.late.json";
        return "mixins.spool.late.json";
    }

    @NotNull
    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        List<String> mixins = new ArrayList<>(IMixins.getLateMixins(Mixins.class, loadedMods));
        mixins.addAll(SpoolCoreMod.lateMixins);
        return mixins;
    }
}
