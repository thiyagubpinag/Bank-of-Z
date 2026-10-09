package com.ibm.jzos.fields;

public class ExternalFloatField implements DoubleAccessor {
    protected int offset;
    protected final int length;
    protected final int scale;
    protected final boolean impliedDecimal;
    protected final boolean showMantissaPlusSign;
    protected final boolean showExponentPlusSign;

    public ExternalFloatField(int offset, int length, int scale, boolean impliedDecimal, boolean showMantissaPlusSign, boolean showExponentPlusSign) {
        this.offset = offset;
        this.length = length;
        this.scale = scale;
        this.impliedDecimal = impliedDecimal;
        this.showMantissaPlusSign = showMantissaPlusSign;
        this.showExponentPlusSign = showExponentPlusSign;
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
    public double getDouble(byte[] buffer) {
        return getDouble(buffer, this.offset);
    }

    @Override
    public double getDouble(byte[] buffer, int off) {
        String str = new String(buffer, off, length).trim();
        return str.isEmpty() ? 0.0 : Double.parseDouble(str);
    }

    @Override
    public void putDouble(double value, byte[] buffer) {
        putDouble(value, buffer, this.offset);
    }

    @Override
    public void putDouble(double value, byte[] buffer, int off) {
        byte[] bytes = String.format("%." + scale + "e", value).getBytes();
        int toCopy = Math.min(bytes.length, length);
        System.arraycopy(bytes, 0, buffer, off, toCopy);
    }

    public boolean equals(Double a, double b) {
        return a != null && Double.compare(a, b) == 0;
    }

    public boolean isImpliedDecimal() {
        return impliedDecimal;
    }

    public int getLength() {
        return length;
    }

    public int getPrecision() {
        return length;
    }

    public int getScale() {
        return scale;
    }

    public boolean isShowExponentPlusSign() {
        return showExponentPlusSign;
    }

    public boolean isShowMantissaPlusSign() {
        return showMantissaPlusSign;
    }
}
