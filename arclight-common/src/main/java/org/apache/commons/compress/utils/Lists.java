package org.apache.commons.compress.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Compatibility shim for third-party mods that incorrectly reference
 * org.apache.commons.compress.utils.Lists, which doesn't actually exist
 * in the Apache Commons Compress library.
 * 
 * This class provides basic List utility methods to maintain compatibility
 * with mods like kaleidoscopecookery that have this incorrect dependency.
 * 
 * @author Arclight Team
 */
public class Lists {

    /**
     * Creates a new ArrayList instance.
     *
     * @param <E> the element type
     * @return a new, empty ArrayList
     */
    public static <E> ArrayList<E> newArrayList() {
        return new ArrayList<>();
    }

    /**
     * Creates a new ArrayList with the given initial capacity.
     *
     * @param <E> the element type
     * @param initialCapacity the initial capacity
     * @return a new ArrayList with specified initial capacity
     */
    public static <E> ArrayList<E> newArrayListWithCapacity(int initialCapacity) {
        return new ArrayList<>(initialCapacity);
    }

    /**
     * Creates a new ArrayList containing the given elements.
     *
     * @param <E> the element type
     * @param elements the elements to add to the list
     * @return a new ArrayList containing the elements
     */
    @SafeVarargs
    public static <E> ArrayList<E> newArrayList(E... elements) {
        ArrayList<E> list = new ArrayList<>(elements.length);
        Collections.addAll(list, elements);
        return list;
    }

    /**
     * Creates a new ArrayList containing the elements from the given iterable.
     *
     * @param <E> the element type
     * @param elements the elements to add to the list
     * @return a new ArrayList containing the elements
     */
    public static <E> ArrayList<E> newArrayList(Iterable<? extends E> elements) {
        if (elements instanceof ArrayList) {
            return new ArrayList<>((ArrayList<? extends E>) elements);
        }
        // Pre-size the list if we know the size
        if (elements instanceof java.util.Collection) {
            return new ArrayList<>((java.util.Collection<? extends E>) elements);
        }
        ArrayList<E> list = new ArrayList<>();
        for (E element : elements) {
            list.add(element);
        }
        return list;
    }

    /**
     * Returns an unmodifiable view of the specified list.
     *
     * @param <E> the element type
     * @param list the list to wrap
     * @return an unmodifiable view of the list
     */
    public static <E> List<E> unmodifiableList(List<? extends E> list) {
        return Collections.unmodifiableList(list);
    }

    /**
     * Returns a fixed-size list backed by the specified array.
     *
     * @param <E> the element type
     * @param elements the array to back the list
     * @return a list view of the array
     */
    @SafeVarargs
    public static <E> List<E> asList(E... elements) {
        return Arrays.asList(elements);
    }

    private Lists() {
        // Utility class, prevent instantiation
    }
}
