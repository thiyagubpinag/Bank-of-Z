# Epic 3 Context: CICS Entry Point, Commarea Binding, and EIBCALEN Partial-Copy

## Epic Goal

Author the `Crecust` entry-point class, the `CrecustService` orchestrator skeleton, CICS version check, time/date population helpers, and the shared exit method. After this epic the program is invocable end-to-end (even if service stubs return immediately).

**Dependencies:** Epic 1 (Maven project, infrastructure utilities), Epic 2 (model classes and serializers).

---

## Package / File Conventions

| Layer | Package |
|---|---|
| Entry point | `com.ibm.cics.botz.crecust` |
| Services | `com.ibm.cics.botz.crecust.service` |
| Models | `com.ibm.cics.botz.crecust.model` |
| Exceptions | `com.ibm.cics.botz.crecust.exception` |

---

## Key Classes Referenced by This Epic

| Class | Role |
|---|---|
| `Crecust` | `@CICSProgram("CRECUST")` entry point; receives commarea, delegates, returns |
| `CrecustService` | Stateless orchestrator; wires all 10-step PREMIERE flow |
| `CrecustareaSerializer` | Deserializes/serializes the 399-byte DFHCOMMAREA |
| `CrecustCommarea` | Model for commarea (25 fields, 399 bytes) |
| `WsCicstsLevelNumGrp` | Cat 3a REDEFINES record: VV/RR/MM int sub-fields of `wsCicstslevel String` |
| `WsOrigDateGrp` | Cat 3a record: DD/sep/MM/sep/YYYY sub-fields of `wsOrigDate String` |
| `WsTimeNowGrp` | Cat 3a record: HH/MM/SS sub-fields of `wsTimeNow int` |

---

## ADR-6 — CICS Version Check

Replace `WS-CICSTSLEVEL` / `CICSTSLEVEL` check with `Task.getTask().getCicsVersion()`. Parse returned string into VV/RR/MM components stored in `WsCicstsLevelNumGrp`. The `WsCicstsLevelNumGrp` Cat 3a class is still required; the check logic in `CrecustService` uses the JCICS inquiry result rather than a commarea-derived field.

---

## CICS API Mapping (Epic 3 Scope)

| COBOL | Java | Owner |
|---|---|---|
| `EXEC CICS RETURN` | Return from `main()` — Java `return` statement; `Task.getTask().returnToCaller()` does NOT exist in JCICS | `Crecust` |
| `EXEC CICS ASKTIME ABSTIME(...)` | `LocalDateTime.now()` — `Task.getTask().getAbstime()` does NOT exist in JCICS | `CrecustService.populateTimeAndDate()` |
| `EXEC CICS FORMATTIME DDMMYYYY DATESEP` | `LocalDateTime` + `DateTimeFormatter("dd/MM/yyyy")` | `CrecustService.populateTimeAndDate()` |
| `CICSTSLEVEL` / WS-CICSTSLEVEL | `Task.getTask().getCicsVersion()` | `CrecustService.populateCicsVersion()` |

---

## Story Status (Epic 3)

| Story | Title | Status |
|---|---|---|
| 3.1 | `Crecust` entry-point class | No spec yet |
| 3.2 | `CrecustService` orchestrator skeleton | No spec yet |
| 3.3 | EIBCALEN partial-copy guard in `CrecustareaSerializer` | No spec yet |
| 3.4 | `CrecustService.populateTimeAndDate()` | No spec yet |
| 3.5 | `CrecustService` CICS version check — `WsCicstsLevelNumGrp` population | **In progress** |

---

## Cat 3a Pattern for `WS-CICSTSLEVEL`

COBOL anchor: `WS-CICSTSLEVEL PIC X(6)` with REDEFINES `WS-CICSTS-LEVEL-NUM-GRP` containing VV/RR/MM integer sub-fields.

Java realisation:
- `CrecustService` holds local field `wsCicstslevel` (String) representing the PIC anchor.
- `WsCicstsLevelNumGrp` is a record-style model class with `int vv`, `int rr`, `int mm`.
- `getWsCicstsLevelNumGrp()` on `CrecustService` parses `wsCicstslevel` to populate and return the group.

---

## Lombok and Infrastructure

- All model classes use `@Data @NoArgsConstructor @AllArgsConstructor @Builder(toBuilder=true)` (ADR-12).
- Infrastructure interfaces (`ByteArraySerializer`, `ByteArraySerializable`, `Settable`, `Lists`) are pre-existing classpath dependencies — do not regenerate them.
