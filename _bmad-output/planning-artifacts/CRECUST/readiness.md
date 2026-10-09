---
title: "Implementation Readiness Report: CRECUST Java Modernisation"
status: PASS
date: 2026-10-01
updated: 2026-10-01
reviewer: Bob (IBM Z Modernisation)
---

# Implementation Readiness Report — CRECUST Java Modernisation

## Verdict: ✅ PASS — All gap resolutions applied; ready for implementation

The planning artifacts for the CRECUST Java modernisation are **complete and implementable**.
All 18 fidelity rules from the COBOL-to-Java transform rulebook are covered by acceptance
criteria in the PRD. All 11 functional requirements and 6 non-functional requirements trace to
at least one story. All 12 data structures in DS-1 through DS-12 map to classes in the
architecture. The single open-questions section in the architecture is empty.

Four ⚠️ gaps were identified and resolved interactively with the product owner (Arnold) on
2026-10-01. All four gap resolutions have now been applied to the planning artifacts (PRD,
Architecture, Epics, Epic context documents, and Story documents). Implementation may begin.

---

## 1. Artifact Inventory

| Artifact | Path | Status |
|---|---|---|
| PRD | `_bmad-output/planning-artifacts/CRECUST/prd.md` | ✅ Complete |
| Architecture | `_bmad-output/planning-artifacts/CRECUST/architecture.md` | ✅ Complete |
| Epics & Stories | `_bmad-output/planning-artifacts/CRECUST/epics.md` | ✅ Complete |
| TS Metadata | `_bmad-output/planning-artifacts/CRECUST/java-transform-metadata.md` | ✅ Present |
| REDEFINES Classification | `_bmad-output/planning-artifacts/CRECUST/redefines-classification.md` | ✅ Present |

---

## 2. Fidelity Rule Coverage (18 rules)

| Rule | Description | Coverage | Notes |
|---|---|---|---|
| Rule 1 | Error-path ordering | ✅ | PRD AC-1; Architecture §7.2 classifies all 8 paths (7 silent-return, 1 notifying-abort); Stories 8.1/8.2 enforce ordering with ordered ACs |
| Rule 2 | Exception-handler wiring | ⚠️ | AC-2 wording conflicts with ADR-10 — see Gap G1 below |
| Rule 3 | Named exception factories | ✅ | PRD AC-3; Story 9.1 declares 16 factory methods; Story 5.3 uses factories at FETCH ANY sites; no raw literals |
| Rule 4 | CICS ABEND code propagation | ✅ | PRD AC-4; `ABEND_CODE_HWPT = "HWPT"` in `CrecustException`; AC-4.2 grep check in Story 9.1 |
| Rule 5 | Commarea/byte-array mapper layout | ✅ | PRD AC-5; Story 2.5 covers all 3 serializers; named offset constants; bounds checks; no stubs |
| Rule 6 | EIBCALEN partial-copy | ✅ | PRD AC-6; Story 3.3 with unit test for 100-byte commarea |
| Rule 7 | SQL column completeness | ✅ | PRD AC-7; Architecture §8 lists all 17 CUSTOMER + 9 PROCTRAN columns; Stories 7.2 / 8.1 assert column counts |
| Rule 8 | Validation constants | ✅ | PRD AC-8; Stories 4.2, 5.1, 5.4, 6.1 declare named constants for all numeric literals |
| Rule 9 | Copybook sentinel values | ✅ | PRD AC-9; architecture §12 Open Questions = none; all 9 copybooks resolved |
| Rule 10 | Security-record return-code inspection | ✅ | PRD AC-10; ADR-11 documented; `linkAbndproc()` returns `void`; Story 8.2 AC confirms no DTO |
| Rule 11 | Date/time field formats | ✅ | PRD AC-11; 6 sub-criteria covering all date/time formats; Stories 8.1, 7.1, 5.4, 3.4 each assert exact format strings |
| Rule 12 | Credential/security fields | ✅ | PRD AC-12; PE-10 confirmed no credential fields; satisfied by construction |
| Rule 13 | No dead classes | ⚠️ | `FcConditionToken`, `Case1ConditionId`, `Case2ConditionId` in DS-11 have no injection site or execution path — see Gap G2 below |
| Rule 14 | Cross-program data-structure completeness | ✅ | PRD AC-14; `CustomerControlRecord` present even though not exercised by procedure division; `WsChildData` / `CrecustCommarea` / `CustomerRecord` / `ProctranData` all present |
| Rule 15 | Serializer delegation | ✅ | PRD AC-15; Story 2.5 enforces single source of truth for layout constants; Story 5.3 enforces `WsChildDataSerializer` for container reads |
| Rule 16 | Exception-handler COBOL grounding | ✅ | PRD AC-16; ADR-10 provides COBOL grounding map; Story 9.2 documents 5 COBOL constructs mapped to inline catch blocks; no `*ExceptionHandler*` class |
| Rule 17 | One class per file | ✅ | PRD AC-17; NFR-2.3; Stories 2.1–2.5 each call out one-type-per-file for every class |
| Rule 18 | User-facing I/O contract preservation | ✅ | PRD AC-18; no `ACCEPT`/`DISPLAY` in CRECUST; satisfied by construction |

