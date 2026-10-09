# Story 3.2 — StatementDateCalculator.calculateMilestones() — Date Calculation with Explicit Leap-Year

## Story Information
- **Epic**: 3 (Business Logic)
- **Story Key**: `3-2-statementdatecalculator-calculatemilestones-date-calculation`
- **Program**: `CREACC`
- **Target Runtime**: `cics_batch` / Java 21 LTS
- **Status**: `review`

## Implementation Details
- **Class**: `com.ibm.bankofz.creacc.service.StatementDateCalculator`
- **Value Object**: `com.ibm.bankofz.creacc.model.StatementDates`
- **Test Class**: `com.ibm.bankofz.creacc.service.StatementDateCalculatorTest`

### Key Highlights
- Implemented `StatementDates` record representing `openedDate`, `lastStatementDate`, and `nextStatementDate` as `LocalDate`.
- Implemented `StatementDateCalculator.calculateMilestones(LocalDate openDate)` reproducing COBOL `CALCULATE-DATES_CD010` date calculations:
  - `openedDate` = `openDate`
  - `lastStatementDate` = `openDate`
  - `nextStatementDate`: for non-February months, adds 30 days; for February, adds 28 days plus 1 day if leap year (`year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)`), ensuring milestone target across month boundary per AC-F-5 through AC-F-8.
- Package-private helper `isLeapYear(int year)`.
- All JUnit 5 unit tests pass, covering leap year, century leap, non-leap, and non-February calendar dates.
