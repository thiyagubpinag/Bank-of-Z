---
title: 'Story 2.2: Cat 3a REDEFINES Groups — Record Classes With PIC Getter/Setter'
type: 'feature'
created: '2026-10-01'
status: 'draft'
route: 'dispatch'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The six Cat 3a REDEFINES groups in CRECUST expose a dual-view contract — a canonical PIC field (the anchor) and a structured record sibling that splits the same bytes into named sub-fields. Without Java equivalents, service classes that need both views (e.g., extracting `year/month/day` from a packed `int` date) are forced into raw arithmetic inlined throughout the codebase.

**Approach:** Create one standalone Java record class per Cat 3a group, and add a paired `get<GrpClass>()` / `set<GrpClass>(GrpClass)` method onto the parent class that already holds the canonical PIC field. The conversion is pure Java arithmetic or string operations — no `byte[]` manipulation.

## Boundaries & Constraints

**Always:**
- Each record class is a standalone `.java` file in `com.ibm.cics.botz.crecust.model` (Rule 17).
- The parent class retains the canonical PIC field (typed as declared in Story 2.1). The getter derives the structured view from that field; the setter writes back to it.
- FILLER separator fields within a record group (e.g., the `/` chars in `WsOrigDateGrp`) are modelled as `private static final` constants — not as settable Java fields.
- `WsOrigDateGrp` separator constants hold the character value `'/'` (FORMATTIME DATESEP default).
- Field types follow the PIC clause: `PIC 9(N)` numeric sub-fields → `int`; `PIC X(N)` string sub-fields → `String`; `PIC X(16)` byte-array sub-fields → `byte[]`.
- `CustomerKy2` sub-fields (`requiredSortCode2` and `requiredCustNumber2`) derive from the record sibling's PIC clauses: `9(6)` → `int` and `9(10)` → `int` respectively. The parent canonical field is `customerKy2Bytes byte[]` (16 bytes).
- `WsCicstsLevelNumGrp` sub-fields `wsCicstsLevelVv`, `wsCicstsLevelRr`, `wsCicstsLevelMm` are each `int` (`PIC 99`).
- Lombok `@Data @NoArgsConstructor @AllArgsConstructor @Builder(toBuilder=true)` on every record class (ADR-12). `@Builder.Default` on fields with initializers.
- The getter/setter pair is hand-written (not Lombok-generated) because the conversion logic cannot be expressed as a simple field assignment.

**Never:**
- Do not use `byte[]` as the intermediate representation for Cat 3a conversion — this is Pattern B (Cat 4a) territory.
- Do not add new fields to the parent classes (`ProctranData`, `CrecustService` locals, etc.) beyond the getter/setter pair; the canonical PIC field already exists from Story 2.1.
- Do not generate an inner class — each record class is a top-level type in its own file.
- Do not create record classes for Cat 4a groups (`ProcTranDesc*`, `ProcTranEyeCatcher*`) — those are Stories 2.3 and 2.4.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| `ProcTranDateGrp` round-trip | `procTranDate = 20261001` (int) | `getProcTranDateGrp()` → `year=2026, month=10, day=1`; `setProcTranDateGrp(grp)` → `procTranDate=20261001` | N/A |
| `ProcTranTimeGrp` round-trip | `procTranTime = 143055` (int) | `getProcTranTimeGrp()` → `hours=14, mins=30, secs=55`; setter restores original `int` | N/A |
| `WsTimeNowGrp` round-trip | `wsTimeNow = 093000` (int) | `getWsTimeNowGrp()` → `hh=9, mm=30, ss=0`; setter restores original `int` | N/A |
| `WsOrigDateGrp` round-trip | `wsOrigDate = "01/10/2026"` (String) | `getWsOrigDateGrp()` → `dd=1, mm=10, yyyy=2026`; separator constants = `'/'`; setter restores `"01/10/2026"` | N/A |
| `CustomerKy2` round-trip | `customerKy2Bytes = 16-byte array encoding sortCode=987654, custNumber=0000001234` | `getCustomerKy2()` → `requiredSortCode2=987654, requiredCustNumber2=1234`; setter restores original bytes | N/A |
| `WsCicstsLevelNumGrp` round-trip | `wsCicstslevel = "550000"` (String) | `getWsCicstsLevelNumGrp()` → `vv=55, rr=00, mm=00`; setter restores `"550000"` | N/A |

