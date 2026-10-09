package com.ibm.cics.botz.crecust.serializer;

import com.ibm.cics.botz.common.ByteArraySerializer;
import com.ibm.cics.botz.crecust.model.AbndInfoRec;
import com.ibm.jzos.fields.CobolDatatypeFactory;
import com.ibm.jzos.fields.ExternalDecimalAsIntField;
import com.ibm.jzos.fields.PackedDecimalAsLongField;
import com.ibm.jzos.fields.StringField;

import java.nio.charset.Charset;

/**
 * Canonical {@link ByteArraySerializer} implementation for the {@code ABNDINFO-REC} / {@code ABNDINFO.cpy} layout.
 *
 * <p>Total layout size: 681 bytes (12 fields).
 * Implements single source-of-truth serialization/deserialization with explicit bounds checks,
 * named offset constants via {@link CobolDatatypeFactory}, and EBCDIC (IBM-1047) string encoding.
 *
 * @see "ABNDINFO.cpy (681 bytes, 12 fields)"
 */
public class AbndInfoRecSerializer implements ByteArraySerializer<AbndInfoRec> {

    /** Singleton instance of {@code AbndInfoRecSerializer}. */
    public static final AbndInfoRecSerializer INSTANCE = new AbndInfoRecSerializer();

    private static final Charset ENCODING = ByteArraySerializer.encoding;
    private static final CobolDatatypeFactory FACTORY = new CobolDatatypeFactory();

    static {
        FACTORY.setStringTrimDefault(false);
        FACTORY.setStringEncoding(ENCODING.name());
    }

    // 05 ABND-UTIME-KEY PIC S9(15) COMP-3 -> offset 0, len 8
    private static final PackedDecimalAsLongField ABND_UTIME_KEY = FACTORY.getPackedDecimalAsLongField(15, true);
    // 05 ABND-TASKNO-KEY PIC 9(4) -> offset 8, len 4
    private static final StringField ABND_TASKNO_KEY = FACTORY.getStringField(4);
    // 03 ABND-APPLID PIC X(8) -> offset 12, len 8
    private static final StringField ABND_APPLID = FACTORY.getStringField(8);
    // 03 ABND-TRANID PIC X(4) -> offset 20, len 4
    private static final StringField ABND_TRANID = FACTORY.getStringField(4);
    // 03 ABND-DATE PIC X(10) -> offset 24, len 10
    private static final StringField ABND_DATE = FACTORY.getStringField(10);
    // 03 ABND-TIME PIC X(8) -> offset 34, len 8
    private static final StringField ABND_TIME = FACTORY.getStringField(8);
    // 03 ABND-CODE PIC X(4) -> offset 42, len 4
    private static final StringField ABND_CODE = FACTORY.getStringField(4);
    // 03 ABND-PROGRAM PIC X(8) -> offset 46, len 8
    private static final StringField ABND_PROGRAM = FACTORY.getStringField(8);
    // 03 ABND-RESPCODE PIC S9(8) DISPLAY SIGN LEADING SEPARATE -> offset 54, len 9
    private static final ExternalDecimalAsIntField ABND_RESPCODE = FACTORY.getExternalDecimalAsIntField(8, true, true, true, false);
    // 03 ABND-RESP2CODE PIC S9(8) DISPLAY SIGN LEADING SEPARATE -> offset 63, len 9
    private static final ExternalDecimalAsIntField ABND_RESP2CODE = FACTORY.getExternalDecimalAsIntField(8, true, true, true, false);
    // 03 ABND-SQLCODE PIC S9(8) DISPLAY SIGN LEADING SEPARATE -> offset 72, len 9
    private static final ExternalDecimalAsIntField ABND_SQLCODE = FACTORY.getExternalDecimalAsIntField(8, true, true, true, false);
    // 03 ABND-FREEFORM PIC X(600) -> offset 81, len 600
    private static final StringField ABND_FREEFORM = FACTORY.getStringField(600);

    /** Total byte length of the serialized ABNDINFO-REC record (681 bytes). */
    public static final int SIZE = FACTORY.getOffset();

    @Override
    public int numBytes() {
        return SIZE;
    }

