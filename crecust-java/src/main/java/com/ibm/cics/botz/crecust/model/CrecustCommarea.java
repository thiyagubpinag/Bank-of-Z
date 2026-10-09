package com.ibm.cics.botz.crecust.model;

import com.ibm.cics.botz.common.ByteArraySerializable;
import com.ibm.cics.botz.common.ByteArraySerializer;
import com.ibm.cics.botz.crecust.serializer.CrecustareaSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for the CRECUST DFHCOMMAREA / CRECUST.cpy (399-byte commarea).
 *
 * <p>Implements {@link ByteArraySerializable} — serializer wired in Story 2.5.
 *
 * @see "CRECUST.cbl LINKAGE SECTION — DFHCOMMAREA (lines 401–)"
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class CrecustCommarea implements ByteArraySerializable<CrecustCommarea> {

    /** COMM-EYECATCHER PIC X(4) — Set to 'CUST' on success. */
    private String commEyecatcher;

    /** COMM-SORTCODE PIC 9(6) DISPLAY — Incoming sort code. */
    private String commSortcode;

    /** COMM-NUMBER PIC 9(10) DISPLAY — Assigned customer number (output). */
    private String commNumber;

    /** COMM-TITLE PIC X(10) — Customer title (input, validated). */
    private String commTitle;

    /** COMM-FIRST-NAME PIC X(50). */
    private String commFirstName;

    /** COMM-LAST-NAME PIC X(50). */
    private String commLastName;

    /** COMM-DOB-DAY PIC 99 DISPLAY. */
    private String commDobDay;

    /** COMM-DOB-MONTH PIC 99 DISPLAY. */
    private String commDobMonth;

    /** COMM-DOB-YEAR PIC 9999 DISPLAY. */
    private String commDobYear;

    /** COMM-PHONE PIC X(20). */
    private String commPhone;

    /** COMM-ADDR-LINE1 PIC X(50). */
    private String commAddrLine1;

    /** COMM-ADDR-LINE2 PIC X(50). */
    private String commAddrLine2;

    /** COMM-CITY PIC X(50). */
    private String commCity;

    /** COMM-POSTCODE PIC X(10). */
    private String commPostcode;

    /** COMM-COUNTRY PIC X(50). */
    private String commCountry;

    /** COMM-STATUS PIC X(10). */
    private String commStatus;

    /** COMM-CREATED-DAY PIC 99 DISPLAY. */
    private String commCreatedDay;

    /** COMM-CREATED-MONTH PIC 99 DISPLAY. */
    private String commCreatedMonth;

    /** COMM-CREATED-YEAR PIC 9999 DISPLAY. */
    private String commCreatedYear;

    /** COMM-CREDIT-SCORE PIC 999 — Set by credit check (output). */
    private String commCreditScore;

    /** COMM-CS-REVIEW-DAY PIC 99 DISPLAY (output). */
    private String commCsReviewDay;

    /** COMM-CS-REVIEW-MONTH PIC 99 DISPLAY (output). */
    private String commCsReviewMonth;

    /** COMM-CS-REVIEW-YEAR PIC 9999 DISPLAY (output). */
    private String commCsReviewYear;

    /** COMM-SUCCESS PIC X — 'Y'=success, 'N'=fail (output). */
    private String commSuccess;

    /** COMM-FAIL-CODE PIC X — fail-code character (output). */
    private String commFailCode;

    /**
     * Returns the serializer for this type.
     *
     * @return serializer for {@code CrecustCommarea}
     */
    @Override
    public ByteArraySerializer<CrecustCommarea> serializer() {
        return CrecustareaSerializer.INSTANCE;
    }

    /**
     * Sets this object's fields from {@code that} (COBOL MOVE semantics).
     *
     * @param that source object
     */
    @Override
    public void set(CrecustCommarea that) {
        if (that == this) return;
        this.commEyecatcher = that.commEyecatcher;
        this.commSortcode = that.commSortcode;
        this.commNumber = that.commNumber;
        this.commTitle = that.commTitle;
        this.commFirstName = that.commFirstName;
        this.commLastName = that.commLastName;
        this.commDobDay = that.commDobDay;
        this.commDobMonth = that.commDobMonth;
        this.commDobYear = that.commDobYear;
        this.commPhone = that.commPhone;
        this.commAddrLine1 = that.commAddrLine1;
        this.commAddrLine2 = that.commAddrLine2;
        this.commCity = that.commCity;
        this.commPostcode = that.commPostcode;
        this.commCountry = that.commCountry;
        this.commStatus = that.commStatus;
        this.commCreatedDay = that.commCreatedDay;
        this.commCreatedMonth = that.commCreatedMonth;
        this.commCreatedYear = that.commCreatedYear;
        this.commCreditScore = that.commCreditScore;
        this.commCsReviewDay = that.commCsReviewDay;
        this.commCsReviewMonth = that.commCsReviewMonth;
        this.commCsReviewYear = that.commCsReviewYear;
        this.commSuccess = that.commSuccess;
        this.commFailCode = that.commFailCode;
    }
}
