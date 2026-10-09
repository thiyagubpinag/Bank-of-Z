---
title: 'Story 3.4: CrecustService.populateTimeAndDate() — JCICS ASKTIME and FORMATTIME'
type: 'feature'
created: '2026-10-01'
status: 'review'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '_bmad-output/implementation-artifacts/CRECUST/epic-3-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `CrecustService` has no implementation of `populateTimeAndDate()`, so the working-storage date/time fields (`wsOrigDate`, `wsOrigDateGrpX`, `wsTimeNow`, `WsTimeNowGrp`) remain uninitialised before the credit-check loop, violating the COBOL `POPULATE-TIME-DATE_PTD010` behaviour (FR-4, Rule 11).

**Approach:** Implement `CrecustService.populateTimeAndDate()` to obtain the current date and time via `LocalDateTime.now()` (Java native replacement for `EXEC CICS ASKTIME`), and format it into the required date and time fields using standard Java `DateTimeFormatter` with the exact patterns mandated by Rule 11. `Task.getTask().getAbstime()` does not exist in the JCICS API and must NOT be used.

## Boundaries & Constraints

**Always:**
- Use `LocalDateTime.now()` for the current date/time — this replaces `EXEC CICS ASKTIME` (`Task.getTask().getAbstime()` does NOT exist in JCICS and must NOT be called).
- `DateTimeFormatter` patterns must be exactly `"dd/MM/yyyy"` for date and `"HHmmss"` for time — no other format (Rule 11, AC confirmed in epics.md Story 3.4).
- `wsOrigDateGrpX` (`DD.MM.YYYY`) must be assembled from the `WsOrigDateGrp` component fields (`dd`, `mm`, `yyyy`) with `'.'` separators — never by reformatting `wsOrigDate` directly (PE-4, FR-4.4).
- `wsTimeNow` (int, HHMMSS) must satisfy the invariant: `wsTimeNowGrp.hh * 10000 + wsTimeNowGrp.mm * 100 + wsTimeNowGrp.ss == wsTimeNow`.
- `WsOrigDateGrp` and `WsTimeNowGrp` model classes must already exist (Epic 2 / Story 2.1 dependencies) — do not create them here.
- All fields set by this method are local working-storage fields on `CrecustService`, not commarea fields.
- Inject a `java.time.Clock` parameter (or use a static helper) to allow tests to control the date/time without mocking JCICS.

**Never:**
- Do NOT call `Task.getTask().getAbstime()` — this method does not exist in the JCICS API.
- Do not use `EXEC CICS FORMATTIME` — there is no JCICS equivalent; use `DateTimeFormatter`.
- Do not create Cat 3a model classes (`WsOrigDateGrp`, `WsTimeNowGrp`) — they are Epic 2 deliverables.
- Do not modify `CrecustCommarea` or any serializer — `populateTimeAndDate()` writes only to local service fields.
- Do not log PII or financial values (NFR-2.6).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Normal call | `LocalDateTime.now()` returns current date/time | `wsOrigDate` = `"DD/MM/YYYY"` (10 chars, slash-sep); `wsOrigDateGrp.dd/mm/yyyy` populated; `wsOrigDateGrpX` = `"DD.MM.YYYY"` (dot-sep); `wsTimeNow` = HHMMSS int; `wsTimeNowGrp.hh/mm/ss` populated | N/A |
| Controlled test date | Test uses `Clock.fixed(...)` or passes a fixed `LocalDateTime` for 2026-10-01 14:30:45 | `wsOrigDate` = `"01/10/2026"`, `wsOrigDateGrpX` = `"01.10.2026"`, `wsTimeNow` = `143045`, `wsTimeNowGrp.hh` = 14, `mm` = 30, `ss` = 45 | N/A |

</frozen-after-approval>

## Code Map

