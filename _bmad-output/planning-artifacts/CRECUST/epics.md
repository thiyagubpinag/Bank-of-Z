---
stepsCompleted: [1, 2, 3, 4]
inputDocuments:
  - _bmad-output/planning-artifacts/CRECUST/prd.md
  - _bmad-output/planning-artifacts/CRECUST/architecture.md
  - _bmad-output/planning-artifacts/CRECUST/java-transform-metadata.md
  - _bmad-output/planning-artifacts/CRECUST/redefines-classification.md
---

# CRECUST Java Modernisation — Epic Breakdown

This document provides the complete epic and story breakdown for the Java modernisation of the
`CRECUST` COBOL program, decomposing requirements from the PRD, Architecture, and static-analysis
metadata into implementable stories.

**Java modernisation of the CRECUST COBOL program.**

`CRECUST` is a CICS-linked Enterprise COBOL program that creates a new bank customer record.
It receives a 399-byte `DFHCOMMAREA`, validates customer data, performs an asynchronous
multi-agency credit-score check via JCICS `AsyncService`, allocates a sequential customer number
protected by a CICS Named Counter ENQ/DEQ, inserts the customer row into the DB2 `CUSTOMER`
table, writes an audit record to `PROCTRAN`, and returns success or a fail-code through the same
commarea.

The modernised Java program runs under the **JCICS JVM server** in CICS Transaction Server.

| Setting | Value |
|---|---|
| App Architecture | `cics_batch` |
| Build Tool | Maven |
| Java Version | 21 |
| Database Connection | JDBC (JNDI lookup) |
| Lombok | Enabled |
| Serializers | Manually authored (ADR-A) |

**Team lead decisions recorded before sprint planning:**

| # | Question | Decision |
|---|---|---|
| Q1 | Credit-check implementation | Option A — `CreditCheckService` implements full JCICS async loop natively (ADR-5); no COBOL helper shim |
| Q2 | Infrastructure utility classes | Generated in a shared library (separate project dependency) |
| Q3 | REDEFINES story granularity | Option B — one story per REDEFINES category group |
| Q4 | Test scope | Option A — unit tests written inline as part of each story |



## Overview

## Requirements Inventory

### Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Program Entry and Commarea Binding — Java class implements JCICS entry-point; 399-byte commarea deserialized via `CrecustareaSerializer`; re-serialized and returned via `Task.getTask().returnToCaller()` |
| FR-2 | Title Validation — `COMM-TITLE` must be one of 11 accepted values; fail-code `'T'` on failure; silent-return, no ENQ taken |
| FR-3 | Date-of-Birth Validation — year ≥ 1601; valid `LocalDate`; age ≤ 150; not in the future; fail-codes `'O'`, `'Z'`, `'Y'` |
| FR-4 | Populate Time and Date — JCICS ASKTIME + FORMATTIME to populate `WS-ORIG-DATE` (DD/MM/YYYY), PROCTRAN time (HHMMSS), and `WS-ORIG-DATE-GRP-X` (DD.MM.YYYY) |
| FR-5 | Asynchronous Credit-Check — `CreditCheckService` uses JCICS `AsyncServiceImpl` (ADR-5) to run OCR1–OCR5 child transactions, FETCH ANY, PUT/GET CONTAINER; returns credit-score and review-date |
| FR-6 | Credit-Score Review Date — compute today + 1..20 random days on success; set today on failure; DDMMYYYY byte layout in both paths |
| FR-7 | Customer Number Allocation — JCICS `NameResource.enqueue()`; JDBC SELECT + UPDATE on `STTESTER.CONTROL`; push new number to four targets; four DEQ call sites |
| FR-8 | Insert Customer Record — populate `HostCustomerRow`; JDBC INSERT CUSTOMER (17 columns); date fields encoded as YYYYMMDD integer; fail-code `'1'` silent-return on SQL failure |
| FR-9 | Write PROCTRAN Audit Record — ASKTIME + FORMATTIME; assemble 40-byte descriptor; JDBC INSERT PROCTRAN (9 columns); notifying-abort on SQL failure (populate ABNDINFO → DEQ → LINK ABNDPROC → ABEND HWPT) |
| FR-10 | CICS Level Check — retrieve CICS version via JCICS Task inquiry; populate `WsCicstsLevelNumGrp` (VV/RR/MM) |
| FR-11 | Fail Code Inventory — 17 distinct fail-code values declared as named constants in `CrecustException` |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | Java 21 target; Maven build; `pom.xml` declares `java.version=21`, JCICS dependency |
| NFR-2 | Coding standards: PascalCase classes, camelCase methods/vars, UPPER_SNAKE_CASE constants; methods ≤ 40 lines; one top-level type per file |
| NFR-3 | Exception handling: all handler classes map to concrete COBOL constructs; `'HWPT'` ABEND code as named constant; `linkAbndproc()` returns `void` |
| NFR-4 | Resource management: all JDBC resources via `try-with-resources`; ENQ released at all four DEQ call sites |
| NFR-5 | Stateless design: no per-request state in instance fields; constants as `static final` |
| NFR-6 | Byte-array contract: all offsets as named constants; bounds check before every array write; no constant redeclared across classes |

### Additional Requirements

