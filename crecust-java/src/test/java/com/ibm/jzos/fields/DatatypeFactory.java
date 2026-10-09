package com.ibm.jzos.fields;

import java.util.ArrayDeque;
import java.util.Deque;

public abstract class DatatypeFactory {
    public static final String USE_DAA_PROPERTY = "com.ibm.jzos.fields.useDaa";
    public static final boolean USE_DAA_DEFAULT = false;

    protected boolean useDaa = USE_DAA_DEFAULT;
    protected int offset = 0;
    protected int maximumOffset = 0;
    protected String stringEncoding = "IBM-1047";
    protected boolean stringTrimDefault = false;
    private final Deque<Integer> offsetStack = new ArrayDeque<>();

    public DatatypeFactory() {
    }

    public int getOffset() {
        return offset;
    }

    public int getMaximumOffset() {
        return maximumOffset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
        if (offset > maximumOffset) {
            maximumOffset = offset;
        }
    }

    public void pushOffset() {
        offsetStack.push(offset);
    }

    public void popOffset() {
        if (!offsetStack.isEmpty()) {
            setOffset(offsetStack.pop());
        }
    }

    public void incrementOffset(int delta) {
        setOffset(this.offset + delta);
    }

    protected void advanceOffset(Field field) {
        if (field != null) {
            incrementOffset(field.getByteLength());
        }
    }

    public String getStringEncoding() {
        return stringEncoding;
    }

    public void setStringEncoding(String stringEncoding) {
        this.stringEncoding = stringEncoding;
    }

    public boolean getStringTrimDefault() {
        return stringTrimDefault;
    }

    public void setStringTrimDefault(boolean stringTrimDefault) {
        this.stringTrimDefault = stringTrimDefault;
    }

    public boolean useDaa() {
        return useDaa;
    }

    public void setUseDaa(boolean useDaa) {
        this.useDaa = useDaa;
    }

    public BinaryAsIntField getBinaryAsIntField(int length, boolean signed) {
        return new BinaryAsIntField(offset, length, signed);
    }

    public BinaryAsLongField getBinaryAsLongField(int length, boolean signed) {
        return new BinaryAsLongField(offset, length, signed);
    }

    public BinaryAsBigIntegerField getBinaryAsBigIntegerField(int length, boolean signed) {
        return new BinaryAsBigIntegerField(offset, length, signed);
    }

    public BinaryAsBigIntegerField getBinaryAsBigIntegerField(int length, int scale, boolean signed) {
        return new BinaryAsBigIntegerField(offset, length, scale, signed);
    }

    public BinaryAsBigDecimalField getBinaryAsBigDecimalField(int length, int scale, boolean signed) {
        return new BinaryAsBigDecimalField(offset, length, scale, signed);
    }

    public Field getBinaryField(int length, boolean signed) {
        if (length <= 4) {
            return getBinaryAsIntField(length, signed);
        } else if (length <= 8) {
            return getBinaryAsLongField(length, signed);
        } else {
            return getBinaryAsBigIntegerField(length, signed);
        }
    }

    public ExternalDecimalAsIntField getExternalDecimalAsIntField(int length, boolean signed) {
        return getExternalDecimalAsIntField(length, signed, true, false, false);
    }

    public ExternalDecimalAsIntField getExternalDecimalAsIntField(int length, boolean signed, boolean signTrailing, boolean signExternal, boolean blankWhenZero) {
        return new ExternalDecimalAsIntField(offset, length, signed, signTrailing, signExternal, blankWhenZero);
    }

    public ExternalDecimalAsLongField getExternalDecimalAsLongField(int length, boolean signed) {
        return getExternalDecimalAsLongField(length, signed, true, false, false);
    }

    public ExternalDecimalAsLongField getExternalDecimalAsLongField(int length, boolean signed, boolean signTrailing, boolean signExternal, boolean blankWhenZero) {
        return new ExternalDecimalAsLongField(offset, length, signed, signTrailing, signExternal, blankWhenZero);
    }

