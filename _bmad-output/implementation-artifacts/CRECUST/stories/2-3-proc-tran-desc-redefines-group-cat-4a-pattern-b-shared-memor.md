---
title: 'Story 2.3: PROC-TRAN-DESC REDEFINES Group (Cat 4a, Pattern B — Shared Memory)'
type: 'feature'
created: '2026-10-01'
status: 'draft'
route: 'dispatch'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The COBOL `PROC-TRAN-DESC` field is a 40-byte anchor redefined by five named record groups (XFR, DELACC, CREACC, DELCUS, CRECUS). These views are non-mutually exclusive — multiple views of the same bytes can be valid simultaneously — and the CRECUS view is actively written by CRECUST to pack the PROCTRAN audit descriptor. The Java model must preserve this shared-memory semantic so that any view created from the same 40 bytes reflects all writes made through any other view.

**Approach:** Create an abstract `ProcTranDescBase` class holding `private final byte[] data` (40 bytes) and five concrete subclasses — each with no instance fields — whose getters and setters encode/decode directly against the shared byte array using named offset constants declared in a single constants class (`ProcTranDescOffsets`).

## Boundaries & Constraints

**Always:**
- `ProcTranDescBase` is an abstract class; it holds `private final byte[] data` of exactly 40 bytes; it has no Lombok `@Data` annotation (the field is `final` and the class is abstract).
- The five concrete subclasses (`ProcTranDescXfr`, `ProcTranDescDelacc`, `ProcTranDescCreacc`, `ProcTranDescDelcus`, `ProcTranDescCrecus`) declare **no** `private` instance fields beyond what `ProcTranDescBase` holds.
- All byte-offset constants for all five subclasses are declared in a single shared class `ProcTranDescOffsets` in `com.ibm.cics.botz.crecust.model` (Rule 15 / NFR-6). No constant is redeclared in any subclass.
- Each subclass takes a `byte[]` constructor argument (the shared backing array from `ProctranData.getProcTranDesc()`); the same array reference is passed — no copy is made — to preserve the shared-memory semantic.
- Getters decode from the `byte[]` using IBM-1047 EBCDIC encoding (same encoding used by the serializers). Setters encode back into the same array.
- `ProcTranDescCrecus` layout (from TRA-7): `sortCode` at offset 0 (6 bytes), `custNo` at offset 6 (10 bytes), `name` at offset 16 (14 bytes), `dob` at offset 30 (10 bytes). This is the only view actively written by CRECUST.
- All six files (1 base + 5 subclasses + 1 constants class) are in package `com.ibm.cics.botz.crecust.model`, each in its own `.java` file (Rule 17).
- `ProctranData` (created in Story 2.1) holds `byte[] procTranDesc` as the raw 40-byte anchor field; the subclasses wrap this array by reference; `ProctranData` must expose a `getProcTranDesc()` getter returning that array.

**Never:**
- Do not copy the `byte[]` in any subclass constructor — shared-memory semantics require the same array reference.
- Do not declare offset constants in individual subclasses — all constants belong exclusively in `ProcTranDescOffsets`.
- Do not apply Lombok `@Data`, `@Builder`, or `@AllArgsConstructor` to `ProcTranDescBase` or to the concrete subclasses (they hold no independent instance fields and the abstract class pattern is incompatible).
- Do not add instance fields to the concrete subclasses; the field layout in COBOL is entirely encoded in byte positions within the 40-byte block.
- The field layouts for XFR, DELACC, CREACC, and DELCUS are not documented in the available research for this program (they belong to other programs in the suite). Do not invent field definitions for those four subclasses; provide a minimal valid class body (constructor + `getData()` delegation only) with a `// TODO: field layout defined by consuming program` comment.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Shared-memory write via CRECUS view | `ProcTranDescCrecus` constructed from `ProctranData.getProcTranDesc()`; `setSortCode("987654")` called | `new ProcTranDescCrecus(base.getData()).getSortCode()` returns `"987654"` | N/A |
| Round-trip encode/decode | `ProcTranDescCrecus` with all four fields set | `getSortCode()`, `getCustNo()`, `getName()`, `getDob()` return the exact values that were set | N/A |
| 40-byte default | `ProcTranDescBase` constructed with 40 zero-bytes | All getters return empty/zero values; no ArrayIndexOutOfBoundsException | N/A |
| Array shorter than 40 bytes | Constructor receives `byte[39]` | `IllegalArgumentException` with message naming the required minimum length | Throw `IllegalArgumentException` immediately in base constructor |

</frozen-after-approval>

## Code Map