- **Runtime:** JCICS JVM server — no Spring Boot, no Liberty, no DI framework; JNDI DataSource lookup only
- **Package structure (ADR-2):** layer-first under `com.ibm.cics.botz.crecust`; sub-packages: root, service, model, db, serializer, exception
- **Maven coordinates (ADR-3):** groupId `com.ibm.cics.botz`, artifactId `crecust`, version `1.0.0-SNAPSHOT`
- **Serialization (ADR-A):** Three canonical serializers: `CrecustareaSerializer` (399 B), `WsChildDataSerializer` (399 B), `AbndInfoRecSerializer` (≈673 B); implement `ByteArraySerializer<T>`; model classes implement `ByteArraySerializable<T>`
- **Infrastructure interfaces** (`ByteArraySerializer`, `ByteArraySerializable`, `Settable`, `Lists`) generated in a separate shared library (Q2 decision)
- **Credit-check (ADR-5):** `CreditCheckService` implements JCICS async loop natively — no COBOL helper shim (Q1 decision)
- **Lombok (ADR-12):** `@Data`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor` on all model classes
- **SYSIDERR retry (ADR-8):** not implemented — variable declared but never used in COBOL PROCEDURE DIVISION
- **Storm-drain (ADR-9):** out of scope for this transformation
- **No Spring annotations** (`@Component`, `@Service`, `@Bean`, `@Resource`): raw JVM server, JNDI only
- **Project root for `pom.xml`:** `/Users/arnold/github.com/IBM/Bank-of-Z/crecust-java/pom.xml`

### UX Design Requirements

N/A — CRECUST is a CICS-linked back-end program with no user-facing interactive I/O. Rule 18 satisfied by construction.

### FR Coverage Map

| FR | Epic | Story |
|---|---|---|
| FR-1 | Epic 3 — CICS Entry Point | Story 3.1 (`Crecust` entry), Story 3.2 (serializer wiring), Story 3.3 (EIBCALEN partial-copy) |
| FR-2 | Epic 4 — Input Validation | Story 4.1 (`ValidationService.validateTitle`) |
| FR-3 | Epic 4 — Input Validation | Story 4.2 (`ValidationService.validateDateOfBirth`) |
| FR-4 | Epic 3 — CICS Entry Point | Story 3.4 (time/date population) |
| FR-5 | Epic 5 — Credit Check | Story 5.1 (channel + containers), Story 5.2 (async run loop), Story 5.3 (fetch + aggregation) |
| FR-6 | Epic 5 — Credit Check | Story 5.4 (review-date calculation) |
| FR-7 | Epic 6 — Customer Number | Story 6.1 (ENQ), Story 6.2 (CONTROL SELECT + UPDATE), Story 6.3 (DEQ four call sites) |
| FR-8 | Epic 7 — Customer DB2 Write | Story 7.1 (populate `HostCustomerRow`), Story 7.2 (JDBC INSERT CUSTOMER) |
| FR-9 | Epic 8 — PROCTRAN / Abend | Story 8.1 (PROCTRAN assembly + INSERT), Story 8.2 (notifying-abort path) |
| FR-10 | Epic 3 — CICS Entry Point | Story 3.5 (CICS version check) |
| FR-11 | Epic 9 — Exception Class | Story 9.1 (fail-code constants and factories) |
| NFR-1 | Epic 1 — Project Setup | Story 1.1 (`pom.xml`) |
| NFR-2 | All epics (enforced per story) | — |
| NFR-3 | Epic 9 — Exception Class | Story 9.1, Story 9.2 |
| NFR-4 | Epic 6, Epic 8 | Story 6.3, Story 8.2 |
| NFR-5 | All service epics | — |
| NFR-6 | Epic 2 — Data Models | Story 2.3, Story 2.4, Story 2.5 |

## Epic List

### Epic 1: Project Setup and Build Infrastructure
Maven project scaffolding, `pom.xml`, package directories, and shared infrastructure
utility interfaces generated as a shared library dependency.
**FRs covered:** NFR-1 (build), NFR-2 (standards scaffold)

### Epic 2: Data Models and Shared Serialization Contracts
All Java model classes, REDEFINES classes, DB2 host-variable rows, and the three canonical
`ByteArraySerializer` classes (ADR-A). Establishes the shared type system before any
service logic is written.
**FRs covered:** DS-1 through DS-12 (data structures), SER-1, SER-2, NFR-6, AC-5, AC-14, AC-15, AC-17, AC-19

### Epic 3: CICS Entry Point, Commarea Binding, and EIBCALEN Partial-Copy
`Crecust` entry-point class, `CrecustService` orchestrator skeleton, commarea
deserialization/re-serialization, EIBCALEN partial-copy guard, CICS version check,
time/date population, and shared exit method (`returnToCaller`).
**FRs covered:** FR-1, FR-4, FR-10

### Epic 4: Input Validation — Title and Date-of-Birth
`ValidationService` with `validateTitle()` and `validateDateOfBirth()`. Silent-return
error paths with named fail-codes. LE `CEEDAYS`/`CEELOCT` replaced by `java.time`.
**FRs covered:** FR-2, FR-3, AC-11 (DOB date format)

### Epic 5: Asynchronous Credit-Check (JCICS AsyncService Loop)
`CreditCheckService` implementing the full JCICS async loop — PUT CONTAINER, RUN TRANSID
(OCR1–OCR5), DELAY, FETCH ANY NOSUSPEND, GET CONTAINER, score aggregation, and review-date
computation.
**FRs covered:** FR-5, FR-6, AC-11 (review-date format)

### Epic 6: Customer Number Allocation (ENQ / CONTROL / DEQ)
`CustomerNumberService` with `enqueue()`, `getAndIncrementCustomerNumber()`, and
`dequeue()`. All four DEQ call sites. JDBC SELECT + UPDATE on `STTESTER.CONTROL`.
**FRs covered:** FR-7, AC-7 (CONTROL SQL), AC-20

### Epic 7: Insert Customer Record (DB2 CUSTOMER Write)
`CustomerDbService.insertCustomer()` — populate `HostCustomerRow`, date integer encoding,
JDBC INSERT CUSTOMER (17 columns), silent-return on SQL failure.
**FRs covered:** FR-8, AC-7 (CUSTOMER INSERT), AC-11 (date encoding)

### Epic 8: PROCTRAN Audit Write and Notifying-Abort Path
`ProctranDbService.insertProctran()` — time/date population, 40-byte descriptor assembly,
JDBC INSERT PROCTRAN (9 columns), and the full notifying-abort path (populate ABNDINFO →
DEQ → LINK ABNDPROC → ABEND HWPT).
`AbndprocDelegate.linkAbndproc()` returning `void` (ADR-11).
**FRs covered:** FR-9, AC-1 (error-path ordering), AC-10 (ABNDPROC void return), AC-11 (PROCTRAN date format)

### Epic 9: Exception Class, Named Factories, and ABEND Code
`CrecustException` with 17 named fail-code constants, 16 named static factory methods,
`ABEND_CODE_HWPT` constant, and COBOL-grounding documentation for Rule 16.
**FRs covered:** FR-11, AC-2, AC-3, AC-4, AC-13, AC-16

## Epic 1: Project Setup and Build Infrastructure

**Goal:** Java modernisation of the CRECUST COBOL program. Scaffold the Maven project, create
the `pom.xml` with all required dependencies, establish the package directory structure, and
generate the shared infrastructure utility library (separate project) so that every subsequent
epic has a compilable foundation to build on.

---

### Story 1.1: Maven project `pom.xml` is created at the correct path

As a Java developer,
I want a complete Maven `pom.xml` at `/Users/arnold/github.com/IBM/Bank-of-Z/crecust-java/pom.xml`,
So that the project compiles against Java 21, JCICS, JZOS, Lombok, and SLF4J without manual
dependency resolution.

**Acceptance Criteria:**

**Given** the project root `/Users/arnold/github.com/IBM/Bank-of-Z/crecust-java/` is initialized,
**When** the developer runs `mvn compile` from that directory,
**Then** the build succeeds with zero compilation errors on a clean environment.

**And** the `pom.xml` declares:
- `groupId`: `com.ibm.cics.botz`, `artifactId`: `crecust`, `version`: `1.0.0-SNAPSHOT`
- `maven.compiler.source` and `maven.compiler.target` both `21`
- JCICS: `com.ibm.cics:com.ibm.cics.server` (provided scope)
- JZOS: `com.ibm.jzos:ibm.jzos` (for EBCDIC encoding in serializers)
- Lombok: `org.projectlombok:lombok` (annotation-processor and provided scope)
- SLF4J API: `org.slf4j:slf4j-api`

**And** no Spring, CDI, Liberty, or non-JCICS runtime dependency appears in the `pom.xml`.

**And** the six package directories under `src/main/java/com/ibm/cics/botz/crecust/` exist
(root, service, model, db, serializer, exception) per ADR-2.

**Source:** PREMIERE_P010 (lines 407–520) — entry point requiring the full dependency set.

---

### Story 1.2: Shared infrastructure utility library is generated

As a Java developer,
I want the four shared infrastructure types (`ByteArraySerializer<T>`, `ByteArraySerializable<T>`,
`Settable<T>`, `Lists`) to exist in a compiled shared library JAR,
So that Epic 2 serializer classes can implement `ByteArraySerializer<T>` without redeclaring
contracts inline (ADR-A, Rule 15).

**Acceptance Criteria:**

**Given** a separate Maven project (e.g., `botz-cics-common`) containing the four infrastructure
types as separate `.java` files,
**When** that JAR is added as a dependency in `crecust/pom.xml`,
**Then** `CrecustareaSerializer implements ByteArraySerializer<CrecustCommarea>` compiles
without error.

**And** each of the four types is in its own `.java` file (Rule 17 — one top-level type per file).

**And** `ByteArraySerializer<T>` declares at minimum:
- `byte[] toBytes(T model)` — serialize model → byte array
- `T fromBytes(byte[] bytes)` — deserialize byte array → model
- `T fromBytes(byte[] bytes, int offset, int length)` — partial-copy overload (for EIBCALEN)

**And** `ByteArraySerializable<T>` declares `void set(byte[] bytes)` and `byte[] toBytes()`.

**And** no `ByteArraySerializer` logic is re-implemented inline anywhere in the `crecust` project
(Rule 15 — delegate to canonical serializer; never inline byte-packing in service classes).

**Source:** Architecture ADR-A, PRD SER-1.

## Epic 2: Data Models and Shared Serialization Contracts

**Goal:** Author all Java model classes, REDEFINES-derived classes, DB2 host-variable rows, and
the three canonical `ByteArraySerializer` classes. This establishes the shared type system
that every subsequent service epic depends on.

**Dependencies:** Epic 1 (shared library JAR on classpath).

---

### Story 2.1: Core model classes and Lombok annotations

As a Java developer,
I want the primary model classes annotated with Lombok, each in its own `.java` file,
So that service classes in later epics have typed, builder-enabled data objects to work with.

**Acceptance Criteria:**

**Given** the model classes are authored in package `com.ibm.cics.botz.crecust.model`,
**When** `mvn compile` is run,
**Then** all classes compile without error.

**And** the following classes exist, each in its own file (Rule 17), annotated with
`@Data @Builder @AllArgsConstructor @NoArgsConstructor` (ADR-12):
- `CrecustCommarea` — 25 fields, 399 bytes (DS-1), implements `ByteArraySerializable<CrecustCommarea>`
- `CustomerRecord` — 23 fields, 397 bytes (DS-2)
- `ProctranData` — core fields plus `byte[]` for `procTranDesc` (DS-3), implements `ByteArraySerializable<ProctranData>`
- `WsChildData` — 399 bytes = 397-byte customer mirror + `wsChildSuccess` + `wsChildFailCode` (DS-7), implements `ByteArraySerializable<WsChildData>`
- `AbndInfoRec` — 12 fields ≈673 bytes (DS-8), implements `ByteArraySerializable<AbndInfoRec>`
- `CustomerControlRecord` — from CUSTCTRL.cpy, includes FILLER padding (DS-9)
- `NcsCustNoStuff` — 6 fields (DS-12)

**And** DB2 host-variable row classes exist in package `com.ibm.cics.botz.crecust.db`,
each in its own file:
- `HostCustomerRow` — 17 fields; three date fields are `int` (YYYYMMDD encoding)
- `HostProctranRow` — 9 fields; `hvProctranDate` is `String` (`DD.MM.YYYY`); `hvProctranAmount` is `BigDecimal`
- `HostControlRow` — 3 fields

**And** no model class is missing from the class list (Rule 13 — no dead classes; Rule 14 —
cross-program completeness). `CustomerControlRecord` is present even though the CRECUST
PROCEDURE DIVISION does not call it directly (Rule 14: "Defined here; consumed by suite").

**Source:** PREMIERE_P010 (lines 407–520)

---

### Story 2.2: Cat 3a REDEFINES groups — record classes with PIC getter/setter

As a Java developer,
I want the six Cat 3a REDEFINES groups modelled as record classes with getter/setter access
on the parent class,
So that the PIC canonical field and the structured group view are usable simultaneously
without raw byte manipulation (Rule: Cat 3a — one record sibling + PICs).

**Acceptance Criteria:**

**Given** the following six Cat 3a anchor variables and their record siblings,
**When** a developer sets `procTranDate = 20261001` (an `int`),
**Then** `getProcTranDateGrp()` returns a `ProcTranDateGrp` with `year=2026`, `month=10`, `day=1`.

**And** all six record classes exist in `com.ibm.cics.botz.crecust.model`, each in its own file:

| Record Class | Canonical PIC Field | Parent Class |
|---|---|---|
| `ProcTranDateGrp` (year/month/day `int`) | `procTranDate int` | `ProctranData` |
| `ProcTranTimeGrp` (hours/mins/secs `int`) | `procTranTime int` | `ProctranData` |
| `WsTimeNowGrp` (hh/mm/ss `int`) | `wsTimeNow int` | local in `CrecustService` |
| `WsOrigDateGrp` (dd/sep/mm/sep/yyyy int + FILLER) | `wsOrigDate String` | local in `CrecustService` |
| `CustomerKy2` (sortCode `String`, custNumber `String`) | `customerKy2Bytes byte[]` | local in `CustomerNumberService` |
| `WsCicstsLevelNumGrp` (vv/rr/mm `int`) | `wsCicstslevel String` | local in `CrecustService` |

**And** for each group, round-trip conversion is correct: setting via the group's setter and
reading via the canonical PIC getter returns the original value, and vice versa (AC-19.2).

**And** FILLER fields within `WsOrigDateGrp` (the separator characters `'/'`) are preserved
as constants, not as settable Java fields.

**Source:** PREMIERE_P010 (lines 407–520)

---

### Story 2.3: `PROC-TRAN-DESC` REDEFINES group (Cat 4a, Pattern B — shared memory)

As a Java developer,
I want `ProcTranDescBase` and its five concrete subclasses modelled with a shared `byte[]`
backing store,
So that the non-mutually exclusive views of the 40-byte PROCTRAN descriptor are all valid
simultaneously (Cat 4a Pattern B, AC-19.1).

**Acceptance Criteria:**

**Given** `ProcTranDescBase` is an abstract class holding `private final byte[] data`
(40 bytes),
**When** a `ProcTranDescCrecus` subclass sets `sortCode = "987654"`,
**Then** `new ProcTranDescCrecus(base.getData()).getSortCode()` returns `"987654"` —
proving both views access the same underlying bytes.

**And** `ProcTranDescBase` exists in `com.ibm.cics.botz.crecust.model` (abstract class,
holds `byte[]`, no Lombok `@Data`).

**And** the following five concrete subclasses exist, each in its own file, each with
**no instance fields** other than inherited ones — all getters/setters encode/decode
from the base `byte[]` using byte offsets derived from the COBOL PIC clauses:
- `ProcTranDescXfr`
- `ProcTranDescDelacc`
- `ProcTranDescCreacc`
- `ProcTranDescDelcus`
- `ProcTranDescCrecus` — this view is actively written by CRECUST

**And** all five field-offset constants are declared in a single constants class
(no constant is redeclared across classes — Rule 15 / NFR-6).

**And** no subclass declares `private` instance fields beyond what `ProcTranDescBase` holds.
A code review confirms this (AC-19.1).

**Source:** PREMIERE_P010 (lines 407–520)

---

### Story 2.4: `PROC-TRAN-EYE-CATCHER` and `CASE-1/CASE-2 CONDITION-ID` REDEFINES groups
(Cat 4a, Pattern A — mutually exclusive)

As a Java developer,
I want mutually exclusive REDEFINES groups modelled as a common interface plus separate
concrete classes (Pattern A),
So that each view holds only its own typed fields and the shared-byte-array complexity of
Pattern B is avoided where it is not needed (ADR-7, AC-19.3).

**Acceptance Criteria:**

**Given** `PROC-TRAN-EYE-CATCHER` is mutually exclusive with `PROC-TRAN-LOGICAL-DELETE-AREA`
(ADR-7),
**When** a `ProcTranValid` instance is created with `eyecatcher = "PRTR"`,
**Then** `procTranValid.isValid()` returns `true`.

**And** `ProcTranValid.isValid()` returns `false` for any value other than `"PRTR"`.

**And** `ProcTranLogicalDeleteArea.isLogicallyDeleted()` returns `true` if and only if
`logicalDeleteFlag == (byte) 0xFF`.

**And** the interface `ProcTranEyeCatcher` and two concrete classes (`ProcTranValid`,
`ProcTranLogicalDeleteArea`) each exist in their own `.java` file in
`com.ibm.cics.botz.crecust.model`.

**And** `ProcTranValid` holds `String eyecatcher` (4 bytes); `ProcTranLogicalDeleteArea`
holds `byte logicalDeleteFlag` and 3-byte FILLER. No shared `byte[]` base class exists
for this group (Pattern A — independent typed fields).

**And** the `ConditionId` interface plus `Case1ConditionId` (severity + msgNo) and
`Case2ConditionId` (classCode + causeCode) each exist in their own `.java` file in
`com.ibm.cics.botz.crecust.model`. `FcConditionToken` is **not generated** — CEEDAYS and
CEELOCT are replaced by `java.time`; generating the CEE condition-token wrapper would produce
dead code (Rule 13, G2 resolved 2026-10-01).

**Source:** PREMIERE_P010 (lines 407–520)

---

### Story 2.5: Three canonical `ByteArraySerializer` classes (ADR-A, Rule 5 / Rule 15)

As a Java developer,
I want three canonical serializer classes — `CrecustareaSerializer`, `WsChildDataSerializer`,
and `AbndInfoRecSerializer` — each implementing `ByteArraySerializer<T>`,
So that all byte-array ↔ model conversions go through a single authoritative class per
layout and no service class re-implements byte-offset arithmetic (Rule 15).

**Acceptance Criteria:**

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
- `AbndInfoRecSerializer` — ≈673 bytes (ABNDINFO-REC)

**And** each serializer implements `ByteArraySerializer<T>` from the shared library (Story 1.2).

**And** every field offset and length in each serializer is declared as a named
`private static final int` constant — no bare integer literals in offset arithmetic (NFR-6,
Rule 5, AC-5.2).

**And** a bounds check precedes every array write: if `buffer.length < offset + fieldLength`,
an `IllegalArgumentException` is thrown naming the field and required minimum length
(AC-5.3, Rule 5).

**And** no field-width constant is redeclared outside the owning serializer or its paired
layout-constants class (AC-5.4, Rule 15 / AC-15.2).

**And** no stub or redirect file exists — every `.java` file in `serializer/` contains a real
class (AC-5.6, Rule 5).

**And** the partial-copy overload `fromBytes(byte[] bytes, int offset, int length)` is
implemented in `CrecustareaSerializer` to support the EIBCALEN guard (Story 3.3 depends on
this).

**Source:** PREMIERE_P010 (lines 407–520)

## Epic 3: CICS Entry Point, Commarea Binding, and EIBCALEN Partial-Copy

**Goal:** Author the `Crecust` entry-point class, the `CrecustService` orchestrator skeleton,
CICS version check, time/date population helpers, and the shared exit method. After this epic the
program is invocable end-to-end (even if service stubs return immediately).

**Dependencies:** Epic 1, Epic 2 (model classes and serializers).

---

### Story 3.1: `Crecust` entry-point class — commarea receive, delegate, and return

As a CICS caller,
I want the Java entry-point to receive the 399-byte commarea, delegate to `CrecustService`,
serialize the result back, and return from `main()` to exit back to the CICS caller,
So that the Java program is callable from any CICS program using the same 399-byte contract
as the original COBOL `CRECUST` (FR-1, Goal G-2).

**Acceptance Criteria:**

**Given** the `Crecust` class is annotated with `@CICSProgram("CRECUST")` and extends the
JCICS `AbstractProgram` (or equivalent entry-point mechanism),
**When** the CICS container invokes `Crecust.main()`,
**Then** `CrecustareaSerializer.fromBytes(commareaBytes)` is called on the raw commarea array
before any business logic executes.

**And** `CrecustareaSerializer.toBytes(commarea)` writes the updated commarea back into the
raw byte array after `CrecustService.execute()` returns.

**And** `Crecust.main()` returns normally at a single shared exit point (Java `return` statement
maps to `EXEC CICS RETURN`; `Task.getTask().returnToCaller()` does not exist in the JCICS API
and must NOT be called) — mirroring `GET-ME-OUT-OF-HERE`.

**And** `Crecust` holds no business logic — it calls `CrecustService.execute()` and returns.
The class body is ≤ 20 lines excluding imports and Javadoc.

**And** a `private static final Logger LOGGER` (SLF4J) is declared in `Crecust`; entry and
exit are logged at `INFO` level.

**Source:** PREMIERE_P010 (lines 407–520)

---

### Story 3.2: `CrecustService` orchestrator skeleton with full flow wiring

As a Java developer,
I want `CrecustService.execute(CrecustCommarea)` to call each service in the correct sequence
mirroring the COBOL `PREMIERE_P010` flow,
So that substituting a real service implementation behind a stub is sufficient to activate
each new epic's code without changing orchestration logic.

**Acceptance Criteria:**

**Given** stub implementations of all downstream services return immediately without error,
**When** `CrecustService.execute(commarea)` is called with a valid commarea,
**Then** the calls occur in this exact order (mirroring PREMIERE_P010):
1. `validationService.validateTitle(commarea)` — returns on fail-code `'T'`
2. `validationService.validateDateOfBirth(commarea)` — returns on fail-codes `'O'`/`'Z'`/`'Y'`
3. `populateTimeAndDate()` — inline JCICS ASKTIME + FORMATTIME
4. `creditCheckService.performCreditCheck(commarea)` — returns on credit-check failure
5. `customerNumberService.enqueue(commarea)` — returns on fail-code `'3'`
6. `customerNumberService.getAndIncrementCustomerNumber(commarea)` — returns on fail-code `'4'`
7. `customerDbService.insertCustomer(commarea, customerRecord, hostCustomerRow)` — returns on `'1'`
8. `proctranDbService.insertProctran(...)` — notifying-abort on failure
9. `customerNumberService.dequeue(commarea)` — final DEQ (success path)
10. Set `COMM-SUCCESS='Y'`, `COMM-FAIL-CODE=' '`, `COMM-EYECATCHER='CUST'`

**And** `CrecustService` holds no per-request state in instance fields (NFR-5 — stateless).

**And** `CrecustService` is in package `com.ibm.cics.botz.crecust.service`.

**And** a `private static final Logger LOGGER` (SLF4J) is declared; key milestones logged at
`INFO`, per-step detail at `DEBUG` (NFR-2.5). No PII or financial values are logged (NFR-2.6).

**Source:** PREMIERE_P010 (lines 407–520)

---

### Story 3.3: EIBCALEN partial-copy guard in `CrecustareaSerializer`

As a Java developer,
I want `CrecustareaSerializer` to handle a commarea shorter than 399 bytes correctly,
So that callers with a partial commarea do not trigger an `ArrayIndexOutOfBoundsException`
and remaining fields default to zero/empty (FR-1.2, Rule 6, AC-6).

**Acceptance Criteria:**

**Given** the CICS container passes a commarea of `n < 399` bytes (`EIBCALEN < 399`),
**When** `CrecustareaSerializer.fromBytes(bytes, 0, n)` is called,
**Then** only the first `n` bytes are read; all fields beyond byte `n` remain at Java default
values (zero for numerics, empty string for `String` fields).

**And** a unit test passes a 100-byte commarea and asserts:
- `commSortcode` is populated (bytes 4–9 are within range)
- `commCreditScore` is `0` (byte offset 280 is beyond 100)
- No `ArrayIndexOutOfBoundsException` is thrown

**And** the full-length overload `fromBytes(byte[] bytes)` still behaves correctly for a
399-byte commarea (no regression).

**And** the constant `MAX_COMMAREA_LENGTH = 399` is declared as a named `private static final int`
in `CrecustareaSerializer` (Rule 8 — no magic number).

**Source:** PREMIERE_P010 (lines 407–520)

---

### Story 3.4: `CrecustService.populateTimeAndDate()` — Java native date/time

As a Java developer,
I want `CrecustService.populateTimeAndDate()` to obtain the current date and time via
`LocalDateTime.now()` and format it into the date/time working-storage equivalents,
So that `WS-ORIG-DATE` (`DD/MM/YYYY`), `WS-ORIG-DATE-GRP-X` (`DD.MM.YYYY`), and the PROCTRAN
time field (`HHMMSS`) are populated correctly before the credit-check loop (FR-4, Rule 11).

**Note:** `EXEC CICS ASKTIME` maps to `LocalDateTime.now()`. `Task.getTask().getAbstime()` does
not exist in the JCICS API and must NOT be used.

**Acceptance Criteria:**

**Given** `LocalDateTime.now()` returns the current system date and time,
**When** `populateTimeAndDate()` is called,
**Then** `wsOrigDate` is a `String` in format `DD/MM/YYYY` (10 chars, slash separators), where
`DD`, `MM`, `YYYY` match today's date — verified by a test that controls the clock via
`Clock` injection or a test-scoped `LocalDateTime`.

**And** `wsOrigDateGrp.dd`, `wsOrigDateGrp.mm`, `wsOrigDateGrp.yyyy` are populated from the
`DD/MM/YYYY` string components (getter on `WsOrigDateGrp` round-trips correctly).

**And** `wsOrigDateGrpX` (`DD.MM.YYYY`, dot-separated) is assembled from the component
fields `WS-ORIG-DATE-DD`, `WS-ORIG-DATE-MM`, `WS-ORIG-DATE-YYYY` with `'.'` separators
(FR-4.4) — test confirms the dot-separated format.

**And** `wsTimeNow` (and `WsTimeNowGrp`) are populated in `HHMMSS` format (no separators)
— test confirms `wsTimeNowGrp.hh * 10000 + wsTimeNowGrp.mm * 100 + wsTimeNowGrp.ss`
equals `wsTimeNow`.

**And** `DateTimeFormatter` patterns used are exactly:
- `DD/MM/YYYY` → formatter `"dd/MM/yyyy"` (4-digit year, Rule 11)
- `HHMMSS` → formatter `"HHmmss"` (no separators, Rule 11)

**And** the Lilian epoch offset constant (`LILIAN_EPOCH_OFFSET`) is declared as a named
`private static final long` constant (Rule 8 — no magic number).

**Source:** PREMIERE_P010 (lines 407–520), POPULATE-TIME-DATE_PTD010 (lines 523–534)

---

### Story 3.5: `CrecustService` CICS version check — `WsCicstsLevelNumGrp` population

As a Java developer,
I want `CrecustService` to retrieve the CICS version using the JCICS Task inquiry API and
parse it into `WsCicstsLevelNumGrp` (VV/RR/MM),
So that the version-level check present in the COBOL source (FR-10) is preserved in Java
without using a commarea-derived field.

**Acceptance Criteria:**

**Given** `Task.getTask().getCicsVersion()` returns a version string such as `"730"`,
**When** `populateCicsVersion()` is called,
**Then** `wsCicstsLevelNumGrp.vv` equals `7`, `wsCicstsLevelNumGrp.rr` equals `3`,
`wsCicstsLevelNumGrp.mm` equals `0`.

**And** `wsCicstslevel` (the canonical `String` PIC field) equals `"730"`.

**And** `WsCicstsLevelNumGrp` round-trip is correct: setting `wsCicstslevel = "730"` and
calling `getWsCicstsLevelNumGrp()` returns `{vv=7, rr=3, mm=0}` (AC-19.2).

**And** a unit test mocks `Task.getTask()` and verifies correct VV/RR/MM extraction for at
least three version strings (e.g., `"520"`, `"730"`, `"650"`).

**Source:** PREMIERE_P010 (lines 407–520)

## Epic 4: Input Validation — Title and Date-of-Birth

**Goal:** Author `ValidationService` with `validateTitle()` and `validateDateOfBirth()`,
replacing the COBOL `CEEDAYS`/`CEELOCT` LE calls with `java.time` and implementing all
silent-return error paths for invalid title and DOB conditions.

**Dependencies:** Epic 1, Epic 2 (model classes), Epic 3 (orchestrator wiring).

---

### Story 4.1: `ValidationService.validateTitle()` — 11-value title check (FR-2)

As a CICS caller,
I want the Java program to reject a commarea whose `COMM-TITLE` is not in the accepted set,
So that `COMM-SUCCESS='N'` and `COMM-FAIL-CODE='T'` are returned without touching ENQ
or any DB2 table (FR-2 — silent-return path, Rule 1).

**Acceptance Criteria:**

**Given** a `CrecustCommarea` with `commTitle` set to a rejected value (e.g., `"Unknown   "`),
**When** `ValidationService.validateTitle(commarea)` is called,
**Then** `commarea.commSuccess` is `'N'` and `commarea.commFailCode` is `'T'`.

**And** for each of the 11 accepted values (`"Professor "`, `"Mr        "`, `"Mrs       "`,
`"Miss      "`, `"Ms        "`, `"Dr        "`, `"Drs       "`, `"Lord      "`,
`"Sir       "`, `"Lady      "`, `"          "`), `validateTitle()` does NOT set a fail-code.
A parameterised test covers all 11 values.

**And** the accepted titles are declared as a named constant collection — no raw string
literals at validation call sites (Rule 3 / NFR-2.4, FR-2.3).

**And** no ENQ, DEQ, or DB2 access occurs in `validateTitle()` or when it returns a fail-code
(FR-2.2, AC-1.3 — confirmed by a test that injects mock services and asserts zero calls).

**And** `ValidationService` is in package `com.ibm.cics.botz.crecust.service` and has no
instance state (NFR-5).

**Source:** PREMIERE_P010 (lines 407–520), CREDIT-CHECK_CC010 (lines 605–1131)

---

### Story 4.2: `ValidationService.validateDateOfBirth()` — `java.time` DOB checks (FR-3)

As a CICS caller,
I want the Java program to validate the date-of-birth using `java.time` (replacing CEEDAYS /
CEELOCT),
So that invalid, future, and out-of-range DOBs all set the correct fail-code without any
calls to COBOL LE routines (FR-3, Rule 11 — exact date semantics).

**Acceptance Criteria:**

**Given** `commDobYear < 1601`,
**When** `validateDateOfBirth(commarea)` is called,
**Then** `commFailCode` is `'O'` and `commSuccess` is `'N'` — test covers year `= 1600`.

**And** given an invalid calendar date (e.g., day=31, month=2, year=2000) that causes
`LocalDate.of()` to throw `DateTimeException`,
**When** `validateDateOfBirth(commarea)` is called,
**Then** `commFailCode` is `'Z'` and `commSuccess` is `'N'`.

**And** given a valid `LocalDate` where `today.getYear() - commDobYear > 150`,
**When** `validateDateOfBirth(commarea)` is called,
**Then** `commFailCode` is `'O'` and `commSuccess` is `'N'` — test covers age `= 151`.

**And** given a DOB whose Lilian day count exceeds today's Lilian day count (i.e., the DOB
is in the future),
**When** `validateDateOfBirth(commarea)` is called,
**Then** `commFailCode` is `'Y'` and `commSuccess` is `'N'`.

**And** a valid DOB (e.g., 15-Jun-1990) causes no fail-code to be set.

**And** the following named `private static final int` constants are declared in
`ValidationService` (or a shared constants class), each traceable to a PIC clause or
architectural decision (Rule 8, AC-8.1):
- `MIN_DOB_YEAR = 1601` (traced to FR-3.1)
- `MAX_CUSTOMER_AGE = 150` (traced to FR-3.4)
- `LILIAN_EPOCH_OFFSET` — the difference in days between the Lilian epoch (15 Oct 1582)
  and the Java epoch (1 Jan 1970), traced to the PRD FR-3.2 CEEDAYS replacement

**And** `LocalDate.of(commDobYear, commDobMonth, commDobDay)` is used for date construction
(replacing `CEEDAYS`), and `LocalDate.now()` is used for today (replacing `CEELOCT`)
(FR-3.2, FR-3.4).

**And** no `// UNRESOLVED` placeholder constants exist (all copybooks were resolved — AC-9.1).