</frozen-after-approval>

## Code Map

- `_bmad-output/planning-artifacts/CRECUST/architecture.md` lines 472–481 — Cat 3a group table (anchor / record class / PIC field / parent class)
- `_bmad-output/planning-artifacts/CRECUST/technical-research.md` lines 153–156 — `PROC-TRAN-DATE` and `PROC-TRAN-TIME` PIC clauses (`9(8)` 4 bytes; `9(6)` 3 bytes)
- `_bmad-output/planning-artifacts/CRECUST/technical-research.md` lines 174–179 — `WS-TIME-DATA` / `WS-TIME-NOW-GRP` layout
- `_bmad-output/planning-artifacts/CRECUST/technical-research.md` lines 239–267 — `WS-ORIG-DATE-GRP` (DD/sep/MM/sep/YYYY, 10 bytes) and `CUSTOMER-KY2-BYTES` (16 bytes)
- `_bmad-output/planning-artifacts/CRECUST/technical-research.md` lines 304–309 — `WS-CICSTS-LEVEL-NUM-GRP` (VV/RR/MM, 6 bytes)
- `_bmad-output/planning-artifacts/CRECUST/redefines-classification.md` — confirms all six as Cat 3a
- Story 2.1 spec (when written) — defines `ProctranData` with `procTranDate int` and `procTranTime int`; defines base fields these getters/setters operate on
- `com.ibm.cics.botz.crecust.model` package — destination for all six new classes
- Parent classes that receive getter/setter pairs: `ProctranData` (2 pairs), `CrecustService` (3 pairs as local-variable helpers or inner static helpers), `CustomerNumberService` (1 pair)

## Tasks & Acceptance

**Execution:**

- [ ] `src/main/java/com/ibm/cics/botz/crecust/model/ProcTranDateGrp.java` — create new class with fields `year int`, `month int`, `day int`; `@Data @NoArgsConstructor @AllArgsConstructor @Builder(toBuilder=true)`; conversion formula: `year = procTranDate / 10000`, `month = (procTranDate / 100) % 100`, `day = procTranDate % 100`; inverse: `procTranDate = year * 10000 + month * 100 + day`
- [ ] `src/main/java/com/ibm/cics/botz/crecust/model/ProcTranTimeGrp.java` — create new class with fields `hours int`, `mins int`, `secs int`; conversion formula: `hours = procTranTime / 10000`, `mins = (procTranTime / 100) % 100`, `secs = procTranTime % 100`; inverse: `procTranTime = hours * 10000 + mins * 100 + secs`
- [ ] `src/main/java/com/ibm/cics/botz/crecust/model/ProctranData.java` — add `getProcTranDateGrp()` and `setProcTranDateGrp(ProcTranDateGrp)` hand-written methods; add `getProcTranTimeGrp()` and `setProcTranTimeGrp(ProcTranTimeGrp)` hand-written methods; these operate on the existing `procTranDate int` and `procTranTime int` fields respectively
- [ ] `src/main/java/com/ibm/cics/botz/crecust/model/WsTimeNowGrp.java` — create new class with fields `hh int`, `mm int`, `ss int`; same HHMMSS split/merge arithmetic as `ProcTranTimeGrp`
- [ ] `src/main/java/com/ibm/cics/botz/crecust/model/WsOrigDateGrp.java` — create new class with fields `dd int`, `mm int`, `yyyy int`; two `private static final char SEPARATOR = '/'` constants (one between DD and MM, one between MM and YYYY); getter parses `wsOrigDate String` as `substring(0,2)`, `substring(3,5)`, `substring(6,10)` converting to int; setter formats `String.format("%02d/%02d/%04d", dd, mm, yyyy)`
- [ ] `src/main/java/com/ibm/cics/botz/crecust/model/CustomerKy2.java` — create new class with fields `requiredSortCode2 int` (6 digits), `requiredCustNumber2 int` (10 digits); getter decodes `customerKy2Bytes byte[]` (16 bytes, EBCDIC `9(6)` + `9(10)` numeric characters → int); setter encodes back to 16-byte array; use JZOS `ExternalDecimalAsIntField` / `ExternalDecimalAsLongField` via `CobolDatatypeFactory` for encode/decode
- [ ] `src/main/java/com/ibm/cics/botz/crecust/model/WsCicstsLevelNumGrp.java` — create new class with fields `wsCicstsLevelVv int`, `wsCicstsLevelRr int`, `wsCicstsLevelMm int`; getter parses `wsCicstslevel String` (6 chars) as three consecutive 2-digit substrings → int; setter formats back via `String.format("%02d%02d%02d", vv, rr, mm)`