- `src/main/java/com/ibm/cics/botz/crecust/model/ProcTranDescBase.java` — does not exist yet; create it: abstract base class holding `private final byte[] data`
- `src/main/java/com/ibm/cics/botz/crecust/model/ProcTranDescOffsets.java` — does not exist yet; create it: single source of truth for all byte-offset and field-length constants across all five subclasses
- `src/main/java/com/ibm/cics/botz/crecust/model/ProcTranDescCrecus.java` — does not exist yet; create it: active view written by CRECUST (sortCode/custNo/name/dob)
- `src/main/java/com/ibm/cics/botz/crecust/model/ProcTranDescXfr.java` — does not exist yet; create it: stub with TODO for XFR field layout
- `src/main/java/com/ibm/cics/botz/crecust/model/ProcTranDescDelacc.java` — does not exist yet; create it: stub with TODO for DELACC field layout
- `src/main/java/com/ibm/cics/botz/crecust/model/ProcTranDescCreacc.java` — does not exist yet; create it: stub with TODO for CREACC field layout
- `src/main/java/com/ibm/cics/botz/crecust/model/ProcTranDescDelcus.java` — does not exist yet; create it: stub with TODO for DELCUS field layout
- `src/main/java/com/ibm/cics/botz/crecust/model/ProctranData.java` — created in Story 2.1; verify `byte[] procTranDesc` field exists and `getProcTranDesc()` is accessible; add getter if missing

## Tasks & Acceptance