**Source:** PREMIERE_P010 (lines 407–520), DATE-OF-BIRTH-CHECK_DOBC010 (lines 1556–1609)

## Epic 5: Asynchronous Credit-Check (JCICS AsyncService Loop)

**Goal:** Author `CreditCheckService` implementing the full JCICS async credit-check loop
(ADR-5) — channel/container creation, five async child-transaction launches, delay, FETCH ANY
NOSUSPEND result collection, score aggregation, and review-date computation. No COBOL helper
shim is generated (Q1 decision).

**Dependencies:** Epic 1, Epic 2 (model classes, `WsChildDataSerializer`), Epic 3 (orchestrator).

---

### Story 5.1: `CreditCheckService.putContainer()` — CICS channel and container creation

As a Java developer,
I want `CreditCheckService` to create a CICS channel and PUT the commarea bytes into
five containers (`CIPA`–`CIPE`) before launching child transactions,
So that each OCR1–OCR5 transaction receives the correct input data via its named container
(FR-5, ADR-5 — `EXEC CICS PUT CONTAINER` → `Channel.createContainer().put()`).

**Acceptance Criteria:**

**Given** a valid `CrecustCommarea`,
**When** `creditCheckService.putContainer(commarea, channelName, containerName)` is called,
**Then** `Task.createChannel(channelName)` is called and
`channel.createContainer(containerName).put(commareaBytes)` places the serialized commarea
bytes into the named container.

