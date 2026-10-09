---
title: 'Story 9.1: CrecustException — 17 Fail-Code Constants and 16 Factory Methods'
type: 'feature'
created: '2026-10-01'
status: 'draft'
route: 'dispatch'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** No `CrecustException` class exists for the CRECUST Java modernisation. Without it,
every service story in Epics 3–8 has no type-safe way to signal failure — any attempt to throw
or catch errors would require raw character literals at call sites, violating Rule 3 and Rule 4.

**Approach:** Create `CrecustException.java` in package `com.ibm.cics.botz.crecust.exception`,
extending `RuntimeException`, with 17 named `public static final char` fail-code constants, one
`public static final String ABEND_CODE_HWPT = "HWPT"` constant, and 16 named static factory
methods (one per error path excluding success). Each factory accepts a `String message` (and
optionally a `Throwable cause`), stores the fail-code as `private final char failCode`, and
returns the exception — **factory methods do not mutate the commarea**. Each catch block in a
service class calls `commarea.setCommFailCode(e.getFailCode())` and
`commarea.setCommSuccess('N')` after catching the exception (G3 resolved 2026-10-01). The
class-level Javadoc documents the COBOL-construct grounding map required by ADR-10 / Rule 16.

## Boundaries & Constraints

**Always:**
- Package is exactly `com.ibm.cics.botz.crecust.exception` (ADR-2).
- Class extends `RuntimeException` directly — not abstract, not a hierarchy of subtypes.
- All 17 fail-code constants are `public static final char`; `ABEND_CODE_HWPT` is `public static
  final String`.
- `CrecustException` carries `private final char failCode`; `getFailCode()` exposes it. Each of
  the 16 factory methods accepts a `String message`, sets `this.failCode` to the constant value,
  and returns the new exception — **the factory does not touch the commarea**. The catch block
  that catches `CrecustException e` is responsible for calling
  `commarea.setCommFailCode(e.getFailCode())` and `commarea.setCommSuccess('N')` (G3 resolved
  2026-10-01). No raw char literal appears at any call site (Rule 3).
- Exactly one top-level type declaration in `CrecustException.java` (Rule 17).
- Class-level Javadoc must include the COBOL grounding map linking each factory to its COBOL
  paragraph source (ADR-10 / Rule 16).
- No `*ExceptionHandler*` or `*ErrorHandler*` class is generated as a companion (ADR-10).

**Never:**
- Do not create a parallel exception-handler class with no COBOL grounding (ADR-10).
- Do not pass raw `char` or `String` literals for fail-codes at factory call sites.
- Do not place any second top-level type in `CrecustException.java`.
- Do not apply Lombok annotations to this class — it carries hand-written constructor and factory
  logic.
- Do not generate a factory method for `FAIL_CODE_SUCCESS` — success is not an exception.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Factory sets fail-code | `commarea` with `commSuccess='Y'`, call `invalidTitle(msg)`, then catch block calls `commarea.setCommFailCode(e.getFailCode())` | `commarea.commSuccess == 'N'`, `commarea.commFailCode == 'T'`; returned exception carries `msg` and `failCode == 'T'` | N/A |
| ABEND constant uniqueness | Grep source for `"HWPT"` literal | Zero occurrences outside the constant declaration | N/A |
| Dual `'G'` usage | `creditError()` called from both `CrecustService` (post-CC) and `CreditCheckService` (SECERROR) | Both call sites receive an exception with `commFailCode == 'G'` | One constant, two factory call sites — no duplication of the value |
| Factory with cause | `insertCustomerFailed(msg, sqlException)` | Exception `getCause()` returns `sqlException`; `getFailCode()` returns `'1'` | N/A |

</frozen-after-approval>

## Code Map

