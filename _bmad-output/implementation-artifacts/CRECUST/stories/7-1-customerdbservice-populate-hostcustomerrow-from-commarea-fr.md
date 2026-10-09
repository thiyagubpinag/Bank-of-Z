---
title: 'Story 7.1: CustomerDbService — populate HostCustomerRow from commarea'
type: 'feature'
created: '2026-10-01'
status: 'review'
route: 'dispatch'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `CustomerDbService` does not yet exist; the DB2 INSERT CUSTOMER path (FR-8.1, FR-8.2) has no Java implementation to populate the 17 `HostCustomerRow` host-variable fields — including YYYYMMDD integer encoding for three date columns — from the commarea and `CustomerRecord`.

**Approach:** Create `CustomerDbService` with a `populateHostCustomerRow()` method that maps all 17 fields in PE-3 column order, encodes the three date integers via a shared private helper, and sets `hvCustomerCreditScore` as `short`. Accompany this with a unit test that verifies the DOB integer-encoding formula with a known input.

## Boundaries & Constraints

**Always:**
- All 17 `HostCustomerRow` fields populated in PE-3 order: eyecatcher, sortcode, number, title, first_name, last_name, dob_int, phone, addr_line1, addr_line2, city, postcode, country, status, created_date_int, credit_score, cs_review_date_int (AC-7.1).
- `hvCustomerEyecatcher` is set to `"CUST"` as the first field assignment (FR-8.4).
- Date integer formula: `(year * 10000) + (month * 100) + day` — identical for all three date fields (TRA-4, PE-3).
- `hvCustomerCreditScore` declared as `short` in `HostCustomerRow` — SMALLINT maps to S9(4) COMP, 2 bytes (AC-11.4).
- `CustomerDbService` in package `com.ibm.cics.botz.crecust.service`; no instance state (NFR-5).
- `HostCustomerRow` in package `com.ibm.cics.botz.crecust.db` with Lombok `@Data @NoArgsConstructor @AllArgsConstructor @Builder` (ADR-12).
- SLF4J `private static final Logger`; `DEBUG` logging for per-record detail; no PII logged (NFR-2.5, NFR-2.6).
- No magic literals — the eyecatcher constant `"CUST"` must be a named `private static final String` (NFR-2.4).

**Never:**
- Do not duplicate the date-integer formula inline three times — extract to `private static int toDateInt(int year, int month, int day)`.
- Do not implement the JDBC INSERT in this story — that is Story 7.2.
- Do not add any instance fields or Spring/CDI injection annotations to `CustomerDbService`.
- Do not add a `commSuccess` or `commFailCode` assignment in this method — population is a pure data-mapping step.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| DOB encoding — typical date | `commDobYear=1990`, `commDobMonth=6`, `commDobDay=15` | `hvCustomerDob = 19900615` | N/A |
| DOB encoding — January boundary | `commDobYear=2000`, `commDobMonth=1`, `commDobDay=1` | `hvCustomerDob = 20000101` | N/A |
| Create date encoding | `commCreatedYear=2026`, `commCreatedMonth=10`, `commCreatedDay=1` | `hvCustomerCreateDate = 20261001` | N/A |
| CS review date encoding | `customerRecord.customerCsReviewYear=2027`, `Month=3`, `Day=5` | `hvCustomerCsReviewDate = 20270305` | N/A |
| Credit score type | `customerRecord.customerCreditScore = 750` (int in CustomerRecord PIC 999) | `hostCustomerRow.hvCustomerCreditScore = (short) 750` | N/A |
| String field copy — sortcode | `commarea.commSortcode = "987654"` | `hostCustomerRow.hvCustomerSortcode = "987654"` | N/A |
| Eyecatcher set first | Any valid commarea | `hvCustomerEyecatcher = "CUST"` regardless of other field values | N/A |

</frozen-after-approval>

## Code Map