    @Override
    public byte[] toBytes(byte[] bytes, int offset, final AbndInfoRec obj) {
        if (bytes == null) {
            throw new IllegalArgumentException("Target byte buffer cannot be null");
        }
        bytes = padToSize(bytes, offset, SIZE);

        checkWriteBounds("ABND-UTIME-KEY", bytes, offset, ABND_UTIME_KEY.getOffset(), ABND_UTIME_KEY.getByteLength());
        ABND_UTIME_KEY.putLong(obj.getAbndUtimeKey(), bytes, offset);

        checkWriteBounds("ABND-TASKNO-KEY", bytes, offset, ABND_TASKNO_KEY.getOffset(), ABND_TASKNO_KEY.getByteLength());
        ABND_TASKNO_KEY.putString(padRight(obj.getAbndTasknoKey(), 4), bytes, offset);

        checkWriteBounds("ABND-APPLID", bytes, offset, ABND_APPLID.getOffset(), ABND_APPLID.getByteLength());
        ABND_APPLID.putString(padRight(obj.getAbndApplid(), 8), bytes, offset);

        checkWriteBounds("ABND-TRANID", bytes, offset, ABND_TRANID.getOffset(), ABND_TRANID.getByteLength());
        ABND_TRANID.putString(padRight(obj.getAbndTranid(), 4), bytes, offset);

        checkWriteBounds("ABND-DATE", bytes, offset, ABND_DATE.getOffset(), ABND_DATE.getByteLength());
        ABND_DATE.putString(padRight(obj.getAbndDate(), 10), bytes, offset);

        checkWriteBounds("ABND-TIME", bytes, offset, ABND_TIME.getOffset(), ABND_TIME.getByteLength());
        ABND_TIME.putString(padRight(obj.getAbndTime(), 8), bytes, offset);

        checkWriteBounds("ABND-CODE", bytes, offset, ABND_CODE.getOffset(), ABND_CODE.getByteLength());
        ABND_CODE.putString(padRight(obj.getAbndCode(), 4), bytes, offset);

        checkWriteBounds("ABND-PROGRAM", bytes, offset, ABND_PROGRAM.getOffset(), ABND_PROGRAM.getByteLength());
        ABND_PROGRAM.putString(padRight(obj.getAbndProgram(), 8), bytes, offset);

        checkWriteBounds("ABND-RESPCODE", bytes, offset, ABND_RESPCODE.getOffset(), ABND_RESPCODE.getByteLength());
        ABND_RESPCODE.putInt(parseIntOrDefault(obj.getAbndRespcode(), 0), bytes, offset);

        checkWriteBounds("ABND-RESP2CODE", bytes, offset, ABND_RESP2CODE.getOffset(), ABND_RESP2CODE.getByteLength());
        ABND_RESP2CODE.putInt(parseIntOrDefault(obj.getAbndResp2code(), 0), bytes, offset);

        checkWriteBounds("ABND-SQLCODE", bytes, offset, ABND_SQLCODE.getOffset(), ABND_SQLCODE.getByteLength());
        ABND_SQLCODE.putInt(parseIntOrDefault(obj.getAbndSqlcode(), 0), bytes, offset);

        checkWriteBounds("ABND-FREEFORM", bytes, offset, ABND_FREEFORM.getOffset(), ABND_FREEFORM.getByteLength());
        ABND_FREEFORM.putString(padRight(obj.getAbndFreeform(), 600), bytes, offset);

        return bytes;
    }

    @Override
    public AbndInfoRec fromBytes(byte[] bytes, int offset) {
        if (bytes == null) {
            return new AbndInfoRec();
        }
        byte[] padded = padToSize(bytes, offset, SIZE);
        AbndInfoRec obj = new AbndInfoRec();
        obj.setAbndUtimeKey(ABND_UTIME_KEY.getLong(padded, offset));
        obj.setAbndTasknoKey(ABND_TASKNO_KEY.getString(padded, offset));
        obj.setAbndApplid(ABND_APPLID.getString(padded, offset));
        obj.setAbndTranid(ABND_TRANID.getString(padded, offset));
        obj.setAbndDate(ABND_DATE.getString(padded, offset));
        obj.setAbndTime(ABND_TIME.getString(padded, offset));
        obj.setAbndCode(ABND_CODE.getString(padded, offset));
        obj.setAbndProgram(ABND_PROGRAM.getString(padded, offset));
        obj.setAbndRespcode(String.valueOf(ABND_RESPCODE.getInt(padded, offset)));
        obj.setAbndResp2code(String.valueOf(ABND_RESP2CODE.getInt(padded, offset)));
        obj.setAbndSqlcode(String.valueOf(ABND_SQLCODE.getInt(padded, offset)));
        obj.setAbndFreeform(ABND_FREEFORM.getString(padded, offset));
        return obj;
    }

    private static void checkWriteBounds(String fieldName, byte[] buffer, int offset, int fieldOffset, int fieldLength) {
        int required = offset + fieldOffset + fieldLength;
        if (buffer.length < required) {
            throw new IllegalArgumentException(
                    "Target buffer too short for field " + fieldName + ": required minimum " + required + ", got " + buffer.length);
        }
    }

    private static int parseIntOrDefault(String s, int defaultValue) {
        if (s == null || s.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String padRight(String s, int len) {
        if (s == null) {
            return " ".repeat(len);
        }
        if (s.length() >= len) {
            return s.substring(0, len);
        }
        return s + " ".repeat(len - s.length());
    }
}
