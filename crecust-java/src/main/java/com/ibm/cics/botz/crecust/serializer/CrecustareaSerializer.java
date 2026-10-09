package com.ibm.cics.botz.crecust.serializer;

import com.ibm.cics.botz.common.ByteArraySerializer;
import com.ibm.cics.botz.crecust.model.CrecustCommarea;
import com.ibm.jzos.fields.CobolDatatypeFactory;
import com.ibm.jzos.fields.StringField;

import java.nio.charset.Charset;

/**
 * Canonical {@link ByteArraySerializer} implementation for the CRECUST DFHCOMMAREA / {@code CRECUST.cpy} layout.
 *
 * <p>Total layout size: 399 bytes (25 fields).
 * Implements single source-of-truth serialization/deserialization with explicit bounds checks,
 * named offset/width constants, and EBCDIC (IBM-1047) string encoding.
 *
 * <p>The partial-copy overload {@link #fromBytes(byte[], int, int)} supports the EIBCALEN guard
 * (Rule 6 / AC-5.4): only fields whose byte range falls entirely within {@code [offset, offset+length)}
 * are populated; all other fields are left at Java defaults (null for String).
 *
 * @see "CRECUST.cpy (399 bytes, 25 fields)"
 */
public class CrecustareaSerializer implements ByteArraySerializer<CrecustCommarea> {

    /** Singleton instance of {@code CrecustareaSerializer}. */
    public static final CrecustareaSerializer INSTANCE = new CrecustareaSerializer();

    /** Maximum CRECUST commarea length as defined by the COBOL copybook (EIBCALEN upper bound). */
    public static final int MAX_COMMAREA_LENGTH = 399;

    private static final Charset ENCODING = ByteArraySerializer.encoding;
    private static final CobolDatatypeFactory FACTORY = new CobolDatatypeFactory();

    static {
        FACTORY.setStringTrimDefault(false);
        FACTORY.setStringEncoding(ENCODING.name());
    }

    // ---------------------------------------------------------------------------
    // Named field-offset constants (PE-1, technical-research.md lines 500-524)
    // ---------------------------------------------------------------------------

    /** COMM-EYECATCHER: PIC X(4), offset 0. */
    private static final int COMM_EYECATCHER_OFFSET = 0;
    /** COMM-EYECATCHER field width in bytes. */
    private static final int COMM_EYECATCHER_WIDTH  = 4;

    /** COMM-SORTCODE: PIC 9(6) DISPLAY, offset 4. */
    private static final int COMM_SORTCODE_OFFSET = 4;
    /** COMM-SORTCODE field width in bytes. */
    private static final int COMM_SORTCODE_WIDTH  = 6;

    /** COMM-NUMBER: PIC 9(10) DISPLAY, offset 10. */
    private static final int COMM_NUMBER_OFFSET = 10;
    /** COMM-NUMBER field width in bytes. */
    private static final int COMM_NUMBER_WIDTH  = 10;

    /** COMM-TITLE: PIC X(10), offset 20. */
    private static final int COMM_TITLE_OFFSET = 20;
    /** COMM-TITLE field width in bytes. */
    private static final int COMM_TITLE_WIDTH  = 10;

    /** COMM-FIRST-NAME: PIC X(50), offset 30. */
    private static final int COMM_FIRST_NAME_OFFSET = 30;
    /** COMM-FIRST-NAME field width in bytes. */
    private static final int COMM_FIRST_NAME_WIDTH  = 50;

    /** COMM-LAST-NAME: PIC X(50), offset 80. */
    private static final int COMM_LAST_NAME_OFFSET = 80;
    /** COMM-LAST-NAME field width in bytes. */
    private static final int COMM_LAST_NAME_WIDTH  = 50;

    /** COMM-DOB-DAY: PIC 99 DISPLAY, offset 130. */
    private static final int COMM_DOB_DAY_OFFSET = 130;
    /** COMM-DOB-DAY field width in bytes. */
    private static final int COMM_DOB_DAY_WIDTH  = 2;

    /** COMM-DOB-MONTH: PIC 99 DISPLAY, offset 132. */
    private static final int COMM_DOB_MONTH_OFFSET = 132;
    /** COMM-DOB-MONTH field width in bytes. */
    private static final int COMM_DOB_MONTH_WIDTH  = 2;

