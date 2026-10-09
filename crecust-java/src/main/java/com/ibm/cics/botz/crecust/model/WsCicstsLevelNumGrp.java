package com.ibm.cics.botz.crecust.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cat 3a REDEFINES view of {@code WS-CICSTSLEVEL PIC X(6)} in {@code WS-CICSTS-LEVEL-DATA}.
 *
 * <p>COBOL source (CRECUST.cbl lines 300–305):
 * <pre>
 *    01 WS-CICSTS-LEVEL-DATA.
 *       03 WS-CICSTSLEVEL              PIC X(6).
 *       03 WS-CICSTS-LEVEL-NUM-GRP REDEFINES WS-CICSTSLEVEL.
 *          05 WS-CICSTS-LEVEL-NUM-VV   PIC 99.
 *          05 WS-CICSTS-LEVEL-NUM-RR   PIC 99.
 *          05 WS-CICSTS-LEVEL-NUM-MM   PIC 99.
 * </pre>
 *
 * <p>The 6-character string is split into three consecutive 2-digit decimal fields (VV, RR, MM)
 * representing CICS TS version, release, and modification level.
 *
 * <p>Per ADR (architecture.md line 213–219), the Java equivalent populates this group from
 * {@code Task.getTask().getCicsVersion()} rather than from a commarea field.
 *
 * <p>Conversion:
 * <ul>
 *   <li>getter: {@code vv = Integer.parseInt(s.substring(0,2))},
 *               {@code rr = Integer.parseInt(s.substring(2,4))},
 *               {@code mm = Integer.parseInt(s.substring(4,6))}</li>
 *   <li>setter: {@code String.format("%02d%02d%02d", vv, rr, mm)}</li>
 * </ul>
 *
 * <p>The paired getter/setter ({@code getWsCicstsLevelNumGrp()} /
 * {@code setWsCicstsLevelNumGrp(WsCicstsLevelNumGrp)}) belong on {@code CrecustService},
 * which holds the {@code wsCicstslevel String} field.
 * <p><b>TODO:</b> {@code CrecustService} is not yet generated (Story 3+). Add
 * {@code getWsCicstsLevelNumGrp()} and {@code setWsCicstsLevelNumGrp(WsCicstsLevelNumGrp)} to
 * that class when it is created.
 *
 * @see "CRECUST.cbl lines 300–305"
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class WsCicstsLevelNumGrp {

    /** WS-CICSTS-LEVEL-NUM-VV PIC 99 — CICS TS version (e.g. 55 for CTS 5.5). */
    private int wsCicstsLevelVv;

    /** WS-CICSTS-LEVEL-NUM-RR PIC 99 — CICS TS release (e.g. 00). */
    private int wsCicstsLevelRr;

    /** WS-CICSTS-LEVEL-NUM-MM PIC 99 — CICS TS modification level (e.g. 00). */
    private int wsCicstsLevelMm;
}
