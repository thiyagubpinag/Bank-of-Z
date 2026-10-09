package com.ibm.cics.botz.crecust.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for the {@code CASE-1-CONDITION-ID} anchor view.
 *
 * <p>Implements {@link ConditionId} under Cat 4a Pattern A (mutually exclusive).
 * Holds its own independent typed fields ({@link #severity} and {@link #msgNo})
 * representing {@code PIC S9(4) BINARY} fields, without any shared {@code byte[]} base class.
 *
 * @see "CRECUST.cbl WORKING-STORAGE — CASE-1-CONDITION-ID (lines 351-353)"
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class Case1ConditionId implements ConditionId {

    /** Message number constant for {@code CEE000} (success). */
    public static final short CEE_000_MSG_NO = (short) 0;

    /** SEVERITY PIC S9(4) BINARY — 2 bytes. */
    private short severity;

    /** MSG-NO PIC S9(4) BINARY — 2 bytes. */
    private short msgNo;

    /**
     * Checks if the condition represents {@code CEE000} (success status).
     *
     * @return {@code true} if {@link #msgNo} equals {@value #CEE_000_MSG_NO}, {@code false} otherwise
     */
    public boolean isCee000() {
        return msgNo == CEE_000_MSG_NO;
    }
}
