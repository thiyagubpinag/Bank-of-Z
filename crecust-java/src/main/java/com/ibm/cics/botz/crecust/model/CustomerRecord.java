package com.ibm.cics.botz.crecust.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for the CUSTOMER.cpy record (397-byte customer record).
 *
 * <p>Does NOT implement {@link com.ibm.cics.botz.common.ByteArraySerializable} — no serializer
 * required for this class.
 *
 * @see "CRECUST.cbl LOCAL-STORAGE — OUTPUT-DATA / CUSTOMER-RECORD (line 364)"
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class CustomerRecord {

    /** CUSTOMER-EYECATCHER PIC X(4) — 4 bytes. 88: 'CUST'. */
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

    /** CUSTOMER-STATUS PIC X(10) — 10 bytes. 88s: ACTIVE, INACTIVE, SUSPENDED. */
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
}