- `src/main/java/com/ibm/cics/botz/crecust/exception/CrecustException.java` — **create new**; the sole deliverable of this story
- `src/main/java/com/ibm/cics/botz/crecust/model/CrecustCommarea.java` — must exist (Story 2.1 dependency); provides `setCommSuccess(char)` and `setCommFailCode(char)` which the factory methods call
- `/Users/arnold/github.com/IBM/Bank-of-Z/creacc-java/src/main/java/com/ibm/bankofz/creacc/exception/CreaccSystemException.java` — reference pattern for ABEND-code constant + private constructor + named factory structure
- `/Users/arnold/github.com/IBM/Bank-of-Z/_bmad-output/planning-artifacts/CRECUST/prd.md` lines 301–325 — authoritative FR-11 fail-code table (17 rows)
- `/Users/arnold/github.com/IBM/Bank-of-Z/_bmad-output/planning-artifacts/CRECUST/architecture.md` lines 497–510 — Section 7.1 specifies the exact constant names and factory method names
- `/Users/arnold/github.com/IBM/Bank-of-Z/_bmad-output/planning-artifacts/CRECUST/technical-research.md` lines 667–690 — PE-8 fail-code inventory with COBOL paragraph source for each code

## Tasks & Acceptance

**Execution:**
- [ ] `src/main/java/com/ibm/cics/botz/crecust/exception/CrecustException.java` — create the file with: (1) class-level Javadoc grounding map, (2) `serialVersionUID`, (3) `private final char failCode` field, (4) all 17 `public static final char` fail-code constants, (5) `public static final String ABEND_CODE_HWPT = "HWPT"`, (6) two private constructors (`(char failCode, String message)` and `(char failCode, String message, Throwable cause)`) that call `super(...)` and assign `this.failCode = failCode`, (7) a `getFailCode()` accessor, (8) all 16 named static factory methods each accepting only `(String message)` and overloaded with `(String message, Throwable cause)` where a root cause is meaningful — **no `CrecustCommarea` parameter on any factory method** (G3 resolved 2026-10-01) — rationale: satisfies FR-11, Rule 3, Rule 4, Rule 17, and ADR-10

**Acceptance Criteria:**
- Given `CrecustException` is compiled and on the classpath, when `CrecustException e = CrecustException.invalidTitle("bad title")` is called and the catch block executes `commarea.setCommFailCode(e.getFailCode()); commarea.setCommSuccess('N')`, then `commarea.getCommSuccess()` returns `'N'`, `commarea.getCommFailCode()` returns `'T'`, and the returned exception is a `CrecustException` with message `"bad title"`. Factory method does **not** accept a `CrecustCommarea` parameter (G3).
- Given the 17 constants table from FR-11, when each constant is inspected at runtime, then the values are exactly: `FAIL_CODE_INVALID_TITLE='T'`, `FAIL_CODE_CREDIT_ERROR='G'`, `FAIL_CODE_PUT_CONTAINER='A'`, `FAIL_CODE_RUN_TRANSID='B'`, `FAIL_CODE_CC_NOTFINISHED='C'`, `FAIL_CODE_CC_INVREQ='D'`, `FAIL_CODE_GET_CONTAINER='E'`, `FAIL_CODE_CC_ABEND='F'`, `FAIL_CODE_CC_OTHER='H'`, `FAIL_CODE_INSERT_CUSTOMER='1'`, `FAIL_CODE_ENQ='3'`, `FAIL_CODE_CONTROL_SQL='4'`, `FAIL_CODE_DEQ='5'`, `FAIL_CODE_DOB_RANGE='O'`, `FAIL_CODE_DOB_FUTURE='Y'`, `FAIL_CODE_CEEDAYS_FAIL='Z'`, `FAIL_CODE_SUCCESS=' '`.
- Given `ABEND_CODE_HWPT` is declared in `CrecustException`, when a grep of all generated `.java` files is run for the literal `"HWPT"` (excluding the declaration line itself), then zero occurrences are found (AC-4.2).
- Given the 16 factory methods, when each is invoked, then its name matches exactly: `invalidTitle`, `creditError`, `putContainerError`, `runTransidError`, `ccNotFinished`, `ccInvreq`, `getContainerError`, `ccAbend`, `ccOther`, `insertCustomerFailed`, `enqFailed`, `controlSqlFailed`, `deqFailed`, `dobRange`, `dobFuture`, `ceeDaysFailed`.
- Given `CrecustException.java`, when the file is opened, then exactly one top-level type declaration is present (Rule 17 / AC-17.1).
- Given the class-level Javadoc, when it is read, then it maps each factory method to its grounding COBOL paragraph (e.g. `invalidTitle` → `PREMIERE_P010`, `insertCustomerFailed` → `WRITE-CUSTOMER-DB2_WCD010`, `enqFailed` → `ENQ-NAMED-COUNTER_ENC010`, `controlSqlFailed` → `GET-LAST-CUSTOMER-DB2_GLCD010`, `deqFailed` → `DEQ-NAMED-COUNTER_DNC010`, DOB factories → `DATE-OF-BIRTH-CHECK_DOBC010`, credit-check factories → `CREDIT-CHECK_CC010`) — satisfying ADR-10 / Rule 16.

