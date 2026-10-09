---
title: 'Story 4.1: ValidationService.validateTitle() — 11-value title check (FR-2)'
type: 'feature'
created: '2026-10-01'
status: 'draft'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '05b61033e33a7211860a23b46abd5f630420dfa3'
context:
  - '_bmad-output/implementation-artifacts/CRECUST/epic-4-context.md'
  - '_bmad-output/planning-artifacts/CRECUST/epics.md'
  - '_bmad-output/planning-artifacts/CRECUST/architecture.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** In the COBOL program `CRECUST`, incoming customer registration requests must have a valid title. If `COMM-TITLE` is invalid, the transaction must reject the request immediately without performing ENQ or touching DB2.

**Approach:** Implement `ValidationService.validateTitle(CrecustCommarea commarea)` in package `com.ibm.cics.botz.crecust.service`. Check `commarea.getCommTitle()` against the 11 accepted 10-byte title strings (right-padded with spaces). On mismatch, set `commarea.setCommSuccess('N')` and `commarea.setCommFailCode('T')` (silent-return path). On match, leave fail-code untouched.

## Boundaries & Constraints

**Always:**
- Package must be `com.ibm.cics.botz.crecust.service`.
- `ValidationService` must be a stateless service with no instance state (NFR-5.1).
- The 11 accepted titles must be declared as a named constant collection (`Set<String> ACCEPTED_TITLES` or `private static final` constants) — no magic strings at validation call sites (FR-2.3, NFR-2.4).
- The 11 exact values (10 characters each, right-padded with spaces):
  1. `"Professor "`
  2. `"Mr        "`
  3. `"Mrs       "`
  4. `"Miss      "`
  5. `"Ms        "`
  6. `"Dr        "`
  7. `"Drs       "`
  8. `"Lord      "`
  9. `"Sir       "`
  10. `"Lady      "`
  11. `"          "` (10 spaces)
- If validation fails, set `commSuccess = 'N'` and `commFailCode = 'T'` directly on the passed `CrecustCommarea` instance and return immediately (silent-return path, Rule 1).
- If validation succeeds, do not alter `commSuccess` or `commFailCode`.
- Method length must be ≤ 40 lines (NFR-2.2).
- One top-level type per file (Rule 17, NFR-2.3).
- SLF4J logger with no PII logged (NFR-2.5, NFR-2.6).

**Never:**
- Never perform CICS ENQ/DEQ, database operations (DB2), or CICS ABEND within `validateTitle()` or its failure path (FR-2.2).
- Never trim or alter the input title string before comparison unless comparing against canonical 10-byte right-padded constants or matching exact 10-byte length.
- Never throw an unhandled exception for title validation failures; it is a silent-return business error path returning `'T'`.

## I/O & Edge-Case Matrix

| Scenario | Input / State (`commTitle`) | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Valid Title - "Mr        " | `commTitle = "Mr        "` | Returns without modifying `commSuccess` or `commFailCode` | Normal return |
| Valid Title - "Professor " | `commTitle = "Professor "` | Returns without modifying `commSuccess` or `commFailCode` | Normal return |
| Valid Title - All Spaces | `commTitle = "          "` (10 spaces) | Returns without modifying `commSuccess` or `commFailCode` | Normal return |
| Valid Titles (remaining 8) | `"Mrs       "`, `"Miss      "`, `"Ms        "`, `"Dr        "`, `"Drs       "`, `"Lord      "`, `"Sir       "`, `"Lady      "` | Returns without modifying `commSuccess` or `commFailCode` | Normal return |
| Invalid Title - Unknown | `commTitle = "Unknown   "` | `commSuccess = 'N'`, `commFailCode = 'T'` | Silent return |
| Invalid Title - Lowercase / Mixed | `commTitle = "mr        "` or `"PROFESSOR "` | `commSuccess = 'N'`, `commFailCode = 'T'` | Silent return |
| Invalid Title - Empty string / short | `commTitle = ""` or `"Mr"` | `commSuccess = 'N'`, `commFailCode = 'T'` | Silent return |
| Invalid Title - Null | `commTitle = null` | `commSuccess = 'N'`, `commFailCode = 'T'` | Silent return |

</frozen-after-approval>

## Code Map

- `.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl` -- Source COBOL program: lines 407–458 (`PREMIERE_P010` title check)
- `_bmad-output/planning-artifacts/CRECUST/epics.md` -- Epic 4, Story 4.1 specifications and acceptance criteria
- `_bmad-output/planning-artifacts/CRECUST/architecture.md` -- Section 5.3 `ValidationService` architecture
- `_bmad-output/planning-artifacts/CRECUST/prd.md` -- Section FR-2 / BR-1 Title validation requirements
- `crecust-java/src/main/java/com/ibm/cics/botz/crecust/service/ValidationService.java` -- Target service class to create/update
- `crecust-java/src/test/java/com/ibm/cics/botz/crecust/service/ValidationServiceTest.java` -- Target unit test class covering all 11 valid titles and invalid edge cases

## Tasks & Acceptance

**Execution:**
- [x] `crecust-java/src/main/java/com/ibm/cics/botz/crecust/service/ValidationService.java` -- Implement `validateTitle(CrecustCommarea commarea)` -- Enforce 11-value accepted title set, set `'N'` and `'T'` on rejection
- [x] `crecust-java/src/test/java/com/ibm/cics/botz/crecust/service/ValidationServiceTest.java` -- Parameterized unit tests for all 11 valid values and invalid cases -- Verify silent-return behavior and failure code assignment

**Acceptance Criteria:**
- Given a `CrecustCommarea` with any of the 11 accepted titles (`"Professor "`, `"Mr        "`, `"Mrs       "`, `"Miss      "`, `"Ms        "`, `"Dr        "`, `"Drs       "`, `"Lord      "`, `"Sir       "`, `"Lady      "`, `"          "`), when `validateTitle(commarea)` is called, then no fail code is set.
- Given a `CrecustCommarea` with an invalid title (e.g. `"Unknown   "`, `null`, `""`), when `validateTitle(commarea)` is called, then `commarea.getCommSuccess()` is `'N'` and `commarea.getCommFailCode()` is `'T'`.
- All 11 accepted titles are defined as named constants in a `Set<String>` constant collection without magic strings in the method body.
- No DB2 or CICS ENQ/DEQ operations are invoked.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Design Notes

COBOL Source logic (`CRECUST.cbl:413-457`):
```cobol
     MOVE ' ' TO WS-TITLE-VALID.
     EVALUATE COMM-TITLE
     WHEN 'Professor'
     WHEN 'Mr       '
     WHEN 'Mrs      '
     WHEN 'Miss     '
     WHEN 'Ms       '
     WHEN 'Dr       '
     WHEN 'Drs      '
     WHEN 'Lord     '
     WHEN 'Sir      '
     WHEN 'Lady     '
     WHEN '         '
          MOVE 'Y' TO WS-TITLE-VALID
     WHEN OTHER
          MOVE 'N' TO WS-TITLE-VALID
     END-EVALUATE.

     IF WS-TITLE-VALID = 'N'
        MOVE 'N' TO COMM-SUCCESS
        MOVE 'T' TO COMM-FAIL-CODE
        GOBACK
     END-IF
```

In Java, `ACCEPTED_TITLES` is initialized with a `Set.of(...)` containing the 11 exact 10-byte values. `validateTitle` checks membership and sets `commSuccess = 'N'` and `commFailCode = 'T'` if not present.

## Verification

**Commands:**
- `mvn test -Dtest=ValidationServiceTest` -- expected: All 11 valid title tests pass, invalid title tests pass, no exceptions thrown.
