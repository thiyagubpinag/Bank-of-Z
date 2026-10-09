package com.ibm.cics.botz.crecust;

import com.ibm.cics.botz.crecust.exception.CrecustException;
import com.ibm.cics.botz.crecust.model.CrecustCommarea;
import com.ibm.cics.botz.crecust.serializer.CrecustareaSerializer;
import com.ibm.cics.botz.crecust.service.AbndprocDelegate;
import com.ibm.cics.botz.crecust.service.CreditCheckService;
import com.ibm.cics.botz.crecust.service.CrecustService;
import com.ibm.cics.botz.crecust.service.CustomerNumberService;
import com.ibm.cics.botz.crecust.service.ValidationService;
import com.ibm.cics.server.invocation.CICSProgram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CICS entry-point class for the CRECUST program.
 *
 * <p>Receives the 399-byte commarea from CICS, delegates to {@link CrecustService},
 * serialises the result back into the commarea byte buffer, and returns normally.
 * Equivalent to the COBOL {@code PREMIERE_P010} entry-point and the
 * {@code GET-ME-OUT-OF-HERE} single shared-exit paragraph.
 *
 * <p>{@code Task.getTask().returnToCaller()} does NOT exist in JCICS.
 * Returning from {@code main()} is the complete equivalent of {@code EXEC CICS RETURN}.
 *
 * <p>The {@link #main(byte[])} static method is the CICS OSGi JVM-server
 * entry point. The {@code @CICSProgram("CRECUST")}-annotated {@link #main()} instance
 * method is the Liberty CDI / annotation-processor entry point; the JCICS annotation
 * processor generates the corresponding {@code static main(byte[])} wrapper.
 */
public class Crecust {

    private static final Logger LOGGER = LoggerFactory.getLogger(Crecust.class);

    /** Commarea bytes set by the JCICS runtime before invoking the no-arg {@link #main()}. */
    private byte[] commareaHolder;

    private final AbndprocDelegate abndprocDelegate = AbndprocDelegate.getInstance();

    private final CrecustService service = new CrecustService(
            new ValidationService(),
            new CreditCheckService(),
            new CustomerNumberService());

    /**
     * JCICS Liberty / annotation-processor entry point.
     * The JCICS annotation processor generates a {@code static main(byte[])} proxy
     * that sets {@link #commareaHolder} and then calls this method.
     */
    @CICSProgram("CRECUST")
    public void main() {
        run(commareaHolder);
    }

    /**
     * CICS OSGi JVM-server entry point — called directly by the CICS runtime.
     *
     * @param cah commarea bytes provided by CICS
     */
    public static void main(byte[] cah) {
        Crecust instance = new Crecust();
        instance.commareaHolder = cah;
        instance.run(cah);
    }

    private void run(byte[] cah) {
        LOGGER.info("CRECUST entry");
        byte[] raw = abndprocDelegate.readCommarea(cah);
        CrecustCommarea commarea = CrecustareaSerializer.INSTANCE.fromBytes(raw, 0);
        try {
            service.execute(commarea);
        } catch (CrecustException e) {
            // fail-code already set in commarea before throw — fall through to serialise and return
        }
        byte[] out = CrecustareaSerializer.INSTANCE.toBytes(new byte[CrecustareaSerializer.SIZE], 0, commarea);
        abndprocDelegate.writeCommarea(out, raw);
        LOGGER.info("CRECUST exit");
        // return = EXEC CICS RETURN; Task.getTask().returnToCaller() does NOT exist in JCICS
    }
}
