---
title: 'Story 9.2: Rule 16 Grounding — No Exception Handler Without COBOL Construct'
type: 'feature'
created: '2026-10-01'
status: 'draft'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '_bmad-output/implementation-artifacts/CRECUST/epic-9-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Rule 16 (ADR-10) requires that every exception-handler class maps to a concrete
COBOL error-handling construct. Without a documented grounding map, a future developer could
incorrectly generate a spurious `CrecustExceptionHandler` class that has no COBOL basis,
creating dead code and violating ADR-10 and AC-16.1/AC-16.2.

**Approach:** Document the Rule 16 grounding map inside a class-level JavaDoc comment on
`CrecustException`, confirming that no `*ExceptionHandler*` or `*ErrorHandler*` class exists,
that every error path is handled inline in its respective service class, and that every catch
block performs at least one side effect before returning.

## Boundaries & Constraints

**Always:**
- The grounding map must live as a `@since 1.0.0` class-level JavaDoc comment on
  `CrecustException` — not in a README or separate doc file.
- Every documented COBOL construct must trace to a real paragraph in `CRECUST.cbl` (with line
  numbers matching the architecture and epics source references).
- All service catch blocks must use named factory methods from `CrecustException`; no inline
  construction of error responses (Rule 2, AC-2.1). Each catch block calls
  `commarea.setCommFailCode(e.getFailCode())` and `commarea.setCommSuccess('N')` after catching
  `CrecustException e` — factory methods do not mutate the commarea (G3).
- No catch block may exist whose sole behaviour is to re-throw with no side effect (AC-16.2).

**Never:**
- Do not generate any class named `*ExceptionHandler*` or `*ErrorHandler*`.
- Do not add a new catch-block wrapper class or a delegating handler layer.
- Do not alter the `CrecustException` factory methods or constants defined in Story 9.1.
- Do not modify service class logic beyond ensuring each catch block has at least one side
  effect — this story is a documentation and conformance story, not a logic change story.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Handler class absent | Full class listing searched for `*ExceptionHandler*` / `*ErrorHandler*` | Zero matches | N/A |
| Catch block side-effect present | Each catch block in ProctranDbService, CustomerDbService, CustomerNumberService, CreditCheckService reviewed | At least one side-effect statement (fail-code set, DEQ call, or linkAbndproc) precedes any re-throw or return | If a catch block only re-throws, add the missing side-effect from the architecture error-path table |
| COBOL grounding complete | JavaDoc on CrecustException class | Each of the 5 COBOL constructs listed in AC-16.1 appears in the Javadoc grounding map | N/A |
| Factory method use enforced | Each catch block that sets a fail-code | Uses a named factory (`CrecustException.enqFailed(...)` etc.) not a raw character literal | N/A |

</frozen-after-approval>

## Code Map

- `src/base/cics/cobol/CRECUST.cbl` — COBOL source; paragraphs `WRITE-PROCTRAN-DB2_WPD010`
  (lines 1321–1458), `WRITE-CUSTOMER-DB2_WCD010` (lines 1139–1306),
  `GET-LAST-CUSTOMER-DB2_GLCD010` (lines ~1491–1540), `CREDIT-CHECK_CC010` (lines 605–1131),
  `ENQ-NAMED-COUNTER_ENC010` (lines 541–556), `DEQ-NAMED-COUNTER_DNC010` (lines 563–581)
  are the grounding source for the five COBOL constructs.
- `_bmad-output/planning-artifacts/CRECUST/architecture.md` — ADR-10 (lines 284–307) specifies
  the exact decision text that must appear in the grounding Javadoc.
- `_bmad-output/planning-artifacts/CRECUST/prd.md` — AC-16.1 and AC-16.2 (lines 881–894)
  define the acceptance bar.
- `src/main/java/com/ibm/cics/botz/crecust/exception/CrecustException.java` — target file;
  produced by Story 9.1. This story adds/amends the class-level Javadoc only.
