---
title: 'Story 2.1: Core Model Classes And Lombok Annotations'
type: 'feature'
created: '2026-10-01'
status: 'draft'
route: 'dispatch'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** No Java model classes exist for CRECUST yet. Every subsequent service epic (validation, credit check, customer number, DB2 writes, ABEND path) needs typed, builder-enabled data objects before any logic can be written.

**Approach:** Generate the seven primary model classes (in `com.ibm.cics.botz.crecust.model`) and the three DB2 host-variable row classes (in `com.ibm.cics.botz.crecust.db`), each in its own `.java` file, annotated with the standard Lombok set defined in ADR-12. Three of the model classes also implement `ByteArraySerializable<T>` from the shared library. No serializer logic, no REDEFINES classes, no service code — those belong to Stories 2.2–2.5 and later epics.

## Boundaries & Constraints

**Always:**
- One top-level type per `.java` file (Rule 17).
- Every class in the task list must be generated — including `CustomerControlRecord`, which CRECUST's PROCEDURE DIVISION does not call directly but is a shared contract for the suite (Rule 14).
- Concrete model classes get `@Data @Builder(toBuilder=true) @AllArgsConstructor @NoArgsConstructor` (ADR-12). `@EqualsAndHashCode(callSuper=true)` is added only when a class extends another.
- Non-static fields with inline initializers get `@Builder.Default`.
- FILLER fields (byte arrays for COBOL padding in `CustomerControlRecord`) must be `@Setter(AccessLevel.NONE) private final byte[] fillerN = new byte[N]`.
- `CrecustCommarea`, `WsChildData`, and `AbndInfoRec` implement `ByteArraySerializable<T>`. Each must declare a hand-written `serializer()` method returning the static singleton of its paired serializer class (those serializer classes are created in Story 2.5 — use a forward reference or stub only if absolutely needed to compile; otherwise leave the method body as a `// TODO Story 2.5` placeholder that still compiles).
- All three date fields in `HostCustomerRow` (`hvCustomerDob`, `hvCustomerCreateDate`, `hvCustomerCsReviewDate`) are `int` (YYYYMMDD encoding). `hvCustomerCreditScore` is `short`.
- `hvProctranDate` in `HostProctranRow` is `String` (DD.MM.YYYY). `hvProctranAmount` is `BigDecimal`.
- Infrastructure classes (`ByteArraySerializer`, `ByteArraySerializable`, `Settable`, `Lists`) are already on the classpath from Story 1.2 — do not regenerate them.
- Package: model classes → `com.ibm.cics.botz.crecust.model`; DB2 rows → `com.ibm.cics.botz.crecust.db`.
- Java 21; `org.projectlombok:lombok` is already in `pom.xml` (Story 1.1).

**Never:**
- Do not generate serializer classes here (those belong to Story 2.5).
- Do not generate REDEFINES classes (`ProcTranDateGrp`, `ProcTranDescBase`, etc.) — those belong to Stories 2.2–2.4.
- Do not generate service or entry-point classes.
- Do not create or modify `pom.xml`.
- Do not place multiple top-level types in a single `.java` file.
- Do not add `@EqualsAndHashCode(callSuper=true)` to classes that do not extend another class.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Compile clean | All 10 `.java` files present with correct annotations | `mvn compile` succeeds, zero errors | N/A |
| Field count — `CrecustCommarea` | Class inspected at runtime | 25 fields, total serialized width 399 bytes | — |
| Field count — `CustomerRecord` | Class inspected at runtime | 23 fields, total serialized width 397 bytes | — |
| Field count — `WsChildData` | Class inspected at runtime | Mirror of `CustomerRecord` 397 bytes + `wsChildSuccess` (String 1) + `wsChildFailCode` (String 1) = 399 bytes | — |
| Field count — `AbndInfoRec` | Class inspected at runtime | 12 fields, total serialized width ≈673 bytes | — |
| Field count — `HostCustomerRow` | Class inspected | 17 fields; `hvCustomerDob`, `hvCustomerCreateDate`, `hvCustomerCsReviewDate` are `int`; `hvCustomerCreditScore` is `short` | — |
| FILLER in `CustomerControlRecord` | Class inspected | All FILLER fields are `final byte[]`, have `@Setter(AccessLevel.NONE)`, no setter generated | — |

</frozen-after-approval>

## Code Map

