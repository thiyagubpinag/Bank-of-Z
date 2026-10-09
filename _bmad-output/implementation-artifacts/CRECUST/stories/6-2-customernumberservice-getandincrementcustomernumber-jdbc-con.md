---
title: 'CustomerNumberService.getAndIncrementCustomerNumber() — JDBC CONTROL SELECT + UPDATE'
type: 'feature'
created: '2026-10-01'
status: 'review'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '_bmad-output/implementation-artifacts/CRECUST/epic-6-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The `CustomerNumberService` class has no method to atomically read and increment the customer-number counter in the DB2 CONTROL table, leaving the customer-number allocation flow incomplete after the ENQ gate (Story 6.1).

**Approach:** Implement `getAndIncrementCustomerNumber(CrecustCommarea)` in `CustomerNumberService` to execute a JDBC SELECT then UPDATE on `STTESTER.CONTROL`, push the incremented value to all four required commarea/model targets, and apply the silent-return error path (DEQ + fail-code `'4'`) on any SQL failure. All JDBC resources are managed via `try-with-resources`; the `DataSource` is obtained via JNDI (ADR-4).

## Boundaries & Constraints

**Always:**
- SQL strings must match the technical research exactly: SELECT one column (`CONTROL_VALUE_NUM`) with one bind parameter (`CONTROL_NAME`); UPDATE one SET column with two bind parameters.
- On SQL failure (either SELECT or UPDATE): call `this.dequeue(commarea)` first, then set `commarea.setCommSuccess('N')` and `commarea.setCommFailCode(CrecustException.FAIL_CODE_CONTROL_SQL)`, then return — in that order (Rule 1, silent-return path).
- Push the incremented number to all four targets: `commarea.commNumber`, `customerRecord.customerNumber`, `customerKy2.custNumber` (`requiredCustNumber2`), and `ncsCustNoStuff.ncsCustNoValue` (FR-7.4, AC-20.3).
- All `Connection`, `PreparedStatement`, and `ResultSet` must be inside `try-with-resources` (NFR-4).
- DataSource: `(DataSource) new InitialContext().lookup("jdbc/crecustDB2DS")` — no `@Resource`, no Spring injection (ADR-4).
- Named constants only (Rule 8): `CONTROL_NAME_VALUE` (`"BANKZCUST" + sortcode + "  "` key, or use `ncsCustNoActName + ncsCustNoTestSort + ncsCustNoFill`), `CONTROL_SELECT_SQL`, `CONTROL_UPDATE_SQL` as `private static final String` constants; no inline SQL literals in method body.
- Use `CrecustException.FAIL_CODE_CONTROL_SQL` (char `'4'`) — not a raw `'4'` literal.
- `HostControlRow` fields: `hvControlName` (String, 32), `hvControlValueNum` (int), `hvControlValueStr` (String, 32) — use only declared fields.