**And** the channel name constant `CIPCREDCHANN` and container name constants `CIPA` through
`CIPE` are declared as named `private static final String` constants — no raw string literals
at call sites (Rule 8 / NFR-2.4).

**And** a unit test mocks `Task.createChannel()` and verifies the correct channel name and
container contents for a known commarea.

**And** `CreditCheckService` holds no per-request state in instance fields (NFR-5).

**Source:** PREMIERE_P010 (lines 407–520), CREDIT-CHECK_CC010 (lines 605–1131)

---

### Story 5.2: `CreditCheckService.runChildTransaction()` — async launch of OCR1–OCR5

As a Java developer,
I want `CreditCheckService` to launch all five OCR credit-check transactions using
`AsyncServiceImpl.runTransactionId(tranId, channel)` (replacing `EXEC CICS RUN TRANSID`),
So that the five child credit-check transactions execute asynchronously in parallel (FR-5,
ADR-5 — `EXEC CICS RUN TRANSID` → `AsyncServiceImpl.runTransactionId(tranId, channel)`).

**Note:** `AsyncServiceImpl.run()` is NOT the correct method. Use `AsyncServiceImpl.runTransactionId(tranId, channel)`.

**Acceptance Criteria:**

**Given** five transaction IDs `OCR1`–`OCR5` and a named channel `CIPCREDCHANN`,
**When** `runChildTransaction(transId, channelName)` is called five times,
**Then** `AsyncServiceImpl.runTransactionId(transId, channelName)` is called exactly five times
with the correct transaction IDs.

