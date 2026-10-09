---
title: 'Story 4.2: ValidationService.validateDateOfBirth() — java.time DOB checks (FR-3)'
type: 'feature'
created: '2026-10-01'
status: 'draft'
route: 'dispatch'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The COBOL `DATE-OF-BIRTH-CHECK_DOBC010` paragraph (lines 1556–1609) validates the customer date-of-birth using LE runtime calls `CEEDAYS` and `CEELOCT` that have no Java equivalent. Without a Java replacement, the DOB gate before ENQ / DB2 work cannot be implemented.

**Approach:** Implement `ValidationService.validateDateOfBirth(CrecustCommarea)` using `java.time` (`LocalDate.of`, `LocalDate.now`, `toEpochDay`) to replicate all four COBOL validation checks (year floor, calendar validity, age ceiling, future-date guard). All four checks are silent-return paths: set `commFailCode` and `commSuccess = 'N'` and return; no logging beyond DEBUG, no side effects.

## Boundaries & Constraints

**Always:**
- All four DOB checks must be implemented in the order they appear in COBOL: year-floor check → `LocalDate.of` construction (DateTimeException catch) → age-ceiling check → Lilian future-date check.
- Named constants `MIN_DOB_YEAR = 1601`, `MAX_CUSTOMER_AGE = 150`, and `LILIAN_EPOCH_OFFSET` must be declared as `private static final int` in `ValidationService`.
- `LILIAN_EPOCH_OFFSET` must be the exact day-count difference between the Lilian epoch (15 Oct 1582) and the Java epoch (1 Jan 1970). Computed as `LocalDate.of(1970, 1, 1).toEpochDay() - LocalDate.of(1582, 10, 15).toEpochDay()` = 141427 days; this value must be the declared constant.
- Every validation failure is a silent-return path (Rule 1 / NFR-3.2): set fail-code, set `commSuccess = 'N'`, return. No exception thrown, no notification call, no logging above DEBUG.
- No `// UNRESOLVED` placeholder constants (all copybooks resolved — AC-9.1).
- Method must not exceed 40 lines (NFR-2).
- `ValidationService` lives in package `com.ibm.cics.botz.crecust.service`; no constructor injection or instance state (NFR-5.1).

**Never:**
- Do not call `CEEDAYS`, `CEELOCT`, or any non-`java.time` date library.
- Do not throw a `CrecustException` or any exception from this method — silent return only.
- Do not add logging above DEBUG level for validation failures (no PII).
- Do not add ENQ, DB2 access, or any other side effect.
- Do not create a new class — `validateDateOfBirth` is a method added to the existing `ValidationService` class that Story 4.1 creates.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Year below floor | `commDobYear = 1600` | `commFailCode = 'O'`, `commSuccess = 'N'`, return | silent-return |
| Year at floor (pass) | `commDobYear = 1601` | no fail-code set from year check | — |
| Invalid calendar date | `commDobYear=2000, commDobMonth=2, commDobDay=31` | `commFailCode = 'Z'`, `commSuccess = 'N'`, return | `DateTimeException` caught — silent-return |
| Age exceeds ceiling | Valid `LocalDate`; `today.getYear() - commDobYear = 151` | `commFailCode = 'O'`, `commSuccess = 'N'`, return | silent-return |
| Age at ceiling (pass) | `today.getYear() - commDobYear = 150` | no fail-code set from age check | — |
| DOB in future | `LocalDate` where `localDob.toEpochDay() + LILIAN_EPOCH_OFFSET > today.toEpochDay() + LILIAN_EPOCH_OFFSET` | `commFailCode = 'Y'`, `commSuccess = 'N'`, return | silent-return |
| Valid DOB | e.g. `1990-06-15` within age and not future | no fail-code set; method returns normally | — |

</frozen-after-approval>

## Code Map

- `src/main/java/com/ibm/cics/botz/crecust/service/ValidationService.java` — target class; Story 4.1 creates it with `validateTitle()`; this story adds `validateDateOfBirth()` to the same file. Does not exist yet — will be created by Story 4.1 before this story runs.
- `src/main/java/com/ibm/cics/botz/crecust/model/CrecustCommarea.java` — model class (Epic 2 / Story 2.1); provides `getCommDobYear()`, `getCommDobMonth()`, `getCommDobDay()`, `setCommSuccess(char)`, `setCommFailCode(char)`. Does not exist yet; must exist before this story compiles.
- `_bmad-output/planning-artifacts/CRECUST/technical-research.md` — PE-2 (validation constants), PE-8 (fail-code inventory `'O'`, `'Y'`, `'Z'`), PE-9 (DISPLAY diagnostics — DEBUG only), TRA-3 (CEEDAYS / CEELOCT Java replacement rationale).
- `_bmad-output/planning-artifacts/CRECUST/epics.md` lines 650–694 — Story 4.2 acceptance criteria (authoritative source for the four checks and the three named constants).
- `_bmad-output/implementation-artifacts/CRECUST/epic-4-context.md` — DOB validation requirements, Lilian-epoch arithmetic, coding standards.

## Tasks & Acceptance

