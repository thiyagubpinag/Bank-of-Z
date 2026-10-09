package com.ibm.cics.botz.crecust.service;

import com.ibm.cics.botz.crecust.exception.CrecustException;
import com.ibm.cics.botz.crecust.model.CrecustCommarea;
import com.ibm.cics.botz.crecust.model.WsChildData;
import com.ibm.cics.botz.crecust.serializer.CrecustareaSerializer;
import com.ibm.cics.botz.crecust.serializer.WsChildDataSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Performs the asynchronous credit check using CICS async/channel APIs.
 *
 * <p>Implements the {@code CREDIT-CHECK SECTION} (CC010, CRECUST.cbl lines 604–1131).
 * Stateless — no per-request state held in instance fields (NFR-5.1).
 *
 * @see "CRECUST.cbl CREDIT-CHECK SECTION (lines 604–)"
 */
public class CreditCheckService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreditCheckService.class);

    /** Adapter singleton — sole owner of all CICS middleware dependencies. */
    private final AbndprocDelegate abndprocDelegate = AbndprocDelegate.getInstance();

    // -------------------------------------------------------------------------
    // Named constants (Rule 8 / NFR-2.4)
    // -------------------------------------------------------------------------

    /** CICS channel name — WS-CHANNEL-NAME PIC X(16) VALUE 'CIPCREDCHANN    '. */
    private static final String CIPCREDCHANN = "CIPCREDCHANN    ";

    /** Container name for credit-check agency 1 (WS-PUT-CONT-NAME WHEN 1). */
    private static final String CIPA = "CIPA";

    /** Container name for credit-check agency 2 (WS-PUT-CONT-NAME WHEN 2). */
    private static final String CIPB = "CIPB";

    /** Container name for credit-check agency 3 (WS-PUT-CONT-NAME WHEN 3). */
    private static final String CIPC = "CIPC";

    /** Container name for credit-check agency 4 (WS-PUT-CONT-NAME WHEN 4). */
    private static final String CIPD = "CIPD";

    /** Container name for credit-check agency 5 (WS-PUT-CONT-NAME WHEN 5). */
    private static final String CIPE = "CIPE";

    /** Number of credit-check agencies (WS-CC-CNT loop bound = 5). */
    private static final int AGENCY_COUNT = 5;

    /** Transaction ID for credit-check agency 1 (WS-RUN-TRANSID = 'OCR1'). */
    private static final String OCR1 = "OCR1";

    /** Transaction ID for credit-check agency 2 (WS-RUN-TRANSID = 'OCR2'). */
    private static final String OCR2 = "OCR2";

    /** Transaction ID for credit-check agency 3 (WS-RUN-TRANSID = 'OCR3'). */
    private static final String OCR3 = "OCR3";

    /** Transaction ID for credit-check agency 4 (WS-RUN-TRANSID = 'OCR4'). */
    private static final String OCR4 = "OCR4";

    /** Transaction ID for credit-check agency 5 (WS-RUN-TRANSID = 'OCR5'). */
    private static final String OCR5 = "OCR5";

    /** Transaction IDs for child credit-check transactions OCR1–OCR5. */
    private static final String[] OCR_TRANSIDS = {OCR1, OCR2, OCR3, OCR4, OCR5};

    /** Container names indexed 0–4 (parallel to OCR_TRANSIDS). */
    private static final String[] CONTAINER_NAMES = {CIPA, CIPB, CIPC, CIPD, CIPE};

    /** Delay in milliseconds before first FETCH ANY (EXEC CICS DELAY FOR SECONDS(3)). */
    private static final int DELAY_MILLIS = 3000;

    /** Exclusive upper bound for review-date random offset (BR-3, FR-6). */
    private static final int REVIEW_DATE_MAX_DAYS = 21;

    // -------------------------------------------------------------------------
    // Fail-code constant (PE-8, epic-5-context.md)
    // -------------------------------------------------------------------------

    /** COMM-SUCCESS 'N' — failure sentinel. */
    private static final String COMM_SUCCESS_NO = "N";

    // -------------------------------------------------------------------------
    // Public entry point
    // -------------------------------------------------------------------------

    /**
     * Executes the asynchronous credit check loop (OCR1–OCR5 child transactions).
     *
     * <p>Implements CREDIT-CHECK SECTION / CC010 (CRECUST.cbl lines 604–1131).
     * On success: sets {@code commArea.commCreditScore} and {@code commArea.commCsReview*} fields.
     * On any failure: sets {@code commArea.commSuccess='N'} and {@code commArea.commFailCode} to
     * the appropriate code and returns immediately (silent-return paths per Rule 1 / NFR-3.2).
     *
     * @param commArea the commarea; credit-score output fields are populated on success
     */
    public void performCreditCheck(CrecustCommarea commArea) {
        LOGGER.info("performCreditCheck entry: sortcode={}", commArea.getCommSortcode());

        for (int i = 0; i < AGENCY_COUNT; i++) {
            String containerName = CONTAINER_NAMES[i];
            LOGGER.debug("performCreditCheck: putContainer channel={} container={}",
                    CIPCREDCHANN.trim(), containerName);
            putContainer(commArea, CIPCREDCHANN, containerName);
            if (COMM_SUCCESS_NO.equals(commArea.getCommSuccess())) {
                return;
            }
        }

        // Launch all five OCRn child transactions asynchronously (CRECUST.cbl lines 687–726)
        abndprocDelegate.clearIssuedTokens();
        List<Integer> tokens = new ArrayList<>();
        for (String transId : OCR_TRANSIDS) {
            Integer token = runChildTransaction(transId, CIPCREDCHANN, commArea);
            if (token == null) {
                return; // fail-code already set in runChildTransaction
            }
            tokens.add(token);
        }

        // EXEC CICS DELAY FOR SECONDS(3) — called exactly once after all five runs (ADR-5)
        delayForResults();

        // Story 5-3: collect results; return value used by Story 5-4 for review-date logic
        int wsRetrievedCnt = fetchAny(commArea, tokens);
        if (COMM_SUCCESS_NO.equals(commArea.getCommSuccess())) {
            // FETCH-path failures (C–H) set COMM-CS-REVIEW-DATE to today (WS-ORIG-DATE)
            computeReviewDate(commArea, 0L, 0);
            return;
        }

        // MOVE EIBTASKN TO WS-SEED (CRECUST.cbl lines 833 / 940)
        long eibtaskn = abndprocDelegate.getTaskNumber();
        computeReviewDate(commArea, eibtaskn, wsRetrievedCnt);
    }

    // -------------------------------------------------------------------------
    // Story 5-1: putContainer
    // -------------------------------------------------------------------------

    /**
     * Serializes {@code commArea} and PUTs it into the named CICS container on {@code channelName}.
     *
     * <p>Maps {@code EXEC CICS PUT CONTAINER(WS-PUT-CONT-NAME) FROM(DFHCOMMAREA)
     * FLENGTH(WS-PUT-CONT-LEN) CHANNEL(WS-CHANNEL-NAME)} (CRECUST.cbl lines 656–682).
     * On CICS error: sets {@code commArea.commSuccess='N'}, {@code commArea.commFailCode='A'}
     * and returns (silent-return path, Rule 1).
     *
     * @param commArea      the commarea to serialize and PUT
     * @param channelName   CICS channel name (use constant {@link #CIPCREDCHANN})
     * @param containerName container name (one of {@link #CIPA}–{@link #CIPE})
     */
    void putContainer(CrecustCommarea commArea, String channelName, String containerName) {
        byte[] commareaBytes = CrecustareaSerializer.INSTANCE.toBytes(commArea);
        try {
            abndprocDelegate.createChannelAndPutContainer(channelName, containerName, commareaBytes);
        } catch (CicsAdapterException.CicsCondition e) {
            LOGGER.error("putContainer failed: channel={} container={} error={}",
                    channelName, containerName, e.getMessage());
            commArea.setCommSuccess(COMM_SUCCESS_NO);
            commArea.setCommFailCode(CrecustException.FAIL_CODE_PUT_CONTAINER);
        }
    }

    // -------------------------------------------------------------------------
    // Story 5-2: runChildTransaction
    // -------------------------------------------------------------------------

    /**
     * Launches an asynchronous OCRn child transaction on the named channel.
     *
     * <p>Maps {@code EXEC CICS RUN TRANSID(WS-RUN-TRANSID) CHANNEL(WS-CHANNEL-NAME)
     * CHILD(WS-ANY-CHILD-TKN)} (CRECUST.cbl lines 687–711).
     * On CICS error: sets {@code commArea.commSuccess='N'}, {@code commArea.commFailCode='B'}
     * via {@code CrecustException.runTransidError()} and returns {@code null}
     * (silent-return path, Rule 1 / epic-5-context fail-code 'B').
     *
     * @param transId     one of OCR1–OCR5
     * @param channelName the CICS channel name (use constant {@link #CIPCREDCHANN})
     * @param commArea    the commarea (fail-code set on error)
     * @return the child token index or {@code null} on failure
     */
    private Integer runChildTransaction(
            String transId, String channelName, CrecustCommarea commArea) {
        try {
            int token = abndprocDelegate.runTransactionId(transId, channelName);
            LOGGER.debug("runChildTransaction: launched transId={} channel={}", transId, channelName);
            return token;
        } catch (CicsAdapterException.CicsCondition e) {
            LOGGER.error("runChildTransaction failed: transId={} channel={} error={}",
                    transId, channelName, e.getMessage());
            CrecustException ex = CrecustException.runTransidError(e);
            commArea.setCommSuccess(ex.getCommSuccess());
            commArea.setCommFailCode(ex.getCommFailCode());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Story 5-2: delayForResults
    // -------------------------------------------------------------------------

    /**
     * Delays execution before FETCH ANY result collection.
     *
     * <p>Maps {@code EXEC CICS DELAY FOR SECONDS(3)} (CRECUST.cbl lines 733–735).
     * {@code Task.getTask().delay()} does NOT exist in JCICS; uses {@link Thread#sleep} instead.
     * On {@code InterruptedException}: restores interrupt flag and continues — the credit check
     * is not failed for an interrupt.
     */
    private void delayForResults() {
        try {
            Thread.sleep(DELAY_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.debug("delayForResults: sleep interrupted; continuing");
        }
    }

    // -------------------------------------------------------------------------
    // Story 5-3: fetchAny
    // -------------------------------------------------------------------------

    /** Canonical serializer for WS-CHILD-DATA (ADR-1 / Rule 15). */
    private static final WsChildDataSerializer wsChildDataSerializer = WsChildDataSerializer.INSTANCE;

    /**
     * Collects results from child transactions via FETCH ANY NOSUSPEND and aggregates scores.
     *
     * <p>Implements the FETCH ANY loop in {@code CREDIT-CHECK_CC010} (CRECUST.cbl lines 741–1131).
     * Calls {@code AbndprocDelegate.fetchAnyNosuspend()} until all issued children
     * are collected or a terminal error occurs. Container bytes are deserialized exclusively via
     * {@link WsChildDataSerializer#fromBytes(byte[], int)} (Rule 15 / ADR-1).
     *
     * <p>Error paths are silent-return: sets {@code commArea.commSuccess='N'} and the appropriate
     * {@code commArea.commFailCode} then returns immediately (Rule 1 / NFR-3.2).
     *
     * <p>After the loop: if {@code wsRetrievedCnt > 0}, sets
     * {@code commArea.commCreditScore = wsTotalCsScr / wsRetrievedCnt}.
     *
     * @param commArea the commarea; {@code commCreditScore} is set on success
     * @param tokens   the child token indices returned by {@link #runChildTransaction}, in OCR1–OCR5 order
     * @return the number of children whose containers were successfully retrieved and deserialized
     */
    int fetchAny(CrecustCommarea commArea, List<Integer> tokens) {
        int wsRetrievedCnt = 0;
        int wsTotalCsScr = 0;
        int wsChildReceivedCnt = 0;
        boolean wsFinishedFetching = false;
        int childIssuedCnt = tokens.size();

        // PERFORM UNTIL WS-FINISHED-FETCHING = 'Y' (CRECUST.cbl line 741)
        while (!wsFinishedFetching) {

            ChildResult childResult;
            try {
                // EXEC CICS FETCH ANY NOSUSPEND (CRECUST.cbl lines 749–756)
                childResult = abndprocDelegate.fetchAnyNosuspend();
            } catch (CicsAdapterException.NotFinished e) {
                // DFHRESP(NOTFINISHED) RESP2=52 (CRECUST.cbl lines 767–856)
                if (wsRetrievedCnt == 0) {
                    // No children replied at all — fatal (CRECUST.cbl lines 774–796)
                    LOGGER.error("fetchAny: NOTFINISHED with zero retrieved — fail-code C");
                    CrecustException ex = CrecustException.ccNotFinished();
                    commArea.setCommSuccess(ex.getCommSuccess());
                    commArea.setCommFailCode(ex.getCommFailCode());
                    return 0;
                }
                // Some children replied — compute score and exit (CRECUST.cbl lines 806–855)
                LOGGER.debug("fetchAny: NOTFINISHED with {} retrieved; computing average", wsRetrievedCnt);
                wsFinishedFetching = true;
                continue;
            } catch (CicsAdapterException.InvReq e) {
                // DFHRESP(INVREQ) RESP2=1 — parent never had any children (CRECUST.cbl lines 861–884)
                LOGGER.error("fetchAny: INVREQ (no children) — fail-code D");
                CrecustException ex = CrecustException.ccInvreq(e);
                commArea.setCommSuccess(ex.getCommSuccess());
                commArea.setCommFailCode(ex.getCommFailCode());
                return 0;
            } catch (CicsAdapterException.NotFound e) {
                // DFHRESP(NOTFND) RESP2=1 — no more available responses (CRECUST.cbl lines 889–963)
                // Exit the loop cleanly; score computed below if wsRetrievedCnt > 0
                LOGGER.debug("fetchAny: NOTFND — no more responses; retrievedCnt={}", wsRetrievedCnt);
                wsFinishedFetching = true;
                continue;
            }

            // Evaluate WS-CHILD-FETCH-COMPST (CRECUST.cbl lines 970–1127)
            String compStatus = childResult.getCompletionStatus();

            if (ChildResult.STATUS_NORMAL.equals(compStatus)) {
                // Determine container name from token index (mirrors WS-CHILD-TKN(n) lookup)
                String containerName = resolveContainerName(childResult);

                byte[] containerBytes;
                try {
                    // EXEC CICS GET CONTAINER (CRECUST.cbl lines 1010–1016)
                    containerBytes = abndprocDelegate.getContainerBytes(
                            childResult.getTokenIndex(), containerName);
                } catch (CicsAdapterException.CicsCondition e) {
                    // GET CONTAINER failed — fail-code 'E' (CRECUST.cbl lines 1018–1044)
                    LOGGER.error("fetchAny: GET CONTAINER failed: container={} error={}",
                            containerName, e.getMessage());
                    CrecustException ex = CrecustException.getContainerError(e);
                    commArea.setCommSuccess(ex.getCommSuccess());
                    commArea.setCommFailCode(ex.getCommFailCode());
                    return wsRetrievedCnt;
                }

                // Deserialize via canonical serializer only (Rule 15 / ADR-1)
                WsChildData wsChildData = wsChildDataSerializer.fromBytes(containerBytes, 0);

                // COMPUTE WS-RETRIEVED-CNT = WS-RETRIEVED-CNT + 1 (CRECUST.cbl line 1049–1050)
                wsRetrievedCnt++;
                // COMPUTE WS-TOTAL-CS-SCR = WS-TOTAL-CS-SCR + WS-CHILD-CREDIT-SCORE (CRECUST.cbl lines 1051–1053)
                wsTotalCsScr += parseScore(wsChildData.getCustomerCreditScore());
                LOGGER.debug("fetchAny: NORMAL result: container={} score={} retrievedCnt={}",
                        containerName, wsChildData.getCustomerCreditScore(), wsRetrievedCnt);

            } else if (ChildResult.STATUS_ABEND.equals(compStatus)) {
                // WHEN DFHVALUE(ABEND) (CRECUST.cbl lines 1055–1072)
                LOGGER.error("fetchAny: child ABEND completion — fail-code F");
                CrecustException ex = CrecustException.ccAbend();
                commArea.setCommSuccess(ex.getCommSuccess());
                commArea.setCommFailCode(ex.getCommFailCode());
                return wsRetrievedCnt;

            } else if (ChildResult.STATUS_SECERROR.equals(compStatus)) {
                // WHEN DFHVALUE(SECERROR) (CRECUST.cbl lines 1074–1098)
                LOGGER.error("fetchAny: child SECERROR completion — fail-code G");
                CrecustException ex = CrecustException.ccSecError();
                commArea.setCommSuccess(ex.getCommSuccess());
                commArea.setCommFailCode(ex.getCommFailCode());
                return wsRetrievedCnt;

            } else {
                // WHEN OTHER (CRECUST.cbl lines 1101–1126)
                LOGGER.error("fetchAny: child OTHER completion status — fail-code H");
                CrecustException ex = CrecustException.ccOther();
                commArea.setCommSuccess(ex.getCommSuccess());
                commArea.setCommFailCode(ex.getCommFailCode());
                return wsRetrievedCnt;
            }

            // Increment received counter; terminate loop when all children accounted for
            wsChildReceivedCnt++;
            if (wsChildReceivedCnt >= childIssuedCnt) {
                wsFinishedFetching = true;
            }
        }

        // COMPUTE WS-ACTUAL-CS-SCR = WS-TOTAL-CS-SCR / WS-RETRIEVED-CNT (CRECUST.cbl line 812–813)
        if (wsRetrievedCnt > 0) {
            int wsActualCsScr = wsTotalCsScr / wsRetrievedCnt;
            commArea.setCommCreditScore(String.format("%03d", wsActualCsScr));
            LOGGER.info("fetchAny: credit score computed: total={} count={} avg={}",
                    wsTotalCsScr, wsRetrievedCnt, wsActualCsScr);
        }
        return wsRetrievedCnt;
    }

    /**
     * Resolves the container name for a NORMAL child response from its token index
     * (mirrors EVALUATE WS-ANY-CHILD-FETCH-TKN, CRECUST.cbl lines 976–1005).
     *
     * <p>Uses the token index carried by {@link ChildResult#getTokenIndex()} to look up the
     * corresponding entry in {@link #CONTAINER_NAMES}. Falls back to {@link #CIPA} if the
     * index is out of bounds (defensive).
     *
     * @param childResult the result returned by {@link AbndprocDelegate#fetchAnyNosuspend()}
     * @return the container name (CIPA–CIPE) for this response
     */
    private String resolveContainerName(ChildResult childResult) {
        int idx = childResult.getTokenIndex();
        if (idx >= 0 && idx < CONTAINER_NAMES.length) {
            return CONTAINER_NAMES[idx];
        }
        // Defensive fallback: return CIPA (first container)
        LOGGER.warn("resolveContainerName: no matching token found (index={}); defaulting to {}", idx, CIPA);
        return CIPA;
    }

    /**
     * Parses WS-CHILD-CREDIT-SCORE (PIC 999, 3-byte display string) to an int.
     *
     * @param score the credit score string from {@link WsChildData#getCustomerCreditScore()}
     * @return the parsed integer score, or 0 if the string is blank or unparseable
     */
    private static int parseScore(String score) {
        if (score == null || score.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(score.trim());
        } catch (NumberFormatException e) {
            LOGGER.warn("parseScore: unparseable credit score '{}'; treating as 0", score);
            return 0;
        }
    }

    // -------------------------------------------------------------------------
    // Story 5-4: computeReviewDate
    // -------------------------------------------------------------------------

    /**
     * Computes and sets the credit-score review date on the commarea (FR-6).
     *
     * <p>Success path ({@code wsRetrievedCnt > 0}, CRECUST.cbl lines 819–853 / 927–960):
     * {@code WS-REVIEW-DATE-ADD = ((21 - 1) * RANDOM(EIBTASKN)) + 1}, then
     * today + offset via {@code INTEGER-OF-DATE}/{@code DATE-OF-INTEGER} → {@link LocalDate#plusDays}.
     * Failure path ({@code wsRetrievedCnt == 0}): review date = today
     * ({@code STRING WS-ORIG-DATE-DD WS-ORIG-DATE-MM WS-ORIG-DATE-YYYY INTO COMM-CS-REVIEW-DATE}).
     * DDMMYYYY byte layout is owned by {@code CrecustareaSerializer} (offsets 389–396, TRA-5).
     *
     * @param commArea       the commarea (review-date fields set)
     * @param eibtaskn       the CICS task number (EIBTASKN) used as the random seed
     * @param wsRetrievedCnt number of child credit-check responses retrieved (WS-RETRIEVED-CNT)
     */
    void computeReviewDate(CrecustCommarea commArea, long eibtaskn, int wsRetrievedCnt) {
        LocalDate today = LocalDate.now();
        LocalDate reviewDate;
        if (wsRetrievedCnt > 0) {
            int daysOffset = (int) ((REVIEW_DATE_MAX_DAYS - 1) * randomFromTask(eibtaskn)) + 1;
            reviewDate = today.plusDays(daysOffset);
        } else {
            reviewDate = today;
        }
        commArea.setCommCsReviewDay(String.format("%02d", reviewDate.getDayOfMonth()));
        commArea.setCommCsReviewMonth(String.format("%02d", reviewDate.getMonthValue()));
        commArea.setCommCsReviewYear(String.format("%04d", reviewDate.getYear()));
        LOGGER.debug("computeReviewDate: retrievedCnt={} reviewDate={}", wsRetrievedCnt, reviewDate);
    }

    /**
     * Deterministic replacement for COBOL {@code FUNCTION RANDOM(WS-SEED)} seeded from EIBTASKN.
     *
     * @param taskNumber the CICS task number
     * @return a value in [0.0, 1.0)
     */
    private double randomFromTask(long taskNumber) {
        return (taskNumber % 100) / 100.0;
    }
}
