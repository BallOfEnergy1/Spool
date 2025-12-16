package com.gamma.spool.gui;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;

import com.gamma.spool.Tags;
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

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class GuiStats extends GuiScreen {

    private static final int STATS = -1;
    private static final int VERSION = 0;
    private static final int SDB = 1;
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
        int line = 0;
        for (Entry entry : entriesOnScreen) {
            String[] strings = entry.getStringsRecursive();
            for (String string : strings) {
                this.drawString(
                    this.fontRendererObj,
                    string,
                    20,
                    80 + (line++ * (5 + this.fontRendererObj.FONT_HEIGHT)),
                    0xFFFFFF);
            }
        }

        entriesFromSelectedItemOnScreen.removeIf(Objects::isNull);
        line = 0;
        for (Entry entry : entriesFromSelectedItemOnScreen) {
            String[] strings = entry.getStringsRecursive();
            for (String string : strings) {
                int y = (numManagerButtons * (20 + 5)) + 80 + (line++ * (5 + this.fontRendererObj.FONT_HEIGHT));
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
                if (manager instanceof ForkThreadManager || manager instanceof TimedOperationThreadManager) continue;
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
            topEntry.nestedEntries.add(new Entry("Is running? ", DebugConfig.fullCompatLogging));
            if (DebugConfig.fullCompatLogging) {
                topEntry.nestedEntries.add(
                    new Entry(EnumChatFormatting.ITALIC + "Running on localhost:7655." + EnumChatFormatting.RESET));
                Entry connectionsEntry = new Entry("Connections allowed? ", DebugConfig.allowSDBConnections);
                if (DebugConfig.allowSDBConnections) {
                    connectionsEntry.nestedEntries.add(
                        new Entry(
                            "Connections/Max Connections: ",
                            SDBSocket.count() + "/" + DebugConfig.maxSDBConnections));
                }
                topEntry.nestedEntries.add(connectionsEntry);
            }
            entriesOnScreen.add(topEntry);
        } else {
            Entry topLevelStatsEntry = new Entry("Spool config");
            topLevelStatsEntry.nestedEntries
                .add(new Entry("Experimental threading", ThreadsConfig.isExperimentalThreadingEnabled()));
            topLevelStatsEntry.nestedEntries
                .add(new Entry("Distance threading", ThreadsConfig.isDistanceThreadingEnabled()));
            topLevelStatsEntry.nestedEntries
                .add(new Entry("Dimension threading", ThreadsConfig.isDimensionThreadingEnabled()));
            topLevelStatsEntry.nestedEntries
                .add(new Entry("Entity AI threading", ThreadsConfig.isEntityAIThreadingEnabled()));
            topLevelStatsEntry.nestedEntries
                .add(new Entry("Chunk threading", ThreadsConfig.isThreadedChunkLoadingEnabled()));

            Entry topLevelManagerEntry = new Entry("Managers:");
            for (IThreadManager manager : SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS.values()) {
                Entry managerEntry = new Entry("Thread Manager", manager.getName());
                managerEntry.nestedEntries.add(
                    new Entry(
                        "Manager type",
                        manager.getClass()
                            .getSimpleName()));
                managerEntry.nestedEntries.add(new Entry("Thread count", manager.getNumThreads()));

                if (!DebugConfig.debug) {
                    managerEntry.nestedEntries
                        .add(new Entry("Additional information unavailable (debugging inactive)."));
                    topLevelManagerEntry.nestedEntries.add(managerEntry);
                    continue;
                }

                managerEntry.nestedEntries.add(
                    new Entry(
                        "Time spent executing",
                        String.format(
                            "%.2f ms (avg: %.2f ms)",
                            manager.getTimeExecuting() / 1000000d,
                            manager.getAvgTimeExecuting() / 1000000d)));

                managerEntry.nestedEntries.add(
                    new Entry(
                        "Time spent on overhead",
                        String.format(
                            "%.2f ms (avg: %.2f ms)",
                            manager.getTimeOverhead() / 1000000d,
                            manager.getAvgTimeOverhead() / 1000000d)));

                managerEntry.nestedEntries.add(
                    new Entry(
                        "Time spent waiting",
                        String.format(
                            "%.2f ms (avg: %.2f ms)",
                            manager.getTimeWaiting() / 1000000d,
                            manager.getAvgTimeWaiting() / 1000000d)));

                managerEntry.nestedEntries.add(
                    new Entry(
                        "Total time saved",
                        String.format(
                            "%.2f ms (avg: %.2f ms)",
                            (manager.getTimeExecuting() - manager.getTimeOverhead() - manager.getTimeWaiting())
                                / 1000000d,
                            (manager.getAvgTimeExecuting() - manager.getAvgTimeOverhead() - manager.getAvgTimeWaiting())
                                / 1000000d)));

                topLevelManagerEntry.nestedEntries.add(managerEntry);
            }
            Entry topLevelCacheEntry = new Entry("Caches:");

            for (Int2ObjectMap.Entry<RegisteredCache> cache : SpoolManagerOrchestrator.REGISTERED_CACHES
                .int2ObjectEntrySet()) {
                Entry cacheEntry = new Entry("Cache", ManagerNames.values()[cache.getIntKey()].getName());
                cacheEntry.nestedEntries.add(
                    new Entry(
                        "Spool cache calculated size",
                        String.format(
                            "%.2fMB (%d Bytes)",
                            (double) cache.getValue()
                                .getCachedSize() / 1000000d,
                            cache.getValue()
                                .getCachedSize())));
                topLevelCacheEntry.nestedEntries.add(cacheEntry);
            }

            entriesOnScreen.add(topLevelStatsEntry);
            entriesOnScreen.add(topLevelManagerEntry);
            entriesOnScreen.add(topLevelCacheEntry);

            if (currentScreen < THREAD_MANAGER_START_INDEX) return;

            IThreadManager manager = SpoolManagerOrchestrator.REGISTERED_THREAD_MANAGERS
                .get(currentScreen - THREAD_MANAGER_START_INDEX);
            Entry managerEntry = new Entry("Thread Manager", manager.getName());
            if (manager instanceof ThreadManager) {
                Entry futuresEntry = new Entry("Futures information:");
                futuresEntry.nestedEntries.add(new Entry("Futures queue size", ((ThreadManager) manager).futuresSize));
                futuresEntry.nestedEntries
                    .add(new Entry("Overflow queue size", ((ThreadManager) manager).overflowSize));
                managerEntry.nestedEntries.add(futuresEntry);
            } else if (manager instanceof KeyedPoolThreadManager) {
                Entry keysEntry = new Entry("Keys information:");
                keysEntry.nestedEntries.add(
                    new Entry(
                        "All keys",
                        Arrays.toString(
                            ((KeyedPoolThreadManager) manager).getAllKeys()
                                .toArray())));
                keysEntry.nestedEntries.add(
                    new Entry(
                        "Used (mapped) keys",
                        Arrays.toString(
                            ((KeyedPoolThreadManager) manager).getKeys()
                                .toArray())));
                keysEntry.nestedEntries.add(
                    new Entry(
                        "Remapped keys",
                        Arrays.toString(
                            ((KeyedPoolThreadManager) manager).getMappedKeys()
                                .toArray())));
                Entry loadEntry = new Entry("Load information:");
                if (manager instanceof LBKeyedPoolThreadManager) {
                    for (int key : ((LBKeyedPoolThreadManager) manager).getAllKeys()) {
                        loadEntry.nestedEntries.add(
                            new Entry(
                                "Load for thread " + key,
                                String.format("%.2f", ((LBKeyedPoolThreadManager) manager).getLoadFactorForKey(key))));
                    }
                    keysEntry.nestedEntries.add(loadEntry);
                }
                managerEntry.nestedEntries.add(keysEntry);
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
