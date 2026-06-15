package com.gamma.spool.mixin.minecraft;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
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
            target = "Lcpw/mods/fml/common/IFMLSidedHandler;getAdditionalBrandingInformation()Ljava/util/List;"))
    private List<String> wrapBranding(IFMLSidedHandler instance, Operation<List<String>> original) {
        // Because Angelica *AND* FML make this an immutable list.
        List<String> additional = new ObjectArrayList<>(original.call(instance));
        additional.add(String.format("Spool %s", Tags.VERSION));
        return additional;
    }

    // null default values allow us to detect assignment.
    @Unique
    private static final ThreadLocal<Boolean> spool$isServerThread = ThreadLocal.withInitial(() -> null);

    /**
     * @author BallOfEnergy01
     * @reason Fix improper thread analysis.
     */
    @Overwrite
    public Side getEffectiveSide() {
        Boolean bool = spool$isServerThread.get();
        // End early if we know we're either one or the other.
        if (bool != null) return bool ? Side.SERVER : Side.CLIENT;

        Side currentSide = Side.CLIENT;

        Thread thread = Thread.currentThread();
        String name = thread.getName();
        if (name.equals("Server thread")) {
            currentSide = Side.SERVER;
        } else if (name.contains("Spool") || name.toLowerCase()
            .contains("spool")) {
                // As it turns out, `toLowerCase` is slow. Check if it contains the uppercase `Spool` (standard...
                // mostly) first.
                currentSide = Side.SERVER;
            }

        // Set our thread-local now that we've checked
        if (currentSide.isServer()) spool$isServerThread.set(true);
        else spool$isServerThread.set(false);

        return currentSide;
    }
}
