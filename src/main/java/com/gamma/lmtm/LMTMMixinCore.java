package com.gamma.lmtm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

@LateMixin
public class LMTMMixinCore implements IMixinConfigPlugin, ILateMixinLoader {

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return "";
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return new ArrayList<>();
    }

    @Override
    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {}

    @Override
    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {}

    // LATE MIXINS

    public enum CompatMixins {

        hodgepodge(new String[] { "FastUtilLongHashMapMixin_hodgepodge", "SimulationDistanceHelperMixin_hodgepodge" });

        public final String[] mixinsToLoad;

        CompatMixins(String[] mixinsToLoad) {
            this.mixinsToLoad = mixinsToLoad;
        }
    }

    @Override
    public String getMixinConfig() {
        return "mixins.late.lmtm.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        int amount = 0;
        List<String> mixinsToLoad = new ArrayList<>();
        for (CompatMixins modMixins : CompatMixins.values()) {
            if (loadedMods.contains(modMixins.name())) {
                amount++;
                Collections.addAll(mixinsToLoad, modMixins.mixinsToLoad);
            }
        }
        LMTM.logger.info("Loaded {} compatibility mixins for {} mod(s).", mixinsToLoad.size(), amount);
        return mixinsToLoad;
    }
}
