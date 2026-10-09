---
title: "PRD: CRECUST COBOL-to-Java Modernisation"
status: draft
created: 2026-10-01
updated: 2026-10-01
---

## Pre-read

See [Transformation Rulebook](transformation-rulebook.md) for the fidelity goals that shaped this PRD.




### Program Summary

`CRECUST` is an Enterprise COBOL CICS-linked program that creates a new bank customer record. It
receives customer data in a 399-byte DFHCOMMAREA from a calling CICS transaction, validates the
data, performs an asynchronous multi-agency credit-score check, assigns a sequential customer
number from a DB2 CONTROL table (protected by a CICS Named Counter ENQ/DEQ), inserts the customer
row into the DB2 CUSTOMER table, writes a transaction audit record to the DB2 PROCTRAN table, and
returns success or a fail-code to the caller via the same commarea.

### Modernisation Goal

Rewrite `CRECUST` in Java 21, running under JCICS, preserving all business logic, all DB2 data
contracts, and all CICS interoperability contracts exactly. The Java program must be callable by
the same CICS callers using the same 399-byte commarea, and must continue to interoperate with the
five OCR1–OCR5 COBOL child transactions and the ABNDPROC COBOL abend-handler program — all of
which remain in CICS COBOL unchanged.

### Source Program

| Attribute | Value |
|---|---|
| Program name | `CRECUST` |
| Source language | Enterprise COBOL for z/OS |
| Middleware | CICS (EXEC CICS), Db2 for z/OS (EXEC SQL) |
| Entry point | `PROCEDURE DIVISION USING DFHCOMMAREA` |
| Paragraphs | 12 sections (plus GET-ME-OUT-OF-HERE) |
| Variables | 419 total; 89 top-level 01/77 items |
| Copybooks | 9: SORTCODE, CUSTDB2, PROCDB2, CONTDB2, PROCTRAN, ABNDINFO, CUSTOMER, CUSTCTRL, CRECUST |
| DB2 tables | 3: CUSTOMER (INSERT), PROCTRAN (INSERT), CONTROL / schema STTESTER (SELECT + UPDATE) |
| External programs | ABNDPROC (CICS LINK), OCR1–OCR5 (CICS RUN TRANSID via COBOL helper) |

### Product Owner Decisions

| Decision | Choice |
|---|---|
| Runtime subsystem | CICS — modernised program runs under JCICS |
| DB2 tables | Retain all three on Db2 for z/OS; Java uses JDBC |
| OCR1–OCR5 child transactions | Keep as CICS COBOL; Java calls COBOL helper via `Program.link()` |
| ABNDPROC | Keep as CICS COBOL; Java calls via `Program.link()` |
| Customer-number ENQ/DEQ | Use JCICS `NameResource.enqueue()` / `NameResource.dequeue()` APIs |
| LE Runtime calls (CEEDAYS/CEELOCT) | Replace with equivalent `java.time` computations |


## Overview

## Goals and Success Metrics

### Modernisation Goals

| # | Goal |
|---|---|
| G-1 | The modernised Java program is functionally equivalent to the COBOL source: same inputs, same outputs, same fail codes, same DB2 writes, same CICS interactions. |
| G-2 | The 399-byte DFHCOMMAREA contract is preserved exactly; existing CICS callers require no changes. |
| G-3 | The program continues to interoperate with OCR1–OCR5 CICS COBOL child transactions and ABNDPROC with no change to those programs. |
| G-4 | All DB2 table schemas, column order, and data encodings are preserved (including YYYYMMDD integer encoding for date columns). |
| G-5 | The Java source follows the Java Batch enterprise coding standards and is maintainable by Java developers without COBOL knowledge. |
| G-6 | All 18 fidelity rules from the COBOL-to-Java transform rulebook are satisfied; every rule maps to at least one testable acceptance criterion. |

### Success Metrics

| Metric | Target |
|---|---|
| All functional acceptance criteria pass | 100% |
| COMM-SUCCESS and COMM-FAIL-CODE values match COBOL for all test scenarios | 100% |
| INSERT CUSTOMER row in DB2 matches expected byte layout | 100% |
| INSERT PROCTRAN row in DB2 matches expected column values and encoding | 100% |
| No commarea fields dropped or shifted (byte offset regression test) | 0 regressions |
| No COBOL callers or OCR/ABNDPROC programs require modification | 0 changes to COBOL side |



## User Configuration

| Setting | Value |
|---|---|
| App Architecture | `cics_batch` |
| Build Tool | Maven |
| Java Version | 21 |
| Database Connection | JDBC |
| Lombok | Enabled |

> **Serializer note:** Although serializers are disabled in the generator configuration, the CICS
> commarea and container byte-array contracts require manually authored serializer classes.
> See the Serialization Strategy section.



## Functional Requirements

### FR-1: Program Entry and Commarea Binding

FR-1.1 The Java class implements `com.ibm.cics.server.AbstractProgram` (or equivalent JCICS
entry-point mechanism) and receives the 399-byte `DFHCOMMAREA` on entry.

FR-1.2 The commarea is immediately deserialized into a `CrecustCommarea` Java object using the
canonical `CrecustareaSerializer` (the single authoritative deserializer for this layout).

FR-1.3 On completion (success or any fail-code path), `CrecustareaSerializer` serializes the
updated `CrecustCommarea` back into the commarea byte array before `EXEC CICS RETURN`.

FR-1.4 The program returns to its caller by returning from the `main()` method — Java's
`return` statement is the equivalent of `EXEC CICS RETURN`; no JCICS `returnToCaller()` API
call is made. This return occurs from a single shared exit point, mirroring `GET-ME-OUT-OF-HERE`.

---

### FR-2: Title Validation (BR-1)

FR-2.1 `COMM-TITLE` (10 bytes, right-padded with spaces) must be exactly one of the 11 accepted
values: `'Professor '`, `'Mr        '`, `'Mrs       '`, `'Miss      '`, `'Ms        '`,
`'Dr        '`, `'Drs       '`, `'Lord      '`, `'Sir       '`, `'Lady      '`,
`'          '` (all spaces).

FR-2.2 On title validation failure: set `COMM-SUCCESS='N'`, `COMM-FAIL-CODE='T'`, and return
immediately (no ENQ has been taken at this point — no DEQ is needed).

FR-2.3 The accepted title set is declared as a named constant collection; magic strings are
prohibited.

---

### FR-3: Date-of-Birth Validation (BR-4)

