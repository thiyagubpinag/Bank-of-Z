---
status: in-review
route: oneshot
story_key: 2-1-infrastructure-interfaces-bytearrayserializer-bytearrayseria
context:
  - _bmad-output/implementation-artifacts/CREACC/epic-2-context.md
  - _bmad-output/planning-artifacts/CREACC/architecture-class-list.md
---

# Story 2.1 — Infrastructure Interfaces (`ByteArraySerializer`, `ByteArraySerializable`, `Settable`, `Lists`)

## Summary

Implement the four infrastructure utility types in `com.ibm.bankofz.creacc.infrastructure` so that all serializer and model classes in Epic 2 have a stable, shared contract to implement.

## Context

- Epic: 2 — Data Models and Infrastructure
- Program: CREACC (Create Account CICS batch program)
- Target: Java 21 · No Spring · JZOS byte-array serialization (ADR-A)
- Sprint-status key: `2-1-infrastructure-interfaces-bytearrayserializer-bytearrayseria`

<frozen-after-approval>

## Tasks & Acceptance

### Task 1 — `Settable<T>` interface

- [x] `creacc-java/src/main/java/com/ibm/bankofz/creacc/infrastructure/Settable.java` created
- [x] Declares exactly one method: `void set(T that)`
- [x] Package: `com.ibm.bankofz.creacc.infrastructure`
- [x] No Spring annotations

### Task 2 — `ByteArraySerializer<T>` interface

- [x] `creacc-java/src/main/java/com/ibm/bankofz/creacc/infrastructure/ByteArraySerializer.java` created
- [x] Generic constraint: `T extends ByteArraySerializable<T>`
- [x] Static `encoding` field: `Charset.forName("IBM-1047")`
- [x] Abstract methods: `int numBytes()`, `byte[] toBytes(byte[] bytes, int offset, T obj)`, `T fromBytes(byte[] bytes, int offset)`
- [x] Default convenience overloads: `toBytes(byte[], T)`, `toBytes(T)`, `toByteString(T)`, `fromBytes(byte[])`, `fromByteString(String)`
- [x] Default `padToSize` method using EBCDIC space `0x40`
- [x] No Spring annotations

### Task 3 — `ByteArraySerializable<T>` interface

- [x] `creacc-java/src/main/java/com/ibm/bankofz/creacc/infrastructure/ByteArraySerializable.java` created
- [x] Extends `Settable<T>`
- [x] Generic constraint: `T extends ByteArraySerializable<T>`
- [x] Abstract method: `ByteArraySerializer<T> serializer()`
- [x] Default methods: `toBytes()`, `getBytes()` (alias), `toByteString()`, `copyFrom(byte[])`, `copyFrom(String)`, `copyFrom(U that)` (generic overload)
- [x] `toBytes()` and `getBytes()` return `byte[]` — NOT `void`
- [x] No extra alias methods beyond those in the spec
- [x] No Spring annotations

### Task 4 — `Lists` utility class

- [x] `creacc-java/src/main/java/com/ibm/bankofz/creacc/infrastructure/Lists.java` created
- [x] Static methods only: `resize(List<T>, int, Supplier<? extends T>)` and `create(Supplier<? extends List<T>>, int, Supplier<? extends T>)`
- [x] No instance state or instance methods
- [x] No Spring annotations

### Task 5 — Unit test

- [x] `creacc-java/src/test/java/com/ibm/bankofz/creacc/infrastructure/ByteArraySerializerTest.java` created
- [x] Round-trip test: a simple 4-byte `ByteArraySerializer` implementation: `toBytes(obj)` followed by `fromBytes(bytes)` returns an object equal to the original

</frozen-after-approval>

## Design Notes

- `ByteArraySerializable<T>` extends `Settable<T>` — models gain `set(T that)` for COBOL value-copy semantics in addition to serialization methods.
- The `encoding` constant in `ByteArraySerializer` is `static` (interface-level) so it is shared across all implementations; no duplicate charset declarations.
- `padToSize` uses `0x40` (EBCDIC space) for padding — not ASCII space `0x20`.
- Package is `com.ibm.bankofz.creacc.infrastructure` (full name, matching the directory created by Story 1.2, not the shorthand `infra` in the architecture doc).
- All four types are in separate `.java` files (Rule 17).

## Code Map

- `creacc-java/src/main/java/com/ibm/bankofz/creacc/infrastructure/package-info.java` — package stub already present; must remain untouched.
- No existing implementation files to modify.

## Implementation Notes

## Spec Change Log

- Created: initial spec for story 2-1.
