# Epic 9 Context: Exception Class, Named Factories, and ABEND Code

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Author `CrecustException` — the single exception class for the entire CRECUST modernisation. It
must declare all 17 named fail-code constants (type `char`), the `ABEND_CODE_HWPT` string
constant, and 16 named static factory methods (one per error path, excluding success). This class
is a foundational dependency: every story in Epics 3–8 throws `CrecustException` instances
created via these factories. Epic 9 should be implemented immediately after Epic 1 (project setup)
so the class is present when any service story begins.

## Stories

- Story 9.1: `CrecustException` — 17 fail-code constants and 16 factory methods (FR-11)
- Story 9.2: Rule 16 grounding — no exception handler without COBOL construct (ADR-10)

## Requirements & Constraints

- All 17 fail-code values from FR-11 must be `public static final char` constants (except
  `FAIL_CODE_SUCCESS = ' '` which is also `char`). Values: `T`, `G`, `A`, `B`, `C`, `D`, `E`,
  `F`, `H`, `1`, `3`, `4`, `5`, `O`, `Y`, `Z`, `' '`.
- `ABEND_CODE_HWPT = "HWPT"` must be `public static final String` (Rule 4, AC-4.1). No raw
  `"HWPT"` literal may appear anywhere outside this declaration (AC-4.2).
- Each factory method accepts a `String message`, stores the fail-code as `private final char failCode`,
  and returns a new `CrecustException`. Factory methods **do not mutate the commarea**. Each catch
  block that catches a `CrecustException e` calls `commarea.setCommFailCode(e.getFailCode())` and
  `commarea.setCommSuccess('N')` (G3 resolved 2026-10-01). No raw char literal at any call site (Rule 3).
- Factory method for success path is NOT required (success is not an exception).
- `CrecustException` extends `RuntimeException` (not abstract — it is the one and only exception
  class for this program).
- Package: `com.ibm.cics.botz.crecust.exception` (ADR-2).
- Exactly one top-level type declaration per file (Rule 17).
- No separate `*ExceptionHandler*` or `*ErrorHandler*` class is generated (ADR-10, G1 resolved
  2026-10-01): CRECUST has no `HANDLE CONDITION`, no shared error-response paragraph, and no
  cross-cutting catch policy. Every catch block is inline in its service class.

## Technical Decisions

- **ADR-10:** A single `CrecustException` with named static factories satisfies all error paths.
  All catch blocks in service classes construct exceptions via these factories — never by passing
  raw literals to a constructor. Every catch block must perform at least one side effect (set
  fail-code, call DEQ, or call linkAbndproc) before re-throwing. **The commarea mutation
  (`setCommFailCode` / `setCommSuccess`) belongs in the catch block, not in the factory method**
  (G3 resolved 2026-10-01 — factory carries `failCode` attribute, catch block reads it).
- **ADR-2 (package):** `com.ibm.cics.botz.crecust.exception`.
- **ADR-3 (coordinates):** groupId `com.ibm.cics.botz`, artifactId `crecust`, Java 21.
- The class carries a class-level Javadoc that maps each factory to the COBOL paragraph that
  grounds it (Rule 16 / ADR-10 documentation requirement, Story 9.2).
- Each factory accepts a `String message` parameter; some also accept a `Throwable cause`.
- The notifying-abort path (`ABEND_CODE_HWPT`) is invoked by `ProctranDbService` only after
  calling `AbndprocDelegate.linkAbndproc()` — this ordering is enforced by Rule 1 and documented
  in the class Javadoc.
- **`'G'` dual-use:** `FAIL_CODE_CREDIT_ERROR` serves both the post-CC-error path in
  `PREMIERE_P010` and the SECERROR path in `CREDIT-CHECK_CC010`. One constant, two factory call
  sites.

## Cross-Story Dependencies

- Epic 1 must be complete (Maven project and `pom.xml` exist) before any Java file can be placed.
- All service stories (Epics 3–8) depend on `CrecustException` being importable. Story 9.1 must
  be `done` before any service story begins implementation.
- Story 9.2 can be delivered together with 9.1 (it adds Javadoc and a verification grep — no
  additional Java files).
