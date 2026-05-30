package com.gamma.spool.concurrent.async;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class ImmediateFallAsync {

    private static final Object2ObjectMap<World, LongSet> worlds = Object2ObjectMaps
        .synchronize(new Object2ObjectOpenHashMap<>());

    public static boolean isLocationImmediateFall(World worldObj, int x, int z) {
        LongSet set = worlds.get(worldObj);
        if (set == null) return false;
        return set.contains(ChunkCoordIntPair.chunkXZ2Int(x, z));
    }

    public static void addImmediateFall(World worldObj, int x, int z) {
        LongSet set = worlds.get(worldObj);
        if (set == null) worlds.put(worldObj, set = LongSets.synchronize(new LongOpenHashSet()));
        set.add(ChunkCoordIntPair.chunkXZ2Int(x, z));
    }

    public static void removeImmediateFall(World worldObj, int x, int z) {
        LongSet set = worlds.get(worldObj);
        if (set == null) return;
        set.remove(ChunkCoordIntPair.chunkXZ2Int(x, z));
    }
}