    /** COMM-DOB-YEAR: PIC 9999 DISPLAY, offset 134. */
    private static final int COMM_DOB_YEAR_OFFSET = 134;
    /** COMM-DOB-YEAR field width in bytes. */
    private static final int COMM_DOB_YEAR_WIDTH  = 4;

    /** COMM-PHONE: PIC X(20), offset 138. */
    private static final int COMM_PHONE_OFFSET = 138;
    /** COMM-PHONE field width in bytes. */
    private static final int COMM_PHONE_WIDTH  = 20;

    /** COMM-ADDR-LINE1: PIC X(50), offset 158. */
    private static final int COMM_ADDR_LINE1_OFFSET = 158;
    /** COMM-ADDR-LINE1 field width in bytes. */
    private static final int COMM_ADDR_LINE1_WIDTH  = 50;

    /** COMM-ADDR-LINE2: PIC X(50), offset 208. */
    private static final int COMM_ADDR_LINE2_OFFSET = 208;
    /** COMM-ADDR-LINE2 field width in bytes. */
    private static final int COMM_ADDR_LINE2_WIDTH  = 50;

    /** COMM-CITY: PIC X(50), offset 258. */
    private static final int COMM_CITY_OFFSET = 258;
    /** COMM-CITY field width in bytes. */
    private static final int COMM_CITY_WIDTH  = 50;

    /** COMM-POSTCODE: PIC X(10), offset 308. */
    private static final int COMM_POSTCODE_OFFSET = 308;
    /** COMM-POSTCODE field width in bytes. */
    private static final int COMM_POSTCODE_WIDTH  = 10;

    /** COMM-COUNTRY: PIC X(50), offset 318. */
    private static final int COMM_COUNTRY_OFFSET = 318;
    /** COMM-COUNTRY field width in bytes. */
    private static final int COMM_COUNTRY_WIDTH  = 50;

    /** COMM-STATUS: PIC X(10), offset 368. */
    private static final int COMM_STATUS_OFFSET = 368;
    /** COMM-STATUS field width in bytes. */
    private static final int COMM_STATUS_WIDTH  = 10;

    /** COMM-CREATED-DAY: PIC 99 DISPLAY, offset 378. */
    private static final int COMM_CREATED_DAY_OFFSET = 378;
    /** COMM-CREATED-DAY field width in bytes. */
    private static final int COMM_CREATED_DAY_WIDTH  = 2;

    /** COMM-CREATED-MONTH: PIC 99 DISPLAY, offset 380. */
    private static final int COMM_CREATED_MONTH_OFFSET = 380;
    /** COMM-CREATED-MONTH field width in bytes. */
    private static final int COMM_CREATED_MONTH_WIDTH  = 2;

    /** COMM-CREATED-YEAR: PIC 9999 DISPLAY, offset 382. */
    private static final int COMM_CREATED_YEAR_OFFSET = 382;
    /** COMM-CREATED-YEAR field width in bytes. */
    private static final int COMM_CREATED_YEAR_WIDTH  = 4;

    /** COMM-CREDIT-SCORE: PIC 999, offset 386. */
    private static final int COMM_CREDIT_SCORE_OFFSET = 386;
    /** COMM-CREDIT-SCORE field width in bytes. */
    private static final int COMM_CREDIT_SCORE_WIDTH  = 3;

    /** COMM-CS-REVIEW-DAY: PIC 99 DISPLAY, offset 389. */
    private static final int COMM_CS_REVIEW_DAY_OFFSET = 389;
    /** COMM-CS-REVIEW-DAY field width in bytes. */
    private static final int COMM_CS_REVIEW_DAY_WIDTH  = 2;

    /** COMM-CS-REVIEW-MONTH: PIC 99 DISPLAY, offset 391. */
    private static final int COMM_CS_REVIEW_MONTH_OFFSET = 391;
    /** COMM-CS-REVIEW-MONTH field width in bytes. */
    private static final int COMM_CS_REVIEW_MONTH_WIDTH  = 2;

    /** COMM-CS-REVIEW-YEAR: PIC 9999 DISPLAY, offset 393. */
    private static final int COMM_CS_REVIEW_YEAR_OFFSET = 393;
    /** COMM-CS-REVIEW-YEAR field width in bytes. */
    private static final int COMM_CS_REVIEW_YEAR_WIDTH  = 4;

