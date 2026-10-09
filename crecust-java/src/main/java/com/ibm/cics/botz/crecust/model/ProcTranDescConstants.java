package com.ibm.cics.botz.crecust.model;

/**
 * Constants for the {@code PROC-TRAN-DESC} 40-byte REDEFINES group.
 *
 * <p>Single source of truth for all byte offsets and field lengths across the
 * {@link ProcTranDescBase} hierarchy (Rule 15 / NFR-6).
 */
public final class ProcTranDescConstants {

    /** Total length of the PROC-TRAN-DESC shared byte array in bytes. */
    public static final int DATA_SIZE = 40;

    // ── CRECUS View Offsets and Lengths (TRA-7) ────────────────────────────

    /** Offset for sortCode (STORED-SORTCODE) — 6 bytes. */
    public static final int CRECUS_SORT_CODE_OFFSET = 0;
    /** Length for sortCode — 6 bytes. */
    public static final int CRECUS_SORT_CODE_LEN = 6;

    /** Offset for custNo (STORED-CUSTNO) — 10 bytes. */
    public static final int CRECUS_CUST_NO_OFFSET = 6;
    /** Length for custNo — 10 bytes. */
    public static final int CRECUS_CUST_NO_LEN = 10;

    /** Offset for name (STORED-NAME) — 14 bytes (at offset 16). */
    public static final int CRECUS_NAME_OFFSET = 16;
    /** Length for name — 14 bytes. */
    public static final int CRECUS_NAME_LEN = 14;

    /** Offset for dob (STORED-DOB, DD/MM/YYYY) — 10 bytes (at offset 30). */
    public static final int CRECUS_DOB_OFFSET = 30;
    /** Length for dob — 10 bytes. */
    public static final int CRECUS_DOB_LEN = 10;

    private ProcTranDescConstants() {
        // Prevent instantiation
    }
}
