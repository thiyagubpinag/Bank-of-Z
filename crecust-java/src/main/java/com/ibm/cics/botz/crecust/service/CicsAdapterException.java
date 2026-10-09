package com.ibm.cics.botz.crecust.service;

/**
 * Wraps CICS vendor middleware exceptions thrown by {@link AbndprocDelegate}.
 *
 * <p>Vendor CICS exceptions ({@code CicsConditionException}, {@code NotFinishedException},
 * {@code InvalidRequestException}, {@code NotFoundException}) are caught inside the adapter
 * and rethrown as one of the named subclasses of this class, so that the calling service
 * classes ({@code CreditCheckService}) never import middleware types.
 *
 * <p>Subclasses mirror the CICS RESP/RESP2 conditions that CRECUST.cbl handles inline:
 * <ul>
 *   <li>{@link NotFinished}   — {@code DFHRESP(NOTFINISHED)} from FETCH ANY</li>
 *   <li>{@link InvReq}        — {@code DFHRESP(INVREQ)} from FETCH ANY</li>
 *   <li>{@link NotFound}      — {@code DFHRESP(NOTFND)} from FETCH ANY</li>
 *   <li>{@link CicsCondition} — any other {@code CicsConditionException}</li>
 * </ul>
 */
public class CicsAdapterException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a {@code CicsAdapterException} with the given message and cause.
     *
     * @param message detail message
     * @param cause   the underlying middleware exception
     */
    public CicsAdapterException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@code CicsAdapterException} with the given message and no cause.
     *
     * @param message detail message
     */
    public CicsAdapterException(String message) {
        super(message);
    }

    // -------------------------------------------------------------------------
    // Named subclasses — one per CICS RESP condition
    // -------------------------------------------------------------------------

    /**
     * Thrown when the CICS runtime returns {@code DFHRESP(NOTFINISHED)} (RESP2=52) from
     * {@code FETCH ANY NOSUSPEND} — no children have finished yet.
     */
    public static final class NotFinished extends CicsAdapterException {
        private static final long serialVersionUID = 1L;

        /** @param cause the original {@code NotFinishedException} */
        public NotFinished(Throwable cause) {
            super("FETCH ANY NOSUSPEND: NOTFINISHED", cause);
        }
    }

    /**
     * Thrown when the CICS runtime returns {@code DFHRESP(INVREQ)} (RESP2=1) from
     * {@code FETCH ANY NOSUSPEND} — the parent task had no child transactions.
     */
    public static final class InvReq extends CicsAdapterException {
        private static final long serialVersionUID = 1L;

        /** @param cause the original {@code InvalidRequestException} */
        public InvReq(Throwable cause) {
            super("FETCH ANY NOSUSPEND: INVREQ", cause);
        }
    }

    /**
     * Thrown when the CICS runtime returns {@code DFHRESP(NOTFND)} from
     * {@code FETCH ANY NOSUSPEND} — no more available responses.
     */
    public static final class NotFound extends CicsAdapterException {
        private static final long serialVersionUID = 1L;

        /** @param cause the original {@code NotFoundException} */
        public NotFound(Throwable cause) {
            super("FETCH ANY NOSUSPEND: NOTFND", cause);
        }
    }

    /**
     * Thrown when any other {@code CicsConditionException} occurs inside the adapter.
     */
    public static final class CicsCondition extends CicsAdapterException {
        private static final long serialVersionUID = 1L;

        /** @param cause the original {@code CicsConditionException} */
        public CicsCondition(Throwable cause) {
            super("CICS condition: " + cause.getMessage(), cause);
        }
    }
}