    /** COMM-SUCCESS: PIC X, offset 397. */
    private static final int COMM_SUCCESS_OFFSET = 397;
    /** COMM-SUCCESS field width in bytes. */
    private static final int COMM_SUCCESS_WIDTH  = 1;

    /** COMM-FAIL-CODE: PIC X, offset 398. */
    private static final int COMM_FAIL_CODE_OFFSET = 398;
    /** COMM-FAIL-CODE field width in bytes. */
    private static final int COMM_FAIL_CODE_WIDTH  = 1;

    // ---------------------------------------------------------------------------
    // StringField instances (ordered — offset tracked by CobolDatatypeFactory)
    // ---------------------------------------------------------------------------

    private static final StringField COMM_EYECATCHER     = FACTORY.getStringField(COMM_EYECATCHER_WIDTH);
    private static final StringField COMM_SORTCODE       = FACTORY.getStringField(COMM_SORTCODE_WIDTH);
    private static final StringField COMM_NUMBER         = FACTORY.getStringField(COMM_NUMBER_WIDTH);
    private static final StringField COMM_TITLE          = FACTORY.getStringField(COMM_TITLE_WIDTH);
    private static final StringField COMM_FIRST_NAME     = FACTORY.getStringField(COMM_FIRST_NAME_WIDTH);
    private static final StringField COMM_LAST_NAME      = FACTORY.getStringField(COMM_LAST_NAME_WIDTH);
    private static final StringField COMM_DOB_DAY        = FACTORY.getStringField(COMM_DOB_DAY_WIDTH);
    private static final StringField COMM_DOB_MONTH      = FACTORY.getStringField(COMM_DOB_MONTH_WIDTH);
    private static final StringField COMM_DOB_YEAR       = FACTORY.getStringField(COMM_DOB_YEAR_WIDTH);
    private static final StringField COMM_PHONE          = FACTORY.getStringField(COMM_PHONE_WIDTH);
    private static final StringField COMM_ADDR_LINE1     = FACTORY.getStringField(COMM_ADDR_LINE1_WIDTH);
    private static final StringField COMM_ADDR_LINE2     = FACTORY.getStringField(COMM_ADDR_LINE2_WIDTH);
    private static final StringField COMM_CITY           = FACTORY.getStringField(COMM_CITY_WIDTH);
    private static final StringField COMM_POSTCODE       = FACTORY.getStringField(COMM_POSTCODE_WIDTH);
    private static final StringField COMM_COUNTRY        = FACTORY.getStringField(COMM_COUNTRY_WIDTH);
    private static final StringField COMM_STATUS         = FACTORY.getStringField(COMM_STATUS_WIDTH);
    private static final StringField COMM_CREATED_DAY    = FACTORY.getStringField(COMM_CREATED_DAY_WIDTH);
    private static final StringField COMM_CREATED_MONTH  = FACTORY.getStringField(COMM_CREATED_MONTH_WIDTH);
    private static final StringField COMM_CREATED_YEAR   = FACTORY.getStringField(COMM_CREATED_YEAR_WIDTH);
    private static final StringField COMM_CREDIT_SCORE   = FACTORY.getStringField(COMM_CREDIT_SCORE_WIDTH);
    private static final StringField COMM_CS_REVIEW_DAY  = FACTORY.getStringField(COMM_CS_REVIEW_DAY_WIDTH);
    private static final StringField COMM_CS_REVIEW_MONTH = FACTORY.getStringField(COMM_CS_REVIEW_MONTH_WIDTH);
    private static final StringField COMM_CS_REVIEW_YEAR = FACTORY.getStringField(COMM_CS_REVIEW_YEAR_WIDTH);
    private static final StringField COMM_SUCCESS        = FACTORY.getStringField(COMM_SUCCESS_WIDTH);
    private static final StringField COMM_FAIL_CODE      = FACTORY.getStringField(COMM_FAIL_CODE_WIDTH);

    /** Total byte length of the serialized CRECUST commarea (must equal {@link #MAX_COMMAREA_LENGTH}). */
    public static final int SIZE = FACTORY.getOffset();

    @Override
    public int numBytes() {
        return SIZE;
    }

    // ---------------------------------------------------------------------------
    // Serialization (write path)
    // ---------------------------------------------------------------------------

