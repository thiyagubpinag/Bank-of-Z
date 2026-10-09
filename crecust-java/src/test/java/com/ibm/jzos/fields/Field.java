package com.ibm.jzos.fields;

public interface Field {
    int getByteLength();
    int getOffset();
    void setOffset(int offset);
}
