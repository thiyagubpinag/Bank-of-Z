package com.ibm.cics.botz.crecust.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DB2 host-variable row class for the {@code CONTROL} table ({@code HOST-CONTROL-ROW}).
 *
 * <p>3 fields. {@code hvControlValueNum} is {@code int} (from {@code S9(9) COMP} INTEGER column).
 *
 * @see "CRECUST.cbl WORKING-STORAGE — HOST-CONTROL-ROW (line 193)"
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class HostControlRow {

    /**
     * HV-CONTROL-NAME PIC X(32) — 32 bytes.
     *
     * <p>Key: 'BANKZCUST' + sortcode + '  ' (two spaces).
     */
    private String hvControlName;

    /**
     * HV-CONTROL-VALUE-NUM PIC S9(9) COMP — 4 bytes.
     *
     * <p>Mapped to {@code int}. Current maximum sequential customer number (DB2 INTEGER column).
     */
    private int hvControlValueNum;

    /** HV-CONTROL-VALUE-STR PIC X(32) — 32 bytes. Not used by CRECUST. */
    private String hvControlValueStr;
}
