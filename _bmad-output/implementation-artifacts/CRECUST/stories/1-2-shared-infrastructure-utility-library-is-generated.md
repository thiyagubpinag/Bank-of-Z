---
title: 'Story 1.2: Shared Infrastructure Utility Library Is Generated'
type: 'feature'
created: '2026-10-01'
status: 'review'
route: 'dispatch'
review_loop_iteration: 0
context: []
baseline_commit: '05b61033e33a7211860a23b46abd5f630420dfa3'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Epic 2 serializer classes must implement `ByteArraySerializer<T>` and `ByteArraySerializable<T>`, but those infrastructure contracts do not yet exist in the `crecust-java` project — they are only present in the sibling `creacc-java` project under a different package. Without them, every serializer would need to re-declare the byte-packing contracts inline, violating ADR-A and Rule 15.

**Approach:** Create a `botz-cics-common` Maven sub-project (or a `common` source directory within `crecust-java`) containing the four infrastructure types — `ByteArraySerializer<T>`, `ByteArraySerializable<T>`, `Settable<T>`, and `Lists` — under the `com.ibm.cics.botz.common` package, each in its own `.java` file (Rule 17). Update `crecust/pom.xml` to depend on the common JAR so that all downstream serializer classes in Epic 2 can compile without re-implementing byte-packing logic.

## Boundaries & Constraints

**Always:**
- Each of the four types must live in its own `.java` file (Rule 17 — one top-level type per file).
- Package for all four types: `com.ibm.cics.botz.common` (matches the `com.ibm.cics.botz` groupId from ADR-3).
- `ByteArraySerializer<T>` generic bound: `T extends ByteArraySerializable<T>`.
- `ByteArraySerializable<T>` must extend `Settable<T>` and declare `ByteArraySerializer<T> serializer()`.
- The `encoding` static field in `ByteArraySerializer` must use `Charset.forName("IBM-1047")` (EBCDIC — not UTF-8).
- `padToSize()` must fill padding bytes with `(byte) 0x40` (EBCDIC space).
- The `Lists` utility class must be `final` with a private constructor (static-only utility).
- The common library must carry no CICS, JZOS, Lombok, or SLF4J compile-scope dependency (those belong in `crecust/pom.xml`). It must be pure Java 21.
- `crecust/pom.xml` (created by Story 1.1) must declare a `compile`-scope dependency on the common library JAR.
- No `ByteArraySerializer` logic may be re-implemented inline anywhere in `crecust-java` (Rule 15).

**Never:**
- Do not add a `ZFileSerializer` or `ZFileSerializable` (those are JZOS-specific; not required by the CRECUST architecture — the three canonical serializers operate on in-memory byte arrays, not ZFile I/O).
- Do not place the four types inside `crecust-java`'s own source tree as internal classes — they must be in a separate, reusable artifact so that any future CICS Java project in Bank-of-Z can depend on them.
- Do not copy the `creacc-java` infrastructure classes and rebrand them with a find-replace only; verify that every method signature, generic bound, and Javadoc comment matches the canonical specifications in the `static-utility-class-or-interface-generation` skill.
- Do not use Spring, CDI, or any framework annotation in the common library.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Short byte array passed to `fromBytes` | `bytes.length < numBytes()` | `padToSize` pads to `numBytes()` with EBCDIC `0x40` before field reads | No exception thrown; padded copy used |
| `Lists.resize` shrink | list size 5, target `n=2` | Elements at indices 2, 3, 4 removed; list size becomes 2 | — |
| `Lists.resize` grow | list size 1, target `n=3`, supplier `() -> ""` | Two `""` elements appended; list size becomes 3 | — |
| `Lists.create` | `n=0`, any supplier | Returns an empty list (no supplier calls) | — |
| `copyFrom(byte[])` round-trip | Any serializable object serialized then deserialized | Deserialized object equals original | — |

</frozen-after-approval>

## Code Map

