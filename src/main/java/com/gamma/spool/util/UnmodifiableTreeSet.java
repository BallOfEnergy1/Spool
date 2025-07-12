package com.gamma.spool.util;

import java.util.Collection;
import java.util.TreeSet;
import java.util.function.Predicate;

@SuppressWarnings("NullableProblems")
public class UnmodifiableTreeSet<V> extends TreeSet<V> {

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends V> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(V v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeIf(Predicate<? super V> filter) {
        throw new UnsupportedOperationException();
    }
}
