package com.gamma.spool.mixin.minecraft;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;

import com.gamma.spool.Tags;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.IFMLSidedHandler;
import cpw.mods.fml.relauncher.Side;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

@Mixin(value = FMLCommonHandler.class, remap = false)
public abstract class FMLCommonHandlerMixin {

    // FML branding (inspired by Angelica (mitchej123))
    @WrapOperation(
        method = "computeBranding",
        at = @At(
            value = "INVOKE",
            target = "Lcpw/mods/fml/common/IFMLSidedHandler;getAdditionalBrandingInformation()Ljava/util/List;",
            remap = false),
        remap = false)
    private List<String> wrapBranding(IFMLSidedHandler instance, Operation<List<String>> original) {
        // Because Angelica *AND* FML make this an immutable list.
        List<String> additional = new ObjectArrayList<>(original.call(instance));
        additional.add(String.format("Spool %s", Tags.VERSION));
        return additional;
    }

    /**
     * @author BallOfEnergy01
     * @reason Fix improper thread analysis.
     */
    @Overwrite(remap = false)
    public Side getEffectiveSide() {
        Thread thread = Thread.currentThread();
        if (thread.getName()
            .toLowerCase()
            .contains("server thread")) {
            return Side.SERVER;
        } else if (thread.getName()
            .toLowerCase()
            .contains("client thread")) {
                return Side.CLIENT;
            } else if (thread.getName()
                .toLowerCase()
                .contains("spool")) {
                    return Side.SERVER;
                }
        return Side.CLIENT;
    }
}
