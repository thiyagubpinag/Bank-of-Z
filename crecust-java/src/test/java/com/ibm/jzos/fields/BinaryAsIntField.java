package com.ibm.jzos.fields;

public class BinaryAsIntField implements IntAccessor {
    public static final int MAX_FIELD_WIDTH = 4;
    public static final int DEFAULT_FIELD_WIDTH = 4;
    public static final int SIGNED_MIN_LEN1_VAL = -128;
    public static final int SIGNED_MAX_LEN1_VAL = 127;
    public static final int SIGNED_MIN_LEN2_VAL = -32768;
    public static final int SIGNED_MAX_LEN2_VAL = 32767;
    public static final int SIGNED_MIN_LEN3_VAL = -8388608;
    public static final int SIGNED_MAX_LEN3_VAL = 8388607;
    public static final int SIGNED_MIN_LEN4_VAL = Integer.MIN_VALUE;
    public static final int SIGNED_MAX_LEN4_VAL = Integer.MAX_VALUE;
    public static final int UNSIGNED_MAX_LEN1_VAL = 255;
    public static final int UNSIGNED_MAX_LEN2_VAL = 65535;
    public static final int UNSIGNED_MAX_LEN3_VAL = 16777215;
    public static final int UNSIGNED_MAX_LEN4_VAL = -1;

    protected int offset;
    protected final int length;
    protected boolean signed;

    public BinaryAsIntField(int length) {
        this(0, length, true);
    }

    public BinaryAsIntField(int offset, int length) {
        this(offset, length, true);
    }

    public BinaryAsIntField(int offset, int length, boolean signed) {
        this.offset = offset;
        this.length = length;
        this.signed = signed;
    }

    @Override
    public int getByteLength() {
        return length;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public int getInt(byte[] buffer) {
        return getInt(buffer, 0);
    }

    @Override
    public int getInt(byte[] buffer, int off) {
        off += this.offset;
        int val = 0;
        for (int i = 0; i < length; i++) {
            val = (val << 8) | (buffer[off + i] & 0xFF);
        }
        if (signed) {
            int shift = (4 - length) * 8;
            val = (val << shift) >> shift;
        }
        return val;
    }

    @Override
    public void putInt(int value, byte[] buffer) throws IllegalArgumentException {
        putInt(value, buffer, 0);
    }

    @Override
    public void putInt(int value, byte[] buffer, int off) throws IllegalArgumentException {
        off += this.offset;
        for (int i = length - 1; i >= 0; i--) {
            buffer[off + i] = (byte) (value & 0xFF);
            value >>= 8;
        }
    }

    protected final void rangeCheck(int val, int min, int max) throws IllegalArgumentException {
        if (val < min || val > max) {
            throw new IllegalArgumentException("Value out of range: " + val);
        }
    }

    @Override
    public boolean isSigned() {
        return signed;
    }

    public void setSigned(boolean signed) {
        this.signed = signed;
    }

    public boolean equals(Integer a, int b) {
        return a != null && a == b;
    }
}
