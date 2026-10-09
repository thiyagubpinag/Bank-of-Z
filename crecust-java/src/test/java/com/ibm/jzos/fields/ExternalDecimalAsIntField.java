package com.ibm.jzos.fields;

public class ExternalDecimalAsIntField implements IntAccessor {
    protected int offset;
    protected final int length;
    protected final boolean signed;
    protected final boolean signTrailing;
    protected final boolean signExternal;
    protected final boolean blankWhenZero;
    protected final int minValue;
    protected final int maxValue;

    public ExternalDecimalAsIntField(int offset, int length, boolean signed, boolean signTrailing, boolean signExternal, boolean blankWhenZero) {
        this.offset = offset;
        this.length = length;
        this.signed = signed;
        this.signTrailing = signTrailing;
        this.signExternal = signExternal;
        this.blankWhenZero = blankWhenZero;
        int max = 0;
        for (int i = 0; i < length; i++) {
            max = max * 10 + 9;
        }
        this.maxValue = max;
        this.minValue = signed ? -max : 0;
    }

    @Override
    public int getByteLength() {
        return length;
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
    public int getInt(byte[] buffer) {
        return getInt(buffer, 0);
    }

    @Override
    public int getInt(byte[] buffer, int off) {
        off += this.offset;
        long val = 0;
        boolean negative = false;
        boolean allBlank = true;

        for (int i = 0; i < length; i++) {
            byte b = buffer[off + i];
            if (b != ' ' && b != 0x40 && b != 0) {
                allBlank = false;
            }
            int digit;
            if (b >= '0' && b <= '9') {
                digit = b - '0';
            } else if ((b & 0xF0) == 0xF0 || (b & 0xF0) == 0xC0 || (b & 0xF0) == 0xD0) {
                digit = b & 0x0F;
                if ((b & 0xF0) == 0xD0) {
                    negative = true;
                }
            } else {
                digit = b & 0x0F;
            }
            val = val * 10 + digit;
        }

        if (allBlank && blankWhenZero) {
            return 0;
        }
        int result = (int) val;
        return negative ? -result : result;
    }

    @Override
    public void putInt(int value, byte[] buffer) throws IllegalArgumentException {
        putInt(value, buffer, 0);
    }

    @Override
    public void putInt(int value, byte[] buffer, int off) throws IllegalArgumentException {
        off += this.offset;
        rangeCheck(value);
        boolean negative = value < 0;
        int absVal = Math.abs(value);
        for (int i = length - 1; i >= 0; i--) {
            int digit = absVal % 10;
            byte ebcdicByte = (byte) (0xF0 | digit);
            if (signed && !signExternal && ((signTrailing && i == length - 1) || (!signTrailing && i == 0))) {
                ebcdicByte = (byte) ((negative ? 0xD0 : 0xC0) | digit);
            }
            buffer[off + i] = ebcdicByte;
            absVal /= 10;
        }
    }

    @Override
    public boolean isSigned() {
        return signed;
    }

    public boolean equals(Integer a, int b) {
        return a != null && a == b;
    }

    public boolean isBlankWhenZero() {
        return blankWhenZero;
    }

    public int getLength() {
        return length;
    }

    public int getPrecision() {
        return length;
    }

    public boolean isSignExternal() {
        return signExternal;
    }

    public boolean isSignTrailing() {
        return signTrailing;
    }

    protected void rangeCheck(int value) {
        // Range check implementation
    }
}
