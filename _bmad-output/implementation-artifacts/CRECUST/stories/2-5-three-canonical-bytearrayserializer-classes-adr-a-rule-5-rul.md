---
status: review
epic: 2
story: 5
route: oneshot
---

# Story 2.5 — Three Canonical `ByteArraySerializer` Classes (ADR-A, Rule 5 / Rule 15)

## Goal

Implement the three canonical serializer classes — `CrecustareaSerializer`, `WsChildDataSerializer`,
and `AbndInfoRecSerializer` — each implementing `ByteArraySerializer<T>` from the shared library,
so that all byte-array ↔ model conversions go through a single authoritative class per layout and
no service class re-implements byte-offset arithmetic (Rule 15).

## Acceptance Criteria

**Given** a 399-byte byte array representing a valid CRECUST commarea (EBCDIC-encoded),
**When** `CrecustareaSerializer.fromBytes(bytes)` is called,
**Then** the returned `CrecustCommarea` has `commSortcode = "987654"` (the SORTCODE constant)
and all 25 fields populated correctly.

**And** `CrecustareaSerializer.toBytes(commarea)` round-trips: `fromBytes(toBytes(c))` returns
a `CrecustCommarea` equal to `c` for all non-null field values.

**And** all three serializers exist in package `com.ibm.cics.botz.crecust.serializer`,
each in its own `.java` file:
- `CrecustareaSerializer` — 399 bytes (DFHCOMMAREA / CRECUST.cpy)
- `WsChildDataSerializer` — 399 bytes (WS-CHILD-DATA)
- `AbndInfoRecSerializer` — 681 bytes (ABNDINFO-REC)

**And** each serializer implements `ByteArraySerializer<T>` from the shared library (Story 1.2).

**And** every field offset and length in each serializer is declared as a named
`private static final` field via `CobolDatatypeFactory` — no bare integer literals in offset
arithmetic (NFR-6, Rule 5, AC-5.2).

**And** a bounds check precedes every array write: if `buffer.length < offset + fieldLength`,
an `IllegalArgumentException` is thrown naming the field and required minimum length
(AC-5.3, Rule 5).

**And** no field-width constant is redeclared outside the owning serializer (AC-5.4, Rule 15 / AC-15.2).

**And** no stub or redirect file exists — every `.java` file in `serializer/` contains a real
class (AC-5.6, Rule 5).

**And** the partial-copy overload `fromBytes(byte[] bytes, int offset, int length)` is
implemented in `CrecustareaSerializer` to support the EIBCALEN guard (Story 3.3 depends on this).

**And** the three model classes `CrecustCommarea`, `WsChildData`, and `AbndInfoRec` now return
the corresponding serializer singleton from their `serializer()` method instead of throwing
`UnsupportedOperationException`.

## Implementation Notes

### Files Created

- `crecust-java/src/main/java/com/ibm/cics/botz/crecust/serializer/CrecustareaSerializer.java`
  — 399 bytes, 25 fields, mirrors `CRECUST.cpy` layout (PE-1).
  Includes `fromBytes(byte[], int, int)` partial-copy overload for EIBCALEN guard.
  Singleton exposed via `INSTANCE`.

- `crecust-java/src/main/java/com/ibm/cics/botz/crecust/serializer/WsChildDataSerializer.java`
  — 399 bytes, 25 fields (23 customer fields + wsChildSuccess + wsChildFailCode).
  Singleton exposed via `INSTANCE`.

- `crecust-java/src/main/java/com/ibm/cics/botz/crecust/serializer/AbndInfoRecSerializer.java`
  — 681 bytes, 12 fields. Special field types:
  - `ABND-UTIME-KEY S9(15) COMP-3` → `PackedDecimalAsLongField(15, true)` → 8 bytes
  - `ABND-RESPCODE / RESP2CODE / SQLCODE S9(8) SIGN LEADING SEPARATE` →
    `ExternalDecimalAsIntField(8, true, true, true, false)` → 9 bytes each
  Singleton exposed via `INSTANCE`.

### Files Modified

- `CrecustCommarea.java` — `serializer()` now returns `CrecustareaSerializer.INSTANCE`
- `WsChildData.java` — `serializer()` now returns `WsChildDataSerializer.INSTANCE`
- `AbndInfoRec.java` — `serializer()` now returns `AbndInfoRecSerializer.INSTANCE`

### JZOS API Notes

- `getPackedDecimalAsLongField(int numDigits, boolean signed)` — 2-arg form (not 3-arg)
- `getExternalDecimalAsIntField(int numDigits, boolean signed, boolean signLeading, boolean signSeparate, boolean blankWhenZero)` — 5-arg form for SIGN LEADING SEPARATE fields

### Build Verification

`mvn compile` passes with no errors.

## Dev Notes

- Implemented `CrecustareaSerializer` (399 bytes, 25 fields, including `fromBytes(byte[], int, int)` partial-copy overload).
- Implemented `WsChildDataSerializer` (399 bytes, 25 fields).
- Implemented `AbndInfoRecSerializer` (681 bytes, 12 fields) with JZOS `PackedDecimalAsLongField` and `ExternalDecimalAsIntField`.
- Updated `CrecustCommarea`, `WsChildData`, and `AbndInfoRec` to return their respective serializer singleton instances (`INSTANCE`).
- Verified build with `mvn clean compile`.

## Spec Change Log

- 2026-10-01 (initial): Story created and implemented.
- 2026-10-09: Implemented canonical serializers and updated model classes; status moved to review.
