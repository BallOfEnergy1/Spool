package com.gamma.spool.util.distance;

import com.gamma.spool.util.caching.CachedItem;
import com.gamma.spool.util.caching.ICache;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class DistanceThreadingCache implements ICache {

    final CachedItem<LongOpenHashSet> processedChunks = new CachedItem<>(null);
    final CachedItem<Long2ObjectMap<DistanceThreadingUtil.Nearby>> nearestPlayerCache = new CachedItem<>(
        Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>()));
    final CachedItem<Long2ObjectMap<DistanceThreadingUtil.Nearby>> nearestChunkCache = new CachedItem<>(
        Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>()));
    final CachedItem<Integer> amountLoadedChunks = new CachedItem<>(-1);

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
    public CachedItem<?>[] getCachesForDebug() {
        return new CachedItem[] { processedChunks, nearestPlayerCache, nearestChunkCache, amountLoadedChunks };
    }

    @Override
    public String getNameForDebug() {
        return "DistanceThreadingCache";
    }
}
