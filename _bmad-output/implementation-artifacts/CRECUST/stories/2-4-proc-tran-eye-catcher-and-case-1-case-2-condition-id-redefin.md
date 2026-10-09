---
title: 'Story 2.4: PROC-TRAN-EYE-CATCHER and CASE-1/CASE-2 CONDITION-ID REDEFINES groups'
type: 'feature'
created: '2026-10-01'
status: 'review'
route: 'dispatch'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The two Cat 4a, Pattern A (mutually exclusive) REDEFINES groups — `PROC-TRAN-EYE-CATCHER` / `PROC-TRAN-LOGICAL-DELETE-AREA` and `CASE-1-CONDITION-ID` / `CASE-2-CONDITION-ID` — have no Java representation. Without them, `ProctranData` has no way to distinguish a valid eye-catcher from a logically-deleted record.

**Approach:** Implement Pattern A (ADR-7) for both groups: a shared interface plus two independent concrete classes per group, each holding its own typed fields and no shared `byte[]`. Wire the new level-88 constants and boolean accessors onto the concrete classes. **`FcConditionToken` is not generated** — CEEDAYS and CEELOCT are replaced by `java.time`, making the CEE condition-token wrapper dead code (Rule 13, G2 resolved 2026-10-01).

## Boundaries & Constraints

**Always:**
- Pattern A only — no shared `byte[]` base class for either group (ADR-7).
- One top-level type per `.java` file (Rule 17).
- Every level-88 condition becomes a `private static final` constant + `isXxx()` boolean getter on its concrete class, following the level-88-handling skill rules.
- `PROC-TRAN-VALID VALUE 'PRTR'` maps to `ProcTranValid.isValid()` checking `eyecatcher.equals("PRTR")`.
- `PROC-TRAN-LOGICALLY-DELETED VALUE X'FF'` maps to `ProcTranLogicalDeleteArea.isLogicallyDeleted()` checking `logicalDeleteFlag == (byte) 0xFF`.
- `Case1ConditionId` fields: `severity` (`short`), `msgNo` (`short`) — both `PIC S9(4) BINARY`.
- `Case2ConditionId` fields: `classCode` (`short`), `causeCode` (`short`) — both `PIC S9(4) BINARY`.
- `FcConditionToken` is **not generated** (G2 resolved 2026-10-01 — dead code, Rule 13).
- Lombok `@Data @Builder @AllArgsConstructor @NoArgsConstructor` on all concrete data classes (ADR-12); interfaces have no Lombok annotations.
- All files in package `com.ibm.cics.botz.crecust.model`.

**Never:**
- Do not create a shared `byte[]` base class for `ProcTranEyeCatcher` or `ConditionId` groups.
- Do not add `ProcTranValid` or `ProcTranLogicalDeleteArea` fields to `ProctranData` — these classes are standalone; callers construct them independently.
- Do not produce serializers in this story (Story 2.5 owns serializers).
- Do not generate `FcConditionToken` in any form — it is removed from scope (G2, Rule 13).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Valid eye-catcher | `new ProcTranValid("PRTR")` | `isValid()` returns `true` | — |
| Invalid eye-catcher | `new ProcTranValid("XXXX")` | `isValid()` returns `false` | — |
| Logically deleted | `new ProcTranLogicalDeleteArea((byte)0xFF, new byte[3])` | `isLogicallyDeleted()` returns `true` | — |
| Not logically deleted | `new ProcTranLogicalDeleteArea((byte)0x00, new byte[3])` | `isLogicallyDeleted()` returns `false` | — |
| CEE success check | `Case1ConditionId` with `msgNo == 0` | `isCee000()` returns `true` (msgNo == 0 signals `CEE000`) | — |

</frozen-after-approval>

## Code Map

- `_bmad-output/planning-artifacts/CRECUST/architecture.md` lines 224–249, 464–493 — ADR-7 decision; class names and field types for both groups
- `_bmad-output/planning-artifacts/CRECUST/technical-research.md` lines 719–735, TRA-1 — REDEFINES classification table confirming Cat 4a for both anchors
- `_bmad-output/planning-artifacts/CRECUST/technical-research.md` lines 335–336 — FC structure: `CONDITION-TOKEN-VALUE` + `I-S-INFO`
- `.bobz/expanded/cobol/src/base/cics/cobol/CRECUST.cbl` lines 217–223 — `PROC-TRAN-EYE-CATCHER` source: `PIC X(4)`, 88 `PROC-TRAN-VALID VALUE 'PRTR'`; `PROC-TRAN-LOGICAL-DELETE-FLAG PIC X`, 88 `PROC-TRAN-LOGICALLY-DELETED VALUE X'FF'`; `FILLER PIC X(3)`
- `.bobz/expanded/cobol/src/base/cics/cobol/CRECUST.cbl` lines 349–361 — `FC` / `CONDITION-TOKEN-VALUE` / `CASE-1-CONDITION-ID` (SEVERITY + MSG-NO) / `CASE-2-CONDITION-ID` REDEFINES (CLASS-CODE + CAUSE-CODE) / `I-S-INFO`
- `_bmad-output/planning-artifacts/CRECUST/epics.md` lines 358–393 — Story 2.4 acceptance criteria (canonical source)
- No existing Java files exist for these classes yet — all files are new.

