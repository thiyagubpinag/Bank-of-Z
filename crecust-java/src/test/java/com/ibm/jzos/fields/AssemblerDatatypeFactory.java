package com.ibm.jzos.fields;

public class AssemblerDatatypeFactory extends DatatypeFactory {

    public AssemblerDatatypeFactory() {
        super();
    }

    public Field getZonedDecimalField(int length, int scale, boolean signed) {
        return getExternalDecimalField(length, scale, signed, true, false, false);
    }

    @Override
    public Field getPackedDecimalField(int length, int scale, boolean signed) {
        int precision = (length * 2) - 1;
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
