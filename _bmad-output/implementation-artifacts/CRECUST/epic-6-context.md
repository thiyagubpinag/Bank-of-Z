# Epic 6 Context: Customer Number Allocation (ENQ / CONTROL / DEQ)

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Author `CustomerNumberService` with three methods — `enqueue()`, `getAndIncrementCustomerNumber()`, and `dequeue()` — that together implement COBOL's customer-number allocation protocol. The service acquires a CICS named-resource ENQ (serialising concurrent allocations), reads and atomically increments the CONTROL table counter via JDBC, pushes the new number to four targets in the commarea/model, and releases the ENQ from exactly four call sites (the Fan-In-4 pattern from `DEQ-NAMED-COUNTER_DNC010`). Correct resource release on every exit path (success, SQL failure, INSERT failure, PROCTRAN failure) is the primary correctness requirement.

## Stories

- Story 6.1: `CustomerNumberService.enqueue()` — JCICS NameResource ENQ (FR-7.1)
- Story 6.2: `CustomerNumberService.getAndIncrementCustomerNumber()` — JDBC CONTROL SELECT + UPDATE (FR-7.2, FR-7.3)
- Story 6.3: `CustomerNumberService.dequeue()` — four DEQ call sites (FR-7.5, AC-20.2)

## Requirements & Constraints

- **ENQ before DB2:** `NameResource.enqueue()` must be called before the CONTROL table SELECT. ENQ failure → silent-return with fail-code `'3'` (`FAIL_CODE_ENQ`); no DB2 access occurs.
- **CONTROL table atomicity:** SELECT then UPDATE on `STTESTER.CONTROL` (schema explicit). Bind parameter for SELECT is `HV-CONTROL-NAME` (`CHAR(32)`: `'BANKZCUST' + sortcode + '  '`). SQL failure on either statement → call `dequeue()`, set fail-code `'4'` (`FAIL_CODE_CONTROL_SQL`), silent-return.
- **Four-target push:** New customer number written to `commNumber`, `customerRecord.customerNumber`, `customerKy2.requiredCustNumber2 ` (mapped from `REQUIRED-CUST-NUMBER2`), and `ncsCustNoValue` (FR-7.4, AC-20.3).
- **Fan-In-4 DEQ:** `dequeue()` is called from exactly four sites — no more, no fewer (AC-20.2): (a) after successful PROCTRAN INSERT, (b) after INSERT CUSTOMER failure, (c) after INSERT PROCTRAN failure (before ABEND), (d) after SELECT/UPDATE CONTROL failure. Sites (b)–(d) live in Stories 7.2, 8.1/8.2, and 6.2 respectively; site (a) lives in Story 8.1.
- **DEQ failure:** `NameResource.dequeue()` failure → silent-return with fail-code `'5'` (`FAIL_CODE_DEQ`); no ABEND.
- **JNDI DataSource:** `(DataSource) new InitialContext().lookup("jdbc/crecustDB2DS")` — no `@Resource`, no Spring injection (ADR-4).
- **try-with-resources:** All `Connection`, `PreparedStatement`, `ResultSet` objects managed via try-with-resources (NFR-4).
- **Lombok enabled:** `CustomerNumberService` is a service class (not a data class); Lombok is for data models only (ADR-12).
- **Package:** `com.ibm.cics.botz.crecust.service`.
- **CICS API:** `NameResource` from `com.ibm.cics.server.*`; no custom wrappers.
- **16-byte resource name:** ENQ/DEQ resource = `'BANKZCUST'(9) + sortcode(6) + '  '(2)` = 17 logical bytes, passed as 16 bytes (`ENQ_RESOURCE_LENGTH = 16`). Named constant `NCS_ACT_NAME = "BANKZCUST"` required (Rule 8, AC-8.1).

## Technical Decisions

- **Error-path classification (Rule 1):** All three methods use silent-return paths only. ENQ failure, CONTROL SQL failure, and DEQ failure all set fail-code and return — no notification, no ABEND. The notifying-abort path (PROCTRAN failure) is owned by Story 8.2 and calls `dequeue()` as a step before ABEND; `dequeue()` itself still uses silent-return.
- **CrecustException constants:** `FAIL_CODE_ENQ = '3'`, `FAIL_CODE_CONTROL_SQL = '4'`, `FAIL_CODE_DEQ = '5'` are declared in `CrecustException` (Epic 9). Stories in this epic must reference those constants — never inline char literals.
- **No exception handler class:** There is no `HANDLE CONDITION` or shared error-response paragraph in the COBOL source for this epic's scope. No exception-handler class is generated for `CustomerNumberService` (ADR-10 / Rule 16).
- **HV-CONTROL-NAME is 32 bytes:** The ENQ/DEQ resource name is 16 bytes. The CONTROL table key (`HV-CONTROL-NAME`) is `CHAR(32)` — a different (longer) field, padded with spaces.
- **`getAndIncrementCustomerNumber()` calls `dequeue()`:** On SQL failure, `dequeue()` is invoked inside `getAndIncrementCustomerNumber()` before setting the fail-code and returning. This is one of the four Fan-In-4 DEQ call sites.

## Cross-Story Dependencies

- **Epic 1 (Story 1.1/1.2):** Maven project and infrastructure utilities (`ByteArraySerializer`, `Lists`, etc.) must exist before any service class can compile.
- **Epic 2:** `HostControlRow`, `NcsCustNoStuff`, `CustomerRecord`, `CustomerKy2` model classes required by `getAndIncrementCustomerNumber()`.
- **Epic 3:** `CrecustCommarea` model class; `CrecustService` orchestrator that calls `enqueue()` and `getAndIncrementCustomerNumber()`.
- **Epic 9:** `CrecustException` with `FAIL_CODE_ENQ`, `FAIL_CODE_CONTROL_SQL`, `FAIL_CODE_DEQ` constants required before any method in this epic sets a fail-code.
- **Stories 7.2 and 8.1/8.2** add the remaining three DEQ call sites (INSERT CUSTOMER failure, PROCTRAN notifying-abort path, and PROCTRAN success). AC-20.2 (exactly four call sites) cannot be fully verified until all those stories are done.
