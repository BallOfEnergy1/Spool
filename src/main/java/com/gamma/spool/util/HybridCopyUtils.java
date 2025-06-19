package com.gamma.spool.util;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import com.gamma.spool.Spool;

import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

/**
 * This class is dedicated to creating "Hybrid Copies" of collections, or in other words, a compromise between deep
 * copies and shallow copies.
 * <p>
 * This means that the collection object itself is a newly created object, however the objects inside the collection are
 * references to the elements of the original collection.
 * <p>
 * This allows for creating an "unmodifiable collection" in a more concurrent fashion, where operations do not
 * read-through/write-through to the original collection.
 * <p>
 * These are not super expensive operations, however they *do* involve a full instantiation of a collection *and* a copy
 * from one list to another, which can slow down significantly for sorted sets.
 */
public class HybridCopyUtils {

    // Fucking horrifying...
    private static Object getNewInstance(Object setToGetFrom) {
        try {
            Constructor<?> constructor = setToGetFrom.getClass()
                .getConstructor();
            return constructor.newInstance();
        } catch (Exception e) {
            Spool.logger.error(
                "Failed to get a new array instance for class {}, using default for type.",
                setToGetFrom.getClass()
                    .getSimpleName());
            return null;
        }
    }

    /**
     * Performs a "Hybrid Copy" of a list.
     */
    public static <V> List<V> hybridCopy(List<V> listToCopy) {
        @SuppressWarnings("unchecked")
        List<V> newList = (List<V>) getNewInstance(listToCopy); // Unchecked because of the nature of
                                                                // `getNewInstance(Object)`
        if (newList == null) newList = new ObjectArrayList<>(listToCopy);
        else newList.addAll(listToCopy);
        return newList;
    }

    /**
     * Performs a "Hybrid Copy" of a set.
     */
    public static <V> Set<V> hybridCopy(Set<V> setToCopy) {
        @SuppressWarnings("unchecked")
        Set<V> newList = (Set<V>) getNewInstance(setToCopy); // Unchecked because of the nature of
                                                             // `getNewInstance(Object)`
        if (newList == null) newList = new ObjectArraySet<>(setToCopy);
        else newList.addAll(setToCopy);
        return newList;
    }

    /**
     * Performs a "Hybrid Copy" of a sorted set.
     */
    public static <V> SortedSet<V> hybridCopy(SortedSet<V> setToCopy) {
        @SuppressWarnings("unchecked")
        SortedSet<V> newList = (SortedSet<V>) getNewInstance(setToCopy); // Unchecked because of the nature of
                                                                         // `getNewInstance(Object)`
        if (newList == null) newList = new ObjectAVLTreeSet<>(setToCopy);
        else newList.addAll(setToCopy);
        return newList;
    }

    /**
     * Performs a "Hybrid Copy" of a synchronized set.
     */
    public static <V> ObjectList<V> hybridCopy(ObjectLists.SynchronizedList<V> setToCopy) {
        ObjectList<V> newList = ObjectLists.synchronize(new ObjectArrayList<>());
        newList.addAll(setToCopy);
        return newList;
    }

    /**
     * Performs a "Hybrid Copy" of an unmodifiable set.
     */
    public static <V> ObjectList<V> hybridCopy(ObjectLists.UnmodifiableList<V> setToCopy) {
        ObjectList<V> newList = ObjectLists.unmodifiable(new ObjectArrayList<>());
        newList.addAll(setToCopy);
        return newList;
    }
}
