package com.ibm.cics.botz.common;

/**
 * Allows overwriting an object while maintaining its existing references and object identity.
 *
 * <p>This interface models COBOL MOVE semantics: the calling code keeps a reference to
 * {@code this} (as COBOL keeps a pointer to the storage area), but the <em>content</em>
 * of that storage is replaced with a shallow copy of {@code that}.
 *
 * <p>Prefer {@code that.clone()} if a deep copy is required.
 * If {@code that} is a subclass of {@code this}, only the superclass fields are written.
 *
 * @param <T> class implementing {@code Settable}.
 */
public interface Settable<T> {

    /**
     * Overwrite {@code this} with a shallow copy of {@code that}.
     * Prefer {@code that.clone()} if a deep copy is required.
     * If {@code that} is a subclass of {@code this}, will not write the subclass fields.
     *
     * @param that the object to copy from
     */
    void set(T that);
}
