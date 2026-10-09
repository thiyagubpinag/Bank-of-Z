---
title: 'Story 8.2: Notifying-Abort Path — AbndprocDelegate and PROCTRAN SQL Failure Sequence'
type: 'feature'
created: '2026-10-01'
status: 'review'
route: 'dispatch'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** When the PROCTRAN JDBC INSERT fails, the COBOL source executes a strict 4-step notifying-abort sequence before terminating the task. This sequence — populate `AbndInfoRec` → DEQ → LINK ABNDPROC → ABEND `'HWPT'` — has no Java equivalent yet; the catch block inside `ProctranDbService.insertProctran()` is incomplete, and `AbndprocDelegate` does not exist.

**Approach:** Extend the `SQLException` catch block in `ProctranDbService.insertProctran()` to execute the four steps in exact COBOL order, and create `AbndprocDelegate` (package `com.ibm.cics.botz.crecust.service`) whose `linkAbndproc(AbndInfoRec)` method serialises via the canonical `AbndInfoRecSerializer` and calls `new Program("ABNDPROC").link(commareaBytes)` — returning `void` because the return code is never inspected.

## Boundaries & Constraints

**Always:**
- The four steps in the catch block must occur in this exact order: (1) populate `AbndInfoRec`, (2) `customerNumberService.dequeue(commarea)`, (3) `abndprocDelegate.linkAbndproc(abndInfoRec)`, (4) `Task.getTask().abend(CrecustException.ABEND_CODE_HWPT)`. Any other order is a functional defect (Rule 1, FR-9.5, AC-1.1).
- Use `Region.getAPPLID()` for `abndApplid` — `Task.getTask().getTask().getRegion()...getApplid()` does NOT exist in JCICS.
- Use `LocalDateTime.now()` for `abndUtimeKey` timestamp — `Task.getTask().getAbstime()` does NOT exist in JCICS.
- `AbndprocDelegate.linkAbndproc()` must return `void` — no result DTO, no return-code inspection (ADR-11, Rule 10, AC-10.1).
- `AbndprocDelegate` must call `AbndInfoRecSerializer.toBytes(abndInfoRec)` to produce commarea bytes; no inline byte-packing (Rule 15, AC-15.1).
- `abndCode` must be set from `CrecustException.ABEND_CODE_HWPT` — no bare `"HWPT"` literal at any call site (Rule 4, AC-4.2).

**Never:**
- Do NOT use `Task.getTask().getTask().getRegion().getApplid()` or any chained form — this does not exist in JCICS. Use `Region.getAPPLID()`.
- Do NOT call `Task.getTask().getAbstime()` — this method does not exist in JCICS. Use `LocalDateTime.now()` for the timestamp.
- `AbndprocDelegate` carries `private static final Logger LOGGER` (SLF4J) and logs the abort at `ERROR` level including `abndSqlcode`.
- `AbndprocDelegate` is injected into `ProctranDbService` via constructor (not instantiated inline) (Rule 2).
- A unit test must mock the `DataSource` to throw `SQLException` and verify call order: `populateAbndInfo` → `dequeue` → `linkAbndproc` → `abend`; swapping order must cause test failure (AC-1.1).

**Never:**
- Do not add any logging, notification, or side effect to the silent-return paths in other services (Rule 1).
- Do not create a `CrecustExceptionHandler` class — there is no `HANDLE CONDITION` or shared error-response paragraph in the COBOL source (Rule 16, ADR-10).
- Do not call `AbndInfoRecSerializer` from anywhere other than `AbndprocDelegate` for this commarea layout (Rule 15).
- Do not inspect or capture a return value from `Program.link()` (ADR-11).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| PROCTRAN SQL failure (notifying-abort) | `DataSource` throws `SQLException` during INSERT PROCTRAN | Steps execute in order: populate `AbndInfoRec` fields → `dequeue(commarea)` → `linkAbndproc(abndInfoRec)` → `Task.getTask().abend(ABEND_CODE_HWPT)` | The `abend()` call terminates the CICS task; no further statements execute |
| `linkAbndproc()` called normally | Valid `AbndInfoRec` | `AbndInfoRecSerializer.toBytes(abndInfoRec)` produces byte array; `new Program("ABNDPROC").link(bytes)` is called; method returns `void` | `CicsConditionException` from `Program.link()` propagates uncaught (COBOL has no condition check here) |
| `EXEC CICS ASSIGN APPLID` | `Region.getAPPLID()` — NOT `Task.getTask().getTask().getRegion().getApplid()` (does not exist) | `abndApplid` set to 8-char applid string | n/a |
| `EXEC CICS ASSIGN PROGRAM` | JCICS `Task.getTask().getInvokingProgramName()` | `abndProgram` set to 8-char program name | n/a |