- `crecust-java/src/main/java/com/ibm/cics/botz/crecust/model/` — target package for all 7 model classes; does not exist yet, must be created
- `crecust-java/src/main/java/com/ibm/cics/botz/crecust/db/` — target package for all 3 DB2 host-variable row classes; does not exist yet
- `_bmad-output/planning-artifacts/CRECUST/technical-research.md` — authoritative field-by-field tables for all structures (lines 47–350); field names, PIC clauses, byte widths, and notes derived from here
- `_bmad-output/planning-artifacts/CRECUST/architecture.md` (§6.1, §ADR-12) — class list, byte widths, Lombok annotation set, `ByteArraySerializable` implementors
- `_bmad-output/planning-artifacts/CRECUST/epics.md` (Story 2.1, lines 249–283) — acceptance criteria and source reference
- `creacc-java/src/main/java/com/ibm/bankofz/creacc/infrastructure/ByteArraySerializable.java` — reference implementation of the `ByteArraySerializable<T>` interface (shared library pattern; do not copy to CRECUST, just import)
- `creacc-java/src/main/java/com/ibm/bankofz/creacc/infrastructure/Settable.java` — reference `Settable<T>` interface

## Tasks & Acceptance

**Execution:**

- [x] `crecust-java/src/main/java/com/ibm/cics/botz/crecust/model/CrecustCommarea.java` — Create class with 25 fields matching DFHCOMMAREA / CRECUST.cpy (technical-research lines 352–382); annotate `@Data @Builder(toBuilder=true) @AllArgsConstructor @NoArgsConstructor`; implement `ByteArraySerializable<CrecustCommarea>`; add `serializer()` stub (TODO Story 2.5)
- [x] `crecust-java/src/main/java/com/ibm/cics/botz/crecust/model/CustomerRecord.java` — Create class with 23 fields matching CUSTOMER.cpy (technical-research lines 198–226); annotate `@Data @Builder(toBuilder=true) @AllArgsConstructor @NoArgsConstructor`; does NOT implement `ByteArraySerializable`
- [x] `crecust-java/src/main/java/com/ibm/cics/botz/crecust/model/ProctranData.java` — Create class with core PROCTRAN fields plus `byte[] procTranDesc` (40 bytes) matching PROCTRAN.cpy root record (technical-research lines 143–165); annotate `@Data @Builder(toBuilder=true) @AllArgsConstructor @NoArgsConstructor`; does NOT implement `ByteArraySerializable`
- [x] `crecust-java/src/main/java/com/ibm/cics/botz/crecust/model/WsChildData.java` — Create class: mirror `CustomerRecord`'s 23 fields (397 bytes) plus `wsChildSuccess` (String, 1 byte) and `wsChildFailCode` (String, 1 byte) = 399 bytes total (technical-research lines 293–302); annotate `@Data @Builder(toBuilder=true) @AllArgsConstructor @NoArgsConstructor`; implement `ByteArraySerializable<WsChildData>`; add `serializer()` stub (TODO Story 2.5)
- [x] `crecust-java/src/main/java/com/ibm/cics/botz/crecust/model/AbndInfoRec.java` — Create class with 12 fields matching ABNDINFO.cpy (technical-research lines 181–196); annotate `@Data @Builder(toBuilder=true) @AllArgsConstructor @NoArgsConstructor`; implement `ByteArraySerializable<AbndInfoRec>`; add `serializer()` stub (TODO Story 2.5)
- [x] `crecust-java/src/main/java/com/ibm/cics/botz/crecust/model/CustomerControlRecord.java` — Create class matching CUSTCTRL.cpy (technical-research lines 337–348); annotate `@Data @Builder(toBuilder=true) @AllArgsConstructor @NoArgsConstructor`; declare FILLER fields as `@Setter(AccessLevel.NONE) private final byte[] fillerN = new byte[N]` per Lombok FILLER rules; does NOT implement `ByteArraySerializable`
- [x] `crecust-java/src/main/java/com/ibm/cics/botz/crecust/model/NcsCustNoStuff.java` — Create class with 6 fields matching NCS-CUST-NO-STUFF (technical-research lines 228–237): `ncsCustNoActName` (String 9), `ncsCustNoTestSort` (String 6), `ncsCustNoFill` (String 2), `ncsCustNoInc` (long), `ncsCustNoValue` (long), `ncsCustNoResp` (String 2); annotate `@Data @Builder(toBuilder=true) @AllArgsConstructor @NoArgsConstructor`
- [x] `crecust-java/src/main/java/com/ibm/cics/botz/crecust/db/HostCustomerRow.java` — Create DB2 host-variable row class with 17 fields (technical-research lines 47–67); `hvCustomerDob`, `hvCustomerCreateDate`, `hvCustomerCsReviewDate` → `int`; `hvCustomerCreditScore` → `short`; all other fields → `String`; annotate `@Data @Builder(toBuilder=true) @AllArgsConstructor @NoArgsConstructor`
- [x] `crecust-java/src/main/java/com/ibm/cics/botz/crecust/db/HostProctranRow.java` — Create DB2 host-variable row class with 9 fields (technical-research lines 69–81); `hvProctranDate` → `String` (DD.MM.YYYY); `hvProctranAmount` → `BigDecimal`; all other fields → `String`; annotate `@Data @Builder(toBuilder=true) @AllArgsConstructor @NoArgsConstructor`
- [x] `crecust-java/src/main/java/com/ibm/cics/botz/crecust/db/HostControlRow.java` — Create DB2 host-variable row class with 3 fields (technical-research lines 83–89): `hvControlName` (String), `hvControlValueNum` (int), `hvControlValueStr` (String); annotate `@Data @Builder(toBuilder=true) @AllArgsConstructor @NoArgsConstructor`

