package com.ibm.cics.botz.crecust.serializer;

import com.ibm.cics.botz.common.ByteArraySerializer;
import com.ibm.cics.botz.crecust.model.WsChildData;
import com.ibm.jzos.fields.CobolDatatypeFactory;
import com.ibm.jzos.fields.StringField;

import java.nio.charset.Charset;

/**
 * Canonical {@link ByteArraySerializer} implementation for {@code WS-CHILD-DATA} (399 bytes).
 *
 * <p>Mirrors the 23 fields of {@code CUSTOMER-RECORD} (397 bytes) plus 2 extra trailing fields:
 * {@code WS-CHILD-SUCCESS} (1 byte) and {@code WS-CHILD-FAIL-CODE} (1 byte), totaling 399 bytes.
 * Implements single source-of-truth serialization/deserialization with explicit bounds checks,
 * named offset constants via {@link CobolDatatypeFactory}, and EBCDIC (IBM-1047) string encoding.
 *
 * @see "CRECUST.cbl WORKING-STORAGE — WS-CHILD-DATA (399 bytes, 25 fields)"
 */
public class WsChildDataSerializer implements ByteArraySerializer<WsChildData> {

    /** Singleton instance of {@code WsChildDataSerializer}. */
    public static final WsChildDataSerializer INSTANCE = new WsChildDataSerializer();

    private static final Charset ENCODING = ByteArraySerializer.encoding;
    private static final CobolDatatypeFactory FACTORY = new CobolDatatypeFactory();

    static {
        FACTORY.setStringTrimDefault(false);
        FACTORY.setStringEncoding(ENCODING.name());
    }

    // 05 CUSTOMER-EYECATCHER PIC X(4) -> offset 0, len 4
    private static final StringField CUSTOMER_EYECATCHER = FACTORY.getStringField(4);
    // 07 CUSTOMER-SORTCODE PIC 9(6) DISPLAY -> offset 4, len 6
    private static final StringField CUSTOMER_SORTCODE = FACTORY.getStringField(6);
    // 07 CUSTOMER-NUMBER PIC 9(10) DISPLAY -> offset 10, len 10
    private static final StringField CUSTOMER_NUMBER = FACTORY.getStringField(10);
    // 07 CUSTOMER-TITLE PIC X(10) -> offset 20, len 10
    private static final StringField CUSTOMER_TITLE = FACTORY.getStringField(10);
    // 07 CUSTOMER-FIRST-NAME PIC X(50) -> offset 30, len 50
    private static final StringField CUSTOMER_FIRST_NAME = FACTORY.getStringField(50);
    // 07 CUSTOMER-LAST-NAME PIC X(50) -> offset 80, len 50
    private static final StringField CUSTOMER_LAST_NAME = FACTORY.getStringField(50);
    // 07 CUSTOMER-DOB-DAY PIC 99 DISPLAY -> offset 130, len 2
    private static final StringField CUSTOMER_DOB_DAY = FACTORY.getStringField(2);
    // 07 CUSTOMER-DOB-MONTH PIC 99 DISPLAY -> offset 132, len 2
    private static final StringField CUSTOMER_DOB_MONTH = FACTORY.getStringField(2);
    // 07 CUSTOMER-DOB-YEAR PIC 9999 DISPLAY -> offset 134, len 4
    private static final StringField CUSTOMER_DOB_YEAR = FACTORY.getStringField(4);
    // 05 CUSTOMER-PHONE PIC X(20) -> offset 138, len 20
    private static final StringField CUSTOMER_PHONE = FACTORY.getStringField(20);
    // 07 CUSTOMER-ADDR-LINE1 PIC X(50) -> offset 158, len 50
    private static final StringField CUSTOMER_ADDR_LINE1 = FACTORY.getStringField(50);
    // 07 CUSTOMER-ADDR-LINE2 PIC X(50) -> offset 208, len 50
    private static final StringField CUSTOMER_ADDR_LINE2 = FACTORY.getStringField(50);
    // 07 CUSTOMER-CITY PIC X(50) -> offset 258, len 50
    private static final StringField CUSTOMER_CITY = FACTORY.getStringField(50);
    // 07 CUSTOMER-POSTCODE PIC X(10) -> offset 308, len 10
    private static final StringField CUSTOMER_POSTCODE = FACTORY.getStringField(10);
    // 07 CUSTOMER-COUNTRY PIC X(50) -> offset 318, len 50
    private static final StringField CUSTOMER_COUNTRY = FACTORY.getStringField(50);
    // 05 CUSTOMER-STATUS PIC X(10) -> offset 368, len 10
    private static final StringField CUSTOMER_STATUS = FACTORY.getStringField(10);
    // 07 CUSTOMER-CREATED-DAY PIC 99 DISPLAY -> offset 378, len 2
    private static final StringField CUSTOMER_CREATED_DAY = FACTORY.getStringField(2);
    // 07 CUSTOMER-CREATED-MONTH PIC 99 DISPLAY -> offset 380, len 2
    private static final StringField CUSTOMER_CREATED_MONTH = FACTORY.getStringField(2);
    // 07 CUSTOMER-CREATED-YEAR PIC 9999 DISPLAY -> offset 382, len 4
    private static final StringField CUSTOMER_CREATED_YEAR = FACTORY.getStringField(4);
    // 05 CUSTOMER-CREDIT-SCORE PIC 999 -> offset 386, len 3
    private static final StringField CUSTOMER_CREDIT_SCORE = FACTORY.getStringField(3);
    // 07 CUSTOMER-CS-REVIEW-DAY PIC 99 DISPLAY -> offset 389, len 2
    private static final StringField CUSTOMER_CS_REVIEW_DAY = FACTORY.getStringField(2);
    // 07 CUSTOMER-CS-REVIEW-MONTH PIC 99 DISPLAY -> offset 391, len 2
    private static final StringField CUSTOMER_CS_REVIEW_MONTH = FACTORY.getStringField(2);
    // 07 CUSTOMER-CS-REVIEW-YEAR PIC 9999 DISPLAY -> offset 393, len 4
    private static final StringField CUSTOMER_CS_REVIEW_YEAR = FACTORY.getStringField(4);
    // 05 WS-CHILD-SUCCESS PIC X -> offset 397, len 1
    private static final StringField WS_CHILD_SUCCESS = FACTORY.getStringField(1);
    // 05 WS-CHILD-FAIL-CODE PIC X -> offset 398, len 1
    private static final StringField WS_CHILD_FAIL_CODE = FACTORY.getStringField(1);

