package com.ibm.jzos.fields;

public class BinaryAsLongField implements LongAccessor {
    public static final int MAX_FIELD_WIDTH = 8;
    public static final int DEFAULT_FIELD_WIDTH = 8;
    public static final long SIGNED_MIN_LEN1_VAL = -128L;
    public static final long SIGNED_MAX_LEN1_VAL = 127L;
    public static final long SIGNED_MIN_LEN2_VAL = -32768L;
    public static final long SIGNED_MAX_LEN2_VAL = 32767L;
    public static final long SIGNED_MIN_LEN3_VAL = -8388608L;
    public static final long SIGNED_MAX_LEN3_VAL = 8388607L;
    public static final long SIGNED_MIN_LEN4_VAL = Integer.MIN_VALUE;
    public static final long SIGNED_MAX_LEN4_VAL = Integer.MAX_VALUE;
    public static final long SIGNED_MIN_LEN5_VAL = -549755813888L;
    public static final long SIGNED_MAX_LEN5_VAL = 549755813887L;
    public static final long SIGNED_MIN_LEN6_VAL = -140737488355328L;
    public static final long SIGNED_MAX_LEN6_VAL = 140737488355327L;
    public static final long SIGNED_MIN_LEN7_VAL = -36028797018963968L;
    public static final long SIGNED_MAX_LEN7_VAL = 36028797018963967L;
    public static final long SIGNED_MIN_LEN8_VAL = Long.MIN_VALUE;
    public static final long SIGNED_MAX_LEN8_VAL = Long.MAX_VALUE;
    public static final long UNSIGNED_MAX_LEN1_VAL = 255L;
    public static final long UNSIGNED_MAX_LEN2_VAL = 65535L;
    public static final long UNSIGNED_MAX_LEN3_VAL = 16777215L;
    public static final long UNSIGNED_MAX_LEN4_VAL = 4294967295L;
    public static final long UNSIGNED_MAX_LEN5_VAL = 1099511627775L;
    public static final long UNSIGNED_MAX_LEN6_VAL = 281474976710655L;
    public static final long UNSIGNED_MAX_LEN7_VAL = 72057594037927935L;
    public static final long UNSIGNED_MAX_LEN8_VAL = -1L;

    protected int offset;
    protected final int length;
    protected final long maxValue;
    protected final long minValue;
    protected boolean signed;

    public BinaryAsLongField(int length) {
        this(0, length, true);
    }

    public BinaryAsLongField(int offset, int length) {
        this(offset, length, true);
    }

    public BinaryAsLongField(int offset, int length, boolean signed) {
        this.offset = offset;
        this.length = length;
        this.signed = signed;
        this.minValue = signed ? -(1L << (length * 8 - 1)) : 0L;
        this.maxValue = signed ? (1L << (length * 8 - 1)) - 1 : -1L;
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
    public long getLong(byte[] buffer) {
        return getLong(buffer, 0);
    }

    @Override
    public long getLong(byte[] buffer, int off) {
        off += this.offset;
        long val = 0;
        for (int i = 0; i < length; i++) {
            val = (val << 8) | (buffer[off + i] & 0xFFL);
        }
        if (signed) {
            int shift = (8 - length) * 8;
            val = (val << shift) >> shift;
        }
        return val;
    }

    @Override
    public void putLong(long value, byte[] buffer) throws IllegalArgumentException {
        putLong(value, buffer, 0);
    }

    @Override
    public void putLong(long value, byte[] buffer, int off) throws IllegalArgumentException {
        off += this.offset;
        for (int i = length - 1; i >= 0; i--) {
            buffer[off + i] = (byte) (value & 0xFF);
            value >>= 8;
        }
    }

    @Override
    public boolean isSigned() {
        return signed;
    }

    public void setSigned(boolean signed) {
        this.signed = signed;
    }

    public boolean equals(Long a, long b) {
        return a != null && a == b;
    }

    protected final void rangeCheck(long val, long min, long max) throws IllegalArgumentException {
        if (val < min || val > max) {
            throw new IllegalArgumentException("Value out of range: " + val);
        }
    }
}