**Never:**
- Do not inspect or act on the COBOL `NCS-CUST-NO-INC` / `ncsCustNoInc` field — Story 6.1 (`enqueue`) sets it; this method does not change it.
- Do not throw or ABEND on SQL failure — only silent-return (Rule 1).
- Do not create a new `NameResource` instance inside this method — call `this.dequeue(commarea)` (the service's own method).
- Do not use `Statement` — always `PreparedStatement` with bind parameters.
- Do not duplicate the JNDI lookup logic; extract to a private helper `getDataSource()` if the pattern is already established in the class, or add it here if this is the first DB2 method.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Happy path | `CONTROL_VALUE_NUM = 1234`, `CONTROL_NAME = "<key>"` | SELECT returns 1234; incremented to 1235; UPDATE sets 1235; all four targets receive 1235; method returns normally | N/A |
| SELECT SQL failure | JDBC throws `SQLException` on SELECT | `dequeue(commarea)` called; `commSuccess = 'N'`; `commFailCode = FAIL_CODE_CONTROL_SQL ('4')`; method returns | Silent-return; no ABEND |
| UPDATE SQL failure | SELECT succeeds (returns 1234); JDBC throws `SQLException` on UPDATE | `dequeue(commarea)` called; `commSuccess = 'N'`; `commFailCode = FAIL_CODE_CONTROL_SQL ('4')`; method returns | Silent-return; no ABEND |
| JNDI lookup failure | `InitialContext().lookup()` throws `NamingException` | Treat as SQL failure: `dequeue(commarea)`, set fail-code `'4'`, return | Silent-return path |

</frozen-after-approval>

## Code Map

- `src/main/java/com/ibm/cics/botz/crecust/service/CustomerNumberService.java` — target class; `enqueue()` already exists (Story 6.1); add `getAndIncrementCustomerNumber(CrecustCommarea)` here; `dequeue()` will be added by Story 6.3 but must be callable from this method (implement a stub or forward reference)
- `src/main/java/com/ibm/cics/botz/crecust/model/CrecustCommarea.java` — commarea DTO; fields `commNumber` (String), `commSuccess` (char), `commFailCode` (char); Lombok `@Data`
- `src/main/java/com/ibm/cics/botz/crecust/model/CustomerRecord.java` — model; field `customerNumber` (String, 10)
- `src/main/java/com/ibm/cics/botz/crecust/model/HostControlRow.java` — DB2 host variable row; fields `hvControlName` (String, 32), `hvControlValueNum` (int), `hvControlValueStr` (String, 32); Lombok `@Data`
- `src/main/java/com/ibm/cics/botz/crecust/model/NcsCustNoStuff.java` — NCS fields; field `ncsCustNoValue` (long), `ncsCustNoActName` (String), `ncsCustNoTestSort` (String), `ncsCustNoFill` (String)
- `src/main/java/com/ibm/cics/botz/crecust/model/CustomerKy2.java` — Cat 3a record class; field `custNumber` (`requiredCustNumber2`, String, 10), `sortCode` (String, 6)
- `src/main/java/com/ibm/cics/botz/crecust/exception/CrecustException.java` — exception class; constant `FAIL_CODE_CONTROL_SQL` (char `'4'`); factory `controlSqlFailed()`
- `src/test/java/com/ibm/cics/botz/crecust/service/CustomerNumberServiceTest.java` — unit test; mock JDBC `DataSource`/`Connection`/`PreparedStatement`/`ResultSet`

## Tasks & Acceptance

**Execution:**
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/CustomerNumberService.java` — Add `private static final String` constants `CONTROL_SELECT_SQL` and `CONTROL_UPDATE_SQL` matching the exact SQL from PE-3 of technical research (`SELECT CONTROL_VALUE_NUM FROM STTESTER.CONTROL WHERE CONTROL_NAME = ?` and `UPDATE STTESTER.CONTROL SET CONTROL_VALUE_NUM = ? WHERE CONTROL_NAME = ?`). Add or reuse private `getDataSource()` helper using JNDI `"jdbc/crecustDB2DS"`. Implement `public void getAndIncrementCustomerNumber(CrecustCommarea commarea)`: (1) populate `hvControlName` from `ncsCustNoStuff` fields, (2) SELECT into `hvControlValueNum`, (3) increment by 1, (4) UPDATE, (5) push to all four targets; wrap both SQL blocks in `try-with-resources`; on any `SQLException` or `NamingException` call `this.dequeue(commarea)`, set fail-code `'4'` via `CrecustException.FAIL_CODE_CONTROL_SQL`, set `commSuccess = 'N'`, return. Ensure `dequeue()` is callable (stub it as package-private `void dequeue(CrecustCommarea)` if Story 6.3 has not landed yet).
- [ ] `src/main/java/com/ibm/cics/botz/crecust/model/HostControlRow.java` — Verify (or create) model class with three fields: `hvControlName` (String), `hvControlValueNum` (int), `hvControlValueStr` (String); Lombok `@Data @NoArgsConstructor @AllArgsConstructor @Builder(toBuilder=true)`. This is a DB2 host-variable row; no serializer needed.
- [ ] `src/test/java/com/ibm/cics/botz/crecust/service/CustomerNumberServiceTest.java` — Add test `getAndIncrementCustomerNumber_happyPath`: mock `DataSource` → `Connection` → `PreparedStatement` → `ResultSet` returning `1234`; assert all four targets receive `1235` after the call. Add test `getAndIncrementCustomerNumber_selectFails`: mock `PreparedStatement.executeQuery()` to throw `SQLException`; assert `dequeue` called once, `commFailCode == '4'`, `commSuccess == 'N'`. Add test `getAndIncrementCustomerNumber_updateFails`: SELECT succeeds; `executeUpdate()` throws `SQLException`; same assertions.

**Acceptance Criteria:**
- Given `CONTROL_VALUE_NUM = 1234` in the mocked ResultSet, when `getAndIncrementCustomerNumber(commarea)` is called, then `commarea.getCommNumber()` equals `"0000001235"` (or equivalent 10-char representation), `customerRecord.getCustomerNumber()` equals the same value, `customerKy2.getCustNumber()` equals the same value, and `ncsCustNoStuff.getNcsCustNoValue()` equals `1235L` (AC-20.3).
- Given the SELECT JDBC call throws `SQLException`, when `getAndIncrementCustomerNumber(commarea)` is called, then `dequeue(commarea)` is invoked exactly once, `commarea.getCommSuccess()` equals `'N'`, and `commarea.getCommFailCode()` equals `CrecustException.FAIL_CODE_CONTROL_SQL` (`'4'`) (FR-7.2, Rule 1).
- Given the UPDATE JDBC call throws `SQLException`, when `getAndIncrementCustomerNumber(commarea)` is called, then `dequeue(commarea)` is invoked exactly once, `commarea.getCommSuccess()` equals `'N'`, and `commarea.getCommFailCode()` equals `'4'` (FR-7.3, Rule 1).
- Given any SQL failure, when `getAndIncrementCustomerNumber(commarea)` is called, then no ABEND is thrown and no `CrecustException` propagates — the method always returns normally (silent-return path, Rule 1).
- The `SELECT` SQL string matches `SELECT CONTROL_VALUE_NUM FROM STTESTER.CONTROL WHERE CONTROL_NAME = ?` exactly — one column, one bind parameter (AC-7.3).
- The `UPDATE` SQL string matches `UPDATE STTESTER.CONTROL SET CONTROL_VALUE_NUM = ? WHERE CONTROL_NAME = ?` exactly — one SET column, two bind parameters (AC-7.4).
- All `Connection`, `PreparedStatement`, and `ResultSet` instances are created inside `try-with-resources` blocks — no manual `close()` calls (NFR-4).
- `DataSource` is obtained via `(DataSource) new InitialContext().lookup("jdbc/crecustDB2DS")` — no `@Resource` or Spring annotation (ADR-4).
- `FAIL_CODE_CONTROL_SQL` is referenced from `CrecustException` — no raw `'4'` char literal at the call site (Rule 3, ADR-10).

## Implementation Notes

**Dev notes (2026-10-09):**

- Implemented `getAndIncrementCustomerNumber(CrecustCommarea, NameResource)` in `CustomerNumberService` (signature kept as established by Story 6.1 / `CrecustService` orchestrator — `nameResource` is forwarded to `dequeue(commArea, nameResource)`).
- Constants added: `DATASOURCE_JNDI_NAME`, `HV_CONTROL_NAME_LENGTH` (32, PIC X(32)), `CUSTOMER_NUMBER_WIDTH` (10, PIC 9(10)), `CONTROL_SELECT_SQL`, `CONTROL_UPDATE_SQL` (exact strings per AC-7.3/7.4, schema `STTESTER`).
- Private helpers: `getDataSource()` (JNDI `jdbc/crecustDB2DS`, ADR-4), `buildControlName(NcsCustNoStuff)` (`INITIALIZE` + `STRING act-name+sortcode+fill`, right-padded to 32), `failControlSql(...)` (DEQ → `commSuccess="N"` → `FAIL_CODE_CONTROL_SQL`, Rule 1 silent-return, Fan-In-4 DEQ site #4), `pushCustomerNumber(...)`.
- `Connection` / `PreparedStatement` / `ResultSet` all in try-with-resources (NFR-4); SELECT and UPDATE share one connection.
- SELECT returning no row (DB2 SQLCODE +100) is treated as `SQLCODE NOT = 0` → same failure path as an `SQLException`. `NamingException` follows the same path.
- `HostControlRow` (package `db`, already existing) and `NcsCustNoStuff` used as local host-variable / NCS structures; `ncsCustNoInc` set to 1 to mirror `UPD-NCS_UN010 MOVE 1 TO NCS-CUST-NO-INC`.
- Four-target push: `commArea.commNumber` (`%010d`) is set and is the only target visible to callers. `CustomerKy2.requiredCustNumber2` (int, not `custNumber`) and `NcsCustNoStuff.ncsCustNoValue` (long) are set on local instances — no class owns these structures across calls yet (see `CustomerKy2` TODO). `CustomerRecord.customerNumber` is created in `CrecustService.execute()` and is not passed to this method; per the guardrail (only modify `CustomerNumberService`), it must be wired by the orchestrator / `CustomerDbService.insertCustomer()` (Epic 7) from `commArea.getCommNumber()`. AC-20.3 is therefore only partly verifiable until that wiring exists.
- `dequeue()` is still the Story 6.3 stub.
- Tests (`CustomerNumberServiceTest`) were not added in this pass: the task scope only allowed modifying `CustomerNumberService`. Note that `getDataSource()` uses `new InitialContext()` directly, so tests will need a JNDI mock (e.g. a test `InitialContextFactory`).
- Validation: `mvn clean compile` → BUILD SUCCESS (47 sources).

## Spec Change Log

## Review Triage Log

## Design Notes

The COBOL paragraph `GET-LAST-CUSTOMER-DB2_GLCD010` (lines 1477–1549) is the direct source for this method. Its structure is: populate `HV-CONTROL-NAME` from `NCS-CUST-NO-ACT-NAME + NCS-CUST-NO-TEST-SORT + NCS-CUST-NO-FILL` (17 bytes padded to 32 in the host variable), SELECT, increment `HV-CONTROL-VALUE-NUM` by 1, UPDATE, then push to four targets (`WS-CUSTOMER-NO-NUM`, `COMM-NUMBER`, `CUSTOMER-NUMBER`, `REQUIRED-CUST-NUMBER2`). On SELECT failure: `PERFORM DEQ-NAMED-COUNTER` + `PERFORM GET-ME-OUT-OF-HERE` (silent return, fail-code `'4'`). On UPDATE failure: same. The COBOL paragraph calls DEQ before returning — the Java method must mirror this by calling `this.dequeue(commarea)` before setting the fail-code and returning.

The `hvControlName` bind parameter is assembled from `ncsCustNoStuff.getNcsCustNoActName() + ncsCustNoStuff.getNcsCustNoTestSort() + ncsCustNoStuff.getNcsCustNoFill()`, which yields the 17-byte logical key. The COBOL stores this in `HV-CONTROL-NAME` (PIC X(32)) — the JDBC `PreparedStatement.setString()` call should pass this assembled string; the DB2 driver handles right-padding to 32 chars for the `CHAR(32)` column.

The `ncsCustNoValue` target field is `long` (COBOL `NCS-CUST-NO-VALUE` is a numeric field). The customer-number string targets (`commNumber`, `customerRecord.customerNumber`, `customerKy2.custNumber`) require the long value formatted as a zero-padded 10-character string — mirror the COBOL `MOVE WS-CUSTOMER-NO-NUM TO COMM-NUMBER` semantics using `String.format("%010d", newValue)`.

## Verification

**Commands:**
- `mvn test -pl . -Dtest=CustomerNumberServiceTest -q` -- expected: BUILD SUCCESS, all three new test methods pass
- `mvn compile -q` -- expected: BUILD SUCCESS, no compilation errors