    /** Total byte length of the serialized WS-CHILD-DATA record. */
    public static final int SIZE = FACTORY.getOffset();

    @Override
    public int numBytes() {
        return SIZE;
    }

    @Override
    public byte[] toBytes(byte[] bytes, int offset, final WsChildData obj) {
        if (bytes == null) {
            throw new IllegalArgumentException("Target byte buffer cannot be null");
        }
        bytes = padToSize(bytes, offset, SIZE);

        checkWriteBounds("CUSTOMER-EYECATCHER", bytes, offset, CUSTOMER_EYECATCHER);
        CUSTOMER_EYECATCHER.putString(padRight(obj.getCustomerEyecatcher(), 4), bytes, offset);

        checkWriteBounds("CUSTOMER-SORTCODE", bytes, offset, CUSTOMER_SORTCODE);
        CUSTOMER_SORTCODE.putString(padRight(obj.getCustomerSortcode(), 6), bytes, offset);

        checkWriteBounds("CUSTOMER-NUMBER", bytes, offset, CUSTOMER_NUMBER);
        CUSTOMER_NUMBER.putString(padRight(obj.getCustomerNumber(), 10), bytes, offset);

        checkWriteBounds("CUSTOMER-TITLE", bytes, offset, CUSTOMER_TITLE);
        CUSTOMER_TITLE.putString(padRight(obj.getCustomerTitle(), 10), bytes, offset);

        checkWriteBounds("CUSTOMER-FIRST-NAME", bytes, offset, CUSTOMER_FIRST_NAME);
        CUSTOMER_FIRST_NAME.putString(padRight(obj.getCustomerFirstName(), 50), bytes, offset);

        checkWriteBounds("CUSTOMER-LAST-NAME", bytes, offset, CUSTOMER_LAST_NAME);
        CUSTOMER_LAST_NAME.putString(padRight(obj.getCustomerLastName(), 50), bytes, offset);

        checkWriteBounds("CUSTOMER-DOB-DAY", bytes, offset, CUSTOMER_DOB_DAY);
        CUSTOMER_DOB_DAY.putString(padRight(obj.getCustomerDobDay(), 2), bytes, offset);

        checkWriteBounds("CUSTOMER-DOB-MONTH", bytes, offset, CUSTOMER_DOB_MONTH);
        CUSTOMER_DOB_MONTH.putString(padRight(obj.getCustomerDobMonth(), 2), bytes, offset);

        checkWriteBounds("CUSTOMER-DOB-YEAR", bytes, offset, CUSTOMER_DOB_YEAR);
        CUSTOMER_DOB_YEAR.putString(padRight(obj.getCustomerDobYear(), 4), bytes, offset);

        checkWriteBounds("CUSTOMER-PHONE", bytes, offset, CUSTOMER_PHONE);
        CUSTOMER_PHONE.putString(padRight(obj.getCustomerPhone(), 20), bytes, offset);

        checkWriteBounds("CUSTOMER-ADDR-LINE1", bytes, offset, CUSTOMER_ADDR_LINE1);
        CUSTOMER_ADDR_LINE1.putString(padRight(obj.getCustomerAddrLine1(), 50), bytes, offset);

        checkWriteBounds("CUSTOMER-ADDR-LINE2", bytes, offset, CUSTOMER_ADDR_LINE2);
        CUSTOMER_ADDR_LINE2.putString(padRight(obj.getCustomerAddrLine2(), 50), bytes, offset);

        checkWriteBounds("CUSTOMER-CITY", bytes, offset, CUSTOMER_CITY);
        CUSTOMER_CITY.putString(padRight(obj.getCustomerCity(), 50), bytes, offset);

        checkWriteBounds("CUSTOMER-POSTCODE", bytes, offset, CUSTOMER_POSTCODE);
        CUSTOMER_POSTCODE.putString(padRight(obj.getCustomerPostcode(), 10), bytes, offset);

        checkWriteBounds("CUSTOMER-COUNTRY", bytes, offset, CUSTOMER_COUNTRY);
        CUSTOMER_COUNTRY.putString(padRight(obj.getCustomerCountry(), 50), bytes, offset);

        checkWriteBounds("CUSTOMER-STATUS", bytes, offset, CUSTOMER_STATUS);
        CUSTOMER_STATUS.putString(padRight(obj.getCustomerStatus(), 10), bytes, offset);

        checkWriteBounds("CUSTOMER-CREATED-DAY", bytes, offset, CUSTOMER_CREATED_DAY);
        CUSTOMER_CREATED_DAY.putString(padRight(obj.getCustomerCreatedDay(), 2), bytes, offset);

        checkWriteBounds("CUSTOMER-CREATED-MONTH", bytes, offset, CUSTOMER_CREATED_MONTH);
        CUSTOMER_CREATED_MONTH.putString(padRight(obj.getCustomerCreatedMonth(), 2), bytes, offset);

        checkWriteBounds("CUSTOMER-CREATED-YEAR", bytes, offset, CUSTOMER_CREATED_YEAR);
        CUSTOMER_CREATED_YEAR.putString(padRight(obj.getCustomerCreatedYear(), 4), bytes, offset);

        checkWriteBounds("CUSTOMER-CREDIT-SCORE", bytes, offset, CUSTOMER_CREDIT_SCORE);
        CUSTOMER_CREDIT_SCORE.putString(padRight(obj.getCustomerCreditScore(), 3), bytes, offset);

        checkWriteBounds("CUSTOMER-CS-REVIEW-DAY", bytes, offset, CUSTOMER_CS_REVIEW_DAY);
        CUSTOMER_CS_REVIEW_DAY.putString(padRight(obj.getCustomerCsReviewDay(), 2), bytes, offset);

        checkWriteBounds("CUSTOMER-CS-REVIEW-MONTH", bytes, offset, CUSTOMER_CS_REVIEW_MONTH);
        CUSTOMER_CS_REVIEW_MONTH.putString(padRight(obj.getCustomerCsReviewMonth(), 2), bytes, offset);

        checkWriteBounds("CUSTOMER-CS-REVIEW-YEAR", bytes, offset, CUSTOMER_CS_REVIEW_YEAR);
        CUSTOMER_CS_REVIEW_YEAR.putString(padRight(obj.getCustomerCsReviewYear(), 4), bytes, offset);

        checkWriteBounds("WS-CHILD-SUCCESS", bytes, offset, WS_CHILD_SUCCESS);
        WS_CHILD_SUCCESS.putString(padRight(obj.getWsChildSuccess(), 1), bytes, offset);

        checkWriteBounds("WS-CHILD-FAIL-CODE", bytes, offset, WS_CHILD_FAIL_CODE);
        WS_CHILD_FAIL_CODE.putString(padRight(obj.getWsChildFailCode(), 1), bytes, offset);

        return bytes;
    }

