package com.gamma.spool.mixin.minecraft;

import java.io.File;
import java.net.Proxy;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin extends MinecraftServer {

    public IntegratedServerMixin(File workDir, Proxy proxy) {
        super(workDir, proxy);
    }

    @WrapOperation(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/integrated/IntegratedServer;saveAllWorlds(Z)V"))
    public void wrappedSave(IntegratedServer instance, boolean b, Operation<Void> original) {
        if (getTickCounter() < 100) // Why the hell this happens? I DON'T KNOW! Whenever the game pauses in the very
                                    // beginning of creating a game, it tries to save millions of entries to disk.
            // TODO: Find a better way to detect this...
            original.call(instance, b);
    }
}
