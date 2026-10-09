package com.ibm.jzos.fields;

public class IbmDoubleField implements DoubleAccessor {
    public static final int BYTE_LENGTH = 8;
    public static final long SIGN_BIT = 0x8000000000000000L;
    public static final long ALL_NON_SIGN_BITS = 0x7FFFFFFFFFFFFFFFL;
    public static final long HFP_FRACTION_MASK = 0x00FFFFFFFFFFFFFFL;
    public static final long BFP_FRACTION_MASK = 0x000FFFFFFFFFFFFFL;
    public static final long BFP_FRACTION_IMPLIED_HOB = 0x0010000000000000L;
    public static final long BFP_FRACTION_HON = 0x000F000000000000L;
    public static final long HFP_EXP_MASK = 0x7F00000000000000L;
    public static final long BFP_EXP_MASK = 0x7FF0000000000000L;
    public static final int HFP_FRACTION_LENGTH = 56;
    public static final int BFP_FRACTION_LENGTH = 52;
    public static final int HFP_BIAS = 64;
    public static final int HFP_MAX_EXP = 127;
    public static final int BFP_BIAS = 1023;
    public static final int BFP_INF_EXP = 2047;

    protected int offset;

    public IbmDoubleField(int offset) {
        this.offset = offset;
    }

    @Override
    public int getByteLength() {
        return BYTE_LENGTH;
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
    public double getDouble(byte[] buffer) {
        return getDouble(buffer, this.offset);
    }

    @Override
    public double getDouble(byte[] buffer, int off) {
        long bits = 0;
        for (int i = 0; i < 8; i++) {
            bits = (bits << 8) | (buffer[off + i] & 0xFFL);
        }
        return Double.longBitsToDouble(bits);
    }

    @Override
    public void putDouble(double value, byte[] buffer) {
        putDouble(value, buffer, this.offset);
    }

    @Override
    public void putDouble(double value, byte[] buffer, int off) {
        long bits = Double.doubleToLongBits(value);
        for (int i = 7; i >= 0; i--) {
            buffer[off + i] = (byte) (bits & 0xFF);
            bits >>= 8;
        }
    }

    public boolean equals(Double a, double b) {
        return a != null && Double.compare(a, b) == 0;
    }
}
