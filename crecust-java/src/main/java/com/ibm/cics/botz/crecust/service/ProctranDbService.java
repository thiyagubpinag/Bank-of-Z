package com.ibm.cics.botz.crecust.service;

import com.ibm.cics.botz.crecust.db.HostProctranRow;
import com.ibm.cics.botz.crecust.exception.CrecustException;
import com.ibm.cics.botz.crecust.model.AbndInfoRec;
import com.ibm.cics.botz.crecust.model.CrecustCommarea;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import com.ibm.cics.botz.crecust.service.adapters.DBAdapter;
import com.ibm.cics.botz.crecust.service.adapters.RESTAdapter;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes the PROCTRAN audit record to the DB2 {@code PROCTRAN} table.
 *
 * <p>Corresponds to CRECUST.cbl {@code WRITE-PROCTRAN-DB2_WPD010} (lines 1320–1460).
 *
 * @see "CRECUST.cbl WRITE-PROCTRAN-DB2 section"
 */
public class ProctranDbService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProctranDbService.class);

    private final RESTAdapter restAdapter = RESTAdapter.getInstance();

    private final DBAdapter dbAdapter = DBAdapter.getInstance();

    private final AbndprocDelegate abndprocDelegate = AbndprocDelegate.getInstance();

    /** JNDI name for the PROCTRAN DB2 DataSource (ADR-4). */
    private static final String PROCTRAN_DATASOURCE = "jdbc/crecustDB2DS";

    /** 9-column INSERT in PE-3 column order (COBOL EXEC SQL WPD010 lines 1359–1384). */
    private static final String INSERT_PROCTRAN_SQL =
            "INSERT INTO STTESTER.PROCTRAN"
            + " (PROCTRAN_EYECATCHER, PROCTRAN_SORTCODE, PROCTRAN_NUMBER,"
            + "  PROCTRAN_DATE, PROCTRAN_TIME, PROCTRAN_REF,"
            + "  PROCTRAN_TYPE, PROCTRAN_DESC, PROCTRAN_AMOUNT)"
            + " VALUES (?,?,?,?,?,?,?,?,?)";

    /** MOVE 'OCC' TO HV-PROCTRAN-TYPE (CRECUST.cbl line 1356). */
    private static final String PROCTRAN_TYPE = "OCC";

    /** MOVE 'PRTR' TO HV-PROCTRAN-EYECATCHER (CRECUST.cbl line 1328). */
    private static final String PROCTRAN_EYECATCHER = "PRTR";

    /** MOVE ZEROS TO HV-PROCTRAN-ACC-NUMBER — 8 chars of zeros (CRECUST.cbl line 1330). */
    private static final String PROCTRAN_ACC_NUMBER = "00000000";

    /** HV-PROCTRAN-DESC PIC X(40) — 40 bytes (TRA-7). */
    private static final int PROCTRAN_DESC_LENGTH = 40;

    /**
     * Free-form abort description prefix (COBOL ABND-FREEFORM STRING statement,
     * CRECUST.cbl lines 1428–1438).
     *
     * <p>COBOL source: {@code 'WPD010  - Unable to write to PROCTRAN DB2 datastore with the
     * following data:'}.
     */
    private static final String ABND_FREEFORM_PREFIX =
            "WPD010  - Unable to write to PROCTRAN DB2 datastore with the following data: SQLCODE=";

    /**
     * Assembles the PROCTRAN descriptor and executes the JDBC INSERT into the PROCTRAN table.
     *
     * <p>Mirrors COBOL {@code WRITE-PROCTRAN-DB2_WPD010} (CRECUST.cbl lines 1320–1384):
     * <ol>
     *   <li>Obtains the current timestamp via {@code LocalDateTime.now()} (replaces
     *       {@code EXEC CICS ASKTIME/FORMATTIME}).</li>
     *   <li>Formats {@code hvProctranDate} as {@code DD.MM.YYYY} and
     *       {@code hvProctranTime} as {@code HHmmss}.</li>
     *   <li>Assembles the 40-byte descriptor from the four stored values at exact offsets
     *       (TRA-7): sortcode[0–5], custno[6–15], name[16–29], dob[30–39].</li>
     *   <li>Executes the 9-column INSERT via JDBC {@code try-with-resources} (NFR-4).</li>
     * </ol>
     *
     * <p>On {@link SQLException}: executes the notifying-abort sequence (AC-1.1, Rule 1,
     * Story 8.2) in the exact COBOL WPD010 order:
     * <ol>
     *   <li>Populate {@link AbndInfoRec} via {@link #populateAbndInfo(int, CrecustCommarea)}</li>
     *   <li>LINK ABNDPROC — {@code abndprocDelegate.linkAbndproc(abndInfoRec)}</li>
     *   <li>DEQ named counter — {@code customerNumberService.dequeue(commArea, nameResource)}</li>
     *   <li>ABEND 'HWPT' — {@code Task.getTask().abend(CrecustException.ABEND_CODE_HWPT)}</li>
     * </ol>
     *
     * <p><strong>Implementation note (COBOL source order):</strong> CRECUST.cbl WPD010 issues
     * {@code EXEC CICS LINK} (line 1441) <em>before</em> {@code PERFORM DEQ-NAMED-COUNTER}
     * (line 1453). This differs from the story spec ordering (which lists DEQ before LINK). The
     * COBOL source is authoritative per the story's Design Notes; LINK precedes DEQ here to
     * match the source faithfully.
     *
     * @param commArea              commarea providing task/time context and sort-code
     * @param storedSortcode        sort code (6 chars) embedded at descriptor bytes 0–5
     * @param storedCustno          customer number (10 chars) embedded at descriptor bytes 6–15
     * @param storedName            customer name (14 chars) embedded at descriptor bytes 16–29
     * @param storedDob             date of birth in DD/MM/YYYY format (10 chars) at descriptor
     *                              bytes 30–39
     * @param customerNumberService service owning the CICS DEQ operation (Fan-In-4 site 3)
     * @param nameResource          the resource-name token to release on abort
     * @param abndprocDelegate      CICS LINK delegate to ABNDPROC
     */
    public void insertProctran(
            CrecustCommarea commArea,
            String storedSortcode,
            String storedCustno,
            String storedName,
            String storedDob,
            CustomerNumberService customerNumberService,
            String nameResource) {

        // Step 1 — timestamp (replaces EXEC CICS ASKTIME / FORMATTIME)
        LocalDateTime now = LocalDateTime.now();

        // Step 2 — format date DD.MM.YYYY and time HHmmss (AC-11.1, AC-11.2, Rule 11)
        String hvProctranDate = now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        String hvProctranTime = now.format(DateTimeFormatter.ofPattern("HHmmss"));

        // Step 3 — assemble 40-byte descriptor (TRA-7, COBOL lines 1351–1354, 1-based → 0-based)
        // COBOL: HV-PROCTRAN-DESC(1:6)  → Java [0..5]
        // COBOL: HV-PROCTRAN-DESC(7:10) → Java [6..15]
        // COBOL: HV-PROCTRAN-DESC(17:14)→ Java [16..29]
        // COBOL: HV-PROCTRAN-DESC(31:10)→ Java [30..39]
        char[] desc = new char[PROCTRAN_DESC_LENGTH];
        System.arraycopy(storedSortcode.toCharArray(), 0, desc, 0, 6);
        System.arraycopy(storedCustno.toCharArray(),   0, desc, 6, 10);
        System.arraycopy(storedName.toCharArray(),     0, desc, 16, 14);
        System.arraycopy(storedDob.toCharArray(),      0, desc, 30, 10);
        String hvProctranDesc = new String(desc);

        // Step 4 — fixed fields
        long eibtaskn = abndprocDelegate.getTaskNumber();
        String hvProctranRef = String.format("%012d", eibtaskn);

        HostProctranRow row = HostProctranRow.builder()
                .hvProctranEyecatcher(PROCTRAN_EYECATCHER)
                .hvProctranSortCode(storedSortcode)
                .hvProctranAccNumber(PROCTRAN_ACC_NUMBER)
                .hvProctranDate(hvProctranDate)
                .hvProctranTime(hvProctranTime)
                .hvProctranRef(hvProctranRef)
                .hvProctranType(PROCTRAN_TYPE)
                .hvProctranDesc(hvProctranDesc)
                .hvProctranAmount(BigDecimal.ZERO)
                .build();

        LOGGER.debug(
                "insertProctran: eyecatcher={} sortCode={} accNumber={} date={} time={}"
                + " ref={} type={} desc={} amount={}",
                row.getHvProctranEyecatcher(), row.getHvProctranSortCode(),
                row.getHvProctranAccNumber(), row.getHvProctranDate(),
                row.getHvProctranTime(), row.getHvProctranRef(),
                row.getHvProctranType(), row.getHvProctranDesc(),
                row.getHvProctranAmount());

        // Step 5 — JDBC INSERT (9 columns, PE-3 order, try-with-resources — NFR-4)
        //
        // The outer try converts NamingException into SQLException so both failure paths
        // reach the single notifying-abort catch block below (COBOL WPD010 IF SQLCODE NOT = 0).
        try {
            try {
                DataSource ds = restAdapter.getDataSource(PROCTRAN_DATASOURCE);
                try (Connection conn = dbAdapter.getConnection(ds);
                     PreparedStatement ps = dbAdapter.prepareStatement(conn, INSERT_PROCTRAN_SQL)) {
                    ps.setString(1, row.getHvProctranEyecatcher());
                    ps.setString(2, row.getHvProctranSortCode());
                    ps.setString(3, row.getHvProctranAccNumber());
                    ps.setString(4, row.getHvProctranDate());   // String, NOT java.sql.Date (Rule 7)
                    ps.setString(5, row.getHvProctranTime());
                    ps.setString(6, row.getHvProctranRef());
                    ps.setString(7, row.getHvProctranType());
                    ps.setString(8, row.getHvProctranDesc());
                    ps.setBigDecimal(9, row.getHvProctranAmount());
                    dbAdapter.executeUpdate(ps);
                }
            } catch (NamingException e) {
                throw new SQLException("JNDI lookup failed for " + PROCTRAN_DATASOURCE, e);
            }
        } catch (SQLException e) {
            // Notifying-abort path (Rule 1, AC-1.1, Story 8.2).
            //
            // COBOL WPD010 order (CRECUST.cbl lines 1397–1457, authoritative):
            //   Step 1: Populate AbndInfoRec fields
            //   Step 2: EXEC CICS LINK PROGRAM(WS-ABEND-PGM) COMMAREA(ABNDINFO-REC) [line 1441]
            //   Step 3: PERFORM DEQ-NAMED-COUNTER [line 1453]
            //   Step 4: EXEC CICS ABEND ABCODE('HWPT')                               [line 1455]
            //
            // NOTE: The COBOL source places LINK (step 2) BEFORE DEQ (step 3).  This differs
            // from the story spec ordering (DEQ then LINK).  Per Design Notes in Story 8.2:
            // "If source review shows DEQ and LINK order differs from epics spec, follow the
            // COBOL source and record the discrepancy."  The COBOL source is followed here.

            // Step 1 — populate AbndInfoRec
            AbndInfoRec abndInfoRec = populateAbndInfo(e.getErrorCode(), commArea);

            // Step 2 — LINK ABNDPROC (EXEC CICS LINK PROGRAM(WS-ABEND-PGM) — line 1441)
            abndprocDelegate.linkAbndproc(abndInfoRec);

            // Step 3 — DEQ named counter (Fan-In-4 site #3 — line 1453)
            customerNumberService.dequeue(commArea, nameResource);

            // Step 4 — ABEND 'HWPT' (line 1455) — no bare "HWPT" literal (Rule 4, AC-4.2)
            abndprocDelegate.abend(CrecustException.ABEND_CODE_HWPT);
        }
        // Step 6 — on success, return normally; caller (CrecustService) sets commSuccess='Y'
    }

    /**
     * Populates and returns an {@link AbndInfoRec} from the JDBC error context and commarea.
     *
     * <p>Translates CRECUST.cbl {@code WRITE-PROCTRAN-DB2_WPD010} lines 1397–1438:
     * <ul>
     *   <li>{@code INITIALIZE ABNDINFO-REC} → fresh {@code AbndInfoRec} builder</li>
     *   <li>{@code MOVE EIBRESP TO ABND-RESPCODE} → {@code "0"} (no CICS call failed here)</li>
     *   <li>{@code MOVE EIBRESP2 TO ABND-RESP2CODE} → {@code "0"}</li>
     *   <li>{@code EXEC CICS ASSIGN APPLID(ABND-APPLID)} → {@link Region#getAPPLID()}</li>
     *   <li>{@code MOVE EIBTASKN TO ABND-TASKNO-KEY} →
     *       {@code Task.getTask().getTaskNumber()} formatted as 4-digit string</li>
     *   <li>{@code MOVE EIBTRNID TO ABND-TRANID} →
     *       {@code Task.getTask().getTransactionName()}</li>
     *   <li>{@code PERFORM POPULATE-TIME-DATE2} → {@code LocalDateTime.now()}</li>
     *   <li>{@code MOVE WS-ORIG-DATE TO ABND-DATE} → {@code "DD/MM/YYYY"} formatted date</li>
     *   <li>{@code STRING WS-TIME-NOW-GRP-HH ':' WS-TIME-NOW-GRP-MM ':' WS-TIME-NOW-GRP-SS
     *       INTO ABND-TIME} → {@code "HH:MM:SS"} formatted time</li>
     *   <li>{@code MOVE WS-U-TIME TO ABND-UTIME-KEY} →
     *       {@code LocalDateTime.now()} converted to epoch millis ({@code Task.getTask().getAbstime()}
     *       does NOT exist in JCICS)</li>
     *   <li>{@code MOVE 'HWPT' TO ABND-CODE} → {@link CrecustException#ABEND_CODE_HWPT}
     *       (no bare literal — Rule 4, AC-4.2)</li>
     *   <li>{@code EXEC CICS ASSIGN PROGRAM(ABND-PROGRAM)} →
     *       {@code Task.getTask().getInvokingProgramName()}</li>
     *   <li>{@code MOVE SQLCODE-DISPLAY TO ABND-SQLCODE} →
     *       {@code String.format("%d", sqlcode)}</li>
     *   <li>STRING into {@code ABND-FREEFORM} → {@link #ABND_FREEFORM_PREFIX} + sqlcode</li>
     * </ul>
     *
     * @param sqlcode  the JDBC error code from the failing SQL statement (from
     *                 {@code SQLException.getErrorCode()})
     * @param commArea the current commarea (not mutated by this method)
     * @return a fully populated {@link AbndInfoRec} ready for serialization and LINK
     */
    private AbndInfoRec populateAbndInfo(int sqlcode, CrecustCommarea commArea) {
        // EXEC CICS ASKTIME / PERFORM POPULATE-TIME-DATE2 equivalent
        LocalDateTime now = LocalDateTime.now();

        // MOVE WS-ORIG-DATE TO ABND-DATE — "DD/MM/YYYY" (DATESEP, DDMMYYYY pattern)
        String abndDate = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        // STRING WS-TIME-NOW-GRP-HH ':' ... INTO ABND-TIME → "HH:MM:SS" (8 chars, colon-sep)
        String abndTime = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        // MOVE WS-U-TIME TO ABND-UTIME-KEY — Task.getTask().getAbstime() does NOT exist in JCICS;
        // use LocalDateTime.now() converted to epoch millis (Story 8.2 Design Notes, epic-8-context)
        long abndUtimeKey = now.toInstant(ZoneOffset.UTC).toEpochMilli();

        // MOVE EIBTASKN TO ABND-TASKNO-KEY — 4-digit zero-padded task number
        String abndTasknoKey = abndprocDelegate.getFormattedTaskNumber();

        // EXEC CICS ASSIGN APPLID(ABND-APPLID) — Region.getAPPLID() (NOT Task.getTask().getTask()
        // .getRegion().getApplid() — does NOT exist in JCICS, story Boundaries & Constraints)
        String abndApplid = abndprocDelegate.getApplid();

        // MOVE EIBTRNID TO ABND-TRANID
        String abndTranid = abndprocDelegate.getTransactionName();

        // EXEC CICS ASSIGN PROGRAM(ABND-PROGRAM)
        String abndProgram = abndprocDelegate.getInvokingProgramName();

        // MOVE 'HWPT' TO ABND-CODE — no bare literal; use named constant (Rule 4, AC-4.2)
        String abndCode = CrecustException.ABEND_CODE_HWPT;

        // MOVE SQLCODE-DISPLAY TO ABND-SQLCODE
        String abndSqlcode = String.format("%d", sqlcode);

        // STRING 'WPD010  - Unable to write ...' ... ABND-RESP2CODE INTO ABND-FREEFORM
        // (CRECUST.cbl lines 1428–1438)
        String abndFreeform = ABND_FREEFORM_PREFIX + abndSqlcode;

        // EIBRESP / EIBRESP2 — no CICS call was active at the point of SQL failure; set to "0"
        String abndRespcode  = "0";
        String abndResp2code = "0";

        return AbndInfoRec.builder()
                .abndUtimeKey(abndUtimeKey)
                .abndTasknoKey(abndTasknoKey)
                .abndApplid(abndApplid)
                .abndTranid(abndTranid)
                .abndDate(abndDate)
                .abndTime(abndTime)
                .abndCode(abndCode)
                .abndProgram(abndProgram)
                .abndRespcode(abndRespcode)
                .abndResp2code(abndResp2code)
                .abndSqlcode(abndSqlcode)
                .abndFreeform(abndFreeform)
                .build();
    }
}
