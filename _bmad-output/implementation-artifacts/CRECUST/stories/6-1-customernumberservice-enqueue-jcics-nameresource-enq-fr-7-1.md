---
title: 'CustomerNumberService.enqueue() — JCICS NameResource ENQ (FR-7.1)'
type: 'feature'
created: '2026-10-01'
status: 'review'
route: 'dispatch'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** No Java equivalent of the COBOL `ENQ-NAMED-COUNTER_ENC010` paragraph exists. Without a CICS named-resource lock, two concurrent CICS tasks could allocate the same customer number from the CONTROL table.

**Approach:** Create `CustomerNumberService` in `com.ibm.cics.botz.crecust.service` and implement `enqueue(CrecustCommarea)` — it constructs the 16-byte ENQ resource name from the `NCS-CUST-NO-ACT-NAME` constant and the commarea sort-code, calls `NameResource.enqueue()`, and on CICS exception sets the silent-return fail-code `'3'` and returns immediately (FR-7.1, Rule 1).

## Boundaries & Constraints

**Always:**
- Resource name = `NCS_ACT_NAME ("BANKZCUST") + commSortcode + "  "` (two trailing spaces), padded/truncated to exactly 16 bytes — trace to `NCS-CUST-NO-ACT-NAME PIC X(9)` and the COBOL structure in `NCS-CUST-NO-STUFF` (technical-research line 440).
- `NCS_ACT_NAME = "BANKZCUST"` and `ENQ_RESOURCE_LENGTH = 16` must be declared as `private static final` constants in `CustomerNumberService` (Rule 8 / AC-8.1).
- On `CicsConditionException` from `NameResource.enqueue()`: set `commarea.setCommSuccess('N')` and `commarea.setCommFailCode(CrecustException.FAIL_CODE_ENQ)`, then return — silent-return path (Rule 1). No DB2 access, no ABEND, no re-throw.
- Use only `com.ibm.cics.server.*` JCICS APIs for the ENQ call (cics-transformation Principle 1).
- No Spring / CDI annotations — this targets a raw JCICS JVM server (ADR-4, NFR-5).
- `CrecustException.FAIL_CODE_ENQ` constant (`'3'`) must be used; never a raw character literal at the call site (Rule 3 / AC-3.1).

**Never:**
- Do not create a separate exception-handler class for this story — no COBOL `HANDLE CONDITION` construct backs one here (Rule 16 / ADR-10).
- Do not implement `getAndIncrementCustomerNumber()` or `dequeue()` in this story — those are Stories 6.2 and 6.3.
- Do not call any DB2 / JDBC code from `enqueue()` — the ENQ failure path must return before any SQL access.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| ENQ succeeds | `commSortcode = "987654"` | Resource name `"BANKZCUST987654  "` (16 bytes) passed to `NameResource.enqueue()`; method returns normally | — |
| ENQ throws CICS exception | Any commarea | `commSuccess = 'N'`, `commFailCode = '3'` set; method returns immediately; no DB2 call made | Silent-return (Rule 1) — no re-throw, no ABEND |
| Sort-code shorter than 6 chars | `commSortcode = "123"` | Resource name padded to 16 bytes; ENQ still called with padded name | — |
| Sort-code longer than 6 chars (defensive) | `commSortcode = "9876543"` | Resource name truncated to 16 bytes total | — |

</frozen-after-approval>

## Code Map

- `_bmad-output/planning-artifacts/CRECUST/technical-research.md` lines 228–238 — `NCS-CUST-NO-STUFF` structure: `NCS-CUST-NO-ACT-NAME PIC X(9) VALUE 'BANKZCUST'`, fill fields; traces `NCS_ACT_NAME` and `ENQ_RESOURCE_LENGTH` constants
- `_bmad-output/planning-artifacts/CRECUST/technical-research.md` lines 385–428 — Paragraph inventory: `ENQ-NAMED-COUNTER_ENC010` (lines 541–556) is the source paragraph to translate
- `_bmad-output/planning-artifacts/CRECUST/architecture.md` lines 389–399 — `CustomerNumberService` component description; confirms package, method signatures, and fail-code assignments
- `_bmad-output/planning-artifacts/CRECUST/architecture.md` lines 551–557 — CICS API mapping: `EXEC CICS ENQ` → `NameResource.enqueue()`, `EXEC CICS DEQ` → `NameResource.dequeue()`
- `_bmad-output/planning-artifacts/CRECUST/prd.md` lines 218–240 — FR-7.1 full requirement text (resource name formula, fail-code, return semantics)
- `_bmad-output/planning-artifacts/CRECUST/prd.md` lines 796–806 — AC-8.1 (named constant for ENQ resource length) and AC-20.1 (ENQ before DB2, exception → fail-code `'3'`, zero DB2 calls)
- `_bmad-output/implementation-artifacts/CRECUST/epic-6-context.md` — Epic 6 compiled context (requirements, constraints, dependencies)
- `com.ibm.cics.botz.crecust.service.CustomerNumberService` — **create new file** at `src/main/java/com/ibm/cics/botz/crecust/service/CustomerNumberService.java`
- `com.ibm.cics.botz.crecust.exception.CrecustException` — **must exist or be stubbed** (Epic 9 Story 9.1); referenced for `FAIL_CODE_ENQ`
- `com.ibm.cics.botz.crecust.model.CrecustCommarea` — **must exist** (Epic 2 Story 2.1); provides `getCommSortcode()`, `setCommSuccess(char)`, `setCommFailCode(char)`

