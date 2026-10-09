package com.ibm.cics.botz.crecust.service;

import com.ibm.cics.botz.crecust.model.AbndInfoRec;
import com.ibm.cics.botz.crecust.serializer.AbndInfoRecSerializer;
import com.ibm.cics.server.AsyncService.BlockingAction;
import com.ibm.cics.server.AsyncServiceImpl;
import com.ibm.cics.server.Channel;
import com.ibm.cics.server.ChildResponse;
import com.ibm.cics.server.CicsConditionException;
import com.ibm.cics.server.InvalidRequestException;
import com.ibm.cics.server.NameResource;
import com.ibm.cics.server.NotFinishedException;
import com.ibm.cics.server.NotFoundException;
import com.ibm.cics.server.Program;
import com.ibm.cics.server.Region;
import com.ibm.cics.server.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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

    private static final AbndprocDelegate INSTANCE = new AbndprocDelegate();

    private final AbndInfoRecSerializer abndInfoRecSerializer;

    /**
     * Constructs an {@code AbndprocDelegate} using the singleton serializer instance.
     */
    public AbndprocDelegate() {
        this(AbndInfoRecSerializer.INSTANCE);
    }

    /**
     * Constructs an {@code AbndprocDelegate} with the canonical serializer injected.
     *
     * @param abndInfoRecSerializer the singleton serializer for the {@code ABNDINFO-REC} layout
     */
    public AbndprocDelegate(AbndInfoRecSerializer abndInfoRecSerializer) {
        this.abndInfoRecSerializer = abndInfoRecSerializer != null ? abndInfoRecSerializer : AbndInfoRecSerializer.INSTANCE;
    }

    /**
     * Returns the singleton instance of {@code AbndprocDelegate}.
     *
     * @return the singleton instance
     */
    public static AbndprocDelegate getInstance() {
        return INSTANCE;
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

    /**
     * Retrieves the CICS TS version string (e.g. {@code "730"}), corresponding to
     * {@code EXEC CICS ASSIGN CICSTSLEVEL(WS-CICSTSLEVEL)}.
     *
     * <p>{@code Task.getCicsVersion()} is available on CICS TS 7.6+ / JCICS ≥ 2.200 GA on z/OS but is
     * absent from the local development stub jar (2.200.0-6.3 pre-GA). Uses reflection so
     * the call resolves at runtime on z/OS without modifying the provided-scope pom entry.
     * The fallback path (NoSuchMethodException) is only reachable in the off-z/OS dev build.
     *
     * @return the 3-character CICS TS version string or dev fallback {@code "000"}
     */
    public String getCicsVersion() {
        try {
            Method getCicsVersion = Task.class.getDeclaredMethod("getCicsVersion");
            return (String) getCicsVersion.invoke(Task.getTask());
        } catch (NoSuchMethodException e) {
            Package jcicsPackage = Task.class.getPackage();
            String implVersion = (jcicsPackage != null) ? jcicsPackage.getImplementationVersion() : null;
            if (implVersion != null && implVersion.length() >= 3) {
                return implVersion.substring(0, 3).replace(".", "");
            }
            LOGGER.debug("AbndprocDelegate.getCicsVersion() — getCicsVersion() not available in local JCICS stub; defaulting to '000'");
            return "000";
        } catch (Exception e) {
            LOGGER.warn("AbndprocDelegate.getCicsVersion() — unexpected exception calling getCicsVersion(); defaulting to '000'", e);
            return "000";
        }
    }

    // -------------------------------------------------------------------------
    // Async credit-check support (CreditCheckService)
    // -------------------------------------------------------------------------

    /** Completion status: child completed normally. */
    public static final String COMPLETION_NORMAL = "NORMAL";
    /** Completion status: child abended. */
    public static final String COMPLETION_ABEND = "ABEND";
    /** Completion status: child security error. */
    public static final String COMPLETION_SECERROR = "SECERROR";
    /** Completion status: any other completion. */
    public static final String COMPLETION_OTHER = "OTHER";

    /**
     * Returns the current CICS task number (EIBTASKN).
     *
     * @return the task number
     */
    public long getTaskNumber() {
        return Task.getTask().getTaskNumber();
    }

    /**
     * Returns the current CICS task number formatted as a zero-padded 4-digit string.
     * Translates {@code MOVE EIBTASKN TO ABND-TASKNO-KEY} (CRECUST.cbl).
     *
     * @return the 4-digit zero-padded task number string
     */
    public String getFormattedTaskNumber() {
        return String.format("%04d", Task.getTask().getTaskNumber());
    }

    /**
     * Returns the CICS region APPLID.
     * Translates {@code EXEC CICS ASSIGN APPLID(ABND-APPLID)} (CRECUST.cbl).
     *
     * @return the APPLID string
     */
    public String getApplid() {
        return Region.getAPPLID();
    }

    /**
     * Returns the current CICS transaction name (EIBTRNID).
     * Translates {@code MOVE EIBTRNID TO ABND-TRANID} (CRECUST.cbl).
     *
     * @return the transaction name string
     */
    public String getTransactionName() {
        return Task.getTask().getTransactionName();
    }

    /**
     * Returns the invoking program name.
     * Translates {@code EXEC CICS ASSIGN PROGRAM(ABND-PROGRAM)} (CRECUST.cbl).
     *
     * @return the invoking program name string
     */
    public String getInvokingProgramName() {
        return Task.getTask().getInvokingProgramName();
    }

    /**
     * Issues {@code EXEC CICS ABEND ABCODE(abendCode)}.
     * Translates {@code EXEC CICS ABEND ABCODE('HWPT')} (CRECUST.cbl line 1455).
     *
     * @param abendCode the 4-character abend code
     */
    public void abend(String abendCode) {
        Task.getTask().abend(abendCode);
    }

    /**
     * Creates the named channel and PUTs {@code data} into the named container.
     * Maps {@code EXEC CICS PUT CONTAINER ... CHANNEL(...)}.
     *
     * @param channelName   CICS channel name
     * @param containerName container name
     * @param data          bytes to put
     * @throws CicsAdapterException on any CICS condition
     */
    public void putContainer(String channelName, String containerName, byte[] data)
            throws CicsAdapterException {
        try {
            Channel channel = Task.getTask().createChannel(channelName);
            channel.createContainer(containerName).put(data);
        } catch (CicsConditionException e) {
            throw new CicsAdapterException(e.getMessage(), e.getRESP(), e.getRESP2(), e);
        }
    }

    /**
     * Runs a child transaction asynchronously on the named channel.
     * Maps {@code EXEC CICS RUN TRANSID(...) CHANNEL(...) CHILD(...)}.
     *
     * @param transId     the transaction id
     * @param channelName the channel name
     * @return an opaque child token
     * @throws CicsAdapterException on any CICS condition
     */
    public Future<?> runTransaction(String transId, String channelName) throws CicsAdapterException {
        try {
            Channel channel = Task.getTask().getChannel(channelName);
            AsyncServiceImpl asyncService = new AsyncServiceImpl();
            Future<ChildResponse> token = asyncService.runTransactionId(transId, channel);
            return token;
        } catch (CicsConditionException e) {
            throw new CicsAdapterException(e.getMessage(), e.getRESP(), e.getRESP2(), e);
        }
    }

    /**
     * Performs {@code EXEC CICS FETCH ANY NOSUSPEND}.
     *
     * @return an opaque child response handle
     * @throws CicsAdapterException.NotFinished    on NOTFINISHED
     * @throws CicsAdapterException.InvalidRequest on INVREQ
     * @throws CicsAdapterException.NotFound       on NOTFND
     */
    public Object fetchAnyNoSuspend() throws CicsAdapterException.NotFinished,
            CicsAdapterException.InvalidRequest, CicsAdapterException.NotFound {
        AsyncServiceImpl asyncService = new AsyncServiceImpl();
        try {
            return asyncService.getAny(BlockingAction.NOSUSPEND);
        } catch (NotFinishedException e) {
            throw new CicsAdapterException.NotFinished(e.getMessage(), e.getRESP(), e.getRESP2(), e);
        } catch (InvalidRequestException e) {
            throw new CicsAdapterException.InvalidRequest(e.getMessage(), e.getRESP(), e.getRESP2(), e);
        } catch (NotFoundException e) {
            throw new CicsAdapterException.NotFound(e.getMessage(), e.getRESP(), e.getRESP2(), e);
        }
    }

    /**
     * Returns the completion status of a child response as one of
     * {@link #COMPLETION_NORMAL}, {@link #COMPLETION_ABEND}, {@link #COMPLETION_SECERROR},
     * {@link #COMPLETION_OTHER}.
     *
     * @param childResponse handle returned by {@link #fetchAnyNoSuspend()}
     * @return the completion status string
     */
    public String getCompletionStatus(Object childResponse) {
        ChildResponse.CompletionStatus compStatus = ((ChildResponse) childResponse).getCompletionStatus();
        if (compStatus == ChildResponse.CompletionStatus.NORMAL) {
            return COMPLETION_NORMAL;
        } else if (compStatus == ChildResponse.CompletionStatus.ABEND) {
            return COMPLETION_ABEND;
        } else if (compStatus == ChildResponse.CompletionStatus.SECERROR) {
            return COMPLETION_SECERROR;
        }
        return COMPLETION_OTHER;
    }

    /**
     * Returns the channel name of a child response (for diagnostics).
     *
     * @param childResponse handle returned by {@link #fetchAnyNoSuspend()}
     * @return the channel name, or {@code null} if none
     */
    public String getChildChannelName(Object childResponse) {
        Channel channel = ((ChildResponse) childResponse).getChannel();
        return channel != null ? channel.getName() : null;
    }

    /**
     * GETs a container from the channel of a child response.
     * Maps {@code EXEC CICS GET CONTAINER(...) CHANNEL(...)}.
     *
     * @param childResponse handle returned by {@link #fetchAnyNoSuspend()}
     * @param containerName container name
     * @return the container bytes
     * @throws CicsAdapterException on any CICS condition
     */
    public byte[] getChildContainer(Object childResponse, String containerName) throws CicsAdapterException {
        Channel channel = ((ChildResponse) childResponse).getChannel();
        try {
            return channel.getContainer(containerName).get();
        } catch (CicsConditionException e) {
            throw new CicsAdapterException(e.getMessage(), e.getRESP(), e.getRESP2(), e);
        }
    }

    /**
     * Returns whether the given child token resolves to the given child response.
     *
     * @param childResponse handle returned by {@link #fetchAnyNoSuspend()}
     * @param token         token returned by {@link #runTransaction(String, String)}
     * @return {@code true} if they match
     * @throws InterruptedException if interrupted while getting the future
     * @throws ExecutionException   if the future completed exceptionally
     */
    public boolean isSameChildResponse(Object childResponse, Future<?> token)
            throws InterruptedException, ExecutionException {
        return childResponse.equals(token.get());
    }

    // -------------------------------------------------------------------------
    // Named-counter ENQ / DEQ support (CustomerNumberService)
    // -------------------------------------------------------------------------

    /**
     * Issues {@code EXEC CICS ENQ RESOURCE(resourceName) LENGTH(length)} for the
     * customer-number named-counter lock.
     *
     * <p>Translates {@code ENQ-NAMED-COUNTER_ENC010} (CRECUST.cbl lines 541–556):
     * <pre>
     *   EXEC CICS ENQ RESOURCE(NCS-CUST-NO-NAME) LENGTH(16) ...
     * </pre>
     *
     * @param resourceName the 16-byte ENQ resource name
     * @throws CicsAdapterException if the CICS ENQ condition is not NORMAL
     */
    public void enqueue(String resourceName) throws CicsAdapterException {
        NameResource nameResource = new NameResource();
        nameResource.setName(resourceName);
        try {
            nameResource.enqueue();
        } catch (CicsConditionException e) {
            throw new CicsAdapterException(e.getMessage(), e.getRESP(), e.getRESP2(), e);
        }
    }

    /**
     * Issues {@code EXEC CICS DEQ RESOURCE(resourceName) LENGTH(length)} to release the
     * customer-number named-counter lock.
     *
     * <p>Translates {@code DEQ-NAMED-COUNTER_DNC010} (CRECUST.cbl lines 562–584):
     * <pre>
     *   EXEC CICS DEQ RESOURCE(NCS-CUST-NO-NAME) LENGTH(16) ...
     * </pre>
     *
     * @param resourceName the 16-byte DEQ resource name (must match the one passed to
     *                     {@link #enqueue(String)})
     * @throws CicsAdapterException if the CICS DEQ condition is not NORMAL
     */
    public void dequeue(String resourceName) throws CicsAdapterException {
        NameResource nameResource = new NameResource();
        nameResource.setName(resourceName);
        try {
            nameResource.dequeue();
        } catch (CicsConditionException e) {
            throw new CicsAdapterException(e.getMessage(), e.getRESP(), e.getRESP2(), e);
        }
    }
}
