package com.ibm.cics.botz.crecust.db;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DB2 host-variable row class for the {@code PROCTRAN} table ({@code HOST-PROCTRAN-ROW}).
 *
 * <p>9 fields. {@code hvProctranDate} is {@code String} (DD.MM.YYYY format from DATESEP '.').
 * {@code hvProctranAmount} is {@code BigDecimal} (from {@code S9(10)V99 COMP-3} packed decimal).
 *
 * @see "CRECUST.cbl WORKING-STORAGE — HOST-PROCTRAN-ROW (line 160)"
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class HostProctranRow {

    /** HV-PROCTRAN-EYECATCHER PIC X(4) — 4 bytes. Lit 'PRTR'. */
    private String hvProctranEyecatcher;

    /** HV-PROCTRAN-SORT-CODE PIC X(6) — 6 bytes. */
    private String hvProctranSortCode;

    /** HV-PROCTRAN-ACC-NUMBER PIC X(8) — 8 bytes. Set to ZEROS for customer-create. */
    private String hvProctranAccNumber;

    /**
     * HV-PROCTRAN-DATE PIC X(10) — 10 bytes.
     *
     * <p>DD.MM.YYYY format (EXEC CICS FORMATTIME with DATESEP '.').
     */
    private String hvProctranDate;

    /** HV-PROCTRAN-TIME PIC X(6) — 6 bytes. HHMMSS format. */
    private String hvProctranTime;

    /** HV-PROCTRAN-REF PIC X(12) — 12 bytes. EIBTASKN (task number). */
    private String hvProctranRef;

    /** HV-PROCTRAN-TYPE PIC X(3) — 3 bytes. 'OCC' for Branch Create Customer. */
    private String hvProctranType;

    /**
     * HV-PROCTRAN-DESC PIC X(40) — 40 bytes.
     *
     * <p>Packed: sortcode(6) + custno(10) + name(14) + dob(10).
     */
    private String hvProctranDesc;

    /**
     * HV-PROCTRAN-AMOUNT PIC S9(10)V99 COMP-3 — 7 bytes (packed decimal).
     *
     * <p>Mapped to {@code BigDecimal}. ZEROS for customer-create transactions.
     */
    private BigDecimal hvProctranAmount;
}
