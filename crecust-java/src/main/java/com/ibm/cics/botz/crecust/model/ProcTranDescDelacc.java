package com.ibm.cics.botz.crecust.model;

/**
 * Concrete view for {@code PROC-TRAN-DESC-DELACC} over the 40-byte shared buffer.
 *
 * <p>Declares NO instance fields.
 */
public class ProcTranDescDelacc extends ProcTranDescBase {

    // TODO: field layout defined by consuming program (DELACC transaction)

    public ProcTranDescDelacc(byte[] data) {
        super(data);
    }
}
