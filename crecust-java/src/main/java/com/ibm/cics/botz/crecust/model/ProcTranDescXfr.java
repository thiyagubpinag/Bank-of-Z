package com.ibm.cics.botz.crecust.model;

/**
 * Concrete view for {@code PROC-TRAN-DESC-XFR} over the 40-byte shared buffer.
 *
 * <p>Declares NO instance fields.
 */
public class ProcTranDescXfr extends ProcTranDescBase {

    // TODO: field layout defined by consuming program (XFR transaction)

    public ProcTranDescXfr(byte[] data) {
        super(data);
    }
}
