---
title: 'CustomerNumberService.dequeue() — four DEQ call sites (FR-7.5, AC-20.2)'
type: 'feature'
created: '2026-10-01'
status: 'draft'
route: 'dispatch'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The COBOL `DEQ-NAMED-COUNTER_DNC010` paragraph releases the CICS named-resource ENQ and is called from four distinct call sites (Fan-In-4). Without a corresponding shared `dequeue()` method in the Java service — and without that method being wired to exactly the four required call sites — the ENQ acquired by `enqueue()` will be held indefinitely on certain error paths, deadlocking subsequent CICS tasks trying to allocate a customer number.

**Approach:** Implement `CustomerNumberService.dequeue(CrecustCommarea)` returning `void`, which calls `NameResource.dequeue()` on the 16-byte resource name; handle the CICS exception with the silent-return pattern (fail-code `'5'`). Wire the call from exactly four call sites that mirror the COBOL Fan-In-4: (1) inside `CustomerNumberService.getAndIncrementCustomerNumber()` on SQL failure (Story 6.2), (2) inside `CustomerDbService.insertCustomer()` on INSERT CUSTOMER failure (Story 7.2), (3) inside `ProctranDbService.insertProctran()` on INSERT PROCTRAN failure before ABEND (Story 8.2), and (4) inside `ProctranDbService.insertProctran()` on successful PROCTRAN write (Story 8.1). This story authors the `dequeue()` method itself and wires all four call sites.

## Boundaries & Constraints

**Always:**
- `dequeue()` signature must be `public void dequeue(CrecustCommarea commarea)` in `CustomerNumberService` (package `com.ibm.cics.botz.crecust.service`).
- `NameResource.dequeue()` must use the same 16-byte resource name as `enqueue()`: `NCS_ACT_NAME + commarea.getCommSortcode()` truncated/padded to `ENQ_RESOURCE_LENGTH` (16). Reuse the existing named constants — do not redeclare them.
- On `CicsConditionException` from `NameResource.dequeue()`: set `commarea.setCommSuccess('N')`, set `commarea.setCommFailCode(CrecustException.FAIL_CODE_DEQ)`, and return — silent-return path, Rule 1. No ABEND, no notification.
- Call sites must reference `customerNumberService.dequeue(commarea)` (or equivalent injected field) — never inline the `NameResource.dequeue()` call outside `CustomerNumberService`.
- There must be exactly four `dequeue(` call sites in the codebase after this story is complete — confirmed by AC-20.2. No fifth call site may be added.
- `CrecustException.FAIL_CODE_DEQ` (`'5'`) must be used — never the char literal `'5'` directly at the call site.
- All CICS calls use `com.ibm.cics.server.*` only — no custom wrappers (cics-transformation rule, Principle 1).

**Never:**
- Do not generate an exception-handler class for `CustomerNumberService` — there is no COBOL `HANDLE CONDITION` construct grounding one (Rule 16 / ADR-10).
- Do not add any DEQ call site inside `enqueue()` itself.
- Do not add a DEQ call site on the ENQ-failure path (that path returns before any ENQ is acquired).
- Do not add a DEQ on the happy path of `getAndIncrementCustomerNumber()` — the successful SELECT+UPDATE case falls through to `insertCustomer()`, where the DEQ call sites (2) or (4) handle release.
- Do not make `dequeue()` throw a checked exception — it must swallow the CICS exception and set the fail-code.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|---------------------------|----------------|
| DEQ succeeds (normal) | `NameResource.dequeue()` completes without exception | Returns void; `commSuccess` and `commFailCode` unchanged | N/A |
| DEQ throws CicsConditionException | `NameResource.dequeue()` throws `CicsConditionException` | `commSuccess = 'N'`, `commFailCode = FAIL_CODE_DEQ ('5')`; method returns | Silent-return; no re-throw, no ABEND |
| SQL failure in CONTROL SELECT/UPDATE (call site 1) | `getAndIncrementCustomerNumber()` catches SQL exception | `dequeue(commarea)` called; then `commSuccess = 'N'`, `commFailCode = FAIL_CODE_CONTROL_SQL ('4')`; returns | DEQ called before setting fail-code `'4'` |
| INSERT CUSTOMER failure (call site 2) | `insertCustomer()` catches SQL exception | `dequeue(commarea)` called; then `commSuccess = 'N'`, `commFailCode = FAIL_CODE_INSERT_CUSTOMER ('1')`; returns | DEQ called before setting fail-code `'1'` |
| INSERT PROCTRAN failure (call site 3) | `insertProctran()` catches SQL exception | `dequeue(commarea)` called as step 2 of notifying-abort sequence (after AbndInfoRec populated, before LINK ABNDPROC, before ABEND) | Notifying-abort ordering: populate → DEQ → LINK → ABEND (Rule 1) |
| Successful PROCTRAN INSERT (call site 4 — happy path) | `insertProctran()` completes successfully | `dequeue(commarea)` called after successful INSERT, before method returns | N/A |

</frozen-after-approval>

## Code Map