- `creacc-java/src/main/java/com/ibm/bankofz/creacc/infrastructure/ByteArraySerializer.java` — canonical reference implementation to model the new file from; already production-quality with full Javadoc and `padToSize` implementation
- `creacc-java/src/main/java/com/ibm/bankofz/creacc/infrastructure/ByteArraySerializable.java` — canonical reference for `copyFrom` overloads, `getBytes` alias, and `toByteString`
- `creacc-java/src/main/java/com/ibm/bankofz/creacc/infrastructure/Settable.java` — canonical reference for the `set(T that)` single-method interface
- `creacc-java/src/main/java/com/ibm/bankofz/creacc/infrastructure/Lists.java` — canonical reference for `resize` and `create` static methods
- `creacc-java/pom.xml` — reference Maven structure (Java 21, JCICS `provided`, JZOS `provided`, SLF4J `compile`); note: `creacc` does NOT use Lombok — the `crecust` pom must add Lombok per Story 1.1
- `crecust-java/pom.xml` — target: add `com.ibm.cics.botz:botz-cics-common` dependency here (created by Story 1.1; will not exist until Story 1.1 is done)
- `_bmad-output/planning-artifacts/CRECUST/architecture.md` §10 — lists the four required infrastructure types and their purposes
- `_bmad-output/planning-artifacts/CRECUST/architecture.md` ADR-1 — mandates `ByteArraySerializable<Self>` on every model class with a byte-array interface; `ByteArraySerializer<T>` is the single-source-of-truth contract

## Tasks & Acceptance

**Execution:**

- [x] `botz-cics-common/pom.xml` — create new Maven project file; `groupId=com.ibm.cics.botz`, `artifactId=botz-cics-common`, `version=1.0.0-SNAPSHOT`, `packaging=jar`, Java 21 source/target, no compile-scope dependencies beyond the JDK — this is a pure-Java utility library
- [x] `botz-cics-common/src/main/java/com/ibm/cics/botz/common/Settable.java` — create the `Settable<T>` interface with single method `void set(T that)`; Javadoc must explain COBOL value-copy (MOVE) semantics and object-identity preservation
- [x] `botz-cics-common/src/main/java/com/ibm/cics/botz/common/ByteArraySerializer.java` — create the `ByteArraySerializer<T extends ByteArraySerializable<T>>` interface; must include: `encoding` static field (`IBM-1047`), abstract `int numBytes()`, abstract `byte[] toBytes(byte[], int, T)`, abstract `T fromBytes(byte[], int)`; default methods: `numBytesFor(T)`, `toBytes(byte[], T)`, `toBytes(T)`, `toByteString(T)`, `fromBytes(byte[])`, `fromByteString(String)`, `padToSize(byte[], int, int)` with EBCDIC `0x40` padding
- [x] `botz-cics-common/src/main/java/com/ibm/cics/botz/common/ByteArraySerializable.java` — create `ByteArraySerializable<T extends ByteArraySerializable<T>>` extending `Settable<T>`; abstract `ByteArraySerializer<T> serializer()`; default methods: `toBytes()`, `getBytes()` (alias for `toBytes()`), `toByteString()`, `copyFrom(byte[])`, `copyFrom(String)`, `copyFrom(U that)` (generic cross-type overlay overload)
- [x] `botz-cics-common/src/main/java/com/ibm/cics/botz/common/Lists.java` — create `final class Lists` with private constructor; static methods `resize(List<T>, int, Supplier<? extends T>)` and `create(Supplier<? extends List<T>>, int, Supplier<? extends T>)`
- [x] `crecust-java/pom.xml` — add `<dependency>` block for `com.ibm.cics.botz:botz-cics-common:1.0.0-SNAPSHOT` with `compile` scope; this file was created by Story 1.1 and must already exist before this task runs

**Acceptance Criteria:**

