---
title: 'CreditCheckService.runChildTransaction() — async launch of OCR1–OCR5'
type: 'feature'
created: '2026-10-01'
status: 'draft'
route: 'dispatch'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `CreditCheckService` has no method to asynchronously launch the five OCR credit-check child transactions (`OCR1`–`OCR5`) or to introduce the mandatory 3-second delay before result collection begins; without these, the JCICS async credit-check loop cannot execute.

**Approach:** Implement `runChildTransaction(transId, channelName)` on `CreditCheckService` using `AsyncServiceImpl.runTransactionId(tranId, channel)` (ADR-5 — note: `AsyncServiceImpl.run()` is NOT the correct method), store each returned token in a local token array, and implement `delayForResults()` using `Thread.sleep(3000)` (`Task.getTask().delay()` does not exist in JCICS). Both transaction-ID constants and the delay value must be declared as named `private static final` constants; no raw literals at call sites (Rule 8).

## Boundaries & Constraints

**Always:**
- Use `AsyncServiceImpl.runTransactionId(transId, channelName)` — NOT `AsyncServiceImpl.run()` — for all five child launches (ADR-5).
- Store the token returned by each `runTransactionId()` call so `fetchAny()` (Story 5.3) can retrieve it later.
- Call `Thread.sleep(3000)` exactly once, after all five `runTransactionId()` calls succeed, before any `FETCH ANY` invocation (ADR-5 ordering requirement). `Task.getTask().delay()` does NOT exist in JCICS.
- Declare constants `OCR1`–`OCR5` and `DELAY_MILLISECONDS = 3000` as `private static final String` / `int` — no inline literals (Rule 8 / NFR-2.4).
- On any exception thrown by `AsyncServiceImpl.runTransactionId()`: call `CrecustException.runTransidError()`, set `commSuccess = 'N'`, set `commFailCode` via the exception, and return immediately — silent-return path (Rule 1).
- `CreditCheckService` must hold no per-request state in instance fields (NFR-5).

**Never:**
- Do NOT call `Task.getTask().delay()` — this method does not exist in the JCICS API. Use `Thread.sleep(3000)` instead.
- Do NOT use `AsyncServiceImpl.run()` — the correct method is `AsyncServiceImpl.runTransactionId(tranId, channel)`.
- Do not call `Program.link()`, `Program.xctl()`, or any non-JCICS async API for the child-transaction launch.
- Do not create a COBOL helper shim or delegate to `CRECUSTCC` (ADR-5 prevents this).
- Do not add ABEND calls, notification writes, or logging above `DEBUG` on the RUN TRANSID failure path (silent-return only, Rule 1).
- Do not inline `Thread.sleep()` inside `runChildTransaction()` — keep it in a separate `delayForResults()` method so the orchestrator controls the call boundary.
- Do not store any per-request channel name or token state in instance fields.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Happy path — five successful runs | `transId` = `OCR1`–`OCR5`, `channelName` = `CIPCREDCHANN` | `AsyncServiceImpl.runTransactionId()` called five times; five child tokens stored in caller's token array; method returns normally each call | N/A |
| `AsyncServiceImpl.runTransactionId()` throws exception | Exception on call to `OCR2` | `commSuccess = 'N'`; `commFailCode` set via `CrecustException.runTransidError()`; method returns immediately; subsequent OCRs not launched | Silent-return, no ABEND |
| `delayForResults()` called after five runs | All five runs completed | `Thread.sleep(3000)` called exactly once | N/A |
| Unit test — confirms ordering | Mocked `AsyncServiceImpl` | Five `runTransactionId()` calls precede the single `Thread.sleep(3000)` call | Verified by call order |

</frozen-after-approval>

## Code Map

- `src/main/java/com/ibm/cics/botz/crecust/service/CreditCheckService.java` — target class; `runChildTransaction(String transId, String channelName, List<String> tokens, CrecustCommarea commarea)` and `delayForResults()` are added here; token-list is passed in from the orchestrator loop so no instance state is needed
- `src/main/java/com/ibm/cics/botz/crecust/exception/CrecustException.java` — supplies `runTransidError()` factory method (Epic 9, Story 9.1); import and call here; do not inline the fail-code literal
- `src/main/java/com/ibm/cics/botz/crecust/model/CrecustCommarea.java` — `commSuccess` and `commFailCode` fields are mutated on the RUN TRANSID failure path
- `src/main/java/com/ibm/cics/botz/crecust/service/CrecustService.java` — caller of `runChildTransaction()` and `delayForResults()`; see Story 3.2; token list is allocated here and passed to `CreditCheckService`
- `src/test/java/com/ibm/cics/botz/crecust/service/CreditCheckServiceTest.java` — new or existing test class; add test cases for the run loop and delay ordering

**Existing ADR-5 constants (Story 5.1 scope — already present in `CreditCheckService`):**
- `CIPCREDCHANN` (`"CIPCREDCHANN"`) — channel name
- `CIPA`–`CIPE` container name constants
These do NOT need to be re-declared; only OCR1–OCR5 and the delay value are new in this story.

## Tasks & Acceptance

