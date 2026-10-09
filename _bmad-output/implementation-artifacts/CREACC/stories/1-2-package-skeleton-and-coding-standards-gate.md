---
status: in-review
route: oneshot
story_key: 1-2-package-skeleton-and-coding-standards-gate
context:
  - _bmad-output/implementation-artifacts/CREACC/epic-1-context.md
  - _bmad-output/planning-artifacts/CREACC/architecture-class-list.md
---

# Story 1.2 — Package Skeleton and Coding-Standards Gate

## Summary

Create the full package directory tree under `creacc-java/src/main/java/com/ibm/bankofz/creacc/`
with one `package-info.java` per package so that all subsequent epics can add classes to a
pre-existing, compile-clean project structure. Also add `checkstyle.xml` and wire it into
`pom.xml` so every build enforces the project's Java coding standards.

## Context

- Epic: 1 — Project Setup
- Program: CREACC (Create Account CICS batch program)
- Target: Java 21 · CICS-resident (JCICS) · Plain JDBC · No Spring
- Sprint-status key: `1-2-package-skeleton-and-coding-standards-gate`

<frozen-after-approval>

## Tasks & Acceptance

### Task 1 — Create package directories with `package-info.java` stubs

- [x] `creacc-java/src/main/java/com/ibm/bankofz/creacc/package-info.java` exists
- [x] `creacc-java/src/main/java/com/ibm/bankofz/creacc/service/package-info.java` exists
- [x] `creacc-java/src/main/java/com/ibm/bankofz/creacc/dao/package-info.java` exists
- [x] `creacc-java/src/main/java/com/ibm/bankofz/creacc/model/package-info.java` exists
- [x] `creacc-java/src/main/java/com/ibm/bankofz/creacc/exception/package-info.java` exists
- [x] `creacc-java/src/main/java/com/ibm/bankofz/creacc/constants/package-info.java` exists
- [x] `creacc-java/src/main/java/com/ibm/bankofz/creacc/infrastructure/package-info.java` exists
- [x] `creacc-java/src/main/java/com/ibm/bankofz/creacc/infrastructure/serialization/package-info.java` exists
- [x] Each `package-info.java` contains exactly a Javadoc comment describing the package and the correct `package` statement
- [x] No business logic in any stub file

### Task 2 — Add Checkstyle coding-standards gate

- [x] `creacc-java/checkstyle.xml` exists and enforces:
  - No wildcard imports
  - One top-level type per file
  - Method length ≤ 40 lines
  - Class/file length ≤ 2000 lines
  - Naming: PascalCase types, camelCase methods/variables, UPPER_SNAKE_CASE constants
  - Mandatory Javadoc on public types
- [x] `pom.xml` references `checkstyle.xml` via `maven-checkstyle-plugin` bound to `verify`
- [x] Checkstyle plugin version pinned in `<properties>`

### Task 3 — Create `creacc-java/README.md`

- [x] `creacc-java/README.md` exists
- [x] README covers: project overview, build command, dependency notes, package structure

</frozen-after-approval>

## Design Notes

- Package structure is derived from `architecture-class-list.md` layers: entry-point / batch runner,
  service, DAO, model, exception, constants, infrastructure.
- Serializer classes live in `infrastructure/serialization` per ADR-A.
- No Spring anywhere — package-info files must not import or reference Spring annotations.
- Checkstyle rules are deliberately minimal for this project; they match NFR-5 and NFR-8 exactly.

## Code Map

- `pom.xml` (from Story 1.1): extended with `maven-checkstyle-plugin` configuration.

## Spec Change Log

- Created: initial spec for story 1-2.
