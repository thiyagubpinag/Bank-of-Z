package com.ibm.cics.botz.crecust.model;

/**
 * Abstract base class for the {@code PROC-TRAN-DESC} 40-byte REDEFINES hierarchy (Cat 4a, Pattern B).
 *
 * <p>Holds the single 40-byte backing array that represents the shared-memory region.
 * Concrete subclasses act as typed lenses over this shared array and declare no additional
 * instance fields.
 */
public abstract class ProcTranDescBase {

    private final byte[] data;

    /**
     * Constructs a view over the supplied 40-byte array by reference.
     *
     * @param data backing byte array of exactly {@value ProcTranDescConstants#DATA_SIZE} bytes
     * @throws IllegalArgumentException if data is null or shorter than 40 bytes
     */
    public ProcTranDescBase(byte[] data) {
        if (data == null || data.length < ProcTranDescConstants.DATA_SIZE) {
            throw new IllegalArgumentException(
                "PROC-TRAN-DESC buffer must be at least " + ProcTranDescConstants.DATA_SIZE
                    + " bytes, but got: " + (data == null ? "null" : data.length));
        }
        this.data = data;
    }

    /**
     * Returns the underlying 40-byte backing array.
     *
     * @return the backing byte array
     */
    public byte[] getData() {
        return data;
    }
}