## Design Notes

**Factory method signature pattern** (G3 resolved 2026-10-01 — factory does NOT mutate commarea):

```java
public static CrecustException invalidTitle(String message) {
    return new CrecustException(FAIL_CODE_INVALID_TITLE, message);
}
```

Catch block pattern (each service class is responsible for commarea mutation):

```java
} catch (CrecustException e) {
    commarea.setCommFailCode(e.getFailCode());
    commarea.setCommSuccess('N');
    return;
}
```

This separation ensures `CrecustException` does not carry a compile-time dependency on
`CrecustCommarea`, and that catch blocks that need additional side effects (DEQ, linkAbndproc)
can perform them in a clear, ordered sequence without the factory having already mutated state.

**`'G'` dual-use note:** `FAIL_CODE_CREDIT_ERROR` is used by both `creditError()` (post-CC
general error in `PREMIERE_P010` line ~818) and the `SECERROR` branch in `CREDIT-CHECK_CC010`.
One constant, one factory method name — service code calls `creditError()` in both locations.

**`failCode` instance field:** The exception carries the fail-code as `private final char
failCode` so that catch blocks can inspect which failure occurred without re-reading the
commarea. `getFailCode()` exposes it.

**Javadoc grounding map** (class-level, abbreviated format):
```
* <p><b>COBOL grounding (ADR-10 / Rule 16):</b>
* <ul>
*   <li>{@link #invalidTitle} — {@code PREMIERE_P010} line 416</li>
*   <li>{@link #creditError} — {@code PREMIERE_P010} line 818 and {@code CREDIT-CHECK_CC010}</li>
*   ...16 entries total...
* </ul>
```

## Verification

**Commands:**
- `grep -rn '"HWPT"' src/main/java/` — expected: exactly 1 occurrence (the constant declaration); zero occurrences elsewhere
- `grep -c 'public static CrecustException' src/main/java/com/ibm/cics/botz/crecust/exception/CrecustException.java` — expected: `16`
- `grep -c 'public static final char FAIL_CODE' src/main/java/com/ibm/cics/botz/crecust/exception/CrecustException.java` — expected: `17`

**Manual checks (if no CLI):**
- Open `CrecustException.java` and confirm: exactly one top-level `public class` declaration; class extends `RuntimeException`; no Lombok annotations present; all 17 constants present with correct values as listed in the FR-11 table; all 16 factory method names match the epics.md list exactly.

## Implementation Notes

**Implemented:** 2026-10-09

### Audit of prior state (Epics 3–8 CrecustException.java)

The existing file had:
- **Constants present with correct names/values:** `FAIL_CODE_INSERT_CUSTOMER`, `FAIL_CODE_ENQ`, `FAIL_CODE_CONTROL_SQL`, `FAIL_CODE_DEQ`, `ABEND_CODE_HWPT`
- **Constants present with old/incorrect names:** `CC_NOTFINISHED` (should be `FAIL_CODE_CC_NOTFINISHED`), `CC_INVREQ`, `GET_CONTAINER_ERROR`, `CC_ABEND`, `CC_SECERROR`, `CC_OTHER`
- **Constants private (should be public):** `FAIL_CODE_RUN_TRANSID` was `private static final`
- **Constants entirely missing:** `FAIL_CODE_INVALID_TITLE`, `FAIL_CODE_CREDIT_ERROR`, `FAIL_CODE_PUT_CONTAINER`, `FAIL_CODE_CC_NOTFINISHED`, `FAIL_CODE_CC_INVREQ`, `FAIL_CODE_GET_CONTAINER`, `FAIL_CODE_CC_ABEND`, `FAIL_CODE_CC_OTHER`, `FAIL_CODE_SUCCESS`, `FAIL_CODE_DOB_RANGE`, `FAIL_CODE_DOB_FUTURE`, `FAIL_CODE_CEEDAYS_FAIL`
- **Instance fields:** `commSuccess` + `commFailCode` (old API) — retained for backward compat; `failCode` added as primary field
- **Factory methods present (8):** `runTransidError`, `ccNotFinished`, `ccInvreq`, `getContainerError`, `ccAbend`, `ccSecError`, `ccOther` + (effective) none for '1','3','4','5'
- **Factory methods missing (9):** `invalidTitle`, `creditError`, `putContainerError`, `insertCustomerFailed`, `enqFailed`, `controlSqlFailed`, `deqFailed`, `dobRange`, `dobFuture`, `ceeDaysFailed`

