# Epic 5 Context: Asynchronous Credit-Check (JCICS AsyncService Loop)

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Author `CreditCheckService` implementing the full JCICS async credit-check loop (ADR-5) —
channel/container creation, five async child-transaction launches, delay, FETCH ANY NOSUSPEND
result collection, score aggregation, and review-date computation. No COBOL helper shim is
generated (Q1 decision).

## Stories

- Story 5.1: `CreditCheckService.putContainer()` — CICS channel and container creation
- Story 5.2: `CreditCheckService.runChildTransaction()` — async launch of OCR1–OCR5
- Story 5.3: `CreditCheckService.fetchAny()` — result collection and score aggregation
- Story 5.4: `CreditCheckService` review-date computation (FR-6)

## Requirements & Constraints

**Source paragraph:** `CREDIT-CHECK_CC010` (lines 605–1131) — 526-line paragraph; highest
cyclomatic complexity in the program. Also references PREMIERE_P010 (lines 407–520).

**Key working-storage fields (Credit-Check section, lines 495–560):**

| COBOL Field | Java equivalent | Notes |
|---|---|---|
| `WS-CC-CNT` (PIC 9) | `wsCcCnt` int | Loop counter 1–5 |
| `WS-FINISHED-FETCHING` (PIC X) | `wsFinishedFetching` boolean / char | `'Y'` = done |
| `WS-RETRIEVED-CNT` (PIC 9) | `wsRetrievedCnt` int | Count of successful child responses |
| `WS-CHANNEL-NAME` (PIC X(16)) | constant `CIPCREDCHANN` | `'CIPCREDCHANN    '` |
| `WS-CREDIT-CHECK-ERROR` (PIC X) | `wsCreditCheckError` char | `'Y'` = error |
| `WS-ACTUAL-CS-SCR` (PIC 9(6)) | `wsActualCsScr` int | Average credit score accumulator |
| `WS-TOTAL-CS-SCR` (PIC 9(6)) | `wsTotalCsScr` int | Running total |
| `WS-ANY-CHILD-TKN` (PIC X(16)) | `wsAnyChildTkn` | Token from RUN TRANSID |
| `WS-ANY-CHILD-FETCH-TKN` (PIC X(16)) | `wsAnyChildFetchTkn` | Token from FETCH ANY |
| `WS-ANY-CHILD-FETCH-CHAN` (PIC X(16)) | `wsAnyChildFetchChan` | Channel from FETCH ANY |
| `WS-ANY-CHILD-FETCH-ABCODE` (PIC X(4)) | `wsAnyChildFetchAbcode` | ABCODE from FETCH ANY |
| `WS-CHILD-ISSUED-CNT` (PIC 9) | `wsChildIssuedCnt` int | Number of child transactions issued |
| `WS-CHILD-ARRAY` (OCCURS 9) | list of `WsChildChan`+`WsChildTkn` | channel + token per child |
| `WS-CHILD-RECEIVED-CNT` (PIC 9) | `wsChildReceivedCnt` int | |
| `WS-CHILD-FETCH-COMPST` (S9(8) COMP) | `wsChildFetchCompst` int | Completion status from FETCH ANY |
| `WS-RUN-TRANSID` (PIC X(4)) | built as `"OCR" + wsCcCnt` | Transaction IDs OCR1–OCR5 |
| `WS-PUT-CONT-NAME` (PIC X(16)) | constants `CIPA`–`CIPE` | Container names |
| `WS-PUT-CONT-LEN` (S9(8) COMP) | `LENGTH OF DFHCOMMAREA` = 399 | Commarea length |

**WS-CHILD-DATA / WsChildData (lines 523, total 399 bytes):**
- Mirrors `CUSTOMER-RECORD` (397 bytes from CUSTOMER.cpy)
- Plus `WS-CHILD-SUCCESS` (PIC X, 1 byte)
- Plus `WS-CHILD-FAIL-CODE` (PIC X, 1 byte)
- `WsChildDataSerializer` is the canonical serializer (ADR-1, Rule 5/15)

**Fail codes set by Credit-Check paragraph (PE-8):**

| Fail Code | Trigger |
|---|---|
| `'A'` | PUT CONTAINER failed |
| `'B'` | RUN TRANSID failed |
| `'C'` | FETCH ANY NOTFINISHED + zero retrieved |
| `'D'` | FETCH ANY INVREQ (no children) |
| `'E'` | GET CONTAINER failed |
| `'F'` | FETCH ANY completion = ABEND |
| `'G'` | FETCH ANY completion = SECERROR |
| `'H'` | FETCH ANY completion = OTHER non-success |

**CICS API mapping (ADR-5):**