## Tasks & Acceptance

**Execution:**

- [x] `src/main/java/com/ibm/cics/botz/crecust/model/ProcTranEyeCatcher.java` -- Create interface `ProcTranEyeCatcher` with no methods (marker/common abstraction for ADR-7 Pattern A) -- enables polymorphic reference to either view
- [x] `src/main/java/com/ibm/cics/botz/crecust/model/ProcTranValid.java` -- Create `@Data @Builder @AllArgsConstructor @NoArgsConstructor` class implementing `ProcTranEyeCatcher`; field: `String eyecatcher` (4 bytes); constant `EYECATCHER_VALUE = "PRTR"`; method `isValid()` returning `eyecatcher.equals(EYECATCHER_VALUE)` -- models anchor `PROC-TRAN-EYE-CATCHER` with its level-88
- [x] `src/main/java/com/ibm/cics/botz/crecust/model/ProcTranLogicalDeleteArea.java` -- Create `@Data @Builder @AllArgsConstructor @NoArgsConstructor` class implementing `ProcTranEyeCatcher`; fields: `byte logicalDeleteFlag`, `@Setter(AccessLevel.NONE) final byte[] filler` (3 bytes); constant `LOGICAL_DELETE_FLAG_VALUE = (byte) 0xFF`; method `isLogicallyDeleted()` returning `logicalDeleteFlag == LOGICAL_DELETE_FLAG_VALUE` -- models `PROC-TRAN-LOGICAL-DELETE-AREA` with its level-88
- [x] `src/main/java/com/ibm/cics/botz/crecust/model/ConditionId.java` -- Create interface `ConditionId` with no methods (marker/common abstraction for CASE-1/CASE-2 mutual exclusion) -- enables polymorphic reference within the condition pair
- [x] `src/main/java/com/ibm/cics/botz/crecust/model/Case1ConditionId.java` -- Create `@Data @Builder @AllArgsConstructor @NoArgsConstructor` class implementing `ConditionId`; fields: `short severity`, `short msgNo` (both `PIC S9(4) BINARY`); constant `CEE_000_MSG_NO = (short) 0` and method `isCee000()` returning `msgNo == CEE_000_MSG_NO` -- models `CASE-1-CONDITION-ID`
- [x] `src/main/java/com/ibm/cics/botz/crecust/model/Case2ConditionId.java` -- Create `@Data @Builder @AllArgsConstructor @NoArgsConstructor` class implementing `ConditionId`; fields: `short classCode`, `short causeCode` (both `PIC S9(4) BINARY`) -- models `CASE-2-CONDITION-ID` REDEFINES
- ~~`FcConditionToken.java` — not generated (G2 resolved 2026-10-01: dead code, Rule 13)~~

**Acceptance Criteria:**

- Given `ProcTranEyeCatcher`, `ProcTranValid`, and `ProcTranLogicalDeleteArea` each exist in their own `.java` file in `com.ibm.cics.botz.crecust.model`, when `mvn compile` is run, then all files compile without error.
- Given `new ProcTranValid("PRTR")`, when `isValid()` is called, then it returns `true`; for any other value, it returns `false`.
- Given `new ProcTranLogicalDeleteArea((byte) 0xFF, new byte[3])`, when `isLogicallyDeleted()` is called, then it returns `true`; for `(byte) 0x00` it returns `false`.
- Given `ConditionId`, `Case1ConditionId`, and `Case2ConditionId` each exist in their own `.java` file in `com.ibm.cics.botz.crecust.model`, when `mvn compile` is run, then all files compile without error.
- Given the G2 resolution (2026-10-01), when the model package is reviewed, then no `FcConditionToken.java` file exists in any form — the class is out of scope (Rule 13).
- Given the architecture's class list (section 6.2), when a code review is done, then no shared `byte[]` base class exists for either group (Pattern A confirmed — ADR-7).
- Given `ProcTranValid` and `ProcTranLogicalDeleteArea`, when a code review is done, then no instance fields other than those declared in this spec exist on either class (Rule 17 — no cross-contamination of siblings).
- Given `Case1ConditionId` has `severity` and `msgNo` fields (both `short`), and `Case2ConditionId` has `classCode` and `causeCode` fields (both `short`), when a code review is done, then no field from one class appears in the other class.

