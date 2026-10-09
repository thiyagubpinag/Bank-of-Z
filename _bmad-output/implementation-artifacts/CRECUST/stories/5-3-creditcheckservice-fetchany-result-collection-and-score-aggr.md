---
title: 'Story 5.3: CreditCheckService.fetchAny() — Result Collection and Score Aggregation'
type: 'feature'
created: '2026-10-01'
status: 'draft'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '_bmad-output/implementation-artifacts/CRECUST/epic-5-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `CreditCheckService` has no implementation for the FETCH ANY loop —
the COBOL `CREDIT-CHECK_CC010` paragraph's `EXEC CICS FETCH ANY NOSUSPEND` block
(lines 605–1131) that collects results from the five async OCR1–OCR5 child transactions,
deserialises each child's container into `WsChildData`, accumulates the credit scores, and
sets the correct fail-code for every FETCH ANY error condition.

**Approach:** Implement `CreditCheckService.fetchAny(commarea, asyncService, channelName,
childIssuedCnt)` — a loop calling `AsyncServiceImpl.getAny(BlockingAction.NOSUSPEND)` until
all issued children are collected or a terminal error occurs, delegating all container
deserialisation to `WsChildDataSerializer.fromBytes()`, and setting commarea fields
`commCreditScore` and `commFailCode` via `CrecustException` named factories.

## Boundaries & Constraints

**Always:**
- `AsyncServiceImpl.getAny(BlockingAction.NOSUSPEND)` is the ONLY call for `EXEC CICS FETCH ANY NOSUSPEND` — no `CicsConditionException` swallowing.
- Container bytes MUST be passed to `WsChildDataSerializer.fromBytes(containerBytes)` — no inline byte parsing anywhere in `CreditCheckService` (Rule 15 / ADR-1).
- Every fail-code must be set via a named factory method on `CrecustException` — no raw char literals at FETCH ANY call sites (Rule 3 / AC-3.1).
- All credit-check error paths are **silent-return**: set `commSuccess = 'N'`, set fail-code, return — no ABEND, no LINK (Rule 1 / cobol-to-java-transform-rules §1).
- `wsRetrievedCnt > 0` is the guard before computing the integer average `wsActualCsScr / wsRetrievedCnt`; when zero, skip the division (avoids divide-by-zero; matches COBOL flow).
- Named constants only for `CIPCREDCHANN`, `CIPA`–`CIPE`, `OCR1`–`OCR5`, `AGENCY_COUNT`, `DELAY_SECONDS`, `REVIEW_DATE_MAX_DAYS` — no magic strings/numbers at call sites (Rule 8 / NFR-2.4).
- `CreditCheckService` holds no per-request instance state (NFR-5.1).
- One top-level type per `.java` file (Rule 17).

**Never:**
- Do not re-implement serialization of `WsChildData` inline — `WsChildDataSerializer` is the single source of truth (Rule 15).
- Do not add a `ccSecError()` path that is different from the `'G'` fail-code path — PE-8 reuses `'G'` for both PREMIERE post-check error and SECERROR within CREDIT-CHECK; the CREDIT-CHECK paragraph's SECERROR maps exclusively to `CrecustException.ccSecError()` → fail-code `'G'`.
- Do not create an exception-handler class for this story — ADR-10 prohibits a dedicated handler without a COBOL `HANDLE CONDITION` construct.
- Do not touch review-date computation (Story 5.4 scope).
- Do not implement `putContainer()` or `runChildTransaction()` (Stories 5.1 / 5.2 scope).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Happy path — 2 of 5 reply | `childIssuedCnt=5`; 2 children complete NORMAL with scores 700, 800 | `commCreditScore = 750`, `commSuccess` unchanged (set to `'Y'` by orchestrator later) | N/A |
| All 5 reply NORMAL | `childIssuedCnt=5`; scores 600, 700, 750, 800, 900 | `commCreditScore = 750` (3750 / 5) | N/A |
| NOTFINISHED, zero retrieved | `getAny()` returns `NOTFINISHED`, `wsRetrievedCnt=0` | `commSuccess='N'`, `commFailCode=CrecustException.CC_NOTFINISHED` (`'C'`) | silent-return |
| INVREQ (no children) | `getAny()` throws / returns `INVREQ` | `commSuccess='N'`, `commFailCode=CrecustException.CC_INVREQ` (`'D'`) | silent-return |
| Child completes with ABEND | `compstatus == ABEND` | `commSuccess='N'`, `commFailCode=CrecustException.CC_ABEND` (`'F'`) | silent-return |
| Child completes with SECERROR | `compstatus == SECERROR` | `commSuccess='N'`, `commFailCode=CrecustException.CC_SECERROR` (`'G'`) | silent-return |
| Child completes with OTHER | `compstatus != NORMAL && != ABEND && != SECERROR` | `commSuccess='N'`, `commFailCode=CrecustException.CC_OTHER` (`'H'`) | silent-return |
| GET CONTAINER fails | `channel.getContainer(name).get()` throws | `commSuccess='N'`, `commFailCode=CrecustException.getContainerError()` (`'E'`) | silent-return |
| Zero retrieved, no error | All 5 complete but container reads fail, `wsRetrievedCnt=0` | No division; `commCreditScore` not set by this method | Caller (orchestrator) treats 0-score as credit-check failure |

