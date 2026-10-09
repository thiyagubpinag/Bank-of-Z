package com.ibm.cics.botz.crecust.service;

import com.ibm.cics.botz.crecust.db.HostCustomerRow;
import com.ibm.cics.botz.crecust.exception.CrecustException;
import com.ibm.cics.botz.crecust.model.CrecustCommarea;
import com.ibm.cics.botz.crecust.model.CustomerRecord;
import com.ibm.cics.botz.crecust.service.adapters.DBAdapter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.naming.NamingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inserts a new customer record into the DB2 {@code CUSTOMER} table.
 *
 * <p>Story 7.1 implements {@link #populateHostCustomerRow} — maps all 17 {@link HostCustomerRow}
 * fields from commarea in PE-3 column order, encodes three date columns as YYYYMMDD integers, and
 * casts the credit-score to {@code short} (SMALLINT).
 *
 * <p>Story 7.2 implements {@link #insertCustomer} — the JDBC INSERT execution (FR-8.3).
 *
 * @see "CRECUST.cbl WRITE-CUSTOMER-DB2 section (WCD010, lines 1138–)"
 */
public class CustomerDbService {

    private static final Logger log = LoggerFactory.getLogger(CustomerDbService.class);

    private final DBAdapter dbAdapter = DBAdapter.getInstance();

    /** Eyecatcher literal — COBOL: MOVE 'CUST' TO HV-CUSTOMER-EYECATCHER (line 1161). */
    private static final String EYECATCHER = "CUST";

    /** JNDI name of the DB2 DataSource (ADR-4). */
    private static final String DATASOURCE_JNDI_NAME = "jdbc/crecustDB2DS";

    /**
     * INSERT CUSTOMER — 17 columns in PE-3 column order (CRECUST.cbl lines 1219–1256).
     *
     * <p>Column names verified from COBOL {@code EXEC SQL INSERT INTO CUSTOMER} statement
     * (CRECUST.cbl lines 1221–1237): {@code CUSTOMER_EYECATCHER, CUSTOMER_SORTCODE,
     * CUSTOMER_NUMBER, CUSTOMER_TITLE, CUSTOMER_FIRST_NAME, CUSTOMER_LAST_NAME,
     * CUSTOMER_DATE_OF_BIRTH, CUSTOMER_PHONE, CUSTOMER_ADDR_LINE1, CUSTOMER_ADDR_LINE2,
     * CUSTOMER_CITY, CUSTOMER_POSTCODE, CUSTOMER_COUNTRY, CUSTOMER_STATUS,
     * CUSTOMER_CREATED_DATE, CUSTOMER_CREDIT_SCORE, CUSTOMER_CS_REVIEW_DATE}.
     */
    private static final String INSERT_CUSTOMER_SQL =
            "INSERT INTO STTESTER.CUSTOMER " +
            "(CUSTOMER_EYECATCHER, CUSTOMER_SORTCODE, CUSTOMER_NUMBER, CUSTOMER_TITLE, " +
            " CUSTOMER_FIRST_NAME, CUSTOMER_LAST_NAME, CUSTOMER_DATE_OF_BIRTH, CUSTOMER_PHONE, " +
            " CUSTOMER_ADDR_LINE1, CUSTOMER_ADDR_LINE2, CUSTOMER_CITY, CUSTOMER_POSTCODE, " +
            " CUSTOMER_COUNTRY, CUSTOMER_STATUS, CUSTOMER_CREATED_DATE, CUSTOMER_CREDIT_SCORE, " +
            " CUSTOMER_CS_REVIEW_DATE) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    // -----------------------------------------------------------------------
    // Story 7.1 — populate HostCustomerRow from commarea
    // -----------------------------------------------------------------------

    /**
     * Populates all 17 fields of {@code row} from {@code commArea} in PE-3 column order.
     *
     * <p>Mirrors COBOL {@code WRITE-CUSTOMER-DB2 / WCD010} host-variable MOVE and COMPUTE
     * statements (CRECUST.cbl lines 1161–1197). No instance state; pure data mapping.
     *
     * @param commArea       commarea supplying all customer input fields
     * @param customerRecord pre-allocated customer record model (not used for host variables in
     *                       this story; reserved for Story 7.2 context)
     * @param row            host-variable row to populate (side-effected in place)
     */
    public void populateHostCustomerRow(
            CrecustCommarea commArea,
            CustomerRecord customerRecord,
            HostCustomerRow row) {

        log.debug("populateHostCustomerRow: sortcode={} number={}",
                commArea.getCommSortcode(), commArea.getCommNumber());

        // 1. Eyecatcher — always first (FR-8.4)
        // COBOL: MOVE 'CUST' TO HV-CUSTOMER-EYECATCHER (line 1161)
        row.setHvCustomerEyecatcher(EYECATCHER);

        // 2. Sort code
        // COBOL: MOVE SORTCODE TO HV-CUSTOMER-SORTCODE (line 1162)
        row.setHvCustomerSortcode(commArea.getCommSortcode());

        // 3. Customer number
        // COBOL: MOVE WS-CUSTOMER-NO-NUM TO HV-CUSTOMER-NUMBER (line 1163)
        row.setHvCustomerNumber(commArea.getCommNumber());

        // 4. Title
        // COBOL: MOVE COMM-TITLE OF COMM-NAME TO HV-CUSTOMER-TITLE (line 1164)
        row.setHvCustomerTitle(commArea.getCommTitle());

        // 5. First name
        // COBOL: MOVE COMM-FIRST-NAME OF COMM-NAME TO HV-CUSTOMER-FIRST-NAME (line 1165)
        row.setHvCustomerFirstName(commArea.getCommFirstName());

        // 6. Last name
        // COBOL: MOVE COMM-LAST-NAME OF COMM-NAME TO HV-CUSTOMER-LAST-NAME (line 1166)
        row.setHvCustomerLastName(commArea.getCommLastName());

        // 7. Date of birth — YYYYMMDD integer
        // COBOL: COMPUTE HV-CUSTOMER-DOB = (COMM-DOB-YEAR * 10000) + ... (lines 1167–1170)
        row.setHvCustomerDob(toDateInt(
                parseIntField(commArea.getCommDobYear()),
                parseIntField(commArea.getCommDobMonth()),
                parseIntField(commArea.getCommDobDay())));

        // 8. Phone
        // COBOL: MOVE COMM-PHONE TO HV-CUSTOMER-PHONE (line 1171)
        row.setHvCustomerPhone(commArea.getCommPhone());

        // 9. Address line 1
        // COBOL: MOVE COMM-ADDR-LINE1 OF COMM-ADDR TO HV-CUSTOMER-ADDR-LINE1 (lines 1172–1173)
        row.setHvCustomerAddrLine1(commArea.getCommAddrLine1());

        // 10. Address line 2
        // COBOL: MOVE COMM-ADDR-LINE2 OF COMM-ADDR TO HV-CUSTOMER-ADDR-LINE2 (lines 1174–1175)
        row.setHvCustomerAddrLine2(commArea.getCommAddrLine2());

        // 11. City
        // COBOL: MOVE COMM-CITY OF COMM-ADDR TO HV-CUSTOMER-CITY (line 1176)
        row.setHvCustomerCity(commArea.getCommCity());

        // 12. Postcode
        // COBOL: MOVE COMM-POSTCODE OF COMM-ADDR TO HV-CUSTOMER-POSTCODE (line 1177)
        row.setHvCustomerPostcode(commArea.getCommPostcode());

        // 13. Country
        // COBOL: MOVE COMM-COUNTRY OF COMM-ADDR TO HV-CUSTOMER-COUNTRY (line 1178)
        row.setHvCustomerCountry(commArea.getCommCountry());

        // 14. Status
        // COBOL: MOVE COMM-STATUS TO HV-CUSTOMER-STATUS (line 1179)
        row.setHvCustomerStatus(commArea.getCommStatus());

        // 15. Created date — YYYYMMDD integer
        // COBOL: COMPUTE HV-CUSTOMER-CREATE-DATE = (COMM-CREATED-YEAR * 10000) + ... (lines 1184–1187)
        row.setHvCustomerCreateDate(toDateInt(
                parseIntField(commArea.getCommCreatedYear()),
                parseIntField(commArea.getCommCreatedMonth()),
                parseIntField(commArea.getCommCreatedDay())));

        // 16. Credit score — SMALLINT (S9(4) COMP → short)
        // COBOL: MOVE COMM-CREDIT-SCORE TO HV-CUSTOMER-CREDIT-SCORE (line 1189)
        row.setHvCustomerCreditScore((short) parseIntField(commArea.getCommCreditScore()));

        // 17. CS review date — YYYYMMDD integer
        // COBOL: COMPUTE HV-CUSTOMER-CS-REVIEW-DATE = (COMM-CS-REVIEW-YEAR * 10000) + ... (lines 1194–1197)
        row.setHvCustomerCsReviewDate(toDateInt(
                parseIntField(commArea.getCommCsReviewYear()),
                parseIntField(commArea.getCommCsReviewMonth()),
                parseIntField(commArea.getCommCsReviewDay())));

        log.debug("populateHostCustomerRow: complete — dob={} createDate={} csReviewDate={}",
                row.getHvCustomerDob(), row.getHvCustomerCreateDate(), row.getHvCustomerCsReviewDate());
    }

    // -----------------------------------------------------------------------
    // Story 7.2 — JDBC INSERT CUSTOMER (FR-8.3)
    // -----------------------------------------------------------------------

    /**
     * Populates {@code hostCustomerRow} and executes the JDBC INSERT CUSTOMER (17 columns).
     *
     * <p>Implements {@code WRITE-CUSTOMER-DB2 / WCD010} (CRECUST.cbl lines 1138–1270):
     * populates all host variables, executes the INSERT, then handles the result.
     *
     * <p>Silent-return error path (Rule 1, AC-1.2): on {@link SQLException} or
     * {@link NamingException}, calls {@link CustomerNumberService#dequeue} (Fan-In-4 DEQ
     * site #2), sets {@code commSuccess='N'} and {@code commFailCode='1'}, and returns.
     * No ABEND, no {@code Program.link()} on this path.
     *
     * <p>Success path (FR-8.6): sets {@code commSuccess='Y'}, {@code commFailCode=' '},
     * {@code commEyecatcher="CUST"}.
     *
     * @param commArea              the commarea providing customer fields and receiving results
     * @param customerRecord        pre-allocated customer record model (used by populate step)
     * @param hostCustomerRow       pre-allocated host-variable row (populated internally)
     * @param customerNumberService service providing {@link CustomerNumberService#dequeue} for
     *                              Fan-In-4 DEQ site #2 on INSERT failure
     * @param nameResource          the resource-name token returned by ENQ; passed to dequeue
     *                              on INSERT failure
     */
    public void insertCustomer(
            CrecustCommarea commArea,
            CustomerRecord customerRecord,
            HostCustomerRow hostCustomerRow,
            CustomerNumberService customerNumberService,
            String nameResource) {

        // Step 1 — Populate host variable row from commarea (WCD010 lines 1161–1197)
        populateHostCustomerRow(commArea, customerRecord, hostCustomerRow);

        // Step 2/3 — Get DataSource + execute INSERT with 17 bound parameters (NFR-4 try-with-resources)
        try {
            try (Connection conn = dbAdapter.getConnection(DATASOURCE_JNDI_NAME);
                 PreparedStatement ps = dbAdapter.prepareStatement(conn, INSERT_CUSTOMER_SQL)) {

                // Bind all 17 parameters in PE-3 column order (CRECUST.cbl lines 1239–1255)
                // 1: CUSTOMER_EYECATCHER — HV-CUSTOMER-EYECATCHER PIC X(4)
                ps.setString(1,  hostCustomerRow.getHvCustomerEyecatcher());
                // 2: CUSTOMER_SORTCODE — HV-CUSTOMER-SORTCODE PIC X(6)
                ps.setString(2,  hostCustomerRow.getHvCustomerSortcode());
                // 3: CUSTOMER_NUMBER — HV-CUSTOMER-NUMBER PIC X(10)
                ps.setString(3,  hostCustomerRow.getHvCustomerNumber());
                // 4: CUSTOMER_TITLE — HV-CUSTOMER-TITLE PIC X(10)
                ps.setString(4,  hostCustomerRow.getHvCustomerTitle());
                // 5: CUSTOMER_FIRST_NAME — HV-CUSTOMER-FIRST-NAME PIC X(50)
                ps.setString(5,  hostCustomerRow.getHvCustomerFirstName());
                // 6: CUSTOMER_LAST_NAME — HV-CUSTOMER-LAST-NAME PIC X(50)
                ps.setString(6,  hostCustomerRow.getHvCustomerLastName());
                // 7: CUSTOMER_DATE_OF_BIRTH — HV-CUSTOMER-DOB S9(9) COMP → INTEGER
                ps.setInt(7,     hostCustomerRow.getHvCustomerDob());
                // 8: CUSTOMER_PHONE — HV-CUSTOMER-PHONE PIC X(20)
                ps.setString(8,  hostCustomerRow.getHvCustomerPhone());
                // 9: CUSTOMER_ADDR_LINE1 — HV-CUSTOMER-ADDR-LINE1 PIC X(50)
                ps.setString(9,  hostCustomerRow.getHvCustomerAddrLine1());
                // 10: CUSTOMER_ADDR_LINE2 — HV-CUSTOMER-ADDR-LINE2 PIC X(50)
                ps.setString(10, hostCustomerRow.getHvCustomerAddrLine2());
                // 11: CUSTOMER_CITY — HV-CUSTOMER-CITY PIC X(50)
                ps.setString(11, hostCustomerRow.getHvCustomerCity());
                // 12: CUSTOMER_POSTCODE — HV-CUSTOMER-POSTCODE PIC X(10)
                ps.setString(12, hostCustomerRow.getHvCustomerPostcode());
                // 13: CUSTOMER_COUNTRY — HV-CUSTOMER-COUNTRY PIC X(50)
                ps.setString(13, hostCustomerRow.getHvCustomerCountry());
                // 14: CUSTOMER_STATUS — HV-CUSTOMER-STATUS PIC X(10)
                ps.setString(14, hostCustomerRow.getHvCustomerStatus());
                // 15: CUSTOMER_CREATED_DATE — HV-CUSTOMER-CREATE-DATE S9(9) COMP → INTEGER
                ps.setInt(15,    hostCustomerRow.getHvCustomerCreateDate());
                // 16: CUSTOMER_CREDIT_SCORE — HV-CUSTOMER-CREDIT-SCORE S9(4) COMP → SMALLINT
                ps.setShort(16,  hostCustomerRow.getHvCustomerCreditScore());
                // 17: CUSTOMER_CS_REVIEW_DATE — HV-CUSTOMER-CS-REVIEW-DATE S9(9) COMP → INTEGER
                ps.setInt(17,    hostCustomerRow.getHvCustomerCsReviewDate());

                dbAdapter.executeUpdate(ps);
            }
        } catch (SQLException | NamingException e) {
            // Silent-return path (Rule 1, AC-1.2) — COBOL: IF SQLCODE NOT = 0 (lines 1261–1268)
            // MOVE 'N' TO COMM-SUCCESS, MOVE '1' TO COMM-FAIL-CODE, PERFORM DEQ-NAMED-COUNTER
            log.error("CustomerDbService.insertCustomer() — INSERT CUSTOMER failed", e);
            // Fan-In-4 DEQ site #2 (PERFORM DEQ-NAMED-COUNTER — CRECUST.cbl line 1267)
            customerNumberService.dequeue(commArea, nameResource);
            commArea.setCommSuccess("N");
            commArea.setCommFailCode(CrecustException.FAIL_CODE_INSERT_CUSTOMER);
            return;
        }

        // Step 4 — Success path (FR-8.6): COBOL falls through to success after INSERT
        // MOVE 'Y' TO COMM-SUCCESS, MOVE ' ' TO COMM-FAIL-CODE, MOVE 'CUST' TO COMM-EYECATCHER
        commArea.setCommSuccess("Y");
        commArea.setCommFailCode(" ");
        commArea.setCommEyecatcher("CUST");

        log.debug("CustomerDbService.insertCustomer() — INSERT CUSTOMER success; "
                + "sortcode={} number={}", commArea.getCommSortcode(), commArea.getCommNumber());
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Encodes a calendar date as a YYYYMMDD integer.
     *
     * <p>Formula: {@code (year * 10000) + (month * 100) + day} — identical to the three
     * COBOL {@code COMPUTE} statements in {@code WCD010} (lines 1167–1197).
     *
     * @param year  4-digit year
     * @param month 2-digit month (1–12)
     * @param day   2-digit day (1–31)
     * @return YYYYMMDD integer
     */
    private static int toDateInt(int year, int month, int day) {
        return (year * 10000) + (month * 100) + day;
    }

    /**
     * Parses a COBOL display-numeric field (PIC 99 / PIC 9999 / PIC 999 stored as {@code String})
     * to {@code int}. Returns {@code 0} for null or blank input.
     *
     * @param s the string to parse (may be null or whitespace-only)
     * @return parsed integer value, or {@code 0} if blank/null
     */
    private static int parseIntField(String s) {
        if (s == null) {
            return 0;
        }
        String trimmed = s.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(trimmed);
    }
}
