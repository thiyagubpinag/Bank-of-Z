package com.ibm.cics.botz.crecust.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for the {@code PROC-TRAN-EYE-CATCHER} anchor view.
 *
 * <p>Implements {@link ProcTranEyeCatcher} under Cat 4a Pattern A (mutually exclusive).
 * Holds its own independent typed field {@link #eyecatcher} (4 bytes) and level-88 logic
 * without any shared {@code byte[]} base class.
 *
 * @see "CRECUST.cbl WORKING-STORAGE — PROC-TRAN-EYE-CATCHER (line 217)"
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ProcTranValid implements ProcTranEyeCatcher {

    /** Expected literal value for {@code PROC-TRAN-VALID} (level-88). */
    public static final String EYECATCHER_VALUE = "PRTR";

    /** PROC-TRAN-EYE-CATCHER PIC X(4) — 4 bytes. */
    private String eyecatcher;

    /**
     * Evaluates level-88 condition {@code PROC-TRAN-VALID VALUE 'PRTR'}.
     *
     * @return {@code true} if {@link #eyecatcher} equals {@value #EYECATCHER_VALUE}, {@code false} otherwise
     */
    public boolean isValid() {
        return EYECATCHER_VALUE.equals(eyecatcher);
    }
}