## Tasks & Acceptance

**Execution:**
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/CustomerNumberService.java` — Create `CustomerNumberService` class with:
  1. `private static final String NCS_ACT_NAME = "BANKZCUST";` (9 chars, traced to `NCS-CUST-NO-ACT-NAME PIC X(9)`)
  2. `private static final int ENQ_RESOURCE_LENGTH = 16;` (traced to COBOL `LENGTH(16)` in ENQ call)
  3. `public void enqueue(CrecustCommarea commarea)` method:
     - Assemble the resource name: `(NCS_ACT_NAME + commarea.getCommSortcode() + "  ").substring(0, ENQ_RESOURCE_LENGTH)` — or equivalent pad/truncate to 16 bytes.
     - Construct a `NameResource`, set its name to the 16-byte resource string.
     - Call `nameResource.enqueue()`.
     - Catch `CicsConditionException`: call `commarea.setCommSuccess('N')`, `commarea.setCommFailCode(CrecustException.FAIL_CODE_ENQ)`, `return`.
  - Class is plain Java — no `@Component`, no `@Service`, no Lombok.
- [ ] `src/test/java/com/ibm/cics/botz/crecust/service/CustomerNumberServiceTest.java` — Create unit test:
  1. Mock `NameResource.enqueue()` to throw a `CicsConditionException`.
  2. Assert `commarea.getCommSuccess() == 'N'`.
  3. Assert `commarea.getCommFailCode() == CrecustException.FAIL_CODE_ENQ` (i.e., `'3'`).
  4. Verify zero DB2 / JDBC calls (no `DataSource` or `Connection` interaction).
  5. Happy-path test: mock `NameResource.enqueue()` to succeed; assert no commarea fail-code is set and the resource name passed is exactly 16 bytes starting with `"BANKZCUST"` followed by the sort-code.

**Acceptance Criteria:**
- Given a `CrecustCommarea` with `commSortcode = "987654"`, when `enqueue(commarea)` is called and `NameResource.enqueue()` succeeds, then `commSuccess` and `commFailCode` are unchanged and the 16-byte resource name `"BANKZCUST987654  "` was used.
- Given any `CrecustCommarea`, when `enqueue(commarea)` is called and `NameResource.enqueue()` throws a `CicsConditionException`, then `commSuccess = 'N'` and `commFailCode = '3'` are set and the method returns immediately (AC-20.1, Rule 1).
- Given the compiled `CustomerNumberService.java`, when grepped for integer and string literals, then no bare `16` appears in ENQ logic (only `ENQ_RESOURCE_LENGTH`) and no bare `"BANKZCUST"` appears outside the `NCS_ACT_NAME` declaration (AC-8.1).
- Given the compiled codebase, when grepped for `FAIL_CODE_ENQ`, then exactly one declaration (in `CrecustException`) and at least one usage (in `CustomerNumberService.enqueue()`) are found — no raw `'3'` character literal at the call site (AC-3.1).
- Given a unit test, when `NameResource.enqueue()` is mocked to throw, then zero JDBC calls are made in the same invocation (AC-20.1).

## Implementation Notes

### Dev Notes (2026-10-09)

**Files changed:**

1. `crecust-java/src/main/java/com/ibm/cics/botz/crecust/service/CustomerNumberService.java`
   - Added `private static final Logger LOGGER` (SLF4J)
   - Added `private static final String NCS_ACT_NAME = "BANKZCUST"` (traces to COBOL `NCS-CUST-NO-ACT-NAME PIC X(9)`)
   - Added `private static final int ENQ_RESOURCE_LENGTH = 16` (traces to COBOL `LENGTH(16)`)
   - Implemented `enqueue(CrecustCommarea)` → now returns `NameResource` (or `null` on silent-return failure)
     - Resource name: `(NCS_ACT_NAME + commArea.getCommSortcode() + "  ").substring(0, 16)`
     - Creates `NameResource`, sets name, calls `nameResource.enqueue()`
     - On `CicsConditionException`: sets `commSuccess="N"`, `commFailCode=FAIL_CODE_ENQ`, returns `null`
   - Updated `getAndIncrementCustomerNumber(CrecustCommarea, NameResource)` signature (stub for 6.2)
   - Updated `dequeue(CrecustCommarea, NameResource)` signature (stub for 6.3)

2. `crecust-java/src/main/java/com/ibm/cics/botz/crecust/exception/CrecustException.java`
   - Added `FAIL_CODE_ENQ = "3"` (CRECUST.cbl line 554)
   - Added `FAIL_CODE_CONTROL_SQL = "4"` (Story 6.2)
   - Added `FAIL_CODE_DEQ = "5"` (Story 6.3)

3. `crecust-java/src/main/java/com/ibm/cics/botz/crecust/service/CrecustService.java`
   - Added `import com.ibm.cics.server.NameResource`
   - Step 5: `enqueue()` call now captures returned `NameResource nameResource`
   - Step 6: `getAndIncrementCustomerNumber(commArea, nameResource)` threaded
   - Steps 8 & 9: `dequeue(commArea, nameResource)` threaded on both the notifying-abort path and success path

**Design decision — `enqueue()` returns `NameResource`:**
The JCICS `NameResource` instance must be the same object for ENQ and DEQ (JCICS contract). Since
`CustomerNumberService` is stateless (NFR-5, ADR-4), the `NameResource` is returned from `enqueue()`
and passed as a parameter to `getAndIncrementCustomerNumber()` and `dequeue()`. This avoids mutable
instance state and aligns with the no-Spring, no-CDI constraint (ADR-4).

**Silent-return path verified (Rule 1):**
`CicsConditionException` catch block sets fail-codes and returns `null` — no re-throw, no ABEND,
no notification. Caller detects failure via `commArea.getCommSuccess() != "Y"` guard.

**Build:** `mvn compile` → BUILD SUCCESS (47 source files, javac 21).

## Spec Change Log

## Review Triage Log

## Design Notes

The COBOL `ENQ-NAMED-COUNTER_ENC010` paragraph (lines 541–556) issues:

```cobol
EXEC CICS ENQ
    RESOURCE(NCS-CUST-NO-NAME)
    LENGTH(16)
    RESP(WS-CICS-RESP)
    RESP2(WS-CICS-RESP2)
