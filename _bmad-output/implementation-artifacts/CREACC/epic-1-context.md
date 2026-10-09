# Epic 1 Context: Project Setup

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Establish the Maven project scaffold and package skeleton for the CREACC Java 21 modernisation. This epic creates the build infrastructure (`pom.xml`) and the full package directory tree with `package-info.java` stubs so that all subsequent epics can add classes to a pre-existing, compile-clean project structure. No business logic is written here.

## Stories

- Story 1.1: Maven project scaffold (`pom.xml`)
- Story 1.2: Package skeleton and coding-standards gate

## Requirements & Constraints

- Target runtime: Java 21 LTS; IBM Semeru Runtime for z/OS; CICS-resident (JCICS); plain JDBC; no Spring.
- `groupId` = `com.ibm.bankofz`, `artifactId` = `creacc`.
- All CICS-related classes use JCICS (`com.ibm.cics.server.*`); all serializers use JZOS (`com.ibm.jzos.*`).
- Dependencies: JCICS and JZOS are `provided`; DB2 JDBC driver is `provided`; SLF4J `slf4j-api` is `compile`; JUnit Jupiter 5.x and Mockito are `test`.
- Build must produce a CICS bundle-compatible JAR.
- No Spring dependencies anywhere in the project.
- One top-level class per `.java` file (Rule 17).
- Methods ≤ 40 lines; classes ≤ 2000 lines; naming: PascalCase classes, camelCase methods/variables, UPPER_SNAKE_CASE constants.

## Technical Decisions

- Serialization follows ADR-A: all wire-format classes implement `ByteArraySerializable<T>`; all serializers implement `ByteArraySerializer<T>` via JZOS.
- No dependency injection framework — manual constructor injection throughout, wired in `CreaccBatchRunner`.
- SLF4J logging only; one `private static final Logger` per class; silent-return paths emit no log output.
- All `AutoCloseable` resources (Connection, PreparedStatement, ResultSet) use `try-with-resources`.
- CONTROL table schema is assumed; every assumed constant gets a `// TODO: replace with confirmed DBA schema` comment.

## Cross-Story Dependencies

Story 1.2 depends on Story 1.1 (pom.xml must exist before package directories are meaningful to the build). All epics 2–6 depend on both stories in this epic being `done`.