    public ExternalDecimalAsBigIntegerField getExternalDecimalAsBigIntegerField(int length, boolean signed) {
        return getExternalDecimalAsBigIntegerField(length, 0, signed, true, false, false);
    }

    public ExternalDecimalAsBigIntegerField getExternalDecimalAsBigIntegerField(int length, int scale, boolean signed, boolean signTrailing, boolean signExternal, boolean blankWhenZero) {
        return new ExternalDecimalAsBigIntegerField(offset, length, scale, signed, signTrailing, signExternal, blankWhenZero);
    }

    public ExternalDecimalAsBigDecimalField getExternalDecimalAsBigDecimalField(int length, int scale, boolean signed) {
        return getExternalDecimalAsBigDecimalField(length, scale, signed, true, false, false);
    }

    public ExternalDecimalAsBigDecimalField getExternalDecimalAsBigDecimalField(int length, int scale, boolean signed, boolean signTrailing, boolean signExternal, boolean blankWhenZero) {
        return new ExternalDecimalAsBigDecimalField(offset, length, scale, signed, signTrailing, signExternal, blankWhenZero);
    }

    public Field getExternalDecimalField(int length, int scale, boolean signed, boolean signTrailing, boolean signExternal, boolean blankWhenZero) {
        if (scale > 0) {
            return getExternalDecimalAsBigDecimalField(length, scale, signed, signTrailing, signExternal, blankWhenZero);
        } else if (length <= 9) {
            return getExternalDecimalAsIntField(length, signed, signTrailing, signExternal, blankWhenZero);
        } else if (length <= 18) {
            return getExternalDecimalAsLongField(length, signed, signTrailing, signExternal, blankWhenZero);
        } else {
            return getExternalDecimalAsBigIntegerField(length, scale, signed, signTrailing, signExternal, blankWhenZero);
        }
    }

    public ByteArrayField getByteArrayField(int length) {
        return new ByteArrayField(offset, length);
    }

    public IbmFloatField getIbmFloatField() {
        return new IbmFloatField(offset);
    }

    public IbmDoubleField getIbmDoubleField() {
        return new IbmDoubleField(offset);
    }

    public PackedDecimalAsIntField getPackedDecimalAsIntField(int precision, boolean signed) {
        return new PackedDecimalAsIntField(offset, precision, signed);
    }

    public PackedDecimalAsLongField getPackedDecimalAsLongField(int precision, boolean signed) {
        return new PackedDecimalAsLongField(offset, precision, signed);
    }

    public PackedDecimalAsBigIntegerField getPackedDecimalAsBigIntegerField(int precision, int scale, boolean signed) {
        return new PackedDecimalAsBigIntegerField(offset, precision, scale, signed);
    }

    public PackedDecimalAsBigDecimalField getPackedDecimalAsBigDecimalField(int precision, int scale, boolean signed) {
        return new PackedDecimalAsBigDecimalField(offset, precision, scale, signed);
    }

    public abstract Field getPackedDecimalField(int precision, int scale, boolean signed);

    public StringField getStringField(int length) {
        return getStringField(length, stringTrimDefault);
    }

    public StringField getStringField(int length, boolean trim) {
        return getStringField(length, trim, false, false, stringEncoding);
    }

    public StringField getStringField(int length, boolean trim, boolean padLeft) {
        return getStringField(length, trim, padLeft, false, stringEncoding);
    }

    public StringField getStringField(int length, boolean trim, boolean padLeft, boolean allowTruncation) {
        return getStringField(length, trim, padLeft, allowTruncation, stringEncoding);
    }

    public StringField getStringField(int length, boolean trim, boolean padLeft, boolean allowTruncation, String encoding) {
        return new StringField(offset, length, trim, padLeft, allowTruncation, encoding);
    }
}