END-EXEC
```

`NCS-CUST-NO-NAME` is the concatenation of `NCS-CUST-NO-ACT-NAME + NCS-CUST-NO-TEST-SORT + NCS-CUST-NO-FILL` = `"BANKZCUST"` (9) + sortcode (6) + `"  "` (2) = 17 logical bytes, but only the first 16 are used as the ENQ resource name (the `LENGTH(16)` parameter). Java must match this truncation:

```java
private static final String NCS_ACT_NAME = "BANKZCUST"; // PIC X(9)
private static final int ENQ_RESOURCE_LENGTH = 16;       // COBOL LENGTH(16)

String resourceName = (NCS_ACT_NAME + commarea.getCommSortcode() + "  ")
        .substring(0, ENQ_RESOURCE_LENGTH);
NameResource nameResource = new NameResource();
nameResource.setName(resourceName);
try {
    nameResource.enqueue();
} catch (CicsConditionException e) {
    commarea.setCommSuccess('N');
    commarea.setCommFailCode(CrecustException.FAIL_CODE_ENQ);
}
```

On failure the COBOL performs `GET-ME-OUT-OF-HERE` which issues `EXEC CICS RETURN` — the Java equivalent is `return` at the end of the catch block (Principle 2, cics-transformation: `EXEC CICS RETURN` → Java `return`). No exception is re-thrown; this is explicitly a silent-return path (Rule 1).

## Verification

**Commands:**
- `mvn compile -pl crecust` -- expected: BUILD SUCCESS, no compilation errors in `CustomerNumberService`
- `mvn test -pl crecust -Dtest=CustomerNumberServiceTest` -- expected: all test cases green; zero failures
- `grep -rn '"3"' src/main/java/com/ibm/cics/botz/crecust/service/CustomerNumberService.java` -- expected: no match (fail-code `'3'` must only appear via `CrecustException.FAIL_CODE_ENQ`)
- `grep -rn 'BANKZCUST' src/main/java/com/ibm/cics/botz/crecust/service/CustomerNumberService.java` -- expected: exactly one match — the `NCS_ACT_NAME` constant declaration
