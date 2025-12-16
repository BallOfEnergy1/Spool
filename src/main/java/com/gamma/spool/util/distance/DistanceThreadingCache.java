package com.gamma.spool.util.distance;

import java.util.Map;

import net.minecraft.world.World;

import org.jctools.maps.NonBlockingHashMap;
import org.jctools.maps.NonBlockingHashMapLong;

import com.gamma.spool.api.statistics.ICache;
import com.gamma.spool.api.statistics.ICachedItem;
import com.gamma.spool.util.caching.CachedInt;
import com.gamma.spool.util.caching.CachedItem;

import it.unimi.dsi.fastutil.longs.LongSet;

public class DistanceThreadingCache implements ICache {

    // TODO: Make these caches last across multiple ticks.
    // This would be a large milestone, as it would greatly reduce the required calculations for each tick.
    // Normally, these only last for a single tick, then they are wiped.
    // If these could last across ticks (within reason), near-zero overhead could be achieved.

    final CachedItem<NonBlockingHashMap<World, LongSet>> processedChunks = new CachedItem<>(new NonBlockingHashMap<>());

    final CachedItem<NonBlockingHashMap<World, NonBlockingHashMapLong<DistanceThreadingUtil.Nearby>>> nearestPlayerCache = new CachedItem<>(
        new NonBlockingHashMap<>());

    final CachedItem<NonBlockingHashMap<World, NonBlockingHashMapLong<DistanceThreadingUtil.Nearby>>> nearestChunkCache = new CachedItem<>(
        new NonBlockingHashMap<>());

    final CachedInt amountLoadedChunks = new CachedInt(-1);

    public LongSet getCachedProcessedChunk(World worldObj) {
        return this.processedChunks.getItem()
            .get(worldObj);
    }

    public void setCachedProcessedChunk(World worldObj, LongSet value) {
        this.processedChunks.getItem()
            .put(worldObj, value);
    }

    public NonBlockingHashMapLong<DistanceThreadingUtil.Nearby> getCachedNearestPlayerList(World worldObj) {
        return this.nearestPlayerCache.getItem()
            .get(worldObj);
    }

    public DistanceThreadingUtil.Nearby getCachedNearestPlayer(World worldObj, long key) {
        NonBlockingHashMapLong<DistanceThreadingUtil.Nearby> map = this.nearestPlayerCache.getItem()
            .get(worldObj);

        if (map == null) return null;

        return map.get(key);
    }

    public void setCachedNearestPlayer(World worldObj, long key, DistanceThreadingUtil.Nearby value) {
        NonBlockingHashMap<World, NonBlockingHashMapLong<DistanceThreadingUtil.Nearby>> cachedItem = this.nearestPlayerCache
            .getItem();
        NonBlockingHashMapLong<DistanceThreadingUtil.Nearby> map = cachedItem.get(worldObj);

        if (map == null) cachedItem.put(worldObj, map = new NonBlockingHashMapLong<>());

        map.put(key, value);
    }

    public NonBlockingHashMapLong<DistanceThreadingUtil.Nearby> getCachedNearestChunkList(World worldObj) {
        return this.nearestChunkCache.getItem()
            .get(worldObj);
    }

    public DistanceThreadingUtil.Nearby getCachedNearestChunk(World worldObj, long key) {
        NonBlockingHashMapLong<DistanceThreadingUtil.Nearby> map = this.nearestChunkCache.getItem()
            .get(worldObj);

        if (map == null) return null;

        return map.get(key);
    }

    public void setCachedNearestChunk(World worldObj, long key, DistanceThreadingUtil.Nearby value) {
        NonBlockingHashMap<World, NonBlockingHashMapLong<DistanceThreadingUtil.Nearby>> cachedItem = this.nearestChunkCache
            .getItem();
        NonBlockingHashMapLong<DistanceThreadingUtil.Nearby> map = cachedItem.get(worldObj);

        if (map == null) cachedItem.put(worldObj, map = new NonBlockingHashMapLong<>());

        map.put(key, value);
    }

    public int getAmountOfLoadedChunks() {
        return this.amountLoadedChunks.getIntItem();
    }

    @Override
    public void invalidate() {
        processedChunks.getItem()
            .clear();
        nearestPlayerCache.getItem()
            .clear();
        nearestChunkCache.getItem()
            .clear();
        amountLoadedChunks.setIntItem(-1);
    }

    @Override
    public ICachedItem<?>[] getCacheItems() {
        return new ICachedItem[] { processedChunks, nearestPlayerCache, nearestChunkCache, amountLoadedChunks };
    }

    @Override
    public int getCount() {
        int count = 1;
        for (Map.Entry<World, LongSet> entry : processedChunks.getItem()
            .entrySet()) {
            count++;
            count += entry.getValue()
                .size();
        }
        for (Map.Entry<World, NonBlockingHashMapLong<DistanceThreadingUtil.Nearby>> entry : nearestPlayerCache.getItem()
            .entrySet()) {
            count++;
            count += entry.getValue()
                .size();
        }
        for (Map.Entry<World, NonBlockingHashMapLong<DistanceThreadingUtil.Nearby>> entry : nearestChunkCache.getItem()
            .entrySet()) {
            count++;
            count += entry.getValue()
                .size();
        }
        return count;
    }

    @Override
    public String getNameForDebug() {
        return "DistanceThreadingCache";
    }
}
