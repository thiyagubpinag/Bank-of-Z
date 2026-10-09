package com.ibm.cics.botz.crecust.service;

/**
 * Adapter-level exception that carries the details of a CICS condition raised inside
 * {@link AbndprocDelegate}, so that calling code never depends on JCICS exception types.
 *
 * <p>Nested subclasses identify the specific conditions the callers distinguish
 * (NOTFINISHED, INVREQ, NOTFND) so existing catch blocks keep their exact semantics.
 */
public class CicsAdapterException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int resp;
    private final int resp2;

    public CicsAdapterException(String message, int resp, int resp2, Throwable cause) {
        super(message, cause);
        this.resp = resp;
        this.resp2 = resp2;
    }

    /** @return the CICS RESP value of the original condition */
    public int getResp() {
        return resp;
    }

    /** @return the CICS RESP2 value of the original condition */
    public int getResp2() {
        return resp2;
    }

    /** CICS NOTFINISHED condition. */
    public static class NotFinished extends CicsAdapterException {
        private static final long serialVersionUID = 1L;

        public NotFinished(String message, int resp, int resp2, Throwable cause) {
            super(message, resp, resp2, cause);
        }
    }

    /** CICS INVREQ condition. */
    public static class InvalidRequest extends CicsAdapterException {
        private static final long serialVersionUID = 1L;

        public InvalidRequest(String message, int resp, int resp2, Throwable cause) {
            super(message, resp, resp2, cause);
        }
    }

    /** CICS NOTFND condition. */
    public static class NotFound extends CicsAdapterException {
        private static final long serialVersionUID = 1L;

        public NotFound(String message, int resp, int resp2, Throwable cause) {
            super(message, resp, resp2, cause);
        }
    }
}