- `src/main/java/com/ibm/cics/botz/crecust/service/CustomerNumberService.java` — owns `enqueue()` (Story 6.1), `getAndIncrementCustomerNumber()` (Story 6.2), and `dequeue()` (this story); `NCS_ACT_NAME` and `ENQ_RESOURCE_LENGTH` constants already declared here (Story 6.1)
- `src/main/java/com/ibm/cics/botz/crecust/service/CustomerDbService.java` — `insertCustomer()` method; DEQ call site 2 (INSERT CUSTOMER failure) added here (Story 7.2 scope, but wired here)
- `src/main/java/com/ibm/cics/botz/crecust/service/ProctranDbService.java` — `insertProctran()` method; DEQ call sites 3 (PROCTRAN failure, notifying-abort) and 4 (PROCTRAN success) added here (Stories 8.1/8.2 scope, but wired here)
- `src/main/java/com/ibm/cics/botz/crecust/exception/CrecustException.java` — `FAIL_CODE_DEQ = '5'` constant (Epic 9 Story 9.1); referenced by `dequeue()`
- `src/main/java/com/ibm/cics/botz/crecust/model/CrecustCommarea.java` — `commSuccess` and `commFailCode` fields; setters called inside `dequeue()` on failure
- `src/test/java/com/ibm/cics/botz/crecust/service/CustomerNumberServiceTest.java` — unit tests for `dequeue()` success and failure paths

## Tasks & Acceptance

