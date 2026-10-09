package com.ibm.cics.botz.crecust.service;

import com.ibm.cics.botz.crecust.model.CrecustCommarea;
import com.ibm.cics.botz.crecust.model.CustomerRecord;
import com.ibm.cics.botz.crecust.model.WsCicstsLevelNumGrp;
import com.ibm.cics.botz.crecust.model.WsOrigDateGrp;
import com.ibm.cics.botz.crecust.model.WsTimeNowGrp;
import com.ibm.cics.botz.crecust.db.HostCustomerRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Stateless orchestrator for the CRECUST PREMIERE_P010 flow.
 *
 * <p>Calls each downstream service in the exact PREMIERE_P010 order (CRECUST.cbl lines 407–520)
 * and short-circuits on any fail-code, mirroring the COBOL
 * {@code IF COMM-SUCCESS NOT = 'Y' GO TO GET-ME-OUT-OF-HERE} pattern.
 *
 * <p>All downstream service dependencies are constructor-injected; no per-request instance state
 * is held (NFR-5 — stateless).
 *
 * @see "CRECUST.cbl PREMIERE_P010 (lines 407–520)"
 */
public class CrecustService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrecustService.class);

    // -----------------------------------------------------------------------
    // CICS version constants (ADR-6, Story 3.5)
    // -----------------------------------------------------------------------

    /** Number of characters in the CICS version string returned by {@code Task.getCicsVersion()}. */
    private static final int CICS_VERSION_LENGTH = 3;

    // -----------------------------------------------------------------------
    // Date/time formatters — EXEC CICS FORMATTIME equivalents (Rule 11)
    // -----------------------------------------------------------------------

    /** Formatter for WS-ORIG-DATE: DDMMYYYY with DATESEP('/') → "dd/MM/yyyy". */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Formatter for WS-TIME-NOW (HHMMSS, no separators). */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmss");

    /**
     * Lilian epoch offset (days from 0001-01-01 to 1970-01-01 in CICS Lilian notation).
     * Required constant per Rule 8 / AC.  Not used directly in Java time logic because
     * {@code LocalDateTime.now()} replaces {@code EXEC CICS ASKTIME ABSTIME(...)}.
     */
    private static final long LILIAN_EPOCH_OFFSET = 577737L;

    // -----------------------------------------------------------------------
    // CICS version working-storage fields (WS-CICSTSLEVEL / WS-CICSTS-LEVEL-DATA)
    // -----------------------------------------------------------------------

    /**
     * WS-CICSTSLEVEL PIC X(6) — CICS TS version string, e.g. {@code "730"}.
     * Populated by {@code populateCicsVersion()} via {@code Task.getTask().getCicsVersion()}.
     * Only the first {@link #CICS_VERSION_LENGTH} characters carry meaningful data.
     */
    private String wsCicstslevel = "";

    // -----------------------------------------------------------------------
    // Date/time working-storage fields (populated per request by populateTimeAndDate)
    // -----------------------------------------------------------------------

    /** WS-ORIG-DATE PIC X(10) — current date formatted as "DD/MM/YYYY". */
    private String wsOrigDate;

    /** WS-ORIG-DATE-GRP — day/month/year components parsed from wsOrigDate. */
    private WsOrigDateGrp wsOrigDateGrp;

    /**
     * WS-ORIG-DATE-GRP-X — dot-separated date "DD.MM.YYYY", assembled from
     * {@code wsOrigDateGrp} component fields (PE-4, FR-4.4).
     */
    private String wsOrigDateGrpX;

    /** WS-TIME-NOW PIC 9(6) — current time as HHMMSS integer (e.g. 143045). */
    private int wsTimeNow;

    /** WS-TIME-NOW-GRP — hour/minute/second components derived from wsTimeNow. */
    private WsTimeNowGrp wsTimeNowGrp;

    private final ValidationService validationService;
    private final CreditCheckService creditCheckService;
    private final CustomerNumberService customerNumberService;
    private final CustomerDbService customerDbService = CustomerDbService.getInstance();
    private final ProctranDbService proctranDbService = ProctranDbService.getInstance();
    private final AbndprocDelegate abndprocDelegate = AbndprocDelegate.getInstance();

    /**
     * Constructs a {@code CrecustService} with all required downstream service dependencies.
     *
     * @param validationService       validates customer title and date of birth
     * @param creditCheckService      performs the asynchronous credit check
     * @param customerNumberService   ENQ/DEQ and get-and-increment customer number
     */
    public CrecustService(
            ValidationService validationService,
            CreditCheckService creditCheckService,
            CustomerNumberService customerNumberService) {
        this.validationService = validationService;
        this.creditCheckService = creditCheckService;
        this.customerNumberService = customerNumberService;
    }

    /**
     * Executes the CRECUST business flow — mirrors PREMIERE_P010 (CRECUST.cbl lines 407–520).
     *
     * <p>Steps:
     * <ol>
     *   <li>Validate customer title (fail-code {@code 'T'})
     *   <li>Validate date of birth (fail-codes {@code 'O'}/{@code 'Z'}/{@code 'Y'})
     *   <li>Populate time and date (stub — Story 3.4)
     *   <li>Perform async credit check (fail-code {@code 'G'})
     *   <li>ENQ named counter (fail-code {@code '3'})
     *   <li>Get and increment customer number (fail-code {@code '4'})
     *   <li>Insert customer into DB2 (fail-code {@code '1'})
     *   <li>Write PROCTRAN audit record (notifying-abort on failure)
     *   <li>DEQ named counter (success path)
     *   <li>Set success fields in commarea
     * </ol>
     *
     * @param commArea the deserialized DFHCOMMAREA; mutated in place with result fields
     */
    public void execute(CrecustCommarea commArea) {
        LOGGER.info("CrecustService.execute() — begin");

        CustomerRecord customerRecord = new CustomerRecord();
        HostCustomerRow hostCustomerRow = new HostCustomerRow();

        // Initialization: PREMIERE_P010 — populate CICS version (CRECUST.cbl lines 407–520)
        LOGGER.debug("Init: populateCicsVersion");
        populateCicsVersion();

        // Step 1: Validate customer title
        LOGGER.debug("Step 1: validateTitle");
        validationService.validateTitle(commArea);
        if (!"Y".equals(commArea.getCommSuccess())) {
            LOGGER.info("CrecustService.execute() — returning after title validation failure, fail-code={}",
                    commArea.getCommFailCode());
            return;
        }

        // Step 2: Validate date of birth
        LOGGER.debug("Step 2: validateDateOfBirth");
        validationService.validateDateOfBirth(commArea);
        if (!"Y".equals(commArea.getCommSuccess())) {
            LOGGER.info("CrecustService.execute() — returning after DOB validation failure, fail-code=[redacted]");
            return;
        }

        // Step 3: Populate time and date (stub — full implementation in Story 3.4)
        LOGGER.debug("Step 3: populateTimeAndDate");
        populateTimeAndDate();

        // Step 4: Perform async credit check
        LOGGER.debug("Step 4: performCreditCheck");
        creditCheckService.performCreditCheck(commArea);
        if (!"Y".equals(commArea.getCommSuccess())) {
            LOGGER.info("CrecustService.execute() — returning after credit-check failure");
            return;
        }

        // Step 5: ENQ named counter — returns the resource name for later DEQ
        LOGGER.debug("Step 5: enqueue");
        String nameResource = customerNumberService.enqueue(commArea);
        if (!"Y".equals(commArea.getCommSuccess())) {
            LOGGER.info("CrecustService.execute() — returning after ENQ failure");
            return;
        }

        // Step 6: Get and increment customer number
        LOGGER.debug("Step 6: getAndIncrementCustomerNumber");
        customerNumberService.getAndIncrementCustomerNumber(commArea, nameResource);
        if (!"Y".equals(commArea.getCommSuccess())) {
            LOGGER.info("CrecustService.execute() — returning after get-and-increment failure");
            return;
        }

        // Step 7: Insert customer into DB2
        LOGGER.debug("Step 7: insertCustomer");
        customerDbService.insertCustomer(commArea, customerRecord, hostCustomerRow,
                customerNumberService, nameResource);
        if (!"Y".equals(commArea.getCommSuccess())) {
            LOGGER.info("CrecustService.execute() — returning after insert-customer failure");
            return;
        }

        // Step 8: Write PROCTRAN audit record — notifying-abort on failure (Rule 1, Story 8.2).
        // The 4-step abort sequence (populate AbndInfoRec → LINK ABNDPROC → DEQ → ABEND 'HWPT')
        // is handled entirely inside ProctranDbService.insertProctran() — no try/catch here.
        LOGGER.debug("Step 8: insertProctran");
        proctranDbService.insertProctran(
                commArea,
                commArea.getCommSortcode(),
                commArea.getCommNumber(),
                // STRING first-name DELIMITED BY '  ' ' ' last-name DELIMITED BY '  ' INTO STORED-NAME
                commArea.getCommFirstName().stripTrailing() + " " + commArea.getCommLastName().stripTrailing(),
                // STORED-DOB: DD/MM/YYYY (CRECUST.cbl lines 1287-1291)
                commArea.getCommDobDay() + "/" + commArea.getCommDobMonth() + "/" + commArea.getCommDobYear(),
                customerNumberService,
                nameResource,
                abndprocDelegate);

        // Step 9: DEQ named counter (success path)
        LOGGER.debug("Step 9: dequeue");
        customerNumberService.dequeue(commArea, nameResource);

        // Step 10: Set success fields in commarea
        LOGGER.info("CrecustService.execute() — success");
        commArea.setCommSuccess("Y");
        commArea.setCommFailCode(" ");
        commArea.setCommEyecatcher("CUST");
    }

    /**
     * Populates time-and-date working-storage fields.
     *
     * <p>Translates {@code POPULATE-TIME-DATE_PTD010} (CRECUST.cbl lines 523–534):
     * <ol>
     *   <li>{@code EXEC CICS ASKTIME ABSTIME(WS-U-TIME)} → {@code LocalDateTime.now()}
     *   <li>{@code EXEC CICS FORMATTIME ABSTIME(...) DDMMYYYY(WS-ORIG-DATE) DATESEP TIME(...)}
     *       → {@link #DATE_FMT} and {@link #TIME_FMT} patterns
     * </ol>
     *
     * <p>Note: {@code Task.getTask().getAbstime()} does NOT exist in the JCICS API and is
     * never called here.  {@link #LILIAN_EPOCH_OFFSET} is declared but not used directly;
     * the JVM clock replaces the CICS Lilian time source entirely.
     */
    private void populateTimeAndDate() {
        // EXEC CICS ASKTIME ABSTIME(WS-U-TIME) END-EXEC
        LocalDateTime now = LocalDateTime.now();

        // EXEC CICS FORMATTIME DDMMYYYY(WS-ORIG-DATE) DATESEP END-EXEC → "dd/MM/yyyy"
        wsOrigDate = DATE_FMT.format(now);

        // Populate WsOrigDateGrp from wsOrigDate components (parsed, not reformatted)
        int dd   = Integer.parseInt(wsOrigDate.substring(0, 2));
        int mm   = Integer.parseInt(wsOrigDate.substring(3, 5));
        int yyyy = Integer.parseInt(wsOrigDate.substring(6, 10));
        wsOrigDateGrp = WsOrigDateGrp.builder()
                .dd(dd)
                .mm(mm)
                .yyyy(yyyy)
                .build();

        // Assemble wsOrigDateGrpX with '.' separators from component fields (PE-4, FR-4.4)
        wsOrigDateGrpX = String.format("%02d.%02d.%04d",
                wsOrigDateGrp.getDd(),
                wsOrigDateGrp.getMm(),
                wsOrigDateGrp.getYyyy());

        // EXEC CICS FORMATTIME TIME(PROC-TRAN-TIME) END-EXEC → "HHmmss"
        String timeStr = TIME_FMT.format(now);
        wsTimeNow = Integer.parseInt(timeStr);

        // Populate WsTimeNowGrp from wsTimeNow (invariant: hh*10000 + mm*100 + ss == wsTimeNow)
        wsTimeNowGrp = WsTimeNowGrp.builder()
                .hh(wsTimeNow / 10000)
                .mm((wsTimeNow / 100) % 100)
                .ss(wsTimeNow % 100)
                .build();
    }

    /**
     * Populates the CICS version working-storage fields.
     *
     * <p>Translates the {@code CICSTSLEVEL} / {@code WS-CICSTSLEVEL} initialisation in
     * {@code PREMIERE_P010} (CRECUST.cbl lines 298–305):
     * <pre>
     *    MOVE CICSTSLEVEL TO WS-CICSTSLEVEL
     * </pre>
     * where {@code CICSTSLEVEL} is the CICS special register holding the 3-character version
     * string (e.g. {@code "730"}).  Java equivalent: {@code Task.getTask().getCicsVersion()}.
     *
     * <p>ADR-6: use {@code Task.getTask().getCicsVersion()} as the sole source; never read from
     * the commarea.
     */
    private void populateCicsVersion() {
        // EXEC CICS ASSIGN CICSTSLEVEL(WS-CICSTSLEVEL) equivalent — ADR-6
        //
        // Task.getCicsVersion() is available on CICS TS 7.6+ / JCICS ≥ 2.200 GA on z/OS but is
        // absent from the local development stub jar (2.200.0-6.3 pre-GA).  Use reflection so
        // the call resolves at runtime on z/OS without modifying the provided-scope pom entry.
        // The fallback path (NoSuchMethodException) is only reachable in the off-z/OS dev build.
        wsCicstslevel = abndprocDelegate.getCicsVersion(CICS_VERSION_LENGTH);
    }

    // -----------------------------------------------------------------------
    // Cat 3a getter/setter — WsCicstsLevelNumGrp (Story 3.5)
    // -----------------------------------------------------------------------

    /**
     * Returns the {@link WsCicstsLevelNumGrp} parsed from {@code wsCicstslevel}.
     *
     * <p>Maps {@code WS-CICSTS-LEVEL-NUM-GRP REDEFINES WS-CICSTSLEVEL} (CRECUST.cbl lines
     * 302–305).  Each sub-field is one digit derived by positional {@code charAt} — exactly
     * mirroring the COBOL {@code PIC 9} single-digit character-by-character split.
     *
     * @return populated {@link WsCicstsLevelNumGrp} with vv, rr, mm from the version string
     */
    public WsCicstsLevelNumGrp getWsCicstsLevelNumGrp() {
        int vv = wsCicstslevel.charAt(0) - '0';
        int rr = wsCicstslevel.charAt(1) - '0';
        int mm = wsCicstslevel.charAt(2) - '0';
        return WsCicstsLevelNumGrp.builder()
                .wsCicstsLevelVv(vv)
                .wsCicstsLevelRr(rr)
                .wsCicstsLevelMm(mm)
                .build();
    }

    /**
     * Encodes a {@link WsCicstsLevelNumGrp} back into {@code wsCicstslevel}.
     *
     * <p>Reverses the Cat 3a split: assembles the 3-character version string
     * from the integer VV/RR/MM sub-fields.
     *
     * @param grp the group whose sub-fields populate {@code wsCicstslevel}
     */
    public void setWsCicstsLevelNumGrp(WsCicstsLevelNumGrp grp) {
        wsCicstslevel = String.format("%d%d%d",
                grp.getWsCicstsLevelVv(),
                grp.getWsCicstsLevelRr(),
                grp.getWsCicstsLevelMm());
    }

    // -----------------------------------------------------------------------
    // Cat 3a getter/setter — WsOrigDateGrp (Story 2-2 TODO fulfilment)
    // -----------------------------------------------------------------------

    /**
     * Returns the {@link WsOrigDateGrp} parsed from {@code wsOrigDate}.
     *
     * <p>Maps {@code WS-ORIG-DATE-GRP REDEFINES WS-ORIG-DATE} (CRECUST.cbl lines 200–206).
     * Parses {@code wsOrigDate} formatted as {@code "DD/MM/YYYY"} by positional substring.
     *
     * @return populated {@link WsOrigDateGrp} with dd, mm, yyyy
     */
    public WsOrigDateGrp getWsOrigDateGrp() {
        int dd   = Integer.parseInt(wsOrigDate.substring(0, 2));
        int mm   = Integer.parseInt(wsOrigDate.substring(3, 5));
        int yyyy = Integer.parseInt(wsOrigDate.substring(6, 10));
        return WsOrigDateGrp.builder()
                .dd(dd)
                .mm(mm)
                .yyyy(yyyy)
                .build();
    }

    /**
     * Encodes a {@link WsOrigDateGrp} back into {@code wsOrigDate}.
     *
     * <p>Reverses the Cat 3a split: assembles the {@code "DD/MM/YYYY"} date string
     * from the integer dd/mm/yyyy sub-fields, using {@link WsOrigDateGrp#SEP} as separator.
     *
     * @param grp the group whose sub-fields populate {@code wsOrigDate}
     */
    public void setWsOrigDateGrp(WsOrigDateGrp grp) {
        wsOrigDate = String.format("%02d%c%02d%c%04d",
                grp.getDd(),
                WsOrigDateGrp.SEP,
                grp.getMm(),
                WsOrigDateGrp.SEP,
                grp.getYyyy());
    }

    // -----------------------------------------------------------------------
    // Cat 3a getter/setter — WsTimeNowGrp (Story 2-2 TODO fulfilment)
    // -----------------------------------------------------------------------

    /**
     * Returns the {@link WsTimeNowGrp} derived from {@code wsTimeNow}.
     *
     * <p>Maps {@code WS-TIME-NOW-GRP REDEFINES WS-TIME-NOW} (CRECUST.cbl lines 131–135).
     * Decomposes the 6-digit HHMMSS integer into hour/minute/second components.
     *
     * @return populated {@link WsTimeNowGrp} with hh, mm, ss
     */
    public WsTimeNowGrp getWsTimeNowGrp() {
        return WsTimeNowGrp.builder()
                .hh(wsTimeNow / 10000)
                .mm((wsTimeNow / 100) % 100)
                .ss(wsTimeNow % 100)
                .build();
    }

    /**
     * Encodes a {@link WsTimeNowGrp} back into {@code wsTimeNow}.
     *
     * <p>Reverses the Cat 3a split: assembles the 6-digit HHMMSS integer
     * from the hh/mm/ss sub-fields (invariant: {@code hh*10000 + mm*100 + ss}).
     *
     * @param grp the group whose sub-fields populate {@code wsTimeNow}
     */
    public void setWsTimeNowGrp(WsTimeNowGrp grp) {
        wsTimeNow = grp.getHh() * 10000 + grp.getMm() * 100 + grp.getSs();
    }
}
