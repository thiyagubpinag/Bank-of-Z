package com.ibm.jzos.fields;

public interface DoubleAccessor extends Field {
    double getDouble(byte[] buffer);
    double getDouble(byte[] buffer, int offset);
    void putDouble(double value, byte[] buffer);
    void putDouble(double value, byte[] buffer, int offset);
}
