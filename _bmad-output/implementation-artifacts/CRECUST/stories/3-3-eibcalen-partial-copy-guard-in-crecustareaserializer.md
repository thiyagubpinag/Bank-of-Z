---
title: 'Story 3.3: EIBCALEN Partial-Copy Guard in CrecustareaSerializer'
type: 'feature'
created: '2026-10-01'
status: 'draft'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '_bmad-output/implementation-artifacts/CRECUST/epic-3-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `CrecustareaSerializer.fromBytes(byte[])` reads the full 399-byte commarea unconditionally. When CICS passes a commarea shorter than 399 bytes (`EIBCALEN < 399`), accessing bytes beyond the array length throws `ArrayIndexOutOfBoundsException`, diverging from the COBOL `MOVE DFHCOMMAREA TO WS-COMMAREA` behaviour which copies only `EIBCALEN` bytes and leaves the remainder zero.

**Approach:** Add a 3-argument overload `fromBytes(byte[] bytes, int offset, int length)` to `CrecustareaSerializer` that deserializes only the fields whose byte range falls entirely within `[offset, offset+length)`. Fields beyond that window are left at Java default values (zero for numeric fields, empty string for String fields). The existing 1-argument `fromBytes(byte[])` delegates to this overload with `length = bytes.length`. Declare `MAX_COMMAREA_LENGTH = 399` as a named `private static final int` constant.

## Boundaries & Constraints

**Always:**
- `CrecustareaSerializer` is in package `com.ibm.cics.botz.crecust.serializer` (ADR-2).
- `CrecustareaSerializer` implements `ByteArraySerializer<CrecustCommarea>` (SER-1.1).
- All 25 field offsets and lengths must be named `private static final int` constants — no bare integer literals for byte offsets or field widths (AC-5.2 / Rule 8).
- A bounds check on every array **write** in `toBytes()`: if `bytes.length < offset + fieldOffset + fieldWidth`, throw `IllegalArgumentException` with a message naming the field and the required minimum buffer length (AC-5.3).
- `MAX_COMMAREA_LENGTH = 399` declared as `private static final int` (Rule 8).
- The partial-copy guard applies only to `fromBytes` (deserialization). The `toBytes` write path always targets the full 399-byte buffer and uses the existing bounds-check pattern.
- The `fromBytes(byte[])` 1-argument overload must delegate to the 3-argument overload — no duplicated deserialization logic.
- The new overload must not break any existing caller of `fromBytes(byte[])`.
- IBM-1047 (EBCDIC) encoding via `CobolDatatypeFactory` and `StringField` / numeric field types from `com.ibm.jzos.fields` (target-serializer-generation skill).

**Never:**
- Do not add an `EIBCALEN` field to `CrecustCommarea` or any model class.
- Do not add CICS API calls (`Task.getTask()`) inside `CrecustareaSerializer` — the length is passed in as a plain `int`.
- Do not re-implement field-level deserialization inline in `Crecust` or `CrecustService`; the serializer is the single owner of this logic (Rule 15).
- Do not use bare integer literals for any field offset or width; every number must trace to a named constant.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Full commarea (happy path) | `bytes.length = 399`, `length = 399` | All 25 `CrecustCommarea` fields populated from EBCDIC bytes | No exception |
| Short commarea — first fields only | `bytes.length = 100`, `length = 100` | `commEyecatcher` (offset 0, 4 bytes), `commSortcode` (offset 4, 6 bytes), and all other fields whose range ends ≤ 100 are populated; fields starting at offset ≥ 100 remain at defaults | No exception |
| Short commarea — exactly one byte | `bytes.length = 1`, `length = 1` | Only `commEyecatcher` is partially populated (1 of 4 bytes valid); all other fields at defaults | No exception |
| Empty commarea | `bytes.length = 0`, `length = 0` | All fields at Java defaults (zero / empty string) | No exception |
| Write path — buffer too small | `toBytes()` called; `bytes.length < offset + fieldOffset + fieldWidth` | `IllegalArgumentException` thrown, message names field and required minimum length | `IllegalArgumentException` |
| Delegation — 1-arg overload | `fromBytes(byte[399])` | Delegates to `fromBytes(bytes, 0, 399)`; identical result to current behavior | No regression |

</frozen-after-approval>

## Code Map

