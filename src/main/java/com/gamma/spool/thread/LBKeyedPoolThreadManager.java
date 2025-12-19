package com.gamma.spool.thread;

import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;

import org.jctools.maps.NonBlockingHashMapLong;

import com.gamma.spool.config.ThreadManagerConfig;
import com.gamma.spool.core.SpoolLogger;
import com.github.bsideup.jabel.Desugar;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.ObjectSet;

/**
 * Inherits the {@link KeyedPoolThreadManager} class and offers load-balancing capabilities.
 */
public class LBKeyedPoolThreadManager extends KeyedPoolThreadManager {

    final NonBlockingHashMapLong<String> keyNames = new NonBlockingHashMapLong<>();

    private final ObjectList<LoadData> loadFactorList = ObjectLists.synchronize(new ObjectArrayList<>(threadLimit - 1));
    private final Int2ObjectMap<DoubleSupplier> loadSupplierMap = Int2ObjectMaps
        .synchronize(new Int2ObjectOpenHashMap<>(threadLimit - 1));

    public LBKeyedPoolThreadManager(String name, int threadLimit) {
        super(name, threadLimit);
    }

    /**
     * Every x ticks, run once to recalculate the load for each thread (potentially intensive).
     */
    public void updatePool() {
        recalculateLoadFactors();
        balance();
        checkForMissingLoadFunctions();
    }

    protected void recalculateLoadFactors() {
        if (isDisabled) {
            return;
        }

        synchronized (loadFactorList) {
            loadFactorList.clear();

            ObjectSet<Int2ObjectMap.Entry<DoubleSupplier>> entrySet = loadSupplierMap.int2ObjectEntrySet();
            for (Int2ObjectMap.Entry<DoubleSupplier> entry : entrySet) {
                double load = entry.getValue()
                    .getAsDouble();
                if (!Double.isFinite(load)) throw new IllegalStateException(
                    "Load factor for thread " + entry.getIntKey() + " is not finite: " + load);
                loadFactorList.add(new LoadData(entry.getIntKey(), load));
            }
            loadFactorList.unstableSort(Comparator.reverseOrder());
        }

        // loadFactorQueue is now populated with the load factor data for each thread.
        // We can leave the queue in its place, using it for the later operations.
        // It will be cleared again at the beginning of this function whenever it runs again.
    }

    private void balance() {
        if (isDisabled) {
            return;
        }

        if (threads > threadLimit) {
            ObjectList<LoadData> prioritizedThreads = loadFactorList.subList(0, threadLimit - 1);
            prioritizedThreads.forEach(loadData -> moveKeyedThread(loadData.key, false));

            ObjectList<LoadData> toSort = new ObjectArrayList<>();
            for (long num : keyedPool.keySet()) {
                // Skip the default thread...
                if (num == DEFAULT_THREAD_KEY) continue;
                int key = (int) num;
                if (!prioritizedThreads.contains(new LoadData(key))) {
                    LoadData loadData = loadFactorList.get(loadFactorList.indexOf(new LoadData(key)));
                    toSort.add(loadData);
                }
            }
            toSort.unstableSort(Comparator.reverseOrder());
            for (LoadData loadData : toSort) {
                moveKeyedThread(loadData.key(), true);
            }
        }
    }

    protected void checkForMissingLoadFunctions() {
        for (LoadData thread : loadFactorList) {
            if (!this.doesKeyHaveLoadFunction(thread.key))
                throw new IllegalStateException("Thread " + thread.key + " has no load function assigned.");
        }
    }

    public void setLoadFunction(int key, DoubleSupplier loadFactorFunction) {
        loadSupplierMap.put(key, loadFactorFunction);
    }

    public boolean doesKeyHaveLoadFunction(int key) {
        return loadSupplierMap.containsKey(key);
    }

    public double getLoadFactorForKey(int key) {
        return loadSupplierMap.get(key)
            .getAsDouble();
    }

