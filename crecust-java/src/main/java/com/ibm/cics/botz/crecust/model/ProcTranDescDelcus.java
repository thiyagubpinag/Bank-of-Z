package com.ibm.cics.botz.crecust.model;

/**
 * Concrete view for {@code PROC-TRAN-DESC-DELCUS} over the 40-byte shared buffer.
 *
 * <p>Declares NO instance fields.
 */
public class ProcTranDescDelcus extends ProcTranDescBase {

    // TODO: field layout defined by consuming program (DELCUS transaction)

    public ProcTranDescDelcus(byte[] data) {
        super(data);
    }
}
