package com.gamma.spool.util.distance;

import net.minecraft.world.World;

import com.gamma.spool.api.statistics.ICache;
import com.gamma.spool.util.caching.CachedItem;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;

public class DistanceThreadingCache implements ICache {

    final CachedItem<Object2ObjectMap<World, LongOpenHashSet>> processedChunks = new CachedItem<>(null);
    final CachedItem<Object2ObjectMap<World, Long2ObjectMap<DistanceThreadingUtil.Nearby>>> nearestPlayerCache = new CachedItem<>(
        Object2ObjectMaps.synchronize(new Object2ObjectArrayMap<>()));
    final CachedItem<Object2ObjectMap<World, Long2ObjectMap<DistanceThreadingUtil.Nearby>>> nearestChunkCache = new CachedItem<>(
        Object2ObjectMaps.synchronize(new Object2ObjectArrayMap<>()));
    final CachedItem<Integer> amountLoadedChunks = new CachedItem<>(-1);

    public LongOpenHashSet getCachedProcessedChunk(World worldObj) {
        return this.processedChunks.getItem()
            .get(worldObj);
    }

    public void setCachedProcessedChunk(World worldObj, LongOpenHashSet value) {
        this.processedChunks.getItem()
            .put(worldObj, value);
    }

    public DistanceThreadingUtil.Nearby getCachedNearestPlayer(World worldObj, long key) {
        return this.nearestPlayerCache.getItem()
            .get(worldObj)
            .get(key);
    }

    public void setCachedNearestPlayer(World worldObj, long key, DistanceThreadingUtil.Nearby value) {
        this.nearestPlayerCache.getItem()
            .get(worldObj)
            .put(key, value);
    }

    public DistanceThreadingUtil.Nearby getCachedNearestChunk(World worldObj, long key) {
        return this.nearestChunkCache.getItem()
            .get(worldObj)
            .get(key);
    }

    public void setCachedNearestChunk(World worldObj, long key, DistanceThreadingUtil.Nearby value) {
        this.nearestChunkCache.getItem()
            .get(worldObj)
            .put(key, value);
    }

    @Override
    public void invalidate() {
        processedChunks.setItem(null); // One "chunk" of entries.
        nearestPlayerCache.getItem()
            .clear(); // Multiple separate entries.
        nearestChunkCache.getItem()
            .clear();
        amountLoadedChunks.setItem(-1);
    }

    @Override
    public CachedItem<?>[] getCacheItems() {
        return new CachedItem[] { processedChunks, nearestPlayerCache, nearestChunkCache, amountLoadedChunks };
    }

    @Override
    public String getNameForDebug() {
        return "DistanceThreadingCache";
    }
}
