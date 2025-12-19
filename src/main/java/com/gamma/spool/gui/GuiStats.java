package com.gamma.spool.gui;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import org.jctools.maps.NonBlockingHashMapLong;

import com.gamma.spool.Tags;
import com.gamma.spool.concurrent.providers.ConcurrentChunkProviderServer;
import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.config.ThreadsConfig;
import com.gamma.spool.core.SpoolCompat;
import com.gamma.spool.core.SpoolManagerOrchestrator;
import com.gamma.spool.db.SDBSocket;
import com.gamma.spool.thread.ForkThreadManager;
import com.gamma.spool.thread.IThreadManager;
import com.gamma.spool.thread.KeyedPoolThreadManager;
import com.gamma.spool.thread.LBKeyedPoolThreadManager;
import com.gamma.spool.thread.ManagerNames;
import com.gamma.spool.thread.ThreadManager;
import com.gamma.spool.thread.TimedOperationThreadManager;
import com.gamma.spool.util.caching.RegisteredCache;
import com.gamma.spool.util.concurrent.chunk.BlobConstants;
import com.gamma.spool.util.distance.DistanceThreadingPlayerUtil;
import com.gamma.spool.util.distance.DistanceThreadingUtil;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class GuiStats extends GuiScreen {

    private static final int STATS = -1;
    private static final int VERSION = 0;
    private static final int SDB = 1;
    private static final int DISTANCE_AND_BLOBBING_STATS = 2;
    private static final int THREAD_MANAGER_START_INDEX = 100;

    private int numManagerButtons = 0;
    private int currentScreen = 0;
    private final List<Entry> entriesOnScreen = new ObjectArrayList<>();
    private final List<Entry> entriesFromSelectedItemOnScreen = new ObjectArrayList<>();

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(VERSION, 20, 20, 100, 20, "Version"));
        this.buttonList.add(new GuiButton(STATS, 140, 20, 100, 20, "Stats"));
        this.buttonList.add(new GuiButton(SDB, 260, 20, 100, 20, "SDB Status"));
        this.buttonList.add(new GuiButton(DISTANCE_AND_BLOBBING_STATS, 380, 20, 100, 20, "Chunk Stats"));
        populateScreen();
        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawDefaultBackground();
        this.drawString(
            this.fontRendererObj,
            "Spool Debug Screen",
            20,
            40 + (5 + this.fontRendererObj.FONT_HEIGHT),
            0xFFFFFF);

        entriesOnScreen.removeIf(Objects::isNull); // why does this happen vro :wilted_rose:
        int[] line = new int[3]; // max 3 columns
        for (Entry entry : entriesOnScreen) {
            String[] strings = entry.getStringsRecursive();
            for (String string : strings) {
                this.drawString(
                    this.fontRendererObj,
                    string,
                    20 + (entry.column * 300),
                    80 + (line[entry.column]++ * (5 + this.fontRendererObj.FONT_HEIGHT)),
                    0xFFFFFF);
            }
        }

        entriesFromSelectedItemOnScreen.removeIf(Objects::isNull);
        int selectedLine = 0;
        for (Entry entry : entriesFromSelectedItemOnScreen) {
            String[] strings = entry.getStringsRecursive();
            for (String string : strings) {
                int y = (numManagerButtons * (20 + 5)) + 80 + (selectedLine++ * (5 + this.fontRendererObj.FONT_HEIGHT));
                this.drawString(this.fontRendererObj, string, 460, y, 0xFFFFFF);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        this.buttonList.removeIf(thatButton -> thatButton.id >= THREAD_MANAGER_START_INDEX);
        currentScreen = button.id;

        if (button.id != -1 && button.id < THREAD_MANAGER_START_INDEX) {
            populateScreen();
            super.actionPerformed(button);
            return;
        }

        int i = 0;
        numManagerButtons = 0;
        if (DebugConfig.debug) {
            for (Int2ObjectMap.Entry<IThreadManager> entry : SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS
                .int2ObjectEntrySet()) {
                IThreadManager manager = entry.getValue();
                // nothing special to show for these :ayo:
                if (manager instanceof ForkThreadManager) continue;
                numManagerButtons++;
                String text = "Show extra data for " + manager.getName();
                this.buttonList.add(
                    new GuiButton(
                        THREAD_MANAGER_START_INDEX + entry.getIntKey(),
                        460,
                        80 + (i * (20 + 5)),
                        fontRendererObj.getStringWidth(text) + 20,
                        20,
                        text));
                i++;
            }
        }
        populateScreen();
        super.actionPerformed(button);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        // TODO: figure out how to link this to some kind of frame-counter?
        // if (MinecraftServer.getServer().getTickCounter() % 2 == 0)
        populateScreen();
    }

    private void populateScreen() {
        entriesOnScreen.clear();
        entriesFromSelectedItemOnScreen.clear();
        if (currentScreen == VERSION) {
            entriesOnScreen.add(new Entry("Version", Tags.VERSION));
            entriesOnScreen.add(new Entry("Loaded compat profiles", SpoolCompat.compatSet.size()));
            entriesOnScreen.add(new Entry("Is obfuscated?", SpoolCompat.isObfuscated));
        } else if (currentScreen == SDB) {
            Entry topEntry = new Entry("SDB (Spool Database)");
            topEntry.add("Is running?", DebugConfig.fullCompatLogging);
            if (DebugConfig.fullCompatLogging) {
                topEntry.add(EnumChatFormatting.ITALIC + "Running on localhost:7655." + EnumChatFormatting.RESET);
                Entry connectionsEntry = new Entry("Connections allowed?", DebugConfig.allowSDBConnections);
                if (DebugConfig.allowSDBConnections) {
                    connectionsEntry
                        .add("Connections/Max Connections", SDBSocket.count() + "/" + DebugConfig.maxSDBConnections);
                }
                topEntry.add(connectionsEntry);
            }
            entriesOnScreen.add(topEntry);
        } else if (currentScreen == DISTANCE_AND_BLOBBING_STATS) {
            Entry distanceEntry = new Entry("Distance Threading Stats");
            if (ThreadsConfig.isDistanceThreadingEnabled()) {
                Entry playerStats = new Entry("Player executor stats");
                Object2IntMap<EntityPlayer> playerExecutorMap = DistanceThreadingUtil.getPlayerExecutorMap();
                for (EntityPlayer player : MinecraftServer.getServer()
                    .getConfigurationManager().playerEntityList) {
                    playerStats.add(player.getDisplayName(), playerExecutorMap.getInt(player));
                    Entry nearbyEntry = new Entry("Nearby");
                    DistanceThreadingUtil.Nearby nearby = DistanceThreadingUtil.cache
                        .getCachedNearestPlayer(player.worldObj, DistanceThreadingPlayerUtil.playerHashcode(player));
                    if (nearby == null) {
                        nearbyEntry.add(EnumChatFormatting.RED + "Recalculating caches..." + EnumChatFormatting.RESET);
                    } else {
                        nearbyEntry.add(
                            "Closest",
                            nearby.nearest() != null ? nearby.nearest()
                                .getDisplayName() : "None");
                        StringBuilder listOfNearby = new StringBuilder();
                        if (nearby.nearby()
                            .isEmpty()) {
                            listOfNearby.append("0: []");
                        } else {
                            listOfNearby.append(
                                nearby.nearby()
                                    .size())
                                .append(": [");
                            for (EntityPlayer nearbyPlayer : nearby.nearby()) {
                                listOfNearby.append(nearbyPlayer.getDisplayName());
                                listOfNearby.append(", ");
                            }
                            listOfNearby.delete(listOfNearby.length() - 2, listOfNearby.length());
                            listOfNearby.append("]");
                        }
                        nearbyEntry.add("All close", listOfNearby.toString());
                        nearbyEntry.add("Ignored limit?", nearby.usedIgnoreLimit());
                    }
                    playerStats.add(nearbyEntry);
                }
                distanceEntry.add(playerStats);

                Long2IntMap chunkExecutorMap = DistanceThreadingUtil.getChunkExecutorMap();
                Int2ObjectMap<LongSet> reverseChunkExecutorMap = new Int2ObjectOpenHashMap<>();
                for (Long2IntMap.Entry entry : chunkExecutorMap.long2IntEntrySet()) {
                    if (reverseChunkExecutorMap.containsKey(entry.getIntValue()))
                        reverseChunkExecutorMap.get(entry.getIntValue())
                            .add(entry.getLongKey());
                    else reverseChunkExecutorMap.put(entry.getIntValue(), LongArraySet.of(entry.getLongKey()));
                }

                Entry forcedChunkStats = new Entry("Forced chunk executor stats");

                for (Int2ObjectMap.Entry<LongSet> entry : reverseChunkExecutorMap.int2ObjectEntrySet()) {
                    Entry chunkEntry = new Entry("Executor", entry.getIntKey());
                    chunkEntry.add(
                        "Chunks covered",
                        entry.getValue()
                            .size());
                    forcedChunkStats.add(chunkEntry);
                }

                distanceEntry.add(forcedChunkStats);

                Entry cacheStats = new Entry("Cache data");
                cacheStats
                    .add("Forced chunk count across all worlds", DistanceThreadingUtil.cache.getAmountOfLoadedChunks());
                for (World world : MinecraftServer.getServer().worldServers) {
                    Entry worldEntry = new Entry("World", world.provider.dimensionId);
                    LongSet processedChunksSet = DistanceThreadingUtil.cache.getCachedProcessedChunks(world);
                    worldEntry
                        .add("Forced chunk cache size", processedChunksSet != null ? processedChunksSet.size() : 0);

                    NonBlockingHashMapLong<DistanceThreadingUtil.Nearby> nearbyPlayerList = DistanceThreadingUtil.cache
                        .getCachedNearestPlayerList(world);
                    worldEntry
                        .add("Nearby (player) cache size", nearbyPlayerList != null ? nearbyPlayerList.size() : 0);

                    NonBlockingHashMapLong<DistanceThreadingUtil.Nearby> nearbyChunkList = DistanceThreadingUtil.cache
                        .getCachedNearestPlayerList(world);
                    worldEntry.add("Nearby (chunk) cache size", nearbyChunkList != null ? nearbyChunkList.size() : 0);

                    cacheStats.add(worldEntry);
                }

                distanceEntry.add(cacheStats);

            } else {
                distanceEntry.add("Distance threading disabled.");
            }

            Entry blobEntry = new Entry("Chunk Blobbing Stats");
            blobEntry.column = 1;

            blobEntry.add("Blob radius", BlobConstants.RADIUS);
            blobEntry.add("Blob diameter", BlobConstants.DIAMETER);
            blobEntry.add("Blob sq. area", BlobConstants.AREA);

            for (World world : MinecraftServer.getServer().worldServers) {
                Entry worldEntry = new Entry("World", world.provider.dimensionId);
                if (world.getChunkProvider() instanceof ConcurrentChunkProviderServer chunkProviderServer) {
                    worldEntry.add("Blob count", chunkProviderServer.concurrentLoadedChunkHashMap.size());
                }
                blobEntry.add(worldEntry);
            }

            entriesOnScreen.add(distanceEntry);
            entriesOnScreen.add(blobEntry);
        } else {
            Entry topLevelStatsEntry = new Entry("Spool config");
            topLevelStatsEntry.add("Experimental threading", ThreadsConfig.isExperimentalThreadingEnabled());
            topLevelStatsEntry.add("Distance threading", ThreadsConfig.isDistanceThreadingEnabled());
            topLevelStatsEntry.add("Dimension threading", ThreadsConfig.isDimensionThreadingEnabled());
            topLevelStatsEntry.add("Entity AI threading", ThreadsConfig.isEntityAIThreadingEnabled());
            topLevelStatsEntry.add("Chunk threading", ThreadsConfig.isThreadedChunkLoadingEnabled());

            Entry topLevelManagerEntry = new Entry("Managers:");
            for (IThreadManager manager : SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS.values()) {
                Entry managerEntry = new Entry(
                    (manager.isPoolDisabled() ? EnumChatFormatting.RED : EnumChatFormatting.GREEN) + "Thread Manager",
                    manager.getName() + EnumChatFormatting.RESET);
                if (manager.isPoolDisabled()) {
                    managerEntry.add("Thread manager disabled.");
                    continue;
                }
                // managerEntry.add("Manager type", manager.getClass().getSimpleName());
                managerEntry.add("Thread count", manager.getNumThreads());

                if (!DebugConfig.debug) {
                    managerEntry.add("Additional information unavailable (debugging inactive).");
                    topLevelManagerEntry.add(managerEntry);
                    continue;
                }

                managerEntry.add(
                    "Time spent executing",
                    String.format(
                        "%.2f ms (avg: %.2f ms)",
                        manager.getTimeExecuting() / 1000000d,
                        manager.getAvgTimeExecuting() / 1000000d));

                // managerEntry.add("Time spent on overhead", String.format("%.2f ms (avg: %.2f ms)",
                // manager.getTimeOverhead() / 1000000d, manager.getAvgTimeOverhead() / 1000000d));

                managerEntry.add(
                    "Time spent waiting",
                    String.format(
                        "%.2f ms (avg: %.2f ms)",
                        manager.getTimeWaiting() / 1000000d,
                        manager.getAvgTimeWaiting() / 1000000d));

                managerEntry.add(
                    "Total time saved",
                    String.format(
                        "%.2f ms (avg: %.2f ms)",
                        (manager.getTimeExecuting() - manager.getTimeOverhead() - manager.getTimeWaiting()) / 1000000d,
                        (manager.getAvgTimeExecuting() - manager.getAvgTimeOverhead() - manager.getAvgTimeWaiting())
                            / 1000000d));

                topLevelManagerEntry.add(managerEntry);
            }
            Entry topLevelCacheEntry = new Entry("Caches:");

            for (Int2ObjectMap.Entry<RegisteredCache> cache : SpoolManagerOrchestrator.REGISTERED_CACHES
                .int2ObjectEntrySet()) {
                Entry cacheEntry = new Entry("Cache", ManagerNames.values()[cache.getIntKey()].getName());
                cacheEntry.add(
                    "Spool cache calculated size",
                    String.format(
                        "%.2fMB (%d Bytes)",
                        (double) cache.getValue()
                            .getCachedSize() / 1000000d,
                        cache.getValue()
                            .getCachedSize()));
                topLevelCacheEntry.add(cacheEntry);
            }

            entriesOnScreen.add(topLevelStatsEntry);
            entriesOnScreen.add(topLevelManagerEntry);
            entriesOnScreen.add(topLevelCacheEntry);

            if (currentScreen < THREAD_MANAGER_START_INDEX) return;

            IThreadManager manager = SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS
                .get(currentScreen - THREAD_MANAGER_START_INDEX);
            if (manager == null) {
                currentScreen = -1;
                return;
            }

            Entry managerEntry = new Entry("Thread Manager", manager.getName());
            if (manager instanceof ThreadManager threadManager) {
                Entry futuresEntry = new Entry("Futures information:");
                futuresEntry.add("Futures queue size", threadManager.futuresSize);
                futuresEntry.add("Overflow queue size", threadManager.overflowSize);
                managerEntry.add(futuresEntry);
            }

            if (manager instanceof KeyedPoolThreadManager kpThreadManager) {
                Entry keysEntry = new Entry("Keys information:");
                keysEntry.add(
                    "All keys",
                    Arrays.toString(
                        kpThreadManager.getAllKeys()
                            .toArray()));
                keysEntry.add(
                    "Used (mapped) keys",
                    Arrays.toString(
                        kpThreadManager.getKeys()
                            .toArray()));
                keysEntry.add(
                    "Remapped keys",
                    Arrays.toString(
                        kpThreadManager.getMappedKeys()
                            .toArray()));
                Entry loadEntry = new Entry("Load information:");
                if (manager instanceof LBKeyedPoolThreadManager lbkpThreadManager) {
                    for (int key : lbkpThreadManager.getAllKeys()) {
                        loadEntry.add(
                            "Load for thread " + key,
                            String.format("%.2f", lbkpThreadManager.getLoadFactorForKey(key)));
                    }
                    keysEntry.add(loadEntry);
                }
                managerEntry.add(keysEntry);
            }

            if (manager instanceof TimedOperationThreadManager toThreadManager) {
                Entry keysEntry = new Entry("Current tasks:");
                for (Long2LongMap.Entry entry : toThreadManager.taskMap.long2LongEntrySet()) {
                    long currentTime = System.currentTimeMillis();
                    long key = entry.getLongKey();
                    long value = entry.getLongValue();
                    Entry taskEntry = new Entry("Task scheduled", Math.abs(currentTime - key) + " ms ago");
                    taskEntry.add("Time delay", ((value - key) + " ms"));
                    taskEntry.add("Finish time", (value + " ms"));
                    taskEntry.add("Finishes in", ((value - currentTime) + " ms"));
                    keysEntry.add(taskEntry);
                }
                managerEntry.add(keysEntry);
            }
            entriesFromSelectedItemOnScreen.add(managerEntry);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public static class Entry {

        public List<Entry> nestedEntries = new ObjectArrayList<>();
        public String name;
        public String value;
        public int column = 0;

        public Entry(String name) {
            this.name = name;
            this.value = null;
        }

        public Entry(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public Entry(String name, int value) {
            this.name = name;
            this.value = String.valueOf(value);
        }

        public Entry(String name, boolean value) {
            this.name = name;
            this.value = String.valueOf(value);
        }

        public Entry(String name, double value) {
            this.name = name;
            this.value = String.valueOf(value);
        }

        public void add(Entry entry) {
            this.nestedEntries.add(entry);
        }

        public void add(String name) {
            this.nestedEntries.add(new Entry(name));
        }

        public void add(String name, String value) {
            this.nestedEntries.add(new Entry(name, value));
        }

        public void add(String name, int value) {
            this.nestedEntries.add(new Entry(name, value));
        }

        public void add(String name, boolean value) {
            this.nestedEntries.add(new Entry(name, value));
        }

        public void add(String name, double value) {
            this.nestedEntries.add(new Entry(name, value));
        }

        public int countOfEntriesRecursive() {
            int entries = 1;
            for (Entry nestedEntry : nestedEntries) {
                entries += nestedEntry.countOfEntriesRecursive();
            }
            return entries;
        }

        public String[] getStringsRecursive() {
            String[] strings = new String[countOfEntriesRecursive()];

            if (value == null) strings[0] = this.name;
            else strings[0] = String.format("%s: %s", this.name, this.value);

            int filledSlots = 1;

            for (Entry nestedEntry : nestedEntries) {
                String[] nested = nestedEntry.getStringsRecursive();

                for (int i = 0; i < nested.length; i++) {
                    nested[i] = "  " + nested[i];
                }

                System.arraycopy(nested, 0, strings, filledSlots, nested.length);
                filledSlots += nested.length;
            }

            return strings;
        }
    }
}