- Given the `botz-cics-common` project is built with `mvn install`, when `mvn compile` is run from `crecust-java/`, then the build succeeds and resolves `com.ibm.cics.botz.common.ByteArraySerializer` from the classpath without error.
- Given four `.java` source files in `botz-cics-common/src/main/java/com/ibm/cics/botz/common/`, when the directory is listed, then exactly `ByteArraySerializer.java`, `ByteArraySerializable.java`, `Settable.java`, and `Lists.java` are present — one top-level type per file (Rule 17).
- Given a test class in `botz-cics-common` that implements `ByteArraySerializer<T>` and calls `padToSize(new byte[2], 0, 10)`, when the method returns, then the result is a 10-byte array where bytes 2–9 equal `0x40`.
- Given a `List<String>` of size 3 passed to `Lists.resize(list, 5, () -> "")`, when the call returns, then `list.size() == 5` and `list.get(3).equals("") && list.get(4).equals("")`.
- Given a `List<String>` of size 5 passed to `Lists.resize(list, 2, () -> "")`, when the call returns, then `list.size() == 2`.
- Given `ByteArraySerializable.copyFrom(byte[])` is called, when the method completes, then `this.set(serializer().fromBytes(bytes))` has been invoked — no inline byte-parsing occurs in the interface.
- Given `crecust-java/pom.xml` contains the `botz-cics-common` dependency, when `mvn dependency:list` is run from `crecust-java/`, then `com.ibm.cics.botz:botz-cics-common:jar:1.0.0-SNAPSHOT:compile` appears in the output.
- Given no class in `crecust-java/src/` (any package), when the source tree is scanned for inline byte-packing logic (direct use of `byte[]` writes without delegating to a `ByteArraySerializer` implementation), then zero such occurrences exist (Rule 15).

## Implementation Notes

**Dev Record (build pass — story 1-2):**

Completed on 2026-10-09.

Files created:
- `botz-cics-common/pom.xml` — new Maven project; `groupId=com.ibm.cics.botz`, `artifactId=botz-cics-common`, `version=1.0.0-SNAPSHOT`, Java 21, no compile-scope dependencies beyond the JDK
- `botz-cics-common/src/main/java/com/ibm/cics/botz/common/Settable.java`
- `botz-cics-common/src/main/java/com/ibm/cics/botz/common/ByteArraySerializer.java`
- `botz-cics-common/src/main/java/com/ibm/cics/botz/common/ByteArraySerializable.java`
- `botz-cics-common/src/main/java/com/ibm/cics/botz/common/Lists.java`

Files modified:
- `crecust-java/pom.xml` — added `compile`-scope dependency on `com.ibm.cics.botz:botz-cics-common:1.0.0-SNAPSHOT`

Build results:
- `cd botz-cics-common && mvn -q clean install` → EXIT:0 (BUILD SUCCESS)
- `cd crecust-java && mvn -q compile` → EXIT:0 (BUILD SUCCESS)
- `mvn dependency:list | grep botz` → `com.ibm.cics.botz:botz-cics-common:jar:1.0.0-SNAPSHOT:compile` confirmed

## Spec Change Log

## Design Notes

The `botz-cics-common` library is intentionally dependency-free (pure Java 21). JZOS classes (`CobolDatatypeFactory`, `StringField`, etc.) are used only inside concrete `ByteArraySerializer` implementations in `crecust-java` — not in the abstract contracts. This keeps the common library lightweight and reusable by any future CICS Java project in Bank-of-Z that does not need JZOS.

The four types in `creacc-java/infrastructure/` are the proven reference implementations to copy from. The only changes required are:
1. Package declaration: `com.ibm.bankofz.creacc.infrastructure` → `com.ibm.cics.botz.common`
2. Cross-references between the four types updated to the new package.
3. `Lists` must be `final` with a private no-arg constructor (the `creacc-java` version already has this).

Do not alter any method signature, generic bound, or default-method body from the reference — they are already correct per the `static-utility-class-or-interface-generation` and `target-serializer-generation` skills.

## Verification

**Commands:**
- `cd botz-cics-common && mvn clean install` -- expected: `BUILD SUCCESS` with zero compilation errors and zero test failures
- `cd crecust-java && mvn dependency:list | grep botz-cics-common` -- expected: `com.ibm.cics.botz:botz-cics-common:jar:1.0.0-SNAPSHOT:compile` appears in output
- `cd crecust-java && mvn compile` -- expected: `BUILD SUCCESS`; confirms `ByteArraySerializer`, `ByteArraySerializable`, `Settable`, and `Lists` resolve from the classpath
