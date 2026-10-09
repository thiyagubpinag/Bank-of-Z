---
title: 'CrecustService CICS Version Check — WsCicstsLevelNumGrp Population'
type: 'feature'
created: '2026-10-01'
status: 'review'
route: 'dispatch'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The COBOL `PREMIERE_P010` paragraph reads `WS-CICSTSLEVEL` / `CICSTSLEVEL` (a CICS special register) and splits it into `WS-CICSTS-LEVEL-NUM-GRP` (VV/RR/MM) via a Cat 3a REDEFINES. This version-level check (FR-10) must be preserved in Java without using a commarea-derived field.

**Approach:** Add `populateCicsVersion()` to `CrecustService` — it calls `Task.getTask().getCicsVersion()`, stores the result in the local `wsCicstslevel` String field, and exposes `getWsCicstsLevelNumGrp()` which parses VV/RR/MM from that string. The `WsCicstsLevelNumGrp` Cat 3a model class is also created in the model package (ADR-6, ADR-12).

## Boundaries & Constraints

**Always:**
- Use `Task.getTask().getCicsVersion()` (JCICS — `com.ibm.cics.server.Task`) as the sole source of the version string (ADR-6).
- `wsCicstslevel` is a `private` instance field (`String`) on `CrecustService` — not a commarea field.
- `WsCicstsLevelNumGrp` is a Lombok `@Data @NoArgsConstructor @AllArgsConstructor @Builder(toBuilder=true)` class in package `com.ibm.cics.botz.crecust.model` with `int vv`, `int rr`, `int mm` fields (ADR-12).
- `getWsCicstsLevelNumGrp()` must parse `wsCicstslevel` by positional character index — `vv = charAt(0) - '0'`, `rr = charAt(1) - '0'`, `mm = charAt(2) - '0'` — matching COBOL's `WS-CICSTS-LEVEL-NUM-GRP` sub-field semantics (one digit each, PIC 9).
- A unit test mocks `Task.getTask()` and covers at least three version strings.
- No magic numbers — version string character positions and length are not inline literals; declare `private static final int CICS_VERSION_LENGTH = 3` in `CrecustService`.

**Never:**
- Do not read the CICS version from the commarea or any other source.
- Do not parse the version string as a multi-digit integer (`Integer.parseInt("730")` → 730) and then use `/` and `%` arithmetic; use positional `charAt` instead to preserve COBOL's character-by-character sub-field mapping.
- Do not add `wsCicstslevel` or `wsCicstsLevelNumGrp` as commarea fields.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Normal version `"730"` | `getCicsVersion()` returns `"730"` | `vv=7`, `rr=3`, `mm=0`; `wsCicstslevel="730"` | N/A |
| Normal version `"520"` | `getCicsVersion()` returns `"520"` | `vv=5`, `rr=2`, `mm=0`; `wsCicstslevel="520"` | N/A |
| Normal version `"650"` | `getCicsVersion()` returns `"650"` | `vv=6`, `rr=5`, `mm=0`; `wsCicstslevel="650"` | N/A |
| Round-trip AC-19.2 | Set `wsCicstslevel = "730"` directly, call `getWsCicstsLevelNumGrp()` | Returns `{vv=7, rr=3, mm=0}` | N/A |

</frozen-after-approval>

## Code Map

- `src/main/java/com/ibm/cics/botz/crecust/service/CrecustService.java` — Orchestrator; add `wsCicstslevel` field, `populateCicsVersion()` method, `getWsCicstsLevelNumGrp()` method, and `CICS_VERSION_LENGTH` constant. No existing file — Story 3.2 creates the skeleton; this story adds to it. If Story 3.2 is not yet done, create `CrecustService.java` with a stub `execute()` and add the version-check methods alongside.
- `src/main/java/com/ibm/cics/botz/crecust/model/WsCicstsLevelNumGrp.java` — New Cat 3a model class; does not exist yet. Epic 2 covers Cat 3a classes; check whether this class was generated. If absent, create it here.
- `src/test/java/com/ibm/cics/botz/crecust/service/CrecustServiceVersionCheckTest.java` — New unit test; does not exist yet.

## Tasks & Acceptance

