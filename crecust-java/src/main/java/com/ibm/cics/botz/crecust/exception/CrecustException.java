package com.ibm.cics.botz.crecust.exception;

/**
 * Single exception class for all CRECUST error paths (ADR-10, Rule 16).
 *
 * <p>Extends {@link RuntimeException} directly. No {@code *ExceptionHandler} or
 * {@code *ErrorHandler} companion class is generated because CRECUST has no COBOL
 * {@code HANDLE CONDITION}, no shared error-response paragraph, and no cross-cutting catch
 * policy (ADR-10 / Rule 16, G1 resolved 2026-10-01). Every error path is handled inline in
 * its respective service class.
 *
 * <p>Each named static factory method corresponds to exactly one COBOL fail-code path (FR-11,
 * Rule 3, Rule 4). Factory methods <em>do not</em> mutate the commarea; the catch block that
 * catches a {@code CrecustException} is responsible for calling
 * {@code commArea.setCommFailCode(e.getFailCode())} and {@code commArea.setCommSuccess("N")}
 * (G3 resolved 2026-10-01).
 *
 * <p><b>Rule 16 COBOL construct grounding (ADR-10 — Story 9.2):</b>
 *
 * <p>The five COBOL error-handling constructs that ground this exception class are listed below.
 * Each entry shows the COBOL paragraph name, the type of inline COBOL error check, the Java
 * service class that contains the corresponding catch block, and the catch scope. No separate
 * {@code *ExceptionHandler*} or {@code *ErrorHandler*} class exists — this is the complete
 * exception boundary for the CRECUST modernisation.
 *
 * <ol>
 *   <li><b>{@code WRITE-PROCTRAN-DB2_WPD010}</b> (CRECUST.cbl lines 1321–1458) —
 *       Inline SQLCODE check; <em>notifying-abort path</em> (Rule 1). Maps to
 *       {@code ProctranDbService.insertProctran()} catch block. Catch scope: on
 *       {@link java.sql.SQLException}, calls (in COBOL source order):
 *       (1) populate {@code AbndInfoRec},
 *       (2) {@code abndprocDelegate.linkAbndproc(abndInfoRec)},
 *       (3) {@code customerNumberService.dequeue(commArea, nameResource)},
 *       (4) {@code Task.getTask().abend(}{@link #ABEND_CODE_HWPT}{@code )}.
 *       No fail-code is set — ABEND terminates the task (Rule 1).</li>
 *
 *   <li><b>{@code WRITE-CUSTOMER-DB2_WCD010}</b> (CRECUST.cbl lines 1139–1306) —
 *       Inline SQLCODE check; <em>silent-return path</em> (Rule 1). Maps to
 *       {@code CustomerDbService.insertCustomer()} catch block. Catch scope: on
 *       {@link java.sql.SQLException} or {@code NamingException}, calls
 *       {@code customerNumberService.dequeue(commArea, nameResource)}, sets
 *       {@code commSuccess='N'} and
 *       {@code commFailCode=}{@link #FAIL_CODE_INSERT_CUSTOMER}{@code  ('1')}, returns.
 *       No ABEND, no LINK on this path.</li>
 *
 *   <li><b>{@code GET-LAST-CUSTOMER-DB2_GLCD010}</b> (CRECUST.cbl lines ~1491–1540) —
 *       Inline SQLCODE check; <em>silent-return path</em> (Rule 1). Maps to
 *       {@code CustomerNumberService.getAndIncrementCustomerNumber()} catch block (via
 *       {@code failControlSql} helper). Catch scope: on {@link java.sql.SQLException} or
 *       {@code NamingException}, or SELECT not-found ({@code SQLCODE +100}), calls
 *       {@code dequeue(commArea, nameResource)}, sets {@code commSuccess='N'} and
 *       {@code commFailCode=}{@link #FAIL_CODE_CONTROL_SQL}{@code  ('4')}, returns.</li>
 *
 *   <li><b>{@code CREDIT-CHECK_CC010}</b> (CRECUST.cbl lines 605–1131) —
 *       Inline {@code EIBRESP}/{@code EIBRESP2} checks on CICS async/channel operations;
 *       <em>silent-return paths</em>. Maps to multiple inline catch blocks in
 *       {@code CreditCheckService}: PUT CONTAINER ({@code CicsConditionException} →
 *       fail-code {@link #FAIL_CODE_PUT_CONTAINER} 'A'), RUN TRANSID
 *       ({@code CicsConditionException} → fail-code {@link #FAIL_CODE_RUN_TRANSID} 'B'),
 *       FETCH ANY NOTFINISHED ({@link #FAIL_CODE_CC_NOTFINISHED} 'C'), FETCH ANY INVREQ
 *       ({@link #FAIL_CODE_CC_INVREQ} 'D'), GET CONTAINER ({@link #FAIL_CODE_GET_CONTAINER}
 *       'E'), child ABEND ({@link #FAIL_CODE_CC_ABEND} 'F'), child SECERROR
 *       ({@link #FAIL_CODE_CREDIT_ERROR} 'G'), child OTHER ({@link #FAIL_CODE_CC_OTHER}
 *       'H'). Every catch block sets the fail-code via a named factory method before
 *       returning.</li>
 *
 *   <li><b>{@code ENQ-NAMED-COUNTER_ENC010} / {@code DEQ-NAMED-COUNTER_DNC010}</b>
 *       (CRECUST.cbl lines 541–581) — Inline {@code EIBRESP} checks on CICS ENQ and DEQ;
 *       <em>silent-return paths</em>. Maps to inline catch blocks in
 *       {@code CustomerNumberService.enqueue()} (fail-code {@link #FAIL_CODE_ENQ} '3')
 *       and {@code CustomerNumberService.dequeue()} (fail-code {@link #FAIL_CODE_DEQ}
 *       '5'). Each catch block sets {@code commSuccess='N'} and the named fail-code constant
 *       before returning.</li>
 * </ol>
 *
 * <p><b>COBOL factory-method grounding (individual paths):</b>
 * <ul>
 *   <li>{@link #invalidTitle} — {@code PREMIERE_P010} line 416: {@code MOVE 'T' TO COMM-FAIL-CODE}</li>
 *   <li>{@link #creditError} — {@code PREMIERE_P010} line 818 and {@code CREDIT-CHECK_CC010}
 *       SECERROR branch: {@code MOVE 'G' TO COMM-FAIL-CODE}</li>
 *   <li>{@link #putContainerError} — {@code PUT-CONTAINER_PCT010}: {@code MOVE 'A' TO COMM-FAIL-CODE}</li>
 *   <li>{@link #runTransidError} — {@code RUN-TRANSID_RTC010} / {@code PREMIERE_P010} line 696:
 *       {@code MOVE 'B' TO COMM-FAIL-CODE}</li>
 *   <li>{@link #ccNotFinished} — {@code CREDIT-CHECK_CC010} lines 785–786:
 *       {@code MOVE 'C' TO COMM-FAIL-CODE} when NOTFINISHED + zero retrieved</li>
 *   <li>{@link #ccInvreq} — {@code CREDIT-CHECK_CC010} lines 872–873:
 *       {@code MOVE 'D' TO COMM-FAIL-CODE} on INVREQ</li>
 *   <li>{@link #getContainerError} — {@code CREDIT-CHECK_CC010} line 1028:
 *       {@code MOVE 'E' TO COMM-FAIL-CODE}</li>
 *   <li>{@link #ccAbend} — {@code CREDIT-CHECK_CC010} line 1069:
 *       {@code MOVE 'F' TO COMM-FAIL-CODE} when child completion = ABEND</li>
 *   <li>{@link #ccOther} — {@code CREDIT-CHECK_CC010} line 1115:
 *       {@code MOVE 'H' TO COMM-FAIL-CODE} when child completion = OTHER</li>
 *   <li>{@link #insertCustomerFailed} — {@code WRITE-CUSTOMER-DB2_WCD010} line 1266:
 *       {@code MOVE '1' TO COMM-FAIL-CODE}</li>
 *   <li>{@link #enqFailed} — {@code ENQ-NAMED-COUNTER_ENC010} line 554:
 *       {@code MOVE '3' TO COMM-FAIL-CODE}</li>
 *   <li>{@link #controlSqlFailed} — {@code GET-LAST-CUSTOMER-DB2_GLCD010}:
 *       {@code MOVE '4' TO COMM-FAIL-CODE}</li>
 *   <li>{@link #deqFailed} — {@code DEQ-NAMED-COUNTER_DNC010}:
 *       {@code MOVE '5' TO COMM-FAIL-CODE}</li>
 *   <li>{@link #dobRange} — {@code DATE-OF-BIRTH-CHECK_DOBC010}:
 *       {@code MOVE 'O' TO COMM-FAIL-CODE} when year &lt; 1601 or age &gt; 150</li>
 *   <li>{@link #dobFuture} — {@code DATE-OF-BIRTH-CHECK_DOBC010}:
 *       {@code MOVE 'Y' TO COMM-FAIL-CODE} when DOB is in the future</li>
 *   <li>{@link #ceeDaysFailed} — {@code DATE-OF-BIRTH-CHECK_DOBC010}:
 *       {@code MOVE 'Z' TO COMM-FAIL-CODE} when CEE date construction fails</li>
 *   <li>{@code WRITE-PROCTRAN-DB2_WPD010} notifying-abort path →
 *       {@code Task.getTask().abend(}{@link #ABEND_CODE_HWPT}{@code )} (Rule 1: linkAbndproc first)</li>
 * </ul>
 *
 * @author CRECUST modernisation team
 * @since 1.0.0
 */