**Note on parent class location for `WsTimeNowGrp`, `WsOrigDateGrp`, `WsCicstsLevelNumGrp`, `CustomerKy2`:** The architecture lists these as "local in `CrecustService`" or "local in `CustomerNumberService`". The record classes themselves go into `com.ibm.cics.botz.crecust.model`; the paired getter/setter methods that perform the conversion are added to the class that holds the canonical PIC field. If those fields are instance fields of `CrecustService` or `CustomerNumberService` (which are service classes, not model classes), the conversion methods are placed there. Confirm field placement against Story 2.1 output before editing those service classes.

**Acceptance Criteria:**

- Given `procTranDate` is set to `20261001`, when `getProcTranDateGrp()` is called on the owning object, then the returned `ProcTranDateGrp` has `year=2026`, `month=10`, `day=1`.
- Given a `ProcTranDateGrp` with `year=2026`, `month=10`, `day=1`, when `setProcTranDateGrp(grp)` is called, then the canonical `procTranDate` field equals `20261001`.
- Given `procTranTime` is set to `143055`, when `getProcTranTimeGrp()` is called, then `hours=14`, `mins=30`, `secs=55`; and `setProcTranTimeGrp(grp)` restores `143055`.
- Given `wsTimeNow` is set to `093000`, when `getWsTimeNowGrp()` is called, then `hh=9`, `mm=30`, `ss=0`; setter round-trip restores the original value.
- Given `wsOrigDate = "01/10/2026"`, when `getWsOrigDateGrp()` is called, then `dd=1`, `mm=10`, `yyyy=2026`. The two SEPARATOR constants in `WsOrigDateGrp` equal `'/'`. Calling `setWsOrigDateGrp(grp)` restores `"01/10/2026"`.
- Given `wsCicstslevel = "550000"`, when `getWsCicstsLevelNumGrp()` is called, then `vv=55`, `rr=0`, `mm=0`; setter restores `"550000"`.
- Given `customerKy2Bytes` is a 16-byte EBCDIC-encoded array holding sortCode=987654, custNumber=1234, when `getCustomerKy2()` is called, then `requiredSortCode2=987654` and `requiredCustNumber2=1234`; setter restores the original 16-byte array.
- All six record classes are in `com.ibm.cics.botz.crecust.model`, each in its own `.java` file (Rule 17).
- No record class contains a `byte[]` instance field (Cat 3a pattern — no shared-memory backing store).
- FILLER separator fields in `WsOrigDateGrp` are `private static final` constants, not Lombok-managed instance fields. No `setFiller*` method is generated for them.
- `mvn compile` passes with no errors after all six classes and their parent-class methods are added.

## Implementation Notes

### Dev Notes (2026-10-09)

All 6 Cat 3a record classes created in `com.ibm.cics.botz.crecust.model`. `ProctranData` modified.