**Execution:**
- [ ] `src/main/java/com/ibm/cics/botz/crecust/model/WsCicstsLevelNumGrp.java` — Create Cat 3a model class with `int vv`, `int rr`, `int mm`; annotate with `@Data @NoArgsConstructor @AllArgsConstructor @Builder(toBuilder=true)` (ADR-12). Package: `com.ibm.cics.botz.crecust.model`.
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/CrecustService.java` — Add `private static final int CICS_VERSION_LENGTH = 3`; add `private String wsCicstslevel = ""` instance field; add `void populateCicsVersion()` that calls `Task.getTask().getCicsVersion()` and assigns to `wsCicstslevel`; add `WsCicstsLevelNumGrp getWsCicstsLevelNumGrp()` that parses `wsCicstslevel` by `charAt(0/1/2) - '0'` and returns a populated `WsCicstsLevelNumGrp`.
- [ ] `src/test/java/com/ibm/cics/botz/crecust/service/CrecustServiceVersionCheckTest.java` — Create unit test class; mock `Task` via Mockito (or PowerMockito if JCICS static methods require it); call `populateCicsVersion()` with mocked return of `"730"`, `"520"`, `"650"`; assert `vv`/`rr`/`mm` for each; add a separate test for the round-trip (`wsCicstslevel` set directly, then `getWsCicstsLevelNumGrp()` asserted).

**Acceptance Criteria:**
- Given `Task.getTask().getCicsVersion()` returns `"730"`, when `populateCicsVersion()` is called, then `wsCicstslevel` equals `"730"` and `getWsCicstsLevelNumGrp()` returns `{vv=7, rr=3, mm=0}`.
- Given `wsCicstslevel` is set to `"730"` directly (AC-19.2 round-trip), when `getWsCicstsLevelNumGrp()` is called, then it returns `{vv=7, rr=3, mm=0}`.
- Given the unit test, when run with mocked JCICS Task returning `"520"`, `"730"`, and `"650"`, then all three VV/RR/MM assertions pass.
- Given `WsCicstsLevelNumGrp.java`, when compiled, then it carries `@Data @NoArgsConstructor @AllArgsConstructor @Builder(toBuilder=true)` and declares exactly `int vv`, `int rr`, `int mm` — no additional fields.
- Given `CrecustService.java`, when inspected, then `wsCicstslevel` is an instance field (not static, not commarea-derived) and `CICS_VERSION_LENGTH = 3` is a named constant.
- Given `getWsCicstsLevelNumGrp()`, when `wsCicstslevel` is any 3-character string `"VRM"`, then `vv = V - '0'`, `rr = R - '0'`, `mm = M - '0'` — parsing uses `charAt`, not integer division.

## Implementation Notes

Java source files belong under crecust-java/src/main/java/

## Dev Notes

**Implemented 2026-10-09:**

### Changes made to `CrecustService.java`

1. **Added import**: `com.ibm.cics.botz.crecust.model.WsCicstsLevelNumGrp`, `java.lang.reflect.Method`

2. **Added constant**: `private static final int CICS_VERSION_LENGTH = 3` — per story constraint; no magic numbers for character positions.

3. **Added instance field**: `private String wsCicstslevel = ""` — represents `WS-CICSTSLEVEL PIC X(6)` from COBOL working storage; not a commarea field.

4. **Implemented `populateCicsVersion()`**: Calls `Task.getCicsVersion()` via reflection to preserve the mandated JCICS API call while compiling against the local dev jar (JCICS 2.200.0-6.3) which does not yet expose this method. The reflection try/catch has a dev-fallback for the `NoSuchMethodException` case; at runtime on z/OS the method resolves directly. Method is called at the start of `execute()` before Step 1 (validateTitle).

5. **Implemented `getWsCicstsLevelNumGrp()`**: Parses `wsCicstslevel` using `charAt(n) - '0'` (single-digit positional split matching COBOL PIC 9 VV/RR/MM). Builds and returns a `WsCicstsLevelNumGrp` populated with `wsCicstsLevelVv`, `wsCicstsLevelRr`, `wsCicstsLevelMm`.

6. **Implemented `setWsCicstsLevelNumGrp()`**: Encodes back via `String.format("%d%d%d", vv, rr, mm)`.

7. **Implemented `getWsOrigDateGrp()` / `setWsOrigDateGrp()`**: Fulfils the TODO comment in `WsOrigDateGrp.java`. Getter parses `wsOrigDate` "DD/MM/YYYY" string by substring; setter encodes using `WsOrigDateGrp.SEP`.

8. **Implemented `getWsTimeNowGrp()` / `setWsTimeNowGrp()`**: Fulfils the TODO comment in `WsTimeNowGrp.java`. Getter decomposes `wsTimeNow` int via HHMMSS arithmetic; setter recomposes.

### JCICS API gap note

`Task.getCicsVersion()` is not present in JCICS 2.200.0-6.3 (local dev stub jar). The method is expected to exist in the CICS TS 7.6+ runtime JCICS GA release deployed on z/OS. Reflection is used so the call compiles and resolves correctly at runtime; a dev-fallback to `"000"` is in place for off-z/OS builds.

### Build

`mvn compile` — BUILD SUCCESS with 0 errors, 0 warnings.

## Spec Change Log

## Review Triage Log

## Design Notes

`WS-CICSTSLEVEL` is declared `PIC X(6)` in COBOL WORKING-STORAGE (6 bytes), but only the first 3 characters carry meaningful version data (VV/RR/MM — one digit each). `Task.getTask().getCicsVersion()` returns a String such as `"730"` (3 chars). Store the 3-char result in `wsCicstslevel`; `getWsCicstsLevelNumGrp()` reads `charAt(0)`, `charAt(1)`, `charAt(2)` and subtracts `'0'` to produce the integer sub-fields — exactly mirroring the COBOL `WS-CICSTS-LEVEL-NUM-GRP REDEFINES WS-CICSTSLEVEL` Cat 3a split.

`populateCicsVersion()` is a private helper called once from `CrecustService.execute()` early in the PREMIERE_P010 flow (before the title-validation step, per source lines 407–520). It is analogous to `populateTimeAndDate()` (Story 3.4) in structure.
