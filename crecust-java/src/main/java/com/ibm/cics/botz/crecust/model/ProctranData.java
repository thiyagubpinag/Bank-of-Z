package com.ibm.cics.botz.crecust.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for the PROCTRAN.cpy root record ({@code PROC-TRAN-DATA}).
 *
 * <p>The 40-byte {@code PROC-TRAN-DESC} field is stored as a raw {@code byte[]} to preserve the
 * shared-memory semantics required by the five Cat 4a REDEFINES view classes generated in Story
 * 2.3. Does NOT implement {@link com.ibm.cics.botz.common.ByteArraySerializable}.
 *
 * @see "CRECUST.cbl WORKING-STORAGE — PROCTRAN-AREA / PROC-TRAN-DATA (line 203)"
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ProctranData {

    /** PROC-TRAN-EYE-CATCHER PIC X(4) — 4 bytes. 88: PROC-TRAN-VALID VALUE 'PRTR'. */
    private String procTranEyeCatcher;

    /** PROC-TRAN-SORT-CODE PIC 9(6) — 6 bytes. */
    private String procTranSortCode;

    /** PROC-TRAN-NUMBER PIC 9(8) — 8 bytes. */
    private String procTranNumber;

    /**
     * PROC-TRAN-DATE PIC 9(8) — 8-digit packed decimal date (YYYYMMDD).
     *
     * <p>Cat 3a REDEFINES: use {@link #getProcTranDateGrp()} / {@link #setProcTranDateGrp(ProcTranDateGrp)}
     * to access the year/month/day sub-fields.
     */
    private int procTranDate;

    /**
     * PROC-TRAN-TIME PIC 9(6) — 6-digit packed decimal time (HHMMSS).
     *
     * <p>Cat 3a REDEFINES: use {@link #getProcTranTimeGrp()} / {@link #setProcTranTimeGrp(ProcTranTimeGrp)}
     * to access the hours/mins/secs sub-fields.
     */
    private int procTranTime;

    /** PROC-TRAN-REF PIC 9(12) — 6 bytes. */
    private String procTranRef;

    /** PROC-TRAN-TYPE PIC X(3) — 3 bytes. Many 88-level type codes. */
    private String procTranType;

    /**
     * PROC-TRAN-DESC PIC X(40) — 40 bytes stored as raw byte array.
     *
     * <p>Five Cat 4a REDEFINES groups ({@code PROC-TRAN-DESC-XFR}, {@code -DELACC},
     * {@code -CREACC}, {@code -DELCUS}, {@code -CRECUS}) overlay this field; those view classes
     * are created in Story 2.3 and hold a reference to this array.
     */
    @Builder.Default
    private byte[] procTranDesc = new byte[40];

    /** PROC-TRAN-AMOUNT PIC S9(10)V99 COMP-3 — 6 bytes. */
    private String procTranAmount;

    // ── Cat 3a REDEFINES: PROC-TRAN-DATE-GRP ────────────────────────────────

    /**
     * Returns a {@link ProcTranDateGrp} view of {@code procTranDate}.
     *
     * <p>COBOL equivalent: reference to {@code PROC-TRAN-DATE-GRP} fields YYYY/MM/DD.
     * Conversion: {@code year = procTranDate / 10000},
     * {@code month = (procTranDate / 100) % 100}, {@code day = procTranDate % 100}.
     */
    public ProcTranDateGrp getProcTranDateGrp() {
        return ProcTranDateGrp.builder()
                .year(procTranDate / 10000)
                .month((procTranDate / 100) % 100)
                .day(procTranDate % 100)
                .build();
    }

    /**
     * Encodes the supplied {@link ProcTranDateGrp} back into {@code procTranDate}.
     *
     * <p>Inverse: {@code procTranDate = year * 10000 + month * 100 + day}.
     *
     * @param grp structured date view to encode
     */
    public void setProcTranDateGrp(ProcTranDateGrp grp) {
        this.procTranDate = grp.getYear() * 10000 + grp.getMonth() * 100 + grp.getDay();
    }

    // ── Cat 3a REDEFINES: PROC-TRAN-TIME-GRP ────────────────────────────────

    /**
     * Returns a {@link ProcTranTimeGrp} view of {@code procTranTime}.
     *
     * <p>COBOL equivalent: reference to {@code PROC-TRAN-TIME-GRP} fields HH/MM/SS.
     * Conversion: {@code hours = procTranTime / 10000},
     * {@code mins = (procTranTime / 100) % 100}, {@code secs = procTranTime % 100}.
     */
    public ProcTranTimeGrp getProcTranTimeGrp() {
        return ProcTranTimeGrp.builder()
                .hours(procTranTime / 10000)
                .mins((procTranTime / 100) % 100)
                .secs(procTranTime % 100)
                .build();
    }

    /**
     * Encodes the supplied {@link ProcTranTimeGrp} back into {@code procTranTime}.
     *
     * <p>Inverse: {@code procTranTime = hours * 10000 + mins * 100 + secs}.
     *
     * @param grp structured time view to encode
     */
    public void setProcTranTimeGrp(ProcTranTimeGrp grp) {
        this.procTranTime = grp.getHours() * 10000 + grp.getMins() * 100 + grp.getSecs();
    }
}
