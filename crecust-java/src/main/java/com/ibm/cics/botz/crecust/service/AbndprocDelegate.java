package com.ibm.cics.botz.crecust.service;

import com.ibm.cics.botz.crecust.model.AbndInfoRec;
import com.ibm.cics.botz.crecust.serializer.AbndInfoRecSerializer;
import com.ibm.cics.server.CicsConditionException;
import com.ibm.cics.server.Program;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delegates to the {@code ABNDPROC} CICS program via {@code EXEC CICS LINK}.
 *
 * <p>Corresponds to CRECUST.cbl {@code WRITE-PROCTRAN-DB2_WPD010} (line 1441):
 * <pre>
 *   EXEC CICS LINK PROGRAM(WS-ABEND-PGM) COMMAREA(ABNDINFO-REC) END-EXEC
 * </pre>
 *
 * <p>The return code from {@code ABNDPROC} is never inspected in the COBOL source — method
 * therefore returns {@code void} (ADR-11, Rule 10, AC-10.1).
 *
 * <p>{@code AbndInfoRecSerializer} is the canonical serializer for the {@code ABNDINFO-REC}
 * byte-array layout; no inline byte-packing is performed here (Rule 15, AC-15.1).
 *
 * @see "CRECUST.cbl WRITE-PROCTRAN-DB2_WPD010 (line 1441): EXEC CICS LINK PROGRAM(WS-ABEND-PGM)"
 * @see "ADR-Rule-10 — return code not inspected; method returns void"
 * @see "Rule 15 — serializer delegation: AbndInfoRecSerializer.INSTANCE is the sole byte-packer"
 */
public class AbndprocDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbndprocDelegate.class);

    private final AbndInfoRecSerializer abndInfoRecSerializer;

    /**
     * Constructs an {@code AbndprocDelegate} with the canonical serializer injected.
     *
     * @param abndInfoRecSerializer the singleton serializer for the {@code ABNDINFO-REC} layout
     */
    public AbndprocDelegate(AbndInfoRecSerializer abndInfoRecSerializer) {
        this.abndInfoRecSerializer = abndInfoRecSerializer;
    }

    /**
     * Serialises {@code abndInfoRec} and calls {@code new Program("ABNDPROC").link(commarea)}.
     *
     * <p>Translates CRECUST.cbl line 1441:
     * <pre>
     *   EXEC CICS LINK PROGRAM(WS-ABEND-PGM) COMMAREA(ABNDINFO-REC) END-EXEC
     * </pre>
     *
     * <p>The return code from {@code ABNDPROC} is not inspected — method returns {@code void}
     * (ADR-11, Rule 10). Byte-packing is fully delegated to
     * {@link AbndInfoRecSerializer#toBytes(Object)} (Rule 15, AC-15.1).
     *
     * <p>A {@link CicsConditionException} from {@code Program.link()} is caught and logged at
     * {@code ERROR} level; it does not prevent the subsequent ABEND from occurring.
     *
     * @param abndInfoRec the fully populated {@code ABNDINFO-REC} to pass as the ABNDPROC commarea
     */
    public void linkAbndproc(AbndInfoRec abndInfoRec) {
        LOGGER.error(
                "AbndprocDelegate.linkAbndproc() — linking ABNDPROC; abndSqlcode={}",
                abndInfoRec.getAbndSqlcode());

        // Rule 15 / AC-15.1 — canonical serializer is the only byte-packing path
        byte[] bytes = abndInfoRecSerializer.toBytes(abndInfoRec);

        // EXEC CICS LINK PROGRAM(WS-ABEND-PGM) COMMAREA(ABNDINFO-REC) (CRECUST.cbl line 1441)
        try {
            new Program("ABNDPROC").link(bytes);
        } catch (CicsConditionException e) {
            // COBOL has no condition check on this LINK; log and continue to ABEND
            LOGGER.error(
                    "AbndprocDelegate.linkAbndproc() — ABNDPROC link failed (CICS RESP={}); "
                    + "continuing to ABEND",
                    e.getRESP(), e);
        }
        // Return type is void — no return value captured (ADR-11, Rule 10, AC-10.1)
    }
}
