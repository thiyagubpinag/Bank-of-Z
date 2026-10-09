package com.ibm.jzos.fields;

public class PackedDecimalAsIntField implements IntAccessor {
    protected int offset;
    protected final int precision;
    protected final boolean signed;
    protected final int byteLength;
    protected final int minValue;
    protected final int maxValue;

    public PackedDecimalAsIntField(int offset, int precision, boolean signed) {
        this.offset = offset;
        this.precision = precision;
        this.signed = signed;
        this.byteLength = (precision / 2) + 1;
        int max = 0;
        for (int i = 0; i < precision; i++) {
            max = max * 10 + 9;
        }
        this.maxValue = max;
        this.minValue = signed ? -max : 0;
    }

    @Override
    public int getByteLength() {
        return byteLength;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getPrecision() {
        return precision;
    }

    @Override
    public int getInt(byte[] buffer) throws IllegalArgumentException {
        return getInt(buffer, this.offset);
    }

    @Override
    public int getInt(byte[] buffer, int off) throws IllegalArgumentException {
        int val = 0;
        for (int i = 0; i < byteLength - 1; i++) {
            byte b = buffer[off + i];
            int high = (b >> 4) & 0x0F;
            int low = b & 0x0F;
            val = val * 100 + high * 10 + low;
        }
        byte lastByte = buffer[off + byteLength - 1];
        int high = (lastByte >> 4) & 0x0F;
        val = val * 10 + high;
        int signNibble = lastByte & 0x0F;
        boolean negative = (signNibble == 0x0D || signNibble == 0x0B);
        return negative ? -val : val;
    }

    @Override
    public void putInt(int value, byte[] buffer) throws IllegalArgumentException {
        putInt(value, buffer, this.offset);
    }

    @Override
    public void putInt(int value, byte[] buffer, int off) throws IllegalArgumentException {
        rangeCheck(value);
        boolean negative = value < 0;
        int absVal = Math.abs(value);
        byte signNibble = signed ? (byte) (negative ? 0x0D : 0x0C) : (byte) 0x0F;
        buffer[off + byteLength - 1] = (byte) (((absVal % 10) << 4) | signNibble);
        absVal /= 10;
        for (int i = byteLength - 2; i >= 0; i--) {
            int low = absVal % 10;
            absVal /= 10;
            int high = absVal % 10;
            absVal /= 10;
            buffer[off + i] = (byte) ((high << 4) | low);
        }
    }

    @Override
    public boolean isSigned() {
        return signed;
    }

    public boolean equals(Integer a, int b) {
        return a != null && a == b;
    }

    protected void rangeCheck(int value) {
        // Range check
    }
}
