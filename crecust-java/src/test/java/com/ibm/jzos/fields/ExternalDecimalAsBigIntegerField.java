package com.ibm.jzos.fields;

import java.math.BigInteger;

public class ExternalDecimalAsBigIntegerField implements BigIntegerAccessor {
    protected int offset;
    protected int length;
    protected int scale;
    protected boolean signed;
    protected boolean signTrailing;
    protected boolean signExternal;
    protected boolean blankWhenZero;

    public ExternalDecimalAsBigIntegerField(int offset, int length, int scale, boolean signed, boolean signTrailing, boolean signExternal, boolean blankWhenZero) {
        this.offset = offset;
        this.length = length;
        this.scale = scale;
        this.signed = signed;
        this.signTrailing = signTrailing;
        this.signExternal = signExternal;
        this.blankWhenZero = blankWhenZero;
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

    public int getPrecision() {
        return length;
    }

    public int getScale() {
        return scale;
    }

    public boolean isSigned() {
        return signed;
    }

    public boolean isSignExternal() {
        return signExternal;
    }

    public boolean isSignTrailing() {
        return signTrailing;
    }

    public boolean isBlankWhenZero() {
        return blankWhenZero;
    }

    @Override
    public BigInteger getBigInteger(byte[] buffer) {
        return getBigInteger(buffer, this.offset);
    }

    @Override
    public BigInteger getBigInteger(byte[] buffer, int off) {
        StringBuilder sb = new StringBuilder();
        boolean negative = false;
        for (int i = 0; i < length; i++) {
            byte b = buffer[off + i];
            int digit;
            if (b >= '0' && b <= '9') {
                digit = b - '0';
            } else if ((b & 0xF0) == 0xD0) {
                digit = b & 0x0F;
                negative = true;
            } else {
                digit = b & 0x0F;
            }
            sb.append(digit);
        }
        BigInteger bi = sb.length() == 0 ? BigInteger.ZERO : new BigInteger(sb.toString());
        return negative ? bi.negate() : bi;
    }

    @Override
    public void putBigInteger(BigInteger value, byte[] buffer) throws IllegalArgumentException {
        putBigInteger(value, buffer, this.offset);
    }

    @Override
    public void putBigInteger(BigInteger value, byte[] buffer, int off) throws IllegalArgumentException {
        boolean negative = value.signum() < 0;
        String str = value.abs().toString();
        while (str.length() < length) {
            str = "0" + str;
        }
        if (str.length() > length) {
            str = str.substring(str.length() - length);
        }
        for (int i = 0; i < length; i++) {
            int digit = str.charAt(i) - '0';
            byte b = (byte) (0xF0 | digit);
            if (signed && !signExternal && ((signTrailing && i == length - 1) || (!signTrailing && i == 0))) {
                b = (byte) ((negative ? 0xD0 : 0xC0) | digit);
            }
            buffer[off + i] = b;
        }
    }

    public boolean equals(BigInteger a, BigInteger b) {
        return a == null ? b == null : a.equals(b);
    }
}
