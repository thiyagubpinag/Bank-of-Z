package com.ibm.jzos.fields;

import java.math.BigInteger;

public class BinaryAsBigIntegerField implements BigIntegerAccessor {
    protected int offset;
    protected int length;
    protected int scale;
    protected boolean signed;

    public BinaryAsBigIntegerField(int offset, int length, boolean signed) {
        this(offset, length, 0, signed);
    }

    public BinaryAsBigIntegerField(int offset, int length, int scale, boolean signed) {
        this.offset = offset;
        this.length = length;
        this.scale = scale;
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

    public int getScale() {
        return scale;
    }

    public int getLength() {
        return length;
    }

    public boolean isSigned() {
        return signed;
    }

    @Override
    public BigInteger getBigInteger(byte[] buffer) {
        return getBigInteger(buffer, this.offset);
    }

    @Override
    public BigInteger getBigInteger(byte[] buffer, int off) {
        byte[] bytes = new byte[length];
        System.arraycopy(buffer, off, bytes, 0, length);
        return signed ? new BigInteger(bytes) : new BigInteger(1, bytes);
    }

    @Override
    public void putBigInteger(BigInteger value, byte[] buffer) throws IllegalArgumentException {
        putBigInteger(value, buffer, this.offset);
    }

    @Override
    public void putBigInteger(BigInteger value, byte[] buffer, int off) throws IllegalArgumentException {
        byte[] valBytes = value.toByteArray();
        for (int i = 0; i < length; i++) {
            int srcIdx = valBytes.length - 1 - i;
            buffer[off + length - 1 - i] = srcIdx >= 0 ? valBytes[srcIdx] : (byte) (value.signum() < 0 ? 0xFF : 0x00);
        }
    }

    public boolean equals(BigInteger a, BigInteger b) {
        return a == null ? b == null : a.equals(b);
    }
}
