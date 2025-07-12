package com.gamma.spool.mixin.minecraft.profiler;

import java.text.DecimalFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.profiler.IPlayerUsage;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.MathHelper;

import org.apache.commons.lang3.NotImplementedException;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gamma.spool.util.AsyncProfiler;
import com.gamma.spool.util.ThreadUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin implements IPlayerUsage {

    @WrapOperation(
        method = "runGameLoop",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;checkGLError(Ljava/lang/String;)V",
            ordinal = 1))
    public void wrappedCheckGLError(Minecraft instance, String s, Operation<Void> original) {
        this.mcProfiler.startSection("postRenderErrors");
        original.call(instance, s);
        this.mcProfiler.endSection();
    }

    @WrapOperation(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;func_147120_f()V"))
    public void wrappedFunc_147120_f(Minecraft instance, Operation<Void> original) {
        this.mcProfiler.startSection("updateDisplay");
        original.call(instance);
        this.mcProfiler.endSection();
    }

    @Shadow
    @Mutable
    @Final
    public Profiler mcProfiler;
    @Shadow
    private String debugProfilerName;
    @Shadow
    public int displayWidth;
    @Shadow
    public int displayHeight;
    @Shadow
    public FontRenderer fontRenderer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        mcProfiler = new AsyncProfiler(); // New async profiler.
    }

    /**
     * @author BallOfEnergy01
     * @reason Use async profiler instead of traditional profiler.
     */
    @Overwrite
    private void displayDebugInfo(long elapsedTicksTime) {
        if (this.mcProfiler.profilingEnabled) {
            AsyncProfiler.AsyncResults results = ((AsyncProfiler) this.mcProfiler)
                .getAsyncProfilingData(this.debugProfilerName);
            // TODO: Implement this. This will probably take rewriting a lot of this.
            if (results.numThreadsProfiled > 1) throw new NotImplementedException(
                "Client profiler results found more than one thread. This is not supported by the profiler overlay. Please report this to the mod author.");

            // Sometimes happens on the split second before the profiler kicks in. Just display nothing.
            if (results.list.isEmpty()) return;

            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glEnable(GL11.GL_COLOR_MATERIAL);
            GL11.glLoadIdentity();
            GL11.glOrtho(0.0D, this.displayWidth, this.displayHeight, 0.0D, 1000.0D, 3000.0D);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            GL11.glTranslatef(0.0F, 0.0F, -2000.0F);
            GL11.glLineWidth(1.0F);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            Tessellator tessellator = Tessellator.instance;
            short short1 = 160;
            int j = this.displayWidth - short1 - 10;
            int k = this.displayHeight - short1 * 2;
            GL11.glEnable(GL11.GL_BLEND);
            tessellator.startDrawingQuads();
            tessellator.setColorRGBA_I(0, 200);
            tessellator
                .addVertex(((float) j - (float) short1 * 1.1F), ((float) k - (float) short1 * 0.6F - 16.0F), 0.0D);
            tessellator.addVertex(((float) j - (float) short1 * 1.1F), (k + short1 * 2), 0.0D);
            tessellator.addVertex(((float) j + (float) short1 * 1.1F), (k + short1 * 2), 0.0D);
            tessellator
                .addVertex(((float) j + (float) short1 * 1.1F), ((float) k - (float) short1 * 0.6F - 16.0F), 0.0D);
            tessellator.draw();
            GL11.glDisable(GL11.GL_BLEND);
            double d0 = 0.0D;
            int i1;

            for (int i = 1, listSize = results.list.size(); i < listSize; i++) {
                AsyncProfiler.AsyncResult result1 = results.list.get(i);
                i1 = MathHelper.floor_double(result1.percentSection() / 4.0D) + 1;
                tessellator.startDrawing(6);
                tessellator.setColorOpaque_I(result1.getColor());
                tessellator.addVertex(j, k, 0.0D);
                int j1;
                float f;
                float f1;
                float f2;

                for (j1 = i1; j1 >= 0; --j1) {
                    f = (float) ((d0 + result1.percentSection() * (double) j1 / (double) i1) * Math.PI * 2.0D / 100.0D);
                    f1 = MathHelper.sin(f) * (float) short1;
                    f2 = MathHelper.cos(f) * (float) short1 * 0.5F;
                    tessellator.addVertex(((float) j + f1), ((float) k - f2), 0.0D);
                }

                tessellator.draw();
                tessellator.startDrawing(5);
                tessellator.setColorOpaque_I((result1.getColor() & 16711422) >> 1);

                for (j1 = i1; j1 >= 0; --j1) {
                    f = (float) ((d0 + result1.percentSection() * (double) j1 / (double) i1) * Math.PI * 2.0D / 100.0D);
                    f1 = MathHelper.sin(f) * (float) short1;
                    f2 = MathHelper.cos(f) * (float) short1 * 0.5F;
                    tessellator.addVertex(((float) j + f1), ((float) k - f2), 0.0D);
                    tessellator.addVertex(((float) j + f1), ((float) k - f2 + 10.0F), 0.0D);
                }

                tessellator.draw();
                d0 += result1.percentSection();
            }

            DecimalFormat decimalformat = new DecimalFormat("##0.00");
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            String s = "";

            AsyncProfiler.AsyncResult result = results.list.remove(0);
            if (result != null) {
                if (!result.sectionName()
                    .equals("unspecified")) {
                    s = s + "[0] ";
                }

                if (result.sectionName()
                    .isEmpty()) {
                    s = s + "ROOT ";
                } else {
                    s = s + result.sectionName() + " ";
                }

                i1 = 16777215;
                this.fontRenderer.drawStringWithShadow(s, j - short1, k - short1 / 2 - 16, i1);
                this.fontRenderer.drawStringWithShadow(
                    s = decimalformat.format(result.percentRoot()) + "%",
                    j + short1 - this.fontRenderer.getStringWidth(s),
                    k - short1 / 2 - 16,
                    i1);
            }

            for (int k1 = 0; k1 < results.list.size(); ++k1) {
                AsyncProfiler.AsyncResult result2 = results.list.get(k1);
                String s1 = "";

                if (result2.sectionName()
                    .equals("unspecified")) {
                    s1 = s1 + "[?] ";
                } else {
                    s1 = s1 + "[" + (k1 + 1) + "] ";
                }

                s1 = s1 + "(thread " + ThreadUtil.getThreadNameById(result2.threadID()) + ") " + result2.sectionName();
                this.fontRenderer
                    .drawStringWithShadow(s1, j - short1, k + short1 / 2 + k1 * 8 + 20, result2.getColor());
                this.fontRenderer.drawStringWithShadow(
                    s1 = decimalformat.format(result2.percentSection()) + "%",
                    j + short1 - 50 - this.fontRenderer.getStringWidth(s1),
                    k + short1 / 2 + k1 * 8 + 20,
                    result2.getColor());
                this.fontRenderer.drawStringWithShadow(
                    s1 = decimalformat.format(result2.percentRoot()) + "%",
                    j + short1 - this.fontRenderer.getStringWidth(s1),
                    k + short1 / 2 + k1 * 8 + 20,
                    result2.getColor());
            }
        }
    }

    /**
     * @author BallOfEnergy01
     * @reason Use async profiler instead of traditional profiler.
     */
    @Overwrite
    private void updateDebugProfilerName(int keyCount) {
        AsyncProfiler.AsyncResults results = ((AsyncProfiler) this.mcProfiler)
            .getAsyncProfilingData(this.debugProfilerName);

        if (!results.list.isEmpty()) {
            results.list.removeElements(0, results.numThreadsProfiled);

            if (keyCount == 0) {
                if (!this.debugProfilerName.isEmpty() /* && !this.debugProfilerName.equals("root") */) {
                    int j = this.debugProfilerName.lastIndexOf(".");

                    if (j >= 0) {
                        this.debugProfilerName = this.debugProfilerName.substring(0, j);
                    }
                }
            } else {
                --keyCount;
                if (keyCount < results.list.size() && !results.list.get(keyCount)
                    .sectionName()
                    .equals("unspecified")) {
                    if (!this.debugProfilerName.isEmpty()) {
                        this.debugProfilerName = this.debugProfilerName + ".";
                    }

                    this.debugProfilerName = this.debugProfilerName + results.list.get(keyCount)
                        .sectionName();
                }
            }
        }
    }
}
