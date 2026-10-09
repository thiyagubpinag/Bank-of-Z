---
title: 'Story 3.1: Crecust Entry Point Class — Commarea Receive, Delegate, and Return'
type: 'feature'
created: '2026-10-01'
status: 'draft'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '05b61033e33a7211860a23b46abd5f630420dfa3'
context:
  - '_bmad-output/implementation-artifacts/CRECUST/epic-3-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The COBOL `CRECUST` program's `PREMIERE_P010` entry-point logic — receive the 399-byte commarea, delegate to service logic, and return to the CICS caller via `GET-ME-OUT-OF-HERE` — has no Java equivalent. Without it, the modernised program cannot be invoked from CICS.

**Approach:** Create `Crecust.java`, a thin JCICS `@CICSProgram` entry-point class that deserialises the raw commarea byte array via `CrecustareaSerializer`, calls `CrecustService.execute(commarea)`, serialises the result back into the byte array, then returns from `main()` at the single shared exit point. Java's `return` statement is the equivalent of `EXEC CICS RETURN`; `Task.getTask().returnToCaller()` does not exist in the JCICS API.

## Boundaries & Constraints

**Always:**
- Class is annotated `@CICSProgram("CRECUST")` and implements the JCICS entry-point mechanism (extends `AbstractProgram` or equivalent).
- `CrecustareaSerializer.fromBytes(byte[], 0)` is called before any business logic.
- `CrecustareaSerializer.toBytes(commarea)` writes the result back into the raw commarea byte array after `CrecustService.execute()` returns.
- `main()` returns normally at the single shared exit point (`return` from method = `EXEC CICS RETURN`). This happens exactly once — never inside a branch or catch block.
- Class body is ≤ 20 lines, excluding imports and Javadoc.
- `private static final Logger LOGGER` (SLF4J `LoggerFactory.getLogger`) is declared; entry is logged at INFO before deserialisation, exit at INFO before `return`.
- Package: `com.ibm.cics.botz.crecust`.

**Never:**
- Do NOT call `Task.getTask().returnToCaller()` — this method does not exist in the JCICS API.
- No business logic inside `Crecust` — all logic lives in `CrecustService`.
- No Spring, Liberty, or CDI annotations (`@Component`, `@Autowired`, `@Resource`, etc.).
- Do not inline byte-packing or manual field reads from the raw byte array — delegate entirely to `CrecustareaSerializer`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Happy path — full 399-byte commarea | Raw 399-byte byte array passed by CICS | `fromBytes` populates `CrecustCommarea`; `execute()` succeeds; `toBytes` writes updated fields back; `main()` returns | None — propagate any unchecked exception upward |
| Service throws `CrecustException` | `CrecustService.execute()` throws a `CrecustException` (silent-return fail code already set in commarea before throw) | Commarea byte array is still serialised back with the fail-code set; `main()` returns normally | Catch `CrecustException`, serialise commarea, then fall through to the single `return` at the end of `main()` |
| Short commarea (`EIBCALEN < 399`) | Byte array shorter than 399 bytes | `CrecustareaSerializer.fromBytes` handles partial reads (Story 3.3 guard); same exit path | Same — no special handling needed in `Crecust` itself |

</frozen-after-approval>

## Code Map

- `src/main/java/com/ibm/cics/botz/crecust/Crecust.java` — new file; the sole deliverable of this story
- `src/main/java/com/ibm/cics/botz/crecust/service/CrecustService.java` — must exist on classpath; `execute(CrecustCommarea)` is called by `Crecust`; may be a stub returning immediately for compile purposes (Story 3.2 delivers the full implementation)
- `src/main/java/com/ibm/cics/botz/crecust/serializer/CrecustareaSerializer.java` — must exist on classpath; `fromBytes(byte[])` and `toBytes(CrecustCommarea)` are the only calls made; delivered by Epic 2 Story 2.5
- `src/main/java/com/ibm/cics/botz/crecust/model/CrecustCommarea.java` — model class delivered by Epic 2 Story 2.1; used as the typed commarea object
- `com.ibm.cics.server.AbstractProgram` (or `@CICSProgram` entry-point base) — JCICS; `Crecust` extends or implements this. Note: `Task.getTask().returnToCaller()` does NOT exist; `main()` simply returns.
- Technical research PE-1 (commarea layout, 399 bytes) and architecture section 5.1 describe the entry-point contract
- COBOL source reference: `PREMIERE_P010` lines 407–520 and `GET-ME-OUT-OF-HERE` ~lines 1797–1806

## Tasks & Acceptance

**Execution:**
- [ ] `src/main/java/com/ibm/cics/botz/crecust/Crecust.java` — create the class; annotate with `@CICSProgram("CRECUST")`; extend the appropriate JCICS entry-point base class; implement `main()` with: (1) INFO log entry, (2) `CrecustareaSerializer.fromBytes` call, (3) `CrecustService.execute(commarea)` inside a try-catch for `CrecustException`, (4) `CrecustareaSerializer.toBytes(commarea)` to write result back into the raw byte array, (5) INFO log exit, (6) `return` — do NOT call `Task.getTask().returnToCaller()`, which does not exist in JCICS; class body ≤ 20 lines excluding imports and Javadoc

