package com.ibm.cics.botz.crecust.model;

import com.ibm.cics.botz.common.ByteArraySerializable;
import com.ibm.cics.botz.common.ByteArraySerializer;
import com.ibm.cics.botz.crecust.serializer.AbndInfoRecSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for the ABNDINFO.cpy record ({@code ABNDINFO-REC}).
 *
 * <p>12 fields; combined serialized width approximately 673 bytes.
 * Implements {@link ByteArraySerializable} — serializer wired in Story 2.5.
 *
 * @see "CRECUST.cbl WORKING-STORAGE — ABNDINFO-REC (line 330)"
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class AbndInfoRec implements ByteArraySerializable<AbndInfoRec> {

    /**
     * ABND-UTIME-KEY PIC S9(15) COMP-3 — 8 bytes (packed decimal).
     *
     * <p>Microsecond CICS absolute time key used as the primary VSAM key.
     */
    private long abndUtimeKey;

    /** ABND-TASKNO-KEY PIC 9(4) — 4 bytes. CICS task number. */
    private String abndTasknoKey;

    /** ABND-APPLID PIC X(8) — 8 bytes. CICS APPLID (from EXEC CICS ASSIGN APPLID). */
    private String abndApplid;

    /** ABND-TRANID PIC X(4) — 4 bytes. CICS transaction identifier. */
    private String abndTranid;

    /** ABND-DATE PIC X(10) — 10 bytes. Date formatted by EXEC CICS FORMATTIME. */
    private String abndDate;

    /** ABND-TIME PIC X(8) — 8 bytes. Time formatted by EXEC CICS FORMATTIME. */
    private String abndTime;

    /** ABND-CODE PIC X(4) — 4 bytes. ABEND code (e.g. 'HWPT'). */
    private String abndCode;

    /** ABND-PROGRAM PIC X(8) — 8 bytes. Program name (from EXEC CICS ASSIGN PROGRAM). */
    private String abndProgram;

    /**
     * ABND-RESPCODE PIC S9(8) DISPLAY SIGN LEADING SEPARATE — 9 bytes.
     *
     * <p>CICS RESP value from the failing call.
     */
    private String abndRespcode;

    /**
     * ABND-RESP2CODE PIC S9(8) DISPLAY SIGN LEADING SEPARATE — 9 bytes.
     *
     * <p>CICS RESP2 value from the failing call.
     */
    private String abndResp2code;

    /**
     * ABND-SQLCODE PIC S9(8) DISPLAY SIGN LEADING SEPARATE — 9 bytes.
     *
     * <p>DB2 SQLCODE from the failing SQL statement (if applicable).
     */
    private String abndSqlcode;

    /** ABND-FREEFORM PIC X(600) — 600 bytes. Free-form abend description text. */
    private String abndFreeform;

    /**
     * Returns the serializer for this type.
     *
     * @return serializer for {@code AbndInfoRec}
     */
    @Override
    public ByteArraySerializer<AbndInfoRec> serializer() {
        return AbndInfoRecSerializer.INSTANCE;
    }

    /**
     * Sets this object's fields from {@code that} (COBOL MOVE semantics).
     *
     * @param that source object
     */
    @Override
    public void set(AbndInfoRec that) {
        if (that == this) return;
        this.abndUtimeKey = that.abndUtimeKey;
        this.abndTasknoKey = that.abndTasknoKey;
        this.abndApplid = that.abndApplid;
        this.abndTranid = that.abndTranid;
        this.abndDate = that.abndDate;
        this.abndTime = that.abndTime;
        this.abndCode = that.abndCode;
        this.abndProgram = that.abndProgram;
        this.abndRespcode = that.abndRespcode;
        this.abndResp2code = that.abndResp2code;
        this.abndSqlcode = that.abndSqlcode;
        this.abndFreeform = that.abndFreeform;
    }
}
