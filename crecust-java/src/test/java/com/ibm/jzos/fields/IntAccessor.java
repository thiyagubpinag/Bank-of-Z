package com.ibm.jzos.fields;

public interface IntAccessor extends Field {
    int getInt(byte[] buffer);
    int getInt(byte[] buffer, int offset);
    void putInt(int value, byte[] buffer) throws IllegalArgumentException;
    void putInt(int value, byte[] buffer, int offset) throws IllegalArgumentException;
    boolean isSigned();
}
