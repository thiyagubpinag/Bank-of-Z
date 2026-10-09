package com.ibm.cics.botz.common;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Serializes an object to and from COBOL-format binary representation.
 *
 * <p>Implementations translate between a Java object graph and a COBOL-format byte array
 * using EBCDIC (IBM-1047) encoding. This is the single source-of-truth contract for all
 * serializer classes (ADR-A, Rule 15): no inline byte-packing is permitted in service classes.
 *
 * @param <T> type of object to serialize; must implement {@link ByteArraySerializable}
 */
public interface ByteArraySerializer<T extends ByteArraySerializable<T>> {

    /** String encoding used by the COBOL program (EBCDIC code page 1047). */
    Charset encoding = Charset.forName("IBM-1047");

    /**
     * The minimum number of bytes required for a COBOL-format byte array
     * representing an object of type {@code T}.
     *
     * @return the minimum byte array size
     */
    int numBytes();

    /**
     * The number of bytes required for a COBOL-format byte array representing
     * {@code obj}. For fixed-size COBOL records this equals {@link #numBytes()};
     * it may be larger for variable-size records (ODO).
     *
     * @param obj the object to measure
     * @return the byte array size needed for this specific object
     */
    default int numBytesFor(final T obj) {
        return numBytes();
    }

    /**
     * Retrieves a COBOL-format byte array representation of {@code obj}.
     *
     * @param bytes  preallocated byte array to store the object serialization
     * @param offset offset in the byte array where serialization should begin
     * @param obj    object to serialize
     * @return the byte array, updated with the newly-added data
     */
    byte[] toBytes(byte[] bytes, int offset, final T obj);

    /**
     * Retrieves a COBOL-format byte array representation of {@code obj},
     * starting at the beginning of the array.
     *
     * @param bytes preallocated byte array to store the object serialization
     * @param obj   object to serialize
     * @return the byte array, updated with the newly-added data
     * @see #toBytes(byte[], int, Object)
     */
    default byte[] toBytes(byte[] bytes, final T obj) {
        return toBytes(bytes, 0, obj);
    }

    /**
     * Retrieves a COBOL-format byte array representation of {@code obj},
     * allocating a new array.
     *
     * @param obj object to serialize
     * @return a new byte array containing the serialized object
     * @see #toBytes(byte[], int, Object)
     */
    default byte[] toBytes(final T obj) {
        return toBytes(new byte[numBytesFor(obj)], obj);
    }

    /**
     * Retrieves a COBOL-format string representation of {@code obj}.
     *
     * @param obj object to serialize
     * @return the result of {@link #toBytes(Object)} interpreted using {@link #encoding}
     */
    default String toByteString(final T obj) {
        return new String(toBytes(obj), encoding).stripTrailing();
    }

    /**
     * Creates a new object from a COBOL-format byte array.
     *
     * <p>The byte array is space-padded to length if necessary.
     *
     * @param bytes  byte array to deserialize from; will be space-padded to length if necessary
     * @param offset offset in the byte array where deserialization should begin
     * @return a new {@code T} object equivalent to the specified array slice
     */
    T fromBytes(byte[] bytes, int offset);

    /**
     * Creates a new object from a COBOL-format byte array, starting at the
     * beginning of the array.
     *
     * @param bytes byte array to deserialize from
     * @return a new {@code T} object equivalent to the byte array
     * @see #fromBytes(byte[], int)
     */
    default T fromBytes(byte[] bytes) {
        return fromBytes(bytes, 0);
    }

    /**
     * Creates a new object from a COBOL-format string.
     * The string is first converted to a byte array using {@link #encoding}.
     *
     * @param bytes string to deserialize from
     * @return a new {@code T} object equivalent to the string
     * @see #fromBytes(byte[], int)
     */
    default T fromByteString(final String bytes) {
        return fromBytes(bytes.getBytes(encoding));
    }

    /**
     * Ensures a byte array has a provided minimum length.
     *
     * <p>If the slice {@code bytes[offset .. offset+size-1]} is too short, a copied array
     * is returned with the remaining slots filled with EBCDIC spaces ({@code 0x40}).
     *
     * @param bytes  the byte array to pad
     * @param offset the offset of the start of the slice
     * @param size   the minimum size of the slice of {@code bytes} at {@code offset}
     * @return {@code bytes} if at least {@code offset + size} elements, otherwise a copied
     *         array with the remaining slots filled with {@code (byte) 0x40}
     */
    default byte[] padToSize(byte[] bytes, int offset, int size) {
        if (bytes.length < offset + size) {
            byte[] newBytes = Arrays.copyOf(bytes, offset + size);
            Arrays.fill(newBytes, bytes.length, offset + size, (byte) 0x40 /* EBCDIC space */);
            return newBytes;
        }
        return bytes;
    }
}
