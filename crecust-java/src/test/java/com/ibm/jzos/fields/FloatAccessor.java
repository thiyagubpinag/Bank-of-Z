package com.ibm.jzos.fields;

public interface FloatAccessor extends Field {
    float getFloat(byte[] buffer);
    float getFloat(byte[] buffer, int offset);
    void putFloat(float value, byte[] buffer);
    void putFloat(float value, byte[] buffer, int offset);
}