---

## 3. Functional Requirements Traceability

| FR | Description | Stories |
|---|---|---|
| FR-1 | Program Entry and Commarea Binding | 3.1, 3.2, 3.3 |
| FR-2 | Title Validation | 4.1 |
| FR-3 | Date-of-Birth Validation | 4.2 |
| FR-4 | Populate Time and Date | 3.4 |
| FR-5 | Asynchronous Credit-Check | 5.1, 5.2, 5.3 |
| FR-6 | Credit-Score Review Date | 5.4 |
| FR-7 | Customer Number Allocation | 6.1, 6.2, 6.3 |
| FR-8 | Insert Customer Record | 7.1, 7.2 |
| FR-9 | Write PROCTRAN Audit Record | 8.1, 8.2 |
| FR-10 | CICS Level Check | 3.5 |
| FR-11 | Fail Code Inventory | 9.1 |

---

## 4. Non-Functional Requirements Traceability

| NFR | Description | Coverage |
|---|---|---|
| NFR-1 | Java 21 / Maven build | Story 1.1 |
| NFR-2 | Coding standards | Enforced per story in all epics |
| NFR-3 | Exception handling (COBOL grounding, HWPT constant, void delegate) | Stories 9.1, 9.2 |
| NFR-4 | Resource management (try-with-resources, 4 DEQ sites) | Stories 6.3, 8.2 |
| NFR-5 | Stateless design | All service epics (stated in every service story AC) |
| NFR-6 | Byte-array contract (named offset constants, bounds checks, no redeclaration) | Stories 2.3–2.5 |

---

## 5. REDEFINES Coverage

| Group | COBOL Anchor | Category | Architecture Decision | Story |
|---|---|---|---|---|
| `PROC-TRAN-DATE` | `PROC-TRAN-DATE` | Cat 3a | `ProcTranDateGrp` record class + `procTranDate int` getter/setter on `ProctranData` | 2.2 |
| `PROC-TRAN-TIME` | `PROC-TRAN-TIME` | Cat 3a | `ProcTranTimeGrp` record class + `procTranTime int` getter/setter | 2.2 |
| `WS-TIME-NOW` | `WS-TIME-NOW` | Cat 3a | `WsTimeNowGrp` record class + `wsTimeNow int` getter/setter | 2.2 |
| `WS-ORIG-DATE` | `WS-ORIG-DATE` | Cat 3a | `WsOrigDateGrp` record class + `wsOrigDate String` getter/setter | 2.2 |
| `CUSTOMER-KY2` | `CUSTOMER-KY2` | Cat 3a | `CustomerKy2` record class + `customerKy2Bytes byte[]` getter/setter | 2.2 |
| `WS-CICSTSLEVEL` | `WS-CICSTSLEVEL` | Cat 3a | `WsCicstsLevelNumGrp` record class + `wsCicstslevel String` getter/setter | 2.2 |
| `PROC-TRAN-EYE-CATCHER` | `PROC-TRAN-EYE-CATCHER` | Cat 4a Pattern A (classification tool shows all NOs — tool edge case; architecture ADR-7 is authoritative) | `ProcTranEyeCatcher` interface + `ProcTranValid` + `ProcTranLogicalDeleteArea` | 2.4 |
| `PROC-TRAN-DESC` | `PROC-TRAN-DESC` | Cat 4a Pattern B | `ProcTranDescBase` abstract + 5 concrete subclasses (Pattern B — shared `byte[]`) | 2.3 |
| `CASE-1-CONDITION-ID` | `CASE-1-CONDITION-ID` | Cat 4a Pattern A | `ConditionId` interface + `Case1ConditionId` + `Case2ConditionId` | 2.4 |

---