**And** the child token returned by each `runTransactionId()` call is stored in `WS-CHILD-TOKENS`
equivalent Java structures for later retrieval.

**And** the transaction ID constants `OCR1`–`OCR5` are declared as named `private static final
String` constants (Rule 8 / NFR-2.4).

**And** after launching all five transactions, `Thread.sleep(3000)` (3-second delay, replacing
`EXEC CICS DELAY FOR SECONDS(3)`) is called exactly once before any `FETCH ANY` call (ADR-5).
`Task.getTask().delay()` does NOT exist in JCICS and must NOT be used.

**And** a unit test confirms five `runTransactionId()` calls followed by one `Thread.sleep(3000)`
call, in that order.

**Source:** PREMIERE_P010 (lines 407–520), CREDIT-CHECK_CC010 (lines 605–1131)

---

### Story 5.3: `CreditCheckService.fetchAny()` — result collection and score aggregation

As a Java developer,
I want `CreditCheckService` to collect results from completed child transactions using
`AsyncServiceImpl.getAny(NOSUSPEND)` and aggregate credit scores,
So that the credit score average and the fail-code for each error condition are correctly
set in the commarea (FR-5, ADR-5 — `EXEC CICS FETCH ANY NOSUSPEND` → `AsyncServiceImpl.getAny(NOSUSPEND)`).

**Acceptance Criteria:**

**Given** one or more child transactions have completed with credit-score results,
**When** `fetchAny()` is called in a loop until all five children are collected or a failure
occurs,
**Then** each successful result's credit score is read from the child container via
`WsChildDataSerializer.fromBytes(containerBytes)` and accumulated in `wsActualCsScr` and
`wsRetrievedCnt`.

**And** the following fail-codes are set when the corresponding FETCH ANY result occurs
(FR-5.4), with `COMM-SUCCESS='N'` and an immediate return on each:
- `NOTFINISHED` with zero retrieved → `'C'`
- `INVREQ` → `'D'`
- `ABEND` completion → `'F'`
- `SECERROR` → `'G'`
- Any other non-success → `'H'`

**And** all fail-code values are set via named factory methods on `CrecustException` — no
raw character literals at FETCH ANY check sites (Rule 3, AC-3.1).

