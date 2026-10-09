---
title: 'CrecustService Orchestrator Skeleton With Full Flow Wiring'
type: 'feature'
created: '2026-10-01'
status: 'review'
route: 'dispatch'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The PREMIERE_P010 orchestration flow has no Java equivalent; downstream service classes (Epics 4–8) cannot be wired into an end-to-end call chain until the orchestrator exists.

**Approach:** Create `CrecustService` in `com.ibm.cics.botz.crecust.service` with a single `execute(CrecustCommarea)` method that calls each downstream service in the exact PREMIERE_P010 order, short-circuits on any fail-code, and sets the success fields on the happy path. Stub interfaces are sufficient for compilation; no service logic is implemented here.

## Boundaries & Constraints

**Always:**
- Call order must exactly mirror PREMIERE_P010 (lines 407–520): `validateTitle` → `validateDateOfBirth` → `populateTimeAndDate` → `performCreditCheck` → `enqueue` → `getAndIncrementCustomerNumber` → `insertCustomer` → `insertProctran` → `dequeue` → set success fields.
- Every short-circuit return sets the commarea fail-code before returning from `execute()`; no silent pass-through.
- Notifying-abort path on PROCTRAN failure: populate `AbndInfoRec` → call `customerNumberService.dequeue()` → call `abndprocDelegate.linkAbndproc(abndInfoRec)` → call `Task.getTask().abend(CrecustException.ABEND_CODE_HWPT)` — in that order (Rule 1).
- `CrecustService` holds no per-request instance state (NFR-5); all context is passed as parameters.
- Package: `com.ibm.cics.botz.crecust.service` (ADR-2).
- SLF4J `private static final Logger`; milestones at `INFO`, per-step detail at `DEBUG`; no PII or financial values logged (NFR-2.5, NFR-2.6).
- All downstream services are constructor-injected plain fields; no Spring/CDI/`@Resource` (ADR-4).
- `populateTimeAndDate()` and `populateCicsVersion()` are declared as `private` methods on `CrecustService` with stub bodies (`// TODO: Story 3.4` and `// TODO: Story 3.5` respectively); they are called from `execute()` in the correct position.
- Success path sets `commArea.setCommSuccess('Y')`, `commArea.setCommFailCode(' ')`, `commArea.setCommEyecatcher("CUST")`.

**Never:**
- No business logic in service field declarations or constructor beyond assignment.
- No raw byte operations in `CrecustService`; serialization belongs to `CrecustareaSerializer`.
- Do not implement any downstream service logic (validation, credit-check, DB2) — stubs only.
- Do not add a dedicated exception-handler class; ADR-10 prohibits one (no COBOL HANDLE CONDITION grounding).
- Do not generate a `CrecustServiceExceptionHandler` or any analogous handler class.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Happy path | Valid commarea; all stubs return without setting fail-code | `commSuccess='Y'`, `commFailCode=' '`, `commEyecatcher="CUST"`; `dequeue()` called last | N/A |
| Title invalid | `validateTitle()` stub sets `commFailCode='T'` | `execute()` returns immediately after step 1; no further service called | Silent-return — fail-code already set |
| DOB invalid | `validateDateOfBirth()` stub sets `commFailCode` to `'O'`/`'Z'`/`'Y'` | Returns immediately after step 2 | Silent-return |
| Credit-check failure | `performCreditCheck()` stub sets a credit-check fail-code | Returns immediately after step 4; ENQ not called | Silent-return |
| ENQ failure | `enqueue()` stub sets `commFailCode='3'` | Returns immediately after step 5 | Silent-return |
| CONTROL SQL failure | `getAndIncrementCustomerNumber()` sets `commFailCode='4'` (also calls DEQ internally) | Returns immediately after step 6 | Silent-return |
| INSERT CUSTOMER failure | `insertCustomer()` sets `commFailCode='1'` (also calls DEQ internally) | Returns immediately after step 7 | Silent-return |
| PROCTRAN failure | `insertProctran()` throws `CrecustException` signalling notifying-abort | Populate `AbndInfoRec` → DEQ → `linkAbndproc` → `Task.getTask().abend(ABEND_CODE_HWPT)` | Notifying-abort (Rule 1) |

</frozen-after-approval>

## Code Map