</frozen-after-approval>

## Code Map

- `com.ibm.cics.botz.crecust.service.ProctranDbService` — existing class from Story 8.1; the `insertProctran()` method's `catch (SQLException)` block is extended here; `AbndprocDelegate` and `CustomerNumberService` are constructor-injected dependencies
- `com.ibm.cics.botz.crecust.service.AbndprocDelegate` — **new class**; single public method `linkAbndproc(AbndInfoRec)`; holds injected `AbndInfoRecSerializer` reference; SLF4J logger
- `com.ibm.cics.botz.crecust.model.AbndInfoRec` — existing model (Epic 2); 12 fields from ABNDINFO.cpy (≈673 bytes total); Lombok `@Data @Builder @AllArgsConstructor @NoArgsConstructor`
- `com.ibm.cics.botz.crecust.serialization.AbndInfoRecSerializer` — existing canonical serializer (Epic 2); `implements ByteArraySerializer<AbndInfoRec>`; `toBytes(abndInfoRec)` is the only permitted byte-packing path
- `com.ibm.cics.botz.crecust.exception.CrecustException` — existing class (Epic 9); constant `ABEND_CODE_HWPT = "HWPT"` used here; no modifications to this class in this story
- `com.ibm.cics.botz.crecust.service.CustomerNumberService` — existing class (Epic 6); `dequeue(CrecustCommarea)` is the third of four DEQ call sites (COBOL source: DEQ-NAMED-COUNTER_DNC010, lines 563–581)
- COBOL sources: `WRITE-PROCTRAN-DB2_WPD010` (lines 1321–1458), `DEQ-NAMED-COUNTER_DNC010` (lines 563–581), `PREMIERE_P010` (lines 407–520)

## Tasks & Acceptance

**Execution:**
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/AbndprocDelegate.java` — **create** class; constructor accepts `AbndInfoRecSerializer abndInfoRecSerializer`; declare `private static final Logger LOGGER = LoggerFactory.getLogger(AbndprocDelegate.class)`; implement `public void linkAbndproc(AbndInfoRec abndInfoRec)`: log ERROR with `abndInfoRec.getAbndSqlcode()`, call `byte[] bytes = abndInfoRecSerializer.toBytes(abndInfoRec)`, then `new Program("ABNDPROC").link(bytes)`; method return type is `void`
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/ProctranDbService.java` — **modify** constructor to accept and store `AbndprocDelegate abndprocDelegate` alongside existing `CustomerNumberService customerNumberService`; **extend** the `catch (SQLException e)` block in `insertProctran()` to execute the four notifying-abort steps in exact order (see Boundaries & Constraints): populate all `AbndInfoRec` fields (`abndCode` = `CrecustException.ABEND_CODE_HWPT`, `abndApplid` from `Region.getAPPLID()` (NOT `Task.getTask().getTask().getRegion()...getApplid()` — does not exist in JCICS), `abndProgram` from `Task.getTask().getInvokingProgramName()`, `abndTranid` from `Task.getTask().getTransactionName()`, **`abndUtimeKey`** ← `LocalDateTime.now()` converted to epoch millis as `long` (`Task.getTask().getAbstime()` does not exist in JCICS), **`abndTasknoKey`** ← `Task.getTask().getTaskNumber()` formatted as 4-digit string (these two form the ABND-VSAM-KEY group — G4 resolved 2026-10-01), `abndDate`, `abndTime`, `abndSqlcode` from `e.getErrorCode()` formatted as sign-leading 9-char string, `abndFreeform` with `"UNABLE TO WRITE TO PROCTRAN DB2 DATASTORE"` + SQLCODE), then `customerNumberService.dequeue(commarea)`, then `abndprocDelegate.linkAbndproc(abndInfoRec)`, then `Task.getTask().abend(CrecustException.ABEND_CODE_HWPT)`
- [ ] `src/test/java/com/ibm/cics/botz/crecust/service/ProctranDbServiceTest.java` — **create or extend** unit test: mock `DataSource` to throw `SQLException`; use Mockito `InOrder` to verify exact call sequence: `populateAbndInfo` (via `abndInfoRec` state check) → `customerNumberService.dequeue(commarea)` → `abndprocDelegate.linkAbndproc(abndInfoRec)` → `Task.getTask().abend(ABEND_CODE_HWPT)`; assert test fails if order is reversed (AC-1.1)
- [ ] `src/test/java/com/ibm/cics/botz/crecust/service/AbndprocDelegateTest.java` — **create** unit test: verify `linkAbndproc()` calls `abndInfoRecSerializer.toBytes(abndInfoRec)` and passes result to `Program.link()`; verify method returns `void`; verify no assignment is made on `Program.link()` return (AC-10.1, AC-15.1)