**And** when `wsRetrievedCnt > 0`, `commCreditScore` is set to
`wsActualCsScr / wsRetrievedCnt` (integer average).

**And** a unit test with two completed children (scores 700, 800) verifies
`commCreditScore = 750`.

**And** `WsChildDataSerializer` is used for ALL container deserialization — no inline
byte-reading in `CreditCheckService` (Rule 15 — serializer delegation, AC-15.1).

**Source:** PREMIERE_P010 (lines 407–520), CREDIT-CHECK_CC010 (lines 605–1131)

---

### Story 5.4: `CreditCheckService` review-date computation (FR-6)

As a Java developer,
I want `CreditCheckService` to compute the credit-score review date and write it into
the commarea in `DDMMYYYY` byte layout,
So that `COMM-CS-REVIEW-DATE` contains the correct review date on both the success path
(today + 1–20 random days) and the failure path (today), and both paths produce identical
byte layouts (FR-6, AC-11.5).

**Acceptance Criteria:**

**Given** at least one credit-check agency replied successfully,
**When** `computeReviewDate(commarea, taskNumber)` is called,
**Then** `commCsReviewDay`, `commCsReviewMonth`, `commCsReviewYear` are populated with a date
between tomorrow and today+20 (inclusive), using
`today.plusDays( ((21 - 1) * random(eibtaskn)) + 1 )` (FR-6.1).

**And** given zero agencies replied (credit-check failure),
**When** `computeReviewDate(commarea, taskNumber)` is called,
**Then** `commCsReviewDay`, `commCsReviewMonth`, `commCsReviewYear` equal today's components
(FR-6.2).

**And** in both cases the `COMM-CS-REVIEW-DATE` byte layout in the serialized commarea is
`DDMMYYYY` (8 bytes, no separators) — confirmed by `CrecustareaSerializer.toBytes(commarea)`,
inspecting bytes at the `COMM-CS-REVIEW-DATE` offset (AC-11.5).

**And** the review-date upper bound constant `REVIEW_DATE_MAX_DAYS = 21` is a named constant
(Rule 8 / AC-8.1 — traced to FR-6.1 `(21 - 1)`).

**And** a unit test with a seeded `eibtaskn` verifies the review date is within [today+1,
today+20].

**Source:** PREMIERE_P010 (lines 407–520), CREDIT-CHECK_CC010 (lines 605–1131)

## Epic 6: Customer Number Allocation (ENQ / CONTROL / DEQ)

**Goal:** Author `CustomerNumberService` with `enqueue()`, `getAndIncrementCustomerNumber()`,
and `dequeue()`, implementing all four DEQ call sites that mirror the COBOL Fan-In-4 pattern.

**Dependencies:** Epic 1, Epic 2 (model classes, `HostControlRow`), Epic 3 (orchestrator).

---

### Story 6.1: `CustomerNumberService.enqueue()` — JCICS NameResource ENQ (FR-7.1)

As a Java developer,
I want `CustomerNumberService.enqueue()` to call `NameResource.enqueue()` on the 16-byte
resource name,
So that no two CICS tasks can allocate the same customer number concurrently (FR-7.1,
silent-return path for ENQ failure — fail-code `'3'`).

**Acceptance Criteria:**

**Given** a valid `CrecustCommarea` with `commSortcode = "987654"`,
**When** `enqueue(commarea)` is called,
**Then** `NameResource.enqueue()` is called with the 16-byte resource name
assembled as `ncsCustNoActName("BANKZCUST") + commSortcode("987654") + "  "` (2 spaces),
padded/truncated to exactly 16 bytes.

**And** the resource name constant `NCS_ACT_NAME = "BANKZCUST"` and length constant
`ENQ_RESOURCE_LENGTH = 16` are declared as named `private static final` constants (Rule 8,
AC-8.1 — traced to FR-7.1 and `NCS-CUST-NO-ACT-NAME` PIC X(9)).

**And** if `NameResource.enqueue()` throws a CICS exception, `commSuccess` is set to `'N'`,
`commFailCode` is set using `CrecustException.FAIL_CODE_ENQ` (`'3'`), and the method
returns immediately — no DB2 access occurs (silent-return path, Rule 1).

**And** a unit test mocks `NameResource.enqueue()` to throw a CICS exception and asserts
`commFailCode = '3'` and zero DB2 calls.

**Source:** PREMIERE_P010 (lines 407–520), ENQ-NAMED-COUNTER_ENC010 (lines 541–556)

---

### Story 6.2: `CustomerNumberService.getAndIncrementCustomerNumber()` — JDBC CONTROL
SELECT + UPDATE (FR-7.2, FR-7.3)

As a Java developer,
I want `CustomerNumberService.getAndIncrementCustomerNumber()` to atomically read and
increment the CONTROL table counter via JDBC,
So that the new customer number is allocated and pushed to all four required targets (FR-7.4).

**Acceptance Criteria:**

**Given** the CONTROL table has `CONTROL_VALUE_NUM = 1234` for the customer-number name,
**When** `getAndIncrementCustomerNumber(commarea)` is called,
**Then** a JDBC `SELECT CONTROL_VALUE_NUM FROM STTESTER.CONTROL WHERE CONTROL_NAME = ?`
is executed with the correct bind parameter (AC-7.3 — one column, one bind parameter).

**And** the retrieved value is incremented by 1 (→ 1235) and a JDBC
`UPDATE STTESTER.CONTROL SET CONTROL_VALUE_NUM = ? WHERE CONTROL_NAME = ?` is executed
(AC-7.4 — one SET column, two bind parameters).

**And** the new number (1235) is pushed to all four targets in the commarea/model (FR-7.4,
AC-20.3): `commNumber`, `customerRecord.customerNumber`,
`customerKy2.custNumber`, and `ncsCustNoValue`.

**And** on SQL failure for either the SELECT or UPDATE:
1. `customerNumberService.dequeue(commarea)` is called (DEQ before return)
2. `commSuccess = 'N'`, `commFailCode = CrecustException.FAIL_CODE_CONTROL_SQL` (`'4'`) are set
3. The method returns — no ABEND (silent-return path, Rule 1)

**And** all JDBC resources are managed in `try-with-resources` (NFR-4).

**And** `DataSource` is obtained via JNDI `new InitialContext().lookup("jdbc/crecustDB2DS")`
(ADR-4 — no `@Resource`, no Spring injection).

**Source:** PREMIERE_P010 (lines 407–520), UPD-NCS_UN010 (lines 588–596),
GET-LAST-CUSTOMER-DB2_GLCD010 (lines 1477–1549)

---

### Story 6.3: `CustomerNumberService.dequeue()` — four DEQ call sites (FR-7.5, AC-20.2)

As a Java developer,
I want `CustomerNumberService.dequeue()` to release the CICS named-counter ENQ and
to be called from exactly four call sites mirroring the COBOL Fan-In-4 pattern,
So that the CICS ENQ is always released regardless of which error path exits (FR-7.5,
NFR-4 — resource management).

**Acceptance Criteria:**

**Given** `NameResource.enqueue()` has been successfully called,
**When** any of the four completion paths is reached,
**Then** `NameResource.dequeue()` is called at that path:
1. After successful PROCTRAN INSERT (happy path — Story 8.1)
2. After INSERT CUSTOMER failure (Story 7.2 silent-return path)
3. After INSERT PROCTRAN failure (Story 8.2 notifying-abort, before ABEND)
4. After SELECT/UPDATE CONTROL failure (Story 6.2 silent-return path)

**And** a code-review or test confirms exactly four and only four `dequeue()` call sites
exist in the codebase (AC-20.2).

**And** if `NameResource.dequeue()` throws a CICS exception, `commSuccess` is set to `'N'`,
`commFailCode` is set using `CrecustException.FAIL_CODE_DEQ` (`'5'`), and the method returns
(silent-return, Rule 1).

**And** the dequeue method is `dequeue(CrecustCommarea)` returning `void` from
`CustomerNumberService` in package `com.ibm.cics.botz.crecust.service`.

**Source:** PREMIERE_P010 (lines 407–520), DEQ-NAMED-COUNTER_DNC010 (lines 563–581),
WRITE-CUSTOMER-DB2_WCD010 (lines 1139–1306), WRITE-PROCTRAN-DB2_WPD010 (lines 1321–1458)

## Epic 7: Insert Customer Record (DB2 CUSTOMER Write)

**Goal:** Author `CustomerDbService.insertCustomer()` — populate `HostCustomerRow`, encode
three date fields as YYYYMMDD integers, execute JDBC INSERT CUSTOMER (17 columns), and handle
the silent-return failure path (fail-code `'1'`).

**Dependencies:** Epic 1, Epic 2 (model classes, `HostCustomerRow`), Epic 3, Epic 6 (DEQ on
failure).

---

### Story 7.1: `CustomerDbService` — populate `HostCustomerRow` from commarea (FR-8.1, FR-8.2)

