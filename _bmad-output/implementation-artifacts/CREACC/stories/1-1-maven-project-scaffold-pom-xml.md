---
status: in-review
route: oneshot
baseline_commit: 05b61033e33a7211860a23b46abd5f630420dfa3
story_key: 1-1-maven-project-scaffold-pom-xml
context:
  - _bmad-output/implementation-artifacts/CREACC/epic-1-context.md
  - _bmad-output/planning-artifacts/CREACC/architecture-class-list.md
---

# Story 1.1 — Maven Project Scaffold (`pom.xml`)

## Summary

Create a Maven `pom.xml` at `creacc-java/pom.xml` so the full CREACC Java 21 modernisation can be built, tested, and packaged in one reproducible command.

## Context

- Epic: 1 — Project Setup
- Program: CREACC (Create Account CICS batch program)
- Target: Java 21 · CICS-resident (JCICS) · Plain JDBC · No Spring
- Sprint-status key: `1-1-maven-project-scaffold-pom-xml`

<frozen-after-approval>

## Tasks & Acceptance

### Task 1 — Create `creacc-java/pom.xml`

- [x] File exists at `creacc-java/pom.xml`
- [x] `<groupId>` = `com.ibm.bankofz`
- [x] `<artifactId>` = `creacc`
- [x] `<java.version>` / `<maven.compiler.source>` / `<maven.compiler.target>` all = `21`
- [x] Dependencies declared with correct scopes:
  - `com.ibm.cics:com.ibm.cics.server` (`provided`)
  - `com.ibm.db2.jcc:db2jcc4` (`provided`)
  - `com.ibm.jzos:ibm.jzos` (`provided`)
  - `org.slf4j:slf4j-api` (`compile`)
  - `org.junit.jupiter:junit-jupiter` 5.x (`test`)
  - `org.mockito:mockito-core` (`test`)
- [x] `maven-jar-plugin` configured to produce a CICS bundle-compatible JAR (no Spring Boot packaging)
- [x] No Spring dependencies present
- [x] No wildcard imports (n/a for XML, but no Spring starters)

</frozen-after-approval>

## Design Notes

- JCICS, JZOS, and DB2 JDBC are `provided` because they are supplied by the CICS runtime environment on z/OS.
- SLF4J is `compile` scope because the logging API ships with the application JAR.
- Test dependencies use `test` scope standard.
- The JAR must be CICS bundle-compatible — plain `maven-jar-plugin`, not `spring-boot-maven-plugin`.

## Code Map

*(No existing code to map — this story creates the project from scratch.)*

## Spec Change Log

- Created: initial spec for story 1-1.
