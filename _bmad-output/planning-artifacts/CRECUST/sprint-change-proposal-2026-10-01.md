# Sprint Change Proposal — JCICS API Mapping Corrections

**Date:** 2026-10-01  
**Project:** Bank-of-Z — CRECUST Java Modernisation  
**Scope:** Minor — direct implementation correction; no story additions or removals required  
**Prepared for:** Arnold

---

## 1. Issue Summary

During review of the CRECUST planning and implementation artifacts, five JCICS API calls were identified as incorrect — the methods or call patterns specified in the planning documents do not exist in the JCICS API library (`com.ibm.cics.server`). These errors would cause compile failures or incorrect runtime behaviour if implemented as written.

### Root Cause

The original architecture and story specifications were authored using assumed or hallucinated JCICS method names. None of the five affected calls exist in the actual JCICS API:

| Incorrect mapping | Problem |
|---|---|
| `Task.getTask().returnToCaller()` | Method does not exist in JCICS. `main()` returning naturally exits to CICS. |
| `Task.getTask().getAbstime()` | Method does not exist in JCICS. Java-native `LocalDateTime.now()` is the replacement. |
| `Task.getTask().delay(3000)` | Method does not exist in JCICS. Java-native `Thread.sleep(3000)` is the replacement. |
| `AsyncServiceImpl.run(tranId, channel)` | Wrong method name. Correct JCICS method is `AsyncServiceImpl.runTransactionId(tranId, channel)`. |
| `Task.getTask().getTask().getRegion()...getApplid()` | Chained call does not exist. Correct JCICS call is `Region.getAPPLID()`. |

---

## 2. Impact Analysis

### Affected Stories

| Story | Issue | Severity |
|---|---|---|
| Story 3.1 — `Crecust` entry point | `Task.getTask().returnToCaller()` specified; method does not exist | Compile error |
| Story 3.4 — `populateTimeAndDate()` | `Task.getTask().getAbstime()` specified; method does not exist | Compile error |
| Story 5.2 — `runChildTransaction()` | `AsyncServiceImpl.run()` + `Task.getTask().delay()` both wrong | Compile error (x2) |
| Story 8.1 — `insertProctran()` happy path | `Task.getTask().getAbstime()` specified; method does not exist | Compile error |
| Story 8.2 — notifying-abort path | `Task.getTask().getTask().getRegion()...getApplid()` + `Task.getTask().getAbstime()` both wrong | Compile error (x2) |

### Affected Artifacts

| Artifact | Change Required |
|---|---|
| `architecture.md` — §9 CICS API Mapping table | All 5 incorrect rows corrected |
| `architecture.md` — ADR-5 middleware mapping table | `AsyncServiceImpl.run()` → `runTransactionId()`; `Task.delay()` → `Thread.sleep()` |
| `prd.md` — FR-1.4 | `returnToCaller()` removed; replaced with Java `return` statement description |
| `prd.md` — FR-4.1 | ASKTIME mapping updated to `LocalDateTime.now()` |
| `prd.md` — FR-5.1–5.5 | Removed COBOL helper shim language; reflected direct `AsyncServiceImpl` pattern |
| `epics.md` — Stories 3.1, 3.4, 5.2, 8.1, 8.2 | User stories, acceptance criteria, and notes updated |
| `epic-3-context.md` | CICS API Mapping table corrected for RETURN and ASKTIME |
| `epic-5-context.md` | CICS API Mapping table corrected for RUN TRANSID and DELAY |
| `epic-8-context.md` | Technical Decisions section corrected for ASKTIME and ASSIGN APPLID |
| `stories/3-1-*.md` | Intent, Constraints, AC, Design Notes, Verification corrected |
| `stories/3-4-*.md` | Intent, Constraints, IO Matrix, Tasks, AC, Design Notes corrected |
| `stories/5-2-*.md` | Intent, Constraints, IO Matrix, Tasks, AC, Design Notes corrected |
| `stories/8-1-*.md` | Intent, Constraints, Tasks, AC, Design Notes corrected |
| `stories/8-2-*.md` | Constraints (Never block added), IO Matrix, Tasks, AC, Design Notes corrected |

