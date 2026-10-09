package com.ibm.cics.botz.crecust.model;

/**
 * Concrete view for {@code PROC-TRAN-DESC-CREACC} over the 40-byte shared buffer.
 *
 * <p>Declares NO instance fields.
 */
public class ProcTranDescCreacc extends ProcTranDescBase {

    // TODO: field layout defined by consuming program (CREACC transaction)

    public ProcTranDescCreacc(byte[] data) {
        super(data);
    }
}