    /**
     * Serializes {@code obj} into {@code bytes} starting at {@code offset}.
     *
     * <p>Throws {@link IllegalArgumentException} if the buffer is too small to hold any field.
     * Unlike deserialization, the write path always requires a full-size buffer; no padding
     * is performed so that a short buffer is detected and reported immediately.
     *
     * @param bytes  target byte array; must have at least {@code offset + SIZE} bytes
     * @param offset starting offset
     * @param obj    commarea to serialize
     * @return {@code bytes} populated with EBCDIC-encoded field data
     * @throws IllegalArgumentException if {@code bytes} is null or too short for any field
     */
    @Override
    public byte[] toBytes(byte[] bytes, int offset, final CrecustCommarea obj) {
        if (bytes == null) {
            throw new IllegalArgumentException("Target byte buffer cannot be null");
        }

        checkWriteBounds("COMM-EYECATCHER",      bytes, offset, COMM_EYECATCHER_OFFSET,      COMM_EYECATCHER_WIDTH);
        COMM_EYECATCHER.putString(padRight(obj.getCommEyecatcher(), COMM_EYECATCHER_WIDTH), bytes, offset);

        checkWriteBounds("COMM-SORTCODE",        bytes, offset, COMM_SORTCODE_OFFSET,        COMM_SORTCODE_WIDTH);
        COMM_SORTCODE.putString(padRight(obj.getCommSortcode(), COMM_SORTCODE_WIDTH), bytes, offset);

        checkWriteBounds("COMM-NUMBER",          bytes, offset, COMM_NUMBER_OFFSET,          COMM_NUMBER_WIDTH);
        COMM_NUMBER.putString(padRight(obj.getCommNumber(), COMM_NUMBER_WIDTH), bytes, offset);

        checkWriteBounds("COMM-TITLE",           bytes, offset, COMM_TITLE_OFFSET,           COMM_TITLE_WIDTH);
        COMM_TITLE.putString(padRight(obj.getCommTitle(), COMM_TITLE_WIDTH), bytes, offset);

        checkWriteBounds("COMM-FIRST-NAME",      bytes, offset, COMM_FIRST_NAME_OFFSET,      COMM_FIRST_NAME_WIDTH);
        COMM_FIRST_NAME.putString(padRight(obj.getCommFirstName(), COMM_FIRST_NAME_WIDTH), bytes, offset);

        checkWriteBounds("COMM-LAST-NAME",       bytes, offset, COMM_LAST_NAME_OFFSET,       COMM_LAST_NAME_WIDTH);
        COMM_LAST_NAME.putString(padRight(obj.getCommLastName(), COMM_LAST_NAME_WIDTH), bytes, offset);

        checkWriteBounds("COMM-DOB-DAY",         bytes, offset, COMM_DOB_DAY_OFFSET,         COMM_DOB_DAY_WIDTH);
        COMM_DOB_DAY.putString(padRight(obj.getCommDobDay(), COMM_DOB_DAY_WIDTH), bytes, offset);

        checkWriteBounds("COMM-DOB-MONTH",       bytes, offset, COMM_DOB_MONTH_OFFSET,       COMM_DOB_MONTH_WIDTH);
        COMM_DOB_MONTH.putString(padRight(obj.getCommDobMonth(), COMM_DOB_MONTH_WIDTH), bytes, offset);

        checkWriteBounds("COMM-DOB-YEAR",        bytes, offset, COMM_DOB_YEAR_OFFSET,        COMM_DOB_YEAR_WIDTH);
        COMM_DOB_YEAR.putString(padRight(obj.getCommDobYear(), COMM_DOB_YEAR_WIDTH), bytes, offset);

        checkWriteBounds("COMM-PHONE",           bytes, offset, COMM_PHONE_OFFSET,           COMM_PHONE_WIDTH);
        COMM_PHONE.putString(padRight(obj.getCommPhone(), COMM_PHONE_WIDTH), bytes, offset);

        checkWriteBounds("COMM-ADDR-LINE1",      bytes, offset, COMM_ADDR_LINE1_OFFSET,      COMM_ADDR_LINE1_WIDTH);
        COMM_ADDR_LINE1.putString(padRight(obj.getCommAddrLine1(), COMM_ADDR_LINE1_WIDTH), bytes, offset);

        checkWriteBounds("COMM-ADDR-LINE2",      bytes, offset, COMM_ADDR_LINE2_OFFSET,      COMM_ADDR_LINE2_WIDTH);
        COMM_ADDR_LINE2.putString(padRight(obj.getCommAddrLine2(), COMM_ADDR_LINE2_WIDTH), bytes, offset);

        checkWriteBounds("COMM-CITY",            bytes, offset, COMM_CITY_OFFSET,            COMM_CITY_WIDTH);
        COMM_CITY.putString(padRight(obj.getCommCity(), COMM_CITY_WIDTH), bytes, offset);

        checkWriteBounds("COMM-POSTCODE",        bytes, offset, COMM_POSTCODE_OFFSET,        COMM_POSTCODE_WIDTH);
        COMM_POSTCODE.putString(padRight(obj.getCommPostcode(), COMM_POSTCODE_WIDTH), bytes, offset);

        checkWriteBounds("COMM-COUNTRY",         bytes, offset, COMM_COUNTRY_OFFSET,         COMM_COUNTRY_WIDTH);
        COMM_COUNTRY.putString(padRight(obj.getCommCountry(), COMM_COUNTRY_WIDTH), bytes, offset);

        checkWriteBounds("COMM-STATUS",          bytes, offset, COMM_STATUS_OFFSET,          COMM_STATUS_WIDTH);
        COMM_STATUS.putString(padRight(obj.getCommStatus(), COMM_STATUS_WIDTH), bytes, offset);

        checkWriteBounds("COMM-CREATED-DAY",     bytes, offset, COMM_CREATED_DAY_OFFSET,     COMM_CREATED_DAY_WIDTH);
        COMM_CREATED_DAY.putString(padRight(obj.getCommCreatedDay(), COMM_CREATED_DAY_WIDTH), bytes, offset);

        checkWriteBounds("COMM-CREATED-MONTH",   bytes, offset, COMM_CREATED_MONTH_OFFSET,   COMM_CREATED_MONTH_WIDTH);
        COMM_CREATED_MONTH.putString(padRight(obj.getCommCreatedMonth(), COMM_CREATED_MONTH_WIDTH), bytes, offset);

        checkWriteBounds("COMM-CREATED-YEAR",    bytes, offset, COMM_CREATED_YEAR_OFFSET,    COMM_CREATED_YEAR_WIDTH);
        COMM_CREATED_YEAR.putString(padRight(obj.getCommCreatedYear(), COMM_CREATED_YEAR_WIDTH), bytes, offset);

        checkWriteBounds("COMM-CREDIT-SCORE",    bytes, offset, COMM_CREDIT_SCORE_OFFSET,    COMM_CREDIT_SCORE_WIDTH);
        COMM_CREDIT_SCORE.putString(padRight(obj.getCommCreditScore(), COMM_CREDIT_SCORE_WIDTH), bytes, offset);

        checkWriteBounds("COMM-CS-REVIEW-DAY",   bytes, offset, COMM_CS_REVIEW_DAY_OFFSET,   COMM_CS_REVIEW_DAY_WIDTH);
        COMM_CS_REVIEW_DAY.putString(padRight(obj.getCommCsReviewDay(), COMM_CS_REVIEW_DAY_WIDTH), bytes, offset);

        checkWriteBounds("COMM-CS-REVIEW-MONTH", bytes, offset, COMM_CS_REVIEW_MONTH_OFFSET, COMM_CS_REVIEW_MONTH_WIDTH);
        COMM_CS_REVIEW_MONTH.putString(padRight(obj.getCommCsReviewMonth(), COMM_CS_REVIEW_MONTH_WIDTH), bytes, offset);

        checkWriteBounds("COMM-CS-REVIEW-YEAR",  bytes, offset, COMM_CS_REVIEW_YEAR_OFFSET,  COMM_CS_REVIEW_YEAR_WIDTH);
        COMM_CS_REVIEW_YEAR.putString(padRight(obj.getCommCsReviewYear(), COMM_CS_REVIEW_YEAR_WIDTH), bytes, offset);

        checkWriteBounds("COMM-SUCCESS",         bytes, offset, COMM_SUCCESS_OFFSET,         COMM_SUCCESS_WIDTH);
        COMM_SUCCESS.putString(padRight(obj.getCommSuccess(), COMM_SUCCESS_WIDTH), bytes, offset);

        checkWriteBounds("COMM-FAIL-CODE",       bytes, offset, COMM_FAIL_CODE_OFFSET,       COMM_FAIL_CODE_WIDTH);
        COMM_FAIL_CODE.putString(padRight(obj.getCommFailCode(), COMM_FAIL_CODE_WIDTH), bytes, offset);

        return bytes;
    }