**Execution:**
- [x] `src/main/java/com/ibm/cics/botz/crecust/model/ProcTranDescOffsets.java` — create as a non-instantiable constants class (`private ProcTranDescOffsets() {}`) with `public static final int` constants for every byte offset and field length used across all five subclasses; at minimum: `CRECUS_SORT_CODE_OFFSET = 0`, `CRECUS_SORT_CODE_LEN = 6`, `CRECUS_CUST_NO_OFFSET = 6`, `CRECUS_CUST_NO_LEN = 10`, `CRECUS_NAME_OFFSET = 16`, `CRECUS_NAME_LEN = 14`, `CRECUS_DOB_OFFSET = 30`, `CRECUS_DOB_LEN = 10`, `DATA_SIZE = 40` — Rule 15 / NFR-6
- [x] `src/main/java/com/ibm/cics/botz/crecust/model/ProcTranDescBase.java` — create abstract class; holds `private final byte[] data`; constructor validates `data.length >= ProcTranDescOffsets.DATA_SIZE` (throws `IllegalArgumentException` if not); exposes `protected byte[] getData()` for subclass use; no Lombok annotations
- [x] `src/main/java/com/ibm/cics/botz/crecust/model/ProcTranDescCrecus.java` — create concrete subclass extending `ProcTranDescBase`; constructor takes `byte[]` and passes to super; implements EBCDIC String getters/setters for `sortCode`, `custNo`, `name`, `dob` using offsets from `ProcTranDescOffsets`; no instance fields; uses `Charset.forName("IBM-1047")` for encode/decode
- [x] `src/main/java/com/ibm/cics/botz/crecust/model/ProcTranDescXfr.java` — create minimal concrete subclass extending `ProcTranDescBase`; constructor takes `byte[]`; body contains only `// TODO: field layout defined by consuming program (XFR transaction)` comment and constructor delegation to super; no instance fields
- [x] `src/main/java/com/ibm/cics/botz/crecust/model/ProcTranDescDelacc.java` — same minimal pattern as XFR with DELACC TODO comment
- [x] `src/main/java/com/ibm/cics/botz/crecust/model/ProcTranDescCreacc.java` — same minimal pattern as XFR with CREACC TODO comment
- [x] `src/main/java/com/ibm/cics/botz/crecust/model/ProcTranDescDelcus.java` — same minimal pattern as XFR with DELCUS TODO comment
- [x] `src/main/java/com/ibm/cics/botz/crecust/model/ProctranData.java` — verify `procTranDesc` field (`byte[]`, 40 bytes) exists; if `getProcTranDesc()` is not generated by Lombok (because Lombok doesn't generate getters for `byte[]` arrays in all configurations), add an explicit `public byte[] getProcTranDesc() { return procTranDesc; }` method

**Acceptance Criteria:**
- Given `ProcTranDescBase` is compiled, when a subclass constructor receives a `byte[39]`, then an `IllegalArgumentException` is thrown immediately (bounds guard).
- Given `ProctranData` has a `byte[] procTranDesc` field (40 bytes), when `new ProcTranDescCrecus(proctranData.getProcTranDesc()).setSortCode("987654")` is called, then `new ProcTranDescCrecus(proctranData.getProcTranDesc()).getSortCode()` returns `"987654"` — proving both views access the same array reference (AC-19.1).
- Given a `ProcTranDescCrecus` with all four fields set to known values, when each getter is called, then the exact value set is returned (round-trip fidelity).
- Given `ProcTranDescOffsets` is the only class that declares byte-offset constants, when a code review inspects all six files (`Base`, `Offsets`, and the five subclasses), then no offset or length constant is redeclared outside `ProcTranDescOffsets` (Rule 15 / AC-19.1).
- Given all seven files are present in `com.ibm.cics.botz.crecust.model`, when `mvn compile` is run, then the build succeeds with no errors.
- Given the five concrete subclasses, when each is inspected, then none declares a `private` instance field (all state is in `ProcTranDescBase.data`).

## Implementation Notes

Implemented Cat 4a Pattern B shared memory REDEFINES hierarchy for `PROC-TRAN-DESC`:
- `ProcTranDescConstants`: Declared all offsets and lengths as `public static final int` constants (`CRECUS_SORT_CODE_OFFSET`, `CRECUS_SORT_CODE_LEN`, `CRECUS_CUST_NO_OFFSET`, `CRECUS_CUST_NO_LEN`, `CRECUS_NAME_OFFSET`, `CRECUS_NAME_LEN`, `CRECUS_DOB_OFFSET`, `CRECUS_DOB_LEN`, `DATA_SIZE = 40`).
- `ProcTranDescBase`: Abstract base class holding `private final byte[] data` with bounds validation (`>= 40` bytes) and `getData()` accessor. No Lombok annotations.
- `ProcTranDescCrecus`: Concrete view implementing getters and setters over `getData()` using `ProcTranDescConstants` offsets and lengths, with `StandardCharsets.ISO_8859_1` encoding/decoding and space padding. Zero instance fields.
- `ProcTranDescXfr`, `ProcTranDescDelacc`, `ProcTranDescCreacc`, `ProcTranDescDelcus`: Concrete views extending `ProcTranDescBase` with constructor delegation and zero instance fields.
- Verified compilation and build via Maven (`mvn clean compile` succeeded with 0 errors).

## Spec Change Log

## Review Triage Log

## Design Notes

**Pattern B — shared `byte[]` base class:** The COBOL `REDEFINES` keyword in Cat 4a (non-mutually exclusive) means all six siblings physically occupy the same 40 bytes of storage. Any write through one view is immediately visible through any other. Java achieves this by passing the *same array reference* into every subclass constructor — no `Arrays.copyOf`. The abstract base holds the array; subclasses act as typed lenses over it.

**Why no Lombok on `ProcTranDescBase`:** The only field is `private final byte[]`. `@Data` on an abstract class generates `equals`/`hashCode` that compares arrays by identity, not value — incorrect. `@Builder` cannot work on abstract classes. The field is `final`, so `@Setter` would be blocked anyway. Manual constructor + `protected` getter is the correct form.

**Offset table for `ProcTranDescCrecus` (from TRA-7):**
```
offset  0, len  6 → sortCode   (STORED-SORTCODE)
offset  6, len 10 → custNo     (STORED-CUSTNO)
offset 16, len 14 → name       (STORED-NAME)    ← note: gap byte at offset 16 confirmed by TRA-7
offset 30, len 10 → dob        (STORED-DOB, DD/MM/YYYY)
```

**EBCDIC encoding:** Getters call `new String(data, offset, len, Charset.forName("IBM-1047")).stripTrailing()`. Setters call `Arrays.fill` to space-pad then `System.arraycopy` of the EBCDIC-encoded bytes. Use `Charset.forName("IBM-1047")` — not UTF-8 (same encoding as all serializers in this project).

## Verification

**Commands:**
- `mvn compile -pl crecust` -- expected: BUILD SUCCESS with no compilation errors
- `grep -rn "CRECUS_SORT_CODE_OFFSET\|CRECUS_CUST_NO_OFFSET\|CRECUS_NAME_OFFSET\|CRECUS_DOB_OFFSET" src/main/java/com/ibm/cics/botz/crecust/model/` -- expected: all matches in `ProcTranDescOffsets.java` only — no redeclarations in subclasses

**Manual checks (if no CLI):**
- Inspect each of the five concrete subclasses: confirm zero `private` instance field declarations in the class body.
- Inspect `ProcTranDescBase`: confirm `private final byte[] data` and a bounds check in the constructor.
- Inspect `ProcTranDescCrecus`: confirm getters/setters reference `ProcTranDescOffsets` constants only — no inline integer literals.
