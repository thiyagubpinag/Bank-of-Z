package com.ibm.cics.botz.crecust.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DB2 host-variable row class for the {@code CUSTOMER} table ({@code HOST-CUSTOMER-ROW}).
 *
 * <p>17 fields. Date fields ({@code hvCustomerDob}, {@code hvCustomerCreateDate},
 * {@code hvCustomerCsReviewDate}) are stored as {@code int} with YYYYMMDD encoding (from
 * {@code S9(9) COMP} INTEGER columns). {@code hvCustomerCreditScore} is {@code short}
 * (SMALLINT from {@code S9(4) COMP}).
 *
 * @see "CRECUST.cbl WORKING-STORAGE — HOST-CUSTOMER-ROW (line 111)"
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class HostCustomerRow {

    /** HV-CUSTOMER-EYECATCHER PIC X(4) — 4 bytes. Lit 'CUST'. */
    private String hvCustomerEyecatcher;

    /** HV-CUSTOMER-SORTCODE PIC X(6) — 6 bytes. */
    private String hvCustomerSortcode;

    /** HV-CUSTOMER-NUMBER PIC X(10) — 10 bytes. */
    private String hvCustomerNumber;

    /** HV-CUSTOMER-TITLE PIC X(10) — 10 bytes. */
    private String hvCustomerTitle;

    /** HV-CUSTOMER-FIRST-NAME PIC X(50) — 50 bytes. */
    private String hvCustomerFirstName;

    /** HV-CUSTOMER-LAST-NAME PIC X(50) — 50 bytes. */
    private String hvCustomerLastName;

    /**
     * HV-CUSTOMER-DOB PIC S9(9) COMP — 4 bytes.
     *
     * <p>Mapped to {@code int}; YYYYMMDD-encoded integer (DB2 INTEGER column).
     */
    private int hvCustomerDob;

    /** HV-CUSTOMER-PHONE PIC X(20) — 20 bytes. */
    private String hvCustomerPhone;

    /** HV-CUSTOMER-ADDR-LINE1 PIC X(50) — 50 bytes. */
    private String hvCustomerAddrLine1;

    /** HV-CUSTOMER-ADDR-LINE2 PIC X(50) — 50 bytes. */
    private String hvCustomerAddrLine2;

    /** HV-CUSTOMER-CITY PIC X(50) — 50 bytes. */
    private String hvCustomerCity;

    /** HV-CUSTOMER-POSTCODE PIC X(10) — 10 bytes. */
    private String hvCustomerPostcode;

    /** HV-CUSTOMER-COUNTRY PIC X(50) — 50 bytes. */
    private String hvCustomerCountry;

    /** HV-CUSTOMER-STATUS PIC X(10) — 10 bytes. */
    private String hvCustomerStatus;

    /**
     * HV-CUSTOMER-CREATE-DATE PIC S9(9) COMP — 4 bytes.
     *
     * <p>Mapped to {@code int}; YYYYMMDD encoding (DB2 INTEGER column).
     */
    private int hvCustomerCreateDate;

    /**
     * HV-CUSTOMER-CREDIT-SCORE PIC S9(4) COMP — 2 bytes.
     *
     * <p>Mapped to {@code short} (DB2 SMALLINT).
     */
    private short hvCustomerCreditScore;

    /**
     * HV-CUSTOMER-CS-REVIEW-DATE PIC S9(9) COMP — 4 bytes.
     *
     * <p>Mapped to {@code int}; YYYYMMDD encoding (DB2 INTEGER column).
     */
    private int hvCustomerCsReviewDate;
}
