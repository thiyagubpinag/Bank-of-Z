# Epic 2 Context: Data Models and Infrastructure

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Produce all Java data model classes, commarea DTOs, infrastructure interfaces, and their paired JZOS-style byte-array serializers (ADR-A). Every wire-format class must implement `ByteArraySerializable<T>`; every serializer must implement `ByteArraySerializer<T>`. No Spring annotations anywhere in Epic 2 output. This epic lays the data-contract foundation that all service, DAO, and integration stories in Epics 3–6 depend on.

## Stories

- Story 2.1: Infrastructure interfaces (`ByteArraySerializer`, `ByteArraySerializable`, `Settable`, `Lists`)
- Story 2.2: `CreaccCommarea` model and `CreaccCommareaSerializer` (92 bytes)
- Story 2.3: `InqcustCommarea` model and `InqcustCommareaSerializer`
- Story 2.4: `InqacccuCommarea` model and `InqacccuCommareaSerializer` (with ODO array)
- Story 2.5: `AbndinfoRec` model and `AbndinfoRecSerializer`
- Story 2.6: `ProcTranData` model and `ProcTranDataSerializer` (with Cat 4a REDEFINES)
- Story 2.7: Cross-program data structures (`AccountData`, `CustomerRecord`, `ReturnData`, `AccountControl`, `NcsAccNoStuff`)
- Story 2.8: `DateTimeFormatConstants` and `CreaccLayoutConstants`

## Requirements & Constraints

- All four infrastructure interfaces (`ByteArraySerializer<T>`, `ByteArraySerializable<T>`, `Settable<T>`, `Lists`) must live in `com.ibm.bankofz.creacc.infrastructure`.
- All concrete serializers live in `com.ibm.bankofz.creacc.infrastructure.serialization` (or subdirectory).
- Every field offset and length in every serializer must be a `private static final int` constant — no bare integer literals in `put`/`get` calls (Rule 5).
- No Spring annotations (`@Component`, `@Bean`, `@Service`, etc.) anywhere in Epic 2 output.
- Each Java type must be in its own `.java` file — no multiple top-level types per file (Rule 17).
- All serializers must use JZOS `CobolDatatypeFactory` fields with IBM-1047 (EBCDIC) encoding.
- Serializer delegation: each layout has exactly one canonical serializer; no service class may duplicate offset constants (Rule 15).
- Lombok is enabled — data model classes use `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder(toBuilder=true)`.

## Technical Decisions

- **ADR-A (Serialization)**: Exact COBOL PIC-derived byte widths; all wire-format DTOs implement `ByteArraySerializable<T>`. Infrastructure interfaces are the foundation — all subsequent serializer stories depend on Story 2.1 being done first.
- **ADR-04 (REDEFINES)**: Date REDEFINES groups (Cat 3a) → getter/setter views using `DateTimeFormatter`; `ProcTranDesc` (Cat 4a) → polymorphic `ProcTranDescription` interface with five concrete implementing classes.
- **ADR-05 (Dates)**: Date fields → `LocalDate`; time fields → `LocalTime`; `DateTimeFormatter` constants in `DateTimeFormatConstants`.
- **Package alignment**: The `infrastructure` directory was created by Story 1.2 using the full name; use `com.ibm.bankofz.creacc.infrastructure` (not the architecture doc's shorthand `infra`).
- **`ByteArraySerializable`** extends `Settable<T>` — models gain `set(T that)` for COBOL value-copy semantics.
- **`ByteArraySerializer`** encoding: `Charset.forName("IBM-1047")` static field; `padToSize` default method with EBCDIC space `0x40`.
- **`Lists`** static-only utility: `resize(List, int, Supplier)` and `create(Supplier<List>, int, Supplier)`.

## Cross-Story Dependencies

- Stories 2.2–2.8 all depend on Story 2.1 (the four infrastructure interfaces must exist first).
- Story 2.8 (`DateTimeFormatConstants`) has no dependency on 2.1 — it only needs Story 1.1 (pom.xml). Stories in Epics 3–4 that format dates import from this constants class.
- All serializer stories (2.2–2.6) depend on the `ByteArraySerializer` / `ByteArraySerializable` interfaces from 2.1.
- Epic 3 (business logic) depends on the model classes from 2.2–2.7 being available.
- Epic 4 (DAO) depends on `AccountData`, `ProcTranData`, and constants from 2.7 / 2.8.
