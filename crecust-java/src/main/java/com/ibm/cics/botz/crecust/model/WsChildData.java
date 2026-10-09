package com.ibm.cics.botz.crecust.model;

import com.ibm.cics.botz.common.ByteArraySerializable;
import com.ibm.cics.botz.common.ByteArraySerializer;
import com.ibm.cics.botz.crecust.serializer.WsChildDataSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for {@code WS-CHILD-DATA} / {@code WS-CHILD-CUSTOMER-RECORD} (399-byte structure).
 *
 * <p>Mirrors the 23 fields of {@link CustomerRecord} (397 bytes) and adds two extra fields:
 * {@code wsChildSuccess} and {@code wsChildFailCode} (1 byte each) for a total of 399 bytes.
 *
 * <p>Implements {@link ByteArraySerializable} — serializer wired in Story 2.5.
 *
 * @see "CRECUST.cbl WORKING-STORAGE — WS-CHILD-DATA (line 523)"
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class WsChildData implements ByteArraySerializable<WsChildData> {

    // ---- Mirror of CUSTOMER-RECORD (397 bytes) ----

    /** CUSTOMER-EYECATCHER PIC X(4) — 4 bytes. */
    private String customerEyecatcher;

    /** CUSTOMER-SORTCODE PIC 9(6) DISPLAY — 6 bytes. */
    private String customerSortcode;

    /** CUSTOMER-NUMBER PIC 9(10) DISPLAY — 10 bytes. */
    private String customerNumber;

    /** CUSTOMER-TITLE PIC X(10) — 10 bytes. */
    private String customerTitle;

    /** CUSTOMER-FIRST-NAME PIC X(50) — 50 bytes. */
    private String customerFirstName;

    /** CUSTOMER-LAST-NAME PIC X(50) — 50 bytes. */
    private String customerLastName;

    /** CUSTOMER-DOB-DAY PIC 99 DISPLAY — 2 bytes. */
    private String customerDobDay;

    /** CUSTOMER-DOB-MONTH PIC 99 DISPLAY — 2 bytes. */
    private String customerDobMonth;

    /** CUSTOMER-DOB-YEAR PIC 9999 DISPLAY — 4 bytes. */
    private String customerDobYear;

    /** CUSTOMER-PHONE PIC X(20) — 20 bytes. */
    private String customerPhone;

    /** CUSTOMER-ADDR-LINE1 PIC X(50) — 50 bytes. */
    private String customerAddrLine1;

    /** CUSTOMER-ADDR-LINE2 PIC X(50) — 50 bytes. */
    private String customerAddrLine2;

    /** CUSTOMER-CITY PIC X(50) — 50 bytes. */
    private String customerCity;

    /** CUSTOMER-POSTCODE PIC X(10) — 10 bytes. */
    private String customerPostcode;

    /** CUSTOMER-COUNTRY PIC X(50) — 50 bytes. */
    private String customerCountry;

    /** CUSTOMER-STATUS PIC X(10) — 10 bytes. */
    private String customerStatus;

    /** CUSTOMER-CREATED-DAY PIC 99 DISPLAY — 2 bytes. */
    private String customerCreatedDay;

    /** CUSTOMER-CREATED-MONTH PIC 99 DISPLAY — 2 bytes. */
    private String customerCreatedMonth;

    /** CUSTOMER-CREATED-YEAR PIC 9999 DISPLAY — 4 bytes. */
    private String customerCreatedYear;

    /** CUSTOMER-CREDIT-SCORE PIC 999 — 3 bytes. */
    private String customerCreditScore;

    /** CUSTOMER-CS-REVIEW-DAY PIC 99 DISPLAY — 2 bytes. */
    private String customerCsReviewDay;

    /** CUSTOMER-CS-REVIEW-MONTH PIC 99 DISPLAY — 2 bytes. */
    private String customerCsReviewMonth;

    /** CUSTOMER-CS-REVIEW-YEAR PIC 9999 DISPLAY — 4 bytes. */
    private String customerCsReviewYear;

    // ---- Extra fields (2 bytes) ----

    /** WS-CHILD-SUCCESS PIC X — 1 byte. */
    private String wsChildSuccess;

    /** WS-CHILD-FAIL-CODE PIC X — 1 byte. */
    private String wsChildFailCode;

    /**
     * Returns the serializer for this type.
     *
     * @return serializer for {@code WsChildData}
     */
    @Override
    public ByteArraySerializer<WsChildData> serializer() {
        return WsChildDataSerializer.INSTANCE;
    }

    /**
     * Sets this object's fields from {@code that} (COBOL MOVE semantics).
     *
     * @param that source object
     */
    @Override
    public void set(WsChildData that) {
        if (that == this) return;
        this.customerEyecatcher = that.customerEyecatcher;
        this.customerSortcode = that.customerSortcode;
        this.customerNumber = that.customerNumber;
        this.customerTitle = that.customerTitle;
        this.customerFirstName = that.customerFirstName;
        this.customerLastName = that.customerLastName;
        this.customerDobDay = that.customerDobDay;
        this.customerDobMonth = that.customerDobMonth;
        this.customerDobYear = that.customerDobYear;
        this.customerPhone = that.customerPhone;
        this.customerAddrLine1 = that.customerAddrLine1;
        this.customerAddrLine2 = that.customerAddrLine2;
        this.customerCity = that.customerCity;
        this.customerPostcode = that.customerPostcode;
        this.customerCountry = that.customerCountry;
        this.customerStatus = that.customerStatus;
        this.customerCreatedDay = that.customerCreatedDay;
        this.customerCreatedMonth = that.customerCreatedMonth;
        this.customerCreatedYear = that.customerCreatedYear;
        this.customerCreditScore = that.customerCreditScore;
        this.customerCsReviewDay = that.customerCsReviewDay;
        this.customerCsReviewMonth = that.customerCsReviewMonth;
        this.customerCsReviewYear = that.customerCsReviewYear;
        this.wsChildSuccess = that.wsChildSuccess;
        this.wsChildFailCode = that.wsChildFailCode;
    }
}
