package com.ibm.cics.botz.crecust.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cat 3a REDEFINES view of {@code PROC-TRAN-TIME PIC 9(6)}.
 *
 * <p>COBOL source: {@code PROC-TRAN-TIME-GRP REDEFINES PROC-TRAN-TIME} with subordinate fields
 * {@code PROC-TRAN-TIME-GRP-HH PIC 99}, {@code PROC-TRAN-TIME-GRP-MM PIC 99},
 * {@code PROC-TRAN-TIME-GRP-SS PIC 99}.
 *
 * <p>Conversion formulae (HHMMSS packed into a 6-digit {@code int}):
 * <ul>
 *   <li>getter: {@code hours = procTranTime / 10000}, {@code mins = (procTranTime / 100) % 100},
 *       {@code secs = procTranTime % 100}</li>
 *   <li>setter: {@code procTranTime = hours * 10000 + mins * 100 + secs}</li>
 * </ul>
 *
 * <p>Paired getter/setter live in {@link ProctranData}:
 * {@code getProcTranTimeGrp()} and {@code setProcTranTimeGrp(ProcTranTimeGrp)}.
 *
 * @see "PROCTRAN.cpy lines 23–27"
 * @see ProctranData
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ProcTranTimeGrp {

    /** PROC-TRAN-TIME-GRP-HH PIC 99 — hours (00–23). */
    private int hours;

    /** PROC-TRAN-TIME-GRP-MM PIC 99 — minutes (00–59). */
    private int mins;

    /** PROC-TRAN-TIME-GRP-SS PIC 99 — seconds (00–59). */
    private int secs;
}
