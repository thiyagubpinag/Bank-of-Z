---
title: 'CreditCheckService review-date computation (FR-6)'
type: 'feature'
created: '2026-10-01'
status: 'draft'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '_bmad-output/implementation-artifacts/CRECUST/epic-5-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `CreditCheckService` has no method to compute and write the credit-score review date into the commarea. Both the success path (today + 1–20 random days) and the failure path (today's date) must produce identical `DDMMYYYY` byte layout in `COMM-CS-REVIEW-DATE`.

**Approach:** Add `computeReviewDate(CrecustCommarea commarea, long eibtaskn)` to `CreditCheckService`. On the success path, derive a random offset from `eibtaskn` using the COBOL `RANDOM(EIBTASKN)` formula and call `LocalDate.plusDays()`; on the failure path, populate with today's components. Decompose the resolved `LocalDate` into `commCsReviewDay`, `commCsReviewMonth`, `commCsReviewYear` integer fields. The constant `REVIEW_DATE_MAX_DAYS = 21` is declared in `CreditCheckService` (FR-6.1, Rule 8).

## Boundaries & Constraints

**Always:**
- `REVIEW_DATE_MAX_DAYS` must be a named `private static final int` constant with value `21`, traceable to the COBOL formula `(21 - 1) * RANDOM(EIBTASKN)) + 1` (Rule 8 / AC-8.1).
- On success, the offset formula is `((REVIEW_DATE_MAX_DAYS - 1) * random(eibtaskn)) + 1` — result is always in [1, 20] inclusive.
- On failure, `commCsReviewDay`, `commCsReviewMonth`, `commCsReviewYear` are set to today's `getDayOfMonth()`, `getMonthValue()`, `getYear()`.
- Both paths write the three integer fields on `CrecustCommarea`; the `CrecustareaSerializer` handles the `DDMMYYYY` byte layout (TRA-5 — no direct byte manipulation in `CreditCheckService`).
- All fail-code values for the credit-check path are set via named factory methods on `CrecustException` — no raw character literals (Rule 3).
- `CreditCheckService` must hold no per-request state in instance fields (NFR-5); `eibtaskn` is a method parameter.
- `computeReviewDate` is a `void` method; it writes directly into the passed `commarea` object.

**Never:**
- Do not manipulate `COMM-CS-REVIEW-DATE` as a raw byte array or string inside `CreditCheckService` — byte layout is owned by `CrecustareaSerializer`.
- Do not hardcode `21`, `20`, or `1` as numeric literals in logic expressions — use `REVIEW_DATE_MAX_DAYS`.
- Do not add per-request instance fields to `CreditCheckService`.
- Do not implement a COBOL SYSIDERR retry loop (ADR-8 — declared but never used in COBOL procedure).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Success — at least one agency replied | `wsRetrievedCnt > 0`, `eibtaskn = 12345` | `commCsReviewDay/Month/Year` = components of `today.plusDays(offset)` where `offset = ((21-1) * random(12345)) + 1` ∈ [1,20] | No error path |
| Failure — zero agencies replied | `wsRetrievedCnt == 0` | `commCsReviewDay/Month/Year` = today's `getDayOfMonth()`, `getMonthValue()`, `getYear()` | No error path |
| Boundary — `eibtaskn` produces `random = 0.0` | Minimum random value | `offset = ((21-1) * 0.0) + 1 = 1` → `today.plusDays(1)` (tomorrow) | No error path |
| Boundary — `eibtaskn` produces `random` near `1.0` | Maximum random value approaching 1 | `offset` approaches but does not reach `21` → at most `today.plusDays(20)` | No error path |

</frozen-after-approval>

## Code Map

- `src/main/java/com/ibm/cics/botz/crecust/service/CreditCheckService.java` — add `computeReviewDate(CrecustCommarea, long)` and `REVIEW_DATE_MAX_DAYS` constant; this file is being built incrementally across Stories 5.1–5.4; Story 5.4 adds the final method
- `src/main/java/com/ibm/cics/botz/crecust/model/CrecustCommarea.java` — target for `commCsReviewDay`, `commCsReviewMonth`, `commCsReviewYear` setters (fields defined in Epic 2 Story 2.1, offsets 389–396 per PE-1)
- `src/main/java/com/ibm/cics/botz/crecust/serializer/CrecustareaSerializer.java` — owns the DDMMYYYY byte layout for `COMM-CS-REVIEW-DATE`; `computeReviewDate` must NOT write bytes here directly
- `src/main/java/com/ibm/cics/botz/crecust/exception/CrecustException.java` — provides named factory methods for any fail-code set in this path (none in review-date itself, but imports already present from 5.3)
- COBOL source reference: `CREDIT-CHECK_CC010` lines 605–1131 (review-date computation at the tail of the paragraph; TRA-5 for dual encoding, PE-2 for the `21` constant)