- `com.ibm.cics.botz.crecust.service.CrecustService` — **new file**; the class being created in this story
- `com.ibm.cics.botz.crecust.model.CrecustCommarea` — Epic 2, Story 2.1; primary parameter to `execute()`; fields: `commSuccess`, `commFailCode`, `commEyecatcher`, `commSortcode`, `commNumber`, `commTitle`, `commDobDay`, `commDobMonth`, `commDobYear`, `commCreditScore`, `commCsReviewDate`
- `com.ibm.cics.botz.crecust.model.CustomerRecord` — Epic 2, Story 2.1; passed to `insertCustomer()`
- `com.ibm.cics.botz.crecust.model.AbndInfoRec` — Epic 2, Story 2.1; populated on notifying-abort path
- `com.ibm.cics.botz.crecust.db.HostCustomerRow` — Epic 2, Story 2.1; passed to `insertCustomer()`
- `com.ibm.cics.botz.crecust.exception.CrecustException` — Epic 9, Story 9.1; `ABEND_CODE_HWPT` constant; factory methods for each fail-code
- `com.ibm.cics.botz.crecust.service.ValidationService` — Epic 4; stub for Story 3.2; `validateTitle(CrecustCommarea)`, `validateDateOfBirth(CrecustCommarea)`
- `com.ibm.cics.botz.crecust.service.CreditCheckService` — Epic 5; stub; `performCreditCheck(CrecustCommarea)`
- `com.ibm.cics.botz.crecust.service.CustomerNumberService` — Epic 6; stub; `enqueue(CrecustCommarea)`, `getAndIncrementCustomerNumber(CrecustCommarea)`, `dequeue(CrecustCommarea)`
- `com.ibm.cics.botz.crecust.service.CustomerDbService` — Epic 7; stub; `insertCustomer(CrecustCommarea, CustomerRecord, HostCustomerRow)`
- `com.ibm.cics.botz.crecust.service.ProctranDbService` — Epic 8; stub; `insertProctran(CrecustCommarea, ...)`
- `com.ibm.cics.botz.crecust.service.AbndprocDelegate` — Epic 8; stub; `linkAbndproc(AbndInfoRec)` returns `void`
- `com.ibm.cics.server.Task` — JCICS; `Task.getTask().abend(String)` used on notifying-abort path
- PREMIERE_P010 source: `.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl` lines 407–520 — authoritative call-order reference

## Tasks & Acceptance

