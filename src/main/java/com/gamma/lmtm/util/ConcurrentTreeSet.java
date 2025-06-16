package com.gamma.lmtm.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeSet;

public class ConcurrentTreeSet<V> extends TreeSet<V> {

    @Override
    public NavigableSet<V> tailSet(V fromElement, boolean inclusive) {
        synchronized (this) {
            return super.tailSet(fromElement, inclusive);
        }
    }

    @Override
    public SortedSet<V> tailSet(V fromElement) {
        synchronized (this) {
            return super.tailSet(fromElement);
        }
    }

    @Override
    public SortedSet<V> subSet(V fromElement, V toElement) {
        synchronized (this) {
            return super.subSet(fromElement, toElement);
        }
    }

    @Override
    public NavigableSet<V> subSet(V fromElement, boolean fromInclusive, V toElement, boolean toInclusive) {
        synchronized (this) {
            return super.subSet(fromElement, fromInclusive, toElement, toInclusive);
        }
    }

    @Override
    public Spliterator<V> spliterator() {
        synchronized (this) {
            return super.spliterator();
        }
    }

    @Override
    public int size() {
        synchronized (this) {
            return super.size();
        }
    }

    @Override
    public boolean remove(Object o) {
        synchronized (this) {
            return super.remove(o);
        }
    }

    @Override
    public V pollLast() {
        synchronized (this) {
            return super.pollLast();
        }
    }

    @Override
    public V pollFirst() {
        synchronized (this) {
            return super.pollFirst();
        }
    }

    @Override
    public V lower(V v) {
        synchronized (this) {
            return super.lower(v);
        }
    }

    @Override
    public V last() {
        synchronized (this) {
            return super.last();
        }
    }

    @Override
    public boolean isEmpty() {
        synchronized (this) {
            return super.isEmpty();
        }
    }

    @Override
    public V higher(V v) {
        synchronized (this) {
            return super.higher(v);
        }
    }

    @Override
    public NavigableSet<V> headSet(V toElement, boolean inclusive) {
        synchronized (this) {
            return super.headSet(toElement, inclusive);
        }
    }

    @Override
    public SortedSet<V> headSet(V toElement) {
        synchronized (this) {
            return super.headSet(toElement);
        }
    }

    @Override
    public V floor(V v) {
        synchronized (this) {
            return super.floor(v);
        }
    }

    @Override
    public V first() {
        synchronized (this) {
            return super.first();
        }
    }

    @Override
    public NavigableSet<V> descendingSet() {
        synchronized (this) {
            return super.descendingSet();
        }
    }

    @Override
    public Iterator<V> descendingIterator() {
        synchronized (this) {
            return super.descendingIterator();
        }
    }

    @Override
    public boolean contains(Object o) {
        synchronized (this) {
            return super.contains(o);
        }
    }

    @Override
    public Comparator<? super V> comparator() {
        synchronized (this) {
            return super.comparator();
        }
    }

    @Override
    public void clear() {
        synchronized (this) {
            super.clear();
        }
    }

    @Override
    public V ceiling(V v) {
        synchronized (this) {
            return super.ceiling(v);
        }
    }

    @Override
    public boolean addAll(Collection<? extends V> c) {
        synchronized (this) {
            return super.addAll(c);
        }
    }

    @Override
    public boolean add(V v) {
        synchronized (this) {
            return super.add(v);
        }
    }
}