    protected void moveKeyedThread(int key, boolean moveToRemap) {
        mappedKeys = null;
        keys = null;
        allKeys = null;
        if (moveToRemap) {
            if (keyDefaultRemap.containsKey(key) || !keyedPool.containsKey(key)) return;
            addRemappedThread(key, keyNames.get(key));
            removeMappedThread(key);
        } else {
            if (!keyDefaultRemap.containsKey(key) || keyedPool.containsKey(key)) return;
            addMappedThread(key, keyDefaultRemap.get(key));
            removeRemappedThread(key);
        }
    }

    private void addMappedThread(int threadKey, String name) {
        keyedPool.put(
            threadKey,
            Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder().setNameFormat("Spool-" + name)
                    .build()));
        keyNames.put(threadKey, name);
        threads++;
    }

    private void addRemappedThread(int threadKey, String name) {
        keyDefaultRemap.put(threadKey, name);
        threads++;
    }

    private void removeRemappedThread(int threadKey) {
        if (keyDefaultRemap.containsKey(threadKey)) {
            keyDefaultRemap.remove(threadKey);
            mappedKeys = null;
            allKeys = null;
            threads--;
            // piss
            SpoolLogger.warn(
                "KeyedPoolThreadManager ({}) removed key ({}) inside remap list, tasks will not be stopped for this key.",
                this.getName(),
                threadKey);
        }
    }

    public void removeMappedThread(int threadKey) {
        ExecutorService executor = keyedPool.get(threadKey);
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor
                    .awaitTermination(ThreadManagerConfig.globalTerminatingSingleThreadTimeout, TimeUnit.MILLISECONDS))
                    executor.shutdownNow();
            } catch (InterruptedException e) {
                throw new RuntimeException("Thread termination interrupted: " + e.getMessage());
            }
            keyedPool.remove(threadKey);
            keyNames.remove(threadKey);
            keys = null;
            allKeys = null;
            threads--;
        }
    }

    @Override
    public void addKeyedThread(int threadKey, String name) {
        if (keyedPool.containsKey(threadKey)) return;
        if (threads >= threadLimit) {
            addRemappedThread(threadKey, name);
            mappedKeys = null;
            allKeys = null;
            return;
        }
        // Maybe performance loss? This won't happen very often, so not a big problem.
        addMappedThread(threadKey, name);
        keys = null;
        allKeys = null;
    }

    @Override
    public void removeKeyedThread(int threadKey) {
        loadSupplierMap.remove(threadKey);

        ExecutorService executor = keyedPool.get(threadKey);
        if (executor == null) {
            if (keyDefaultRemap.containsKey(threadKey)) {
                keyDefaultRemap.remove(threadKey);
                mappedKeys = null;
                allKeys = null;
                threads--;
                // piss
                SpoolLogger.warn(
                    "KeyedPoolThreadManager ({}) removed key ({}) inside remap list, tasks will not be stopped for this key.",
                    this.getName(),
                    threadKey);
            }
            return;
        } else {
            executor.shutdown();
            try {
                if (!executor
                    .awaitTermination(ThreadManagerConfig.globalTerminatingSingleThreadTimeout, TimeUnit.MILLISECONDS))
                    executor.shutdownNow();
            } catch (InterruptedException e) {
                throw new RuntimeException("Thread termination interrupted: " + e.getMessage());
            }
            keyedPool.remove(threadKey);
            keyNames.remove(threadKey);
            keys = null;
            allKeys = null;
        }
        threads--;
    }

    @Desugar
    private record LoadData(int key, double load) implements Comparable<LoadData> {

        LoadData(int key) {
            this(key, 0.0d);
        }

        @Override
        public int compareTo(LoadData o) {
            int compared = Double.compare(this.load, o.load);
            if (compared == 0) return Integer.compare(o.key, this.key);
            else return compared;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof LoadData)) return false;
            return this.key == ((LoadData) obj).key;
        }
    }
}
