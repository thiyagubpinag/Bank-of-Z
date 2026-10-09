---
title: 'Story 8.1: ProctranDbService.insertProctran() — PROCTRAN Assembly and JDBC INSERT'
type: 'feature'
created: '2026-10-01'
status: 'ready-for-dev'
route: 'dispatch'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** No Java service class exists to write the PROCTRAN audit record that every customer
creation requires. The COBOL `WRITE-PROCTRAN-DB2_WPD010` paragraph assembles a 40-byte descriptor
from four stored values, obtains the CICS time via ASKTIME + FORMATTIME, and executes a 9-column
JDBC INSERT — none of this is yet represented in Java.

**Approach:** Create `ProctranDbService` (happy-path INSERT only; SQL failure catch block deferred
to Story 8.2) with `insertProctran()` that assembles `HostProctranRow`, executes the INSERT, and
uses `LocalDateTime.now()` + `DateTimeFormatter` to produce the `DD.MM.YYYY` date and `HHmmss`
time strings. `Task.getTask().getAbstime()` does not exist in the JCICS API and must NOT be used.
All JDBC resources in `try-with-resources`.

## Boundaries & Constraints

**Always:**
- `HostProctranRow` is the sole DB2 host-variable holder; all 9 INSERT parameters are drawn from it.
- Use `LocalDateTime.now()` for the PROCTRAN timestamp — `Task.getTask().getAbstime()` does NOT exist in JCICS and must NOT be called.
- Date formatter pattern must be exactly `"dd.MM.yyyy"` (dot-separated); time formatter pattern must be exactly `"HHmmss"` (no separators) — tested by unit assertions on the exact format strings (AC-11.1, AC-11.2, Rule 11).
- `hvProctranDesc` is a 40-character `String` assembled via exact byte-offset substring assignment mirroring COBOL reference modification (TRA-7): `storedSortcode` at 0–5, `storedCustno` at 6–15, `storedName` at 16–29, `storedDob` at 30–39.
- `storedDob` inside the descriptor is formatted `DD/MM/YYYY` (10 bytes, slash-separated) (AC-11.6).
- The INSERT SQL must contain exactly 9 `?` placeholders in PE-3 column order; a unit test counts them (AC-7.2).
- `PROCTRAN_DATE` receives a `String` value, NOT a `java.sql.Date`.
- `DataSource` obtained via `(DataSource) new InitialContext().lookup("jdbc/crecustDB2DS")` (ADR-4).
- All `Connection` and `PreparedStatement` objects managed via `try-with-resources` (NFR-4).
- Lombok `@Data`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor` on `HostProctranRow` (ADR-12).
- `ProctranDbService` lives in package `com.ibm.cics.botz.crecust.service`; `HostProctranRow` in `com.ibm.cics.botz.crecust.db`.
- SLF4J `private static final Logger LOGGER` in `ProctranDbService`; `DEBUG` for field values pre-INSERT.
- `insertProctran()` signature: `void insertProctran(CrecustCommarea commarea, String storedSortcode, String storedCustno, String storedName, String storedDob)` — throws `SQLException` to allow Story 8.2 to add the catch.

**Never:**
- Do not implement the SQL failure/notifying-abort catch block in this story — that is Story 8.2.
- Do not use `java.sql.Date` for `PROCTRAN_DATE`; it must be a `String` matching `DD.MM.YYYY`.
- Do not hardcode `"HWPT"` or reference `CrecustException.ABEND_CODE_HWPT` in this story.
- Do not inline byte-packing of `AbndInfoRec` here; serializer delegation is Story 8.2.
- Do not use `String.format` or `StringBuilder` for the date/time strings — use `DateTimeFormatter`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Happy path INSERT | Valid commarea + stored values; DataSource available | `HostProctranRow` fully assembled; JDBC INSERT executes; method returns normally | N/A — success |
| ASKTIME returns abstime | `Task.getTask().getAbstime()` succeeds | `hvProctranDate` = `DD.MM.YYYY`; `hvProctranTime` = `HHmmss` | CicsConditionException propagates (not caught here) |
| Desc assembly | `storedSortcode="987654"`, `storedCustno="0000000001"`, `storedName="Smith         "` (14 chars), `storedDob="01/01/1990"` | `hvProctranDesc` = `"987654" + "0000000001" + "Smith         " + "01/01/1990"` exactly 40 chars | IllegalArgumentException if any segment wrong length |
| SQL failure | `DataSource` throws `SQLException` on INSERT | `SQLException` propagates up to caller (caught by Story 8.2) | Uncaught here — deliberately propagated |

</frozen-after-approval>

## Code Map

- `_bmad-output/planning-artifacts/CRECUST/technical-research.md` -- PE-3 (INSERT PROCTRAN 9 columns), PE-4 (date/time formats), TRA-7 (PROCTRAN DESC byte layout), HOST-PROCTRAN-ROW field table (line 160)
- `_bmad-output/planning-artifacts/CRECUST/architecture.md` -- §5.7 (ProctranDbService), §5.8 (AbndprocDelegate), ADR-4 (DataSource JNDI), ADR-12 (Lombok), §9 (CICS API mapping for ASKTIME/FORMATTIME)
- `_bmad-output/planning-artifacts/CRECUST/epics.md` -- Story 8.1 AC detail (lines 1046–1088)
- `_bmad-output/implementation-artifacts/CRECUST/epic-8-context.md` -- compiled epic context
- `com/ibm/cics/botz/crecust/db/HostProctranRow.java` -- CREATE: 9-field DB2 host-variable row class with Lombok
- `com/ibm/cics/botz/crecust/service/ProctranDbService.java` -- CREATE: insertProctran() happy path
- `com/ibm/cics/botz/crecust/model/CrecustCommarea.java` -- READ ONLY: commarea model already created in Epic 2

## Tasks & Acceptance

**Execution:**
- [ ] `src/main/java/com/ibm/cics/botz/crecust/db/HostProctranRow.java` -- CREATE class with 9 fields matching HOST-PROCTRAN-ROW (PE-3: `hvProctranEyecatcher String`, `hvProctranSortCode String`, `hvProctranAccNumber String`, `hvProctranDate String`, `hvProctranTime String`, `hvProctranRef String`, `hvProctranType String`, `hvProctranDesc String`, `hvProctranAmount BigDecimal`); annotate with Lombok `@Data @Builder @AllArgsConstructor @NoArgsConstructor` (ADR-12) -- required before ProctranDbService can compile
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/ProctranDbService.java` -- CREATE class with `insertProctran(CrecustCommarea, String storedSortcode, String storedCustno, String storedName, String storedDob) throws SQLException`; implement: (1) `LocalDateTime.now()` to obtain the current timestamp (NOT `Task.getTask().getAbstime()` — that method does not exist in JCICS); (2) format `hvProctranDate` with pattern `"dd.MM.yyyy"` and `hvProctranTime` with `"HHmmss"` via `DateTimeFormatter`; (3) assemble `hvProctranDesc` as a 40-char `String` using exact substring offsets (sortcode 0–5, custno 6–15, name 16–29, dob 30–39); (4) populate remaining `HostProctranRow` fields (eyecatcher `"PRTR"`, type `"OCC"`, accNumber `"00000000"`, amount `BigDecimal.ZERO`, ref from task number formatted to 12 chars); (5) execute JDBC INSERT with exactly 9 `?` placeholders in PE-3 column order; (6) wrap connection + statement in `try-with-resources`; (7) add `LOGGER.debug` before INSERT -- core story deliverable
- [ ] `src/test/java/com/ibm/cics/botz/crecust/service/ProctranDbServiceTest.java` -- CREATE unit test with mocked `DataSource`; assert: (a) date formatter pattern string equals `"dd.MM.yyyy"` (AC-11.1); (b) time formatter pattern string equals `"HHmmss"` (AC-11.2); (c) `hvProctranDesc` has exactly 40 chars with correct content at each offset (FR-9.2); (d) INSERT SQL string contains exactly 9 `?` placeholders (AC-7.2); (e) `PROCTRAN_DATE` set via `setString` not `setDate` -- verifies all ACs for this story