**Acceptance Criteria:**
- Given `Crecust` is annotated with `@CICSProgram("CRECUST")` and extends the JCICS `AbstractProgram` (or equivalent entry-point mechanism), when the CICS container invokes `Crecust.main()`, then `CrecustareaSerializer.fromBytes(commareaBytes)` is called on the raw commarea array before any business logic executes.
- Given `CrecustService.execute()` returns normally, when `Crecust.main()` completes, then `CrecustareaSerializer.toBytes(commarea)` writes the updated commarea back into the raw byte array, and `main()` returns normally at the single shared exit point. `Task.getTask().returnToCaller()` is NOT called — it does not exist in the JCICS API.
- Given `CrecustService.execute()` throws a `CrecustException` (fail-code already set in commarea), when `Crecust.main()` handles the exception, then the commarea is serialised back with the fail-code intact, `main()` returns normally (no rethrow), and no exception escapes to the JCICS container.
- Given the full `Crecust` class, when the class body (excluding imports and Javadoc) is counted, then it contains ≤ 20 lines.
- Given `Crecust` is instantiated, when `main()` is entered and exited, then `LOGGER.info(...)` is called at both entry and exit (verifiable by log inspection or spy in a unit test).
- Given `Crecust`, when the class is inspected, then it contains zero business logic — the only operations are deserialise, delegate to `CrecustService.execute()`, serialise, log, and return.

## Implementation Notes

Java source files belong under crecust-java/src/main/java/

## Dev Notes

**Implemented:** 2026-10-09

### JCICS API Discovery (CRITICAL — deviates from story skeleton)

The story's design skeleton assumed `extends AbstractProgram` and `getCommarea().getBytes()`.
Inspection of the actual JCICS jar (`com.ibm.cics.server` v2.200.0-6.3) revealed:

1. **`AbstractProgram` does not exist** in JCICS 2.200.0-6.3. There is no base class to extend.
2. **`@CICSProgram` is `ElementType.METHOD`** (not `ElementType.TYPE`). It must be applied to a `public void` no-arg instance method. The JCICS annotation processor generates a `static main(CommAreaHolder)` proxy at compile time.
3. **`Task.getTask()` has no `getCommarea()` method.** Commarea bytes are received via `CommAreaHolder.getValue()` in the static entry-point pattern.
4. **`Task.getTask().returnToCaller()` does NOT exist** — confirmed. `return` from `main()` is the correct exit.

### Implementation Decisions

- Class is a plain POJO (no superclass).
- `@CICSProgram("CRECUST")` is applied to `public void main()` (no-arg) for the Liberty/CDI annotation-processor path.
- `public static void main(CommAreaHolder cah)` is the CICS OSGi JVM-server entry point, delegating to `private void run(CommAreaHolder)` which holds the actual logic.
- Commarea bytes read via `cah.getValue()`, written back via `System.arraycopy` into the same buffer (in-place mutation).
- `CrecustareaSerializer.INSTANCE.fromBytes(raw, 0)` and `INSTANCE.toBytes(new byte[SIZE], 0, commarea)` are used — matching the actual serializer method signatures.
- `CrecustException` stub created in `com.ibm.cics.botz.crecust.exception` (full implementation deferred to Story 9.1).
- `CrecustService` stub created in `com.ibm.cics.botz.crecust.service` (full implementation deferred to Story 3.2).

### Compile Verification

`mvn compile` in `crecust-java/` — **BUILD SUCCESS** (41 source files, 0 errors, 0 warnings).

## Spec Change Log

## Review Triage Log

## Design Notes

The COBOL `GET-ME-OUT-OF-HERE` paragraph (a single `EXEC CICS RETURN`) is called from every error path and the happy path in `PREMIERE_P010`. In Java, returning from `main()` is the equivalent. Java's structured exception handling achieves the same single-exit guarantee by catching `CrecustException` in `Crecust.main()`, serialising the commarea (which already carries the fail-code set before throw), and then falling through to the `return` at the end of `main()`. This ensures the exit is never duplicated.

**Important:** `Task.getTask().returnToCaller()` does NOT exist in the JCICS API. Do NOT include this call. The JCICS `AbstractProgram.main()` method simply returns to the CICS container when it completes.

The `CrecustException` catch in `Crecust.main()` is a structural boundary, not business logic. It only serialises and exits — all fail-code decisions are made inside `CrecustService` before the exception is thrown.

Example skeleton (≤ 20 lines):

```java
@CICSProgram("CRECUST")
public class Crecust extends AbstractProgram {
    private static final Logger LOGGER = LoggerFactory.getLogger(Crecust.class);
    private static final CrecustareaSerializer SERIALIZER = new CrecustareaSerializer();
    private final CrecustService service = new CrecustService();

    public void main() {
        LOGGER.info("CRECUST entry");
        byte[] raw = getCommarea().getBytes();
        CrecustCommarea commarea = SERIALIZER.fromBytes(raw);
        try {
            service.execute(commarea);
        } catch (CrecustException e) {
            // fail-code already set in commarea before throw
        }
        System.arraycopy(SERIALIZER.toBytes(commarea), 0, raw, 0, raw.length);
        LOGGER.info("CRECUST exit");
        // return from main() = EXEC CICS RETURN; do NOT call Task.getTask().returnToCaller()
    }
}
```

Note: confirm the exact JCICS entry-point API (`getCommarea()`, `AbstractProgram`, `@CICSProgram`) against the JCICS library available in the project's `pom.xml`.

## Verification

**Commands:**
- `mvn compile -pl crecust` -- expected: BUILD SUCCESS with no compiler errors; `Crecust.class` present in `target/classes/com/ibm/cics/botz/crecust/`
- `mvn test -pl crecust -Dtest=CrecustTest` -- expected: all test methods pass; log spy confirms INFO messages at entry and exit; confirm `Task.getTask().returnToCaller()` is NOT called anywhere (grep confirms zero references); `main()` returns normally in both happy-path and exception-path scenarios
