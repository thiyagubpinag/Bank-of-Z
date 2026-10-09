package com.ibm.jzos.fields;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class StringField implements Field {
    public static final char HIGH_VALUE = '\uffff';
    public static final String DEFAULT_ENCODING = "ISO-8859-1";

    protected int offset;
    protected int length;
    protected boolean trim;
    protected boolean padLeft;
    protected boolean allowTruncation;
    protected String encoding;

    public StringField(int offset, int length) {
        this(offset, length, false, false, false, DEFAULT_ENCODING);
    }

    public StringField(int offset, int length, boolean trim) {
        this(offset, length, trim, false, false, DEFAULT_ENCODING);
    }

    public StringField(int offset, int length, boolean trim, boolean padLeft) {
        this(offset, length, trim, padLeft, false, DEFAULT_ENCODING);
    }

    public StringField(int offset, int length, boolean trim, boolean padLeft, boolean allowTruncation) {
        this(offset, length, trim, padLeft, allowTruncation, DEFAULT_ENCODING);
    }

    public StringField(int offset, int length, boolean trim, boolean padLeft, boolean allowTruncation, String encoding) {
        this.offset = offset;
        this.length = length;
        this.trim = trim;
        this.padLeft = padLeft;
        this.allowTruncation = allowTruncation;
        this.encoding = encoding != null ? encoding : DEFAULT_ENCODING;
    }

    public static String makeString(int len, char c) {
        char[] arr = new char[len];
        Arrays.fill(arr, c);
        return new String(arr);
    }

    @Override
    public int getByteLength() {
        return length;
    }

    public void setByteLength(int length) {
        this.length = length;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public void setOffset(int offset) {
        this.offset = offset;
    }

    public String getString(byte[] buffer) {
        return getString(buffer, 0);
    }

    public String getString(byte[] buffer, int off) {
        off += this.offset;
        Charset cs;
        try {
            cs = Charset.forName(encoding);
        } catch (Exception e) {
            cs = StandardCharsets.ISO_8859_1;
        }
        String str = new String(buffer, off, length, cs);
        return trim ? str.trim() : str;
    }

    public void putString(String val, byte[] buffer) {
        putString(val, buffer, 0);
    }

    public void putString(String val, byte[] buffer, int off) {
        off += this.offset;
        if (val == null) {
            val = "";
        }
        Charset cs;
        try {
            cs = Charset.forName(encoding);
        } catch (Exception e) {
            cs = StandardCharsets.ISO_8859_1;
        }
        byte[] bytes = val.getBytes(cs);
        byte padByte = (byte) ' ';
        if (bytes.length < length) {
            byte[] padded = new byte[length];
            Arrays.fill(padded, padByte);
            if (padLeft) {
                System.arraycopy(bytes, 0, padded, length - bytes.length, bytes.length);
            } else {
                System.arraycopy(bytes, 0, padded, 0, bytes.length);
            }
            System.arraycopy(padded, 0, buffer, off, length);
        } else {
            System.arraycopy(bytes, 0, buffer, off, length);
        }
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public boolean isAllowTruncation() {
        return allowTruncation;
    }

    public void setAllowTruncation(boolean allowTruncation) {
        this.allowTruncation = allowTruncation;
    }

    public boolean isPadLeft() {
        return padLeft;
    }

    public void setPadLeft(boolean padLeft) {
        this.padLeft = padLeft;
    }

    public boolean isTrim() {
        return trim;
    }

    public void setTrim(boolean trim) {
        this.trim = trim;
    }

    public boolean equals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
