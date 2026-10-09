package com.ibm.jzos.fields;

import java.math.BigInteger;

public class PackedDecimalAsBigIntegerField implements BigIntegerAccessor {
    protected int offset;
    protected int precision;
    protected int scale;
    protected boolean signed;
    protected int byteLength;

    public PackedDecimalAsBigIntegerField(int offset, int precision, int scale, boolean signed) {
        this.offset = offset;
        this.precision = precision;
        this.scale = scale;
        this.signed = signed;
        this.byteLength = (precision / 2) + 1;
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

    public int getScale() {
        return scale;
    }

    public boolean isSigned() {
        return signed;
    }

    @Override
    public BigInteger getBigInteger(byte[] buffer) throws IllegalArgumentException {
        return getBigInteger(buffer, this.offset);
    }

    @Override
    public BigInteger getBigInteger(byte[] buffer, int off) throws IllegalArgumentException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < byteLength - 1; i++) {
            byte b = buffer[off + i];
            sb.append((b >> 4) & 0x0F);
            sb.append(b & 0x0F);
        }
        byte lastByte = buffer[off + byteLength - 1];
        sb.append((lastByte >> 4) & 0x0F);
        int signNibble = lastByte & 0x0F;
        boolean negative = (signNibble == 0x0D || signNibble == 0x0B);
        String s = sb.toString();
        while (s.length() > precision) {
            s = s.substring(1);
        }
        BigInteger bi = s.isEmpty() ? BigInteger.ZERO : new BigInteger(s);
        return negative ? bi.negate() : bi;
    }

    @Override
    public void putBigInteger(BigInteger value, byte[] buffer) throws IllegalArgumentException {
        putBigInteger(value, buffer, this.offset);
    }

    @Override
    public void putBigInteger(BigInteger value, byte[] buffer, int off) throws IllegalArgumentException {
        boolean negative = value.signum() < 0;
        String s = value.abs().toString();
        while (s.length() < precision) {
            s = "0" + s;
        }
        if (s.length() > precision) {
            s = s.substring(s.length() - precision);
        }
        if (precision % 2 == 0) {
            s = "0" + s;
        }
        for (int i = 0; i < byteLength - 1; i++) {
            int high = s.charAt(i * 2) - '0';
            int low = s.charAt(i * 2 + 1) - '0';
            buffer[off + i] = (byte) ((high << 4) | low);
        }
        int lastDigit = s.charAt(s.length() - 1) - '0';
        byte signNibble = signed ? (byte) (negative ? 0x0D : 0x0C) : (byte) 0x0F;
        buffer[off + byteLength - 1] = (byte) ((lastDigit << 4) | signNibble);
    }

    public boolean equals(BigInteger a, BigInteger b) {
        return a == null ? b == null : a.equals(b);
    }
}
