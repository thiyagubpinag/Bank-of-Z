package com.ibm.cics.botz.crecust.service;

import com.ibm.cics.botz.crecust.db.HostControlRow;
import com.ibm.cics.botz.crecust.exception.CrecustException;
import com.ibm.cics.botz.crecust.model.CrecustCommarea;
import com.ibm.cics.botz.crecust.model.CustomerKy2;
import com.ibm.cics.botz.crecust.model.NcsCustNoStuff;
import com.ibm.cics.botz.crecust.service.adapters.DBAdapter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.naming.NamingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the CICS named-counter ENQ/DEQ and the DB2 CONTROL table customer-number sequence.
 *
 * <p>Implements the three-step customer-number allocation protocol from CRECUST.cbl:
 * {@code ENQ-NAMED-COUNTER} (line 540), {@code GET-LAST-CUSTOMER-DB2} / {@code UPD-NCS} sections,
 * and {@code DEQ-NAMED-COUNTER} (line 562).
 *
 * @see "CRECUST.cbl ENQ-NAMED-COUNTER (line 540), DEQ-NAMED-COUNTER (line 562),
 *       UPD-NCS / GET-LAST-CUSTOMER-DB2 sections"
 */
public class CustomerNumberService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerNumberService.class);

    private final AbndprocDelegate abndprocDelegate = AbndprocDelegate.getInstance();
    private final DBAdapter dbAdapter = DBAdapter.getInstance();

    /**
     * CICS named-resource ENQ/DEQ prefix for the customer-number lock.
     *
     * <p>Corresponds to {@code NCS-CUST-NO-ACT-NAME PIC X(9) VALUE 'BANKZCUST'}
     * in CRECUST.cbl (technical-research line 228).
     */
    private static final String NCS_ACT_NAME = "BANKZCUST";

    /**
     * Length of the ENQ/DEQ resource name in bytes.
     *
     * <p>Corresponds to {@code LENGTH(16)} in {@code EXEC CICS ENQ} / {@code EXEC CICS DEQ}
     * (CRECUST.cbl lines 547, 572).
     */
    private static final int ENQ_RESOURCE_LENGTH = 16;

    /** JNDI name of the DB2 DataSource (ADR-4). */
    private static final String DATASOURCE_JNDI_NAME = "jdbc/crecustDB2DS";

    /**
     * Length of {@code HV-CONTROL-NAME PIC X(32)} (CRECUST.cbl line 110).
     */
    private static final int HV_CONTROL_NAME_LENGTH = 32;

    /**
     * Width of the display-numeric customer number {@code COMM-NUMBER PIC 9(10)}.
     */
    private static final int CUSTOMER_NUMBER_WIDTH = 10;

    /**
     * {@code SELECT CONTROL_VALUE_NUM INTO :HV-CONTROL-VALUE-NUM FROM CONTROL
     * WHERE CONTROL_NAME = :HV-CONTROL-NAME} (CRECUST.cbl lines 1496–1501).
     */
    private static final String CONTROL_SELECT_SQL =
            "SELECT CONTROL_VALUE_NUM FROM STTESTER.CONTROL WHERE CONTROL_NAME = ?";

    /**
     * {@code UPDATE CONTROL SET CONTROL_VALUE_NUM = :HV-CONTROL-VALUE-NUM
     * WHERE CONTROL_NAME = :HV-CONTROL-NAME} (CRECUST.cbl lines 1524–1528).
     */
    private static final String CONTROL_UPDATE_SQL =
            "UPDATE STTESTER.CONTROL SET CONTROL_VALUE_NUM = ? WHERE CONTROL_NAME = ?";

    /**
     * Acquires the CICS ENQ named-counter lock for the customer-number resource.
     *
     * <p>Translates {@code ENQ-NAMED-COUNTER_ENC010} (CRECUST.cbl lines 541–556):
     * <pre>
     *   MOVE SORTCODE TO NCS-CUST-NO-TEST-SORT
     *   EXEC CICS ENQ RESOURCE(NCS-CUST-NO-NAME) LENGTH(16) ...
     *   IF WS-CICS-RESP NOT = DFHRESP(NORMAL)
     *     MOVE 'N' TO COMM-SUCCESS
     *     MOVE '3' TO COMM-FAIL-CODE
     *     PERFORM GET-ME-OUT-OF-HERE  (→ return)
     *   END-IF
     * </pre>
     *
     * <p>Resource name = {@code NCS_ACT_NAME + commArea.getCommSortcode() + "  "},
     * truncated to exactly {@link #ENQ_RESOURCE_LENGTH} bytes.
     *
     * <p>On {@code CicsConditionException}: sets {@code commSuccess='N'} and
     * {@code commFailCode=CrecustException.FAIL_CODE_ENQ}, then returns — silent-return path
     * (Rule 1, cics-transformation). No exception is re-thrown.
     *
     * @param commArea commarea; {@code commSortcode} is used to compose the 16-byte resource name
     * @return the acquired resource name (opaque token) on success; {@code null} if ENQ failed and
     *         a silent-return fail-code has been set on the commarea
     */
    public String enqueue(CrecustCommarea commArea) {
        String resourceName = (NCS_ACT_NAME + commArea.getCommSortcode() + "  ")
                .substring(0, ENQ_RESOURCE_LENGTH);

        LOGGER.debug("CustomerNumberService.enqueue() — ENQ resource='{}' (length={})",
                resourceName, ENQ_RESOURCE_LENGTH);

        try {
            abndprocDelegate.enqueue(resourceName);
        } catch (CicsAdapterException e) {
            LOGGER.error("CustomerNumberService.enqueue() — ENQ failed; setting fail-code {}",
                    CrecustException.FAIL_CODE_ENQ, e);
            commArea.setCommSuccess("N");
            commArea.setCommFailCode(CrecustException.FAIL_CODE_ENQ);
            return null;
        }

        return resourceName;
    }

    /**
     * Reads the current customer number from the DB2 {@code CONTROL} table and increments it.
     *
     * <p>Translates {@code UPD-NCS_UN010} (CRECUST.cbl lines 587–599) and
     * {@code GET-LAST-CUSTOMER-DB2_GLCD010} (CRECUST.cbl lines 1476–1552):
     * <pre>
     *   MOVE 1 TO NCS-CUST-NO-INC
     *   STRING NCS-CUST-NO-ACT-NAME NCS-CUST-NO-TEST-SORT NCS-CUST-NO-FILL INTO HV-CONTROL-NAME
     *   EXEC SQL SELECT CONTROL_VALUE_NUM ... END-EXEC   (SQLCODE NOT = 0 → DEQ, fail '4', return)
     *   ADD 1 TO HV-CONTROL-VALUE-NUM
     *   EXEC SQL UPDATE CONTROL ... END-EXEC             (SQLCODE NOT = 0 → DEQ, fail '4', return)
     *   MOVE WS-CUSTOMER-NO-NUM TO COMM-NUMBER CUSTOMER-NUMBER REQUIRED-CUST-NUMBER2
     *   MOVE HV-CONTROL-VALUE-NUM TO NCS-CUST-NO-VALUE
     * </pre>
     *
     * <p>Silent-return path (Rule 1): on {@link SQLException}, {@link NamingException}, or
     * SELECT not-found (DB2 SQLCODE +100), calls {@link #dequeue(CrecustCommarea, NameResource)}
     * (Fan-In-4 DEQ site), sets {@code commSuccess='N'} and
     * {@code commFailCode=CrecustException.FAIL_CODE_CONTROL_SQL}, then returns.
     *
     * @param commArea     commarea; {@code commNumber} is populated with the assigned customer
     *                     number on success
     * @param resourceName the resource name token returned by {@link #enqueue(CrecustCommarea)};
     *                     passed so that DEQ can be issued on SQL failure
     */
    public void getAndIncrementCustomerNumber(CrecustCommarea commArea, String resourceName) {
        // UPD-NCS_UN010: MOVE 1 TO NCS-CUST-NO-INC; MOVE SORTCODE TO NCS-CUST-NO-TEST-SORT
        NcsCustNoStuff ncsCustNoStuff = new NcsCustNoStuff();
        ncsCustNoStuff.setNcsCustNoInc(1L);
        ncsCustNoStuff.setNcsCustNoTestSort(commArea.getCommSortcode());

        HostControlRow hostControlRow = new HostControlRow();
        hostControlRow.setHvControlName(buildControlName(ncsCustNoStuff));

        LOGGER.debug("CustomerNumberService.getAndIncrementCustomerNumber() — "
                + "Searching for CONTROL_NAME=[{}]",
                hostControlRow.getHvControlName().substring(0, ENQ_RESOURCE_LENGTH));

        try (Connection conn = dbAdapter.getConnection(DATASOURCE_JNDI_NAME)) {
            // SELECT CONTROL_VALUE_NUM INTO :HV-CONTROL-VALUE-NUM
            try (PreparedStatement sel = dbAdapter.prepareStatement(conn, CONTROL_SELECT_SQL)) {
                sel.setString(1, hostControlRow.getHvControlName());
                try (ResultSet rs = dbAdapter.executeQuery(sel)) {
                    if (!dbAdapter.next(rs)) {
                        // SQLCODE +100 (row not found) → IF SQLCODE NOT = 0 branch
                        LOGGER.error("CustomerNumberService.getAndIncrementCustomerNumber() — "
                                + "SELECT CONTROL failed: no row for CONTROL_NAME=[{}]",
                                hostControlRow.getHvControlName().substring(0, ENQ_RESOURCE_LENGTH));
                        failControlSql(commArea, resourceName);
                        return;
                    }
                    hostControlRow.setHvControlValueNum(rs.getInt(1));
                }
            }

            // ADD 1 TO HV-CONTROL-VALUE-NUM
            hostControlRow.setHvControlValueNum(hostControlRow.getHvControlValueNum() + 1);

            // UPDATE CONTROL SET CONTROL_VALUE_NUM = :HV-CONTROL-VALUE-NUM
            try (PreparedStatement upd = dbAdapter.prepareStatement(conn, CONTROL_UPDATE_SQL)) {
                upd.setInt(1, hostControlRow.getHvControlValueNum());
                upd.setString(2, hostControlRow.getHvControlName());
                dbAdapter.executeUpdate(upd);
            }
        } catch (SQLException e) {
            LOGGER.error("CustomerNumberService.getAndIncrementCustomerNumber() — CONTROL SQL failed. "
                    + "SQLCODE={}", e.getErrorCode(), e);
            failControlSql(commArea, resourceName);
            return;
        } catch (NamingException e) {
            LOGGER.error("CustomerNumberService.getAndIncrementCustomerNumber() — JNDI lookup of '{}' "
                    + "failed", DATASOURCE_JNDI_NAME, e);
            failControlSql(commArea, resourceName);
            return;
        }

        pushCustomerNumber(commArea, ncsCustNoStuff, hostControlRow.getHvControlValueNum());
    }


    /**
     * Builds {@code HV-CONTROL-NAME PIC X(32)}:
     * {@code INITIALIZE HV-CONTROL-NAME} (spaces) then
     * {@code STRING NCS-CUST-NO-ACT-NAME NCS-CUST-NO-TEST-SORT NCS-CUST-NO-FILL DELIMITED BY SIZE}.
     *
     * @param ncsCustNoStuff NCS fields holding the act-name, sortcode and fill
     * @return the 32-character space-padded control-table key
     */
    private String buildControlName(NcsCustNoStuff ncsCustNoStuff) {
        String raw = ncsCustNoStuff.getNcsCustNoActName()
                + ncsCustNoStuff.getNcsCustNoTestSort()
                + ncsCustNoStuff.getNcsCustNoFill();
        return String.format("%-" + HV_CONTROL_NAME_LENGTH + "s", raw);
    }

    /**
     * Silent-return failure for CONTROL SQL (Rule 1): DEQ (Fan-In-4 site), then
     * {@code MOVE 'N' TO COMM-SUCCESS}, {@code MOVE '4' TO COMM-FAIL-CODE}.
     *
     * @param commArea     commarea receiving the fail-code
     * @param resourceName the ENQ'd resource name token to release
     */
    private void failControlSql(CrecustCommarea commArea, String resourceName) {
        dequeue(commArea, resourceName);
        commArea.setCommSuccess("N");
        commArea.setCommFailCode(CrecustException.FAIL_CODE_CONTROL_SQL);
    }

    /**
     * Pushes the new customer number to its targets (FR-7.4, AC-20.3):
     * {@code MOVE WS-CUSTOMER-NO-NUM TO COMM-NUMBER CUSTOMER-NUMBER REQUIRED-CUST-NUMBER2} and
     * {@code MOVE HV-CONTROL-VALUE-NUM TO NCS-CUST-NO-VALUE}.
     *
     * <p>{@code CUSTOMER-NUMBER} ({@code CustomerRecord}) is owned by the orchestrator and is
     * populated from {@code commNumber} downstream; it is not reachable from this signature.
     *
     * @param commArea       commarea; receives {@code commNumber}
     * @param ncsCustNoStuff NCS fields; receives {@code ncsCustNoValue}
     * @param newValue       incremented {@code HV-CONTROL-VALUE-NUM}
     */
    private void pushCustomerNumber(CrecustCommarea commArea, NcsCustNoStuff ncsCustNoStuff,
            int newValue) {
        // MOVE HV-CONTROL-VALUE-NUM TO WS-CUSTOMER-NO-NUM (COMP → DISPLAY PIC 9(10))
        String wsCustomerNoNum = String.format("%0" + CUSTOMER_NUMBER_WIDTH + "d", newValue);

        commArea.setCommNumber(wsCustomerNoNum);

        CustomerKy2 customerKy2 = new CustomerKy2();
        customerKy2.setRequiredCustNumber2(newValue);

        ncsCustNoStuff.setNcsCustNoValue(newValue);

        LOGGER.debug("CustomerNumberService.getAndIncrementCustomerNumber() — new customer number={}",
                wsCustomerNoNum);
    }

    /**
     * Releases the CICS DEQ named-counter lock for the customer-number resource.
     *
     * <p>Translates {@code DEQ-NAMED-COUNTER_DNC010} (CRECUST.cbl lines 562–584):
     * <pre>
     *   EXEC CICS DEQ RESOURCE(NCS-CUST-NO-NAME) LENGTH(16)
     *             RESP(WS-CICS-RESP) RESP2(WS-CICS-RESP2) END-EXEC.
     *   IF WS-CICS-RESP NOT = DFHRESP(NORMAL)
     *     MOVE 'N' TO COMM-SUCCESS
     *     MOVE '5' TO COMM-FAIL-CODE
     *     PERFORM GET-ME-OUT-OF-HERE  (→ return)
     *   END-IF.
     * </pre>
     *
     * <p>The Fan-In-4 shared DEQ utility — called from four sites:
     * <ol>
     *   <li>{@link #failControlSql} — CONTROL SELECT/UPDATE failure (Story 6.2)</li>
     *   <li>{@code CustomerDbService.insertCustomer()} — INSERT CUSTOMER failure (Story 7.2)</li>
     *   <li>{@code ProctranDbService.insertProctran()} failure path — notifying-abort (Story 8.2)</li>
     *   <li>{@code ProctranDbService.insertProctran()} success path (Story 8.1)</li>
     * </ol>
     *
     * <p>On {@link CicsConditionException}: sets {@code commSuccess='N'} and
     * {@code commFailCode=CrecustException.FAIL_CODE_DEQ ('5')}, then returns —
     * silent-return path (Rule 1, cics-transformation). No exception is re-thrown, no ABEND.
     *
     * <p>If {@code nameResource} is {@code null} (ENQ never succeeded on this path), the method
     * logs a warning and returns immediately without any CICS call.
     *
     * @param commArea     commarea; {@code commSuccess} and {@code commFailCode} are set on
     *                     DEQ failure
     * @param resourceName the resource name token returned by {@link #enqueue(CrecustCommarea)};
     *                     must be the same name that was enqueued; may be {@code null} when
     *                     ENQ was never acquired
     */
    public void dequeue(CrecustCommarea commArea, String resourceName) {
        if (resourceName == null) {
            LOGGER.warn("CustomerNumberService.dequeue() — resourceName is null; DEQ skipped");
            return;
        }

        try {
            abndprocDelegate.dequeue(resourceName);
            LOGGER.debug("CustomerNumberService.dequeue() — DEQ released resource");
        } catch (CicsAdapterException e) {
            LOGGER.error("CustomerNumberService.dequeue() — DEQ failed; setting fail-code {}",
                    CrecustException.FAIL_CODE_DEQ, e);
            commArea.setCommSuccess("N");
            commArea.setCommFailCode(CrecustException.FAIL_CODE_DEQ);
        }
    }
}
