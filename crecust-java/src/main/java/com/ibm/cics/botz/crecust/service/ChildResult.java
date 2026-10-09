package com.ibm.cics.botz.crecust.service;

/**
 * Plain-Java carrier for the result of a {@code FETCH ANY NOSUSPEND} call.
 *
 * <p>Replaces the CICS middleware type {@code ChildResponse} in the signature of
 * {@link AbndprocDelegate#fetchAnyNosuspend()} so that {@code CreditCheckService}
 * compiles without any CICS imports.
 *
 * <p>The completion-status values mirror {@code ChildResponse.CompletionStatus}:
 * <ul>
 *   <li>{@link #STATUS_NORMAL}   — {@code NORMAL}</li>
 *   <li>{@link #STATUS_ABEND}    — {@code ABEND}</li>
 *   <li>{@link #STATUS_SECERROR} — {@code SECERROR}</li>
 *   <li>{@link #STATUS_OTHER}    — any other value</li>
 * </ul>
 */
public final class ChildResult {

    /** Completion status constant — child finished normally. */
    public static final String STATUS_NORMAL   = "NORMAL";

    /** Completion status constant — child abended. */
    public static final String STATUS_ABEND    = "ABEND";

    /** Completion status constant — security error in child. */
    public static final String STATUS_SECERROR = "SECERROR";

    /** Completion status constant — any other completion. */
    public static final String STATUS_OTHER    = "OTHER";

    /** Plain-Java completion status string (one of the STATUS_* constants). */
    private final String completionStatus;

    /**
     * Index into the child-token list matching this response (used to resolve container name).
     * {@code -1} if no match was found.
     */
    private final int tokenIndex;

    /**
     * Constructs a {@code ChildResult}.
     *
     * @param completionStatus one of the {@code STATUS_*} constants
     * @param tokenIndex       index of the matching token in the issued list, or {@code -1}
     */
    public ChildResult(String completionStatus, int tokenIndex) {
        this.completionStatus = completionStatus;
        this.tokenIndex       = tokenIndex;
    }

    /**
     * Returns the completion status string.
     *
     * @return one of {@link #STATUS_NORMAL}, {@link #STATUS_ABEND},
     *         {@link #STATUS_SECERROR}, or {@link #STATUS_OTHER}
     */
    public String getCompletionStatus() {
        return completionStatus;
    }

    /**
     * Returns the index of the matching token in the issued-token list.
     *
     * @return the token index, or {@code -1} if no match was found
     */
    public int getTokenIndex() {
        return tokenIndex;
    }
}
