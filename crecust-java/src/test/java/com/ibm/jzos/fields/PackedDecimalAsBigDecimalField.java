package com.ibm.jzos.fields;

import java.math.BigDecimal;
import java.math.BigInteger;

public class PackedDecimalAsBigDecimalField implements BigDecimalAccessor {
    protected final PackedDecimalAsBigIntegerField delegate;
    protected final int scale;

    public PackedDecimalAsBigDecimalField(int offset, int precision, int scale, boolean signed) {
        this.scale = scale;
        this.delegate = new PackedDecimalAsBigIntegerField(offset, precision, scale, signed);
    }

    @Override
    public int getByteLength() {
        return delegate.getByteLength();
    }

    @Override
    public int getOffset() {
        return delegate.getOffset();
    }

    @Override
    public void setOffset(int offset) {
        delegate.setOffset(offset);
    }

    public int getPrecision() {
        return delegate.getPrecision();
    }

    public int getScale() {
        return scale;
    }

    public boolean isSigned() {
        return delegate.isSigned();
    }

    @Override
    public BigDecimal getBigDecimal(byte[] buffer) throws IllegalArgumentException {
        return getBigDecimal(buffer, delegate.getOffset());
    }

    @Override
    public BigDecimal getBigDecimal(byte[] buffer, int offset) throws IllegalArgumentException {
        BigInteger bi = delegate.getBigInteger(buffer, offset);
        return new BigDecimal(bi, scale);
    }

    @Override
    public void putBigDecimal(BigDecimal value, byte[] buffer) throws IllegalArgumentException {
        putBigDecimal(value, buffer, delegate.getOffset());
    }

    @Override
    public void putBigDecimal(BigDecimal value, byte[] buffer, int offset) throws IllegalArgumentException {
        BigInteger bi = value.scaleByPowerOfTen(scale).toBigInteger();
        delegate.putBigInteger(bi, buffer, offset);
    }

    public boolean equals(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : a.compareTo(b) == 0;
    }
}