**Acceptance Criteria:**
- Given a valid `CrecustCommarea` and a working `DataSource`, when `insertProctran()` is called, then `LocalDateTime.now()` is invoked once to obtain the PROCTRAN timestamp (FR-9.1). `Task.getTask().getAbstime()` must NOT appear in the code — grep confirms zero occurrences.
- Given the abstime value, when date/time fields are formatted, then `hvProctranDate` matches pattern `"dd.MM.yyyy"` (10 chars, dot-separated) and `hvProctranTime` matches pattern `"HHmmss"` (6 chars, no separators) — unit test asserts the exact `DateTimeFormatter` pattern strings (AC-11.1, AC-11.2).
- Given stored values `storedSortcode` (6 chars), `storedCustno` (10 chars), `storedName` (14 chars), `storedDob` in `DD/MM/YYYY` format (10 chars), when `hvProctranDesc` is assembled, then: bytes 0–5 = `storedSortcode`, bytes 6–15 = `storedCustno`, bytes 16–29 = `storedName`, bytes 30–39 = `storedDob`; total length = 40 (FR-9.2, AC-11.6).
- Given the assembled `HostProctranRow`, when the INSERT SQL is constructed, then it contains exactly 9 `?` placeholders in PE-3 column order (`PROCTRAN_EYECATCHER`, `PROCTRAN_SORTCODE`, `PROCTRAN_NUMBER`, `PROCTRAN_DATE`, `PROCTRAN_TIME`, `PROCTRAN_REF`, `PROCTRAN_TYPE`, `PROCTRAN_DESC`, `PROCTRAN_AMOUNT`) and `PROCTRAN_DATE` is bound via `setString` not `setDate` (AC-7.2, Rule 7).
- Given any state, when `insertProctran()` completes successfully, then all JDBC `Connection` and `PreparedStatement` objects have been closed (are within `try-with-resources` blocks) (NFR-4).
- Given a `DataSource` that throws `SQLException` on `getConnection()` or `executeUpdate()`, when `insertProctran()` is called, then the `SQLException` propagates out of the method uncaught (the catch block is Story 8.2's responsibility).

## Implementation Notes

**2026-10-09** — Story 8.1 implemented.

- `ProctranDbService.insertProctran()` implemented in
  `crecust-java/src/main/java/com/ibm/cics/botz/crecust/service/ProctranDbService.java`.
- Timestamp obtained via `LocalDateTime.now()` (NOT `Task.getTask().getAbstime()` — does not exist
  in JCICS). Date formatted `dd.MM.yyyy`; time formatted `HHmmss` via `DateTimeFormatter`.
- 40-byte descriptor assembled via `char[]` + `System.arraycopy` at exact offsets (TRA-7):
  sortcode[0–5], custno[6–15], name[16–29], dob[30–39].
- 9-column JDBC INSERT using `STTESTER.PROCTRAN` in PE-3 column order; `PROCTRAN_DATE` bound via
  `setString()` (not `setDate()`).
- `DataSource` obtained via `(DataSource) new InitialContext().lookup("jdbc/crecustDB2DS")` (ADR-4).
- All JDBC resources in `try-with-resources` (NFR-4). `NamingException` wrapped as `SQLException`.
- All named constants declared: `PROCTRAN_DATASOURCE`, `INSERT_PROCTRAN_SQL`, `PROCTRAN_TYPE`,
  `PROCTRAN_EYECATCHER`, `PROCTRAN_ACC_NUMBER`, `PROCTRAN_DESC_LENGTH`.
- `CrecustService.java` updated with a stub `catch (java.sql.SQLException e)` (Story 8.2 TODO) to
  satisfy the Java checked-exception requirement without breaking compile.
- `mvn compile` → BUILD SUCCESS (47 source files).

## Spec Change Log

## Review Triage Log

## Design Notes

**`hvProctranDesc` assembly:**
COBOL uses reference modification (`HV-PROCTRAN-DESC(1:6)`, `HV-PROCTRAN-DESC(7:10)`, etc. — 1-based). Java substring indices are 0-based. The 40-char `String` must be built by concatenating exactly four segments in a single statement or using a `char[]`/`StringBuilder` filled at the specified offsets:

```java
// Segments must be exactly the right length; pad or truncate if needed
char[] desc = new char[40];
// [0..5]  storedSortcode (6)
System.arraycopy(storedSortcode.toCharArray(), 0, desc, 0, 6);
// [6..15] storedCustno (10)
System.arraycopy(storedCustno.toCharArray(),   0, desc, 6, 10);
// [16..29] storedName (14)
System.arraycopy(storedName.toCharArray(),     0, desc, 16, 14);
// [30..39] storedDob (10)  — must be DD/MM/YYYY
System.arraycopy(storedDob.toCharArray(),      0, desc, 30, 10);
String hvProctranDesc = new String(desc);
```

Note the gap rule from TRA-7: COBOL offsets 1:6 / 7:10 / 17:14 / 31:10 (1-based) map to Java 0-5 / 6-15 / 16-29 / 30-39 (0-based). No gap exists — the `storedCustno` occupies positions 6–15 inclusive (10 chars) and `storedName` starts immediately at 16.

**Timestamp — Java native `LocalDateTime.now()`:**
`Task.getTask().getAbstime()` does NOT exist in the JCICS API. Use `LocalDateTime.now()` directly:

```java
LocalDateTime now = LocalDateTime.now();   // replaces EXEC CICS ASKTIME
String hvProctranDate = now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
String hvProctranTime = now.format(DateTimeFormatter.ofPattern("HHmmss"));
```

No `CICS_EPOCH_OFFSET_MILLIS` constant is needed. To support unit testing without a live clock, inject a `java.time.Clock` and call `LocalDateTime.now(clock)`.

**`HostProctranRow` field types from PE-3:**

| Java field | Java type | Notes |
|---|---|---|
| `hvProctranEyecatcher` | `String` | 4 chars |
| `hvProctranSortCode` | `String` | 6 chars |
| `hvProctranAccNumber` | `String` | 8 chars; set to `"00000000"` |
| `hvProctranDate` | `String` | 10 chars `DD.MM.YYYY` |
| `hvProctranTime` | `String` | 6 chars `HHmmss` |
| `hvProctranRef` | `String` | 12 chars |
| `hvProctranType` | `String` | 3 chars |
| `hvProctranDesc` | `String` | 40 chars |
| `hvProctranAmount` | `BigDecimal` | `BigDecimal.ZERO` |

## Verification

**Commands:**
- `mvn -pl crecust test -Dtest=ProctranDbServiceTest` -- expected: BUILD SUCCESS, all assertions pass
- `mvn -pl crecust compile` -- expected: BUILD SUCCESS with no warnings on new classes
