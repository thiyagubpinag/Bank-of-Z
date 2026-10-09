package com.ibm.jzos.fields;

import java.math.BigInteger;

public interface BigIntegerAccessor extends Field {
    BigInteger getBigInteger(byte[] buffer);
    BigInteger getBigInteger(byte[] buffer, int offset);
    void putBigInteger(BigInteger value, byte[] buffer) throws IllegalArgumentException;
    void putBigInteger(BigInteger value, byte[] buffer, int offset) throws IllegalArgumentException;
}
