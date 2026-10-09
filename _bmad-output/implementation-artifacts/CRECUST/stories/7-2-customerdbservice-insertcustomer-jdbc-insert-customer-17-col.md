---
title: 'Story 7.2: CustomerDbService.insertCustomer() — JDBC INSERT CUSTOMER 17 columns (FR-8.3)'
type: 'feature'
created: '2026-10-01'
status: 'review'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '_bmad-output/implementation-artifacts/CRECUST/epic-7-context.md'
  - '_bmad-output/planning-artifacts/CRECUST/transformation-rulebook.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Db2 persistence for newly created customers requires executing an INSERT with 17 specific columns in exact PE-3 schema order, managing JDBC connections safely, and handling SQL errors via the silent-return error path without abending.

**Approach:** Implement `insertCustomer(CrecustCommarea commarea, CustomerRecord customerRecord, HostCustomerRow hostCustomerRow)` on `CustomerDbService` using a JDBC `PreparedStatement` with 17 parameter placeholders, JNDI DataSource `jdbc/crecustDB2DS`, `try-with-resources`, success commarea updates, and silent-return error handling (calling `customerNumberService.dequeue(commarea)` and setting fail code `'1'`).

## Boundaries & Constraints

**Always:**
- Keep all 17 columns in exact PE-3 logical order (`EYECATCHER`, `SORTCODE`, `NUMBER`, `TITLE`, `FIRST_NAME`, `LAST_NAME`, `DOB`, `PHONE`, `ADDR_LINE1`, `ADDR_LINE2`, `CITY`, `POSTCODE`, `COUNTRY`, `STATUS`, `CREATED_DATE`, `CREDIT_SCORE`, `CS_REVIEW_DATE`) matching 17 `?` parameter markers (Rule 7).
- Manage JDBC `Connection` and `PreparedStatement` within `try-with-resources` blocks (NFR-4).
- On SQL error (`SQLException`): call `customerNumberService.dequeue(commarea)`, set `commarea.setCommSuccess('N')`, set `commarea.setCommFailCode(CrecustException.FAIL_CODE_INSERT_CUSTOMER)` (`'1'`), and return silently (Rule 1, silent-return).
- On SQL success: set `commarea.setCommSuccess('Y')`, set `commarea.setCommFailCode(' ')`, and set `commarea.setCommEyecatcher("CUST")`.
- Keep `CustomerDbService` stateless in package `com.ibm.cics.botz.crecust.service` (NFR-5).

**Never:**
- Never issue `Program.link("ABNDPROC")` or throw an unhandled exception / ABEND on `insertCustomer` SQL failure (Rule 1, silent-return contract).
- Never use `@Resource` injection for `DataSource`; use JNDI lookup `jdbc/crecustDB2DS` (ADR-4).
- Never drop, reorder, or alter columns from the 17-column INSERT specification.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Successful DB2 Insert | Valid `HostCustomerRow`, active DB2 connection | 1 row inserted; `commSuccess='Y'`, `commFailCode=' '`, `commEyecatcher="CUST"` | N/A |
| SQL Error during INSERT | `SQLException` thrown by `PreparedStatement.executeUpdate()` | `customerNumberService.dequeue(commarea)` invoked once; `commSuccess='N'`, `commFailCode='1'`; returns silently | Catch `SQLException`, call dequeue, update commarea, return |

</frozen-after-approval>

## Code Map

- `src/main/java/com/ibm/cics/botz/crecust/service/CustomerDbService.java` -- Contains `insertCustomer` method and JDBC SQL INSERT logic.
- `src/main/java/com/ibm/cics/botz/crecust/service/CustomerNumberService.java` -- Provides `dequeue(commarea)` to release customer number resource on failure.
- `src/main/java/com/ibm/cics/botz/crecust/model/HostCustomerRow.java` -- DTO holding the 17 mapped host variable fields.
- `src/main/java/com/ibm/cics/botz/crecust/model/CrecustCommarea.java` -- Target for status and fail code updates.
- `src/main/java/com/ibm/cics/botz/crecust/exception/CrecustException.java` -- Source of constant `FAIL_CODE_INSERT_CUSTOMER` (`'1'`).
- `src/test/java/com/ibm/cics/botz/crecust/service/CustomerDbServiceTest.java` -- Unit tests verifying 17-column parameter binding, success updates, and SQL failure silent-return handling.

## Tasks & Acceptance

