package com.gamma.spool.commands;

import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.DimensionManager;

import com.gamma.spool.Spool;
import com.gamma.spool.config.APIConfig;
import com.gamma.spool.config.ConcurrentConfig;
import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.config.DistanceThreadingConfig;
import com.gamma.spool.config.ThreadManagerConfig;
import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.thread.KeyedPoolThreadManager;
import com.gamma.spool.thread.ManagerNames;
import com.gamma.spool.thread.ThreadManager;

public class CommandSpool extends CommandBase {

    @Override
    public String getCommandName() {
        return "spool";
    }

    /**
     * Return the required permission level for this command.
     */
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "commands.spool.usage";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            throw new WrongUsageException("commands.spool.usage");
        }

        String category = args[0];

        if (category.equalsIgnoreCase("info")) {
            if (args.length == 1) {
                throw new WrongUsageException("commands.spool.usage.info");
            }

            String subcategory = args[1];

            if (subcategory.equalsIgnoreCase("dimension")) {
                if (!ThreadsConfig.isDimensionThreadingEnabled())
                    throw new CommandException("Dimension threading disabled.");

                KeyedPoolThreadManager dimensionManager = (KeyedPoolThreadManager) Spool.REGISTERED_THREAD_MANAGERS
                    .get(ManagerNames.DIMENSION);

                sender.addChatMessage(new ChatComponentText("Thread stats:"));
                sender.addChatMessage(
                    new ChatComponentText("   Number of dimensions loaded: " + DimensionManager.getIDs().length));
                sender.addChatMessage(
                    new ChatComponentText("   Number of dimension threads: " + dimensionManager.getNumThreads()));
                sender.addChatMessage(
                    new ChatComponentText(
                        "   Thread keys occupied: " + Arrays.toString(
                            dimensionManager.getKeys()
                                .toArray())));
                sender.addChatMessage(
                    new ChatComponentText(
                        "   Remapped keys: " + Arrays.toString(
                            dimensionManager.getMappedKeys()
                                .toArray())));
                sender.addChatMessage(new ChatComponentText("Time stats:"));
                sender.addChatMessage(
                    new ChatComponentText(
                        "   Overhead time: " + String.format("%.2fms", dimensionManager.getTimeOverhead() / 1000000d)));
                sender.addChatMessage(
                    new ChatComponentText(
                        "   Waiting time: " + String.format("%.2fms", dimensionManager.getTimeWaiting() / 1000000d)));
                sender.addChatMessage(
                    new ChatComponentText(
                        "   Internal execution time: "
                            + String.format("%.2fms", dimensionManager.getTimeExecuting() / 1000000d)));
            } else if (subcategory.equalsIgnoreCase("distance")) {
                if (!ThreadsConfig.isDistanceThreadingEnabled())
                    throw new CommandException("Distance threading disabled.");

                KeyedPoolThreadManager distanceManager = (KeyedPoolThreadManager) Spool.REGISTERED_THREAD_MANAGERS
                    .get(ManagerNames.DISTANCE);

                sender.addChatMessage(new ChatComponentText("Thread stats:"));
                sender.addChatMessage(
                    new ChatComponentText(
                        "   Number of players: " + MinecraftServer.getServer()
                            .getCurrentPlayerCount()));
                sender.addChatMessage(
                    new ChatComponentText("   Number of distance threads: " + distanceManager.getNumThreads()));
                sender.addChatMessage(
                    new ChatComponentText(
                        "   Thread keys occupied: " + Arrays.toString(
                            distanceManager.getKeys()
                                .toArray())));
                sender.addChatMessage(
                    new ChatComponentText(
                        "   Remapped keys: " + Arrays.toString(
                            distanceManager.getMappedKeys()
                                .toArray())));

                sender.addChatMessage(new ChatComponentText("Time stats:"));
                sender.addChatMessage(
                    new ChatComponentText(
                        "   Overhead time: " + String.format("%.2fms", distanceManager.getTimeOverhead() / 1000000d)));
                sender.addChatMessage(
                    new ChatComponentText(
                        "   Waiting time: " + String.format("%.2fms", distanceManager.getTimeWaiting() / 1000000d)));
                sender.addChatMessage(
                    new ChatComponentText(
                        "   Internal execution time: "
                            + String.format("%.2fms", distanceManager.getTimeExecuting() / 1000000d)));
            } else if (subcategory.equalsIgnoreCase("chunk")) {
                if (!ThreadsConfig.isThreadedChunkLoadingEnabled())
                    throw new CommandException("Chunk threading disabled.");

                ThreadManager chunkLoadingManager = (ThreadManager) Spool.REGISTERED_THREAD_MANAGERS
                    .get(ManagerNames.CHUNK_LOAD);

                sender.addChatMessage(new ChatComponentText("Thread stats:"));
                sender.addChatMessage(
                    new ChatComponentText("   Number of chunk threads: " + chunkLoadingManager.getNumThreads()));

                sender.addChatMessage(new ChatComponentText("Time stats:"));
                sender.addChatMessage(
                    new ChatComponentText(
                        "   Overhead time: "
                            + String.format("%.2fms", chunkLoadingManager.getTimeOverhead() / 1000000d)));
                sender.addChatMessage(
                    new ChatComponentText(
                        "   Waiting time: "
                            + String.format("%.2fms", chunkLoadingManager.getTimeWaiting() / 1000000d)));
                sender.addChatMessage(
                    new ChatComponentText(
                        "   Internal execution time: "
                            + String.format("%.2fms", chunkLoadingManager.getTimeExecuting() / 1000000d)));
            } else {
                throw new WrongUsageException("commands.spool.usage.info");
            }
        } else if (category.equalsIgnoreCase("config")) {

            // TODO: Maybe make a config registry of some kind? I don't really care that much though.

            sender.addChatMessage(new ChatComponentText("API config: "));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   statisticGatheringAllowed: "
                        + APIConfig.statisticGatheringAllowed
                        + EnumChatFormatting.RESET));

            sender.addChatMessage(new ChatComponentText("Concurrent config: "));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   enableConcurrentWorldAccess: "
                        + ConcurrentConfig.enableConcurrentWorldAccess
                        + EnumChatFormatting.RESET));

            sender.addChatMessage(new ChatComponentText("Debug config: "));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   debug: " + DebugConfig.debug + EnumChatFormatting.RESET));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   debugLogging: "
                        + DebugConfig.debugLogging
                        + EnumChatFormatting.RESET));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   logASM: " + DebugConfig.logASM + EnumChatFormatting.RESET));

            sender.addChatMessage(new ChatComponentText("Distance threading config: "));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   threadChunkDistance: "
                        + DistanceThreadingConfig.threadChunkDistance
                        + EnumChatFormatting.RESET));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   resolveConflicts: "
                        + DistanceThreadingConfig.resolveConflicts
                        + EnumChatFormatting.RESET));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   parallelizeStreams: "
                        + DistanceThreadingConfig.parallelizeStreams
                        + EnumChatFormatting.RESET));

            sender.addChatMessage(new ChatComponentText("Thread manager config: "));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   globalRunningSingleThreadTimeout: "
                        + ThreadManagerConfig.globalRunningSingleThreadTimeout
                        + EnumChatFormatting.RESET));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   globalTerminatingSingleThreadTimeout: "
                        + ThreadManagerConfig.globalTerminatingSingleThreadTimeout
                        + EnumChatFormatting.RESET));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   dropTasksOnTimeout: "
                        + ThreadManagerConfig.dropTasksOnTimeout
                        + EnumChatFormatting.RESET));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   useLambdaOptimization: "
                        + ThreadManagerConfig.useLambdaOptimization
                        + EnumChatFormatting.RESET));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   betterTaskProfiling: "
                        + ThreadManagerConfig.betterTaskProfiling
                        + EnumChatFormatting.RESET));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   allowProcessingDuringSleep: "
                        + ThreadManagerConfig.allowProcessingDuringSleep
                        + EnumChatFormatting.RESET));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   enableSpoolWatchdog: "
                        + ThreadManagerConfig.enableSpoolWatchdog
                        + EnumChatFormatting.RESET));

            sender.addChatMessage(new ChatComponentText("Threads config: "));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   enableExperimentalThreading: "
                        + ThreadsConfig.enableExperimentalThreading
                        + EnumChatFormatting.RESET));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   enableDistanceThreading: "
                        + ThreadsConfig.enableDistanceThreading
                        + EnumChatFormatting.RESET));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   enableDimensionThreading: "
                        + ThreadsConfig.enableDimensionThreading
                        + EnumChatFormatting.RESET));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   enableThreadedChunkLoading: "
                        + ThreadsConfig.enableThreadedChunkLoading
                        + EnumChatFormatting.RESET));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   entityThreads: "
                        + ThreadsConfig.entityThreads
                        + EnumChatFormatting.RESET));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   blockThreads: "
                        + ThreadsConfig.blockThreads
                        + EnumChatFormatting.RESET));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   distanceMaxThreads: "
                        + ThreadsConfig.distanceMaxThreads
                        + EnumChatFormatting.RESET));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   dimensionMaxThreads: "
                        + ThreadsConfig.dimensionMaxThreads
                        + EnumChatFormatting.RESET));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.ITALIC + "   chunkLoadingThreads: "
                        + ThreadsConfig.chunkLoadingThreads
                        + EnumChatFormatting.RESET));

        } else {
            throw new WrongUsageException("commands.spool.usage");
        }
    }

    /**
     * Adds the strings available in this command to the given list of tab completion options.
     */
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "info", "config");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            return getListOfStringsMatchingLastWord(args, "dimension", "distance", "chunk");
        } else {
            return null;
        }
    }
}
