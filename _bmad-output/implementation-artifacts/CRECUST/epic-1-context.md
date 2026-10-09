# Epic 1 Context: Project Setup and Build Infrastructure

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Scaffold the Maven project for the CRECUST Java modernisation so that every subsequent epic has a
compilable foundation to build on. This means creating the `pom.xml` with all required
dependencies, establishing the package directory structure, and generating the four shared
infrastructure utility types (`ByteArraySerializer<T>`, `ByteArraySerializable<T>`, `Settable<T>`,
`Lists`) that serializer classes in Epic 2 will depend on.

## Stories

- Story 1.1: Maven project `pom.xml` is created at the correct path
- Story 1.2: Shared infrastructure utility library is generated

## Requirements & Constraints

- The project must compile against Java 21 with zero errors in a clean environment (`mvn compile`).
- Maven coordinates are fixed: `groupId` = `com.ibm.cics.botz`, `artifactId` = `crecust`,
  `version` = `1.0.0-SNAPSHOT`.
- Required runtime: JCICS JVM server — **no Spring Boot, no Liberty, no dependency injection
  framework**. No Spring, CDI, or non-JCICS runtime dependency may appear in the `pom.xml`.
- Required dependencies: JCICS (`com.ibm.cics:com.ibm.cics.server`, `provided`), JZOS
  (`com.ibm.jzos:ibm.jzos`, for EBCDIC serialization), Lombok (`org.projectlombok:lombok`,
  annotation-processor + provided scope), SLF4J API (`org.slf4j:slf4j-api`).
- Package layout follows ADR-2 (layer-first under `com.ibm.cics.botz.crecust`): root, service,
  model, db, serializer, exception — six directories total.
- Each infrastructure type lives in its own `.java` file (Rule 17 — one top-level type per file).
- The infrastructure types must be on the classpath before any serializer class in Epic 2 can
  compile.

## Technical Decisions

- **ADR-3 (Maven Coordinates):** `groupId`: `com.ibm.cics.botz`, `artifactId`: `crecust`,
  `version`: `1.0.0-SNAPSHOT`, `java.version`: `21`.
- **ADR-2 (Package Structure — layer-first):** Six sub-packages under `com.ibm.cics.botz.crecust`:
  root (entry point), `service`, `model`, `db`, `serializer`, `exception`.
- **ADR-1 (Serialization Strategy — ADR-A):** The four infrastructure types
  (`ByteArraySerializer<T>`, `ByteArraySerializable<T>`, `Settable<T>`, `Lists`) are required
  project dependencies. They are consumed by all three canonical serializers added in Epic 2
  (`CrecustareaSerializer`, `WsChildDataSerializer`, `AbndInfoRecSerializer`).
- **ADR-12 (Lombok Enabled):** Lombok is configured as both annotation-processor and provided
  dependency so `@Data`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor` work on model
  classes in later epics.
- A sibling project `creacc-java` already exists in this repo at `creacc-java/pom.xml` and uses
  the same JCICS / JZOS / SLF4J stack. Use its dependency versions as the reference baseline
  (`jcics.version`: `2.200.0-6.3`, `jzos.version`: `4.0.0.0`, `slf4j.version`: `2.0.13`).
- Lombok `artifactId` in the sibling project is absent (not yet adopted) — add it for `crecust`
  as `org.projectlombok:lombok` with a current stable version (e.g. `1.18.32`).
- JZOS `artifactId` in sibling is `ibm.jzos` — use the same.

## Cross-Story Dependencies

- Story 1.2 depends on Story 1.1: the shared infrastructure types must be placed under the
  `src/main/java/com/ibm/cics/botz/crecust/` package tree established by Story 1.1 (or in a
  dedicated `infrastructure` sub-package). They must be reachable by Epic 2 serializer classes
  without a separate Maven module.
- All subsequent epics (2–9) depend on both stories in Epic 1 being complete before any Java
  source file can compile.