- `src/main/java/com/ibm/cics/botz/crecust/service/CustomerDbService.java` — **create**: new service class; `populateHostCustomerRow()` and private `toDateInt()` helper live here.
- `src/main/java/com/ibm/cics/botz/crecust/db/HostCustomerRow.java` — **create**: 17-field Lombok data class; `hvCustomerCreditScore` must be `short`; all three date fields must be `int`; all `X(n)` fields must be `String`.
- `src/main/java/com/ibm/cics/botz/crecust/model/CrecustCommarea.java` — **read**: source of commarea fields (`commDobYear`, `commDobMonth`, `commDobDay`, `commCreatedYear`, `commCreatedMonth`, `commCreatedDay`, `commSortcode`, `commNumber`, `commTitle`, `commFirstName`, `commLastName`, `commPhone`, `commAddrLine1`, `commAddrLine2`, `commCity`, `commPostcode`, `commCountry`, `commStatus`, `commCreditScore`) — Epic 2, Story 2.1.
- `src/main/java/com/ibm/cics/botz/crecust/model/CustomerRecord.java` — **read**: source of `customerSortcode`, `customerNumber`, `customerStatus`, `customerCsReviewYear`, `customerCsReviewMonth`, `customerCsReviewDay`, `customerCreditScore` — Epic 2, Story 2.1.
- `src/test/java/com/ibm/cics/botz/crecust/service/CustomerDbServiceTest.java` — **create**: JUnit 5 unit test for `populateHostCustomerRow()`; verifies `hvCustomerDob = 19900615` with known commarea input (AC acceptance criterion).

## Tasks & Acceptance

**Execution:**
- [ ] `src/main/java/com/ibm/cics/botz/crecust/db/HostCustomerRow.java` — **create** Lombok `@Data @NoArgsConstructor @AllArgsConstructor @Builder` class with 17 fields matching HOST-CUSTOMER-ROW (technical-research lines 111–127): `hvCustomerEyecatcher` (`String`), `hvCustomerSortcode` (`String`), `hvCustomerNumber` (`String`), `hvCustomerTitle` (`String`), `hvCustomerFirstName` (`String`), `hvCustomerLastName` (`String`), `hvCustomerDob` (`int`), `hvCustomerPhone` (`String`), `hvCustomerAddrLine1` (`String`), `hvCustomerAddrLine2` (`String`), `hvCustomerCity` (`String`), `hvCustomerCountry` (`String`), `hvCustomerPostcode` (`String`), `hvCustomerStatus` (`String`), `hvCustomerCreateDate` (`int`), `hvCustomerCreditScore` (`short`), `hvCustomerCsReviewDate` (`int`) — rationale: DB2 host variable container; SMALLINT must be `short`, INTEGER must be `int`.
- [ ] `src/main/java/com/ibm/cics/botz/crecust/service/CustomerDbService.java` — **create** class with `private static final Logger`, `private static final String EYECATCHER = "CUST"`, `private static int toDateInt(int year, int month, int day)` returning `(year * 10000) + (month * 100) + day`, and `public void populateHostCustomerRow(CrecustCommarea commarea, CustomerRecord customerRecord, HostCustomerRow row)` that sets all 17 fields in PE-3 order — rationale: implements FR-8.1, FR-8.2, AC-7.1 column-order alignment.
- [ ] `src/test/java/com/ibm/cics/botz/crecust/service/CustomerDbServiceTest.java` — **create** JUnit 5 test class; test method `populateHostCustomerRow_encodesDateAsYYYYMMDD` constructs `CrecustCommarea` with `commDobYear=1990`, `commDobMonth=6`, `commDobDay=15`, calls `populateHostCustomerRow`, asserts `hvCustomerDob == 19900615` — rationale: acceptance criterion from epics Story 7.1.

