package com.ibm.jzos.fields;

public class CobolDatatypeFactory extends DatatypeFactory {

    public CobolDatatypeFactory() {
        super();
    }

    public Field getBinaryField(int digits, int scale, boolean signed, boolean nativeBinary) {
        int byteLen;
        if (digits <= 4) {
            byteLen = 2;
        } else if (digits <= 9) {
            byteLen = 4;
        } else {
            byteLen = 8;
        }
        if (scale > 0) {
            return new BinaryAsBigDecimalField(offset, byteLen, scale, signed);
        } else if (byteLen <= 4) {
            return new BinaryAsIntField(offset, byteLen, signed);
        } else {
            return new BinaryAsLongField(offset, byteLen, signed);
        }
    }

    public ExternalFloatField getExternalFloatField(int length, int scale, boolean impliedDecimal, boolean showMantissaPlusSign, boolean showExponentPlusSign) {
        return new ExternalFloatField(offset, length, scale, impliedDecimal, showMantissaPlusSign, showExponentPlusSign);
    }

    @Override
    public Field getPackedDecimalField(int precision, int scale, boolean signed) {
        if (scale > 0) {
            return getPackedDecimalAsBigDecimalField(precision, scale, signed);
        } else if (precision <= 9) {
            return getPackedDecimalAsIntField(precision, signed);
        } else if (precision <= 18) {
            return getPackedDecimalAsLongField(precision, signed);
        } else {
            return getPackedDecimalAsBigIntegerField(precision, scale, signed);
        }
    }
}