## Tasks & Acceptance

**Execution:**
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/CreditCheckService.java` — add `private static final int REVIEW_DATE_MAX_DAYS = 21;` constant alongside the other constants at the top of the class — required by Rule 8 / AC-8.1; traces to COBOL formula `(21 - 1) * RANDOM(EIBTASKN)) + 1`
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/CreditCheckService.java` — implement `public void computeReviewDate(CrecustCommarea commarea, long eibtaskn, int wsRetrievedCnt)`: on success path (`wsRetrievedCnt > 0`) compute `LocalDate reviewDate = LocalDate.now().plusDays((long)(((REVIEW_DATE_MAX_DAYS - 1) * Math.random()) + 1))` seeded from `eibtaskn` (see Design Notes for COBOL RANDOM equivalent); set `commarea.setCommCsReviewDay(reviewDate.getDayOfMonth())`, `commarea.setCommCsReviewMonth(reviewDate.getMonthValue())`, `commarea.setCommCsReviewYear(reviewDate.getYear()); `on failure path (`wsRetrievedCnt == 0`) set the three fields to `LocalDate.now()` components — mirrors COBOL `WS-ORIG-DATE-DD/MM/YYYY` failure path (FR-6.2)
- [ ] `src/test/java/com/ibm/cics/botz/crecust/service/CreditCheckServiceTest.java` — add test `computeReviewDate_success_dateWithinRange`: mock `LocalDate.now()` as a known date (e.g., 2026-01-15), call `computeReviewDate(commarea, seededEibtaskn, 2)`, assert `commCsReviewDay/Month/Year` produce a date in `[today+1, today+20]` inclusive
- [ ] `src/test/java/com/ibm/cics/botz/crecust/service/CreditCheckServiceTest.java` — add test `computeReviewDate_failure_dateTodaysDate`: call `computeReviewDate(commarea, 0L, 0)`, assert `commCsReviewDay == today.getDayOfMonth()`, `commCsReviewMonth == today.getMonthValue()`, `commCsReviewYear == today.getYear()`
- [ ] `src/test/java/com/ibm/cics/botz/crecust/service/CreditCheckServiceTest.java` — add test `computeReviewDate_byteLayout_ddmmyyyy`: call `CrecustareaSerializer.toBytes(commarea)` after `computeReviewDate`, inspect bytes at offset 389–396 and assert layout matches `DDMMYYYY` (no separators, day first) — verifies TRA-5 / AC-11.5

**Acceptance Criteria:**
- Given `wsRetrievedCnt > 0`, when `computeReviewDate(commarea, eibtaskn, wsRetrievedCnt)` is called, then `commCsReviewDay`, `commCsReviewMonth`, `commCsReviewYear` are set to a date between `today+1` and `today+20` inclusive (FR-6.1).
- Given `wsRetrievedCnt == 0`, when `computeReviewDate(commarea, eibtaskn, wsRetrievedCnt)` is called, then `commCsReviewDay`, `commCsReviewMonth`, `commCsReviewYear` equal today's day, month, and year respectively (FR-6.2).
- Given any call to `computeReviewDate`, when `CrecustareaSerializer.toBytes(commarea)` is called, then bytes at offsets 389–390 = day (2-byte `99`), 391–392 = month (2-byte `99`), 393–396 = year (4-byte `9999`) — DDMMYYYY layout, no separators (FR-6.3 / AC-11.5 / TRA-5).
- `CreditCheckService` declares `private static final int REVIEW_DATE_MAX_DAYS = 21` and uses it in all offset computations — no literal `21` in logic expressions (Rule 8 / AC-8.1).
- `CreditCheckService` holds no per-request instance fields after Story 5.4 is added (NFR-5).
- The method is `void`; no return value (results communicated exclusively through the commarea parameter).
- All unit tests in `CreditCheckServiceTest` pass (`mvn test -pl . -Dtest=CreditCheckServiceTest`).

## Implementation Notes

**Dev notes (Story 5-4, 2026-10-09):**

