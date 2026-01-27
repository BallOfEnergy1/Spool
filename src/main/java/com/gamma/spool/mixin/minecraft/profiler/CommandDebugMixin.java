package com.gamma.spool.mixin.minecraft.profiler;

import net.minecraft.command.CommandDebug;
import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import com.gamma.gammalib.util.ThreadUtil;
import com.gamma.spool.util.concurrent.AsyncProfiler;

@Mixin(CommandDebug.class)
public abstract class CommandDebugMixin {

    /**
     * @author BallOfEnergy01
     * @reason Use async profiler instead of traditional profiler.
     */
    @Overwrite
    private void func_147202_a(int p_147202_1_, String p_147202_2_, StringBuilder p_147202_3_) {
        AsyncProfiler.AsyncResults results = ((AsyncProfiler) MinecraftServer.getServer().theProfiler)
            .getAsyncProfilingData(p_147202_2_);

        if (results != null && results.list.size() >= 3) {
            for (int j = 0; j < results.numThreadsProfiled; ++j) {
                AsyncProfiler.AsyncResult result = results.list.get(j);
                p_147202_3_.append(String.format("[%02d] ", p_147202_1_));

                // noinspection StringRepeatCanBeUsed
                for (int k = 0; k < p_147202_1_; ++k) {
                    p_147202_3_.append(" ");
                }

                p_147202_3_.append("Thread ");
                p_147202_3_.append(ThreadUtil.getThreadNameById(result.threadID()));
                p_147202_3_.append(" root");
                p_147202_3_.append(" - ");
                p_147202_3_.append(String.format("%.2f", result.percentSection()));
                p_147202_3_.append("%/");
                p_147202_3_.append(String.format("%.2f", result.percentRoot()));
                p_147202_3_.append("%\n");

                if (!result.sectionName()
                    .equals("unspecified")) {
                    try {
                        this.spool$func_147202_a(p_147202_1_ + 1, p_147202_2_, p_147202_3_, result.threadID());
                    } catch (Exception exception) {
                        p_147202_3_.append("[[ EXCEPTION ")
                            .append(exception)
                            .append(" ]]");
                    }
                }
                p_147202_3_.append("\n");
            }
        }
    }

    @Unique
    private void spool$func_147202_a(int p_147202_1_, String p_147202_2_, StringBuilder p_147202_3_, long threadID) {
        AsyncProfiler.AsyncResults results = ((AsyncProfiler) MinecraftServer.getServer().theProfiler)
            .getAsyncProfilingData(p_147202_2_, threadID, false);

        if (results != null && results.list.size() >= 3) {
            for (int j = results.numThreadsProfiled; j < results.list.size(); ++j) {
                AsyncProfiler.AsyncResult result = results.list.get(j);
                p_147202_3_.append(String.format("[%02d] ", p_147202_1_));

                // noinspection StringRepeatCanBeUsed
                for (int k = 0; k < p_147202_1_; ++k) {
                    p_147202_3_.append(" ");
                }

                p_147202_3_.append(result.sectionName());
                p_147202_3_.append(" - ");
                p_147202_3_.append(String.format("%.2f", result.percentSection()));
                p_147202_3_.append("%/");
                p_147202_3_.append(String.format("%.2f", result.percentRoot()));
                p_147202_3_.append("%\n");

                if (!result.sectionName()
                    .equals("unspecified")) {
                    try {
                        this.spool$func_147202_a(
                            p_147202_1_ + 1,
                            p_147202_2_ + "." + result.sectionName(),
                            p_147202_3_,
                            result.threadID());
                    } catch (Exception exception) {
                        p_147202_3_.append("[[ EXCEPTION ")
                            .append(exception)
                            .append(" ]]");
                    }
                }
            }
        }
    }
}
