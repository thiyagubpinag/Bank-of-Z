package com.ibm.cics.botz.crecust.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for {@code NCS-CUST-NO-STUFF} — CICS Named Counter Service fields (35 bytes).
 *
 * <p>Does NOT implement {@link com.ibm.cics.botz.common.ByteArraySerializable}.
 * Fields with COBOL VALUE clauses are initialised via {@code @Builder.Default}.
 *
 * @see "CRECUST.cbl WORKING-STORAGE — NCS-CUST-NO-STUFF (line 440)"
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class NcsCustNoStuff {

    /**
     * NCS-CUST-NO-ACT-NAME PIC X(9) — 9 bytes.
     *
     * <p>COBOL VALUE 'BANKZCUST'. The Named Counter action name that identifies the counter
     * resource to CICS NCS.
     */
    @Builder.Default
    private String ncsCustNoActName = "BANKZCUST";

    /**
     * NCS-CUST-NO-TEST-SORT PIC X(6) — 6 bytes.
     *
     * <p>Sort code moved here to form the full counter name (BANKZCUST + sortcode).
     */
    private String ncsCustNoTestSort;

    /**
     * NCS-CUST-NO-FILL PIC XX — 2 bytes.
     *
     * <p>COBOL VALUE '  ' (two spaces).
     */
    @Builder.Default
    private String ncsCustNoFill = "  ";

    /**
     * NCS-CUST-NO-INC PIC 9(16) COMP — 8 bytes.
     *
     * <p>Set to 1 before each NCS increment call; VALUE 0 initially.
     */
    @Builder.Default
    private long ncsCustNoInc = 0L;

    /**
     * NCS-CUST-NO-VALUE PIC 9(16) COMP — 8 bytes.
     *
     * <p>Holds the returned next sequential customer number value from NCS.
     * PIC 9(16) COMP exceeds int range — mapped to {@code long}.
     */
    private long ncsCustNoValue;

    /**
     * NCS-CUST-NO-RESP PIC XX — 2 bytes.
     *
     * <p>COBOL VALUE '00'. Named Counter response code.
     */
    @Builder.Default
    private String ncsCustNoResp = "00";
}
