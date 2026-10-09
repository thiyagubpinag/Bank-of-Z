package com.ibm.jzos.fields;

public class IbmFloatField implements FloatAccessor {
    public static final int BYTE_LENGTH = 4;
    protected int offset;

    public IbmFloatField(int offset) {
        this.offset = offset;
    }

    @Override
    public int getByteLength() {
        return BYTE_LENGTH;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public float getFloat(byte[] buffer) {
        return getFloat(buffer, 0);
    }

    @Override
    public float getFloat(byte[] buffer, int off) {
        off += this.offset;
        int bits = 0;
        for (int i = 0; i < 4; i++) {
            bits = (bits << 8) | (buffer[off + i] & 0xFF);
        }
        return Float.intBitsToFloat(bits);
    }

    @Override
    public void putFloat(float value, byte[] buffer) {
        putFloat(value, buffer, 0);
    }

    @Override
    public void putFloat(float value, byte[] buffer, int off) {
        off += this.offset;
        int bits = Float.floatToIntBits(value);
        for (int i = 3; i >= 0; i--) {
            buffer[off + i] = (byte) (bits & 0xFF);
            bits >>= 8;
        }
    }

    public boolean equals(Float a, float b) {
        return a != null && Float.compare(a, b) == 0;
    }
}
