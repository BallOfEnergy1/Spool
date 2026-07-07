package com.gamma.spool.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.function.Consumer;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.NextTickListEntry;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

// This class aims to fix the absolute monstrosity that is the `pendingTickListEntriesHashSet` and
// `pendingTickListEntriesTreeSet`
// fields in the WorldServer class by combining them into one access class.

// Thanks Mojank for making my life a living hell.

@SuppressWarnings("NullableProblems")
public class PendingTickList implements SortedSet<NextTickListEntry> {

    // Set for storing data.
    private final ObjectAVLTreeSet<NextTickListEntry> set = new ObjectAVLTreeSet<>();
    // Map for storing hashes.
    // TODO: Profile if this hashset increases performance much at all
    // since `contains` is potentially performant enough on the ObjectAVLTreeSet already.
    // private final ObjectOpenHashSet<NextTickListEntry> hashSet = new ObjectOpenHashSet<>();

    // Hodgepodge
    private Long2ObjectOpenHashMap<ObjectOpenHashSet<NextTickListEntry>> hodgepodge$tickIndex;

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public void forEach(Consumer<? super NextTickListEntry> action) {
        set.forEach(action);
    }

    @Override
    public boolean contains(Object o) {
        // return hashSet.contains(o);
        return set.contains(o);
    }

    @Override
    public ObjectIterator<NextTickListEntry> iterator() {
        return set.iterator();
    }

    @Override
    public Object[] toArray() {
        return set.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return set.toArray(a);
    }

    @Override
    public synchronized boolean add(NextTickListEntry v) {
        // hashSet.add(v);
        final long key = ChunkCoordIntPair.chunkXZ2Int(v.xCoord >> 4, v.zCoord >> 4);
        this.hodgepodge$getTickIndex()
            .computeIfAbsent(key, _ -> new ObjectOpenHashSet<>())
            .add(v);
        return set.add(v);
    }

    @Override
    public synchronized boolean remove(Object o) {
        // hashSet.remove(o);
        final NextTickListEntry entry = (NextTickListEntry) o;
        final var tickIndex = hodgepodge$getTickIndex();
        final long key = ChunkCoordIntPair.chunkXZ2Int(entry.xCoord >> 4, entry.zCoord >> 4);
        final ObjectOpenHashSet<NextTickListEntry> bucket = tickIndex.get(key);
        if (bucket != null) {
            bucket.remove(entry);
            if (bucket.isEmpty()) tickIndex.remove(key);
        }
        return set.remove(o);
    }

    public synchronized void removeRaw(NextTickListEntry o) {
        // hashSet.remove(o);
        set.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        // return hashSet.containsAll(c);
        return set.containsAll(c);
    }

    @Override
    public synchronized boolean addAll(Collection<? extends NextTickListEntry> c) {
        // hashSet.addAll(c);
        return set.addAll(c);
    }

    @Override
    public synchronized boolean retainAll(Collection<?> c) {
        // hashSet.retainAll(c);
        return set.retainAll(c);
    }

    @Override
    public synchronized boolean removeAll(Collection<?> c) {
        // hashSet.removeAll(c);
        return set.removeAll(c);
    }

    @Override
    public synchronized void clear() {
        // hashSet.clear();
        set.clear();
    }

    @Override
    public Comparator<? super NextTickListEntry> comparator() {
        throw new UnsupportedOperationException("comparator on PendingTickList");
    }

    @Override
    public SortedSet<NextTickListEntry> subSet(NextTickListEntry fromElement, NextTickListEntry toElement) {
        throw new UnsupportedOperationException("subSet on PendingTickList");
    }

    @Override
    public SortedSet<NextTickListEntry> headSet(NextTickListEntry toElement) {
        throw new UnsupportedOperationException("headSet on PendingTickList");
    }

    @Override
    public SortedSet<NextTickListEntry> tailSet(NextTickListEntry fromElement) {
        throw new UnsupportedOperationException("tailSet on PendingTickList");
    }

    @Override
    public NextTickListEntry first() {
        return set.first();
    }

    @Override
    public NextTickListEntry last() {
        return set.last();
    }

    // Hodgepodge
    public Long2ObjectOpenHashMap<ObjectOpenHashSet<NextTickListEntry>> hodgepodge$getTickIndex() {
        if (hodgepodge$tickIndex == null) {
            hodgepodge$tickIndex = new Long2ObjectOpenHashMap<>();
        }
        return hodgepodge$tickIndex;
    }
}