    // ---------------------------------------------------------------------------
    // Deserialization (read path)
    // ---------------------------------------------------------------------------

    /**
     * Partial-copy deserialization overload supporting the EIBCALEN guard (Rule 6).
     *
     * <p>Only fields whose byte range falls entirely within {@code [0, length)} are read from
     * the source array. The check performed per-field is:
     * <pre>
     *     if (FIELD_OFFSET + FIELD_WIDTH &lt;= length) {
     *         obj.setField(FIELD.getString(bytes, offset));
     *     }
     *     // else: leave field at Java default (null for String)
     * </pre>
     *
     * <p>No {@link ArrayIndexOutOfBoundsException} is thrown for any {@code length} in {@code [0, MAX_COMMAREA_LENGTH]}.
     *
     * @param bytes  source byte array; must be at least {@code offset + length} bytes long
     * @param offset starting offset within {@code bytes}
     * @param length number of valid commarea bytes available (e.g. EIBCALEN); may be 0
     * @return deserialized {@link CrecustCommarea}; fields beyond {@code length} are null
     */
    public CrecustCommarea fromBytes(byte[] bytes, int offset, int length) {
        CrecustCommarea obj = new CrecustCommarea();
        if (bytes == null || length <= 0) {
            return obj;
        }

        if (COMM_EYECATCHER_OFFSET + COMM_EYECATCHER_WIDTH <= length) {
            obj.setCommEyecatcher(COMM_EYECATCHER.getString(bytes, offset));
        }
        if (COMM_SORTCODE_OFFSET + COMM_SORTCODE_WIDTH <= length) {
            obj.setCommSortcode(COMM_SORTCODE.getString(bytes, offset));
        }
        if (COMM_NUMBER_OFFSET + COMM_NUMBER_WIDTH <= length) {
            obj.setCommNumber(COMM_NUMBER.getString(bytes, offset));
        }
        if (COMM_TITLE_OFFSET + COMM_TITLE_WIDTH <= length) {
            obj.setCommTitle(COMM_TITLE.getString(bytes, offset));
        }
        if (COMM_FIRST_NAME_OFFSET + COMM_FIRST_NAME_WIDTH <= length) {
            obj.setCommFirstName(COMM_FIRST_NAME.getString(bytes, offset));
        }
        if (COMM_LAST_NAME_OFFSET + COMM_LAST_NAME_WIDTH <= length) {
            obj.setCommLastName(COMM_LAST_NAME.getString(bytes, offset));
        }
        if (COMM_DOB_DAY_OFFSET + COMM_DOB_DAY_WIDTH <= length) {
            obj.setCommDobDay(COMM_DOB_DAY.getString(bytes, offset));
        }
        if (COMM_DOB_MONTH_OFFSET + COMM_DOB_MONTH_WIDTH <= length) {
            obj.setCommDobMonth(COMM_DOB_MONTH.getString(bytes, offset));
        }
        if (COMM_DOB_YEAR_OFFSET + COMM_DOB_YEAR_WIDTH <= length) {
            obj.setCommDobYear(COMM_DOB_YEAR.getString(bytes, offset));
        }
        if (COMM_PHONE_OFFSET + COMM_PHONE_WIDTH <= length) {
            obj.setCommPhone(COMM_PHONE.getString(bytes, offset));
        }
        if (COMM_ADDR_LINE1_OFFSET + COMM_ADDR_LINE1_WIDTH <= length) {
            obj.setCommAddrLine1(COMM_ADDR_LINE1.getString(bytes, offset));
        }
        if (COMM_ADDR_LINE2_OFFSET + COMM_ADDR_LINE2_WIDTH <= length) {
            obj.setCommAddrLine2(COMM_ADDR_LINE2.getString(bytes, offset));
        }
        if (COMM_CITY_OFFSET + COMM_CITY_WIDTH <= length) {
            obj.setCommCity(COMM_CITY.getString(bytes, offset));
        }
        if (COMM_POSTCODE_OFFSET + COMM_POSTCODE_WIDTH <= length) {
            obj.setCommPostcode(COMM_POSTCODE.getString(bytes, offset));
        }
        if (COMM_COUNTRY_OFFSET + COMM_COUNTRY_WIDTH <= length) {
            obj.setCommCountry(COMM_COUNTRY.getString(bytes, offset));
        }
        if (COMM_STATUS_OFFSET + COMM_STATUS_WIDTH <= length) {
            obj.setCommStatus(COMM_STATUS.getString(bytes, offset));
        }
        if (COMM_CREATED_DAY_OFFSET + COMM_CREATED_DAY_WIDTH <= length) {
            obj.setCommCreatedDay(COMM_CREATED_DAY.getString(bytes, offset));
        }
        if (COMM_CREATED_MONTH_OFFSET + COMM_CREATED_MONTH_WIDTH <= length) {
            obj.setCommCreatedMonth(COMM_CREATED_MONTH.getString(bytes, offset));
        }
        if (COMM_CREATED_YEAR_OFFSET + COMM_CREATED_YEAR_WIDTH <= length) {
            obj.setCommCreatedYear(COMM_CREATED_YEAR.getString(bytes, offset));
        }
        if (COMM_CREDIT_SCORE_OFFSET + COMM_CREDIT_SCORE_WIDTH <= length) {
            obj.setCommCreditScore(COMM_CREDIT_SCORE.getString(bytes, offset));
        }
        if (COMM_CS_REVIEW_DAY_OFFSET + COMM_CS_REVIEW_DAY_WIDTH <= length) {
            obj.setCommCsReviewDay(COMM_CS_REVIEW_DAY.getString(bytes, offset));
        }
        if (COMM_CS_REVIEW_MONTH_OFFSET + COMM_CS_REVIEW_MONTH_WIDTH <= length) {
            obj.setCommCsReviewMonth(COMM_CS_REVIEW_MONTH.getString(bytes, offset));
        }
        if (COMM_CS_REVIEW_YEAR_OFFSET + COMM_CS_REVIEW_YEAR_WIDTH <= length) {
            obj.setCommCsReviewYear(COMM_CS_REVIEW_YEAR.getString(bytes, offset));
        }
        if (COMM_SUCCESS_OFFSET + COMM_SUCCESS_WIDTH <= length) {
            obj.setCommSuccess(COMM_SUCCESS.getString(bytes, offset));
        }
        if (COMM_FAIL_CODE_OFFSET + COMM_FAIL_CODE_WIDTH <= length) {
            obj.setCommFailCode(COMM_FAIL_CODE.getString(bytes, offset));
        }

        return obj;
    }