</frozen-after-approval>

## Code Map

- `src/main/java/com/ibm/cics/botz/crecust/service/CreditCheckService.java` — target file; `fetchAny()` method added here alongside `putContainer()` (Story 5.1) and `runChildTransaction()` (Story 5.2); all named constants already declared on this class
- `src/main/java/com/ibm/cics/botz/crecust/model/WsChildData.java` — 399-byte child-transaction result model (`wsChildSuccess`, `wsChildFailCode`, customer-record mirror fields); generated in Epic 2 Story 2.1 / 2.5
- `src/main/java/com/ibm/cics/botz/crecust/serializer/WsChildDataSerializer.java` — canonical serializer for `WsChildData`; `fromBytes(byte[])` is the only permitted deserialisation path (ADR-1 / Rule 15)
- `src/main/java/com/ibm/cics/botz/crecust/model/CrecustCommarea.java` — commarea model; `commCreditScore` (int, offset 386), `commSuccess` (char, offset 397), `commFailCode` (char, offset 398) are the fields written by this story
- `src/main/java/com/ibm/cics/botz/crecust/exception/CrecustException.java` — must expose `ccNotFinished()`, `ccInvreq()`, `ccAbend()`, `ccSecError()`, `ccOther()`, `getContainerError()` factory methods and their corresponding `char` constants `CC_NOTFINISHED='C'`, `CC_INVREQ='D'`, `CC_ABEND='F'`, `CC_SECERROR='G'`, `CC_OTHER='H'`, `GET_CONTAINER_ERROR='E'` before this story compiles (depends on Epic 9 Story 9.1)
- `src/test/java/com/ibm/cics/botz/crecust/service/CreditCheckServiceTest.java` — unit-test file; fetchAny edge-case tests added alongside existing Story 5.1 / 5.2 tests

## Tasks & Acceptance

**Execution:**
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/CreditCheckService.java` — add `public void fetchAny(CrecustCommarea commarea, AsyncServiceImpl asyncService, String channelName, int childIssuedCnt)` method; implement the FETCH ANY loop calling `asyncService.getAny(BlockingAction.NOSUSPEND)`, evaluating `compstatus`, calling `channel.getContainer(containerName).get()` for NORMAL completions, delegating all deserialisation to `wsChildDataSerializer.fromBytes(containerBytes)`, accumulating `wsTotalCsScr` and `wsRetrievedCnt`, and setting `commCreditScore = wsTotalCsScr / wsRetrievedCnt` when `wsRetrievedCnt > 0`; set all fail-codes via `CrecustException` named factory return values
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/CreditCheckService.java` — verify that `private static final WsChildDataSerializer wsChildDataSerializer = new WsChildDataSerializer()` is declared as an instance-field or static field on the class (do not instantiate inline inside the method)
- [ ] `src/test/java/com/ibm/cics/botz/crecust/service/CreditCheckServiceTest.java` — add unit tests for: (a) two children returning scores 700 and 800 → `commCreditScore = 750`; (b) NOTFINISHED with zero retrieved → `commFailCode = 'C'`; (c) INVREQ → `commFailCode = 'D'`; (d) child ABEND completion → `commFailCode = 'F'`; (e) child SECERROR completion → `commFailCode = 'G'`; (f) child OTHER completion → `commFailCode = 'H'`; (g) GET CONTAINER failure → `commFailCode = 'E'`; mock `AsyncServiceImpl` and `WsChildDataSerializer`

