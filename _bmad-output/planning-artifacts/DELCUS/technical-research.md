# Technical Research Report: DELCUS.cbl


| Attribute | Value |
|---|---|
| **Program name** | `DELCUS` |
| **Source language** | Enterprise COBOL for z/OS |
| **Middleware** | CICS (transaction), DB2 (embedded SQL) |
| **Entry point** | `PROCEDURE DIVISION USING DFHCOMMAREA` — section `PREMIERE`, paragraph `A010` |
| **Total paragraphs** | 8 (across 8 sections) |
| **Total variables (top-level)** | 52 top-level groups / 405 total variables |
| **REDEFINES groups** | 13 direct-sibling groups |
| **Copybooks used** | SORTCODE, CUSTDB2, PROCDB2, ACCOUNT, CUSTOMER, PROCTRAN, INQACCCU, INQCUSTZ, ABNDINFO, DELCUS |
| **SQL tables accessed** | `CUSTOMER` (SELECT, DELETE), `PROCTRAN` (INSERT) |
| **External program calls (CICS LINK)** | `INQCUST`, `INQACCCU`, `DELACC`, `ABNDPROC` |
| **CICS ABEND codes** | `WPV6`, `WPV7`, `HWPT` |

### Purpose

DELCUS implements customer-deletion logic for the Bank-of-Z CICS application. Given a customer number and sort code in the COMMAREA, it:

1. Verifies the customer exists (via `EXEC CICS LINK` to `INQCUST`).
2. Retrieves all accounts for that customer (via `EXEC CICS LINK` to `INQACCCU`).
3. Deletes each account one by one (via `EXEC CICS LINK` to `DELACC` in a loop).
4. Selects the customer record from DB2 (to harvest details for the audit trail).
5. Deletes the customer record from DB2.
6. Writes an audit record to the `PROCTRAN` DB2 table (type `ODC` = Online Delete Customer).
7. Returns success/fail status in the COMMAREA.

### External Dependencies

| Dependency | Type | Direction | Purpose |
|---|---|---|---|
| `INQCUST` | CICS program | Called (LINK) | Verify customer exists; return customer details |
| `INQACCCU` | CICS program | Called (LINK, SYNCONRETURN) | Get list of all accounts for the customer |
| `DELACC` | CICS program | Called (LINK) | Delete each individual account |
| `ABNDPROC` | CICS program | Called (LINK) | Centralised abend-notification handler |
| `CUSTOMER` | DB2 table | READ + DELETE | Primary customer master table |
| `PROCTRAN` | DB2 table | INSERT | Audit/transaction-history table |


## Overview

## Data Structures

## Paragraph Inventory

## Business Rules / Complexity

## Precision Extractions

## Transformation Risk Areas
