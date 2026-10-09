package com.ibm.cics.botz.crecust.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for the {@code CASE-2-CONDITION-ID} REDEFINES view.
 *
 * <p>Implements {@link ConditionId} under Cat 4a Pattern A (mutually exclusive).
 * Holds its own independent typed fields ({@link #classCode} and {@link #causeCode})
 * representing {@code PIC S9(4) BINARY} fields, without any shared {@code byte[]} base class.
 *
 * @see "CRECUST.cbl WORKING-STORAGE — CASE-2-CONDITION-ID (lines 355-359)"
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class Case2ConditionId implements ConditionId {

    /** CLASS-CODE PIC S9(4) BINARY — 2 bytes. */
    private short classCode;

    /** CAUSE-CODE PIC S9(4) BINARY — 2 bytes. */
    private short causeCode;
}