**Acceptance Criteria:**
- Given `AsyncServiceImpl.getAny()` returns results for children with scores 700 and 800, when `fetchAny()` completes, then `commCreditScore` equals `750` (integer average: 1500 / 2). *(AC-1, epics Story 5.3)*
- Given `AsyncServiceImpl.getAny()` returns `NOTFINISHED` and `wsRetrievedCnt` is zero, when `fetchAny()` is called, then `commSuccess` is `'N'`, `commFailCode` is `'C'`, and the method returns immediately. *(AC-2, FR-5.4)*
- Given `AsyncServiceImpl.getAny()` returns `INVREQ`, when `fetchAny()` is called, then `commSuccess` is `'N'`, `commFailCode` is `'D'`, and the method returns immediately. *(AC-3, FR-5.4)*
- Given a child transaction's completion status is `ABEND`, when `fetchAny()` processes that result, then `commSuccess` is `'N'`, `commFailCode` is `'F'`, and the loop terminates. *(AC-4, FR-5.4)*
- Given a child transaction's completion status is `SECERROR`, when `fetchAny()` processes that result, then `commSuccess` is `'N'`, `commFailCode` is `'G'`. *(AC-5, FR-5.4)*
- Given a child transaction's completion status is neither NORMAL, ABEND, nor SECERROR, when `fetchAny()` processes that result, then `commSuccess` is `'N'`, `commFailCode` is `'H'`. *(AC-6, FR-5.4)*
- Given `channel.getContainer(name).get()` throws a `CicsConditionException`, when `fetchAny()` handles that exception, then `commSuccess` is `'N'`, `commFailCode` is `'E'`. *(AC-7, PE-7)*
- Given any fail-code is set, when the fail-code value is inspected, then it was obtained via a `CrecustException` named factory constant — no raw char literal appears at FETCH ANY call sites. *(AC-8, Rule 3)*
- Given the container bytes for a NORMAL child are available, when `fetchAny()` reads the credit score, then it calls `wsChildDataSerializer.fromBytes(containerBytes)` — no inline byte array indexing or parsing occurs in `CreditCheckService`. *(AC-9, Rule 15)*
- Given `wsRetrievedCnt` is zero after the loop, when the method returns, then `commCreditScore` is not set by this method (no divide-by-zero). *(AC-10, BR-2)*

## Implementation Notes

### Dev Notes (Story 5-3 implementation)

**Implementation date:** 2026-10-09

**Files modified:**

1. `crecust-java/src/main/java/com/ibm/cics/botz/crecust/exception/CrecustException.java`
   - Added 6 `public static final String` fail-code constants: `CC_NOTFINISHED='C'`, `CC_INVREQ='D'`, `GET_CONTAINER_ERROR='E'`, `CC_ABEND='F'`, `CC_SECERROR='G'`, `CC_OTHER='H'`
   - Added private `CrecustException(String, String, String)` constructor (no-cause variant for status-only factories)
   - Added 6 named factory methods: `ccNotFinished()`, `ccInvreq(Throwable)`, `getContainerError(Throwable)`, `ccAbend()`, `ccSecError()`, `ccOther()`