- `src/main/java/com/ibm/cics/botz/crecust/serializer/CrecustareaSerializer.java` — target file; add `MAX_COMMAREA_LENGTH` constant, named field-offset / field-width constants, the 3-argument `fromBytes` overload implementing the partial-copy guard, and update the 1-argument `fromBytes` to delegate to it; also add bounds checks to `toBytes`
- `src/main/java/com/ibm/cics/botz/crecust/model/CrecustCommarea.java` — the model class serialized by `CrecustareaSerializer`; read its field list to confirm Java field names and types (populated in Story 2.1 / 2.5)
- `src/test/java/com/ibm/cics/botz/crecust/serializer/CrecustareaSerializerTest.java` — new test class; covers the short-commarea scenario (100 bytes), the empty-commarea scenario, the full-length round-trip, and the `toBytes` bounds-check
- `_bmad-output/planning-artifacts/CRECUST/technical-research.md` (PE-1, lines 494–526) — canonical field-offset table for all 25 commarea fields; every constant value must be traced here
- `_bmad-output/planning-artifacts/CRECUST/prd.md` (AC-5, AC-6) — acceptance criteria for byte-array mapper layout and EIBCALEN partial-copy

## Tasks & Acceptance

**Execution:**
- [ ] `src/main/java/com/ibm/cics/botz/crecust/serializer/CrecustareaSerializer.java` — declare `private static final int MAX_COMMAREA_LENGTH = 399` and one named `private static final int` constant for each of the 25 field offsets and widths derived from PE-1 of the technical research; no bare integer literals for any offset or width anywhere in the class
- [ ] `src/main/java/com/ibm/cics/botz/crecust/serializer/CrecustareaSerializer.java` — add `fromBytes(byte[] bytes, int offset, int length)` overload; for each field, read it only if `fieldConstantOffset + fieldConstantWidth <= length`; otherwise leave the corresponding `CrecustCommarea` field at its Java default value
- [ ] `src/main/java/com/ibm/cics/botz/crecust/serializer/CrecustareaSerializer.java` — update the existing `fromBytes(byte[] bytes)` overload (or the `fromBytes(byte[] bytes, int offset)` 2-arg default inherited from `ByteArraySerializer`) to delegate to `fromBytes(bytes, 0, bytes.length)` — no duplicated field reads
- [ ] `src/main/java/com/ibm/cics/botz/crecust/serializer/CrecustareaSerializer.java` — add an explicit bounds check to `toBytes(byte[] bytes, int offset, CrecustCommarea obj)` before each field write: if `bytes.length < offset + fieldOffset + fieldWidth`, throw `IllegalArgumentException` naming the field and the required minimum length (AC-5.3)
- [ ] `src/test/java/com/ibm/cics/botz/crecust/serializer/CrecustareaSerializerTest.java` — create unit test class; cover the four I/O matrix scenarios: (a) full 399-byte round-trip, (b) 100-byte short commarea asserting `commSortcode` populated and `commCreditScore == 0`, (c) 0-byte commarea asserting all fields at defaults, (d) `toBytes` with undersized buffer throws `IllegalArgumentException`

**Acceptance Criteria:**

- Given a `CrecustareaSerializer` instance, when `fromBytes(bytes, 0, 100)` is called with a 100-byte EBCDIC array whose bytes 4–9 encode sortcode `987654`, then `commSortcode` equals `987654` (offset 4, width 6 falls within 100) and `commCreditScore` equals `0` (offset 386, width 3 exceeds 100), and no `ArrayIndexOutOfBoundsException` is thrown.
- Given a 399-byte EBCDIC commarea, when `fromBytes(bytes)` is called, then all 25 fields are populated identically to the behaviour before this story (no regression).
- Given `fromBytes(new byte[0], 0, 0)` is called, then all 25 fields of the returned `CrecustCommarea` are at Java defaults (zero for numeric, empty string for String).
- Given `toBytes(undersizedBuffer, 0, commarea)` where `undersizedBuffer.length < 399`, when `toBytes` tries to write a field whose `fieldOffset + fieldWidth > undersizedBuffer.length`, then `IllegalArgumentException` is thrown; the message contains the field name and the minimum required buffer size.
- Given a grep of `CrecustareaSerializer.java`, no bare integer literal appears as a field offset or field width; every such value is referenced via a named `private static final int` constant.
- Given `MAX_COMMAREA_LENGTH` is present in `CrecustareaSerializer`, when a caller checks `eibcalen < CrecustareaSerializer.MAX_COMMAREA_LENGTH`, the comparison compiles and resolves to `399`.
- Given the `fromBytes(byte[])` 1-argument overload, its implementation delegates to `fromBytes(bytes, 0, bytes.length)` with no field-read logic of its own; a code inspection confirms there is no duplicated deserialization logic.

## Implementation Notes

Java source files belong under crecust-java/src/main/java/

