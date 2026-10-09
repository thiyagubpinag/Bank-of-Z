package com.ibm.cics.botz.crecust.model;

/**
 * Common interface for the {@code PROC-TRAN-EYE-CATCHER} / {@code PROC-TRAN-LOGICAL-DELETE-AREA}
 * REDEFINES group (Cat 4a, Pattern A — mutually exclusive).
 *
 * <p>Enables polymorphic reference to either the valid eye-catcher view
 * ({@link ProcTranValid}) or the logically deleted view ({@link ProcTranLogicalDeleteArea}).
 * Pattern A uses independent typed fields per concrete class with no shared byte array.
 *
 * @see "CRECUST.cbl WORKING-STORAGE — PROC-TRAN-EYE-CATCHER / PROC-TRAN-LOGICAL-DELETE-AREA (lines 217-223)"
 */
public interface ProcTranEyeCatcher {
}