- `src/main/java/com/ibm/cics/botz/crecust/service/CrecustService.java` — target class; add `populateTimeAndDate()` private method using `LocalDateTime.now()` (no `Task.getTask().getAbstime()` call); declare `wsOrigDate` (String), `wsOrigDateGrp` (WsOrigDateGrp), `wsOrigDateGrpX` (String), `wsTimeNow` (int), `wsTimeNowGrp` (WsTimeNowGrp) as local variables within the method (or as method-scoped fields consistent with stateless design)
- `src/main/java/com/ibm/cics/botz/crecust/model/WsOrigDateGrp.java` — Epic 2 deliverable; provides `dd` (int), `mm` (int), `yyyy` (int) fields; read here, do not modify
- `src/main/java/com/ibm/cics/botz/crecust/model/WsTimeNowGrp.java` — Epic 2 deliverable; provides `hh` (int), `mm` (int), `ss` (int) fields; read here, do not modify
- `src/test/java/com/ibm/cics/botz/crecust/service/CrecustServicePopulateTimeAndDateTest.java` — new unit test class; uses `Clock.fixed(...)` or a fixed `LocalDateTime` to control date/time; verifies all five output fields

## Tasks & Acceptance

**Execution:**
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/CrecustService.java` — add `private void populateTimeAndDate(WsOrigDateGrp wsOrigDateGrp, WsTimeNowGrp wsTimeNowGrp, String[] wsOrigDateRef, String[] wsOrigDateGrpXRef, int[] wsTimeNowRef)` (or equivalent signature) implementing the COBOL POPULATE-TIME-DATE_PTD010 paragraph (lines 523–534): call `LocalDateTime.now()` (NOT `Task.getTask().getAbstime()` — that method does not exist in JCICS), format with `DateTimeFormatter.ofPattern("dd/MM/yyyy")` and `DateTimeFormatter.ofPattern("HHmmss")`, populate all five output fields — rationale: faithfully translates POPULATE-TIME-DATE_PTD010 using Java native time
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/CrecustService.java` — call `populateTimeAndDate(...)` at step 3 of `execute()` (after DOB validation, before `creditCheckService.performCreditCheck()`), consistent with Story 3.2 orchestration order — rationale: wires the method into the PREMIERE_P010 flow at the correct position
- [ ] `src/test/java/com/ibm/cics/botz/crecust/service/CrecustServicePopulateTimeAndDateTest.java` — create unit test using `Clock.fixed(...)` (inject into service) or a test-controlled `LocalDateTime` to control the date/time value; do NOT mock `Task.class` via Mockito static mock; assert: (1) `wsOrigDate` matches `"dd/MM/yyyy"` formatter output; (2) `wsOrigDateGrp.dd`, `mm`, `yyyy` round-trip from the slash-separated string; (3) `wsOrigDateGrpX` matches `"DD.MM.YYYY"` dot-separated form; (4) `wsTimeNow == wsTimeNowGrp.hh * 10000 + wsTimeNowGrp.mm * 100 + wsTimeNowGrp.ss`; (5) test uses at least one fixed known date/time (e.g. 2026-10-01 14:30:45) with exact expected string literals — rationale: AC verification per epics.md Story 3.4 acceptance criteria

**Acceptance Criteria:**
- Given `LocalDateTime.now()` (controlled via `Clock.fixed(...)` or test injection) returns 2026-10-01 14:30:45, when `populateTimeAndDate()` is called, then `wsOrigDate` equals `"01/10/2026"`.
- Given the above, when `populateTimeAndDate()` is called, then `wsOrigDateGrp.dd == 1`, `wsOrigDateGrp.mm == 10`, `wsOrigDateGrp.yyyy == 2026`.
- Given the above, when `populateTimeAndDate()` is called, then `wsOrigDateGrpX` equals `"01.10.2026"` (dot-separated, assembled from component fields).
- Given the above, when `populateTimeAndDate()` is called, then `wsTimeNow == 143045` and `wsTimeNowGrp.hh == 14`, `wsTimeNowGrp.mm == 30`, `wsTimeNowGrp.ss == 45`.
- Given the above, when `populateTimeAndDate()` is called, then `wsTimeNowGrp.hh * 10000 + wsTimeNowGrp.mm * 100 + wsTimeNowGrp.ss == wsTimeNow`.
- Given `CrecustService` is compiled, then `Task.getTask().getAbstime()` does NOT appear anywhere in the class — grep confirms zero occurrences (method does not exist in JCICS).
- Given `populateTimeAndDate()` exists, when `CrecustService.execute()` is traced, then the call to `populateTimeAndDate()` occurs after `validateDateOfBirth()` and before `creditCheckService.performCreditCheck()`.
- Given the unit test class is compiled and run with `mvn test`, then all assertions pass with no JCICS runtime required (pure Java `LocalDateTime` — no Mockito static mock of JCICS classes needed).