As a Java developer,
I want `CustomerDbService` to populate `HostCustomerRow` from the commarea and
`CustomerRecord` fields with correct date integer encoding,
So that the DB2 INSERT receives correctly typed host variables including `HV-CUSTOMER-DOB`,
`HV-CUSTOMER-CREATE-DATE`, and `HV-CUSTOMER-CS-REVIEW-DATE` as YYYYMMDD integers (FR-8.2,
AC-11.4).

**Acceptance Criteria:**

**Given** a `CrecustCommarea` with DOB fields `commDobYear=1990`, `commDobMonth=6`,
`commDobDay=15`,
**When** `populateHostCustomerRow(commarea, customerRecord, hostCustomerRow)` is called,
**Then** `hostCustomerRow.hvCustomerDob` equals `19900615`
(computed as `year * 10000 + month * 100 + day`).

**And** `hvCustomerCreateDate` and `hvCustomerCsReviewDate` are encoded with the same formula
(AC-11.4).

**And** `hvCustomerEyecatcher` is set to `"CUST"` before population (FR-8.4).

**And** `hvCustomerCreditScore` is of type `short` (SMALLINT — traced to the CUSTOMER table
schema), not `int`.

**And** all 17 `HostCustomerRow` fields are populated in the order that matches the DB2
INSERT column list (PE-3 order): eyecatcher, sortcode, number, title, first_name, last_name,
dob_int, phone, addr_line1, addr_line2, city, postcode, country, status, created_date_int,
credit_score, cs_review_date_int (AC-7.1 — column-order alignment).

**And** a unit test with a known commarea verifies `hvCustomerDob = 19900615`.

**Source:** PREMIERE_P010 (lines 407–520), WRITE-CUSTOMER-DB2_WCD010 (lines 1139–1306)

---

### Story 7.2: `CustomerDbService.insertCustomer()` — JDBC INSERT CUSTOMER 17 columns (FR-8.3)

As a Java developer,
I want `CustomerDbService.insertCustomer()` to execute a JDBC INSERT with all 17 CUSTOMER
columns and handle the SQL failure path as a silent-return (no ABEND),
So that a new customer row is written to DB2 and the commarea is updated with success or
fail-code `'1'` (FR-8.3, FR-8.5, AC-1.2).

**Acceptance Criteria:**

**Given** a populated `HostCustomerRow` and a JDBC `DataSource`,
**When** `insertCustomer(commarea, customerRecord, hostCustomerRow)` is called,
**Then** a JDBC `PreparedStatement` is executed with the INSERT statement containing all
17 columns in PE-3 order — a column-count assertion on the SQL string confirms 17 `?`
placeholders (AC-7.1).

**And** on success: `commSuccess = 'Y'`, `commFailCode = ' '` (single space), and
`commEyecatcher = "CUST"` are set (FR-8.6).

**And** on SQL failure (JDBC `SQLException`):
1. `customerNumberService.dequeue(commarea)` is called — second of four DEQ call sites
2. `commSuccess = 'N'`, `commFailCode = CrecustException.FAIL_CODE_INSERT_CUSTOMER` (`'1'`)
3. Method returns — **no** `Program.link("ABNDPROC")`, **no** ABEND (AC-1.2 — silent-return)

**And** all JDBC resources are managed in `try-with-resources` (NFR-4).

**And** `DataSource` is obtained via JNDI `"jdbc/crecustDB2DS"` (ADR-4 — no `@Resource`).

**And** `CustomerDbService` is in package `com.ibm.cics.botz.crecust.service` with no
instance state (NFR-5).

**And** a unit test mocks `DataSource` to throw `SQLException` and asserts:
`commFailCode = '1'`, `dequeue()` was called once, `linkAbndproc()` was never called.

**Source:** PREMIERE_P010 (lines 407–520), WRITE-CUSTOMER-DB2_WCD010 (lines 1139–1306)

## Epic 8: PROCTRAN Audit Write and Notifying-Abort Path

**Goal:** Author `ProctranDbService.insertProctran()` and `AbndprocDelegate.linkAbndproc()`
— the only notifying-abort path in the program. The PROCTRAN INSERT failure must follow the
exact ordering: populate ABNDINFO → DEQ → LINK ABNDPROC → ABEND HWPT (Rule 1).

**Dependencies:** Epic 1, Epic 2 (model classes, `AbndInfoRecSerializer`, `HostProctranRow`),
Epic 3, Epic 6 (`dequeue()`), Epic 9 (`CrecustException.ABEND_CODE_HWPT`).

---

### Story 8.1: `ProctranDbService.insertProctran()` — PROCTRAN assembly and JDBC INSERT

As a Java developer,
I want `ProctranDbService.insertProctran()` to assemble the PROCTRAN row, obtain the
current date and time via `LocalDateTime.now()`, and execute JDBC INSERT PROCTRAN (9 columns),
So that every customer creation writes a corresponding audit record to the PROCTRAN table
(FR-9, AC-7.2, AC-11.1, AC-11.2).

**Note:** `EXEC CICS ASKTIME` maps to `LocalDateTime.now()`. `Task.getTask().getAbstime()` does
not exist in the JCICS API and must NOT be used.

**Acceptance Criteria:**

**Given** a successful commarea and a JDBC `DataSource`,
**When** `insertProctran(commarea, storedSortcode, storedCustno, storedName, storedDob)` is
called,
**Then** `LocalDateTime.now()` is called to obtain the PROCTRAN timestamp (FR-9.1).

**And** `hvProctranDate` is formatted as `DD.MM.YYYY` (10 chars, dot separators) using
`DateTimeFormatter` pattern `"dd.MM.yyyy"` — test asserts the exact format string (AC-11.1,
Rule 11).

**And** `hvProctranTime` is formatted as `HHMMSS` (6 chars, no separators) using formatter
pattern `"HHmmss"` — test asserts the exact format string (AC-11.2, Rule 11).

**And** `hvProctranDesc` (40 bytes) is assembled with exact byte-offset reference
modification (FR-9.2):
- bytes 0–5 (6 bytes): `storedSortcode`
- bytes 6–15 (10 bytes): `storedCustno`
- bytes 16–29 (14 bytes): `storedName`
- bytes 30–39 (10 bytes): `storedDob` (`DD/MM/YYYY` format, slash-separated — AC-11.6)

**And** `hvProctranType = "OCC"`, `hvProctranAmount = BigDecimal.ZERO`,
`hvProctranAccNumber = "0000000000"` (zeros), `hvProctranRef` = `eibtaskn` formatted as
12-character string, `hvProctranEyecatcher = "PRTR"` are set (FR-9.3).

**And** the INSERT statement contains all 9 columns in PE-3 order — a column-count assertion
confirms 9 `?` placeholders (AC-7.2). `PROCTRAN_DATE` receives a `String` (not a
`java.sql.Date`).

**And** all JDBC resources are in `try-with-resources` (NFR-4).

**Source:** PREMIERE_P010 (lines 407–520), WRITE-PROCTRAN_WP010 (lines 1313–1314),
WRITE-PROCTRAN-DB2_WPD010 (lines 1321–1458), POPULATE-TIME-DATE2_PTD2010 (lines 1616–1628)

---

### Story 8.2: Notifying-abort path — `AbndprocDelegate` and PROCTRAN SQL failure sequence

As a Java developer,
I want the PROCTRAN SQL failure path to execute in the exact order: populate `AbndInfoRec`
→ DEQ → `AbndprocDelegate.linkAbndproc()` → `Task.getTask().abend(ABEND_CODE_HWPT)`,
So that the ABNDPROC COBOL handler receives all failure context before the task terminates,
and the ENQ is released before the ABEND (FR-9.5, Rule 1 — notifying-abort, AC-1.1).

**Note:** `EXEC CICS ASSIGN APPLID` maps to `Region.getAPPLID()` (static call). The chained
form `Task.getTask().getTask().getRegion()...getApplid()` does not exist in the JCICS API and
must NOT be used.

**Acceptance Criteria:**

**Given** a JDBC `SQLException` is thrown during `insertProctran()`,
**When** the SQL failure handler executes,
**Then** the following steps occur in this exact order (AC-1.1):
1. `abndInfoRec` is populated with: `abndCode = "HWPT"`, `abndSqlcode`, `abndApplid`
   (from `Region.getAPPLID()` — `Task.getTask().getTask().getRegion().getApplid()` does not exist),
   `abndTranid`, `abndProgram` (from `Task.getTask().getInvokingProgramName()`),
   `abndDate`, `abndTime`, `abndFreeform`,
   `abndUtimeKey` ← `LocalDateTime.now()` converted to epoch millis as `long`
   (`Task.getTask().getAbstime()` does not exist in JCICS),
   `abndTasknoKey` ← `Task.getTask().getTaskNumber()` formatted as 4-digit string
   (G4 resolved 2026-10-01 — these two fields form the ABND-VSAM-KEY group used as the DB key)