### No Stories Added or Removed

All corrections are in-place API substitutions. The feature boundaries, acceptance-criteria structure, and delivery order remain unchanged. The task count and sprint plan are unaffected.

---

## 3. Recommended Approach

**Direct adjustment** — all five corrections are drop-in API substitutions requiring no architectural redesign:

| Correction | Impact on implementation |
|---|---|
| `EXEC CICS RETURN` → `return;` | Remove one line from `Crecust.main()`; no functional logic changes |
| `EXEC CICS ASKTIME` → `LocalDateTime.now()` | Replace JCICS call with Java stdlib; simplifies Story 3.4 and 8.1 (no epoch-offset constant needed) |
| `EXEC CICS DELAY FOR SECONDS(3)` → `Thread.sleep(3000)` | Swap one call in `CreditCheckService.delayForResults()`; add `InterruptedException` handler |
| `EXEC CICS RUN TRANSID` → `AsyncServiceImpl.runTransactionId(tranId, channel)` | Rename one method in `CreditCheckService.runChildTransaction()` |
| `EXEC CICS ASSIGN APPLID` → `Region.getAPPLID()` | Simplify one call in `ProctranDbService` SQL-failure catch block |

**Effort estimate:** Trivial. Each change is a single-line substitution at implementation time; the primary work was updating all planning/specification documents (completed in this proposal).

**Risk:** Low. The Java-native replacements (`LocalDateTime.now()`, `Thread.sleep()`, `return`) are well-understood. `Region.getAPPLID()` and `AsyncServiceImpl.runTransactionId()` are standard documented JCICS APIs.

**Timeline impact:** None. These corrections make the stories _simpler_, not more complex.

---

## 4. Detailed Change Proposals

### Change 1 — `EXEC CICS RETURN` → Java `return` statement

**Files:** `architecture.md §9`, `prd.md FR-1.4`, `epics.md Story 3.1`, `story 3-1-*.md`, `epic-3-context.md`

| Field | OLD | NEW |
|---|---|---|
| JCICS call | `Task.getTask().returnToCaller()` | `return;` (fall off end of `main()`) |
| Rationale | Method does not exist in JCICS. `AbstractProgram.main()` returning naturally returns control to CICS — exactly equivalent to `EXEC CICS RETURN`. |

**Story 3.1 Acceptance Criteria change:**

OLD:
> `Task.getTask().returnToCaller()` is called exactly once at the single shared exit point.

NEW:
> `Crecust.main()` returns normally at a single shared exit point. `Task.getTask().returnToCaller()` does NOT exist in JCICS and must NOT be called.

---

### Change 2 — `EXEC CICS ASKTIME` → `LocalDateTime.now()`

**Files:** `architecture.md §9`, `prd.md FR-4`, `epics.md Stories 3.4 + 8.1`, `story 3-4-*.md`, `story 8-1-*.md`, `epic-3-context.md`, `epic-8-context.md`

| Field | OLD | NEW |
|---|---|---|
| JCICS call | `Task.getTask().getAbstime()` | `LocalDateTime.now()` |
| Epoch constant | `LILIAN_EPOCH_OFFSET` / `CICS_EPOCH_OFFSET_MILLIS` required | Not needed — `LocalDateTime.now()` is wall-clock time directly |
| Test strategy | Mockito `mockStatic(Task.class)` | `Clock.fixed(...)` injection — no JCICS mocking required |
| Rationale | Method does not exist in JCICS. `LocalDateTime.now()` is the correct Java-native replacement. |

---

### Change 3 — `EXEC CICS DELAY FOR SECONDS(3)` → `Thread.sleep(3000)`

**Files:** `architecture.md §9 + ADR-5`, `epics.md Story 5.2`, `story 5-2-*.md`, `epic-5-context.md`