public class CrecustException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    // -------------------------------------------------------------------------
    // Fail-code constants (FR-11, Rule 3, Rule 4)
    // -------------------------------------------------------------------------

    /** Fail-code 'T' — title not in accepted list (PREMIERE_P010 line 416). */
    public static final String FAIL_CODE_INVALID_TITLE   = "T";

    /** Fail-code 'G' — credit error or SECERROR (PREMIERE_P010 line 818 and CREDIT-CHECK_CC010). */
    public static final String FAIL_CODE_CREDIT_ERROR    = "G";

    /** Fail-code 'A' — PUT CONTAINER failed (PUT-CONTAINER_PCT010). */
    public static final String FAIL_CODE_PUT_CONTAINER   = "A";

    /** Fail-code 'B' — RUN TRANSID failed (PREMIERE_P010 line 696). */
    public static final String FAIL_CODE_RUN_TRANSID     = "B";

    /** Fail-code 'C' — FETCH ANY NOTFINISHED + zero retrieved (CREDIT-CHECK_CC010 lines 785–786). */
    public static final String FAIL_CODE_CC_NOTFINISHED  = "C";

    /** Fail-code 'D' — FETCH ANY INVREQ / no children (CREDIT-CHECK_CC010 lines 872–873). */
    public static final String FAIL_CODE_CC_INVREQ       = "D";

    /** Fail-code 'E' — GET CONTAINER failed (CREDIT-CHECK_CC010 line 1028). */
    public static final String FAIL_CODE_GET_CONTAINER   = "E";

    /** Fail-code 'F' — FETCH ANY completion = ABEND (CREDIT-CHECK_CC010 line 1069). */
    public static final String FAIL_CODE_CC_ABEND        = "F";

    /** Fail-code 'H' — FETCH ANY completion = OTHER (CREDIT-CHECK_CC010 line 1115). */
    public static final String FAIL_CODE_CC_OTHER        = "H";

    /** Fail-code '1' — INSERT CUSTOMER failed (WRITE-CUSTOMER-DB2_WCD010 line 1266). */
    public static final String FAIL_CODE_INSERT_CUSTOMER = "1";

    /** Fail-code '3' — ENQ named-counter failed (ENQ-NAMED-COUNTER_ENC010 line 554). */
    public static final String FAIL_CODE_ENQ             = "3";

    /** Fail-code '4' — CONTROL table SELECT/UPDATE failed (GET-LAST-CUSTOMER-DB2_GLCD010). */
    public static final String FAIL_CODE_CONTROL_SQL     = "4";

    /** Fail-code '5' — DEQ named-counter failed (DEQ-NAMED-COUNTER_DNC010). */
    public static final String FAIL_CODE_DEQ             = "5";

    /** Fail-code 'O' — DOB year &lt; 1601 or age &gt; 150 (DATE-OF-BIRTH-CHECK_DOBC010). */
    public static final String FAIL_CODE_DOB_RANGE       = "O";

    /** Fail-code 'Y' — DOB is in the future (DATE-OF-BIRTH-CHECK_DOBC010). */
    public static final String FAIL_CODE_DOB_FUTURE      = "Y";

    /** Fail-code 'Z' — CEE date construction failed (DATE-OF-BIRTH-CHECK_DOBC010). */
    public static final String FAIL_CODE_CEEDAYS_FAIL    = "Z";

    /** Fail-code ' ' — success sentinel (COMM-FAIL-CODE value on happy path). */
    public static final String FAIL_CODE_SUCCESS         = " ";

    /**
     * CICS ABEND code used when PROCTRAN write fails.
     *
     * <p>Corresponds to {@code EXEC CICS ABEND ABCODE('HWPT')} in CRECUST.cbl (line 1455).
     * The notifying-abort path calls {@code AbndprocDelegate.linkAbndproc()} <em>before</em>
     * issuing the ABEND (Rule 1 ordering). No raw {@code "HWPT"} literal may appear outside
     * this declaration (AC-4.2).
     */
    public static final String ABEND_CODE_HWPT           = "HWPT";

    // -------------------------------------------------------------------------
    // Legacy constant aliases — preserved for backward compatibility with
    // Epics 3–8 call sites that reference the old names.
    // -------------------------------------------------------------------------

    /** @deprecated Use {@link #FAIL_CODE_CC_NOTFINISHED}. Kept for Epics 3–8 call-site compat. */
    @Deprecated
    public static final String CC_NOTFINISHED    = FAIL_CODE_CC_NOTFINISHED;

    /** @deprecated Use {@link #FAIL_CODE_CC_INVREQ}. Kept for Epics 3–8 call-site compat. */
    @Deprecated
    public static final String CC_INVREQ         = FAIL_CODE_CC_INVREQ;

    /** @deprecated Use {@link #FAIL_CODE_GET_CONTAINER}. Kept for Epics 3–8 call-site compat. */
    @Deprecated
    public static final String GET_CONTAINER_ERROR = FAIL_CODE_GET_CONTAINER;

    /** @deprecated Use {@link #FAIL_CODE_CC_ABEND}. Kept for Epics 3–8 call-site compat. */
    @Deprecated
    public static final String CC_ABEND          = FAIL_CODE_CC_ABEND;

    /** @deprecated Use {@link #FAIL_CODE_CREDIT_ERROR}. Kept for Epics 3–8 call-site compat. */
    @Deprecated
    public static final String CC_SECERROR       = FAIL_CODE_CREDIT_ERROR;

    /** @deprecated Use {@link #FAIL_CODE_CC_OTHER}. Kept for Epics 3–8 call-site compat. */
    @Deprecated
    public static final String CC_OTHER          = FAIL_CODE_CC_OTHER;

    // -------------------------------------------------------------------------
    // Instance fields
    // -------------------------------------------------------------------------

    /**
     * The fail-code carried by this exception (FR-11).
     * Catch blocks read this via {@link #getFailCode()} and write it to the commarea.
     */
    private final String failCode;

    /**
     * COMM-SUCCESS value — always {@code "N"} for exception instances.
     * Retained for backward compatibility with Epics 3–8 call sites.
     */
    private final String commSuccess;

    /**
     * COMM-FAIL-CODE value set on this exception instance.
     * Retained for backward compatibility with Epics 3–8 call sites.
     */
    private final String commFailCode;

    // -------------------------------------------------------------------------
    // Private constructors
    // -------------------------------------------------------------------------

    private CrecustException(String message, String failCode) {
        super(message);
        this.failCode    = failCode;
        this.commSuccess = "N";
        this.commFailCode = failCode;
    }

    private CrecustException(String message, String failCode, Throwable cause) {
        super(message, cause);
        this.failCode    = failCode;
        this.commSuccess = "N";
        this.commFailCode = failCode;
    }

    // -------------------------------------------------------------------------
    // Public constructors — retained for backward compatibility
    // -------------------------------------------------------------------------

    /**
     * General-purpose constructor (backward compat, Epics 3–8).
     * Prefer named factory methods (Rule 3).
     */
    public CrecustException(String message) {
        super(message);
        this.failCode     = null;
        this.commSuccess  = null;
        this.commFailCode = null;
    }

    /**
     * General-purpose constructor with cause (backward compat, Epics 3–8).
     * Prefer named factory methods (Rule 3).
     */
    public CrecustException(String message, Throwable cause) {
        super(message, cause);
        this.failCode     = null;
        this.commSuccess  = null;
        this.commFailCode = null;
    }

    // -------------------------------------------------------------------------
    // Public accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the fail-code carried by this exception (FR-11, Rule 3).
     *
     * <p>Catch blocks call {@code commArea.setCommFailCode(e.getFailCode())} to propagate
     * the fail-code to the commarea (G3 resolved 2026-10-01).
     *
     * @return the single-character fail-code string, or {@code null} for generic instances
     */
    public String getFailCode() {
        return failCode;
    }

    /**
     * Returns the COMM-SUCCESS value ({@code "N"} for all factory-created instances).
     *
     * <p>Retained for backward compatibility with Epics 3–8 call sites that call
     * {@code commArea.setCommSuccess(e.getCommSuccess())}.
     *
     * @return {@code "N"}, or {@code null} for generic instances
     */
    public String getCommSuccess() {
        return commSuccess;
    }

    /**
     * Returns the COMM-FAIL-CODE value carried by this exception.
     *
     * <p>Retained for backward compatibility with Epics 3–8 call sites that call
     * {@code commArea.setCommFailCode(e.getCommFailCode())}.
     *
     * @return the fail-code string, or {@code null} for generic instances
     */
    public String getCommFailCode() {
        return commFailCode;
    }

    // -------------------------------------------------------------------------
    // Named static factory methods (16 total — FR-11, Rule 3, AC-16.1)
    // -------------------------------------------------------------------------

    /**
     * Factory: title not in accepted list (fail-code 'T').
     * <p>COBOL: {@code PREMIERE_P010} — {@code MOVE 'T' TO COMM-FAIL-CODE} (line 416).
     */
    public static CrecustException invalidTitle() {
        return new CrecustException("Title not in accepted list", FAIL_CODE_INVALID_TITLE);
    }

    /**
     * Factory: credit error or SECERROR (fail-code 'G').
     * <p>COBOL: {@code PREMIERE_P010} line 818 and {@code CREDIT-CHECK_CC010} SECERROR branch —
     * {@code MOVE 'G' TO COMM-FAIL-CODE}.
     */
    public static CrecustException creditError() {
        return new CrecustException("Credit error or SECERROR", FAIL_CODE_CREDIT_ERROR);
    }

    /**
     * Factory: PUT CONTAINER failed (fail-code 'A').
     * <p>COBOL: {@code PUT-CONTAINER_PCT010} — {@code MOVE 'A' TO COMM-FAIL-CODE}.
     *
     * @param cause the underlying {@link com.ibm.cics.server.CicsConditionException}
     */
    public static CrecustException putContainerError(Throwable cause) {
        return new CrecustException("PUT CONTAINER failed", FAIL_CODE_PUT_CONTAINER, cause);
    }

    /**
     * Factory: RUN TRANSID failed (fail-code 'B').
     * <p>COBOL: {@code PREMIERE_P010} line 696 — {@code MOVE 'B' TO COMM-FAIL-CODE}.
     *
     * @param cause the underlying {@link com.ibm.cics.server.CicsConditionException}
     */
    public static CrecustException runTransidError(Throwable cause) {
        return new CrecustException("RUN TRANSID failed", FAIL_CODE_RUN_TRANSID, cause);
    }

    /**
     * Factory: FETCH ANY NOTFINISHED with zero retrieved (fail-code 'C').
     * <p>COBOL: {@code CREDIT-CHECK_CC010} lines 785–786 — {@code MOVE 'C' TO COMM-FAIL-CODE}.
     */
    public static CrecustException ccNotFinished() {
        return new CrecustException("FETCH ANY NOTFINISHED, no data", FAIL_CODE_CC_NOTFINISHED);
    }

    /**
     * Factory: FETCH ANY INVREQ / no children (fail-code 'D').
     * <p>COBOL: {@code CREDIT-CHECK_CC010} lines 872–873 — {@code MOVE 'D' TO COMM-FAIL-CODE}.
     *
     * @param cause the underlying {@link com.ibm.cics.server.InvalidRequestException}
     */
    public static CrecustException ccInvreq(Throwable cause) {
        return new CrecustException("FETCH ANY INVREQ", FAIL_CODE_CC_INVREQ, cause);
    }

    /**
     * Factory: GET CONTAINER failed (fail-code 'E').
     * <p>COBOL: {@code CREDIT-CHECK_CC010} line 1028 — {@code MOVE 'E' TO COMM-FAIL-CODE}.
     *
     * @param cause the underlying {@link com.ibm.cics.server.CicsConditionException}
     */
    public static CrecustException getContainerError(Throwable cause) {
        return new CrecustException("GET CONTAINER failed", FAIL_CODE_GET_CONTAINER, cause);
    }

    /**
     * Factory: FETCH ANY child completion = ABEND (fail-code 'F').
     * <p>COBOL: {@code CREDIT-CHECK_CC010} line 1069 — {@code MOVE 'F' TO COMM-FAIL-CODE}.
     */
    public static CrecustException ccAbend() {
        return new CrecustException("FETCH ANY completion = ABEND", FAIL_CODE_CC_ABEND);
    }

    /**
     * Factory: FETCH ANY child completion = OTHER (fail-code 'H').
     * <p>COBOL: {@code CREDIT-CHECK_CC010} line 1115 — {@code MOVE 'H' TO COMM-FAIL-CODE}.
     */
    public static CrecustException ccOther() {
        return new CrecustException("FETCH ANY completion = OTHER", FAIL_CODE_CC_OTHER);
    }

    /**
     * Factory: INSERT CUSTOMER failed (fail-code '1').
     * <p>COBOL: {@code WRITE-CUSTOMER-DB2_WCD010} line 1266 — {@code MOVE '1' TO COMM-FAIL-CODE}.
     *
     * @param cause the underlying {@link java.sql.SQLException} or {@link javax.naming.NamingException}
     */
    public static CrecustException insertCustomerFailed(Throwable cause) {
        return new CrecustException("INSERT CUSTOMER failed", FAIL_CODE_INSERT_CUSTOMER, cause);
    }

    /**
     * Factory: ENQ named-counter failed (fail-code '3').
     * <p>COBOL: {@code ENQ-NAMED-COUNTER_ENC010} line 554 — {@code MOVE '3' TO COMM-FAIL-CODE}.
     *
     * @param cause the underlying {@link com.ibm.cics.server.CicsConditionException}
     */
    public static CrecustException enqFailed(Throwable cause) {
        return new CrecustException("ENQ failed", FAIL_CODE_ENQ, cause);
    }

    /**
     * Factory: CONTROL table SELECT/UPDATE failed (fail-code '4').
     * <p>COBOL: {@code GET-LAST-CUSTOMER-DB2_GLCD010} — {@code MOVE '4' TO COMM-FAIL-CODE}.
     *
     * @param cause the underlying {@link java.sql.SQLException}
     */
    public static CrecustException controlSqlFailed(Throwable cause) {
        return new CrecustException("CONTROL SQL failed", FAIL_CODE_CONTROL_SQL, cause);
    }

    /**
     * Factory: DEQ named-counter failed (fail-code '5').
     * <p>COBOL: {@code DEQ-NAMED-COUNTER_DNC010} — {@code MOVE '5' TO COMM-FAIL-CODE}.
     *
     * @param cause the underlying {@link com.ibm.cics.server.CicsConditionException}
     */
    public static CrecustException deqFailed(Throwable cause) {
        return new CrecustException("DEQ failed", FAIL_CODE_DEQ, cause);
    }

    /**
     * Factory: DOB year &lt; 1601 or age &gt; 150 (fail-code 'O').
     * <p>COBOL: {@code DATE-OF-BIRTH-CHECK_DOBC010} — {@code MOVE 'O' TO COMM-FAIL-CODE}.
     */
    public static CrecustException dobRange() {
        return new CrecustException("DOB year < 1601 or age > 150", FAIL_CODE_DOB_RANGE);
    }

    /**
     * Factory: DOB is in the future (fail-code 'Y').
     * <p>COBOL: {@code DATE-OF-BIRTH-CHECK_DOBC010} — {@code MOVE 'Y' TO COMM-FAIL-CODE}.
     */
    public static CrecustException dobFuture() {
        return new CrecustException("DOB in the future", FAIL_CODE_DOB_FUTURE);
    }

    /**
     * Factory: CEE date construction failed / LocalDate construction failed (fail-code 'Z').
     * <p>COBOL: {@code DATE-OF-BIRTH-CHECK_DOBC010} — {@code MOVE 'Z' TO COMM-FAIL-CODE}.
     */
    public static CrecustException ceeDaysFailed() {
        return new CrecustException("LocalDate construction failed", FAIL_CODE_CEEDAYS_FAIL);
    }

    // -------------------------------------------------------------------------
    // Legacy factory alias — backward compatibility for Epics 3–8 call sites
    // -------------------------------------------------------------------------

    /**
     * @deprecated Use {@link #creditError()}. This alias is kept for Epics 3–8 call-site
     *             backward compatibility ({@code ccSecError()} was the prior name for the
     *             SECERROR branch which maps to {@link #FAIL_CODE_CREDIT_ERROR}).
     */
    @Deprecated
    public static CrecustException ccSecError() {
        return creditError();
    }
}
