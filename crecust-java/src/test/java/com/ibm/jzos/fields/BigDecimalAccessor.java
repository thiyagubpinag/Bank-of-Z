package com.ibm.jzos.fields;

import java.math.BigDecimal;

public interface BigDecimalAccessor extends Field {
    BigDecimal getBigDecimal(byte[] buffer);
    BigDecimal getBigDecimal(byte[] buffer, int offset);
    void putBigDecimal(BigDecimal value, byte[] buffer) throws IllegalArgumentException;
    void putBigDecimal(BigDecimal value, byte[] buffer, int offset) throws IllegalArgumentException;
}
