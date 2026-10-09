# Epic 8 Context: PROCTRAN Audit Write and Notifying-Abort Path

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Author `ProctranDbService.insertProctran()` and `AbndprocDelegate.linkAbndproc()` — the only notifying-abort path in CRECUST. The PROCTRAN INSERT failure must follow the exact ordering: populate `AbndInfoRec` → DEQ → LINK ABNDPROC → ABEND `'HWPT'` (Rule 1). Reversing any step is a functional defect.

## Stories

- Story 8.1: `ProctranDbService.insertProctran()` — PROCTRAN assembly and JDBC INSERT
- Story 8.2: Notifying-abort path — `AbndprocDelegate` and PROCTRAN SQL failure sequence

## Requirements & Constraints

- `ProctranDbService` and `AbndprocDelegate` are both in package `com.ibm.cics.botz.crecust.service`.
- The PROCTRAN INSERT is the **only** notifying-abort path; all other error paths are silent-return.
- The 4-step abort sequence (populate `AbndInfoRec` → DEQ → LINK → ABEND) must be expressed in that exact order; a unit test must enforce it.
- `AbndprocDelegate.linkAbndproc(AbndInfoRec)` returns `void` — ABNDPROC's return code is never inspected (ADR-11, Rule 10).
- `AbndInfoRecSerializer` is the canonical serializer for `AbndInfoRec`; `linkAbndproc()` must delegate to it rather than re-packing bytes inline (Rule 15).
- `CrecustException.ABEND_CODE_HWPT = "HWPT"` is the sole source of the literal `"HWPT"` — no bare string literal at any call site (Rule 4).
- All JDBC resources use `try-with-resources` (NFR-4).
- PROCTRAN INSERT covers exactly 9 columns in PE-3 order; `PROCTRAN_DATE` receives a `String` (`DD.MM.YYYY`), not a `java.sql.Date`.
- `AbndprocDelegate` carries a `private static final Logger LOGGER` (SLF4J) and logs at `ERROR` level with `abndSqlcode`.

## Technical Decisions

- **JCICS APIs used:** `LocalDateTime.now()` for timestamp (`Task.getTask().getAbstime()` does NOT exist in JCICS); `Task.getTask().abend(ABEND_CODE_HWPT)` for ABEND; `new Program("ABNDPROC").link(bytes)` for the LINK.
- **AbndInfoRec population:** `abndApplid` from `Region.getAPPLID()` (`Task.getTask().getTask().getRegion()...getApplid()` does NOT exist — use `Region.getAPPLID()` directly); `abndProgram` from `Task.getTask().getInvokingProgramName()`; `abndCode` from `CrecustException.ABEND_CODE_HWPT`; `abndTime` formatted `HH:MM:SS` (colon-separated, 8 chars) from `WsTimeNowGrp`; `abndDate` formatted same pattern used in POPULATE-TIME-DATE2; `abndUtimeKey` ← `LocalDateTime.now()` converted to epoch millis as `long` (`Task.getTask().getAbstime()` does NOT exist in JCICS); `abndTasknoKey` ← `Task.getTask().getTaskNumber()` formatted as 4-digit string (these two fields form the `ABND-VSAM-KEY` group — the DB key for the ABND record in ABNDPROC; G4 resolved 2026-10-01).
- **DEQ call site:** this is the third of four DEQ call sites (DEQ-NAMED-COUNTER_DNC010 lines 563–581); it must call `customerNumberService.dequeue(commarea)` before `linkAbndproc`.
- **AbndInfoRec byte width:** ≈673 bytes (12 fields from ABNDINFO.cpy including 600-byte freeform).
- **Lombok:** `AbndInfoRec` uses `@Data @Builder @AllArgsConstructor @NoArgsConstructor` (ADR-12).
- **Source paragraphs:** `WRITE-PROCTRAN-DB2_WPD010` (lines 1321–1458), `DEQ-NAMED-COUNTER_DNC010` (lines 563–581), `PREMIERE_P010` (lines 407–520).

## Cross-Story Dependencies

- Story 8.2 depends on Story 8.1 (`ProctranDbService` exists with the JDBC INSERT already in place; the SQL failure catch block is extended in 8.2).
- Both stories depend on Epic 2 (`AbndInfoRec` model, `AbndInfoRecSerializer`), Epic 6 (`CustomerNumberService.dequeue()`), and Epic 9 (`CrecustException.ABEND_CODE_HWPT`).