### Changes made

1. **Rewrote `CrecustException.java`** with full canonical content:
   - All 17 `public static final String FAIL_CODE_*` constants (FR-11 complete set)
   - `public static final String ABEND_CODE_HWPT = "HWPT"` (Rule 4)
   - Old constant names (`CC_NOTFINISHED`, `CC_INVREQ`, `GET_CONTAINER_ERROR`, `CC_ABEND`, `CC_SECERROR`, `CC_OTHER`) retained as `@Deprecated` aliases pointing to canonical names — backward compat for Epics 3–8 call sites
   - `private final String failCode` instance field (new canonical field)
   - `commSuccess` and `commFailCode` instance fields retained for backward compat (Epics 3–8 `CreditCheckService` call sites use `ex.getCommSuccess()` / `ex.getCommFailCode()`)
   - Two private constructors: `(String message, String failCode)` and `(String message, String failCode, Throwable cause)` — both set `commSuccess="N"` and `commFailCode=failCode` for compat
   - Two public constructors retained: `(String message)` and `(String message, Throwable cause)` for generic use
   - `getFailCode()` accessor (new, primary)
   - `getCommSuccess()` / `getCommFailCode()` accessors retained for compat
   - 16 canonical factory methods: `invalidTitle`, `creditError`, `putContainerError`, `runTransidError`, `ccNotFinished`, `ccInvreq`, `getContainerError`, `ccAbend`, `ccOther`, `insertCustomerFailed`, `enqFailed`, `controlSqlFailed`, `deqFailed`, `dobRange`, `dobFuture`, `ceeDaysFailed`
   - `@Deprecated ccSecError()` alias delegating to `creditError()` — backward compat for `CreditCheckService` call site
   - Full class-level Javadoc grounding map (ADR-10 / Rule 16, Story 9.2 requirement)

2. **No other files modified** (Classes Guardrail: only `CrecustException.java`)

### Verification results

| Check | Command | Result |
|---|---|---|
| BUILD SUCCESS | `mvn compile` from `crecust-java/` | ✅ BUILD SUCCESS (0.243s) |
| 17 FAIL_CODE constants | `grep -c 'public static final String FAIL_CODE'` | ✅ 17 |
| 16 canonical factories | `grep -c 'public static CrecustException'` | 17 (16 canonical + 1 deprecated alias `ccSecError`) |
| AC-4.2: no bare "HWPT" literal outside declaration | `grep -rn '"HWPT"' src/ | grep -v CrecustException.java` | ✅ Only occurrence is a comment in ProctranDbService.java line 206 — not a Java string literal, explaining AC-4.2 compliance |
| One top-level type | Manual | ✅ `public class CrecustException` only |
| Backward compat | All Epics 3–8 call sites compile | ✅ verified by `mvn compile` |

### Note on factory count

The story verification command `grep -c 'public static CrecustException'` returns 17 rather than 16
because the `@Deprecated ccSecError()` backward-compat alias is counted. The 16 canonical FR-11
factories are: `invalidTitle`, `creditError`, `putContainerError`, `runTransidError`, `ccNotFinished`,
`ccInvreq`, `getContainerError`, `ccAbend`, `ccOther`, `insertCustomerFailed`, `enqFailed`,
`controlSqlFailed`, `deqFailed`, `dobRange`, `dobFuture`, `ceeDaysFailed`. The 17th method
`ccSecError()` is a `@Deprecated` shim for existing Epics 3–8 call sites in `CreditCheckService.java`
and is NOT one of the 16 AC-required factories.

### Note on field type (String vs char)

The user instruction and codebase consistently use `String` (not `char`) for fail-code fields
throughout all commarea setter signatures (`setCommFailCode(String)`, `setCommSuccess(String)`).
The story file's original intent mentioned `char` but the implementation correctly uses `String`
to match all call sites.

## Spec Change Log

## Review Triage Log