**Execution:**
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/ValidationService.java` — add three named constants (`MIN_DOB_YEAR`, `MAX_CUSTOMER_AGE`, `LILIAN_EPOCH_OFFSET`) and method `validateDateOfBirth(CrecustCommarea commarea)` implementing the four ordered DOB checks using `java.time`; add SLF4J `DEBUG` log entries mirroring COBOL `DISPLAY` diagnostics (lines 1556–1609 of DOBC010).
- [ ] `src/test/java/com/ibm/cics/botz/crecust/service/ValidationServiceDobTest.java` — unit tests covering all seven I/O matrix scenarios; use a mock or stub `CrecustCommarea`; verify `commFailCode` and `commSuccess` for each failure path and the valid-DOB pass-through.

**Acceptance Criteria:**
- Given `commDobYear = 1600`, when `validateDateOfBirth(commarea)` is called, then `commFailCode` is `'O'` and `commSuccess` is `'N'`.
- Given a calendar date that causes `LocalDate.of()` to throw `DateTimeException` (e.g. month=2, day=31, year=2000), when `validateDateOfBirth(commarea)` is called, then `commFailCode` is `'Z'` and `commSuccess` is `'N'`.
- Given a valid `LocalDate` where `today.getYear() - commDobYear = 151`, when `validateDateOfBirth(commarea)` is called, then `commFailCode` is `'O'` and `commSuccess` is `'N'`.
- Given a DOB whose Lilian day count exceeds today's Lilian day count (DOB in the future), when `validateDateOfBirth(commarea)` is called, then `commFailCode` is `'Y'` and `commSuccess` is `'N'`.
- Given a valid DOB (e.g. `1990-06-15`), when `validateDateOfBirth(commarea)` is called, then neither `commFailCode` nor `commSuccess` is mutated by this method.
- `ValidationService` declares `private static final int MIN_DOB_YEAR = 1601`, `MAX_CUSTOMER_AGE = 150`, and `LILIAN_EPOCH_OFFSET = 141427`.
- No `// UNRESOLVED` comments exist in `ValidationService`.
- `validateDateOfBirth` does not exceed 40 lines.
- No exception is thrown or propagated from `validateDateOfBirth` under any input.

## Implementation Notes

- Implemented `ValidationService.validateDateOfBirth(CrecustCommarea commarea)` replacing the stub with full Java date logic (`java.time`).
- Maintained the exact 4-check order matching COBOL `DOBC010`:
  1. Year floor check (`MIN_DOB_YEAR = 1601`) setting `commFailCode = 'O'` and `commSuccess = 'N'`.
  2. Calendar validity via `LocalDate.of(year, month, day)` catching `DateTimeException` / parsing failures and setting `commFailCode = 'Z'` and `commSuccess = 'N'`.
  3. Maximum age check (`MAX_CUSTOMER_AGE = 150`) against `today.getYear()` setting `commFailCode = 'O'` and `commSuccess = 'N'`.
  4. Future date check via Lilian day arithmetic (`LILIAN_EPOCH_OFFSET = 141427L`) setting `commFailCode = 'Y'` and `commSuccess = 'N'`.
- Configured SLF4J logger with `DEBUG` level messages only (no PII logged).
- Maintained silent-return semantics across all validation paths.
- Verified compilation with `mvn compile` (BUILD SUCCESS).

## Spec Change Log

## Review Triage Log

## Design Notes

**Lilian epoch arithmetic:** The COBOL `CEEDAYS` call returns a Lilian day number (days since 14 Oct 1582, i.e. day 1 = 15 Oct 1582). Java's `LocalDate.toEpochDay()` counts from 1 Jan 1970. The constant `LILIAN_EPOCH_OFFSET = 141427` is the number of days from 15 Oct 1582 to 1 Jan 1970 inclusive, computed as `LocalDate.of(1970,1,1).toEpochDay() - LocalDate.of(1582,10,15).toEpochDay()`. Lilian day for any `LocalDate d` = `d.toEpochDay() + LILIAN_EPOCH_OFFSET`. The future-date check is therefore: `if (dob.toEpochDay() > today.toEpochDay())` — the `LILIAN_EPOCH_OFFSET` cancels from both sides, so the comparison is a plain `toEpochDay()` comparison. Declare `LILIAN_EPOCH_OFFSET` regardless, as the epics spec requires it explicitly (FR-3.2 traceability).

**Check order must mirror COBOL DOBC010:**
1. Year-floor guard (`< MIN_DOB_YEAR` → fail `'O'`).
2. `LocalDate.of(year, month, day)` in `try` block; `catch (DateTimeException)` → fail `'Z'`.
3. Age-ceiling guard (`today.getYear() - year > MAX_CUSTOMER_AGE` → fail `'O'`).
4. Future-date guard (`dob.toEpochDay() > today.toEpochDay()` → fail `'Y'`).

**No class creation:** `ValidationService` is created by Story 4.1 with the `validateTitle()` method, `Logger`, and class-level boilerplate. This story only adds the three constants and the new method to the file that already exists.

## Verification

**Commands:**
- `mvn test -pl . -Dtest=ValidationServiceDobTest -q` — expected: `BUILD SUCCESS`, all 7 scenario tests pass.
- `mvn checkstyle:check -pl .` — expected: no violations (method length ≤ 40 lines enforced here if Checkstyle is configured).
