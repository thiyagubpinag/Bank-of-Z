# Epic 2 Context: Data Models and Shared Serialization Contracts

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Author all Java model classes, REDEFINES-derived classes, DB2 host-variable rows, and the three canonical `ByteArraySerializer` classes. This establishes the shared type system that every subsequent service epic depends on. No service class may exist without the model and serializer classes this epic produces.

## Stories

- Story 2.1: Core model classes and Lombok annotations
- Story 2.2: Cat 3a REDEFINES groups — record classes with PIC getter/setter
- Story 2.3: `PROC-TRAN-DESC` REDEFINES group (Cat 4a, Pattern B — shared memory)
- Story 2.4: `PROC-TRAN-EYE-CATCHER` and `CASE-1/CASE-2 CONDITION-ID` REDEFINES groups (Cat 4a, Pattern A — mutually exclusive); `FcConditionToken` is **not generated** (G2)
- Story 2.5: Three canonical `ByteArraySerializer` classes

## Requirements & Constraints

- All model classes live in `com.ibm.cics.botz.crecust.model`; DB2 host-variable rows in `com.ibm.cics.botz.crecust.db`; serializers in `com.ibm.cics.botz.crecust.serializer` (ADR-2).
- Each Java file contains exactly one top-level type (Rule 17).
- Model classes use `@Data @Builder @AllArgsConstructor @NoArgsConstructor` (ADR-12), except abstract classes and interfaces which cannot.
- `ByteArraySerializable<T>`, `ByteArraySerializer<T>`, `Settable<T>`, and `Lists` are pre-existing infrastructure from Story 1.2 — do not regenerate them.
- Every field-offset constant must be declared as a named `private static final` constant; no bare integer literals in byte arithmetic (Rule 5 / NFR-6).
- No constant is redeclared across classes — one source of truth per layout (Rule 15).
- All 9 REDEFINES groups must be transformed; none may be omitted (Rule 14).
- Cross-program structures (`DFHCOMMAREA`, `CUSTOMER.cpy`, `PROCTRAN.cpy`, `WS-CHILD-DATA`) must be modelled even if not fully exercised by CRECUST's own procedure division (Rule 14 / TRA-10).

## Technical Decisions

- **Cat 4a Pattern B (shared `byte[]`):** `PROC-TRAN-DESC` has 6 siblings (1 PIC + 5 records: XFR, DELACC, CREACC, DELCUS, CRECUS). They are non-mutually exclusive — multiple views can be valid simultaneously. Model as abstract `ProcTranDescBase` holding `private final byte[] data` (40 bytes); each concrete subclass has no instance fields and encodes/decodes via byte offsets into the shared array (ADR per TRA-1).
- **Cat 4a Pattern A (mutually exclusive):** `PROC-TRAN-EYE-CATCHER` and `CASE-1/CASE-2 CONDITION-ID` use interface + independent typed-field classes. No shared `byte[]` (ADR-7). `FcConditionToken` is **not generated** — CEEDAYS and CEELOCT are replaced by `java.time`; the CEE condition-token wrapper has no execution path and would be dead code (Rule 13, G2 resolved 2026-10-01).
- **Cat 3a groups:** Modelled as record Java classes with a paired getter/setter on the owning parent for round-trip conversion (Story 2.2).
- **`ProcTranDescCrecus` layout (TRA-7):** sortCode (6) + custNo (10) + name (14) + dob (10) = 40 bytes. Byte offsets 0, 6, 16, 30.
- **Lombok on abstract/interface types:** `ProcTranDescBase` must NOT carry `@Data` — it is abstract and its single `byte[]` field is `final`. Subclasses have no additional fields, so no Lombok annotation is needed on them either.
- **`ProctranData`** holds a `byte[] procTranDesc` (40 bytes) as the raw anchor field for `PROC-TRAN-DESC`; the view classes wrap this array by reference.
- **`CustomerControlRecord`** is present even though CRECUST does not call it directly (Rule 14: consumed by suite).

## Cross-Story Dependencies

- Story 2.3 depends on Story 2.1 (`ProctranData` must exist to provide the `procTranDesc byte[]` field that the subclasses wrap).
- Story 2.5 depends on all model stories (2.1–2.4) being done, as the serializers reference the model types.
- Epic 3 onward depends on all of Epic 2 being done.