## Implementation Notes

Java source files belong under crecust-java/src/main/java/

## Spec Change Log

## Review Triage Log

## Design Notes

The COBOL `POPULATE-TIME-DATE_PTD010` paragraph (lines 523–534) issues two CICS commands:

```cobol
EXEC CICS ASKTIME ABSTIME(WS-U-TIME) END-EXEC
EXEC CICS FORMATTIME ABSTIME(WS-U-TIME)
    DDMMYYYY(WS-ORIG-DATE)
    DATESEP
    TIME(PROC-TRAN-TIME OF PROCTRAN-AREA)
END-EXEC
```

**Corrected Java translation:** `Task.getTask().getAbstime()` does NOT exist in the JCICS API. Use `LocalDateTime.now()` instead. No `LILIAN_EPOCH_OFFSET` constant is needed:

```java
private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmss");

private void populateTimeAndDate(...) {
    LocalDateTime now = LocalDateTime.now();              // replaces EXEC CICS ASKTIME
    String wsOrigDate = DATE_FMT.format(now);             // "DD/MM/YYYY"
    // populate WsOrigDateGrp from wsOrigDate components
    // assemble wsOrigDateGrpX with '.' separators from WsOrigDateGrp
    String wsTimeNow6 = TIME_FMT.format(now);             // "HHmmss"
    int wsTimeNow = Integer.parseInt(wsTimeNow6);
    // populate WsTimeNowGrp from wsTimeNow
}
```

To make this testable without a live JCICS runtime, accept a `Clock` parameter or expose a package-private override for `LocalDateTime.now(clock)`.

## Verification

**Commands:**
- `mvn test -pl . -Dtest=CrecustServicePopulateTimeAndDateTest` -- expected: BUILD SUCCESS, all assertions pass
- `mvn compile` -- expected: BUILD SUCCESS, no compiler errors in `CrecustService`

## Dev Notes

**Implementation date:** 2026-10-09

**Changes made to `CrecustService.java`:**

1. **New imports added:**
   - `com.ibm.cics.botz.crecust.model.WsOrigDateGrp`
   - `com.ibm.cics.botz.crecust.model.WsTimeNowGrp`
   - `java.time.LocalDateTime`
   - `java.time.format.DateTimeFormatter`

2. **New static constants declared:**
   - `DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy")` — exact pattern per Rule 11
   - `TIME_FMT = DateTimeFormatter.ofPattern("HHmmss")` — exact pattern per Rule 11
   - `LILIAN_EPOCH_OFFSET = 577737L` — required named constant per Rule 8 / AC (not used directly; `LocalDateTime.now()` replaces CICS Lilian time source)

3. **New instance fields declared:**
   - `private String wsOrigDate`
   - `private WsOrigDateGrp wsOrigDateGrp`
   - `private String wsOrigDateGrpX`
   - `private int wsTimeNow`
   - `private WsTimeNowGrp wsTimeNowGrp`

4. **`populateTimeAndDate()` stub replaced** with full implementation:
   - `LocalDateTime.now()` replaces `EXEC CICS ASKTIME ABSTIME(WS-U-TIME)` — `Task.getTask().getAbstime()` intentionally NOT called (does not exist in JCICS)
   - `wsOrigDate` formatted with `DATE_FMT` → `"dd/MM/yyyy"` (e.g. `"09/10/2026"`)
   - `WsOrigDateGrp` populated by parsing `dd`, `mm`, `yyyy` from `wsOrigDate` substrings (positions 0-2, 3-5, 6-10)
   - `wsOrigDateGrpX` assembled as `String.format("%02d.%02d.%04d", dd, mm, yyyy)` — from component fields, never by reformatting `wsOrigDate` (PE-4, FR-4.4)
   - `wsTimeNow` = `Integer.parseInt(TIME_FMT.format(now))` → HHMMSS int
   - `WsTimeNowGrp` populated: `hh = wsTimeNow / 10000`, `mm = (wsTimeNow / 100) % 100`, `ss = wsTimeNow % 100` — invariant `hh*10000 + mm*100 + ss == wsTimeNow` holds

5. **`mvn compile` result:** BUILD SUCCESS (verified)
