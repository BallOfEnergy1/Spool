package com.gamma.spool.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.function.Consumer;

import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

// This class aims to fix the absolute monstrosity that is the `pendingTickListEntriesHashSet` and
// `pendingTickListEntriesTreeSet`
// fields in the WorldServer class by combining them into one access class.

// Thanks Mojank for making my life a living hell.

public class PendingTickList<V> implements SortedSet<V> {

    // Set for storing data.
    private final ObjectAVLTreeSet<V> set = new ObjectAVLTreeSet<>();
    // Map for storing hashes.
    private final ObjectOpenHashSet<V> hashSet = new ObjectOpenHashSet<>();

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public void forEach(Consumer<? super V> action) {
        set.forEach(action);
    }

    @Override
    public boolean contains(Object o) {
        // ObjectOpenHashSet implementation with some tweaks to allow for faster `contains()` operations on a standard
        // sorted tree by working
        // with the keys stored in the map.
        // This is broken and I have no idea why!
        /*
         * if (o == null) return set.containsValue(null);
         * Object curr;
         * final Integer[] key = set.keySet().toArray(new Integer[0]);
         * int pos;
         * // The starting point.
         * if (((curr = key[pos = (it.unimi.dsi.fastutil.HashCommon.mix(o.hashCode())) & (set.size() - 1)]) == null))
         * return false;
         * if (o.equals(curr)) return true;
         * while (true) {
         * if ((curr = key[pos = (pos + 1) & (set.size() - 1)]) == null) return false;
         * if (o.equals(curr)) return true;
         * }
         */
        return hashSet.contains(o);
    }

    @Override
    public ObjectIterator<V> iterator() {
        return new ObjectIterator<>() {

            private int size = set.size();
            private final ObjectIterator<V> maskedIterator = set.iterator();

            @Override
            public boolean hasNext() {
                return size != 0;
            }

            @Override
            public V next() {
                size--;
                return maskedIterator.next();
            }
        };
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
    public synchronized boolean add(V v) {
        hashSet.add(v);
        return set.add(v);
    }

    @Override
    public synchronized boolean remove(Object o) {
        hashSet.remove(o);
        return set.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return hashSet.containsAll(c);
    }

    @Override
    public synchronized boolean addAll(Collection<? extends V> c) {
        hashSet.addAll(c);
        return set.addAll(c);
    }

    @Override
    public synchronized boolean retainAll(Collection<?> c) {
        hashSet.retainAll(c);
        return set.retainAll(c);
    }

    @Override
    public synchronized boolean removeAll(Collection<?> c) {
        hashSet.removeAll(c);
        return set.removeAll(c);
    }

    @Override
    public synchronized void clear() {
        hashSet.clear();
        set.clear();
    }

    @Override
    public Comparator<? super V> comparator() {
        throw new UnsupportedOperationException("comparator on PendingTickList");
    }

    @Override
    public SortedSet<V> subSet(V fromElement, V toElement) {
        throw new UnsupportedOperationException("subSet on PendingTickList");
    }

    @Override
    public SortedSet<V> headSet(V toElement) {
        throw new UnsupportedOperationException("headSet on PendingTickList");
    }

    @Override
    public SortedSet<V> tailSet(V fromElement) {
        throw new UnsupportedOperationException("tailSet on PendingTickList");
    }

    @Override
    public V first() {
        return set.first();
    }

    @Override
    public V last() {
        return set.last();
    }
}