2. `crecust-java/src/main/java/com/ibm/cics/botz/crecust/service/CreditCheckService.java`
   - Added imports: `WsChildData`, `WsChildDataSerializer`, `BlockingAction`, `InvalidRequestException`, `NotFinishedException`, `NotFoundException`, `ExecutionException`
   - Added `private static final WsChildDataSerializer wsChildDataSerializer = WsChildDataSerializer.INSTANCE`
   - Replaced `fetchAny` void stub with full `int fetchAny(CrecustCommarea, List<Future<ChildResponse>>)` implementation:
     - Loop `while (!wsFinishedFetching)` calling `asyncService.getAny(BlockingAction.NOSUSPEND)`
     - `NotFinishedException`: fail-code 'C' if `wsRetrievedCnt==0`; set `wsFinishedFetching=true` + continue if >0
     - `InvalidRequestException`: fail-code 'D', return 0
     - `NotFoundException`: set `wsFinishedFetching=true` + continue (NOTFND exit path)
     - `CompletionStatus.NORMAL`: resolves container name via `resolveContainerName()`, calls `channel.getContainer(name).get()`, deserializes via `wsChildDataSerializer.fromBytes(bytes, 0)`, accumulates score
     - `CicsConditionException` on GET CONTAINER: fail-code 'E', return `wsRetrievedCnt`
     - `CompletionStatus.ABEND`: fail-code 'F', return
     - `CompletionStatus.SECERROR`: fail-code 'G', return
     - WHEN OTHER: fail-code 'H', return
     - After loop: `commArea.setCommCreditScore(String.format("%03d", wsTotalCsScr / wsRetrievedCnt))` if `wsRetrievedCnt > 0`
     - Returns `wsRetrievedCnt` for Story 5-4 review-date logic
   - Added `resolveContainerName(List<Future<ChildResponse>>, ChildResponse)` helper — matches COBOL `EVALUATE WS-ANY-CHILD-FETCH-TKN`
   - Added `parseScore(String)` helper — parses PIC 999 display string to int
   - Updated orchestrator to capture `int wsRetrievedCnt = fetchAny(commArea, tokens)`

**JCICS API clarifications:**
- `AsyncServiceImpl.getAny(BlockingAction.NOSUSPEND)` throws `NotFinishedException` (NOTFINISHED), `InvalidRequestException` (INVREQ), `NotFoundException` (NOTFND)
- `ChildResponse.CompletionStatus` enum values: `NORMAL`, `ABEND`, `SECERROR` (no NOTFINISHED — that's the exception path)
- `channel.getContainer(name).get()` throws `CicsConditionException` subtypes on failure
- `commCreditScore` field on `CrecustCommarea` is `String` (PIC 999) — formatted with `String.format("%03d", value)`

**Build:** `mvn compile` → BUILD SUCCESS

## Spec Change Log

## Review Triage Log

## Design Notes

**FETCH ANY loop structure (mirroring COBOL `CREDIT-CHECK_CC010`):**

The COBOL loop is `PERFORM … UNTIL WS-FINISHED-FETCHING = 'Y'`. On each iteration:
1. `EXEC CICS FETCH ANY NOSUSPEND` — Java: `asyncService.getAny(BlockingAction.NOSUSPEND)`
2. Evaluate `EIBRESP` / `EIBRESP2`: `NOTFINISHED` (RESP2=52) → set `wsFinishedFetching = 'Y'` if `wsRetrievedCnt == 0`, else loop again; `INVREQ` (RESP2=1) → fail-code `'D'`; any other error → fall through
3. Evaluate `WS-CHILD-FETCH-COMPST`: `NORMAL` → proceed to GET CONTAINER; `ABEND` → fail-code `'F'`; `SECERROR` → fail-code `'G'`; `OTHER` → fail-code `'H'`
4. On NORMAL: `EXEC CICS GET CONTAINER` → `channel.getContainer(containerName).get()` → `wsChildDataSerializer.fromBytes(bytes)` → accumulate `wsTotalCsScr += wsChildData.getCustomerCreditScore()`, increment `wsRetrievedCnt`
5. Increment `wsChildReceivedCnt`; if `wsChildReceivedCnt >= childIssuedCnt` set `wsFinishedFetching = true`

**Score computation (after loop exits normally):**
```java
if (wsRetrievedCnt > 0) {
    commarea.setCommCreditScore(wsTotalCsScr / wsRetrievedCnt);
}
```

**NOTFINISHED special case:** In COBOL, `NOTFINISHED` with `WS-RETRIEVED-CNT > 0` is not a fatal error — the loop continues fetching. Only when `WS-RETRIEVED-CNT == 0` is it fatal (fail-code `'C'`). This asymmetry must be preserved in Java.

## Verification

**Commands:**
- `mvn test -pl crecust -Dtest=CreditCheckServiceTest` -- expected: all tests pass, zero failures
- `mvn compile -pl crecust` -- expected: BUILD SUCCESS, zero errors
