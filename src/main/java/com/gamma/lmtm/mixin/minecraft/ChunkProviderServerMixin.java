package com.gamma.lmtm.mixin.minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.LongHashMap;
import net.minecraft.util.ReportedException;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.ForgeChunkManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gamma.lmtm.LMTM;

@Mixin(ChunkProviderServer.class)
public abstract class ChunkProviderServerMixin {

    @Shadow
    @Mutable
    private Set chunksToUnload;

    @Shadow(remap = false)
    @Mutable
    private Set<Long> loadingChunks;

    @Shadow
    @Mutable
    public LongHashMap loadedChunkHashMap;

    @Shadow
    @Mutable
    public List<Chunk> loadedChunks;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        loadingChunks = ConcurrentHashMap.newKeySet();
        loadedChunks = Collections.synchronizedList(new ArrayList<>());
        // loadedChunkHashMap = new ConcurrentLongHashMap();
    }

    @Shadow
    private Chunk defaultEmptyChunk;

    @Invoker("safeLoadChunk")
    public abstract Chunk invokeSafeLoadChunk(int p_73158_1_, int p_73158_2_);

    @Inject(method = "originalLoadChunk", at = @At("INVOKE"), cancellable = true, remap = false)
    public void originalLoadChunk(int p_73158_1_, int p_73158_2_, CallbackInfoReturnable<Chunk> cir) {
        ChunkProviderServer instance = (ChunkProviderServer) (Object) this;
        long k = ChunkCoordIntPair.chunkXZ2Int(p_73158_1_, p_73158_2_);
        this.chunksToUnload.remove(Long.valueOf(k));
        Chunk chunk = (Chunk) this.loadedChunkHashMap.getValueByKey(k);

        if (chunk == null) {
            boolean added = this.loadingChunks.add(k);
            if (!added) {
                LMTM.logger.warn(
                    "Chunk loading overlap at chunk ({},{}). This issue has been prevented.",
                    p_73158_1_,
                    p_73158_2_);
            }
            synchronized (instance.worldObj) {
                chunk = ForgeChunkManager.fetchDormantChunk(k, instance.worldObj);
            }
            if (chunk == null) {
                chunk = this.invokeSafeLoadChunk(p_73158_1_, p_73158_2_);
            }

            if (chunk == null) {
                if (instance.currentChunkProvider == null) {
                    chunk = this.defaultEmptyChunk;
                } else {
                    try {
                        synchronized (instance.currentChunkProvider) {
                            chunk = instance.currentChunkProvider.provideChunk(p_73158_1_, p_73158_2_);
                        }
                    } catch (Throwable throwable) {
                        CrashReport crashreport = CrashReport
                            .makeCrashReport(throwable, "Exception generating new chunk");
                        CrashReportCategory crashreportcategory = crashreport.makeCategory("Chunk to be generated");
                        crashreportcategory.addCrashSection(
                            "Location",
                            String.format(
                                "%d,%d",
                                new Object[] { Integer.valueOf(p_73158_1_), Integer.valueOf(p_73158_2_) }));
                        crashreportcategory.addCrashSection("Position hash", Long.valueOf(k));
                        crashreportcategory.addCrashSection("Generator", instance.currentChunkProvider.makeString());
                        throw new ReportedException(crashreport);
                    }
                }
            }

            this.loadedChunkHashMap.add(k, chunk);
            this.loadedChunks.add(chunk);
            this.loadingChunks.remove(k);
            chunk.onChunkLoad();
            chunk.populateChunk(instance, instance, p_73158_1_, p_73158_2_);
        }

        cir.setReturnValue(chunk);
    }
}
