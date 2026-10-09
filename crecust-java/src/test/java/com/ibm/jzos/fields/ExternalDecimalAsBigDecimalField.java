package com.ibm.jzos.fields;

import java.math.BigDecimal;
import java.math.BigInteger;

public class ExternalDecimalAsBigDecimalField implements BigDecimalAccessor {
    protected final ExternalDecimalAsBigIntegerField delegate;
    protected final int scale;

    public ExternalDecimalAsBigDecimalField(int offset, int length, int scale, boolean signed, boolean signTrailing, boolean signExternal, boolean blankWhenZero) {
        this.scale = scale;
        this.delegate = new ExternalDecimalAsBigIntegerField(offset, length, scale, signed, signTrailing, signExternal, blankWhenZero);
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

    public boolean isSignExternal() {
        return delegate.isSignExternal();
    }

    public boolean isSignTrailing() {
        return delegate.isSignTrailing();
    }

    public boolean isBlankWhenZero() {
        return delegate.isBlankWhenZero();
    }

    @Override
    public BigDecimal getBigDecimal(byte[] buffer) {
        return getBigDecimal(buffer, delegate.getOffset());
    }

    @Override
    public BigDecimal getBigDecimal(byte[] buffer, int offset) {
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
