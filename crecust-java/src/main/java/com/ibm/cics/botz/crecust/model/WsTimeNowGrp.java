package com.ibm.cics.botz.crecust.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cat 3a REDEFINES view of {@code WS-TIME-NOW PIC 9(6)} in {@code WS-TIME-DATA}.
 *
 * <p>COBOL source (CRECUST.cbl lines 131–135):
 * <pre>
 *    03 WS-TIME-NOW                PIC 9(6).
 *    03 WS-TIME-NOW-GRP REDEFINES WS-TIME-NOW.
 *       05 WS-TIME-NOW-GRP-HH     PIC 99.
 *       05 WS-TIME-NOW-GRP-MM     PIC 99.
 *       05 WS-TIME-NOW-GRP-SS     PIC 99.
 * </pre>
 *
 * <p>Conversion formulae (HHMMSS packed into a 6-digit {@code int}):
 * <ul>
 *   <li>getter: {@code hh = wsTimeNow / 10000}, {@code mm = (wsTimeNow / 100) % 100},
 *       {@code ss = wsTimeNow % 100}</li>
 *   <li>setter: {@code wsTimeNow = hh * 10000 + mm * 100 + ss}</li>
 * </ul>
 *
 * <p>The paired getter/setter ({@code getWsTimeNowGrp()} / {@code setWsTimeNowGrp(WsTimeNowGrp)})
 * belong on {@code CrecustService}, which holds the {@code wsTimeNow int} field as a local/instance
 * variable.
 * <p><b>TODO:</b> {@code CrecustService} is not yet generated (Story 3+). Add
 * {@code getWsTimeNowGrp()} and {@code setWsTimeNowGrp(WsTimeNowGrp)} to that class when it is
 * created.
 *
 * @see "CRECUST.cbl lines 130–135"
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class WsTimeNowGrp {

    /** WS-TIME-NOW-GRP-HH PIC 99 — hours (00–23). */
    private int hh;

    /** WS-TIME-NOW-GRP-MM PIC 99 — minutes (00–59). */
    private int mm;

    /** WS-TIME-NOW-GRP-SS PIC 99 — seconds (00–59). */
    private int ss;
}