## 6. Data Structure Coverage

| DS | Model Class(es) | Architecture | Story |
|---|---|---|---|
| DS-1 | `CrecustCommarea` (399 B) | §6.1 | 2.1, 2.5 |
| DS-2 | `CustomerRecord` (397 B) | §6.1 | 2.1 |
| DS-3 / 3a / 3b / 3c | `ProctranData`, `ProcTranDescBase`+5 subclasses, `ProcTranDateGrp`, `ProcTranTimeGrp` | §6.1, §6.2 | 2.1, 2.2, 2.3 |
| DS-4 | `HostCustomerRow` | §6.1 | 2.1 |
| DS-5 | `HostProctranRow` | §6.1 | 2.1 |
| DS-6 | `HostControlRow` | §6.1 | 2.1 |
| DS-7 | `WsChildData` (399 B) | §6.1 | 2.1, 2.5 |
| DS-8 | `AbndInfoRec` (≈681 B) | §6.1 | 2.1, 2.5 |
| DS-9 | `CustomerControlRecord` | §6.1 | 2.1 |
| DS-10 | `WsTimeNowGrp`, `WsOrigDateGrp`, `CustomerKy2`, `WsCicstsLevelNumGrp` | §6.2 | 2.2 |
| DS-11 | `FcConditionToken`, `Case1ConditionId`, `Case2ConditionId` — **RESOLVED: remove per G2** | §6.2 | 2.4 (scope reduced) |
| DS-12 | `NcsCustNoStuff` | §6.1 | 2.1 |

---

## 7. Architecture Decisions Coverage

| ADR | Decision | Status |
|---|---|---|
| ADR-1 | Serialization: ADR-A (retain z/OS I/O; JZOS-style) | ✅ Adopted; 3 canonical serializers; shared library |
| ADR-2 | Package structure: layer-first | ✅ Adopted; 6 sub-packages defined |
| ADR-3 | Maven coordinates | ✅ Adopted; groupId/artifactId/version in Story 1.1 |
| ADR-4 | DataSource: JNDI lookup | ✅ Adopted; Story 6.2 / 7.2 / 8.1 confirm no `@Resource` |
| ADR-5 | Async credit-check: native JCICS AsyncService | ✅ Adopted; Epic 5 implements full JCICS async loop |
| ADR-6 | CICS version check: JCICS Task inquiry | ✅ Adopted; Story 3.5 |
| ADR-7 | PROC-TRAN-EYE-CATCHER: Pattern A | ✅ Adopted; Story 2.4 |
| ADR-8 | SYSIDERR retry: not implemented | ✅ Adopted; no retry code anywhere |
| ADR-9 | Storm-drain: out of scope | ✅ Adopted; explicitly deferred |
| ADR-10 | Exception handler COBOL grounding (Rule 16) | ✅ Adopted; Story 9.2 |
| ADR-11 | ABNDPROC return-code not inspected (Rule 10) | ✅ Adopted; `linkAbndproc()` returns `void` |
| ADR-12 | Lombok enabled | ✅ Adopted; Story 2.1 applies all 4 annotations |

---

## 8. Gaps — User Resolutions

### G1 ✅ AC-2 wording contradicts ADR-10 — FIXED

**Observed:** PRD AC-2.1 states "There is exactly one exception-handler class per service. All
catch blocks in the service delegate to this handler." AC-2.2 states "The handler class is
injected." This literal wording would lead a developer to generate an injected
`CrecustExceptionHandler` class — which ADR-10 and Story 9.2 explicitly prohibit.

**User resolution (accepted):** The downstream artifact update must clarify AC-2 to read:
`CrecustException` (accessed via static factory methods and its `failCode` attribute) is the
single delegatee for all error-response construction. There is no separately injected
exception-handler class. Every catch block sets `commFailCode` and `commSuccess` from the
exception's `failCode` attribute, then returns or abends as appropriate.

**Status:** ✅ Fixed. PRD AC-2, PRD NFR-3.1, Architecture §7.1, Epics Story 9.2 AC, Story 9.1
constraints, and Story 9.2 constraints updated to reflect the correct contract.

---

### G2 ✅ `FcConditionToken` / `Case1ConditionId` / `Case2ConditionId` — dead classes (Rule 13) — FIXED

