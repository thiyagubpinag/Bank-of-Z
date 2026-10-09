package com.ibm.cics.botz.crecust.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cat 3a REDEFINES view of {@code WS-ORIG-DATE PIC X(10)}.
 *
 * <p>COBOL source (CRECUST.cbl lines 200–206):
 * <pre>
 *    01 WS-ORIG-DATE                  PIC X(10).
 *    01 WS-ORIG-DATE-GRP REDEFINES WS-ORIG-DATE.
 *       03 WS-ORIG-DATE-DD            PIC 99.
 *       03 FILLER                     PIC X.
 *       03 WS-ORIG-DATE-MM            PIC 99.
 *       03 FILLER                     PIC X.
 *       03 WS-ORIG-DATE-YYYY          PIC 9999.
 * </pre>
 *
 * <p>The two FILLER {@code PIC X} positions carry the DATESEP character injected by
 * {@code EXEC CICS FORMATTIME DDMMYYYY(WS-ORIG-DATE) END-EXEC}; the default separator is
 * {@code '/'} and is never written by the application. They are modelled as {@code static final}
 * constants — not as settable Java fields.
 *
 * <p>Conversion:
 * <ul>
 *   <li>getter: parse {@code wsOrigDate} {@code String} as
 *       {@code dd=Integer.parseInt(s.substring(0,2))},
 *       {@code mm=Integer.parseInt(s.substring(3,5))},
 *       {@code yyyy=Integer.parseInt(s.substring(6,10))}</li>
 *   <li>setter: {@code String.format("%02d%c%02d%c%04d", dd, SEP, mm, SEP, yyyy)}</li>
 * </ul>
 *
 * <p>The paired getter/setter ({@code getWsOrigDateGrp()} / {@code setWsOrigDateGrp(WsOrigDateGrp)})
 * belong on {@code CrecustService}, which holds the {@code wsOrigDate String} field.
 * <p><b>TODO:</b> {@code CrecustService} is not yet generated (Story 3+). Add
 * {@code getWsOrigDateGrp()} and {@code setWsOrigDateGrp(WsOrigDateGrp)} to that class when it is
 * created.
 *
 * @see "CRECUST.cbl lines 200–206"
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class WsOrigDateGrp {

    /**
     * Date separator character used by CICS FORMATTIME DDMMYYYY.
     * Maps to the two FILLER {@code PIC X} positions in {@code WS-ORIG-DATE-GRP}.
     */
    public static final char SEP = '/';

    /** WS-ORIG-DATE-DD PIC 99 — day (01–31). */
    private int dd;

    /** WS-ORIG-DATE-MM PIC 99 — month (01–12). */
    private int mm;

    /** WS-ORIG-DATE-YYYY PIC 9999 — 4-digit year. */
    private int yyyy;
}
