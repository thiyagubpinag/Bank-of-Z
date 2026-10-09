---
status: review
route: oneshot
epic: 3
story: 1
---

# Story 3.1 — `AccountCreationValidator.validateAccountType()` — account type validation

## Intent

Implement `AccountCreationValidator.validateAccountType(String accType)` so that only the five
valid account types (`ISA`, `MORTGAGE`, `SAVING`, `CURRENT`, `LOAN`) are accepted, with failure
returning fail code `'A'`.

## Acceptance Criteria

- **AC-1** Method signature: `void validateAccountType(String accType) throws CreaccValidationException`.
- **AC-2** Validation uses prefix matching against exactly: `"ISA"` (chars 1–3), `"MORTGAGE"`
  (chars 1–8), `"SAVING"` (chars 1–6), `"CURRENT"` (chars 1–7), `"LOAN"` (chars 1–4).
  No other values are valid.
- **AC-3** On mismatch: throws `CreaccValidationException.invalidAccountType(accType)` (named
  factory, Rule 3). The fail code carried by the exception is `'A'`.
- **AC-4** On match: returns normally (no side effects).
- **AC-5** All type-literal strings are declared as `private static final String` constants — no
  magic string literals in the method body (Rule 8 / AC-RULE-8a).
- **AC-6** Method body ≤ 40 lines.
- **AC-7** No Spring annotations; constructor is the only injection point.
- **AC-8** Unit test: each of the five valid types (with and without trailing spaces) does NOT throw.
- **AC-9** Unit test: `"INVALID "` and `""` throw `CreaccValidationException` with fail code `'A'`.

## Source

`PREMIERE_P010` (lines 303–426) → calls `ACCOUNT-TYPE-CHECK_ATC010` (lines 1304–1319 in
`CREACC.cbl`).

COBOL logic:
```cobol
EVALUATE TRUE
WHEN COMM-ACC-TYPE IN DFHCOMMAREA(1:3) = 'ISA'
WHEN COMM-ACC-TYPE IN DFHCOMMAREA(1:8) = 'MORTGAGE'
WHEN COMM-ACC-TYPE IN DFHCOMMAREA(1:6) = 'SAVING'
WHEN COMM-ACC-TYPE IN DFHCOMMAREA(1:7) = 'CURRENT'
WHEN COMM-ACC-TYPE IN DFHCOMMAREA(1:4) = 'LOAN'
     MOVE 'Y' TO COMM-SUCCESS OF DFHCOMMAREA
WHEN OTHER
     MOVE 'N' TO COMM-SUCCESS OF DFHCOMMAREA
     MOVE 'A' TO COMM-FAIL-CODE IN DFHCOMMAREA
END-EVALUATE.
```

## Dependencies

- Story 2.8 (`DateTimeFormatConstants` / `CreaccLayoutConstants`) — not directly referenced by
  this story but available in the same package.
- Story 5.6 exception hierarchy — `CreaccException` base and `CreaccValidationException` stubs
  created as part of this story to unblock implementation.

## Files Created / Modified

| File | Action |
|---|---|
| `creacc-java/src/main/java/com/ibm/bankofz/creacc/exception/CreaccException.java` | Created |
| `creacc-java/src/main/java/com/ibm/bankofz/creacc/exception/CreaccValidationException.java` | Created |
| `creacc-java/src/main/java/com/ibm/bankofz/creacc/service/AccountCreationValidator.java` | Created |
| `creacc-java/src/test/java/com/ibm/bankofz/creacc/service/AccountCreationValidatorTest.java` | Created |

## Design Notes

- **COBOL prefix matching → Java `startsWith()`**: COBOL `COMM-ACC-TYPE(1:N) = 'XXX'` checks the
  first N characters of the 8-char field. Java `String.startsWith(prefix)` is the exact equivalent
  when the input string may be padded with trailing spaces.
- **Silent-return path (ADR-03 Rule 3)**: On account-type mismatch, COBOL sets
  `COMM-SUCCESS = 'N'` and `COMM-FAIL-CODE = 'A'` then returns. The Java translation throws
  `CreaccValidationException` (fail code `'A'`); the caller (`CreaccAccountService`) catches and
  routes this to `CreaccExceptionHandler.handleValidationException()` which sets those commarea
  fields — no ABEND is raised.
- **`CreaccValidationException` stub scope**: All seven named factories (Story 5.6 AC) are
  included here so that the exception class is complete and consistent from the start. Future
  stories that reference the same factories will find them already in place.
