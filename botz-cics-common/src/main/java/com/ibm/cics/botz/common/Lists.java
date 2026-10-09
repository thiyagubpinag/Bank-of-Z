package com.ibm.cics.botz.common;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility methods for list operations, particularly resizing for COBOL ODO
 * (Occurs Depending On) table translations.
 *
 * <p>This is a static-only utility class; it cannot be instantiated.
 */
public final class Lists {

    /** Prevent instantiation. */
    private Lists() {
        throw new UnsupportedOperationException("Lists is a static utility class");
    }

    /**
     * Resizes {@code list} to contain {@code n} elements, by truncating or
     * filling in from {@code elements}.
     *
     * <p>{@code list} must be non-null and support {@link List#remove(int)}.
     *
     * @param <T>      the type of elements in the list
     * @param list     the list to resize
     * @param n        the target size
     * @param elements supplier of elements to add when growing the list
     */
    public static <T> void resize(List<T> list, int n, Supplier<? extends T> elements) {
        for (int i = list.size() - 1; i >= n; i--) {
            list.remove(i);
        }
        for (int j = list.size(); j < n; j++) {
            list.add(elements.get());
        }
    }

    /**
     * Creates and populates a new list.
     *
     * @param <T>      the type of elements in the list
     * @param builder  creates the new list
     * @param n        number of elements to fill
     * @param elements elements to fill list with, in order
     * @return a new list from {@code builder} containing {@code n} elements
     *         drawn from {@code elements}
     */
    public static <T> List<T> create(Supplier<? extends List<T>> builder, int n, Supplier<? extends T> elements) {
        return Stream.generate(elements)
                .limit(n)
                .collect(Collectors.toCollection(builder));
    }
}
