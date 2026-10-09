# Epic 4 Context: Input Validation — Title and Date-of-Birth

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Author `ValidationService` with two methods — `validateTitle()` and `validateDateOfBirth()` — that implement all silent-return error paths for invalid title and DOB conditions. The COBOL `CEEDAYS` / `CEELOCT` LE calls are replaced entirely with `java.time`. On any validation failure the commarea fields `commSuccess` and `commFailCode` are set and the method returns; no ENQ, DEQ, or DB2 access occurs.

## Stories

- Story 4.1: `ValidationService.validateTitle()` — 11-value title check (FR-2)
- Story 4.2: `ValidationService.validateDateOfBirth()` — `java.time` DOB checks (FR-3)

## Requirements & Constraints

**Title validation (FR-2)**
- `COMM-TITLE` is 10 bytes, right-padded with spaces. Accepted values (exactly): `"Professor "`, `"Mr        "`, `"Mrs       "`, `"Miss      "`, `"Ms        "`, `"Dr        "`, `"Drs       "`, `"Lord      "`, `"Sir       "`, `"Lady      "`, `"          "` (all spaces). Any other value is a failure.
- On failure: set `commSuccess = 'N'`, `commFailCode = 'T'`; return immediately. No ENQ, no DB2 access.
- The accepted-title set must be a named constant collection — no raw string literals at the call site (NFR-2.4 / FR-2.3).

**DOB validation (FR-3)**
- If `commDobYear < 1601`: set `commFailCode = 'O'`, `commSuccess = 'N'`; return.
- Construct `LocalDate.of(commDobYear, commDobMonth, commDobDay)`. If construction throws `DateTimeException`: set `commFailCode = 'Z'`, `commSuccess = 'N'`; return.
- If `today.getYear() - commDobYear > 150`: set `commFailCode = 'O'`, `commSuccess = 'N'`; return.
- If DOB Lilian day count > today's Lilian day count (DOB is in the future): set `commFailCode = 'Y'`, `commSuccess = 'N'`; return.
- A valid DOB causes no fail-code to be set.
- `LocalDate.now()` replaces `CEELOCT`; `LocalDate.of(...)` + `toEpochDay()` + Lilian offset replaces `CEEDAYS`.

**Required named constants (all `private static final int` in `ValidationService`):**
- `MIN_DOB_YEAR = 1601` (FR-3.1)
- `MAX_CUSTOMER_AGE = 150` (FR-3.4)
- `LILIAN_EPOCH_OFFSET` — difference in days between the Lilian epoch (15 Oct 1582) and the Java epoch (1 Jan 1970), used to replicate `CEEDAYS` semantics (FR-3.2)

**Error-path classification (Rule 1 / NFR-3.2):** Both validation paths are **silent-return** paths — set fail-code and return; no notification, no logging beyond standard `DEBUG`, no side effects.

**No copybook placeholders (AC-9.1):** All copybooks were resolved; no `// UNRESOLVED` constants may exist.

## Technical Decisions

**Package and class shape:**
- `ValidationService` lives in `com.ibm.cics.botz.crecust.service`.
- No instance state (NFR-5.1); all logic operates on method parameters and local variables.
- Lombok is enabled on data-model classes (ADR-12); `ValidationService` itself is a stateless service class — no Lombok data annotations needed.

**Commarea model:** `CrecustCommarea` (from Epic 2, `com.ibm.cics.botz.crecust.model`). Both methods accept a `CrecustCommarea` instance and mutate `commSuccess` / `commFailCode` directly on it.

**Calling context (from `CrecustService`, Epic 3):** `validateTitle()` is called first, then `validateDateOfBirth()`. Either method may set a fail-code and return; the orchestrator checks and short-circuits before proceeding to credit-check / ENQ / DB2.

**`java.time` date semantics:**
- `LocalDate.of(year, month, day)` — replaces `CEEDAYS`. Throws `DateTimeException` on invalid calendar dates, which maps to fail-code `'Z'`.
- `LocalDate.now()` — replaces `CEELOCT`.
- Lilian day count = `localDate.toEpochDay() + LILIAN_EPOCH_OFFSET`.

**Coding standards (NFR-2):**
- Methods must not exceed 40 lines.
- No magic numbers or strings; all sentinels are named constants.
- SLF4J `Logger` (`private static final`); `INFO` for milestones, `DEBUG` for per-record, `ERROR` for failures.
- No PII logged at any level (NFR-2.6).

## Cross-Story Dependencies

- **Depends on Epic 1** (Maven project and build infrastructure) — must exist before code can compile.
- **Depends on Epic 2** — `CrecustCommarea` model class (Story 2.1) must exist; `commTitle`, `commDobYear`, `commDobMonth`, `commDobDay`, `commSuccess`, and `commFailCode` fields must be present and accessible.
- **Depends on Epic 3** — `CrecustService` (Story 3.2) calls `ValidationService`; the orchestrator wiring must accept the service. `ValidationService` itself has no dependency on Epic 3.
- **No dependency between Story 4.1 and Story 4.2** — they can be implemented independently and composed in `CrecustService`.
- **Downstream:** Epics 5–8 only execute if both validations pass; `ValidationService` is a gate before all ENQ / credit-check / DB2 work.