**Execution:**
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/CustomerDbService.java` -- Add `insertCustomer(...)` method with 17-column JDBC INSERT, JNDI lookup, and silent-return error handling.
- [ ] `src/test/java/com/ibm/cics/botz/crecust/service/CustomerDbServiceTest.java` -- Create unit tests for success path and `SQLException` silent-return failure path.

**Acceptance Criteria:**
- Given a populated `HostCustomerRow` and a mock JDBC `DataSource`, when `insertCustomer(commarea, customerRecord, hostCustomerRow)` is called, then a `PreparedStatement` with exactly 17 `?` placeholders is executed in PE-3 column order.
- Given a successful SQL execution, then `commarea.getCommSuccess()` is `'Y'`, `commarea.getCommFailCode()` is `' '`, and `commarea.getCommEyecatcher()` is `"CUST"`.
- Given a JDBC `SQLException` during insertion, then `customerNumberService.dequeue(commarea)` is called once, `commarea.getCommSuccess()` is `'N'`, `commarea.getCommFailCode()` is `'1'`, no ABEND or exception is thrown, and `Program.link("ABNDPROC")` is never invoked.

## Implementation Notes

### Dev Notes (Story 7.2 — 2026-10-09)

**Implementation summary:**

- Added `FAIL_CODE_INSERT_CUSTOMER = "1"` constant to `CrecustException` (COBOL source: `MOVE '1' TO COMM-FAIL-CODE`, line 1266).
- Implemented `CustomerDbService.insertCustomer(CrecustCommarea, CustomerRecord, HostCustomerRow, CustomerNumberService, NameResource)` — the stub signature was extended with the two Fan-In-4 DEQ parameters required on the silent-return error path.
- SQL constant `INSERT_CUSTOMER_SQL` uses the exact 17 column names verified from CRECUST.cbl lines 1221–1237: `CUSTOMER_EYECATCHER, CUSTOMER_SORTCODE, CUSTOMER_NUMBER, CUSTOMER_TITLE, CUSTOMER_FIRST_NAME, CUSTOMER_LAST_NAME, CUSTOMER_DATE_OF_BIRTH, CUSTOMER_PHONE, CUSTOMER_ADDR_LINE1, CUSTOMER_ADDR_LINE2, CUSTOMER_CITY, CUSTOMER_POSTCODE, CUSTOMER_COUNTRY, CUSTOMER_STATUS, CUSTOMER_CREATED_DATE, CUSTOMER_CREDIT_SCORE, CUSTOMER_CS_REVIEW_DATE`.
- Parameter binding verified against COBOL host-variable types: params 1–6, 8–14 are `setString`; param 7 (`DATE_OF_BIRTH`) and params 15, 17 (`CREATED_DATE`, `CS_REVIEW_DATE`) are `setInt` (S9(9) COMP → INTEGER); param 16 (`CREDIT_SCORE`) is `setShort` (S9(4) COMP → SMALLINT).
- Silent-return error path (Rule 1, AC-1.2): catches `SQLException | NamingException`, calls `customerNumberService.dequeue(commArea, nameResource)` (Fan-In-4 DEQ site #2), sets `commSuccess='N'`, `commFailCode=FAIL_CODE_INSERT_CUSTOMER`. No ABEND, no `Program.link()`.
- Success path sets `commSuccess='Y'`, `commFailCode=' '`, `commEyecatcher="CUST"`.
- `CrecustService.execute()` Step 7 call site updated: now passes `customerNumberService` and `nameResource` to match the expanded signature.
- `mvn compile` → BUILD SUCCESS.

**Fan-In-4 DEQ sites after this story:**
- Site 1 ✅ Story 6-2: CONTROL SQL failure (`CustomerNumberService.failControlSql`)
- Site 2 ✅ This story: INSERT CUSTOMER failure (`CustomerDbService.insertCustomer`)
- Site 3 ⏳ Story 8-2: PROCTRAN notifying-abort
- Site 4 ✅ Story 8-1: `CrecustService` success path

## Spec Change Log

## Review Triage Log

## Design Notes

The SQL string for `CUSTOMER` insert follows:
```sql
INSERT INTO CUSTOMER (
    EYECATCHER, SORTCODE, NUMBER, TITLE, FIRST_NAME, LAST_NAME,
    DOB, PHONE, ADDR_LINE1, ADDR_LINE2, CITY, POSTCODE,
    COUNTRY, STATUS, CREATED_DATE, CREDIT_SCORE, CS_REVIEW_DATE
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
```

Parameter index mapping:
1: `hostCustomerRow.getHvCustomerEyecatcher()` (String)
2: `hostCustomerRow.getHvCustomerSortcode()` (int)
3: `hostCustomerRow.getHvCustomerNumber()` (int)
4: `hostCustomerRow.getHvCustomerTitle()` (String)
5: `hostCustomerRow.getHvCustomerFirstName()` (String)
6: `hostCustomerRow.getHvCustomerLastName()` (String)
7: `hostCustomerRow.getHvCustomerDob()` (int)
8: `hostCustomerRow.getHvCustomerPhone()` (String)
9: `hostCustomerRow.getHvCustomerAddrLine1()` (String)
10: `hostCustomerRow.getHvCustomerAddrLine2()` (String)
11: `hostCustomerRow.getHvCustomerCity()` (String)
12: `hostCustomerRow.getHvCustomerPostcode()` (String)
13: `hostCustomerRow.getHvCustomerCountry()` (String)
14: `hostCustomerRow.getHvCustomerStatus()` (String)
15: `hostCustomerRow.getHvCustomerCreateDate()` (int)
16: `hostCustomerRow.getHvCustomerCreditScore()` (short)
17: `hostCustomerRow.getHvCustomerCsReviewDate()` (int)

## Verification

**Commands:**
- `mvn test -Dtest=CustomerDbServiceTest` -- expected: Tests pass with 100% assertions for success and SQL failure paths.
