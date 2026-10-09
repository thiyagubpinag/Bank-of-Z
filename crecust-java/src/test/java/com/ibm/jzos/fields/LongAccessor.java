package com.ibm.jzos.fields;

public interface LongAccessor extends Field {
    long getLong(byte[] buffer);
    long getLong(byte[] buffer, int offset);
    void putLong(long value, byte[] buffer) throws IllegalArgumentException;
    void putLong(long value, byte[] buffer, int offset) throws IllegalArgumentException;
    boolean isSigned();
}
