package com.ibm.cics.botz.crecust.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Java model for the {@code PROC-TRAN-LOGICAL-DELETE-AREA} REDEFINES view.
 *
 * <p>Implements {@link ProcTranEyeCatcher} under Cat 4a Pattern A (mutually exclusive).
 * Holds its own independent typed fields ({@link #logicalDeleteFlag} and 3-byte {@link #filler})
 * and level-88 logic without any shared {@code byte[]} base class.
 *
 * @see "CRECUST.cbl WORKING-STORAGE — PROC-TRAN-LOGICAL-DELETE-AREA (lines 220-223)"
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ProcTranLogicalDeleteArea implements ProcTranEyeCatcher {

    /** Expected byte value for {@code PROC-TRAN-LOGICALLY-DELETED} (level-88). */
    public static final byte LOGICAL_DELETE_FLAG_VALUE = (byte) 0xFF;

    /** PROC-TRAN-LOGICAL-DELETE-FLAG PIC X — 1 byte. */
    private byte logicalDeleteFlag;

    /** FILLER PIC X(3) — 3 bytes of padding. */
    @Setter(AccessLevel.NONE)
    private final byte[] filler = new byte[3];

    /**
     * Evaluates level-88 condition {@code PROC-TRAN-LOGICALLY-DELETED VALUE X'FF'}.
     *
     * @return {@code true} if {@link #logicalDeleteFlag} equals {@value #LOGICAL_DELETE_FLAG_VALUE}, {@code false} otherwise
     */
    public boolean isLogicallyDeleted() {
        return logicalDeleteFlag == LOGICAL_DELETE_FLAG_VALUE;
    }
}