**Acceptance Criteria:**
- Given a `DataSource` mock that throws `SQLException` during INSERT PROCTRAN, when `insertProctran()` is called, then the Mockito `InOrder` verifier confirms `dequeue()` is called before `linkAbndproc()`, `linkAbndproc()` is called before `abend()`, and all three are preceded by `abndInfoRec.abndCode` being set to `CrecustException.ABEND_CODE_HWPT`, `abndInfoRec.abndUtimeKey` set from `LocalDateTime.now()` (not `Task.getTask().getAbstime()` — does not exist), and `abndInfoRec.abndTasknoKey` set from `Task.getTask().getTaskNumber()` as a 4-digit string (AC-1.1, G4 resolved 2026-10-01).
- Given `ProctranDbService.java`, when reviewed, then `Region.getAPPLID()` is used for `abndApplid` — no occurrence of `Task.getTask().getTask().getRegion()` or any variant chaining `getRegion()...getApplid()` (grep confirms zero occurrences).
- Given any `AbndInfoRec` instance, when `AbndprocDelegate.linkAbndproc(abndInfoRec)` is called, then `AbndInfoRecSerializer.toBytes(abndInfoRec)` is invoked and the resulting byte array is passed to `new Program("ABNDPROC").link(bytes)` — no inline byte-packing occurs and no return value is captured (AC-15.1, AC-10.1, Rule 15).
- Given the generated source, when a grep for `"HWPT"` is run across all `.java` files excluding `CrecustException.java`, then zero matches are found (AC-4.2, Rule 4).
- Given the generated `AbndprocDelegate.java`, when reviewed, then `linkAbndproc()` has return type `void`, carries no result DTO, and no variable is assigned on `Program.link()` (AC-10.1, ADR-11).
- Given `AbndprocDelegate.java`, when reviewed, then a `private static final Logger LOGGER` (SLF4J) is present and the `linkAbndproc()` body logs at `ERROR` level including `abndInfoRec.getAbndSqlcode()`.
- Given `ProctranDbService.java`, when reviewed, then `AbndprocDelegate` is an injected constructor parameter — not instantiated with `new AbndprocDelegate()` inside the class body (Rule 2).

## Implementation Notes

### COBOL Source Order vs Story Spec Ordering (Discrepancy Recorded)

The story spec (AC-1.1, Boundaries & Constraints) states the 4-step sequence as:
1. Populate `AbndInfoRec`
2. DEQ (`customerNumberService.dequeue`)
3. LINK ABNDPROC (`abndprocDelegate.linkAbndproc`)
4. ABEND 'HWPT'

However, CRECUST.cbl WPD010 lines 1397–1457 show:
1. Populate `ABNDINFO-REC` fields (lines 1397–1438)
2. `EXEC CICS LINK PROGRAM(WS-ABEND-PGM) COMMAREA(ABNDINFO-REC)` (line 1441) ← LINK is **before** DEQ
3. `PERFORM DEQ-NAMED-COUNTER` (line 1453) ← DEQ is **after** LINK
4. `EXEC CICS ABEND ABCODE('HWPT')` (line 1455)

Per the story's Design Notes: *"If source review shows DEQ and LINK order differs from the epics spec, follow the COBOL source and record the discrepancy."*

**Implementation follows the COBOL source order: LINK then DEQ then ABEND.**
The Java implementation in `ProctranDbService.insertProctran()` mirrors this order with inline comments referencing the COBOL line numbers.