- Implemented `CreditCheckService.computeReviewDate(CrecustCommarea commArea, long eibtaskn, int wsRetrievedCnt)` (package-private `void`, no instance state — NFR-5). COBOL source: CRECUST.cbl lines 819–853 (NOTFINISHED + some retrieved) and 927–960 (NOTFND + some retrieved).
- Success path: `daysOffset = (int)((REVIEW_DATE_MAX_DAYS - 1) * randomFromTask(eibtaskn)) + 1` ∈ [1, 20]; `LocalDate.now().plusDays(daysOffset)` replaces `INTEGER-OF-DATE` + add + `DATE-OF-INTEGER`.
- `randomFromTask(long)` returns `(taskNumber % 100) / 100.0` ∈ [0.00, 0.99] — a deterministic stand-in for `FUNCTION RANDOM(WS-SEED)` per the dispatch instruction (supersedes the `new Random(eibtaskn)` sketch in Design Notes). It is not bit-identical to LE `RANDOM`; offsets match the COBOL range [1, 20].
- Failure path: today's day/month/year (mirrors `STRING WS-ORIG-DATE-DD/MM/YYYY INTO COMM-CS-REVIEW-DATE`).
- Commarea fields are `String` (`commCsReviewDay/Month/Year`, PIC 99/99/9999 DISPLAY), so values are zero-padded via `String.format("%02d"/"%04d")` rather than ints as the story Tasks text suggested.
- `REVIEW_DATE_MAX_DAYS = 21` verified present (added in Story 5-1); no literal 21/20 in logic.
- Wiring in `performCreditCheck()`: on success, `eibtaskn = Task.getTask().getTaskNumber()` (EIBTASKN → WS-SEED) then `computeReviewDate(commArea, eibtaskn, wsRetrievedCnt)`. On any fetchAny failure (fail-codes C–H, all of which in COBOL STRING WS-ORIG-DATE into COMM-CS-REVIEW-DATE), `computeReviewDate(commArea, 0L, 0)` sets today's date before the silent return. Fail-codes A/B (PUT CONTAINER / RUN TRANSID) do not set the review date in COBOL and are unchanged.
- Note: COBOL NOTFND + zero retrieved (lines 896–905) sets review date to today and `WS-CREDIT-CHECK-ERROR='Y'` but no fail-code; in Java `fetchAny` returns 0 without setting `commSuccess='N'`, so `computeReviewDate(commArea, eibtaskn, 0)` takes the failure branch → today. Behaviour matches.
- Byte layout verified (no change): `CrecustareaSerializer` writes COMM-CS-REVIEW-DAY @389 (2), COMM-CS-REVIEW-MONTH @391 (2), COMM-CS-REVIEW-YEAR @393 (4) — contiguous 389–396 = DDMMYYYY, no separators, matching COBOL `COMM-CS-REVIEW-DATE(1:2)/(3:2)/(5:4)` moves.
- Build: `mvn compile` → BUILD SUCCESS. Unit tests listed in Tasks (`CreditCheckServiceTest`) were not created in this dispatch (classes guardrail: only `CreditCheckService` modified).

## Spec Change Log

## Review Triage Log

## Design Notes

**COBOL RANDOM(EIBTASKN) equivalent in Java:**

The COBOL source uses `FUNCTION RANDOM(EIBTASKN)` to produce a pseudo-random value in [0, 1). Java's `Math.random()` does not accept a seed per-call. To replicate deterministic seeding from `EIBTASKN`:

```java
// Replicate COBOL FUNCTION RANDOM(EIBTASKN) — seed once per call
double randomValue = new Random(eibtaskn).nextDouble(); // [0.0, 1.0)
int offset = (int)((REVIEW_DATE_MAX_DAYS - 1) * randomValue) + 1; // [1, 20]
LocalDate reviewDate = LocalDate.now().plusDays(offset);
```

This matches the COBOL behaviour: the same `EIBTASKN` produces the same review date offset within a transaction.

**DDMMYYYY byte layout (TRA-5):**

`CrecustCommarea` exposes `commCsReviewDay` (int, 2 digits), `commCsReviewMonth` (int, 2 digits), `commCsReviewYear` (int, 4 digits). `CrecustareaSerializer` at offsets 389–396 serialises these as three consecutive display-numeric fields: `99` + `99` + `9999` = DDMMYYYY. No explicit string concatenation is needed in the service layer.

## Verification

**Commands:**
- `mvn test -pl . -Dtest=CreditCheckServiceTest` -- expected: BUILD SUCCESS, all `computeReviewDate_*` tests green
- `mvn verify -pl .` -- expected: BUILD SUCCESS, no checkstyle or compilation errors