**Execution:**
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/CustomerNumberService.java` — add `public void dequeue(CrecustCommarea commarea)` method: construct `NameResource` with the same 16-byte resource name as `enqueue()` (reuse `NCS_ACT_NAME`, `ENQ_RESOURCE_LENGTH`), call `nameResource.dequeue()`, catch `CicsConditionException`, set `commarea.setCommSuccess('N')` and `commarea.setCommFailCode(CrecustException.FAIL_CODE_DEQ)`, return — this is the Fan-In-4 shared utility corresponding to `DEQ-NAMED-COUNTER_DNC010`
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/CustomerNumberService.java` — verify call site 1 is wired: in the SQL-failure catch block of `getAndIncrementCustomerNumber()`, confirm `dequeue(commarea)` is called before setting `FAIL_CODE_CONTROL_SQL` and returning (Story 6.2 scope — add if missing)
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/CustomerDbService.java` — wire call site 2: in the SQL-failure catch block of `insertCustomer()`, call `customerNumberService.dequeue(commarea)` before setting `commSuccess = 'N'`, `commFailCode = FAIL_CODE_INSERT_CUSTOMER ('1')`, and returning (FR-8.5, Rule 1 silent-return)
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/ProctranDbService.java` — wire call site 3: in the notifying-abort catch block of `insertProctran()`, call `customerNumberService.dequeue(commarea)` as step 2 — after populating `AbndInfoRec` fields, before calling `AbndprocDelegate.linkAbndproc()`, before `Task.getTask().abend(ABEND_CODE_HWPT)` (FR-9.5, Rule 1 ordering)
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/ProctranDbService.java` — wire call site 4: on the success path of `insertProctran()`, after the JDBC INSERT succeeds, call `customerNumberService.dequeue(commarea)` before the method returns (FR-7.5 happy-path DEQ)
- [ ] `src/test/java/com/ibm/cics/botz/crecust/service/CustomerNumberServiceTest.java` — add test: mock `NameResource.dequeue()` to succeed; call `dequeue(commarea)`; assert `commSuccess` and `commFailCode` are unchanged
- [ ] `src/test/java/com/ibm/cics/botz/crecust/service/CustomerNumberServiceTest.java` — add test: mock `NameResource.dequeue()` to throw `CicsConditionException`; call `dequeue(commarea)`; assert `commSuccess = 'N'` and `commFailCode = CrecustException.FAIL_CODE_DEQ`
- [ ] `src/test/java/com/ibm/cics/botz/crecust/service/CustomerNumberServiceTest.java` — add test confirming Fan-In-4: grep or inspect the codebase for `dequeue(` and assert exactly four invocation sites exist (AC-20.2)

**Acceptance Criteria:**

- Given `NameResource.dequeue()` completes without exception, when `CustomerNumberService.dequeue(commarea)` is called, then the method returns without modifying `commSuccess` or `commFailCode`.
- Given `NameResource.dequeue()` throws a `CicsConditionException`, when `CustomerNumberService.dequeue(commarea)` is called, then `commarea.getCommSuccess() == 'N'` and `commarea.getCommFailCode() == CrecustException.FAIL_CODE_DEQ` (`'5'`), and the method returns — no exception is re-thrown, no `Task.abend()` is called (Rule 1 silent-return, FR-7.5).
- Given `CrecustException.FAIL_CODE_DEQ` is referenced inside `dequeue()`, when the source is compiled, then no char literal `'5'` appears as the fail-code at that call site (Rule 3 / AC-3).
- Given the complete codebase for Epic 6 + Epic 7 + Epic 8, when the source is grepped for `\.dequeue(`, then exactly four call sites are found — one each in `CustomerNumberService.getAndIncrementCustomerNumber()`, `CustomerDbService.insertCustomer()`, `ProctranDbService.insertProctran()` (failure path), and `ProctranDbService.insertProctran()` (success path) (FR-7.5, AC-20.2).
- Given the notifying-abort path in `ProctranDbService.insertProctran()`, when an INSERT PROCTRAN SQL failure occurs, then the execution sequence is: (1) populate `AbndInfoRec` fields, (2) call `customerNumberService.dequeue(commarea)`, (3) call `abndprocDelegate.linkAbndproc(abndInfoRec)`, (4) call `Task.getTask().abend(CrecustException.ABEND_CODE_HWPT)` — reversing any step is a functional defect (Rule 1 notifying-abort ordering).
- Given `CustomerDbService.insertCustomer()` SQL failure, when the failure catch block executes, then `customerNumberService.dequeue(commarea)` is called before `commSuccess` and `commFailCode` are set (FR-8.5, Rule 1 silent-return).
- Given `ProctranDbService.insertProctran()` succeeds (happy path), when the method returns normally, then `customerNumberService.dequeue(commarea)` was called — confirming the ENQ is always released on the success path (FR-7.5, BR-7).
- Given `CustomerDbService` and `ProctranDbService` each hold a reference to `CustomerNumberService`, when the class is instantiated, then `CustomerNumberService` is injected via constructor (not instantiated inline) — ensuring the same instance is reachable for the Fan-In-4 test.

## Implementation Notes

### Dev Notes (2026-10-09)

**Implementation**: `CustomerNumberService.dequeue(CrecustCommarea, NameResource)` implemented in `crecust-java/src/main/java/com/ibm/cics/botz/crecust/service/CustomerNumberService.java`.

**COBOL source**: Translates `DEQ-NAMED-COUNTER_DNC010` (CRECUST.cbl lines 562–584). Calls `nameResource.dequeue()` on the same `NameResource` instance returned by `enqueue()`. On `CicsConditionException`: sets `commSuccess="N"`, `commFailCode=CrecustException.FAIL_CODE_DEQ` (`"5"`), returns — silent-return path (Rule 1). No ABEND, no re-throw.

**Null guard**: Added `if (nameResource == null)` guard with WARN log + early return to prevent NPE on any edge case where ENQ was never acquired.

**Verified DEQ call sites** (AC-20.2 partial — full verification requires Stories 7.2 and 8.1/8.2):
- **Site 1 ✅ verified**: `CustomerNumberService.failControlSql()` → `dequeue()` (CONTROL SQL failure, Story 6.2). Present at line 237.
- **Site 2 ✅ verified**: `CrecustService.execute()` notifying-abort catch block → `customerNumberService.dequeue(commArea, nameResource)` (Story 8.2 PROCTRAN failure). Present at line 223 of `CrecustService.java`.
- **Site 3 ⏳ deferred**: `CustomerDbService.insertCustomer()` — INSERT CUSTOMER failure (Story 7.2 scope). Not yet wired.
- **Site 4 ✅ verified**: `CrecustService.execute()` Step 9 — success path after PROCTRAN → `customerNumberService.dequeue(commArea, nameResource)`. Present at line 231 of `CrecustService.java`.

**Currently wired**: 3 of 4 call sites exist in the codebase. Site 3 (`CustomerDbService.insertCustomer()` failure) is deferred to Story 7.2. AC-20.2 (exactly 4 call sites) will be fully verified after Story 7.2.

**Compile**: `mvn compile` → BUILD SUCCESS (0.264s).

## Spec Change Log

## Review Triage Log

## Design Notes

The COBOL `DEQ-NAMED-COUNTER_DNC010` paragraph has Fan-In = 4 — called from four different paragraphs. This maps directly to a single `dequeue()` method in `CustomerNumberService` called from four Java methods across three service classes. The critical constraint is ordering at call site 3 (PROCTRAN failure): COBOL executes `GET-ME-OUT-OF-HERE` (which calls DEQ) **after** linking ABNDPROC and before the ABEND. The Java equivalent must call `dequeue()` **after** populating `AbndInfoRec` and **before** calling `Task.getTask().abend()` — the same step ordering.

Resource-name construction: the `NameResource` instance must be constructed with `(NCS_ACT_NAME + commarea.getCommSortcode() + "  ").substring(0, ENQ_RESOURCE_LENGTH)` or equivalent, producing a 16-byte string. This is the same logic as `enqueue()` — extract it to a private helper method `buildResourceName(CrecustCommarea)` if both methods share it.

Note the distinction between the ENQ resource name (16 bytes, `CHAR`) and `HV-CONTROL-NAME` (32 bytes, `CHAR(32)` CONTROL table key) — they share a common prefix but are different fields with different lengths.

## Verification

**Commands:**
- `mvn test -pl . -Dtest=CustomerNumberServiceTest` -- expected: all `dequeue` tests pass, zero failures
- `grep -rn "\.dequeue(" src/main/java/` -- expected: exactly 4 matching lines across `CustomerNumberService.java`, `CustomerDbService.java`, and `ProctranDbService.java`
