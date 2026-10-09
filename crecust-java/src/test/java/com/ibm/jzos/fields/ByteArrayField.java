package com.ibm.jzos.fields;

import java.util.Arrays;

public class ByteArrayField implements Field {
    protected int offset;
    protected int length;

    public ByteArrayField(int offset, int length) {
        this.offset = offset;
        this.length = length;
    }

    @Override
    public int getByteLength() {
        return length;
    }

    public void setByteLength(int length) {
        this.length = length;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public void setOffset(int offset) {
        this.offset = offset;
    }

    public byte[] getByteArray(byte[] buffer) {
        return getByteArray(buffer, this.offset);
    }

    public byte[] getByteArray(byte[] buffer, int off) {
        byte[] copy = new byte[length];
        System.arraycopy(buffer, off, copy, 0, length);
        return copy;
    }

    public void putByteArray(byte[] val, byte[] buffer) {
        putByteArray(val, buffer, this.offset);
    }

    public void putByteArray(byte[] val, int valOffset, byte[] buffer) {
        putByteArray(val, valOffset, buffer, this.offset);
    }

    public void putByteArray(byte[] val, byte[] buffer, int off) {
        putByteArray(val, 0, buffer, off);
    }

    public void putByteArray(byte[] val, int valOffset, byte[] buffer, int off) {
        int toCopy = Math.min(val.length - valOffset, length);
        if (toCopy > 0) {
            System.arraycopy(val, valOffset, buffer, off, toCopy);
        }
    }

    public boolean equals(byte[] a, byte[] b) {
        return Arrays.equals(a, b);
    }
}