| COBOL command | Java method/API |
|---|---|
| `EXEC CICS PUT CONTAINER` | `Channel.createContainer().put()` |
| `EXEC CICS RUN TRANSID` | `AsyncServiceImpl.runTransactionId(tranId, channel)` — NOT `AsyncServiceImpl.run()` |
| `EXEC CICS DELAY FOR SECONDS(3)` | `Thread.sleep(3000)` — `Task.getTask().delay()` does NOT exist in JCICS |
| `EXEC CICS FETCH ANY NOSUSPEND` | `AsyncServiceImpl.getAny(BlockingAction.NOSUSPEND)` |
| `EXEC CICS GET CONTAINER` | `Channel.getContainer().get()` |

**Review-date computation (BR-3, FR-6):**
- Success path (≥1 retrieved): `today.plusDays(((21 - 1) * random(eibtaskn)) + 1)` → today+1 to today+20
- Failure path (0 retrieved): review date = today
- Stored in commarea fields: `commCsReviewDay`, `commCsReviewMonth`, `commCsReviewYear`
- Constant: `REVIEW_DATE_MAX_DAYS = 21` (exclusive upper bound, Rule 8)

**Named constants required (all `private static final`):**
- `CIPCREDCHANN` — channel name (String, 16 bytes)
- `CIPA`, `CIPB`, `CIPC`, `CIPD`, `CIPE` — container names (String)
- `OCR1`–`OCR5` — transaction IDs (String, 4 bytes)
- `AGENCY_COUNT = 5` — credit agencies (int)
- `DELAY_SECONDS = 3000` — delay in milliseconds (int)
- `REVIEW_DATE_MAX_DAYS = 21` — exclusive upper bound (int)

**Error-path classification (Rule 1 / NFR-3.2):** All credit-check fail-code paths are
**silent-return** — set fail-code + `COMM-SUCCESS='N'` and return; no notification, no ABEND.

**Serializer delegation (Rule 15):** `WsChildDataSerializer` is the ONLY class that reads
container bytes into `WsChildData`. `CreditCheckService` must call
`wsChildDataSerializer.fromBytes(containerBytes)` — no inline byte-reading.

**Named exception factories (Rule 3):** All fail-codes set via `CrecustException` static
factory methods — e.g. `CrecustException.ccNotFinished()`, `CrecustException.ccInvreq()`, etc.

## Technical Decisions

**Package and class shape:**
- `CreditCheckService` lives in `com.ibm.cics.botz.crecust.service`.
- No instance state (NFR-5.1); all context passed via method parameters and local variables.
- Lombok is enabled on data-model classes (ADR-12); `CreditCheckService` itself is a stateless
  service class — no Lombok data annotations needed.

**Commarea model:** `CrecustCommarea` (Epic 2, `com.ibm.cics.botz.crecust.model`) — the service
reads and writes fields `commCreditScore`, `commCsReviewDay`, `commCsReviewMonth`,
`commCsReviewYear`, `commSuccess`, `commFailCode` directly on the instance.

**WsChildData model:** `WsChildData` (Epic 2, `com.ibm.cics.botz.crecust.model`) — 399 bytes,
mirrors `CustomerRecord` + success + fail-code fields. Serialized exclusively by
`WsChildDataSerializer`.

**Calling context (from `CrecustService`, Epic 3):** `CreditCheckService.performCreditCheck(commarea)`
is called after `validateTitle()` / `validateDateOfBirth()` succeed. Its fail-codes short-circuit
the orchestrator before ENQ / DB2 access.

**Coding standards (NFR-2):**
- Methods must not exceed 40 lines.
- No magic numbers or strings; all sentinels are named constants.
- SLF4J `Logger` (`private static final`); `INFO` for milestones, `DEBUG` for per-record,
  `ERROR` for failures.
- No PII logged at any level (NFR-2.6).

## Cross-Story Dependencies

- **Depends on Epic 1** — Maven project and build infrastructure must exist before code compiles.
- **Depends on Epic 2** — `CrecustCommarea` model (Story 2.1), `WsChildData` model and
  `WsChildDataSerializer` (Story 2.5) must exist; fields `commCreditScore`, `commSuccess`,
  `commFailCode`, `commCsReviewDay/Month/Year` must be accessible.
- **Depends on Epic 3** — `CrecustService` (Story 3.2) calls `CreditCheckService.performCreditCheck()`;
  the orchestrator wiring must accept the service.
- **Depends on Epic 9** — `CrecustException` (Story 9.1) must expose factory methods for all
  credit-check fail-codes (`ccNotFinished`, `ccInvreq`, `ccAbend`, `ccSecError`, `ccOther`,
  `putContainerError`, `runTransidError`, `getContainerError`) before those call-sites compile.
- **Story 5.1 → 5.2 → 5.3 → 5.4**: Stories build sequentially on the same service class;
  Story 5.3 directly consumes the channel/tokens produced by 5.1 and 5.2.
- **Downstream:** Epics 6–8 only execute if credit-check passes; `CreditCheckService` is a gate
  before ENQ / DB2 work.