**Observed:** DS-11 declares `FcConditionToken`, `Case1ConditionId`, and `Case2ConditionId` as
Java classes for the CEEIGZCT CEE condition-token structure (`FC → CONDITION-TOKEN-VALUE,
I-S-INFO`). CEEDAYS and CEELOCT are **fully replaced by `java.time`** (Product Owner Decision).
No story in Epics 3–9 references these classes in any execution path, and no injection site
exists for them. Generating these classes would produce dead code in violation of Rule 13.

**User resolution (Resolution B accepted):** Do **not** generate `FcConditionToken`,
`Case1ConditionId`, or `Case2ConditionId`. These classes must be **removed from DS-11, the
architecture class list (§6.2), and Story 2.4**. The `ConditionId` interface and both concrete
classes remain in Story 2.4 only for `CASE-1-CONDITION-ID` / `CASE-2-CONDITION-ID` REDEFINES,
not for the CEE condition-token structure.

**Status:** ✅ Fixed. PRD DS-11, PRD AC-13.2, Architecture §6.2, Epics Story 2.4 AC, epic-2-context,
Story 2.4 (intent, constraints, tasks, ACs, design notes) all updated. `FcConditionToken` removed
from all class lists and task lists.

---

### G3 ✅ Story 9.1 factory-method contract ambiguity — FIXED

**Observed:** Story 9.1 AC says each factory method "sets the corresponding commarea fields
(`commSuccess = 'N'`, `commFailCode = <constant>`) on the commarea passed in." This implies the
exception is a commarea mutator, which conflicts with `CrecustException extends RuntimeException`.

**User resolution:** `CrecustException` factory methods accept a `char failCode` and store it
as an instance attribute (e.g., `private final char failCode`). A getter `getFailCode()` exposes
the value. Each catch block in a service class is responsible for:
1. Catching `CrecustException e`
2. Setting `commarea.setCommFailCode(e.getFailCode())`
3. Setting `commarea.setCommSuccess('N')`
4. Returning (or proceeding to ABEND on the notifying-abort path)

The factory methods **do not mutate the commarea directly**. Story 9.1 AC must be updated to
reflect this contract.

**Status:** ✅ Fixed. PRD NFR-3.1, Architecture §7.1, Epics Story 9.1 AC, epic-9-context
(requirements, technical decisions), Story 9.1 (intent, approach, constraints, I/O matrix, task,
ACs, design notes) all updated. Factory methods no longer accept `CrecustCommarea`; catch blocks
read `e.getFailCode()` instead.

---

### G4 ✅ Story 8.2 ABNDINFO population missing `abndUtimeKey` and `abndTasknoKey` — FIXED

**Observed:** Story 8.2 step 1 lists 8 fields to populate in `AbndInfoRec` but omits:
- `abndUtimeKey` (`ABND-UTIME-KEY`, S9(15) COMP-3, 8 bytes) — populated from JCICS ABSTIME
- `abndTasknoKey` (`ABND-TASKNO-KEY`, PIC 9(4), 4 bytes) — populated from EIBTASKN

These are declared in DS-8 (sourced from ABNDINFO.cpy lines 7–9) and form the `ABND-VSAM-KEY`
group used as the DB key for the ABND record in ABNDPROC.

**User resolution (accepted):** Story 8.2 step 1 must be updated to include:
- `abndInfoRec.abndUtimeKey` ← `Task.getTask().getAbstime()` (JCICS ABSTIME, as `long`)
- `abndInfoRec.abndTasknoKey` ← `Task.getTask().getTaskNumber()` formatted as 4-digit string

**Status:** ✅ Fixed. Epics Story 8.2 AC step 1, epic-8-context Technical Decisions, and
Story 8.2 (task, ACs, design notes) updated to include both fields with their JCICS sources
(`Task.getTask().getAbstime()` and `Task.getTask().getTaskNumber()` formatted as 4-digit string).

---

## 9. Gate Verdict

| Criterion | Result |
|---|---|
| All 18 fidelity rules covered by at least one AC | ✅ |
| All FRs trace to ≥1 story | ✅ |
| All NFRs trace to ≥1 story | ✅ |
| Architecture ADRs complete with no open questions | ✅ |
| No unresolved copybook sentinels (Rule 9) | ✅ |
| No ❌ blockers | ✅ |
| 4 ⚠️ gaps recorded, resolved interactively, and applied to all artifacts | ✅ All gaps closed |

**GATE: PASS — all four gap resolutions (G1–G4) have been applied to every affected artifact
(PRD, Architecture, Epics, Epic context documents, and Story documents). Implementation stories
in Epic 2, Epic 8, and Epic 9 may now be picked up without restriction.**
