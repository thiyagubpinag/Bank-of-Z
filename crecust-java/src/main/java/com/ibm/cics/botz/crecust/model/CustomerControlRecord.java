package com.ibm.cics.botz.crecust.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Java model for the CUSTCTRL.cpy record ({@code CUSTOMER-CONTROL-RECORD}).
 *
 * <p>Consumed by other programs in the Bank-of-Z suite (Rule 14 — cross-program contract);
 * CRECUST's own PROCEDURE DIVISION does not call it directly.
 * FILLER fields use {@code @Setter(AccessLevel.NONE)} and {@code final} to prevent setter
 * generation per ADR-12 Lombok FILLER rules.
 * Does NOT implement {@link com.ibm.cics.botz.common.ByteArraySerializable}.
 *
 * @see "CRECUST.cbl WORKING-STORAGE — CUSTOMER-CONTROL (line 656)"
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class CustomerControlRecord {

    /** CUSTOMER-CONTROL-EYECATCHER PIC X(4) — 4 bytes. 88: 'CTRL'. */
    private String customerControlEyecatcher;

    /** CUSTOMER-CONTROL-SORTCODE PIC 9(6) DISPLAY — 6 bytes. */
    private String customerControlSortcode;

    /** CUSTOMER-CONTROL-NUMBER PIC 9(10) DISPLAY — 10 bytes. */
    private String customerControlNumber;

    /** NUMBER-OF-CUSTOMERS PIC 9(10) DISPLAY — 10 bytes. */
    private String numberOfCustomers;

    /** LAST-CUSTOMER-NUMBER PIC 9(10) DISPLAY — 10 bytes. */
    private String lastCustomerNumber;

    /** CUSTOMER-CONTROL-SUCCESS-FLAG PIC X — 1 byte. */
    private String customerControlSuccessFlag;

    /** CUSTOMER-CONTROL-FAIL-CODE PIC X — 1 byte. */
    private String customerControlFailCode;

    /** FILLER PIC X(38) — 38 bytes of padding. */
    @Setter(AccessLevel.NONE)
    private final byte[] filler1 = new byte[38];

    /** FILLER PIC X(160) — 160 bytes of padding. */
    @Setter(AccessLevel.NONE)
    private final byte[] filler2 = new byte[160];

    /** FILLER PIC 9(8) COMP — 4 bytes of padding. */
    @Setter(AccessLevel.NONE)
    private final byte[] filler3 = new byte[4];

    /** FILLER PIC 999 — 3 bytes of padding. */
    @Setter(AccessLevel.NONE)
    private final byte[] filler4 = new byte[3];

    /** FILLER PIC 9(8) COMP — 4 bytes of padding. */
    @Setter(AccessLevel.NONE)
    private final byte[] filler5 = new byte[4];
}