- `src/main/java/com/ibm/cics/botz/crecust/service/ProctranDbService.java` — contains the
  notifying-abort catch block (only path that calls linkAbndproc then abend).
- `src/main/java/com/ibm/cics/botz/crecust/service/CustomerDbService.java` — contains the
  INSERT CUSTOMER silent-return catch block.
- `src/main/java/com/ibm/cics/botz/crecust/service/CustomerNumberService.java` — contains
  SELECT/UPDATE CONTROL and ENQ/DEQ catch blocks.
- `src/main/java/com/ibm/cics/botz/crecust/service/CreditCheckService.java` — contains
  PUT CONTAINER, RUN TRANSID, and FETCH ANY catch blocks.

## Tasks & Acceptance

**Execution:**
- [ ] `src/main/java/com/ibm/cics/botz/crecust/exception/CrecustException.java` —
  Add (or replace) the class-level Javadoc with the Rule 16 grounding map: list each of the
  5 COBOL constructs, its source paragraph and line range, and the Java service class / catch
  block it maps to. Include `@author` and `@since 1.0.0` tags. Do not alter any constants,
  factory methods, or other code.
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/ProctranDbService.java` —
  Verify the PROCTRAN SQL failure catch block calls (in order):
  (1) `abndprocDelegate.linkAbndproc(abndInfoRec)`,
  (2) `Task.getTask().abend(CrecustException.ABEND_CODE_HWPT)`.
  Verify no intermediate re-throw occurs. No logic changes needed if already correct;
  add the missing ordering or side-effect only if the block deviates.
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/CustomerDbService.java` —
  Verify the INSERT CUSTOMER catch block sets the fail-code via
  `CrecustException.insertCustomerFailed(...)` (or equivalent factory) before returning.
  Correct if it uses a raw literal or re-throws without a side effect.
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/CustomerNumberService.java` —
  Verify all four catch blocks (ENQ, SELECT CONTROL, UPDATE CONTROL, DEQ) set the fail-code
  via the corresponding named factory before returning. Correct if any use raw literals.
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/CreditCheckService.java` —
  Verify catch blocks for PUT CONTAINER, RUN TRANSID, FETCH ANY (NOTFINISHED, INVREQ, and
  ABEND sub-paths) each set the fail-code via a named factory before returning or re-throwing.

**Acceptance Criteria:**

- Given the full generated codebase is searched with `grep -r "ExceptionHandler\|ErrorHandler"
  src/main/java/com/ibm/cics/botz/crecust/`, when the search completes, then zero matches are
  found (AC-16.1 — no spurious handler class).
- Given `CrecustException.java` is opened, when the class-level Javadoc is read, then it
  contains entries for all five COBOL grounding constructs:
  `WRITE-PROCTRAN-DB2_WPD010`, `WRITE-CUSTOMER-DB2_WCD010`, `GET-LAST-CUSTOMER-DB2_GLCD010`,
  `CREDIT-CHECK_CC010`, and `ENQ-NAMED-COUNTER_ENC010` / `DEQ-NAMED-COUNTER_DNC010`,
  each with its source paragraph name and Java service class mapping (AC-16.1).
- Given every service catch block in `ProctranDbService`, `CustomerDbService`,
  `CustomerNumberService`, and `CreditCheckService` is reviewed, when each block is inspected,
  then every block contains at least one side-effect statement (fail-code set via named factory,
  DEQ call, or `linkAbndproc` call) before any re-throw or return (AC-16.2).
- Given a grep is run for bare `"HWPT"` string literals with
  `grep -r '"HWPT"' src/main/java/com/ibm/cics/botz/crecust/` excluding the constant
  declaration line, when the search completes, then zero matches are found (AC-4.2).
- Given `CrecustException.java` is opened and its top-level declarations are counted,
  when the file is inspected, then exactly one top-level type declaration exists (Rule 17,
  AC-17.1).

## Implementation Notes

### Dev Notes (Story 9.2 — 2026-10-09)

**Verification grep results and AC verdicts:**