    @Override
    public WsChildData fromBytes(byte[] bytes, int offset) {
        if (bytes == null) {
            return new WsChildData();
        }
        byte[] padded = padToSize(bytes, offset, SIZE);
        WsChildData obj = new WsChildData();
        obj.setCustomerEyecatcher(CUSTOMER_EYECATCHER.getString(padded, offset));
        obj.setCustomerSortcode(CUSTOMER_SORTCODE.getString(padded, offset));
        obj.setCustomerNumber(CUSTOMER_NUMBER.getString(padded, offset));
        obj.setCustomerTitle(CUSTOMER_TITLE.getString(padded, offset));
        obj.setCustomerFirstName(CUSTOMER_FIRST_NAME.getString(padded, offset));
        obj.setCustomerLastName(CUSTOMER_LAST_NAME.getString(padded, offset));
        obj.setCustomerDobDay(CUSTOMER_DOB_DAY.getString(padded, offset));
        obj.setCustomerDobMonth(CUSTOMER_DOB_MONTH.getString(padded, offset));
        obj.setCustomerDobYear(CUSTOMER_DOB_YEAR.getString(padded, offset));
        obj.setCustomerPhone(CUSTOMER_PHONE.getString(padded, offset));
        obj.setCustomerAddrLine1(CUSTOMER_ADDR_LINE1.getString(padded, offset));
        obj.setCustomerAddrLine2(CUSTOMER_ADDR_LINE2.getString(padded, offset));
        obj.setCustomerCity(CUSTOMER_CITY.getString(padded, offset));
        obj.setCustomerPostcode(CUSTOMER_POSTCODE.getString(padded, offset));
        obj.setCustomerCountry(CUSTOMER_COUNTRY.getString(padded, offset));
        obj.setCustomerStatus(CUSTOMER_STATUS.getString(padded, offset));
        obj.setCustomerCreatedDay(CUSTOMER_CREATED_DAY.getString(padded, offset));
        obj.setCustomerCreatedMonth(CUSTOMER_CREATED_MONTH.getString(padded, offset));
        obj.setCustomerCreatedYear(CUSTOMER_CREATED_YEAR.getString(padded, offset));
        obj.setCustomerCreditScore(CUSTOMER_CREDIT_SCORE.getString(padded, offset));
        obj.setCustomerCsReviewDay(CUSTOMER_CS_REVIEW_DAY.getString(padded, offset));
        obj.setCustomerCsReviewMonth(CUSTOMER_CS_REVIEW_MONTH.getString(padded, offset));
        obj.setCustomerCsReviewYear(CUSTOMER_CS_REVIEW_YEAR.getString(padded, offset));
        obj.setWsChildSuccess(WS_CHILD_SUCCESS.getString(padded, offset));
        obj.setWsChildFailCode(WS_CHILD_FAIL_CODE.getString(padded, offset));
        return obj;
    }

    private static void checkWriteBounds(String fieldName, byte[] buffer, int offset, StringField field) {
        int required = offset + field.getOffset() + field.getByteLength();
        if (buffer.length < required) {
            throw new IllegalArgumentException(
                    "Target buffer too short for field " + fieldName + ": required minimum " + required + ", got " + buffer.length);
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