FR-3.1 If `COMM-DOB-YEAR < 1601`, set `COMM-SUCCESS='N'`, `COMM-FAIL-CODE='O'`, and return.

FR-3.2 Convert the DOB components (day, month, year) to a `LocalDate` using `java.time`
(replacing the `CEEDAYS` LE call). Construct the Lilian-equivalent day count using
`LocalDate.toEpochDay()` adjusted for the Lilian epoch offset (difference between
15 Oct 1582 and 1 Jan 1970) to replicate CEEDAYS semantics.

FR-3.3 If the `LocalDate` construction fails (invalid date), set `COMM-SUCCESS='N'`,
`COMM-FAIL-CODE='Z'`, and return.

FR-3.4 Obtain today's date using `LocalDate.now()` (replacing `CEELOCT`). Compute approximate
customer age as `today.getYear() - COMM-DOB-YEAR`. If age > 150, set `COMM-SUCCESS='N'`,
`COMM-FAIL-CODE='O'`, and return.

FR-3.5 If the DOB is in the future (today's Lilian day < DOB Lilian day), set
`COMM-SUCCESS='N'`, `COMM-FAIL-CODE='Y'`, and return.

FR-3.6 The minimum DOB year constant (`1601`), maximum age constant (`150`), and Lilian epoch
offset must be declared as named `private static final int` constants.

---

### FR-4: Populate Time and Date (POPULATE-TIME-DATE_PTD010)

FR-4.1 Obtain the current date and time using `LocalDateTime.now()` (Java native replacement
for `EXEC CICS ASKTIME`). No JCICS ASKTIME API is called; `Task.getTask().getAbstime()` does
not exist in the JCICS API.

FR-4.2 Format the current date as `DD/MM/YYYY` using `DateTimeFormatter.ofPattern("dd/MM/yyyy")`
to populate `WS-ORIG-DATE` (10-char string). This replaces `EXEC CICS FORMATTIME DDMMYYYY DATESEP('/')`.

FR-4.3 Format the current time as `HHMMSS` using `DateTimeFormatter.ofPattern("HHmmss")` to
populate the PROCTRAN time field. This replaces `EXEC CICS FORMATTIME TIME(PROC-TRAN-TIME)`.

FR-4.4 Populate `WS-ORIG-DATE-GRP-X` (the dot-separated display version `DD.MM.YYYY`) by
copying the day, month, and year components from `WS-ORIG-DATE-GRP` with `'.'` separators.

---

### FR-5: Asynchronous Credit-Check (BR-2, CREDIT-CHECK_CC010)

FR-5.1 `CreditCheckService` implements the full JCICS async credit-check loop directly
(ADR-5 — native `AsyncServiceImpl` pattern). The five CICS async commands map as follows:
- `EXEC CICS RUN TRANSID` → `AsyncServiceImpl.runTransactionId(tranId, channel)`
- `EXEC CICS FETCH ANY NOSUSPEND` → `AsyncServiceImpl.getAny(BlockingAction.NOSUSPEND)`
- `EXEC CICS PUT CONTAINER` → `Channel.createContainer().put()`
- `EXEC CICS GET CONTAINER` → `Channel.getContainer().get()`
- `EXEC CICS DELAY FOR SECONDS(3)` → `Thread.sleep(3000)` (Java native; `Task.getTask().delay()` does not exist in JCICS)

No COBOL helper shim (`CRECUSTCC`) is generated; no `Program.link()` is used for credit-check
processing.

FR-5.2 `CreditCheckService` sets `COMM-SUCCESS='N'` and `COMM-FAIL-CODE` directly on the
commarea object for all failure paths. If `COMM-SUCCESS='N'` on return, `CrecustService`
treats this as a credit-check failure and falls through to the credit-error handling block.

FR-5.3 If zero credit-check agencies replied in time, `COMM-FAIL-CODE='C'` is set by
`CreditCheckService`; the orchestrator treats this as a credit-check failure.

FR-5.4 The fail codes (`'A'` PUT CONTAINER, `'B'` RUN TRANSID, `'C'` NOTFINISHED, `'D'`
INVREQ, `'E'` GET CONTAINER, `'F'` ABEND, `'G'` SECERROR, `'H'` OTHER) are set directly
in `COMM-FAIL-CODE` by `CreditCheckService` and returned to the original caller on failure.

FR-5.5 The credit score average (`WS-TOTAL-CS-SCR / WS-RETRIEVED-CNT`) and review date are
computed by `CreditCheckService` and stored directly in `COMM-CREDIT-SCORE` and
`COMM-CS-REVIEW-DATE` in the commarea.

---

### FR-6: Credit-Score Review Date (BR-3)

FR-6.1 On a successful credit check (at least one agency replied), compute the review date as:
`today + ((21 - 1) * RANDOM(EIBTASKN)) + 1` days, producing a date between today+1 and
today+20 inclusive. Store the result in `COMM-CS-REVIEW-DAY`, `COMM-CS-REVIEW-MONTH`,
`COMM-CS-REVIEW-YEAR`.

FR-6.2 On any credit-check failure, set the review date fields to today's date
(`WS-ORIG-DATE-DD/MM/YYYY` components).

FR-6.3 The `COMM-CS-REVIEW-DATE` group in the commarea must be populated with the DDMMYYYY
byte layout as produced by the COBOL STRING/reference-modification paths (see TRA-5 in the
technical research). On error paths the result is `DDMMYYYY` with no separators; on the
success path the positional reorder of `WS-NEW-REVIEW-YYYYMMDD` produces the same layout.
Both Java paths must produce an identical commarea byte layout.

---

### FR-7: Customer Number Allocation (BR-5)

FR-7.1 Issue a JCICS `NameResource.enqueue()` on the 16-byte resource name
`'BANKZCUST' + SORTCODE + '  '` (9 + 6 + 2 = 17 logical bytes, padded/truncated to 16
bytes for the ENQ) before any customer-number read. If ENQ fails, set `COMM-SUCCESS='N'`,
`COMM-FAIL-CODE='3'`, and return.

FR-7.2 Execute a JDBC `SELECT CONTROL_VALUE_NUM FROM STTESTER.CONTROL WHERE CONTROL_NAME = ?`
with `HV-CONTROL-NAME` as the bind parameter. On SQL failure, issue `NameResource.dequeue()`, set
`COMM-SUCCESS='N'`, `COMM-FAIL-CODE='4'`, and return.

FR-7.3 Increment the retrieved value by 1 and execute a JDBC
`UPDATE STTESTER.CONTROL SET CONTROL_VALUE_NUM = ? WHERE CONTROL_NAME = ?`. On SQL failure,
issue `NameResource.dequeue()`, set `COMM-SUCCESS='N'`, `COMM-FAIL-CODE='4'`, and return.

FR-7.4 Push the new customer number to: `COMM-NUMBER`, `CUSTOMER-NUMBER` (in the customer
record), `REQUIRED-CUST-NUMBER2`, and the NCS value field — exactly as in the COBOL source.

FR-7.5 `NameResource.dequeue()` is called from exactly four sites (mirroring Fan-In-4 of
`DEQ-NAMED-COUNTER`): after successful PROCTRAN write, after INSERT CUSTOMER failure, after
INSERT PROCTRAN failure (before ABEND), and after SELECT/UPDATE CONTROL failure.

---

### FR-8: Insert Customer Record (WRITE-CUSTOMER-DB2_WCD010)

FR-8.1 Populate `OUTPUT-DATA` / `CUSTOMER-RECORD` from commarea fields.

FR-8.2 Populate `HOST-CUSTOMER-ROW` from `CUSTOMER-RECORD` fields. All three date columns
(`HV-CUSTOMER-DOB`, `HV-CUSTOMER-CREATE-DATE`, `HV-CUSTOMER-CS-REVIEW-DATE`) must be encoded
as `(YYYY * 10000) + (MM * 100) + DD` integers.

FR-8.3 Execute JDBC `INSERT INTO CUSTOMER` with all 17 columns in the order specified in
PE-3 of the technical research. No column may be omitted.

FR-8.4 Set `HV-CUSTOMER-EYECATCHER = 'CUST'` before the INSERT.

FR-8.5 On SQL failure: call `NameResource.dequeue()`, set `COMM-SUCCESS='N'`, `COMM-FAIL-CODE='1'`,
and return (silent-return path — no ABND link, no ABEND).

FR-8.6 On success: set `COMM-SUCCESS='Y'`, `COMM-FAIL-CODE=' '` (single space), and set
`COMM-EYECATCHER = 'CUST'`.

---

### FR-9: Write PROCTRAN Audit Record (WRITE-PROCTRAN-DB2_WPD010, BR-6)

FR-9.1 Issue `EXEC CICS ASKTIME` + `EXEC CICS FORMATTIME` to obtain the current date and
time for `HV-PROCTRAN-DATE` (`DD.MM.YYYY`) and `HV-PROCTRAN-TIME` (`HHMMSS`).

FR-9.2 Assemble `HV-PROCTRAN-DESC` (40 bytes) using exact byte-offset reference modification:
- bytes 1–6: `STORED-SORTCODE`
- bytes 7–16: `STORED-CUSTNO`
- bytes 17–30: `STORED-NAME` (14 bytes)
- bytes 31–40: `STORED-DOB` (10 bytes, `DD/MM/YYYY`)

FR-9.3 Set `HV-PROCTRAN-TYPE = 'OCC'`, `HV-PROCTRAN-AMOUNT = ZEROS`,
`HV-PROCTRAN-ACC-NUMBER = ZEROS`, `HV-PROCTRAN-REF` = EIBTASKN formatted as 12-character
string, `HV-PROCTRAN-EYECATCHER = 'PRTR'`.

FR-9.4 Execute JDBC `INSERT INTO PROCTRAN` with all 9 columns in the order specified in PE-3
of the technical research. `PROCTRAN_DATE` receives the `DD.MM.YYYY` string.

FR-9.5 On SQL failure — **notifying-abort path** (Rule 1):
  1. Populate `ABNDINFO-REC` fields (ABND-UTIME-KEY, ABND-APPLID, ABND-TRANID, ABND-DATE,
     ABND-TIME, ABND-CODE = `'HWPT'`, ABND-PROGRAM, ABND-RESPCODE, ABND-RESP2CODE,
     ABND-SQLCODE, ABND-FREEFORM).
  2. Call `NameResource.dequeue()` (release ENQ before ABEND).
  3. Call `Program.link()` to `ABNDPROC` with `ABNDINFO-REC` as commarea.
  4. **Only after steps 1–3 complete:** throw the fatal exception / call
     `Task.getTask().abend("HWPT")`.
  Reversing this order is a functional defect.

---

### FR-10: CICS Level Check

FR-10.1 The program checks the CICS version (`WS-CICSTSLEVEL`) on entry and stores the
version/release/maintenance level in `WS-CICSTS-LEVEL-NUM-GRP` (VV/RR/MM split via Cat 3a
REDEFINES). This check must be preserved in the Java equivalent.

---

### FR-11: Fail Code Inventory

All 17 distinct fail-code values must be declared as named constants. The `'G'` code is
used in two distinct contexts (post-credit-check error in PREMIERE, and SECERROR in
CREDIT-CHECK); both usages must set `COMM-SUCCESS='N'`.

| Constant Name | Value | Condition |
|---|---|---|
| `FAIL_CODE_INVALID_TITLE` | `'T'` | Title not in accepted list |
| `FAIL_CODE_CREDIT_ERROR` | `'G'` | Post-CC error (PREMIERE) or SECERROR (CC) |
| `FAIL_CODE_PUT_CONTAINER` | `'A'` | PUT CONTAINER failed |
| `FAIL_CODE_RUN_TRANSID` | `'B'` | RUN TRANSID failed |
| `FAIL_CODE_CC_NOTFINISHED` | `'C'` | FETCH ANY NOTFINISHED, no data |
| `FAIL_CODE_CC_INVREQ` | `'D'` | FETCH ANY INVREQ (no children) |
| `FAIL_CODE_GET_CONTAINER` | `'E'` | GET CONTAINER failed |
| `FAIL_CODE_CC_ABEND` | `'F'` | FETCH ANY completion = ABEND |
| `FAIL_CODE_CC_OTHER` | `'H'` | FETCH ANY completion = OTHER |
| `FAIL_CODE_INSERT_CUSTOMER` | `'1'` | INSERT CUSTOMER failed |
| `FAIL_CODE_ENQ` | `'3'` | ENQ failed |
| `FAIL_CODE_CONTROL_SQL` | `'4'` | SELECT or UPDATE CONTROL failed |
| `FAIL_CODE_DEQ` | `'5'` | DEQ failed |
| `FAIL_CODE_DOB_RANGE` | `'O'` | DOB year < 1601 or age > 150 |
| `FAIL_CODE_DOB_FUTURE` | `'Y'` | DOB is in the future |
| `FAIL_CODE_CEEDAYS_FAIL` | `'Z'` | CEEDAYS/LocalDate construction failed |
| `FAIL_CODE_SUCCESS` | `' '` | Success (single space) |



## Non-Functional Requirements

### NFR-1: Java Version and Build

NFR-1.1 Target Java 21; use language features available in Java 21 (records, pattern matching,
text blocks, sealed interfaces where appropriate).

NFR-1.2 Build tooling: Maven. The `pom.xml` must declare `java.version=21` and the JCICS
dependency (`com.ibm.cics:com.ibm.cics.server`).

NFR-1.3 Lombok is enabled; use `@Data`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor`
where appropriate on data-model classes.

---

### NFR-2: Coding Standards

NFR-2.1 All classes, methods, and variables follow the Java Batch enterprise coding standards:
PascalCase classes, camelCase methods and variables, UPPER_SNAKE_CASE constants.

NFR-2.2 Methods must not exceed 40 lines; extract complex logic into focused helper methods.

NFR-2.3 One top-level type per `.java` file (Rule 17); no multiple top-level types in a
single file.

NFR-2.4 No magic numbers or magic strings; all constants declared as named
`private static final` fields.

NFR-2.5 SLF4J logging: one `private static final Logger` per class. `INFO` for milestones,
`DEBUG` for per-record detail, `ERROR` for failures with exception context.

NFR-2.6 No sensitive data (PII, financial values) logged at any level.

---

### NFR-3: Exception Handling

NFR-3.1 `CrecustException` is the sole exception class; it carries 17 named fail-code
constants, one `ABEND_CODE_HWPT` constant, and 16 named static factory methods. Each factory
method accepts a `char failCode` internally and stores it as `private final char failCode`;
`getFailCode()` exposes it. Every catch block in a service class calls
`commarea.setCommFailCode(e.getFailCode())` and `commarea.setCommSuccess('N')` — the commarea
is mutated by the catch block, not by the factory method. No injected or separately instantiated
exception-handler class is generated; there is no `*ExceptionHandler*` or `*ErrorHandler*`
class of any kind (Rule 16, ADR-10).

NFR-3.2 The PROCTRAN SQL failure path is the only notifying-abort path; all other error paths
are silent-return paths (Rule 1).

NFR-3.3 The CICS ABEND code `'HWPT'` must be declared as a named constant in the exception
class and passed to `Task.getTask().abend()` (Rule 4).

NFR-3.4 The `linkAbndproc()` delegate method returns `void`; no result DTO (Rule 10 — return
code from ABNDPROC is not inspected in the COBOL source).

---

### NFR-4: Resource Management

NFR-4.1 All JDBC resources (`Connection`, `PreparedStatement`, `ResultSet`) are managed via
`try-with-resources`.

NFR-4.2 `NameResource.enqueue()` must be released in all code paths that acquired it (four DEQ call
sites — see FR-7.5).

---

### NFR-5: Stateless Design

NFR-5.1 The service and business-logic classes must not hold per-request state in instance
fields. All context is passed as method parameters or held in local variables.

NFR-5.2 Named constants (chunk sizes, retry limits, commit intervals) are declared as static
finals, not hardcoded inline.

---

### NFR-6: Byte-Array Contract

NFR-6.1 Every field offset and length in the commarea serializer is declared as a named
`private static final int` constant derived from PE-1 in the technical research.

NFR-6.2 An explicit bounds check precedes every array write: if the target buffer is shorter
than `offset + fieldLength`, throw `IllegalArgumentException` stating the field name and
required minimum length (Rule 5).

NFR-6.3 No field-width constant is redeclared in more than one class (Rule 15 — single
source of truth).



## Data Structures and Java Class Inventory

> Every data structure defined in CRECUST must produce a Java class, regardless of whether the
> current program's procedure division actively uses all its fields. Structures defined here are
> shared contracts consumed by other programs in the suite (Rule 14).

### DS-1: Commarea — `CrecustCommarea` (from CRECUST.cpy, 399 bytes)

**Status:** Cross-program contract. Defined here; consumed by every caller of CRECUST.

| Java Field | Type | Bytes | Direction | Source Field |
|---|---|---|---|---|
| `commEyecatcher` | `String` | 4 | OUT | `COMM-EYECATCHER` |
| `commSortcode` | `String` | 6 | IN | `COMM-SORTCODE` |
| `commNumber` | `String` | 10 | OUT | `COMM-NUMBER` |
| `commTitle` | `String` | 10 | IN | `COMM-TITLE` |
| `commFirstName` | `String` | 50 | IN | `COMM-FIRST-NAME` |
| `commLastName` | `String` | 50 | IN | `COMM-LAST-NAME` |
| `commDobDay` | `int` | 2 | IN | `COMM-DOB-DAY` |
| `commDobMonth` | `int` | 2 | IN | `COMM-DOB-MONTH` |
| `commDobYear` | `int` | 4 | IN | `COMM-DOB-YEAR` |
| `commPhone` | `String` | 20 | IN | `COMM-PHONE` |
| `commAddrLine1` | `String` | 50 | IN | `COMM-ADDR-LINE1` |
| `commAddrLine2` | `String` | 50 | IN | `COMM-ADDR-LINE2` |
| `commCity` | `String` | 50 | IN | `COMM-CITY` |
| `commPostcode` | `String` | 10 | IN | `COMM-POSTCODE` |
| `commCountry` | `String` | 50 | IN | `COMM-COUNTRY` |
| `commStatus` | `String` | 10 | IN | `COMM-STATUS` |
| `commCreatedDay` | `int` | 2 | IN | `COMM-CREATED-DAY` |
| `commCreatedMonth` | `int` | 2 | IN | `COMM-CREATED-MONTH` |
| `commCreatedYear` | `int` | 4 | IN | `COMM-CREATED-YEAR` |
| `commCreditScore` | `int` | 3 | OUT | `COMM-CREDIT-SCORE` |
| `commCsReviewDay` | `int` | 2 | OUT | `COMM-CS-REVIEW-DAY` |
| `commCsReviewMonth` | `int` | 2 | OUT | `COMM-CS-REVIEW-MONTH` |
| `commCsReviewYear` | `int` | 4 | OUT | `COMM-CS-REVIEW-YEAR` |
| `commSuccess` | `char` | 1 | OUT | `COMM-SUCCESS` |
| `commFailCode` | `char` | 1 | OUT | `COMM-FAIL-CODE` |

**Total: 399 bytes.** Serializer: `CrecustareaSerializer` (canonical, single source of truth).

---

### DS-2: Customer Record — `CustomerRecord` (from CUSTOMER.cpy, 397 bytes)

**Status:** Cross-program contract. Defined here; consumed by any program reading/writing the
CUSTOMER DB2 table via this copybook.

| Java Field | Type | Bytes | Notes |
|---|---|---|---|
| `customerEyecatcher` | `String` | 4 | `'CUST'` |
| `customerSortcode` | `String` | 6 | |
| `customerNumber` | `String` | 10 | |
| `customerTitle` | `String` | 10 | |
| `customerFirstName` | `String` | 50 | |
| `customerLastName` | `String` | 50 | |
| `customerDobDay` | `int` | 2 | |
| `customerDobMonth` | `int` | 2 | |
| `customerDobYear` | `int` | 4 | |
| `customerPhone` | `String` | 20 | |
| `customerAddrLine1` | `String` | 50 | |
| `customerAddrLine2` | `String` | 50 | |
| `customerCity` | `String` | 50 | |
| `customerPostcode` | `String` | 10 | |
| `customerCountry` | `String` | 50 | |
| `customerStatus` | `String` | 10 | 88s: ACTIVE, INACTIVE, SUSPENDED |
| `customerCreatedDay` | `int` | 2 | |
| `customerCreatedMonth` | `int` | 2 | |
| `customerCreatedYear` | `int` | 4 | |
| `customerCreditScore` | `int` | 3 | |
| `customerCsReviewDay` | `int` | 2 | |
| `customerCsReviewMonth` | `int` | 2 | |
| `customerCsReviewYear` | `int` | 4 | |

**Total: 397 bytes.**

---

### DS-3: PROCTRAN Area — `ProctranData` (from PROCTRAN.cpy)

**Status:** Cross-program contract. Defined here; consumed by all programs writing to PROCTRAN.

Key fields in the Java class:

| Java Field | Type | Notes |
|---|---|---|
| `procTranEyeCatcher` | `String` | 88: `'PRTR'` |
| `procTranSortCode` | `String` | |
| `procTranNumber` | `String` | |
| `procTranDate` | `int` | YYYYMMDD numeric |
| `procTranTime` | `int` | HHMMSS numeric |
| `procTranRef` | `String` | |
| `procTranType` | `String` | Many 88-level type codes |
| `procTranDesc` | `byte[]` | 40 bytes — REDEFINES base (see DS-3a) |
| `procTranAmount` | `java.math.BigDecimal` | S9(10)V99 COMP-3 |

#### DS-3a: PROC-TRAN-DESC REDEFINES — `ProcTranDescBase` and subclasses (Cat 4a, Pattern B)

`PROC-TRAN-DESC` has 6 REDEFINES siblings (1 PIC + 5 records): XFR, DELACC, CREACC, DELCUS,
CRECUS. The technical research classifies these as **non-mutually exclusive** (multiple views
can be read simultaneously). Per Cat 4a Pattern B:

- `ProcTranDescBase`: abstract class holding a single `byte[]` of 40 bytes.
- `ProcTranDescXfr`: concrete subclass; getters/setters encode/decode from the base `byte[]`.
- `ProcTranDescDelacc`: concrete subclass.
- `ProcTranDescCreacc`: concrete subclass.
- `ProcTranDescDelcus`: concrete subclass.
- `ProcTranDescCrecus`: concrete subclass — **this view is actively written by CRECUST**.

The `PROC-TRAN-EYE-CATCHER` REDEFINES group (`PROC-TRAN-LOGICAL-DELETE-AREA` redefines the
eye-catcher — two records, no PICs) requires a separate mutual-exclusivity analysis; the
architect must determine whether Pattern A or B applies.

#### DS-3b: PROC-TRAN-DATE REDEFINES (Cat 3a)

- `procTranDate` (`int`, YYYYMMDD) is the canonical PIC field.
- `ProcTranDateGrp`: Java class with `year` (`int`), `month` (`int`), `day` (`int`) fields.
- Getter/setter on `ProctranData` exposes the group view over the canonical integer.

#### DS-3c: PROC-TRAN-TIME REDEFINES (Cat 3a)

- `procTranTime` (`int`, HHMMSS) is the canonical PIC field.
- `ProcTranTimeGrp`: Java class with `hours` (`int`), `minutes` (`int`), `seconds` (`int`).
- Getter/setter on `ProctranData` exposes the group view.

---

### DS-4: Host Customer Row — `HostCustomerRow`

DB2 host variable row for CUSTOMER INSERT. All 17 fields (see PE-3 in technical research).
Three date fields (`hvCustomerDob`, `hvCustomerCreateDate`, `hvCustomerCsReviewDate`) are
`int` — encoded as `(YYYY * 10000) + (MM * 100) + DD`. `hvCustomerCreditScore` is `short`
(SMALLINT).

---

### DS-5: Host PROCTRAN Row — `HostProctranRow`

DB2 host variable row for PROCTRAN INSERT. All 9 fields. `hvProctranDate` is `String`
(`DD.MM.YYYY` format); `hvProctranAmount` is `BigDecimal` (ZEROS for customer-create).

---

### DS-6: Host CONTROL Row — `HostControlRow`

DB2 host variable row for CONTROL SELECT/UPDATE. Three fields:
`hvControlName` (`String`, 32), `hvControlValueNum` (`int`), `hvControlValueStr` (`String`, 32).

---

### DS-7: Child Data — `WsChildData` (399 bytes)

**Status:** Cross-program contract. Defined here; consumed by OCR1–OCR5 child transactions
(they produce this layout in a CICS container and CRECUST reads it back).

Mirrors `CustomerRecord` (397 bytes) plus:
- `wsChildSuccess` (`char`, 1)
- `wsChildFailCode` (`char`, 1)

**Total: 399 bytes.** Serializer: `WsChildDataSerializer` (canonical).

---

### DS-8: ABNDINFO Record — `AbndInfoRec` (from ABNDINFO.cpy)

Used to pass abend context to ABNDPROC. Fields:

| Java Field | Type | Bytes |
|---|---|---|
| `abndUtimeKey` | `long` | 8 (S9(15) COMP-3) |
| `abndTasknoKey` | `String` | 4 |
| `abndApplid` | `String` | 8 |
| `abndTranid` | `String` | 4 |
| `abndDate` | `String` | 10 |
| `abndTime` | `String` | 8 |
| `abndCode` | `String` | 4 |
| `abndProgram` | `String` | 8 |
| `abndRespcode` | `String` | 9 |
| `abndResp2code` | `String` | 9 |
| `abndSqlcode` | `String` | 9 |
| `abndFreeform` | `String` | 600 |

Serializer: `AbndInfoRecSerializer` (canonical, passed as commarea to ABNDPROC).

---

### DS-9: Customer Control Record — `CustomerControlRecord` (from CUSTCTRL.cpy)

**Status:** Defined here; consumed by other programs in the suite.

| Java Field | Type | Bytes |
|---|---|---|
| `customerControlEyecatcher` | `String` | 4 — `'CTRL'` |
| `customerControlSortcode` | `String` | 6 |
| `customerControlNumber` | `String` | 10 |
| `numberOfCustomers` | `String` | 10 |
| `lastCustomerNumber` | `String` | 10 |
| `customerControlSuccessFlag` | `char` | 1 |
| `customerControlFailCode` | `char` | 1 |
| (FILLERs) | `byte[]` | 209 — layout padding, must be preserved |

---

### DS-10: Working-Storage REDEFINES Groups (additional Cat 3a structures)

Each of the following Cat 3a groups produces a Java class for the record sibling, with
getter/setter access to the PIC sibling on the parent class:

| Anchor | Record Class | PIC Field |
|---|---|---|
| `WS-TIME-NOW` | `WsTimeNowGrp` (HH/MM/SS int fields) | `wsTimeNow` (`int`) |
| `WS-ORIG-DATE` | `WsOrigDateGrp` (DD/MM/YYYY int fields + FILLER separators) | `wsOrigDate` (`String`) |
| `CUSTOMER-KY2` | `CustomerKy2` (sortCode `String`, custNumber `String`) | `customerKy2Bytes` (`byte[]`) |
| `WS-CICSTSLEVEL` | `WsCicstsLevelNumGrp` (VV/RR/MM int fields) | `wsCicstslevel` (`String`) |

---

### DS-11: CASE-1 / CASE-2 CONDITION-ID REDEFINES groups

> **G2 resolved (2026-10-01):** `FcConditionToken`, `Case1ConditionId`, and
> `Case2ConditionId` were originally planned as Java classes for the CEEIGZCT CEE
> condition-token structure. Because CEEDAYS and CEELOCT are fully replaced by
> `java.time`, these CEE-runtime classes have no injection site and no execution path.
> Generating them would produce dead code (Rule 13). **Resolution B adopted:**
> do **not** generate `FcConditionToken`. The REDEFINES pair `CASE-1-CONDITION-ID` /
> `CASE-2-CONDITION-ID` is retained solely as a Cat 4a Pattern A model pair:

The `CASE-1-CONDITION-ID` / `CASE-2-CONDITION-ID` REDEFINES group (Cat 4a, two records,
mutually exclusive). Pattern A applies:

- Common interface `ConditionId` (marker/common abstraction; no methods required).
- `Case1ConditionId`: concrete class with `severity` (`short`) and `msgNo` (`short`) fields.
- `Case2ConditionId`: concrete class with `classCode` (`short`) and `causeCode` (`short`) fields.

`FcConditionToken` is **not generated** (dead code, Rule 13 — CEE runtime replaced by
`java.time`). The `ConditionId` interface and both concrete classes exist independently in
`com.ibm.cics.botz.crecust.model` for the REDEFINES pair only.

---

### DS-12: NCS Customer Number Fields — `NcsCustNoStuff`

| Java Field | Type | Notes |
|---|---|---|
| `ncsCustNoActName` | `String` | `'BANKZCUST'` (9 bytes) |
| `ncsCustNoTestSort` | `String` | 6 bytes — sortcode |
| `ncsCustNoFill` | `String` | 2 spaces |
| `ncsCustNoInc` | `long` | VALUE 0; set to 1 for increment |
| `ncsCustNoValue` | `long` | Returned next value |
| `ncsCustNoResp` | `String` | `'00'` |

The 16-byte ENQ resource name is assembled from `ncsCustNoActName + ncsCustNoTestSort + ncsCustNoFill`.



## Serialization Strategy

**ADR: ADR-A — Retain z/OS I/O (JCICS-style serialization required)**

The modernised program runs under JCICS and exchanges byte-array commareas with CICS callers and
CICS COBOL programs (ABNDPROC, OCR1–OCR5 helpers). All external interfaces use fixed-width
EBCDIC byte arrays. The following serializer contracts apply:

### SER-1: Serializer Requirements

SER-1.1 Every serializer class (e.g., `CrecustareaSerializer`, `WsChildDataSerializer`,
`AbndInfoRecSerializer`) **MUST implement `ByteArraySerializer<T>`** where `T` is the
corresponding model class.

SER-1.2 Every model class that has a paired serializer **MUST implement
`ByteArraySerializable<Self>`**.

SER-1.3 Byte widths are fixed and derived from PIC clause lengths in the technical research.
Strings use EBCDIC encoding via JZOS (`com.ibm.jzos.ZUtil` or equivalent).

SER-1.4 Infrastructure classes required as dependencies:
- `ByteArraySerializer<T>` (interface)
- `ByteArraySerializable<T>` (interface)
- `Settable<T>` (interface)
- `Lists` (utility class)

SER-1.5 A serializer that does **not** implement `ByteArraySerializer<T>` fails acceptance.

SER-1.6 No inline byte-packing of an already-serialised layout in service classes (Rule 5 /
Rule 15). Every class that reads or writes a byte-array layout delegates to the canonical
serializer for that layout.

### SER-2: Canonical Serializer Map

| Layout | Model Class | Canonical Serializer | Total Bytes |
|---|---|---|---|
| DFHCOMMAREA / CRECUST.cpy | `CrecustCommarea` | `CrecustareaSerializer` | 399 |
| WS-CHILD-DATA / CUSTOMER.cpy + extras | `WsChildData` | `WsChildDataSerializer` | 399 |
| ABNDINFO-REC / ABNDINFO.cpy | `AbndInfoRec` | `AbndInfoRecSerializer` | ≈673 |

## Acceptance Criteria

> Acceptance criteria are grouped by the 18 fidelity rules from the COBOL-to-Java transform
> rulebook. Each criterion is testable and maps to at least one functional requirement.

### AC-1: Error-Path Ordering (Rule 1)

AC-1.1 When INSERT PROCTRAN fails, a test confirms: (a) `AbndInfoRec` is populated **before**
`Program.link("ABNDPROC")` is called, (b) `Program.link("ABNDPROC")` is called **before**
`Task.getTask().abend("HWPT")` is invoked, (c) swapping the order causes test failure.

AC-1.2 When INSERT CUSTOMER fails, the test confirms no `Program.link()` call is made and no
ABEND is issued — only `COMM-SUCCESS='N'`, `COMM-FAIL-CODE='1'`, and CICS RETURN.

AC-1.3 When title validation fails, no ENQ or DEQ is issued — only `COMM-SUCCESS='N'`,
`COMM-FAIL-CODE='T'`, and CICS RETURN.

---

### AC-2: Exception-Handler Wiring (Rule 2)

AC-2.1 `CrecustException` (accessed via its static factory methods and its `failCode`
attribute) is the single delegatee for all error-response construction. There is **no**
separately injected exception-handler class. Every catch block in a service class:
  (a) catches `CrecustException e`,
  (b) calls `commarea.setCommFailCode(e.getFailCode())`,
  (c) calls `commarea.setCommSuccess('N')`,
  (d) returns (or, on the notifying-abort path, proceeds to ABEND) as appropriate.

AC-2.2 No `*ExceptionHandler*` or `*ErrorHandler*` class is generated; there is no injected
handler object of any kind. Catch blocks in service classes are inline by design — each maps
to a concrete COBOL error-handling construct (ADR-10, Rule 16).

---

### AC-3: Named Exception Factories (Rule 3)

AC-3.1 For each distinct fail-code value (`T`, `G`, `A`, `B`, `C`, `D`, `E`, `F`, `H`,
`1`, `3`, `4`, `5`, `O`, `Y`, `Z`, `' '`), the exception class exposes a named static
factory method. Raw string literals for fail-codes are not passed to exception constructors
at call sites.

---

### AC-4: CICS ABEND Code Propagation (Rule 4)

AC-4.1 The literal `'HWPT'` is declared as a named constant (e.g.,
`ABEND_CODE_HWPT = "HWPT"`) in the exception class. The constant name appears in the
`Task.getTask().abend()` call.

AC-4.2 A grep of the generated code finds no bare string `"HWPT"` outside the constant
declaration.

---

### AC-5: Commarea / Byte-Array Mapper Layout (Rule 5)

AC-5.1 `CrecustareaSerializer` implements `ByteArraySerializer<CrecustCommarea>`.
`CrecustCommarea` implements `ByteArraySerializable<CrecustCommarea>`.

AC-5.2 All 25 field offsets and lengths in `CrecustareaSerializer` are declared as named
`private static final int` constants. No bare integer literal appears for a byte offset.

AC-5.3 A bounds check is present before every array write: if `buffer.length < offset + length`,
an `IllegalArgumentException` is thrown naming the field and required minimum buffer length.

AC-5.4 Field-width constants are not redeclared in any class other than `CrecustareaLayout`
(or the serializer itself if it owns the constants).

AC-5.5 `WsChildDataSerializer` and `AbndInfoRecSerializer` each implement
`ByteArraySerializer<T>` for their respective types.

AC-5.6 No stub / redirect file exists — every `.java` file contains a real class.

---

### AC-6: EIBCALEN Partial-Copy (Rule 6)

AC-6.1 If the commarea passed to the Java program is shorter than 399 bytes (`EIBCALEN <
MAX_COPY_LENGTH`), only the available bytes are copied; the remainder of the target fields are
left at their default values. A unit test covering a short commarea (e.g., 100 bytes) confirms
that only the first 100 bytes are read and the remaining fields are zero/empty.

---

### AC-7: SQL Column Completeness (Rule 7)

AC-7.1 The `INSERT INTO CUSTOMER` JDBC statement contains all 17 columns in the order
specified in PE-3 of the technical research. A column-count assertion in the test confirms 17
bind parameters.

AC-7.2 The `INSERT INTO PROCTRAN` JDBC statement contains all 9 columns. A column-count
assertion confirms 9 bind parameters. `PROCTRAN_DATE` receives a `String` in `DD.MM.YYYY`
format, not a `java.sql.Date`.

AC-7.3 The `SELECT CONTROL_VALUE_NUM FROM STTESTER.CONTROL WHERE CONTROL_NAME = ?` statement
selects exactly one column with one bind parameter.

AC-7.4 The `UPDATE STTESTER.CONTROL SET CONTROL_VALUE_NUM = ? WHERE CONTROL_NAME = ?`
statement sets one column with two bind parameters.

---

### AC-8: Validation Constants (Rule 8)

AC-8.1 The minimum DOB year (`1601`), maximum customer age (`150`), credit-agency count (`5`),
review-date upper bound (`21`), ENQ resource length (`16`), and Lilian epoch offset are all
declared as named `private static final int` constants. No numeric literal appears for any of
these in conditional expressions.

AC-8.2 The `SORTCODE` value (`987654`) from `SORTCODE.cpy` is a named constant; no raw
`987654` literal appears in service logic.

---

### AC-9: No Copybook Sentinel Placeholders (Rule 9)

AC-9.1 All copybooks were resolved (TRA-9 confirms no unresolved copybooks). No `// UNRESOLVED`
placeholder constants exist in the generated code.

---

### AC-10: ABNDPROC Return-Code (Rule 10)

AC-10.1 `linkAbndproc()` returns `void`. No result DTO or return-code inspection follows the
`Program.link("ABNDPROC")` call. A code review confirms no variable assignment on the
`Program.link()` return.

---

### AC-11: Date / Time Formats (Rule 11)

AC-11.1 `HV-PROCTRAN-DATE` is populated as `DD.MM.YYYY` (10 chars) — test confirms the string
format exactly.

AC-11.2 `HV-PROCTRAN-TIME` is `HHMMSS` (6 chars, no separators) — test confirms the format.

AC-11.3 `ABND-TIME` is `HH:MM:SS` (8 chars, colon separators) — test confirms the format.

AC-11.4 `HV-CUSTOMER-DOB`, `HV-CUSTOMER-CREATE-DATE`, `HV-CUSTOMER-CS-REVIEW-DATE` are
encoded as `(YYYY * 10000) + (MM * 100) + DD` integers. A test with a known date
(e.g., 15-Jun-1990) asserts the integer value equals `19900615`.

AC-11.5 `COMM-CS-REVIEW-DATE` byte layout is `DDMMYYYY` (8 bytes, no separators) on both the
error path and the success path. A test confirms both paths produce identical byte layouts.

AC-11.6 `STORED-DOB` used in `HV-PROCTRAN-DESC` is `DD/MM/YYYY` (10 chars, slash separator) —
test confirms the format.

---

### AC-12: No Credential Fields (Rule 12)

AC-12.1 The technical research confirms no credential or password fields exist in CRECUST
(PE-10). This criterion is satisfied by construction; no credential constants need verification.

---

### AC-13: No Dead Classes (Rule 13)

AC-13.1 Every Java class in the architecture class list appears in at least one execution path,
sequence diagram, or dependency injection site. No `@Component`, `@Service`, or `@Bean` exists
with no injection site.

AC-13.2 `FcConditionToken` is **not generated**. The three CEE condition-token classes
(`FcConditionToken`, `Case1ConditionId`, `Case2ConditionId`) that were originally in DS-11 for
the CEEIGZCT structure have been removed because CEEDAYS and CEELOCT are replaced by `java.time`
and no execution path references these classes (G2 resolved 2026-10-01). `ConditionId`,
`Case1ConditionId`, and `Case2ConditionId` remain only as the Pattern A pair for the
`CASE-1-CONDITION-ID` / `CASE-2-CONDITION-ID` REDEFINES group (Story 2.4).

---

### AC-14: Cross-Program Data-Structure Completeness (Rule 14)

AC-14.1 All four cross-program structures are present as Java classes in the output:
`CrecustCommarea`, `CustomerRecord`, `ProctranData` (with all REDEFINES subclasses),
`WsChildData`.

AC-14.2 `CustomerControlRecord` (CUSTCTRL.cpy) is present even though its fields are not
exercised by the main happy-path flow.

---

### AC-15: Serializer Delegation (Rule 15)

AC-15.1 No service class re-implements byte-field writing for `CrecustareaLayout`,
`WsChildDataLayout`, or `AbndInfoRecLayout` inline. Every write delegates to the canonical
serializer. A code review confirms no duplicate offset arithmetic outside the serializer classes.

AC-15.2 The shared layout-constants class (e.g., `CrecustareaLayout`) is the single source
of truth for all field widths. No width constant is redeclared in the serializer or service.

---

### AC-16: Exception-Handler COBOL Grounding (Rule 16)

AC-16.1 Every exception-handler class maps to a documented COBOL construct:
- The PROCTRAN failure handler maps to the inline SQLCODE check in `WRITE-PROCTRAN-DB2_WPD010`
  (the only notifying-abort path with LINK + ABEND).
- The SQL failure handler maps to the inline SQLCODE checks in `WRITE-CUSTOMER-DB2_WCD010`
  and `GET-LAST-CUSTOMER-DB2_GLCD010`.
- The CICS response handler maps to the inline EIBRESP/EIBRESP2 checks in `CREDIT-CHECK_CC010`
  and `ENQ-NAMED-COUNTER_ENC010` / `DEQ-NAMED-COUNTER_DNC010`.

AC-16.2 No exception handler exists whose only behaviour is to re-throw the exception with no
corresponding COBOL construct. Such a handler is removed or collapsed into the service.

---

### AC-17: One Class Per File (Rule 17)

AC-17.1 Every `.java` file contains exactly one top-level type declaration. A file-scan
confirms no file defines multiple `public` or package-private top-level classes.

---

### AC-18: No User-Facing I/O Contract (Rule 18)

AC-18.1 The technical research confirms no `ACCEPT` statements and no interactive `DISPLAY`/
`ACCEPT` pairs exist in CRECUST (PE-9). Rule 18 is satisfied by construction; no I/O contract
preservation is required.

---

### AC-19: REDEFINES Correctness

AC-19.1 `PROC-TRAN-DESC`: Pattern B (shared `byte[]`) is implemented. Each of the five
concrete subclasses (`ProcTranDescXfr`, `ProcTranDescDelacc`, `ProcTranDescCreacc`,
`ProcTranDescDelcus`, `ProcTranDescCrecus`) encode and decode correctly from the 40-byte base
array. No instance fields are declared in any subclass.

AC-19.2 All six Cat 3a REDEFINES groups produce a record class with getter/setter access over
the canonical PIC field. Tests confirm round-trip conversion (set via record class → read via
PIC getter, and vice versa).

AC-19.3 `CASE-1-CONDITION-ID` / `CASE-2-CONDITION-ID` (Cat 4a, Pattern A, mutually exclusive):
each concrete class holds its own typed fields; no shared byte[] base. A common interface is
present.

---

### AC-20: Customer Number Allocation

AC-20.1 `NameResource.enqueue()` is acquired before the CONTROL table SELECT. A test confirms that
when the ENQ JCICS call throws a CICS exception, `COMM-FAIL-CODE='3'` is set and no DB2
access occurs.

AC-20.2 `NameResource.dequeue()` is called from exactly four call sites (mirroring COBOL Fan-In-4).
A code review or unit test map confirms four and only four `dequeue()` call sites.

AC-20.3 The new customer number is pushed to all four targets: `COMM-NUMBER`,
`customerNumber` in `CustomerRecord`, `requiredCustNumber2` in `CustomerKy2`, and
`ncsCustNoValue`.



## Out of Scope

The following items are explicitly out of scope for this modernisation:

| Item | Reason |
|---|---|
| Modernising OCR1–OCR5 child transactions | Kept as CICS COBOL by Product Owner decision; out of scope for this PRD |
| Modernising ABNDPROC | Kept as CICS COBOL by Product Owner decision |
| Migrating DB2 tables to a different database | Product Owner confirmed all three tables remain on Db2 for z/OS |
| Changing the DFHCOMMAREA byte layout | The 399-byte layout is a fixed external contract; field widths and offsets must not change |
| Adding new business logic or new validation rules | This modernisation is behaviour-preserving only; no new features |
| UI or front-end changes | CRECUST is a back-end CICS-linked program with no user-facing interface |
| Unit test implementation | Tests are referenced as acceptance criteria; test implementation is a delivery task |
| Performance tuning | Not requested; any performance improvement must not change observable behaviour |
| CICS transaction definition (CSD) changes | CICS resource definitions are infrastructure; out of scope for Java code delivery |