#### AC-16.1 — No `*ExceptionHandler*` or `*ErrorHandler*` class

```
grep -rn "ExceptionHandler\|ErrorHandler" crecust-java/src/
```
Result: **zero matches** → ✅ PASS

#### AC-16.1 — COBOL grounding Javadoc complete on `CrecustException`

Class-level Javadoc was present from Story 9.1 but lacked the explicit 5-construct ADR-10 table.
Added dedicated `<b>Rule 16 COBOL construct grounding (ADR-10 — Story 9.2):</b>` section with a
numbered `<ol>` listing all five COBOL constructs, paragraph names, line ranges, Java service
class, and catch scope. Also added `@author` and `@since 1.0.0` tags. → ✅ PASS

Five constructs documented:
1. `WRITE-PROCTRAN-DB2_WPD010` (lines 1321–1458) → `ProctranDbService.insertProctran()` notifying-abort
2. `WRITE-CUSTOMER-DB2_WCD010` (lines 1139–1306) → `CustomerDbService.insertCustomer()` silent-return
3. `GET-LAST-CUSTOMER-DB2_GLCD010` (lines ~1491–1540) → `CustomerNumberService.getAndIncrementCustomerNumber()` silent-return
4. `CREDIT-CHECK_CC010` (lines 605–1131) → `CreditCheckService` inline catch blocks (fail-codes A–H)
5. `ENQ-NAMED-COUNTER_ENC010` / `DEQ-NAMED-COUNTER_DNC010` (lines 541–581) → `CustomerNumberService.enqueue()` / `.dequeue()` silent-return

#### AC-16.2 — Each COBOL construct maps to inline catch block

```
grep -rn "catch.*SQLException" crecust-java/src/main/java/
```
Hits in: `CustomerDbService:241`, `CustomerNumberService:189`, `ProctranDbService:183` → ✅ PASS

```
grep -rn "catch.*CicsConditionException" crecust-java/src/main/java/
```
Hits in: `CreditCheckService:174,208,321`, `CustomerNumberService:114,314`,
`AbndprocDelegate:71` (expected) → ✅ PASS

```
grep -rn "abndprocDelegate.linkAbndproc\|Task.getTask().abend" crecust-java/src/main/java/
```
Hits:
- `ProctranDbService:201` — `abndprocDelegate.linkAbndproc(abndInfoRec)` ✅
- `ProctranDbService:207` — `Task.getTask().abend(CrecustException.ABEND_CODE_HWPT)` ✅
- Other matches are Javadoc comments only → ✅ PASS (only `ProctranDbService` issues abend)

#### AC-16.2 — No catch block that only re-throws

Reviewed all catch blocks in scope:
- `ProctranDbService.insertProctran()` SQL catch: populates `AbndInfoRec`, calls `linkAbndproc`, DEQ, then abend → ✅ side-effects before termination
- `CustomerDbService.insertCustomer()` SQL/Naming catch: logs error, calls `dequeue()`, sets `commSuccess='N'` and `commFailCode` → ✅ side-effects before return
- `CustomerNumberService.getAndIncrementCustomerNumber()` SQL/Naming catch: delegates to `failControlSql()` (DEQ + fail-code) → ✅
- `CustomerNumberService.enqueue()` CICS catch: logs error, sets `commSuccess='N'` and `commFailCode` → ✅
- `CustomerNumberService.dequeue()` CICS catch: logs error, sets `commSuccess='N'` and `commFailCode` → ✅
- `CreditCheckService.putContainer()` CICS catch: logs error, sets `commSuccess` and `commFailCode` → ✅
- `CreditCheckService.runChildTransaction()` CICS catch: creates factory exception, reads fail-code via `ex.getCommFailCode()` → ✅
- All `fetchAny` catches: set fail-code via factory methods → ✅
→ ✅ PASS — every catch block performs at least one side effect

#### AC-4.2 — Named constants / no bare literals for `setCommFailCode()`

```
grep -rn 'setCommFailCode("[A-Z0-9]")' crecust-java/src/main/java/
```
**Violations found and fixed:**