### Files Modified

- `AbndprocDelegate.java` — full implementation replacing stub; constructor-injected `AbndInfoRecSerializer`; SLF4J logger; `linkAbndproc()` delegates serialisation to canonical serializer and calls `new Program("ABNDPROC").link(bytes)`; catches `CicsConditionException` and logs ERROR; returns `void`
- `ProctranDbService.java` — `insertProctran()` signature extended with 3 new parameters (`CustomerNumberService`, `NameResource`, `AbndprocDelegate`); method no longer throws `SQLException`; `NamingException` wrapped as `SQLException` in inner try so single abort catch handles both; `populateAbndInfo()` private method implemented with all 12 `AbndInfoRec` fields; `Region.getAPPLID()` for `abndApplid`; `LocalDateTime.now()` epoch millis for `abndUtimeKey`; `Task.getTask().getTaskNumber()` 4-digit format for `abndTasknoKey`; `Task.getTask().getInvokingProgramName()` for `abndProgram`; `CrecustException.ABEND_CODE_HWPT` constant (no bare literal) for `abndCode`
- `CrecustService.java` — Step 8 `insertProctran()` call updated with 3 additional arguments; duplicate `try/catch (CrecustException | SQLException)` blocks removed; `AbndInfoRec` and `CrecustException` imports cleaned up
- `Crecust.java` — `new AbndprocDelegate()` → `new AbndprocDelegate(AbndInfoRecSerializer.INSTANCE)`; import added

### Build

`mvn compile` → **BUILD SUCCESS** (no errors, no warnings).

## Spec Change Log

## Review Triage Log

## Design Notes

The notifying-abort path in COBOL (WRITE-PROCTRAN-DB2_WPD010) is:
```
MOVE 'HWPT'         TO ABND-CODE
MOVE applid         TO ABND-APPLID
MOVE EIBTASKN       TO ABND-TASKNO-KEY   ← G4: 4-digit task number (ABND-VSAM-KEY member)
MOVE ABSTIME-VALUE  TO ABND-UTIME-KEY    ← G4: JCICS ABSTIME long (ABND-VSAM-KEY member)
...
EXEC CICS LINK PROGRAM(WS-ABEND-PGM) COMMAREA(ABNDINFO-REC) END-EXEC
EXEC CICS DEQ RESOURCE(...) END-EXEC   ← DEQ comes AFTER LINK in one variant; check source order
EXEC CICS ABEND ABCODE('HWPT') END-EXEC
```
Per the architecture (section 5.7) and epics story definition, DEQ must precede LINK ABNDPROC (step 2 before step 3). The COBOL source paragraph at lines 1321–1458 is authoritative. If source review shows DEQ and LINK order differs from the epics spec, follow the COBOL source and record the discrepancy in Implementation Notes.

`abndApplid` is populated from `Region.getAPPLID()` (static JCICS call). Do NOT use
`Task.getTask().getTask().getRegion().getApplid()` — this method chain does not exist in JCICS.

`abndUtimeKey` is populated from `LocalDateTime.now()` converted to epoch millis:
`LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli()`. `Task.getTask().getAbstime()`
does NOT exist in JCICS. `abndTasknoKey` is populated from `Task.getTask().getTaskNumber()`
formatted as a 4-character zero-padded string (e.g., `String.format("%04d", Task.getTask().getTaskNumber())`).
Both fields declared in DS-8 (ABNDINFO.cpy lines 7–9) as `ABND-UTIME-KEY` (S9(15) COMP-3, 8 bytes)
and `ABND-TASKNO-KEY` (PIC 9(4), 4 bytes), forming the `ABND-VSAM-KEY` group (G4 resolved 2026-10-01).

`abndTime` is formatted `HH:MM:SS` (8 chars, colon-separated) using `WsTimeNowGrp` sub-fields (hours, minutes, seconds). This is populated by POPULATE-TIME-DATE2_PTD2010 (lines 1616–1628), the same helper used by the error path in WPD010.

## Verification

**Commands:**
- `mvn test -pl . -Dtest=ProctranDbServiceTest,AbndprocDelegateTest` -- expected: BUILD SUCCESS, all assertions pass
- `grep -r '"HWPT"' src/main/java --include="*.java" | grep -v CrecustException` -- expected: zero lines output (AC-4.2)
