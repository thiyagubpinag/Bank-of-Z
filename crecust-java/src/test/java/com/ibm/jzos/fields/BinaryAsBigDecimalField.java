package com.ibm.jzos.fields;

import java.math.BigDecimal;
import java.math.BigInteger;

public class BinaryAsBigDecimalField extends BinaryAsBigIntegerField implements BigDecimalAccessor {

    public BinaryAsBigDecimalField(int offset, int length, int scale, boolean signed) {
        super(offset, length, scale, signed);
    }

    @Override
    public BigDecimal getBigDecimal(byte[] buffer) {
        return getBigDecimal(buffer, this.offset);
    }

    @Override
    public BigDecimal getBigDecimal(byte[] buffer, int off) {
        BigInteger bi = getBigInteger(buffer, off);
        return new BigDecimal(bi, scale);
    }

    @Override
    public void putBigDecimal(BigDecimal value, byte[] buffer) throws IllegalArgumentException {
        putBigDecimal(value, buffer, this.offset);
    }

    @Override
    public void putBigDecimal(BigDecimal value, byte[] buffer, int off) throws IllegalArgumentException {
        BigInteger bi = value.scaleByPowerOfTen(scale).toBigInteger();
        putBigInteger(bi, buffer, off);
    }

    public boolean equals(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : a.compareTo(b) == 0;
    }
}
