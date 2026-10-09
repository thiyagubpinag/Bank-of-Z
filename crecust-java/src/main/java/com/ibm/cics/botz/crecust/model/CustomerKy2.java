package com.ibm.cics.botz.crecust.model;

import com.ibm.jzos.fields.CobolDatatypeFactory;
import com.ibm.jzos.fields.ExternalDecimalAsIntField;
import com.ibm.jzos.fields.ExternalDecimalAsLongField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cat 3a REDEFINES view of {@code CUSTOMER-KY2-BYTES PIC X(16)}.
 *
 * <p>COBOL source (CRECUST.cbl lines 223–228):
 * <pre>
 *    01 CUSTOMER-KY2.
 *       03 REQUIRED-SORT-CODE2        PIC 9(6)      VALUE 0.
 *       03 REQUIRED-CUST-NUMBER2      PIC 9(10)     VALUE 0.
 *    01 CUSTOMER-KY2-BYTES REDEFINES CUSTOMER-KY2
 *                                     PIC X(16).
 * </pre>
 *
 * <p>The record sibling {@code CUSTOMER-KY2} (anchor) holds two consecutive EBCDIC external-decimal
 * fields: a 6-digit sort code at offset 0 and a 10-digit customer number at offset 6.
 * The getter decodes the 16-byte array using JZOS {@link ExternalDecimalAsIntField} and
 * {@link ExternalDecimalAsLongField}; the setter encodes back.
 *
 * <p>The paired getter/setter ({@code getCustomerKy2()} / {@code setCustomerKy2(CustomerKy2)})
 * belong on {@code CustomerNumberService}, which holds the {@code customerKy2Bytes byte[]} field.
 * <p><b>TODO:</b> {@code CustomerNumberService} is not yet generated (Story 3+). Add
 * {@code getCustomerKy2()} and {@code setCustomerKy2(CustomerKy2)} to that class when it is
 * created, using the DECODE/ENCODE helpers below.
 *
 * <h3>Decoding a {@code byte[]} into {@code CustomerKy2}</h3>
 * <pre>{@code
 * public CustomerKy2 getCustomerKy2() {
 *     int sortCode  = CustomerKy2.SORT_CODE_FIELD.getInt(customerKy2Bytes);
 *     long custNum  = CustomerKy2.CUST_NUMBER_FIELD.getLong(customerKy2Bytes);
 *     return new CustomerKy2(sortCode, (int) custNum);
 * }
 * }</pre>
 *
 * <h3>Encoding a {@code CustomerKy2} back into {@code byte[]}</h3>
 * <pre>{@code
 * public void setCustomerKy2(CustomerKy2 grp) {
 *     byte[] buf = new byte[CustomerKy2.TOTAL_LENGTH];
 *     CustomerKy2.SORT_CODE_FIELD.putInt(grp.getRequiredSortCode2(), buf);
 *     CustomerKy2.CUST_NUMBER_FIELD.putLong(grp.getRequiredCustNumber2(), buf);
 *     this.customerKy2Bytes = buf;
 * }
 * }</pre>
 *
 * @see "CRECUST.cbl lines 223–228"
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class CustomerKy2 {

    // ── Layout constants ─────────────────────────────────────────────────────

    /** Byte length of {@code REQUIRED-SORT-CODE2 PIC 9(6)}. */
    public static final int SORT_CODE_LENGTH = 6;

    /** Byte length of {@code REQUIRED-CUST-NUMBER2 PIC 9(10)}. */
    public static final int CUST_NUMBER_LENGTH = 10;

    /** Total byte length of the {@code CUSTOMER-KY2} / {@code CUSTOMER-KY2-BYTES} layout (16 bytes). */
    public static final int TOTAL_LENGTH = SORT_CODE_LENGTH + CUST_NUMBER_LENGTH;

    // ── JZOS field descriptors ────────────────────────────────────────────────
    // Factory is instantiated once; offset advances automatically through incrementOffset.

    /** JZOS field descriptor for {@code REQUIRED-SORT-CODE2 PIC 9(6)} at offset 0. */
    public static final ExternalDecimalAsIntField SORT_CODE_FIELD;

    /** JZOS field descriptor for {@code REQUIRED-CUST-NUMBER2 PIC 9(10)} at offset 6. */
    public static final ExternalDecimalAsLongField CUST_NUMBER_FIELD;

    static {
        CobolDatatypeFactory factory = new CobolDatatypeFactory();
        factory.setOffset(0);
        SORT_CODE_FIELD   = factory.getExternalDecimalAsIntField(SORT_CODE_LENGTH, false);
        factory.incrementOffset(SORT_CODE_LENGTH);
        CUST_NUMBER_FIELD = factory.getExternalDecimalAsLongField(CUST_NUMBER_LENGTH, false);
    }

    // ── Instance fields ───────────────────────────────────────────────────────

    /** REQUIRED-SORT-CODE2 PIC 9(6) — 6-digit bank sort code. */
    private int requiredSortCode2;

    /** REQUIRED-CUST-NUMBER2 PIC 9(10) — 10-digit customer number. */
    private int requiredCustNumber2;
}
