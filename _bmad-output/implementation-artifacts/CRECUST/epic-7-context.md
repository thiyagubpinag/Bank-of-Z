# Epic 7 Context: Insert Customer Record (DB2 CUSTOMER Write)

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Author `CustomerDbService` with two responsibilities: (1) populate all 17 fields of `HostCustomerRow` from the commarea and `CustomerRecord` — including YYYYMMDD integer-encoding of three date fields — and (2) execute the JDBC INSERT CUSTOMER statement and handle the silent-return SQL failure path (fail-code `'1'`).

## Stories

- Story 7.1: `CustomerDbService` — populate `HostCustomerRow` from commarea (FR-8.1, FR-8.2)
- Story 7.2: `CustomerDbService.insertCustomer()` — JDBC INSERT CUSTOMER 17 columns (FR-8.3)

## Requirements & Constraints

**`HostCustomerRow` population (FR-8.1, FR-8.2)**
- 17 fields must be populated in PE-3 column order: eyecatcher, sortcode, number, title, first_name, last_name, dob_int, phone, addr_line1, addr_line2, city, postcode, country, status, created_date_int, credit_score, cs_review_date_int.
- `hvCustomerEyecatcher` is set to `"CUST"` before all other fields (FR-8.4).
- Three integer date fields encoded as `(year * 10000) + (month * 100) + day` (TRA-4, PE-3):
  - `hvCustomerDob` ← commarea `commDobYear / commDobMonth / commDobDay`
  - `hvCustomerCreateDate` ← commarea `commCreatedYear / commCreatedMonth / commCreatedDay`
  - `hvCustomerCsReviewDate` ← `CustomerRecord.customerCsReviewYear / Month / Day`
- `hvCustomerCreditScore` is of Java type `short` — maps to SMALLINT (`S9(4) COMP`, 2 bytes).
- All other fields are direct String copies from commarea or `CustomerRecord`.

**INSERT CUSTOMER execution (FR-8.3)**
- 17-column JDBC `PreparedStatement` in PE-3 order — 17 `?` placeholders.
- On SQL `SQLException`: call `customerNumberService.dequeue(commarea)`, set `commSuccess = 'N'`, `commFailCode = CrecustException.FAIL_CODE_INSERT_CUSTOMER` (`'1'`), return — no ABEND (AC-1.2 — silent-return).
- On success: `commSuccess = 'Y'`, `commFailCode = ' '`, `commEyecatcher = "CUST"` (FR-8.6).
- `DataSource` obtained via JNDI `"jdbc/crecustDB2DS"` (ADR-4).
- All JDBC resources in `try-with-resources` (NFR-4).

**Error-path classification (Rule 1):** INSERT CUSTOMER failure is a **silent-return** path. No ABEND; no `Program.link()`.

## Technical Decisions

**Package and class shape:**
- `CustomerDbService` lives in `com.ibm.cics.botz.crecust.service`.
- No instance state (NFR-5); all logic on method parameters and locals.
- `HostCustomerRow` lives in `com.ibm.cics.botz.crecust.db` with Lombok `@Data @NoArgsConstructor @AllArgsConstructor @Builder`.

**Method signature (Story 7.1):**
```java
void populateHostCustomerRow(CrecustCommarea commarea, CustomerRecord customerRecord, HostCustomerRow row)
```
Side-effect only; returns void; the populated `row` is consumed by `insertCustomer()` in Story 7.2.

**Date integer encoding helper (Story 7.1):**
A `private static int toDateInt(int year, int month, int day)` method encodes the three dates identically — avoids duplicating the formula inline three times.

**`HostCustomerRow` field types:**
- All `X(n)` fields → `String`
- `HV-CUSTOMER-DOB`, `HV-CUSTOMER-CREATE-DATE`, `HV-CUSTOMER-CS-REVIEW-DATE` → `int` (S9(9) COMP = INTEGER)
- `HV-CUSTOMER-CREDIT-SCORE` → `short` (S9(4) COMP = SMALLINT)

## Cross-Story Dependencies

- **Depends on Epic 1** — Maven project must exist before code compiles.
- **Depends on Epic 2** — `CrecustCommarea` (Story 2.1), `CustomerRecord` (Story 2.1), and `HostCustomerRow` (Story 2.1) must exist with all 17/23/17 fields present.
- **Depends on Epic 6** — `CustomerNumberService.dequeue()` (Story 6.3) is called on INSERT failure; must exist and accept `CrecustCommarea`.
- **Story 7.1 → Story 7.2** — Story 7.2 consumes the populated `HostCustomerRow` produced by Story 7.1; Story 7.1 must be done first.
- **Downstream:** Epic 8 (`ProctranDbService`) proceeds only after the CUSTOMER INSERT succeeds; `CrecustService` (Epic 3) orchestrates the call sequence.