**Acceptance Criteria:**
- Given a `CrecustCommarea` with `commDobYear=1990`, `commDobMonth=6`, `commDobDay=15`, when `populateHostCustomerRow(commarea, customerRecord, row)` is called, then `row.hvCustomerDob == 19900615`.
- Given a `CrecustCommarea` with `commCreatedYear=2026`, `commCreatedMonth=10`, `commCreatedDay=1`, when `populateHostCustomerRow` is called, then `row.hvCustomerCreateDate == 20261001`.
- Given a `CustomerRecord` with `customerCsReviewYear=2027`, `customerCsReviewMonth=3`, `customerCsReviewDay=5`, when `populateHostCustomerRow` is called, then `row.hvCustomerCsReviewDate == 20270305`.
- Given any valid inputs, when `populateHostCustomerRow` is called, then `row.hvCustomerEyecatcher` equals `"CUST"` and is the first field set.
- Given a `CustomerRecord` with `customerCreditScore = 750`, when `populateHostCustomerRow` is called, then `row.hvCustomerCreditScore` equals `(short) 750` and the field is declared `short` in `HostCustomerRow`.
- Given any valid commarea and customer record, when `populateHostCustomerRow` is called, then all 17 fields of `HostCustomerRow` are non-null/non-zero (no field is left at its default unset value), in PE-3 column order.
- `CustomerDbService` has no instance fields and no Spring/CDI annotations.
- `HostCustomerRow` compiles cleanly with Lombok and all field types match the COBOL PIC clauses (three `int` dates, one `short` credit score, thirteen `String` fields).

## Implementation Notes

**Implemented: 2026-10-09**

`CustomerDbService.java` fully implemented with:

- `populateHostCustomerRow(CrecustCommarea, CustomerRecord, HostCustomerRow)` — sets all 17 fields
  in PE-3 column order, faithful to COBOL `WRITE-CUSTOMER-DB2 / WCD010` (lines 1161–1197).
- `private static int toDateInt(int year, int month, int day)` — encodes the three date fields
  (DOB, create-date, CS-review-date) as YYYYMMDD integers using formula `(year*10000)+(month*100)+day`.
- `private static int parseIntField(String s)` — converts COBOL display-numeric `String` fields
  (PIC 99 / 9999 / 999) to `int`; returns 0 for null/blank.
- `private static final String EYECATCHER = "CUST"` — named constant; no magic literals.
- `private static final Logger log` — SLF4J DEBUG logging with no PII.

**COBOL mapping verified against WCD010 (lines 1161–1197):**
All 17 host-variable assignments come from `commArea` (not `CustomerRecord`) per COBOL source.
The CS-review-date source is `COMM-CS-REVIEW-YEAR/MONTH/DAY` in the commarea.
`mvn compile` → BUILD SUCCESS.

## Spec Change Log

## Review Triage Log

## Design Notes

**Date integer encoding — three fields, one formula:**
The COBOL source uses three separate `COMPUTE` statements (PREMIERE_P010 lines ~1200–1250) all with the identical formula `(YYYY * 10000) + (MM * 100) + DD`. In Java, extract to a single private helper to avoid triple repetition:
```java
private static int toDateInt(int year, int month, int day) {
    return (year * 10000) + (month * 100) + day;
}
```
All three calls then read:
```java
row.setHvCustomerDob(toDateInt(commarea.getCommDobYear(), commarea.getCommDobMonth(), commarea.getCommDobDay()));
row.setHvCustomerCreateDate(toDateInt(commarea.getCommCreatedYear(), commarea.getCommCreatedMonth(), commarea.getCommCreatedDay()));
row.setHvCustomerCsReviewDate(toDateInt(customerRecord.getCustomerCsReviewYear(), customerRecord.getCustomerCsReviewMonth(), customerRecord.getCustomerCsReviewDay()));
```

**Credit score type — short, not int:**
`HV-CUSTOMER-CREDIT-SCORE` is `S9(4) COMP` (2 bytes) — this is SMALLINT in DB2. The Java field must be `short`, not `int`. If `CustomerRecord.customerCreditScore` is `int` (from PIC 999, 3 digits), the assignment requires an explicit cast: `row.setHvCustomerCreditScore((short) customerRecord.getCustomerCreditScore())`.

**Column order matters for Story 7.2:**
PE-3 order in population must match the INSERT column order in Story 7.2. The order is fixed by the technical research: eyecatcher → sortcode → number → title → first_name → last_name → dob_int → phone → addr_line1 → addr_line2 → city → postcode → country → status → created_date_int → credit_score → cs_review_date_int.

## Verification

**Commands:**
- `mvn test -pl crecust -Dtest=CustomerDbServiceTest` -- expected: BUILD SUCCESS, all assertions pass
- `mvn compile -pl crecust` -- expected: BUILD SUCCESS with no warnings on `HostCustomerRow` or `CustomerDbService`