| Field | OLD | NEW |
|---|---|---|
| JCICS call | `Task.getTask().delay(3000)` | `Thread.sleep(DELAY_MILLISECONDS)` |
| Exception handling | N/A | Wrap in `try-catch (InterruptedException)`; restore interrupt flag |
| Rationale | Method does not exist in JCICS. Java-native `Thread.sleep()` provides the equivalent blocking delay. |

---

### Change 4 — `EXEC CICS RUN TRANSID` → `AsyncServiceImpl.runTransactionId(tranId, channel)`

**Files:** `architecture.md §9 + ADR-5`, `epics.md Story 5.2`, `story 5-2-*.md`, `epic-5-context.md`

| Field | OLD | NEW |
|---|---|---|
| JCICS call | `AsyncServiceImpl.run(tranId, channelName)` | `AsyncServiceImpl.runTransactionId(tranId, channel)` |
| Rationale | `run()` is not a method on `AsyncServiceImpl`. The correct JCICS method for `EXEC CICS RUN TRANSID` is `runTransactionId(tranId, channel)`. |

---

### Change 5 — `EXEC CICS ASSIGN APPLID` → `Region.getAPPLID()`

**Files:** `architecture.md §9`, `epics.md Story 8.2`, `story 8-2-*.md`, `epic-8-context.md`

| Field | OLD | NEW |
|---|---|---|
| JCICS call | `Task.getTask().getTask().getRegion()...getApplid()` | `Region.getAPPLID()` |
| Rationale | The chained form does not exist in JCICS. `Region.getAPPLID()` is the direct static accessor for the APPLID of the CICS region. |

---

## 5. Implementation Handoff

**Scope classification:** Minor — direct implementation by Developer agent.

**Deliverables from this proposal:**
- ✅ `_bmad-output/planning-artifacts/CRECUST/architecture.md` — corrected
- ✅ `_bmad-output/planning-artifacts/CRECUST/prd.md` — corrected
- ✅ `_bmad-output/planning-artifacts/CRECUST/epics.md` — corrected
- ✅ `_bmad-output/implementation-artifacts/CRECUST/epic-3-context.md` — corrected
- ✅ `_bmad-output/implementation-artifacts/CRECUST/epic-5-context.md` — corrected
- ✅ `_bmad-output/implementation-artifacts/CRECUST/epic-8-context.md` — corrected
- ✅ `stories/3-1-crecust-entry-point-*.md` — corrected
- ✅ `stories/3-4-crecustservice-populatetimeanddate-*.md` — corrected
- ✅ `stories/5-2-creditcheckservice-runchildtransaction-*.md` — corrected
- ✅ `stories/8-1-proctrandbservice-insertproctran-*.md` — corrected
- ✅ `stories/8-2-notifying-abort-path-*.md` — corrected

**Next steps for Developer agent:**
1. When implementing Story 3.1: do not call `Task.getTask().returnToCaller()` — just `return;`
2. When implementing Story 3.4: use `LocalDateTime.now()` (with optional `Clock` injection for testability); no Mockito JCICS static mock needed in tests
3. When implementing Story 5.2: use `AsyncServiceImpl.runTransactionId(tranId, channel)`; use `Thread.sleep(DELAY_MILLISECONDS)` with `InterruptedException` handler
4. When implementing Story 8.1: use `LocalDateTime.now()` directly; no epoch-offset constant needed
5. When implementing Story 8.2: use `Region.getAPPLID()` for `abndApplid`; use `LocalDateTime.now()` for `abndUtimeKey`

**Success criteria for implementation:**
- `grep -r 'returnToCaller' src/` returns zero matches
- `grep -r 'getAbstime' src/` returns zero matches  
- `grep -r 'Task.*delay' src/` returns zero matches
- `grep -r 'AsyncServiceImpl.run(' src/` returns zero matches (only `runTransactionId` allowed)
- `grep -r 'getRegion.*getApplid' src/` returns zero matches
- `mvn compile -pl crecust` returns BUILD SUCCESS

---

*Correct Course workflow complete, Arnold!*