2. `customerNumberService.dequeue(commarea)` is called — third of four DEQ call sites
3. `abndprocDelegate.linkAbndproc(abndInfoRec)` is called
4. `Task.getTask().abend(CrecustException.ABEND_CODE_HWPT)` is called

**And** a unit test with a mock `DataSource` throwing `SQLException` verifies the exact
call order: `populateAbndInfo` before `dequeue` before `linkAbndproc` before `abend`
(AC-1.1 — swapping the order causes test failure).

**And** `AbndprocDelegate.linkAbndproc(AbndInfoRec)` returns `void` — no result DTO, no
return-code inspection (ADR-11, AC-10.1, Rule 10).

**And** `AbndprocDelegate.linkAbndproc()` calls `AbndInfoRecSerializer.toBytes(abndInfoRec)`
to produce the commarea bytes, then calls `new Program("ABNDPROC").link(commareaBytes)` —
delegating to the canonical serializer, never inline byte-packing (Rule 15, AC-15.1).

**And** the `AbndInfoRec` field `abndCode` is set to the named constant
`CrecustException.ABEND_CODE_HWPT = "HWPT"` — no bare `"HWPT"` string literal outside
the constant declaration (Rule 4, AC-4.2).

**And** `AbndprocDelegate` is in package `com.ibm.cics.botz.crecust.service` with a
`private static final Logger LOGGER` (SLF4J), logging the abort at `ERROR` level
including the `abndSqlcode` value.

**Source:** PREMIERE_P010 (lines 407–520), WRITE-PROCTRAN-DB2_WPD010 (lines 1321–1458),
DEQ-NAMED-COUNTER_DNC010 (lines 563–581)

## Epic 9: Exception Class, Named Factories, and ABEND Code

**Goal:** Author `CrecustException` with all 17 named fail-code constants, 16 named static
factory methods, and the `ABEND_CODE_HWPT` constant. Validate Rule 16 grounding: every
exception construct maps to a concrete COBOL error-handling structure.

**Dependencies:** Epic 1. This epic is written early but all stories in Epics 3–8 depend
on the exception class being in place — it should be the first epic implemented after
project setup and models.

---

### Story 9.1: `CrecustException` — 17 fail-code constants and 16 factory methods (FR-11)

As a Java developer,
I want `CrecustException` to declare every fail-code as a named constant and every error
path as a named static factory method,
So that no raw character or string literal for a fail-code or ABEND code ever appears at a
call site in any service class (Rule 3 / Rule 4, AC-3.1, AC-4.1).

**Acceptance Criteria:**

**Given** `CrecustException` is in package `com.ibm.cics.botz.crecust.exception` and
extends `RuntimeException`,
**When** any service class needs to set a fail-code,
**Then** it calls the corresponding named factory method — e.g.,
`CrecustException.invalidTitle()` sets `commFailCode = FAIL_CODE_INVALID_TITLE ('T')`.

**And** the following 17 `public static final char` (or `String`) constants are declared,
all traceable to FR-11:

| Constant | Value | Condition |
|---|---|---|
| `FAIL_CODE_INVALID_TITLE` | `'T'` | Title not in accepted list |
| `FAIL_CODE_CREDIT_ERROR` | `'G'` | Post-CC error or SECERROR |
| `FAIL_CODE_PUT_CONTAINER` | `'A'` | PUT CONTAINER failed |
| `FAIL_CODE_RUN_TRANSID` | `'B'` | RUN TRANSID failed |
| `FAIL_CODE_CC_NOTFINISHED` | `'C'` | FETCH ANY NOTFINISHED, no data |
| `FAIL_CODE_CC_INVREQ` | `'D'` | FETCH ANY INVREQ (no children) |
| `FAIL_CODE_GET_CONTAINER` | `'E'` | GET CONTAINER failed |
| `FAIL_CODE_CC_ABEND` | `'F'` | FETCH ANY completion = ABEND |
| `FAIL_CODE_CC_OTHER` | `'H'` | FETCH ANY completion = OTHER |
| `FAIL_CODE_INSERT_CUSTOMER` | `'1'` | INSERT CUSTOMER failed |
| `FAIL_CODE_ENQ` | `'3'` | ENQ failed |
| `FAIL_CODE_CONTROL_SQL` | `'4'` | SELECT or UPDATE CONTROL failed |
| `FAIL_CODE_DEQ` | `'5'` | DEQ failed |
| `FAIL_CODE_DOB_RANGE` | `'O'` | DOB year < 1601 or age > 150 |
| `FAIL_CODE_DOB_FUTURE` | `'Y'` | DOB in the future |
| `FAIL_CODE_CEEDAYS_FAIL` | `'Z'` | `LocalDate` construction failed |
| `FAIL_CODE_SUCCESS` | `' '` | Success (single space) |

**And** the ABEND code constant `public static final String ABEND_CODE_HWPT = "HWPT"` is
declared in `CrecustException` (Rule 4, AC-4.1).

**And** a grep of all generated source confirms no bare `"HWPT"` string literal outside
the constant declaration (AC-4.2 — zero occurrences expected).

**And** 16 named static factory methods are present (one per fail-code except
`FAIL_CODE_SUCCESS`): `invalidTitle()`, `creditError()`, `putContainerError()`,
`runTransidError()`, `ccNotFinished()`, `ccInvreq()`, `getContainerError()`, `ccAbend()`,
`ccOther()`, `insertCustomerFailed()`, `enqFailed()`, `controlSqlFailed()`, `deqFailed()`,
`dobRange()`, `dobFuture()`, `ceeDaysFailed()`.

**And** each factory method accepts a `String message` parameter and returns a new
`CrecustException` that carries `private final char failCode` (the fail-code constant value).
Factory methods **do not mutate the commarea**. Each catch block that catches a
`CrecustException e` is responsible for calling `commarea.setCommFailCode(e.getFailCode())`
and `commarea.setCommSuccess('N')` — no raw character literal at any call site (Rule 3,
G3 resolved 2026-10-01).

**And** `CrecustException.java` contains exactly one top-level type declaration (Rule 17,
AC-17.1).

**Source:** PREMIERE_P010 (lines 407–520)

---

### Story 9.2: Rule 16 grounding — no exception handler without COBOL construct (ADR-10)

As a Java developer,
I want a documented grounding map confirming that no exception-handler class exists without
a corresponding COBOL error-handling construct,
So that the single `CrecustException` class satisfies Rule 16 (ADR-10) and no spurious
handler class is generated as dead code (AC-16.1, AC-16.2).

**Acceptance Criteria:**

**Given** the architecture confirms no `HANDLE CONDITION`, no shared error-response-building
paragraph, and no cross-cutting catch policy exists in CRECUST,
**When** the full class list is reviewed,
**Then** no class named `*ExceptionHandler*` or `*ErrorHandler*` exists in the codebase.

**And** each of the following COBOL constructs maps to an inline catch block in the
corresponding Java service class (not to a separate handler class):
- Inline SQLCODE check in `WRITE-PROCTRAN-DB2_WPD010` → `ProctranDbService` catch block
  (the notifying-abort path)
- Inline SQLCODE check in `WRITE-CUSTOMER-DB2_WCD010` → `CustomerDbService` catch block
- Inline SQLCODE check in `GET-LAST-CUSTOMER-DB2_GLCD010` →
  `CustomerNumberService` catch block
- Inline EIBRESP/EIBRESP2 checks in `CREDIT-CHECK_CC010` → `CreditCheckService` catch blocks
- Inline EIBRESP checks in `ENQ-NAMED-COUNTER_ENC010` / `DEQ-NAMED-COUNTER_DNC010` →
  `CustomerNumberService` catch blocks

**And** no catch block's sole behaviour is to re-throw the exception — every catch block
performs at least one side effect (set fail-code, call DEQ, or call linkAbndproc) before
re-throwing or returning (AC-16.2 — handlers that only re-throw are a defect).

**And** this grounding map is documented in a JavaDoc comment on `CrecustException` class
itself (`@since 1.0.0`, `@author`, with the COBOL-construct mapping described in the
class-level Javadoc).

**And** all service classes that catch exceptions delegate to named constants or factory
methods from `CrecustException` — no inline construction of error responses that duplicate
handler logic (Rule 2 — AC-2.1).

**Source:** PREMIERE_P010 (lines 407–520), WRITE-PROCTRAN-DB2_WPD010 (lines 1321–1458),
WRITE-CUSTOMER-DB2_WCD010 (lines 1139–1306), CREDIT-CHECK_CC010 (lines 605–1131),
ENQ-NAMED-COUNTER_ENC010 (lines 541–556), DEQ-NAMED-COUNTER_DNC010 (lines 563–581)