**Acceptance Criteria:**

- Given the model classes are authored in package `com.ibm.cics.botz.crecust.model`, when `mvn compile` is run, then all classes compile without error.
- Given each class, when its file is opened, then it contains exactly one top-level type declaration (Rule 17).
- Given `CrecustCommarea`, when its fields are enumerated, then there are exactly 25 fields whose combined serialized width is 399 bytes per the DFHCOMMAREA table in the technical research.
- Given `CustomerRecord`, when its fields are enumerated, then there are exactly 23 fields with combined width 397 bytes per the CUSTOMER.cpy table.
- Given `WsChildData`, when its fields are enumerated, then it mirrors `CustomerRecord`'s 23 fields plus `wsChildSuccess` and `wsChildFailCode`, totalling 399 bytes.
- Given `AbndInfoRec`, when its fields are enumerated, then there are exactly 12 fields per the ABNDINFO.cpy table.
- Given `CustomerControlRecord`, when it is inspected, then all FILLER fields are `private final byte[]` annotated with `@Setter(AccessLevel.NONE)`, and no `setFillerN()` method is generated by Lombok.
- Given `HostCustomerRow`, when its field types are inspected, then `hvCustomerDob`, `hvCustomerCreateDate`, and `hvCustomerCsReviewDate` are `int`, and `hvCustomerCreditScore` is `short`.
- Given `HostProctranRow`, when its field types are inspected, then `hvProctranDate` is `String` and `hvProctranAmount` is `BigDecimal`.
- Given `CrecustCommarea`, `WsChildData`, and `AbndInfoRec`, when their class signatures are inspected, then each implements `ByteArraySerializable<T>` and declares a `serializer()` method.
- Given `CustomerRecord`, `ProctranData`, `CustomerControlRecord`, `NcsCustNoStuff`, and all DB2 row classes, when their class signatures are inspected, then none implements `ByteArraySerializable`.
- Given all 10 classes, when their annotations are inspected, then each has `@Data`, `@Builder(toBuilder=true)`, `@AllArgsConstructor`, and `@NoArgsConstructor`; no class has `@EqualsAndHashCode(callSuper=true)` unless it extends another class (none do in this story).

## Implementation Notes

All 10 classes generated and compiled successfully (`mvn compile` → BUILD SUCCESS, zero errors).

**Model package (`com.ibm.cics.botz.crecust.model`):**

