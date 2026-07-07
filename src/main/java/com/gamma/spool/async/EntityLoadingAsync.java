package com.gamma.spool.async;

import net.minecraft.entity.Entity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

// Sometimes, when an entity is saved to a chunk, it can be ticked before being saved.
// If the entity is moving towards a chunk border *and* the entity is ticked *and* the
// entity moves across the chunk border before it is saved, it can cause a nasty
// log message and some odd artifacts relating to where an entity is in a loaded
// chunk. This is important to handle, which is what this class does.
public class EntityLoadingAsync {

    private static final Object2ObjectMap<World, Long2ObjectMap<ObjectSet<Entity>>> worlds = Object2ObjectMaps
        .synchronize(new Object2ObjectOpenHashMap<>());

    public static void onChunkLoad(World world, Chunk chunk) {
        synchronized (worlds) {
            Long2ObjectMap<ObjectSet<Entity>> map = worlds.get(world);
            if (map == null) return;
            long index = ChunkCoordIntPair.chunkXZ2Int(chunk.xPosition, chunk.zPosition);
            ObjectSet<Entity> set = map.get(index);
            if (set == null) return;
            for (Entity entity : set) {
                chunk.addEntity(entity);
            }
            map.remove(index);
            if (map.isEmpty()) worlds.remove(world);
        }
    }

    public static void addPendingEntity(Entity entity, int trueX, int trueZ) {
        synchronized (worlds) {
            Long2ObjectMap<ObjectSet<Entity>> map = worlds
                .computeIfAbsent(entity.worldObj, (_) -> new Long2ObjectOpenHashMap<>());
            ObjectSet<Entity> set = map
                .computeIfAbsent(ChunkCoordIntPair.chunkXZ2Int(trueX, trueZ), (_) -> new ObjectArraySet<>());
            set.add(entity);
        }
    }
}
