# Epic 3 Context: Business Logic

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Implement the three core business logic services: account type validation, statement date calculation, and CICS Named Counter account number allocation. These services have no Spring annotations and use constructor injection throughout. All classes depend on data models from Epic 2 and exception classes from Epic 5.6.

## Stories

- Story 3.1: `AccountCreationValidator.validateAccountType()` — account type validation
- Story 3.2: `StatementDateCalculator.calculateMilestones()` — date calculation with explicit leap-year
- Story 3.3: `NamedCounterService.allocateNextAccountNumber()` — account number allocation

## Requirements & Constraints

- No Spring annotations anywhere in Epic 3 output (`@Component`, `@Service`, `@Autowired`, etc.).
- Constructor injection only — no field or setter injection.
- All string literals for account types declared as `private static final String` constants (Rule 8 / AC-RULE-8a).
- Method body ≤ 40 lines; class ≤ 2000 lines; UPPER_SNAKE_CASE constants, camelCase methods/fields.
- Each class in its own `.java` file (Rule 17).
- Exception classes (`CreaccValidationException`, `CreaccSystemException`) must be declared with named static factory methods — no raw string literals at call sites (Rules 3, 4).
- Silent-return paths (validation failures) do NOT trigger CICS ABEND; notifying-abort paths (system failures) write to `AbndinfoRec` then throw (Rule 1, ADR-03).

## Technical Decisions

- **ADR-03 (Exception Handling)**: `AccountCreationValidator.validateAccountType()` is a silent-return path — on mismatch it throws `CreaccValidationException.invalidAccountType(accType)` with fail code `'A'`. No ABEND, no notification.
- **ADR-05 (Dates)**: All date fields use `java.time.LocalDate`. The COBOL leap-year algorithm (`year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)`) is replicated verbatim in `StatementDateCalculator.isLeapYear()` without delegating to `Year.isLeap()`.
- **ADR-07 (Named Counter)**: `NamedCounterService` uses JCICS Named Counter with `NCS_RESOURCE_BASE + sortCode` resource name; falls back to `ControlDao` CONTROL table when response is non-zero. ENQ/DEQ is the caller's responsibility, not `NamedCounterService`.
- **Package**: All service classes live in `com.ibm.bankofz.creacc.service`; exception classes in `com.ibm.bankofz.creacc.exception`.
- Story 3.2 (`StatementDateCalculator`) is already implemented and at `review` status.
- Stories 3.1 and 3.3 depend on exception classes from Story 5.6. For Story 3.1, `CreaccValidationException` must be created as a minimal stub sufficient for the story's AC, even though the full hierarchy is Story 5.6's scope.

## Cross-Story Dependencies

- Story 3.1 depends on `CreaccValidationException` (Story 5.6 primary scope) — a working version must exist.
- Story 3.3 depends on `AbndinfoRec` (Story 2.5), `ControlDao` (Story 4.1), `AbendNotificationService` (Story 5.5), and `CreaccSystemException` (Story 5.6).
- Stories 3.1, 3.2, 3.3 all depend on constants from `DateTimeFormatConstants` / `CreaccLayoutConstants` (Story 2.8) where relevant.