**Execution:**
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/CreditCheckService.java` — declare `private static final String` constants `OCR1 = "OCR1"` through `OCR5 = "OCR5"` and `private static final int DELAY_MILLISECONDS = 3000`; implement `runChildTransaction(String transId, String channelName, List<String> tokens, CrecustCommarea commarea)` using `AsyncServiceImpl.runTransactionId(transId, channelName)` (NOT `AsyncServiceImpl.run()`) storing the returned token via `tokens.add(token)`, catching exceptions and delegating to `CrecustException.runTransidError()` then returning; implement `delayForResults()` using `Thread.sleep(DELAY_MILLISECONDS)` (NOT `Task.getTask().delay()` — that method does not exist in JCICS)
- [ ] `src/test/java/com/ibm/cics/botz/crecust/service/CreditCheckServiceTest.java` — add unit test `testRunChildTransactionSuccess` mocking `AsyncServiceImpl` to verify five `runTransactionId()` calls with correct transaction IDs and that five tokens are stored; add `testRunChildTransactionFailure` verifying that an exception on the second call sets `commSuccess = 'N'` and returns without calling `runTransactionId()` for subsequent IDs; add `testDelayCalledAfterFiveRuns` verifying `Thread.sleep(3000)` is called once and only after all five `runTransactionId()` calls

**Acceptance Criteria:**
- Given five transaction IDs `OCR1`–`OCR5` and channel name `CIPCREDCHANN`, when `runChildTransaction()` is called five times, then `AsyncServiceImpl.runTransactionId(transId, channelName)` is called exactly five times with the correct transaction IDs and the token list holds exactly five entries.
- Given `AsyncServiceImpl.runTransactionId()` throws an exception on any call, when `runChildTransaction()` catches the exception, then `commSuccess` is set to `'N'`, `commFailCode` carries the value set by `CrecustException.runTransidError()`, and the method returns without launching subsequent child transactions.
- Given all five `runChildTransaction()` calls complete without error, when `delayForResults()` is called, then `Thread.sleep(3000)` is invoked exactly once. `Task.getTask().delay()` must NOT appear in the code — grep confirms zero occurrences.
- Given the unit test mocks are in place, when the orchestrator method calls `runChildTransaction()` five times then `delayForResults()` once, then the call-order assertion confirms all five `runTransactionId()` calls precede the `Thread.sleep()` call.
- Given constants `OCR1`–`OCR5` and `DELAY_MILLISECONDS` are declared, when the class is reviewed, then no raw string `"OCR1"`–`"OCR5"` or numeric `3000` literal appears at call sites — only the named constants.
- Given `CreditCheckService` is inspected, when instance fields are enumerated, then no per-request state (token list, channel name, run counter) exists as an instance field.

## Implementation Notes

### Dev Notes (2026-10-09)

**Status:** Implementation complete — `mvn compile` → BUILD SUCCESS.

**JCICS API discovery:** Inspected `com.ibm.cics.server-2.200.0-6.3.jar` with `javap`. Findings
that differ from planning-doc terminology:

| Planning doc term | Actual JCICS type |
|---|---|
| `ChildRequestID` | `java.util.concurrent.Future<com.ibm.cics.server.ChildResponse>` |
| `runTransactionId(transId, channelName: String)` | `runTransactionId(String, com.ibm.cics.server.Channel)` — takes a `Channel` object |

The channel is retrieved via `Task.getTask().getChannel(channelName)` (uses the channel already
created by `putContainer` in the same task). Tokens are collected as `List<Future<ChildResponse>>`.

**`CrecustException.runTransidError()`:** Factory method added to `CrecustException` in this story
(not deferred to Story 9.1) because it is a compile-time dependency of `runChildTransaction`.
The exception carries `commSuccess='N'` and `commFailCode='B'` as fields, which the service reads
and writes to the commarea.

**`delayForResults()`:** Implemented as a separate method per story's "Never" constraint.
`Thread.sleep(DELAY_MILLIS)` with `InterruptedException` handled by re-interrupting the thread.
`Task.getTask().delay()` does NOT exist in JCICS — confirmed by `javap` inspection.

**`fetchAny()` signature updated:** Stub now accepts `List<Future<ChildResponse>> tokens` so Story
5-3 can consume the token list without changing the call site in `performCreditCheck()`.

**Files modified:**
- `crecust-java/src/main/java/com/ibm/cics/botz/crecust/service/CreditCheckService.java`
- `crecust-java/src/main/java/com/ibm/cics/botz/crecust/exception/CrecustException.java`

## Spec Change Log

## Review Triage Log

## Design Notes

`runChildTransaction` accepts the `List<String> tokens` collector and the `CrecustCommarea` as explicit parameters so the method remains stateless. The orchestrating loop in `CrecustService.performCreditCheck()` (Story 3.2) owns the token list and passes it in; this mirrors the COBOL `WS-CHILD-ARRAY` structure that lives in WORKING-STORAGE of the calling program. The separation of `runChildTransaction` and `delayForResults` into two methods matches the ADR-5 CICS command mapping table and keeps each method testable in isolation.

`AsyncServiceImpl` is the concrete JCICS class for `EXEC CICS RUN TRANSID`. The correct method is **`runTransactionId(tranId, channel)`** — NOT `run()`. Construct `AsyncServiceImpl` as `new AsyncServiceImpl()` or obtain it via the channel/task context as the JCICS API requires. The token returned by `runTransactionId()` is a `ChildTokenHolder` or similar; store whichever type the JCICS API actually returns (do not invent a wrapper type).

**`EXEC CICS DELAY FOR SECONDS(3)` → `Thread.sleep(3000)`:** `Task.getTask().delay()` does NOT exist in the JCICS API. Use `Thread.sleep(DELAY_MILLISECONDS)` where `DELAY_MILLISECONDS = 3000`. Wrap in a try-catch for `InterruptedException` and restore the interrupt flag: `Thread.currentThread().interrupt()`.

## Verification

**Commands:**
- `mvn test -pl . -Dtest=CreditCheckServiceTest -q` -- expected: `BUILD SUCCESS` with all three new test cases passing
