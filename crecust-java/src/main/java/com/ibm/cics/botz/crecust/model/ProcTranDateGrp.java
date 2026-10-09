package com.ibm.cics.botz.crecust.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cat 3a REDEFINES view of {@code PROC-TRAN-DATE PIC 9(8)}.
 *
 * <p>COBOL source: {@code PROC-TRAN-DATE-GRP REDEFINES PROC-TRAN-DATE} with subordinate fields
 * {@code PROC-TRAN-DATE-GRP-YYYY PIC 9999}, {@code PROC-TRAN-DATE-GRP-MM PIC 99},
 * {@code PROC-TRAN-DATE-GRP-DD PIC 99}.
 *
 * <p>Conversion formulae (YYYYMMDD packed into an 8-digit {@code int}):
 * <ul>
 *   <li>getter: {@code year = procTranDate / 10000}, {@code month = (procTranDate / 100) % 100},
 *       {@code day = procTranDate % 100}</li>
 *   <li>setter: {@code procTranDate = year * 10000 + month * 100 + day}</li>
 * </ul>
 *
 * <p>Paired getter/setter live in {@link ProctranData}:
 * {@code getProcTranDateGrp()} and {@code setProcTranDateGrp(ProcTranDateGrp)}.
 *
 * @see "PROCTRAN.cpy lines 18–22"
 * @see ProctranData
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ProcTranDateGrp {

    /** PROC-TRAN-DATE-GRP-YYYY PIC 9999 — 4-digit year. */
    private int year;

    /** PROC-TRAN-DATE-GRP-MM PIC 99 — month (01–12). */
    private int month;

    /** PROC-TRAN-DATE-GRP-DD PIC 99 — day (01–31). */
    private int day;
}
