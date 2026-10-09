package com.ibm.cics.botz.crecust.model;

/**
 * Common interface for the {@code CASE-1-CONDITION-ID} / {@code CASE-2-CONDITION-ID}
 * REDEFINES group (Cat 4a, Pattern A — mutually exclusive).
 *
 * <p>Enables polymorphic reference within the condition ID pair.
 * Pattern A uses independent typed fields per concrete class with no shared byte array.
 * Note: {@code FcConditionToken} is intentionally omitted (Rule 13, G2 resolved)
 * because CEEDAYS/CEELOCT date-handling calls are replaced by {@code java.time}.
 *
 * @see "CRECUST.cbl WORKING-STORAGE — CASE-1-CONDITION-ID / CASE-2-CONDITION-ID (lines 351-359)"
 */
public interface ConditionId {
}