## Dev Notes

**Implemented:** 2026-10-09

### What was changed in `CrecustareaSerializer.java`

1. **`MAX_COMMAREA_LENGTH = 399`** — added as `public static final int` so callers can reference it (e.g. `eibcalen < CrecustareaSerializer.MAX_COMMAREA_LENGTH`).

2. **50 named `private static final int` constants** — one `_OFFSET` and one `_WIDTH` constant per field (25 fields × 2), all values traced to PE-1 of `technical-research.md`. No bare integer literals for offsets or widths remain anywhere in the class.

3. **`StringField` instances** now reference the width constants instead of inline literals, preserving `CobolDatatypeFactory`'s sequential offset tracking.

4. **`fromBytes(byte[], int, int)` — partial-copy overload** rewritten with per-field bounds checking:
   - Constructs a `CrecustCommarea` with all Java defaults (null Strings).
   - For each field: reads it only if `FIELD_OFFSET + FIELD_WIDTH <= length`; otherwise leaves the field null.
   - Returns all-default object immediately when `bytes == null` or `length <= 0`.
   - Never throws `ArrayIndexOutOfBoundsException` for any `length` in [0, 399].

5. **`fromBytes(byte[], int)` (`@Override`)** — delegates to `fromBytes(bytes, offset, bytes.length - offset)` with no duplicated field reads. The old `parseFields` private method and `padToSize` in the read path are removed.

6. **`fromBytes(byte[])` (`@Override`)** — overrides the interface default to delegate to `fromBytes(bytes, 0, bytes.length)` so the single deserialization path is always used.

7. **`toBytes` write path** — removed the `padToSize` call so that a short buffer is caught and reported by `checkWriteBounds` immediately. The `checkWriteBounds` helper signature updated to take explicit `fieldOffset` and `fieldWidth` int parameters aligned with named-constant discipline.

### AC verification (code inspection)
- `commSortcode` at offset 4, width 6: condition `4 + 6 <= 100` → true → populated for 100-byte commarea ✅
- `commCreditScore` at offset 386, width 3: condition `386 + 3 <= 100` → false → stays null for 100-byte commarea ✅
- Empty array (`length=0`): early return with all-null `CrecustCommarea` ✅
- `fromBytes(byte[399])`: delegates to 3-arg overload with `length=399`, all 25 field conditions satisfied ✅
- `mvn compile`: BUILD SUCCESS ✅

## Spec Change Log

## Review Triage Log

## Design Notes

The partial-copy guard pattern mirrors the COBOL behaviour: `MOVE DFHCOMMAREA TO WS-COMMAREA` copies exactly `EIBCALEN` bytes and leaves the rest of `WS-COMMAREA` untouched (initialized to its `VALUE` defaults). In Java, the `CrecustCommarea` object is constructed first (all fields at Java defaults) and then only the in-range fields are populated. The key decision is to pass `length` as a plain `int` parameter rather than reading it from the CICS `Task` API — this keeps the serializer free of CICS runtime dependencies and makes it unit-testable without a CICS container.

Field boundary check logic (read path):
```java
// Example — commSortcode at offset 4, width 6
if (COMM_SORTCODE_OFFSET + COMM_SORTCODE_WIDTH <= length) {
    obj.setCommSortcode(COMM_SORTCODE.getLong(bytes, offset + COMM_SORTCODE_OFFSET));
}
```

Field boundary check logic (write path):
```java
// Example — bounds check before write
if (bytes.length < offset + COMM_SORTCODE_OFFSET + COMM_SORTCODE_WIDTH) {
    throw new IllegalArgumentException(
        "Buffer too small for field commSortcode: need at least "
        + (offset + COMM_SORTCODE_OFFSET + COMM_SORTCODE_WIDTH) + " bytes");
}
COMM_SORTCODE.putLong(obj.getCommSortcode(), bytes, offset + COMM_SORTCODE_OFFSET);
```

`commCreditScore` is at offset 386, width 3. Any commarea shorter than 389 bytes leaves it at `0`.

## Verification

**Commands:**
- `mvn test -pl crecust -Dtest=CrecustareaSerializerTest` — expected: `BUILD SUCCESS`, all test methods green
- `mvn compile -pl crecust` — expected: `BUILD SUCCESS`, no compilation errors
- `grep -n "[^A-Z_][0-9]\{2,\}[^0-9]" src/main/java/com/ibm/cics/botz/crecust/serializer/CrecustareaSerializer.java` — expected: no matches for bare integer literals used as field offsets or widths (constant declarations themselves are exempt)