**Execution:**
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/CrecustService.java` — create class with `execute(CrecustCommarea commArea)` method implementing the 10-step PREMIERE_P010 flow; declare constructor receiving all six downstream service dependencies; add stub `private void populateTimeAndDate()` and `private void populateCicsVersion()` methods with `// TODO` bodies; add SLF4J logger
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/ValidationService.java` — create stub class with `validateTitle(CrecustCommarea)` and `validateDateOfBirth(CrecustCommarea)` as empty methods (bodies: `// stub — implemented in Story 4.x`)
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/CreditCheckService.java` — create stub class with `performCreditCheck(CrecustCommarea)` as empty method
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/CustomerNumberService.java` — create stub class with `enqueue(CrecustCommarea)`, `getAndIncrementCustomerNumber(CrecustCommarea)`, and `dequeue(CrecustCommarea)` as empty methods
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/CustomerDbService.java` — create stub class with `insertCustomer(CrecustCommarea, CustomerRecord, HostCustomerRow)` as empty method
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/ProctranDbService.java` — create stub class with `insertProctran(CrecustCommarea, String, String, String, String)` as empty method (parameters: commArea, storedSortcode, storedCustno, storedName, storedDob)
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/AbndprocDelegate.java` — create stub class with `void linkAbndproc(AbndInfoRec abndInfoRec)` as empty method

**Acceptance Criteria:**
- Given stub implementations of all downstream services return immediately without setting a fail-code, when `CrecustService.execute(commArea)` is called, then `commArea.getCommSuccess()` equals `'Y'`, `commArea.getCommFailCode()` equals `' '`, and `commArea.getCommEyecatcher()` equals `"CUST"`.
- Given `validateTitle()` sets `commFailCode='T'` and `commSuccess='N'`, when `execute()` is called, then execution returns after step 1 and no other service method (`validateDateOfBirth`, `populateTimeAndDate`, `performCreditCheck`, `enqueue`, `getAndIncrementCustomerNumber`, `insertCustomer`, `insertProctran`, `dequeue`) is invoked.
- Given `validateDateOfBirth()` sets `commFailCode='Z'`, when `execute()` is called, then execution returns after step 2 and steps 3–10 are not invoked.
- Given `performCreditCheck()` sets a credit-check fail-code, when `execute()` is called, then `enqueue()` is not called.
- Given `enqueue()` sets `commFailCode='3'`, when `execute()` is called, then `getAndIncrementCustomerNumber()` is not called.
- Given `getAndIncrementCustomerNumber()` sets `commFailCode='4'`, when `execute()` is called, then `insertCustomer()` is not called.
- Given `insertCustomer()` sets `commFailCode='1'`, when `execute()` is called, then `insertProctran()` is not called.
- Given `CrecustService` is instantiated, then it has no mutable instance fields beyond the six final service references (stateless per NFR-5).
- Given the notifying-abort path is triggered (PROCTRAN failure throws `CrecustException`), then `customerNumberService.dequeue()` is called before `abndprocDelegate.linkAbndproc()`, and `Task.getTask().abend(CrecustException.ABEND_CODE_HWPT)` is called last (Rule 1 ordering).
- Given `CrecustService` is compiled, then it is in package `com.ibm.cics.botz.crecust.service` and contains a `private static final Logger LOGGER` field (SLF4J).

## Implementation Notes

Java source files belong under crecust-java/src/main/java/

## Dev Notes

**Implemented:** 2026-10-09

### Files created / modified

| File | Action |
|---|---|
| `crecust-java/src/main/java/com/ibm/cics/botz/crecust/service/CrecustService.java` | **Replaced stub** — full orchestrator with 10-step PREMIERE_P010 flow, 6-service constructor injection, SLF4J logger, notifying-abort path |
| `crecust-java/src/main/java/com/ibm/cics/botz/crecust/service/ValidationService.java` | **Created stub** — `validateTitle()`, `validateDateOfBirth()` |
| `crecust-java/src/main/java/com/ibm/cics/botz/crecust/service/CreditCheckService.java` | **Created stub** — `performCreditCheck()` |
| `crecust-java/src/main/java/com/ibm/cics/botz/crecust/service/CustomerNumberService.java` | **Created stub** — `enqueue()`, `getAndIncrementCustomerNumber()`, `dequeue()` |
| `crecust-java/src/main/java/com/ibm/cics/botz/crecust/service/CustomerDbService.java` | **Created stub** — `insertCustomer()` |
| `crecust-java/src/main/java/com/ibm/cics/botz/crecust/service/ProctranDbService.java` | **Created stub** — `insertProctran()` with `CrecustException` throws clause |
| `crecust-java/src/main/java/com/ibm/cics/botz/crecust/service/AbndprocDelegate.java` | **Created stub** — `linkAbndproc(AbndInfoRec)` |
| `crecust-java/src/main/java/com/ibm/cics/botz/crecust/exception/CrecustException.java` | **Added** `ABEND_CODE_HWPT = "HWPT"` constant (required by `CrecustService`) |
| `crecust-java/src/main/java/com/ibm/cics/botz/crecust/Crecust.java` | **Updated** `service` field initialisation to pass 6 constructor dependencies |

### Key implementation decisions

- **Short-circuit guard**: uses `!"Y".equals(commArea.getCommSuccess())` — `commSuccess` is `String` in the model; no `char` comparisons.
- **Notifying-abort ordering (Rule 1)**: `setAbndCode` + `setAbndFreeform` → `dequeue()` → `linkAbndproc()` → `Task.getTask().abend("HWPT")` — this order is verbatim from story AC.
- **No PII in logs**: DOB fail-code logged as `[redacted]`; fail-codes for other steps are logged (they are control codes, not customer data).
- **`HostProctranRow` unused in `execute()`**: `ProctranDbService` is responsible for assembling the row internally; the orchestrator passes only scalar `String` values (sortcode, custno, name, dob).
- **`Crecust.java` inline instantiation**: services instantiated inline in field declaration as the simplest wiring that compiles; a DI framework can replace this in a future story.

### Compile verification

`mvn compile` → **BUILD SUCCESS** (47 source files, 0 errors, Java 21).

## Spec Change Log

## Review Triage Log

## Design Notes

The `execute()` method mirrors the COBOL PREMIERE_P010 paragraph's linear control flow. Each step is a guarded call: after each service call the method checks whether a fail-code has been set and returns early if so. This maps directly to COBOL's `IF COMM-SUCCESS NOT = 'Y' GO TO GET-ME-OUT-OF-HERE` pattern.

The notifying-abort path (PROCTRAN failure) is the sole non-silent-return error path. It must never be collapsed into the same catch block as any silent-return path (Rule 1). The ordering — populate `AbndInfoRec`, call `dequeue`, call `linkAbndproc`, call `abend` — must be preserved verbatim.

`populateTimeAndDate()` and `populateCicsVersion()` are declared as empty private stubs in this story so that Stories 3.4 and 3.5 can be implemented as targeted additions without restructuring `execute()`.

Example short-circuit guard pattern:
```java
validationService.validateTitle(commArea);
if (commArea.getCommSuccess() != 'Y') { return; }

validationService.validateDateOfBirth(commArea);
if (commArea.getCommSuccess() != 'Y') { return; }

populateTimeAndDate();
// ... continue
```

## Verification

**Commands:**
- `mvn compile -pl crecust` -- expected: BUILD SUCCESS with no compilation errors
- `mvn test -pl crecust -Dtest=CrecustServiceTest` -- expected: all acceptance-criteria tests pass