**Files created:**
- `ProcTranDateGrp.java` — `year/month/day int`; YYYYMMDD arithmetic.
- `ProcTranTimeGrp.java` — `hours/mins/secs int`; HHMMSS arithmetic.
- `WsTimeNowGrp.java` — `hh/mm/ss int`; same HHMMSS arithmetic. TODO comment: getter/setter pair goes on `CrecustService` (not yet generated).
- `WsOrigDateGrp.java` — `dd/mm/yyyy int` + `public static final char SEP = '/'`. TODO comment: getter/setter pair goes on `CrecustService`.
- `CustomerKy2.java` — `requiredSortCode2 int` + `requiredCustNumber2 int`; JZOS `ExternalDecimalAsIntField`/`ExternalDecimalAsLongField` static descriptors via `CobolDatatypeFactory` static initializer block. TODO comment: getter/setter pair goes on `CustomerNumberService`.
- `WsCicstsLevelNumGrp.java` — `wsCicstsLevelVv/Rr/Mm int`; 2-char substring parse. TODO comment: getter/setter pair goes on `CrecustService`.

**Files modified:**
- `ProctranData.java` — `procTranDate` and `procTranTime` changed from `String` to `int` (PIC 9(8)/9(6) are numeric display, map to `int` per story AC). Added hand-written `getProcTranDateGrp()`/`setProcTranDateGrp(ProcTranDateGrp)` and `getProcTranTimeGrp()`/`setProcTranTimeGrp(ProcTranTimeGrp)` methods.

**JZOS API note:** `CobolDatatypeFactory` methods are instance methods. `CustomerKy2` uses a `static {}` initializer block to create a factory instance, call `setOffset(0)`, obtain `SORT_CODE_FIELD`, `incrementOffset(SORT_CODE_LENGTH)`, then obtain `CUST_NUMBER_FIELD`.

**Validation:** `mvn compile` (crecust-java) → `BUILD SUCCESS`, 0 errors, 22 source files compiled.

## Spec Change Log

## Review Triage Log

## Design Notes

**PIC type mapping for Cat 3a sub-fields:**

| COBOL PIC | Java type | Notes |
|---|---|---|
| `99`, `999`, `9999`, `9(6)`, `9(8)`, `9(10)` | `int` | Numeric display digits; fits in `int` for all sizes present |
| `X(N)` | `String` | Alphanumeric |
| `X(16)` (anchor `CUSTOMER-KY2`) | `byte[]` | Byte-array anchor; getter/setter use JZOS ExternalDecimal fields |

**Getter/setter placement contract:** The six record classes are pure data holders with no knowledge of their parent. The parent class owns the conversion; the record class exposes only its fields and Lombok-generated boilerplate. This means service classes can construct a `ProcTranDateGrp(2026, 10, 1)` directly and pass it to the setter without importing the parent model.

**WsOrigDateGrp separator handling:** `WS-ORIG-DATE-GRP` in COBOL has two FILLER `X` positions whose runtime value is the DATESEP character (`/` by default from FORMATTIME). The Java model treats these as fixed constants rather than variable fields, since the separator is injected by CICS FORMATTIME and never written by the application.

**`CustomerKy2` encoding:** The 16-byte anchor `CUSTOMER-KY2-BYTES` is an EBCDIC external decimal representation of two consecutive numeric fields (`9(6)` + `9(10)`). The getter must decode using JZOS `ExternalDecimalAsIntField` / `ExternalDecimalAsLongField` at the correct offsets (0 and 6 respectively); the setter must encode back. This is the only Cat 3a group that requires JZOS field classes — the remaining five use pure Java arithmetic or `String` operations.

## Verification

**Commands:**
- `mvn compile -pl crecust` — expected: `BUILD SUCCESS`, zero compilation errors
- `mvn test -pl crecust -Dtest=ProcTranDateGrpTest,ProcTranTimeGrpTest,WsTimeNowGrpTest,WsOrigDateGrpTest,CustomerKy2Test,WsCicstsLevelNumGrpTest` — expected: all round-trip tests pass (if unit test classes are authored as part of this story)
