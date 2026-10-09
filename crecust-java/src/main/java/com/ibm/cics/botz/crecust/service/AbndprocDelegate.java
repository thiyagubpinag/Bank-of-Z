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
import com.ibm.cics.server.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
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
     * Constructs an {@code AbndprocDelegate} using the canonical serializer singleton.
     */
    public AbndprocDelegate() {
        this.abndInfoRecSerializer = AbndInfoRecSerializer.INSTANCE;
    }

    /**
     * Returns the singleton instance.
     *
     * @return the shared {@code AbndprocDelegate}
     */
    public static AbndprocDelegate getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the CICS version string ({@code CICSTSLEVEL}) of the current CICS task.
     *
     * <p>{@code Task.getCicsVersion()} is absent from the local development JCICS stub jar, so it is
     * resolved reflectively. When it is unavailable the version is derived from the JCICS package
     * implementation version, falling back to {@code "000"}.
     *
     * @param versionLength number of characters expected in the version string
     * @return the CICS version string
     */
    public String getCicsVersion(int versionLength) {
        try {
            java.lang.reflect.Method getCicsVersion = Task.class.getDeclaredMethod("getCicsVersion");
            return (String) getCicsVersion.invoke(Task.getTask());
        } catch (NoSuchMethodException e) {
            Package jcicsPackage = Task.class.getPackage();
            String implVersion = (jcicsPackage != null) ? jcicsPackage.getImplementationVersion() : null;
            if (implVersion != null && implVersion.length() >= versionLength) {
                return implVersion.substring(0, versionLength).replace(".", "");
            }
            return "000";
        } catch (Exception e) {
            return "000";
        }
    }

    /**
     * Returns the commarea buffer supplied by the CICS runtime entry point.
     *
     * @param commarea the commarea bytes received from CICS
     * @return the commarea buffer to operate on
     */
    public byte[] readCommarea(byte[] commarea) {
        return commarea;
    }

    /**
     * Copies the serialised result back into the commarea buffer, truncating to the
     * smaller of the two lengths.
     *
     * @param out  serialised result bytes
     * @param raw  commarea buffer to update in place
     */
    public void writeCommarea(byte[] out, byte[] raw) {
        System.arraycopy(out, 0, raw, 0, Math.min(out.length, raw.length));
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

    // -------------------------------------------------------------------------
    // CICS async/channel adapter methods — CreditCheckService (7 statements)
    // -------------------------------------------------------------------------

    /**
     * Thread-local list of issued child-transaction tokens, populated by
     * {@link #runTransactionId(String, String)} and consumed by
     * {@link #fetchAnyNosuspend()}.
     *
     * <p>Keyed by issuance order (index 0 = OCR1, …, 4 = OCR5).
     */
    private static final ThreadLocal<List<Future<ChildResponse>>> ISSUED_TOKENS =
            ThreadLocal.withInitial(ArrayList::new);

    /**
     * Thread-local cache of the most recent {@code ChildResponse} returned by
     * {@link #fetchAnyNosuspend()}, used by {@link #getContainerBytes(int, String)}.
     */
    private static final ThreadLocal<ChildResponse> LAST_CHILD_RESPONSE =
            ThreadLocal.withInitial(() -> null);

    /**
     * Returns the CICS task number ({@code EIBTASKN}) of the current task.
     *
     * <p>Encapsulates CRECUST.cbl line 833 / 940:
     * <pre>
     *   MOVE EIBTASKN TO WS-SEED
     * </pre>
     * Java: {@code long eibtaskn = Task.getTask().getTaskNumber();}
     *
     * @return the current task number
     */
    public long getTaskNumber() {
        return Task.getTask().getTaskNumber();
    }

    /**
     * Creates a CICS channel with the given name on the current task.
     *
     * <p>Encapsulates CRECUST.cbl PUT-CONTAINER_PCT010 step 1:
     * <pre>
     *   EXEC CICS CREATE CHANNEL(channelName) END-EXEC
     * </pre>
     * Java: {@code Channel channel = Task.getTask().createChannel(channelName);}
     * Then puts {@code commareaBytes} into container {@code containerName} on that channel.
     *
     * @param channelName   CICS channel name
     * @param containerName CICS container name
     * @param commareaBytes serialised commarea bytes to PUT
     * @throws CicsAdapterException.CicsCondition if a {@code CicsConditionException} is thrown
     */
    public void createChannelAndPutContainer(String channelName, String containerName,
            byte[] commareaBytes) throws CicsAdapterException.CicsCondition {
        try {
            Channel channel = Task.getTask().createChannel(channelName);
            channel.createContainer(containerName).put(commareaBytes);
        } catch (CicsConditionException e) {
            throw new CicsAdapterException.CicsCondition(e);
        }
    }

    /**
     * Launches an asynchronous child transaction on the named channel and stores the token.
     *
     * <p>Encapsulates CRECUST.cbl lines 687–726:
     * <pre>
     *   EXEC CICS GET CHANNEL(channelName) END-EXEC
     *   EXEC CICS RUN TRANSID(transId) CHANNEL(channel) CHILD(token) END-EXEC
     * </pre>
     * Java:
     * <pre>
     *   Channel channel = Task.getTask().getChannel(channelName);
     *   AsyncServiceImpl asyncService = new AsyncServiceImpl();
     *   Future&lt;ChildResponse&gt; token = asyncService.runTransactionId(transId, channel);
     * </pre>
     *
     * <p>The returned token index is the position of this token in the thread-local issued list
     * (0-based), mirroring the COBOL WS-ANY-CHILD-TKN array index.
     *
     * @param transId     one of OCR1–OCR5
     * @param channelName the CICS channel name
     * @return the token index (0-based) in the issued-token list
     * @throws CicsAdapterException.CicsCondition if a {@code CicsConditionException} is thrown
     */
    public int runTransactionId(String transId, String channelName)
            throws CicsAdapterException.CicsCondition {
        try {
            Channel channel = Task.getTask().getChannel(channelName);
            AsyncServiceImpl asyncService = new AsyncServiceImpl();
            Future<ChildResponse> token = asyncService.runTransactionId(transId, channel);
            List<Future<ChildResponse>> issued = ISSUED_TOKENS.get();
            int index = issued.size();
            issued.add(token);
            return index;
        } catch (CicsConditionException e) {
            throw new CicsAdapterException.CicsCondition(e);
        }
    }

    /**
     * Clears the thread-local issued-token list.
     *
     * <p>Must be called before the first {@link #runTransactionId} call in a new request
     * to ensure the list is empty. Safe to call even if no tokens were issued.
     */
    public void clearIssuedTokens() {
        ISSUED_TOKENS.get().clear();
        LAST_CHILD_RESPONSE.set(null);
    }

    /**
     * Invokes {@code AsyncServiceImpl.getAny(BlockingAction.NOSUSPEND)} and returns a
     * plain-Java {@link ChildResult} describing the outcome.
     *
     * <p>Encapsulates CRECUST.cbl lines 749–756:
     * <pre>
     *   EXEC CICS FETCH ANY NOSUSPEND END-EXEC
     * </pre>
     * Java: {@code childResponse = asyncService.getAny(BlockingAction.NOSUSPEND);}
     *
     * <p>The {@code ChildResponse} is stored in a thread-local for subsequent use by
     * {@link #getContainerBytes(int, String)}.
     *
     * <p>Vendor exceptions are mapped to named {@link CicsAdapterException} subclasses:
     * <ul>
     *   <li>{@code NotFinishedException}   → {@link CicsAdapterException.NotFinished}</li>
     *   <li>{@code InvalidRequestException} → {@link CicsAdapterException.InvReq}</li>
     *   <li>{@code NotFoundException}       → {@link CicsAdapterException.NotFound}</li>
     * </ul>
     *
     * @return a {@link ChildResult} with the completion status and matching token index
     * @throws CicsAdapterException.NotFinished if {@code DFHRESP(NOTFINISHED)} is raised
     * @throws CicsAdapterException.InvReq      if {@code DFHRESP(INVREQ)} is raised
     * @throws CicsAdapterException.NotFound    if {@code DFHRESP(NOTFND)} is raised
     */
    public ChildResult fetchAnyNosuspend()
            throws CicsAdapterException.NotFinished,
                   CicsAdapterException.InvReq,
                   CicsAdapterException.NotFound {
        AsyncServiceImpl asyncService = new AsyncServiceImpl();
        ChildResponse childResponse;
        try {
            childResponse = asyncService.getAny(BlockingAction.NOSUSPEND);
        } catch (NotFinishedException e) {
            throw new CicsAdapterException.NotFinished(e);
        } catch (InvalidRequestException e) {
            throw new CicsAdapterException.InvReq(e);
        } catch (NotFoundException e) {
            throw new CicsAdapterException.NotFound(e);
        }

        // Cache the raw response for use by getContainerBytes
        LAST_CHILD_RESPONSE.set(childResponse);

        // Determine completion status as plain String
        String statusStr;
        ChildResponse.CompletionStatus compStatus = childResponse.getCompletionStatus();
        if (compStatus == ChildResponse.CompletionStatus.NORMAL) {
            statusStr = ChildResult.STATUS_NORMAL;
        } else if (compStatus == ChildResponse.CompletionStatus.ABEND) {
            statusStr = ChildResult.STATUS_ABEND;
        } else if (compStatus == ChildResponse.CompletionStatus.SECERROR) {
            statusStr = ChildResult.STATUS_SECERROR;
        } else {
            statusStr = ChildResult.STATUS_OTHER;
        }

        // Resolve token index by matching the response against issued tokens
        List<Future<ChildResponse>> issued = ISSUED_TOKENS.get();
        int tokenIndex = -1;
        for (int i = 0; i < issued.size(); i++) {
            Future<ChildResponse> future = issued.get(i);
            if (future.isDone()) {
                try {
                    if (childResponse.equals(future.get())) {
                        tokenIndex = i;
                        break;
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException ee) {
                    // future completed with exception — skip
                }
            }
        }

        return new ChildResult(statusStr, tokenIndex);
    }

    /**
     * Retrieves the container bytes from the channel of the most recently fetched child response.
     *
     * <p>Encapsulates CRECUST.cbl lines 1010–1016:
     * <pre>
     *   EXEC CICS GET CONTAINER(containerName) CHANNEL(channel) INTO(wsChildData) END-EXEC
     * </pre>
     * Java: {@code containerBytes = channel.getContainer(containerName).get();}
     *
     * <p>The {@code Channel} is obtained from the {@code ChildResponse} cached by the most
     * recent call to {@link #fetchAnyNosuspend()}.
     *
     * @param tokenIndex    the token index returned by {@link #fetchAnyNosuspend()} (unused
     *                      directly; the channel is sourced from the cached response)
     * @param containerName the container name (CIPA–CIPE)
     * @return the raw container bytes
     * @throws CicsAdapterException.CicsCondition if a {@code CicsConditionException} is thrown
     */
    public byte[] getContainerBytes(int tokenIndex, String containerName)
            throws CicsAdapterException.CicsCondition {
        ChildResponse childResponse = LAST_CHILD_RESPONSE.get();
        Channel channel = childResponse.getChannel();
        try {
            return channel.getContainer(containerName).get();
        } catch (CicsConditionException e) {
            throw new CicsAdapterException.CicsCondition(e);
        }
    }

    // -------------------------------------------------------------------------
    // CICS ENQ/DEQ adapter methods — CustomerNumberService (2 statements)
    // -------------------------------------------------------------------------

    /**
     * Acquires a CICS ENQ on the named resource.
     *
     * <p>Encapsulates {@code CustomerNumberService} line 113:
     * <pre>
     *   nameResource.enqueue();
     * </pre>
     *
     * @param resourceName the 16-byte ENQ resource name
     * @throws CicsAdapterException.CicsCondition if {@code CicsConditionException} is thrown
     */
    public void enqueue(String resourceName) throws CicsAdapterException.CicsCondition {
        NameResource nameResource = new NameResource();
        nameResource.setName(resourceName);
        try {
            nameResource.enqueue();
        } catch (CicsConditionException e) {
            throw new CicsAdapterException.CicsCondition(e);
        }
    }

    /**
     * Releases a CICS DEQ on the named resource.
     *
     * <p>Encapsulates {@code CustomerNumberService} line 312:
     * <pre>
     *   nameResource.dequeue();
     * </pre>
     *
     * @param resourceName the 16-byte DEQ resource name
     * @throws CicsAdapterException.CicsCondition if {@code CicsConditionException} is thrown
     */
    public void dequeue(String resourceName) throws CicsAdapterException.CicsCondition {
        NameResource nameResource = new NameResource();
        nameResource.setName(resourceName);
        try {
            nameResource.dequeue();
        } catch (CicsConditionException e) {
            throw new CicsAdapterException.CicsCondition(e);
        }
    }
}
