---
title: 'Story 1.1: Maven project pom.xml is created at the correct path'
type: 'feature'
created: '2026-10-01'
status: 'review'
route: 'dispatch'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The `crecust-java/` Maven project does not yet exist, so no Java source for the
CRECUST modernisation can compile. All nine subsequent epics depend on a working build scaffold.

**Approach:** Create `crecust-java/pom.xml` with the exact Maven coordinates, Java 21 compiler
settings, and the five required dependencies (JCICS, JZOS, Lombok, SLF4J, and optionally test
deps), then create the six package directories under `src/main/java/com/ibm/cics/botz/crecust/`
mandated by ADR-2.

## Boundaries & Constraints

**Always:**
- `groupId`: `com.ibm.cics.botz`, `artifactId`: `crecust`, `version`: `1.0.0-SNAPSHOT`
- `maven.compiler.source` and `maven.compiler.target` both `21`
- JCICS (`com.ibm.cics:com.ibm.cics.server`) scope `provided`
- JZOS (`com.ibm.jzos:ibm.jzos`) scope `provided`
- Lombok (`org.projectlombok:lombok`) configured as both annotation-processor and `provided` scope
- SLF4J API (`org.slf4j:slf4j-api`) scope `compile`
- Use dependency versions matching the sibling `creacc-java/pom.xml` as baseline: JCICS `2.200.0-6.3`, JZOS `4.0.0.0`, SLF4J `2.0.13`; Lombok `1.18.32`
- Six package directories must exist under `src/main/java/com/ibm/cics/botz/crecust/`: root, `service`, `model`, `db`, `serializer`, `exception`
- Each directory must contain a `package-info.java` so Maven's compiler picks up the empty package

**Never:**
- No Spring, Liberty, CDI, or any non-JCICS runtime dependency in `pom.xml`
- Do not create any Java business-logic source files in this story (no `.java` beyond `package-info.java` placeholders)
- Do not modify `creacc-java/pom.xml` or any existing file

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Clean compile | `crecust-java/` exists with `pom.xml` and package dirs | `mvn compile` exits 0, zero errors | N/A |
| Missing Lombok processor | Lombok not declared as annotation-processor | Compilation fails on `@Data` in later epics | Prevented by declaring `<annotationProcessorPaths>` in compiler plugin config |
| Wrong Java version | `source`/`target` not `21` | Compiler rejects Java 21 features (records, text blocks) used by later stories | Prevented by explicit `<source>21</source><target>21</target>` |

</frozen-after-approval>

## Code Map

- `creacc-java/pom.xml` — reference pom for the sibling CREACC project; use its dependency versions and plugin versions as the baseline
- `creacc-java/src/main/java/com/ibm/bankofz/creacc/infrastructure/` — reference package layout showing `package-info.java` convention
- `_bmad-output/planning-artifacts/CRECUST/architecture.md` §3 ADR-2, §11 — authoritative source for package names and required dependencies
- `_bmad-output/implementation-artifacts/CRECUST/epic-1-context.md` — compiled Epic 1 constraints (Maven coords, ADR decisions)

## Tasks & Acceptance

**Execution:**
- [x] `crecust-java/pom.xml` — CREATE — full Maven POM with all five required dependencies, Java 21 compiler settings, maven-compiler-plugin with Lombok annotation-processor path, maven-jar-plugin, and maven-surefire-plugin; no Spring or CDI dependency
- [x] `crecust-java/src/main/java/com/ibm/cics/botz/crecust/package-info.java` — CREATE — root package placeholder so Maven sees the package
- [x] `crecust-java/src/main/java/com/ibm/cics/botz/crecust/service/package-info.java` — CREATE — service layer package placeholder
- [x] `crecust-java/src/main/java/com/ibm/cics/botz/crecust/model/package-info.java` — CREATE — model layer package placeholder
- [x] `crecust-java/src/main/java/com/ibm/cics/botz/crecust/db/package-info.java` — CREATE — DB host-variable row package placeholder
- [x] `crecust-java/src/main/java/com/ibm/cics/botz/crecust/serializer/package-info.java` — CREATE — serializer layer package placeholder
- [x] `crecust-java/src/main/java/com/ibm/cics/botz/crecust/exception/package-info.java` — CREATE — exception package placeholder

**Acceptance Criteria:**
- Given `crecust-java/pom.xml` exists, when `mvn validate` is run from `crecust-java/`, then Maven reports `BUILD SUCCESS` with no validation errors
- Given the six `package-info.java` files exist, when `mvn compile` is run from `crecust-java/`, then the build succeeds with zero compilation errors
- Given the `pom.xml`, when its `<dependencies>` section is inspected, then JCICS, JZOS, and Lombok all have scope `provided`; SLF4J has scope `compile`; no Spring, Liberty, CDI, or non-JCICS runtime dependency is present
- Given the `pom.xml`, when its `<properties>` section is inspected, then `maven.compiler.source` = `21` and `maven.compiler.target` = `21`
- Given the `pom.xml`, when its `<groupId>`, `<artifactId>`, and `<version>` are inspected, then they equal `com.ibm.cics.botz`, `crecust`, and `1.0.0-SNAPSHOT` respectively
- Given the Lombok dependency, when the `maven-compiler-plugin` configuration is inspected, then `org.projectlombok:lombok` appears in `<annotationProcessorPaths>` so Lombok annotation processing is active at compile time
- Given the six package directories, when `find crecust-java/src/main/java/com/ibm/cics/botz/crecust -type d` is run, then exactly six directories are returned (root + service, model, db, serializer, exception)

## Implementation Notes

- Created `crecust-java/pom.xml` matching coordinates `com.ibm.cics.botz:crecust:1.0.0-SNAPSHOT` with Java 21 compiler settings.
- Added `com.ibm.cics:com.ibm.cics.server` (2.200.0-6.3, provided), `com.ibm.jzos:ibm.jzos` (4.0.0.0, provided), `org.projectlombok:lombok` (1.18.32, provided + annotationProcessorPaths), and `org.slf4j:slf4j-api` (2.0.13, compile).
- Created 6 package directories under `src/main/java/com/ibm/cics/botz/crecust/` each containing a `package-info.java`.
- Verified build with `mvn validate` and `mvn compile` (BUILD SUCCESS, 6 source files compiled).

### Files Created
- `crecust-java/pom.xml`
- `crecust-java/src/main/java/com/ibm/cics/botz/crecust/package-info.java`
- `crecust-java/src/main/java/com/ibm/cics/botz/crecust/service/package-info.java`
- `crecust-java/src/main/java/com/ibm/cics/botz/crecust/model/package-info.java`
- `crecust-java/src/main/java/com/ibm/cics/botz/crecust/db/package-info.java`
- `crecust-java/src/main/java/com/ibm/cics/botz/crecust/serializer/package-info.java`
- `crecust-java/src/main/java/com/ibm/cics/botz/crecust/exception/package-info.java`

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- `cd crecust-java && mvn validate` — expected: `BUILD SUCCESS`
- `cd crecust-java && mvn compile` — expected: `BUILD SUCCESS`, zero errors
- `find crecust-java/src/main/java/com/ibm/cics/botz/crecust -mindepth 0 -maxdepth 1 -type d` — expected: 6 directories listed (root + 5 sub-packages)
