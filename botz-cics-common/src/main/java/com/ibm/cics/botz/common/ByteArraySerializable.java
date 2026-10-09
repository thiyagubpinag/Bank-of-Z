package com.ibm.cics.botz.common;

/**
 * Type that can be serialized to and from a COBOL-format binary representation.
 *
 * <p>Provides both serialization (object → bytes) and deserialization (bytes → object) via
 * delegation to the paired {@link ByteArraySerializer}. Extends {@link Settable} to support
 * COBOL MOVE (value-copy) semantics.
 *
 * @param <T> class implementing {@code ByteArraySerializable}
 */
public interface ByteArraySerializable<T extends ByteArraySerializable<T>> extends Settable<T> {

    /**
     * Gets the serialization object for this type.
     *
     * @return the serializer for this type
     */
    ByteArraySerializer<T> serializer();

    /**
     * Retrieves a COBOL-format byte array representation of this object.
     *
     * @return a new byte array containing the serialized object
     * @see ByteArraySerializer#toBytes(Object)
     */
    @SuppressWarnings("unchecked")
    default byte[] toBytes() {
        return serializer().toBytes((T) this);
    }

    /**
     * Alias for {@link #toBytes()}.
     *
     * @return a new byte array containing the serialized object
     * @see #toBytes()
     */
    default byte[] getBytes() {
        return toBytes();
    }

    /**
     * Retrieves a COBOL-format string representation of this object.
     *
     * @return the result of {@link #toBytes()} interpreted using
     *         {@link ByteArraySerializer#encoding}
     * @see ByteArraySerializer#toByteString(Object)
     */
    @SuppressWarnings("unchecked")
    default String toByteString() {
        return serializer().toByteString((T) this);
    }

    /**
     * Updates the fields of this object from a COBOL-format byte array.
     *
     * @param bytes byte array to deserialize from
     * @see ByteArraySerializer#fromBytes(byte[])
     */
    default void copyFrom(byte[] bytes) {
        set(serializer().fromBytes(bytes));
    }

    /**
     * Updates the fields of this object from a COBOL-format string.
     *
     * @param bytes string to deserialize from
     * @see ByteArraySerializer#fromByteString(String)
     */
    default void copyFrom(final String bytes) {
        set(serializer().fromByteString(bytes));
    }

    /**
     * Updates the fields of this object from a {@code ByteArraySerializable} object by
     * serializing and deserializing through the COBOL representation (byte-level overlay).
     *
     * @param <S>  the base serializable type
     * @param <U>  the actual type (subclass of S)
     * @param that the object to copy from
     * @see ByteArraySerializer#toBytes(Object)
     * @see #copyFrom(byte[])
     */
    default <S extends ByteArraySerializable<S>, U extends S> void copyFrom(final U that) {
        copyFrom(that.serializer().toBytes(that));
    }
}