    /**
     * Deserializes a commarea from {@code bytes} starting at {@code offset}, treating all
     * remaining bytes as valid (i.e. {@code length = bytes.length - offset}).
     *
     * <p>Delegates entirely to {@link #fromBytes(byte[], int, int)} — no field-read logic here.
     *
     * @param bytes  source byte array
     * @param offset starting offset
     * @return deserialized {@link CrecustCommarea}
     */
    @Override
    public CrecustCommarea fromBytes(byte[] bytes, int offset) {
        if (bytes == null) {
            return new CrecustCommarea();
        }
        int available = bytes.length - offset;
        return fromBytes(bytes, offset, available < 0 ? 0 : available);
    }

    /**
     * Deserializes a commarea from the full byte array.
     *
     * <p>Overrides the interface default to delegate to {@link #fromBytes(byte[], int, int)}
     * with {@code offset = 0} and {@code length = bytes.length}, preserving no-regression
     * behaviour for existing callers while routing through the single deserialization path.
     *
     * @param bytes source byte array
     * @return deserialized {@link CrecustCommarea}
     */
    @Override
    public CrecustCommarea fromBytes(byte[] bytes) {
        if (bytes == null) {
            return new CrecustCommarea();
        }
        return fromBytes(bytes, 0, bytes.length);
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    /**
     * Throws {@link IllegalArgumentException} if the buffer is too small to accommodate a field write.
     *
     * @param fieldName   display name for the error message
     * @param buffer      the target byte array
     * @param offset      base offset within {@code buffer}
     * @param fieldOffset field's start offset within the commarea
     * @param fieldWidth  field width in bytes
     */
    private static void checkWriteBounds(String fieldName, byte[] buffer, int offset,
                                         int fieldOffset, int fieldWidth) {
        int required = offset + fieldOffset + fieldWidth;
        if (buffer.length < required) {
            throw new IllegalArgumentException(
                    "Target buffer too short for field " + fieldName
                    + ": required minimum " + required + " bytes, got " + buffer.length);
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