| File | Line | Violation | Fix applied |
|------|------|-----------|-------------|
| `ValidationService.java` | 50 | `setCommFailCode("T")` | → `CrecustException.FAIL_CODE_INVALID_TITLE` |
| `ValidationService.java` | 80 | `setCommFailCode("Z")` | → `CrecustException.FAIL_CODE_CEEDAYS_FAIL` |
| `ValidationService.java` | 87 | `setCommFailCode("O")` | → `CrecustException.FAIL_CODE_DOB_RANGE` |
| `ValidationService.java` | 97 | `setCommFailCode("Z")` | → `CrecustException.FAIL_CODE_CEEDAYS_FAIL` |
| `ValidationService.java` | 105 | `setCommFailCode("O")` | → `CrecustException.FAIL_CODE_DOB_RANGE` |
| `ValidationService.java` | 114 | `setCommFailCode("Y")` | → `CrecustException.FAIL_CODE_DOB_FUTURE` |
| `CreditCheckService.java` | 178 | `setCommFailCode(FAIL_CODE_PUT_CONTAINER)` where `FAIL_CODE_PUT_CONTAINER` was a local copy `"A"` | → `CrecustException.FAIL_CODE_PUT_CONTAINER`; local constant removed |

Also added `import com.ibm.cics.botz.crecust.exception.CrecustException` to `ValidationService.java`.

Post-fix grep result: **zero matches** → ✅ PASS

#### AC-4.2 — No bare `"HWPT"` literal outside `CrecustException.java`

```
grep -rn '"HWPT"' crecust-java/src/ | grep -v CrecustException.java
```
Result: one match — `ProctranDbService.java:206` — this is a **comment** (`// Step 4 — ABEND 'HWPT'`), not a code literal. No code literal violation → ✅ PASS

#### `mvn compile` result

```
[INFO] BUILD SUCCESS
```
No errors. One pre-existing deprecation warning in `CreditCheckService.java` (`ccSecError()` is
`@Deprecated`; this is from prior implementation stories, not introduced here) → ✅ PASS

## Spec Change Log

## Review Triage Log

## Design Notes

The five COBOL grounding constructs and their Java mappings:

| COBOL paragraph | COBOL construct type | Java service class | Catch scope |
|---|---|---|---|
| `WRITE-PROCTRAN-DB2_WPD010` (lines 1321–1458) | Inline SQLCODE check; notifying-abort | `ProctranDbService.insertProctran()` | SQL exception → linkAbndproc → abend |
| `WRITE-CUSTOMER-DB2_WCD010` (lines 1139–1306) | Inline SQLCODE check; silent-return | `CustomerDbService.insertCustomer()` | SQL exception → fail-code `'1'` |
| `GET-LAST-CUSTOMER-DB2_GLCD010` (lines ~1491–1540) | Inline SQLCODE check; silent-return | `CustomerNumberService` | SQL exception → DEQ → fail-code `'4'` |
| `CREDIT-CHECK_CC010` (lines 605–1131) | Inline EIBRESP/EIBRESP2 checks | `CreditCheckService` | CICS condition exception → fail-code per sub-path |
| `ENQ-NAMED-COUNTER_ENC010` / `DEQ-NAMED-COUNTER_DNC010` (lines 541–581) | Inline EIBRESP checks | `CustomerNumberService` | CICS condition exception → fail-code `'3'` / `'5'` |

The grounding Javadoc on `CrecustException` must reproduce this table in prose so that any
future developer can verify Rule 16 compliance without opening the COBOL source.

## Verification

**Commands:**
- `grep -r "ExceptionHandler\|ErrorHandler" src/main/java/com/ibm/cics/botz/crecust/` —
  expected: zero matches
- `grep -rn '"HWPT"' src/main/java/com/ibm/cics/botz/crecust/` — expected: exactly one match
  (the constant declaration in `CrecustException.java`)
- `mvn compile -pl crecust` — expected: BUILD SUCCESS with no compilation errors