- **`CrecustCommarea`** — 25 fields, 399-byte DFHCOMMAREA layout. Implements `ByteArraySerializable<CrecustCommarea>`; `serializer()` throws `UnsupportedOperationException` with TODO Story 2.5 comment. Explicit `set()` method provided.
- **`CustomerRecord`** — 23 fields, 397-byte CUSTOMER.cpy record. Does not implement `ByteArraySerializable`. All date sub-fields modelled as `String` (day/month/year split per COBOL DISPLAY PIC).
- **`ProctranData`** — 9 fields (8 `String` + 1 `byte[]`). `procTranDesc` stored as `byte[] procTranDesc = new byte[40]` (`@Builder.Default`) to anchor the five Cat 4a shared-memory REDEFINES view classes in Story 2.3.
- **`WsChildData`** — 25 fields (mirrors CustomerRecord 23 + `wsChildSuccess` + `wsChildFailCode`), 399 bytes total. Implements `ByteArraySerializable<WsChildData>`; `serializer()` stub for Story 2.5.
- **`AbndInfoRec`** — 12 fields from ABNDINFO.cpy; `abndUtimeKey` mapped to `long` (S9(15) COMP-3); sign-leading-separate response/SQL code fields kept as `String`. Implements `ByteArraySerializable<AbndInfoRec>`.
- **`CustomerControlRecord`** — 7 named fields + 5 FILLER byte arrays (filler1=38, filler2=160, filler3=4, filler4=3, filler5=4 bytes). All FILLER fields annotated `@Setter(AccessLevel.NONE) private final byte[]` — no setterN() generated.
- **`NcsCustNoStuff`** — 6 fields; `@Builder.Default` applied to `ncsCustNoActName="BANKZCUST"`, `ncsCustNoFill="  "`, `ncsCustNoInc=0L`, `ncsCustNoResp="00"`. `ncsCustNoInc` and `ncsCustNoValue` mapped to `long` (PIC 9(16) COMP exceeds int range).

**DB package (`com.ibm.cics.botz.crecust.db`):**

- **`HostCustomerRow`** — 17 fields; `hvCustomerDob`, `hvCustomerCreateDate`, `hvCustomerCsReviewDate` → `int` (YYYYMMDD, S9(9) COMP INTEGER); `hvCustomerCreditScore` → `short` (S9(4) COMP SMALLINT).
- **`HostProctranRow`** — 9 fields; `hvProctranDate` → `String` (DD.MM.YYYY); `hvProctranAmount` → `BigDecimal` (S9(10)V99 COMP-3 packed decimal).
- **`HostControlRow`** — 3 fields; `hvControlValueNum` → `int` (S9(9) COMP INTEGER).

**Infrastructure classes NOT regenerated** (pre-existing from Story 1.2 in `com.ibm.cics.botz.common`): `ByteArraySerializable`, `ByteArraySerializer`, `Settable`.

**REDEFINES classes NOT generated** (Stories 2.2–2.4): `ProcTranDateGrp`, `ProcTranTimeGrp`, `ProcTranDescBase` subclasses, `ProcTranEyeCatcher` variants, `CaseConditionId` variants.

**Serializer classes NOT generated** (Story 2.5): `CrecustareaSerializer`, `WsChildDataSerializer`, `AbndInfoRecSerializer`.

## Spec Change Log

## Review Triage Log

## Design Notes

**`ByteArraySerializable<T>` stub pattern for `serializer()`:** Because `CrecustareaSerializer`, `WsChildDataSerializer`, and `AbndInfoRecSerializer` do not exist until Story 2.5, the `serializer()` method body cannot reference them. Use a compile-safe placeholder:

```java
@Override
public CrecustareaSerializer serializer() {
    // TODO Story 2.5: return CrecustareaSerializer.INSTANCE;
    throw new UnsupportedOperationException("Serializer not yet implemented — see Story 2.5");
}
```

This satisfies the `ByteArraySerializable<T>` interface contract at compile time and fails fast at runtime until Story 2.5 wires it up.

**`ProctranData.procTranDesc` is a `byte[]`:** The 40-byte PROC-TRAN-DESC field is stored as `byte[] procTranDesc` in `ProctranData` because five Cat 4a REDEFINES groups overlay it (Story 2.3 adds the view classes). Storing it as `byte[]` preserves the shared-memory semantics that Story 2.3 will rely on.

**`NcsCustNoStuff` field types:** `NCS-CUST-NO-INC` and `NCS-CUST-NO-VALUE` are `PIC 9(16) COMP` → `long` in Java (exceeds `int` range). `NCS-CUST-NO-ACT-NAME` has VALUE `'BANKZCUST'` in COBOL — initialize the field: `@Builder.Default private String ncsCustNoActName = "BANKZCUST"`. `NCS-CUST-NO-FILL` VALUE `'  '` → `@Builder.Default private String ncsCustNoFill = "  "`. `NCS-CUST-NO-RESP` VALUE `'00'` → `@Builder.Default private String ncsCustNoResp = "00"`.

## Verification

**Commands:**
- `mvn compile -pl crecust-java` -- expected: `BUILD SUCCESS`, zero compilation errors
