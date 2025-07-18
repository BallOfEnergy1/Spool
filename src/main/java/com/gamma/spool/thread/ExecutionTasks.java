package com.gamma.spool.thread;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.minecraft.server.MinecraftServer;

import com.gamma.spool.util.concurrent.AsyncProfiler;

public class ExecutionTasks {

    public static Runnable wrapTask(Runnable task) {
        return new ExecutionTaskRunnable(task);
    }

    public static <A> Consumer<A> wrapTask(Consumer<A> task) {
        return new ExecutionTaskConsumer<>(task);
    }

    public static <A, B> BiConsumer<A, B> wrapTask(BiConsumer<A, B> task) {
        return new ExecutionTaskBiConsumer<>(task);
    }

    public static class ExecutionTaskRunnable implements Runnable {

        private final Runnable thisTask;

        public ExecutionTaskRunnable(Runnable taskToWrap) {
            this.thisTask = taskToWrap;
        }

        @Override
        public void run() {
            AsyncProfiler theProfiler = (AsyncProfiler) MinecraftServer.getServer().theProfiler;
            theProfiler.startSection("root");
            thisTask.run();
            theProfiler.endSection();
        }
    }

    public static class ExecutionTaskConsumer<A> implements Consumer<A> {

        private final Consumer<A> thisTask;

        public ExecutionTaskConsumer(Consumer<A> taskToWrap) {
            this.thisTask = taskToWrap;
        }

        @Override
        public void accept(A arg1) {
            AsyncProfiler theProfiler = (AsyncProfiler) MinecraftServer.getServer().theProfiler;
            theProfiler.startSection("root");
            thisTask.accept(arg1);
            theProfiler.endSection();
        }
    }

    public static class ExecutionTaskBiConsumer<A, B> implements BiConsumer<A, B> {

        private final BiConsumer<A, B> thisTask;

        public ExecutionTaskBiConsumer(BiConsumer<A, B> taskToWrap) {
            this.thisTask = taskToWrap;
        }

        @Override
        public void accept(A arg1, B arg2) {
            AsyncProfiler theProfiler = (AsyncProfiler) MinecraftServer.getServer().theProfiler;
            theProfiler.startSection("root");
            thisTask.accept(arg1, arg2);
            theProfiler.endSection();
        }
    }
}