## Implementation Notes

- Implemented Group 1 Pattern A (mutually exclusive REDEFINES) in `com.ibm.cics.botz.crecust.model`:
  - `ProcTranEyeCatcher.java`: Marker interface for the group.
  - `ProcTranValid.java`: Concrete class holding independent typed field `String eyecatcher` (4 bytes), constant `EYECATCHER_VALUE = "PRTR"`, and `isValid()` method verifying value equality. Lombok `@Data @Builder(toBuilder = true) @AllArgsConstructor @NoArgsConstructor`.
  - `ProcTranLogicalDeleteArea.java`: Concrete class holding `byte logicalDeleteFlag`, 3-byte `final byte[] filler` with `@Setter(AccessLevel.NONE)`, constant `LOGICAL_DELETE_FLAG_VALUE = (byte) 0xFF`, and `isLogicallyDeleted()` method. Lombok `@Data @Builder(toBuilder = true) @AllArgsConstructor @NoArgsConstructor`.
- Implemented Group 2 Pattern A (mutually exclusive REDEFINES) in `com.ibm.cics.botz.crecust.model`:
  - `ConditionId.java`: Marker interface for the group.
  - `Case1ConditionId.java`: Concrete class holding `short severity` and `short msgNo` (`PIC S9(4) BINARY`), constant `CEE_000_MSG_NO = (short) 0`, and `isCee000()` method. Lombok `@Data @Builder(toBuilder = true) @AllArgsConstructor @NoArgsConstructor`.
  - `Case2ConditionId.java`: Concrete class holding `short classCode` and `short causeCode` (`PIC S9(4) BINARY`). Lombok `@Data @Builder(toBuilder = true) @AllArgsConstructor @NoArgsConstructor`.
- Adhered strictly to Pattern A: No shared `byte[]` base class created for either group.
- `FcConditionToken` excluded in accordance with G2 resolution and Rule 13 (no dead code).
- Verification: Clean Maven compilation (`mvn clean compile` passed successfully), zero shared `byte[] data` across the classes, and exactly 6 new files generated.

## Spec Change Log

## Design Notes

**Why Pattern A (not Pattern B) for these groups:**
`PROC-TRAN-EYE-CATCHER` and `PROC-TRAN-LOGICAL-DELETE-AREA` are never read simultaneously from the same in-flight record — the program either checks for `'PRTR'` or checks for `X'FF'` based on context. Similarly, `CASE-1-CONDITION-ID` and `CASE-2-CONDITION-ID` represent two interpretations of the same 4 bytes selected by an EVALUATE on the condition code type. Pattern A (independent typed fields per sibling) is correct here; Pattern B (shared `byte[]`) would add unnecessary indirection (ADR-7).

> **G2 note (2026-10-01):** `FcConditionToken` is not generated. The original story intent
> included a `FcConditionToken` wrapper for the CEEIGZCT structure because CEEDAYS/CEELOCT
> were anticipated. Because CEEDAYS and CEELOCT are fully replaced by `java.time`, no
> execution path references `FcConditionToken`; generating it would produce dead code (Rule 13).
> `Case1ConditionId` and `Case2ConditionId` stand alone for the REDEFINES pair only.

**`isCee000()` on `Case1ConditionId`:**
`Case1ConditionId` retains the `CEE_000_MSG_NO = (short) 0` constant and `isCee000()` method — these model the CASE-1-CONDITION-ID field semantics independent of the removed CEE runtime wrapper.

## Verification

**Commands:**
- `mvn compile -pl .` -- expected: `BUILD SUCCESS` with zero compile errors
- `grep -r "byte\[\] data" src/main/java/com/ibm/cics/botz/crecust/model/ProcTranValid.java src/main/java/com/ibm/cics/botz/crecust/model/ProcTranLogicalDeleteArea.java src/main/java/com/ibm/cics/botz/crecust/model/Case1ConditionId.java src/main/java/com/ibm/cics/botz/crecust/model/Case2ConditionId.java` -- expected: no output (Pattern A — no shared byte array on any concrete class)
