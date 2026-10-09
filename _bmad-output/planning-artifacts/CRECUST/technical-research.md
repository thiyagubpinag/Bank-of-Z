# Technical Research Report: CRECUST


**Program:** `CRECUST.cbl`  
**Language:** Enterprise COBOL  
**Middleware:** CICS (EXEC CICS), DB2 (EXEC SQL)  
**Entry point:** `PROCEDURE DIVISION USING DFHCOMMAREA` — CICS-linked program receiving a commarea from a calling transaction.

### Summary

`CRECUST` creates a new bank customer record. It validates the incoming customer data (title, date-of-birth), performs an asynchronous multi-agency credit-score check via CICS child transactions, assigns a sequential customer number from a DB2 CONTROL table protected by a CICS Named Counter Service (NCS) enqueue/dequeue, inserts the customer row into the DB2 CUSTOMER table, writes a transaction audit record to the DB2 PROCTRAN table, and returns success or a fail-code to the caller via the commarea.

### Counts (from TS Metadata)

| Metric | Value |
|---|---|
| Total paragraphs (sections) | 12 (including GET-ME-OUT-OF-HERE) |
| Total variables | 419 |
| Top-level 01/77 items | 89 |
| DB2 tables accessed | 3 (CUSTOMER, PROCTRAN, CONTROL) |
| Copybooks | 9 (SORTCODE, CUSTDB2, PROCDB2, CONTDB2, PROCTRAN, ABNDINFO, CUSTOMER, CUSTCTRL, CRECUST) |

### External Dependencies

| Dependency | Type | Purpose |
|---|---|---|
| `ABNDPROC` | CICS LINK target | Abend-information handler; called before CICS ABEND |
| `OCR1`–`OCR5` | CICS RUN TRANSID async child | Credit-check worker transactions (up to 5 launched) |
| DB2 `CUSTOMER` table | SQL INSERT | Persist new customer row |
| DB2 `PROCTRAN` table | SQL INSERT | Audit trail of customer creation |
| DB2 `CONTROL` table (`STTESTER.CONTROL`) | SQL SELECT + UPDATE | Retrieve and increment sequential customer number |
| CICS Named Counter `BANKZCUST<sortcode>` | ENQ/DEQ | Serialise customer-number allocation |
| `CEEDAYS` | LE language API CALL | Convert DOB to Lilian day number for validation |
| `CEELOCT` | LE language API CALL | Get today's Lilian day + Gregorian components |
| CICS `ASKTIME` / `FORMATTIME` | EXEC CICS | Obtain current absolute time and format date/time strings |
| CICS `DELAY` | EXEC CICS | 3-second pause after launching async child transactions |
| CICS Containers/Channels (`CIPCREDCHANN`) | PUT CONTAINER / GET CONTAINER | Pass commarea to child transactions and retrieve credit scores |


## Overview

## Data Structures


### WORKING-STORAGE SECTION — DB2 Host Variable Rows

#### `HOST-CUSTOMER-ROW` (01-level, line 111) — CUSTOMER DB2 Host Variables

| Field | PIC | Bytes | Notes |
|---|---|---|---|
| `HV-CUSTOMER-EYECATCHER` | `X(4)` | 4 | Lit `'CUST'` |
| `HV-CUSTOMER-SORTCODE` | `X(6)` | 6 | |
| `HV-CUSTOMER-NUMBER` | `X(10)` | 10 | |
| `HV-CUSTOMER-TITLE` | `X(10)` | 10 | |
| `HV-CUSTOMER-FIRST-NAME` | `X(50)` | 50 | |
| `HV-CUSTOMER-LAST-NAME` | `X(50)` | 50 | |
| `HV-CUSTOMER-DOB` | `S9(9) COMP` | 4 | INTEGER; encoded as YYYYMMDD via COMPUTE |
| `HV-CUSTOMER-PHONE` | `X(20)` | 20 | |
| `HV-CUSTOMER-ADDR-LINE1` | `X(50)` | 50 | |
| `HV-CUSTOMER-ADDR-LINE2` | `X(50)` | 50 | |
| `HV-CUSTOMER-CITY` | `X(50)` | 50 | |
| `HV-CUSTOMER-POSTCODE` | `X(10)` | 10 | |
| `HV-CUSTOMER-COUNTRY` | `X(50)` | 50 | |
| `HV-CUSTOMER-STATUS` | `X(10)` | 10 | |
| `HV-CUSTOMER-CREATE-DATE` | `S9(9) COMP` | 4 | INTEGER; YYYYMMDD encoding |
| `HV-CUSTOMER-CREDIT-SCORE` | `S9(4) COMP` | 2 | SMALLINT |
| `HV-CUSTOMER-CS-REVIEW-DATE` | `S9(9) COMP` | 4 | INTEGER; YYYYMMDD encoding |

#### `HOST-PROCTRAN-ROW` (01-level, line 160) — PROCTRAN DB2 Host Variables

| Field | PIC | Bytes | Notes |
|---|---|---|---|
| `HV-PROCTRAN-EYECATCHER` | `X(4)` | 4 | Lit `'PRTR'` |
| `HV-PROCTRAN-SORT-CODE` | `X(6)` | 6 | |
| `HV-PROCTRAN-ACC-NUMBER` | `X(8)` | 8 | Set to ZEROS for customer-create |
| `HV-PROCTRAN-DATE` | `X(10)` | 10 | DD.MM.YYYY format (DATESEP '.') |
| `HV-PROCTRAN-TIME` | `X(6)` | 6 | HHMMSS |
| `HV-PROCTRAN-REF` | `X(12)` | 12 | EIBTASKN (task number) |
| `HV-PROCTRAN-TYPE` | `X(3)` | 3 | `'OCC'` (Branch Create Customer) |
| `HV-PROCTRAN-DESC` | `X(40)` | 40 | Packed: sortcode(6)+custno(10)+name(14)+dob(10) |
| `HV-PROCTRAN-AMOUNT` | `S9(10)V99 COMP-3` | 7 | ZEROS for customer-create |

#### `HOST-CONTROL-ROW` (01-level, line 193) — CONTROL DB2 Host Variables

| Field | PIC | Bytes | Notes |
|---|---|---|---|
| `HV-CONTROL-NAME` | `X(32)` | 32 | Key: `'BANKZCUST' + sortcode + '  '` |
| `HV-CONTROL-VALUE-NUM` | `S9(9) COMP` | 4 | Current max customer number |
| `HV-CONTROL-VALUE-STR` | `X(32)` | 32 | Not used in this program |

---

### DB2 Table Declarations

#### CUSTOMER table (`CUSTDB2.cpy`)

| Column | SQL Type | Notes |
|---|---|---|
| `CUSTOMER_EYECATCHER` | `CHAR(4)` | |
| `CUSTOMER_SORTCODE` | `CHAR(6) NOT NULL` | |
| `CUSTOMER_NUMBER` | `CHAR(10) NOT NULL` | |
| `CUSTOMER_TITLE` | `CHAR(10)` | |
| `CUSTOMER_FIRST_NAME` | `CHAR(50)` | |
| `CUSTOMER_LAST_NAME` | `CHAR(50)` | |
| `CUSTOMER_DATE_OF_BIRTH` | `INTEGER` | YYYYMMDD-encoded integer |
| `CUSTOMER_PHONE` | `CHAR(20)` | |
| `CUSTOMER_ADDR_LINE1` | `CHAR(50)` | |
| `CUSTOMER_ADDR_LINE2` | `CHAR(50)` | |
| `CUSTOMER_CITY` | `CHAR(50)` | |
| `CUSTOMER_POSTCODE` | `CHAR(10)` | |
| `CUSTOMER_COUNTRY` | `CHAR(50)` | |
| `CUSTOMER_STATUS` | `CHAR(10)` | |
| `CUSTOMER_CREATED_DATE` | `INTEGER` | YYYYMMDD-encoded integer |
| `CUSTOMER_CREDIT_SCORE` | `SMALLINT` | |
| `CUSTOMER_CS_REVIEW_DATE` | `INTEGER` | YYYYMMDD-encoded integer |

#### PROCTRAN table (`PROCDB2.cpy`)

| Column | SQL Type |
|---|---|
| `PROCTRAN_EYECATCHER` | `CHAR(4)` |
| `PROCTRAN_SORTCODE` | `CHAR(6) NOT NULL` |
| `PROCTRAN_NUMBER` | `CHAR(8) NOT NULL` |
| `PROCTRAN_DATE` | `DATE` |
| `PROCTRAN_TIME` | `CHAR(6)` |
| `PROCTRAN_REF` | `CHAR(12)` |
| `PROCTRAN_TYPE` | `CHAR(3)` |
| `PROCTRAN_DESC` | `CHAR(40)` |
| `PROCTRAN_AMOUNT` | `DECIMAL(12,2)` |

#### CONTROL table (`CONTDB2.cpy`) — schema `STTESTER`

| Column | SQL Type |
|---|---|
| `CONTROL_NAME` | `CHAR(32) NOT NULL` |
| `CONTROL_VALUE_NUM` | `INTEGER` |
| `CONTROL_VALUE_STR` | `CHAR(40)` |

---

### WORKING-STORAGE SECTION — Key Working Storage Groups

#### `PROCTRAN-AREA` / `PROC-TRAN-DATA` (line 203) — from PROCTRAN.cpy

Full working-storage mirror of the PROCTRAN record. Key sub-fields:

| Field | PIC | Bytes | Notes |
|---|---|---|---|
| `PROC-TRAN-EYE-CATCHER` | `X(4)` | 4 | 88: `PROC-TRAN-VALID VALUE 'PRTR'` |
| `PROC-TRAN-LOGICAL-DELETE-AREA` (REDEFINES) | — | 4 | Cat 3a REDEFINES; flag `X'FF'` = deleted |
| `PROC-TRAN-SORT-CODE` | `9(6)` | 6 | |
| `PROC-TRAN-NUMBER` | `9(8)` | 8 | |
| `PROC-TRAN-DATE` | `9(8)` | 4 | YYYYMMDD numeric |
| `PROC-TRAN-DATE-GRP` (REDEFINES) | — | 4 | Cat 3a: split YYYY/MM/DD |
| `PROC-TRAN-TIME` | `9(6)` | 3 | HHMMSS numeric |
| `PROC-TRAN-TIME-GRP` (REDEFINES) | — | 3 | Cat 3a: split HH/MM/SS |
| `PROC-TRAN-REF` | `9(12)` | 6 | |
| `PROC-TRAN-TYPE` | `X(3)` | 3 | Many 88-level type codes |
| `PROC-TRAN-DESC` | `X(40)` | 40 | |
| `PROC-TRAN-DESC-XFR` (REDEFINES) | — | 40 | Cat 4a: transfer layout |
| `PROC-TRAN-DESC-DELACC` (REDEFINES) | — | 40 | Cat 4a |
| `PROC-TRAN-DESC-CREACC` (REDEFINES) | — | 40 | Cat 4a |
| `PROC-TRAN-DESC-DELCUS` (REDEFINES) | — | 40 | Cat 4a |
| `PROC-TRAN-DESC-CRECUS` (REDEFINES) | — | 40 | Cat 4a; **used by this program** |
| `PROC-TRAN-AMOUNT` | `S9(10)V99` | 6 | |

#### `WS-CICS-WORK-AREA` (line 316)

| Field | PIC | Bytes |
|---|---|---|
| `WS-CICS-RESP` | `S9(8) COMP` | 4 |
| `WS-CICS-RESP2` | `S9(8) COMP` | 4 |

#### `WS-TIME-DATA` (line 322)

| Field | PIC | Notes |
|---|---|---|
| `WS-TIME-NOW` | `9(6)` | HHMMSS numeric |
| `WS-TIME-NOW-GRP` (REDEFINES) | — | Cat 3a: split HH/MM/SS |

#### `ABNDINFO-REC` (line 330) — from ABNDINFO.cpy

| Field | PIC | Bytes |
|---|---|---|
| `ABND-UTIME-KEY` | `S9(15) COMP-3` | 8 |
| `ABND-TASKNO-KEY` | `9(4)` | 4 |
| `ABND-APPLID` | `X(8)` | 8 |
| `ABND-TRANID` | `X(4)` | 4 |
| `ABND-DATE` | `X(10)` | 10 |
| `ABND-TIME` | `X(8)` | 8 |
| `ABND-CODE` | `X(4)` | 4 |
| `ABND-PROGRAM` | `X(8)` | 8 |
| `ABND-RESPCODE` | `S9(8) DISPLAY SIGN LEADING SEPARATE` | 9 |
| `ABND-RESP2CODE` | `S9(8) DISPLAY SIGN LEADING SEPARATE` | 9 |
| `ABND-SQLCODE` | `S9(8) DISPLAY SIGN LEADING SEPARATE` | 9 |
| `ABND-FREEFORM` | `X(600)` | 600 |

#### LOCAL-STORAGE — `OUTPUT-DATA` / `CUSTOMER-RECORD` (line 364) — from CUSTOMER.cpy

| Field | PIC | Bytes | Notes |
|---|---|---|---|
| `CUSTOMER-EYECATCHER` | `X(4)` | 4 | 88: `'CUST'` |
| `CUSTOMER-SORTCODE` | `9(6) DISPLAY` | 6 | |
| `CUSTOMER-NUMBER` | `9(10) DISPLAY` | 10 | |
| `CUSTOMER-TITLE` | `X(10)` | 10 | |
| `CUSTOMER-FIRST-NAME` | `X(50)` | 50 | |
| `CUSTOMER-LAST-NAME` | `X(50)` | 50 | |
| `CUSTOMER-DOB-DAY` | `99 DISPLAY` | 2 | |
| `CUSTOMER-DOB-MONTH` | `99 DISPLAY` | 2 | |
| `CUSTOMER-DOB-YEAR` | `9999 DISPLAY` | 4 | |
| `CUSTOMER-PHONE` | `X(20)` | 20 | |
| `CUSTOMER-ADDR-LINE1` | `X(50)` | 50 | |
| `CUSTOMER-ADDR-LINE2` | `X(50)` | 50 | |
| `CUSTOMER-CITY` | `X(50)` | 50 | |
| `CUSTOMER-POSTCODE` | `X(10)` | 10 | |
| `CUSTOMER-COUNTRY` | `X(50)` | 50 | |
| `CUSTOMER-STATUS` | `X(10)` | 10 | 88s: ACTIVE, INACTIVE, SUSPENDED |
| `CUSTOMER-CREATED-DAY` | `99 DISPLAY` | 2 | |
| `CUSTOMER-CREATED-MONTH` | `99 DISPLAY` | 2 | |
| `CUSTOMER-CREATED-YEAR` | `9999 DISPLAY` | 4 | |
| `CUSTOMER-CREDIT-SCORE` | `999` | 3 | |
| `CUSTOMER-CS-REVIEW-DAY` | `99 DISPLAY` | 2 | |
| `CUSTOMER-CS-REVIEW-MONTH` | `99 DISPLAY` | 2 | |
| `CUSTOMER-CS-REVIEW-YEAR` | `9999 DISPLAY` | 4 | |

**Total CUSTOMER-RECORD byte width:** 4+6+10+10+50+50+2+2+4+20+50+50+50+10+50+10+2+2+4+3+2+2+4 = **397 bytes**

#### `NCS-CUST-NO-STUFF` (line 440) — Named Counter fields

| Field | PIC | Bytes | Notes |
|---|---|---|---|
| `NCS-CUST-NO-ACT-NAME` | `X(9)` | 9 | VALUE `'BANKZCUST'` |
| `NCS-CUST-NO-TEST-SORT` | `X(6)` | 6 | SORTCODE moved here |
| `NCS-CUST-NO-FILL` | `XX` | 2 | VALUE `'  '` |
| `NCS-CUST-NO-INC` | `9(16) COMP` | 8 | VALUE 0; set to 1 for increment |
| `NCS-CUST-NO-VALUE` | `9(16) COMP` | 8 | Holds returned next value |
| `NCS-CUST-NO-RESP` | `XX` | 2 | VALUE `'00'` |

#### `WS-ORIG-DATE` / `WS-ORIG-DATE-GRP` REDEFINES (line 464)

| Field | PIC | Bytes | Notes |
|---|---|---|---|
| `WS-ORIG-DATE` | `X(10)` | 10 | Receives FORMATTIME DDMMYYYY with DATESEP |
| `WS-ORIG-DATE-GRP` (REDEFINES Cat 3a) | — | 10 | Splits as DD / MM / YYYY |
| `WS-ORIG-DATE-DD` | `99` | 2 | |
| FILLER | `X` | 1 | Separator |
| `WS-ORIG-DATE-MM` | `99` | 2 | |
| FILLER | `X` | 1 | Separator |
| `WS-ORIG-DATE-YYYY` | `9999` | 4 | |

#### `WS-ORIG-DATE-GRP-X` (line 472) — formatted display version (DD.MM.YYYY)

| Field | PIC | Bytes |
|---|---|---|
| `WS-ORIG-DATE-DD-X` | `XX` | 2 |
| FILLER `'.'` | `X` | 1 |
| `WS-ORIG-DATE-MM-X` | `XX` | 2 |
| FILLER `'.'` | `X` | 1 |
| `WS-ORIG-DATE-YYYY-X` | `X(4)` | 4 |

#### `CUSTOMER-KY2` / `CUSTOMER-KY2-BYTES` REDEFINES (line 487)

| Field | PIC | Bytes | Notes |
|---|---|---|---|
| `REQUIRED-SORT-CODE2` | `9(6)` | 6 | |
| `REQUIRED-CUST-NUMBER2` | `9(10)` | 10 | |
| `CUSTOMER-KY2-BYTES` (REDEFINES Cat 3a) | `X(16)` | 16 | Byte-array view |

#### Credit-Check Working Storage (lines 495–560)

| Field | PIC | Notes |
|---|---|---|
| `WS-CC-CNT` | `9` | Loop counter 1–5 (credit agencies) |
| `WS-FINISHED-FETCHING` | `X` | `'Y'` = done fetching |
| `WS-RETRIEVED-CNT` | `9` | Count of successful child responses |
| `WS-CHANNEL-NAME` | `X(16)` | `'CIPCREDCHANN    '` |
| `WS-CREDIT-CHECK-ERROR` | `X` | `'Y'` = error |
| `WS-ACTUAL-CS-SCR` | `9(6)` | Average credit score |
| `WS-TOTAL-CS-SCR` | `9(6)` | Running total |
| `WS-ANY-CHILD-TKN` | `X(16)` | Token from RUN TRANSID |
| `WS-ANY-CHILD-FETCH-TKN` | `X(16)` | Token from FETCH ANY |
| `WS-ANY-CHILD-FETCH-CHAN` | `X(16)` | Channel from FETCH ANY |
| `WS-ANY-CHILD-FETCH-ABCODE` | `X(4)` | ABCODE from FETCH ANY |
| `WS-CHILD-ISSUED-CNT` | `9` | Number of child transactions issued |
| `WS-CHILD-ARRAY` | OCCURS 9 | `WS-CHILD-CHAN X(16)` + `WS-CHILD-TKN X(16)` |
| `WS-CHILD-RECEIVED-CNT` | `9` | |
| `WS-RECEIVE-CHILD-ARRAY` | OCCURS 9 | `WS-RECEIVE-CHILD-CHAN X(16)` |
| `WS-CHILD-FETCH-COMPST` | `S9(8) COMP` | Completion status from FETCH ANY |
| `WS-RUN-TRANSID` | `X(4)` | Built as `'OCR' + WS-CC-CNT` |
| `WS-PUT-CONT-NAME` | `X(16)` | Container names CIPA–CIPI |
| `WS-PUT-CONT-LEN` | `S9(8) COMP` | LENGTH OF DFHCOMMAREA |

#### `WS-CHILD-DATA` / `WS-CHILD-CUSTOMER-RECORD` (line 523)

Mirrors the `CUSTOMER-RECORD` (397 bytes) plus two extra fields:

| Extra Field | PIC | Bytes |
|---|---|---|
| `WS-CHILD-SUCCESS` | `X` | 1 |
| `WS-CHILD-FAIL-CODE` | `X` | 1 |

**Total WS-CHILD-DATA byte width:** 399 bytes

#### CICS Level Check (`WS-CICSTS-LEVEL-DATA`, line 564)

| Field | PIC | Notes |
|---|---|---|
| `WS-CICSTSLEVEL` | `X(6)` | |
| `WS-CICSTS-LEVEL-NUM-GRP` (REDEFINES Cat 3a) | — | VV/RR/MM split |

#### Date / Review Date Fields (lines 571–590)

| Field | PIC | Notes |
|---|---|---|
| `WS-CURRENT-DATE-DATA` | group | 16-byte FUNCTION CURRENT-DATE result |
| `WS-CURRENT-DATE-9` | `9(8)` | YYYYMMDD integer for INTEGER-OF-DATE |
| `WS-TODAY-INT` | `9(8)` | Lilian-equivalent integer |
| `WS-REVIEW-DATE-ADD` | `99` | Random offset 1–20 days (RANDOM * 20) |
| `WS-NEW-REVIEW-DATE-INT` | `9(8)` | WS-TODAY-INT + WS-REVIEW-DATE-ADD |
| `WS-NEW-REVIEW-YYYYMMDD` | `9(8)` | DATE-OF-INTEGER result |

#### DOB Validation Structures (lines 597–650)

| Field | PIC | Notes |
|---|---|---|
| `WS-DATE-OF-BIRTH-LILLIAN` | `S9(9) BINARY` | Output from CEEDAYS call |
| `DATE-OF-BIRTH-FORMAT-LENGTH` | `S9(4) BINARY` VALUE 10 | |
| `DATE-OF-BIRTH-FORMAT-TEXT` | `X(8)` VALUE `'YYYYMMDD'` | Format string |
| `CEEDAYS-YEAR` | `9999` | |
| `CEEDAYS-MONTH` | `99` | |
| `CEEDAYS-DAY` | `99` | |
| `WS-TODAY-LILLIAN` | `S9(9) BINARY` | Output from CEELOCT |
| `WS-TODAY-GREGORIAN` | group | YYYY+MM+DD+HH+MM+SS+ms |
| `WS-CUSTOMER-AGE` | `S9999` | Approximate age (year subtraction) |
| `FC` | group | CEEDAYS/CEELOCT feedback code (CEE condition token) |

#### `CUSTOMER-CONTROL` / `CUSTOMER-CONTROL-RECORD` (line 656) — from CUSTCTRL.cpy

| Field | PIC | Bytes |
|---|---|---|
| `CUSTOMER-CONTROL-EYECATCHER` | `X(4)` | 4 — 88: `'CTRL'` |
| `CUSTOMER-CONTROL-SORTCODE` | `9(6) DISPLAY` | 6 |
| `CUSTOMER-CONTROL-NUMBER` | `9(10) DISPLAY` | 10 |
| `NUMBER-OF-CUSTOMERS` | `9(10) DISPLAY` | 10 |
| `LAST-CUSTOMER-NUMBER` | `9(10) DISPLAY` | 10 |
| `CUSTOMER-CONTROL-SUCCESS-FLAG` | `X` | 1 |
| `CUSTOMER-CONTROL-FAIL-CODE` | `X` | 1 |
| FILLERs | `X(38)`, `X(160)`, `9(8)`, `999`, `9(8)` | 209 |

---

### LINKAGE SECTION — `DFHCOMMAREA` (from CRECUST.cpy)

| Field | PIC | Bytes | Direction | Notes |
|---|---|---|---|---|
| `COMM-EYECATCHER` | `X(4)` | 4 | OUT | Set to `'CUST'` on success |
| `COMM-SORTCODE` | `9(6) DISPLAY` | 6 | IN | |
| `COMM-NUMBER` | `9(10) DISPLAY` | 10 | OUT | Assigned customer number |
| `COMM-TITLE` | `X(10)` | 10 | IN | Validated against list |
| `COMM-FIRST-NAME` | `X(50)` | 50 | IN | |
| `COMM-LAST-NAME` | `X(50)` | 50 | IN | |
| `COMM-DOB-DAY` | `99 DISPLAY` | 2 | IN | |
| `COMM-DOB-MONTH` | `99 DISPLAY` | 2 | IN | |
| `COMM-DOB-YEAR` | `9999 DISPLAY` | 4 | IN | |
| `COMM-PHONE` | `X(20)` | 20 | IN | |
| `COMM-ADDR-LINE1` | `X(50)` | 50 | IN | |
| `COMM-ADDR-LINE2` | `X(50)` | 50 | IN | |
| `COMM-CITY` | `X(50)` | 50 | IN | |
| `COMM-POSTCODE` | `X(10)` | 10 | IN | |
| `COMM-COUNTRY` | `X(50)` | 50 | IN | |
| `COMM-STATUS` | `X(10)` | 10 | IN | |
| `COMM-CREATED-DAY` | `99 DISPLAY` | 2 | IN | |
| `COMM-CREATED-MONTH` | `99 DISPLAY` | 2 | IN | |
| `COMM-CREATED-YEAR` | `9999 DISPLAY` | 4 | IN | |
| `COMM-CREDIT-SCORE` | `999` | 3 | OUT | Set by credit check |
| `COMM-CS-REVIEW-DAY` | `99 DISPLAY` | 2 | OUT | |
| `COMM-CS-REVIEW-MONTH` | `99 DISPLAY` | 2 | OUT | |
| `COMM-CS-REVIEW-YEAR` | `9999 DISPLAY` | 4 | OUT | |
| `COMM-SUCCESS` | `X` | 1 | OUT | `'Y'`=success, `'N'`=fail |
| `COMM-FAIL-CODE` | `X` | 1 | OUT | See Precision Extractions |

**Total DFHCOMMAREA byte width:** 4+6+10+10+50+50+2+2+4+20+50+50+50+10+50+10+2+2+4+3+2+2+4+1+1 = **399 bytes**


## Paragraph Inventory

All 12 paragraphs are declared as named SECTIONS; each section ends with an EXIT paragraph.

| # | Section Name | Lines | Purpose |
|---|---|---|---|
| 1 | `PREMIERE_P010` | 407–520 | Main control: validates title, performs credit-check, DOB check, NCS enqueue, customer-number allocation, DB2 write; then exits |
| 2 | `POPULATE-TIME-DATE_PTD010` | 523–534 | Issues `EXEC CICS ASKTIME` + `FORMATTIME DDMMYYYY DATESEP` to populate `WS-U-TIME`, `WS-ORIG-DATE`, and `PROC-TRAN-TIME` |
| 4 | `ENQ-NAMED-COUNTER_ENC010` | 541–556 | ENQ on the CICS Named Counter resource `NCS-CUST-NO-NAME` (16 bytes) |
| 6 | `DEQ-NAMED-COUNTER_DNC010` | 563–581 | DEQ on same Named Counter resource; called from multiple error and success paths |
| 8 | `UPD-NCS_UN010` | 588–596 | Sets `NCS-CUST-NO-INC=1`, calls `GET-LAST-CUSTOMER-DB2`, marks `NCS-UPDATED='Y'` |
| 10 | `CREDIT-CHECK_CC010` | 605–1131 | Full asynchronous credit check: PUT CONTAINER × 5, RUN TRANSID × 5 (OCR1–OCR5), DELAY 3 s, FETCH ANY loop, compute average score, set random review date 1–20 days ahead |
| 12 | `WRITE-CUSTOMER-DB2_WCD010` | 1139–1306 | Populates OUTPUT-DATA + HOST-CUSTOMER-ROW, executes `INSERT INTO CUSTOMER`, stores sortcode/custno/name/dob, calls WRITE-PROCTRAN, DEQ-NAMED-COUNTER, sets COMM-SUCCESS='Y' |
| 14 | `WRITE-PROCTRAN_WP010` | 1313–1314 | Thin wrapper: calls WRITE-PROCTRAN-DB2 |
| 16 | `WRITE-PROCTRAN-DB2_WPD010` | 1321–1458 | Populates HOST-PROCTRAN-ROW, ASKTIME/FORMATTIME, executes `INSERT INTO PROCTRAN`; on SQL failure: fills ABNDINFO-REC, LINKs ABNDPROC, then `EXEC CICS ABEND ABCODE('HWPT')` |
| 20 | `GET-LAST-CUSTOMER-DB2_GLCD010` | 1477–1549 | SELECT CONTROL_VALUE_NUM from CONTROL, increments by 1, UPDATE CONTROL, moves new number to multiple targets |
| 22 | `DATE-OF-BIRTH-CHECK_DOBC010` | 1556–1609 | Validates DOB: year ≥ 1601, calls CEEDAYS to get Lilian, CEELOCT for today, checks age ≤ 150, checks DOB not in future |
| 24 | `POPULATE-TIME-DATE2_PTD2010` | 1616–1628 | Same as PTD010 but populates `WS-TIME-NOW` instead of `PROC-TRAN-TIME`; called from WRITE-PROCTRAN-DB2 error path |
| — | `GET-ME-OUT-OF-HERE` | ~1797–1806 | Issues `EXEC CICS RETURN`; called from every error path and from the happy path at end of PREMIERE |

### Call Graph

```
PREMIERE_P010
  └─ POPULATE-TIME-DATE_PTD010
  └─ CREDIT-CHECK_CC010
  └─ DATE-OF-BIRTH-CHECK_DOBC010
  └─ ENQ-NAMED-COUNTER_ENC010
  └─ UPD-NCS_UN010
       └─ GET-LAST-CUSTOMER-DB2_GLCD010
            └─ DEQ-NAMED-COUNTER_DNC010   (on error)
  └─ WRITE-CUSTOMER-DB2_WCD010
       └─ DEQ-NAMED-COUNTER_DNC010        (on SQL error)
       └─ WRITE-PROCTRAN_WP010
            └─ WRITE-PROCTRAN-DB2_WPD010
                 └─ POPULATE-TIME-DATE2_PTD2010
                 └─ DEQ-NAMED-COUNTER_DNC010   (on SQL error, before ABEND)
       └─ DEQ-NAMED-COUNTER_DNC010        (success path)
  └─ GET-ME-OUT-OF-HERE
```

`DEQ-NAMED-COUNTER` is called from 4 distinct call sites (high reuse, Fan-In = 4).  
`GET-ME-OUT-OF-HERE` is called from every error exit and from the normal end of PREMIERE.



## Business Rules / Complexity

### Business Rules

#### BR-1: Title Validation
`COMM-TITLE` must be exactly one of the following 10-byte values (right-padded with spaces):
`'Professor'`, `'Mr       '`, `'Mrs      '`, `'Miss     '`, `'Ms       '`, `'Dr       '`, `'Drs      '`, `'Lord     '`, `'Sir      '`, `'Lady     '`, `'         '` (all spaces is permitted).  
On failure: `COMM-SUCCESS='N'`, `COMM-FAIL-CODE='T'`, GOBACK (no DEQ — enqueue not yet taken).

#### BR-2: Credit-Score Computation
- Up to 5 credit-check child transactions (OCR1–OCR5) are launched asynchronously.
- The program waits 3 seconds (`EXEC CICS DELAY FOR SECONDS(3)`), then fetches replies without suspending (`NOSUSPEND`).
- Only transactions that complete with `DFHVALUE(NORMAL)` and a successful `GET CONTAINER` contribute to the average.
- The average credit score = `WS-TOTAL-CS-SCR / WS-RETRIEVED-CNT`.
- If zero agencies reply in time, credit-check fails.

#### BR-3: Credit Score Review Date
- Computed only when at least one credit-check reply is received.
- Formula: `WS-NEW-REVIEW-DATE-INT = INTEGER-OF-DATE(today) + ((21 - 1) * RANDOM(EIBTASKN)) + 1`
- Results in a review date between today+1 and today+20 (inclusive).
- Stored in `COMM-CS-REVIEW-DATE` as DDMMYYYY (7-byte packed string from separate day/month/year moves).
- On any credit-check failure the review date is set to today's date (WS-ORIG-DATE-DD/MM/YYYY → COMM-CS-REVIEW-DATE).

#### BR-4: Date-of-Birth Validation
1. `COMM-DOB-YEAR < 1601` → fail code `'O'`
2. CEEDAYS called with format `'YYYYMMDD'`; if CEE000 not returned → fail code `'Z'`
3. CEELOCT called to get today; if CEE000 not returned → error set
4. Approximate age = `WS-TODAY-G-YEAR - COMM-DOB-YEAR`; if > 150 → fail code `'O'`
5. If today's Lilian < DOB Lilian (future date) → fail code `'Y'`

#### BR-5: Customer Number Allocation
- The customer number is retrieved from and incremented in the `STTESTER.CONTROL` table, keyed by `'BANKZCUST' + SORTCODE + '  '` (17-byte key, padded to 32 bytes in HV-CONTROL-NAME).
- SELECT current value → increment by 1 → UPDATE back.
- The new number is pushed to: `WS-CUSTOMER-NO-NUM`, `COMM-NUMBER`, `CUSTOMER-NUMBER`, `REQUIRED-CUST-NUMBER2`, `NCS-CUST-NO-VALUE`.
- Wrapped by CICS ENQ/DEQ on `NCS-CUST-NO-NAME` (16-byte resource name) for serialisation.

#### BR-6: PROCTRAN Audit Record
- Transaction type code `'OCC'` (Branch Create Customer).
- Amount = ZEROS.
- Reference = `EIBTASKN` converted to 12-byte field.
- Description = `STORED-SORTCODE(6) + STORED-CUSTNO(10) + STORED-NAME(14) + STORED-DOB(10)` = 40 bytes.
- Date format for PROCTRAN: `DD.MM.YYYY` (DATESEP('.'), `WS-ORIG-DATE-GRP-X`).
- Failure to INSERT PROCTRAN triggers CICS ABEND `'HWPT'` (notifying-abort path: links ABNDPROC first).

#### BR-7: NCS Enqueue / Dequeue Protocol
- ENQ called before customer-number retrieval; fail code `'3'` if ENQ fails.
- DEQ called: (a) after successful PROCTRAN write, (b) after CUSTOMER INSERT failure, (c) after PROCTRAN INSERT failure (before ABEND), (d) from GET-LAST-CUSTOMER-DB2 on SELECT/UPDATE failure.
- DEQ failure returns fail code `'5'`.

### Complexity Notes

| Area | Detail |
|---|---|
| `CREDIT-CHECK_CC010` (526 lines) | Largest paragraph; contains nested PERFORM loop + UNTIL loop + EVALUATE on COMPSTATUS; high cyclomatic complexity |
| `WRITE-PROCTRAN-DB2_WPD010` (137 lines) | Two paths: success (silent) and failure (notifying-abort: LINK + ABEND) |
| `GET-LAST-CUSTOMER-DB2_GLCD010` (72 lines) | Two SQL statements; two independent failure paths both calling DEQ + GET-ME-OUT-OF-HERE |
| `DATE-OF-BIRTH-CHECK_DOBC010` (53 lines) | 4 separate failure conditions, uses CEEDAYS + CEELOCT LE runtime calls |
| `DEQ-NAMED-COUNTER_DNC010` | Fan-In = 4; must be modelled as a shared utility method |



## Precision Extractions

### PE-1: Commarea Layout

**`DFHCOMMAREA`** — defined in `CRECUST.cpy`, 399 bytes total.

| Offset | Field | PIC | Bytes | Direction |
|---|---|---|---|---|
| 0 | `COMM-EYECATCHER` | `X(4)` | 4 | OUT: `'CUST'` |
| 4 | `COMM-SORTCODE` | `9(6)` | 6 | IN |
| 10 | `COMM-NUMBER` | `9(10)` | 10 | OUT: assigned number |
| 20 | `COMM-TITLE` | `X(10)` | 10 | IN |
| 30 | `COMM-FIRST-NAME` | `X(50)` | 50 | IN |
| 80 | `COMM-LAST-NAME` | `X(50)` | 50 | IN |
| 130 | `COMM-DOB-DAY` | `99` | 2 | IN |
| 132 | `COMM-DOB-MONTH` | `99` | 2 | IN |
| 134 | `COMM-DOB-YEAR` | `9999` | 4 | IN |
| 138 | `COMM-PHONE` | `X(20)` | 20 | IN |
| 158 | `COMM-ADDR-LINE1` | `X(50)` | 50 | IN |
| 208 | `COMM-ADDR-LINE2` | `X(50)` | 50 | IN |
| 258 | `COMM-CITY` | `X(50)` | 50 | IN |
| 308 | `COMM-POSTCODE` | `X(10)` | 10 | IN |
| 318 | `COMM-COUNTRY` | `X(50)` | 50 | IN |
| 368 | `COMM-STATUS` | `X(10)` | 10 | IN |
| 378 | `COMM-CREATED-DAY` | `99` | 2 | IN |
| 380 | `COMM-CREATED-MONTH` | `99` | 2 | IN |
| 382 | `COMM-CREATED-YEAR` | `9999` | 4 | IN |
| 386 | `COMM-CREDIT-SCORE` | `999` | 3 | OUT |
| 389 | `COMM-CS-REVIEW-DAY` | `99` | 2 | OUT |
| 391 | `COMM-CS-REVIEW-MONTH` | `99` | 2 | OUT |
| 393 | `COMM-CS-REVIEW-YEAR` | `9999` | 4 | OUT |
| 397 | `COMM-SUCCESS` | `X` | 1 | OUT |
| 398 | `COMM-FAIL-CODE` | `X` | 1 | OUT |

---

### PE-2: Validation Constants

| Constant | Source | Value | Used In |
|---|---|---|---|
| `SORTCODE` (77-level) | `SORTCODE.cpy` | `987654` (PIC 9(6)) | `REQUIRED-SORT-CODE`, `HV-CUSTOMER-SORTCODE`, `HV-PROCTRAN-SORT-CODE`, NCS name |
| `DATE-OF-BIRTH-FORMAT-LENGTH` | WS line 601 | `10` (S9(4) BINARY) | CEEDAYS format length |
| `DATE-OF-BIRTH-FORMAT-TEXT` | WS line 603 | `'YYYYMMDD'` | CEEDAYS picture string |
| `DATE-OF-BIRTH-CEEDAYS-LENGTH` | WS line 607 | `10` (S9(4) BINARY) | CEEDAYS date string length |
| Min DOB year | inline `PREMIERE_P010` / `DOBC010` | `1601` | If `COMM-DOB-YEAR < 1601` → fail |
| Max customer age | inline `DOBC010` | `150` | If `WS-CUSTOMER-AGE > 150` → fail |
| Credit-check agency count | inline `CREDIT-CHECK_CC010` | `5` | `PERFORM … UNTIL WS-CC-CNT > 5` |
| Review-date maximum offset | inline `CREDIT-CHECK_CC010` | `21` (exclusive upper bound) | `((21 - 1) * RANDOM(EIBTASKN)) + 1` → 1..20 days |
| `NCS-CUST-NO-ACT-NAME` | WS line 442–443 | `'BANKZCUST'` (PIC X(9)) | Named counter base name |
| ENQ length | inline `ENQ-NAMED-COUNTER` | `16` | `EXEC CICS ENQ LENGTH(16)` |
| `NCS-CUST-NO-INC` | WS line 449 | `0` (VALUE), set to `1` in UPD-NCS | NCS increment |
| `CUSTOMER-EYECATCHER-VALUE` | `CUSTOMER.cpy` 88-level | `'CUST'` | Eyecatcher validation |
| `PROC-TRAN-VALID` | `PROCTRAN.cpy` 88-level | `'PRTR'` | Proctran eyecatcher |
| `CUSTOMER-CONTROL-EYECATCHER-V` | `CUSTCTRL.cpy` 88-level | `'CTRL'` | Control record eyecatcher |

---

### PE-3: SQL Column Inventory

#### INSERT INTO CUSTOMER (line 1552–1588)

All 17 columns inserted; all present:

```
CUSTOMER_EYECATCHER   ← :HV-CUSTOMER-EYECATCHER  ('CUST')
CUSTOMER_SORTCODE     ← :HV-CUSTOMER-SORTCODE
CUSTOMER_NUMBER       ← :HV-CUSTOMER-NUMBER       (WS-CUSTOMER-NO-NUM as X(10))
CUSTOMER_TITLE        ← :HV-CUSTOMER-TITLE
CUSTOMER_FIRST_NAME   ← :HV-CUSTOMER-FIRST-NAME
CUSTOMER_LAST_NAME    ← :HV-CUSTOMER-LAST-NAME
CUSTOMER_DATE_OF_BIRTH ← :HV-CUSTOMER-DOB         (YYYYMMDD integer: year*10000+month*100+day)
CUSTOMER_PHONE        ← :HV-CUSTOMER-PHONE
CUSTOMER_ADDR_LINE1   ← :HV-CUSTOMER-ADDR-LINE1
CUSTOMER_ADDR_LINE2   ← :HV-CUSTOMER-ADDR-LINE2
CUSTOMER_CITY         ← :HV-CUSTOMER-CITY
CUSTOMER_POSTCODE     ← :HV-CUSTOMER-POSTCODE
CUSTOMER_COUNTRY      ← :HV-CUSTOMER-COUNTRY
CUSTOMER_STATUS       ← :HV-CUSTOMER-STATUS
CUSTOMER_CREATED_DATE ← :HV-CUSTOMER-CREATE-DATE  (YYYYMMDD integer)
CUSTOMER_CREDIT_SCORE ← :HV-CUSTOMER-CREDIT-SCORE
CUSTOMER_CS_REVIEW_DATE ← :HV-CUSTOMER-CS-REVIEW-DATE (YYYYMMDD integer)
```

**Integer encoding for dates:** `(YYYY * 10000) + (MM * 100) + DD`

#### INSERT INTO PROCTRAN (line 1692–1717)

All 9 columns inserted; all present:

```
PROCTRAN_EYECATCHER ← :HV-PROCTRAN-EYECATCHER  ('PRTR')
PROCTRAN_SORTCODE   ← :HV-PROCTRAN-SORT-CODE
PROCTRAN_NUMBER     ← :HV-PROCTRAN-ACC-NUMBER   (ZEROS for customer-create)
PROCTRAN_DATE       ← :HV-PROCTRAN-DATE          (DD.MM.YYYY — see PE-5)
PROCTRAN_TIME       ← :HV-PROCTRAN-TIME          (HHMMSS)
PROCTRAN_REF        ← :HV-PROCTRAN-REF           (EIBTASKN 12-char)
PROCTRAN_TYPE       ← :HV-PROCTRAN-TYPE          ('OCC')
PROCTRAN_DESC       ← :HV-PROCTRAN-DESC          (40-byte packed desc)
PROCTRAN_AMOUNT     ← :HV-PROCTRAN-AMOUNT        (ZEROS)
```

#### SELECT FROM CONTROL (line 1829–1834)

```
SELECT CONTROL_VALUE_NUM
  INTO :HV-CONTROL-VALUE-NUM
  FROM CONTROL
 WHERE CONTROL_NAME = :HV-CONTROL-NAME
```

#### UPDATE CONTROL (line 1857–1861)

```
UPDATE CONTROL
   SET CONTROL_VALUE_NUM = :HV-CONTROL-VALUE-NUM
 WHERE CONTROL_NAME = :HV-CONTROL-NAME
```

---

### PE-4: Time and Date Formats

| Field | Format | How Set |
|---|---|---|
| `WS-ORIG-DATE` | `DD/MM/YYYY` (10 chars) | `EXEC CICS FORMATTIME DDMMYYYY DATESEP` (uses default separator `/`) |
| `WS-ORIG-DATE-GRP-X` | `DD.MM.YYYY` (10 chars) | `WS-ORIG-DATE-GRP-X` constructed with VALUE `'.'` fillers; moved from `WS-ORIG-DATE` |
| `HV-PROCTRAN-DATE` | `DD.MM.YYYY` (10 chars) | `WS-ORIG-DATE-GRP-X` → `HV-PROCTRAN-DATE` |
| `HV-PROCTRAN-TIME` | `HHMMSS` (6 chars) | `EXEC CICS FORMATTIME … TIME(HV-PROCTRAN-TIME)` |
| `PROC-TRAN-TIME` | `HHMMSS` (numeric 9(6)) | `EXEC CICS FORMATTIME … TIME(PROC-TRAN-TIME OF PROCTRAN-AREA)` |
| `WS-TIME-NOW` | `HHMMSS` (numeric 9(6)) | `EXEC CICS FORMATTIME … TIME(WS-TIME-NOW)` in PTD2010 |
| `ABND-TIME` | `HH:MM:SS` (8 chars) | STRING with `:` separators from `WS-TIME-NOW-GRP-HH/MM/SS` |
| `HV-CUSTOMER-DOB` | `INTEGER` (YYYYMMDD) | `COMPUTE = (YYYY * 10000) + (MM * 100) + DD` |
| `HV-CUSTOMER-CREATE-DATE` | `INTEGER` (YYYYMMDD) | Same formula |
| `HV-CUSTOMER-CS-REVIEW-DATE` | `INTEGER` (YYYYMMDD) | Same formula |
| `COMM-CS-REVIEW-DATE` | `DDMMYYYY` (8-char) | `STRING DD,MM,YYYY INTO COMM-CS-REVIEW-DATE` (no separators) on error paths; positional MOVE on success path |
| `WS-NEW-REVIEW-YYYYMMDD` | `YYYYMMDD` integer (8 digits) | `FUNCTION DATE-OF-INTEGER(WS-NEW-REVIEW-DATE-INT)` |
| `STORED-DOB` | `DD/MM/YYYY` (10 chars) | Manual: `DOB-DAY(1:2)` + `'/'` + `DOB-MONTH(4:2)` + `'/'` + `DOB-YEAR(7:4)` |

**⚠ Date Transformation Note:** `COMM-CS-REVIEW-DATE` is a 7-byte (`PIC 999` for score, then `99 99 9999` for date components) field group but is written as a flat string in error paths. The success path uses positional reference-modification. Both encodings must be preserved.

---

### PE-5: CICS ABEND Codes

| ABEND Code | Where | Path Type | Notes |
|---|---|---|---|
| `'HWPT'` | `WRITE-PROCTRAN-DB2_WPD010` (line 1789) | Notifying-abort | LINK ABNDPROC first (fills ABNDINFO-REC), then ABEND |

**ABND-CODE field:** `MOVE 'HWPT' TO ABND-CODE` (line 1754) — the code is set in ABNDINFO-REC before the LINK.  
**Java rule (Rule 4 + Rule 1):** Declare `'HWPT'` as a named constant in the exception class. The LINK to `ABNDPROC` MUST precede any throw — this is a notifying-abort path.

---

### PE-6: CICS LINK Return-Code Inspection

| LINK Target | After LINK | Notes |
|---|---|---|
| `ABNDPROC` | No return-code inspection — `EXEC CICS ABEND` follows immediately | Return code ignored — falls through to ABEND |

**Java rule (Rule 10):** The `linkAbndproc()` delegate method must return `void`. No result DTO.

---

### PE-7: CICS Async API Inventory

| CICS Command | Field | Notes |
|---|---|---|
| `EXEC CICS PUT CONTAINER` | `WS-PUT-CONT-NAME` (CIPA–CIPE), `WS-CHANNEL-NAME` (`'CIPCREDCHANN    '`), `DFHCOMMAREA` | Fail → `COMM-FAIL-CODE='A'` |
| `EXEC CICS RUN TRANSID` | `WS-RUN-TRANSID` (OCR1–OCR5), channel, `WS-ANY-CHILD-TKN` | Fail → `COMM-FAIL-CODE='B'` |
| `EXEC CICS DELAY FOR SECONDS(3)` | — | Hard-coded 3-second delay |
| `EXEC CICS FETCH ANY` | `NOSUSPEND`, `COMPSTATUS(WS-CHILD-FETCH-COMPST)`, `ABCODE(WS-ANY-CHILD-FETCH-ABCODE)` | NOTFINISHED/RESP2=52, INVREQ/RESP2=1, NOTFND/RESP2=1 all handled |
| `EXEC CICS GET CONTAINER` | `WS-CONTAINER-NAME`, `WS-ANY-CHILD-FETCH-CHAN`, `WS-CHILD-DATA` | Fail → `COMM-FAIL-CODE='E'` |

---

### PE-8: Fail Code Inventory

| `COMM-FAIL-CODE` | Set In | Meaning |
|---|---|---|
| `'T'` | `PREMIERE_P010` | Invalid title |
| `'G'` | `PREMIERE_P010` (post CC check) | Credit-check error (general) |
| `'A'` | `CREDIT-CHECK_CC010` | PUT CONTAINER failed |
| `'B'` | `CREDIT-CHECK_CC010` | RUN TRANSID failed |
| `'C'` | `CREDIT-CHECK_CC010` | FETCH ANY NOTFINISHED + no data |
| `'D'` | `CREDIT-CHECK_CC010` | FETCH ANY INVREQ (no children) |
| `'E'` | `CREDIT-CHECK_CC010` | GET CONTAINER failed |
| `'F'` | `CREDIT-CHECK_CC010` | FETCH ANY completion = ABEND |
| `'G'` | `CREDIT-CHECK_CC010` | FETCH ANY completion = SECERROR |
| `'H'` | `CREDIT-CHECK_CC010` | FETCH ANY completion = OTHER |
| `'1'` | `WRITE-CUSTOMER-DB2_WCD010` | INSERT CUSTOMER failed (SQL) |
| `'3'` | `ENQ-NAMED-COUNTER_ENC010` | ENQ failed |
| `'4'` | `GET-LAST-CUSTOMER-DB2_GLCD010` | SELECT or UPDATE CONTROL failed |
| `'5'` | `DEQ-NAMED-COUNTER_DNC010` | DEQ failed |
| `'O'` | `DATE-OF-BIRTH-CHECK_DOBC010` | DOB year < 1601 or age > 150 |
| `'Y'` | `DATE-OF-BIRTH-CHECK_DOBC010` | DOB is in the future |
| `'Z'` | `DATE-OF-BIRTH-CHECK_DOBC010` | CEEDAYS returned non-CEE000 |
| `' '` | `WRITE-CUSTOMER-DB2_WCD010` | Success (single space) |

**Note:** `'G'` is reused: once for post-credit-check-error (in PREMIERE) and once for SECERROR (in CREDIT-CHECK). Both set `COMM-SUCCESS='N'`.

---

### PE-9: DISPLAY Statements (Diagnostic/Debug)

The program contains numerous `DISPLAY` statements for diagnostic output. None are `DISPLAY … ACCEPT` interactive pairs. No `ACCEPT` statements exist in this program. **Rule 18 does not apply** — there are no user-facing I/O contracts to preserve.

Key `DISPLAY` outputs for context:

| Location | Content |
|---|---|
| `PREMIERE_P010` line 818 | `'WS-CREDIT-CHECK-ERROR = Y ...'` + RESP/RESP2 |
| `CREDIT-CHECK_CC010` | PUT CONTAINER / RUN TRANSID / FETCH ANY failures with RESP/RESP2 |
| `WRITE-CUSTOMER-DB2_WCD010` | Customer field values + HV field values |
| `WRITE-PROCTRAN-DB2_WPD010` | `'UNABLE TO WRITE TO PROCTRAN DB2 DATASTORE'` + SQLCODE + HOST-PROCTRAN-ROW |
| `GET-LAST-CUSTOMER-DB2_GLCD010` | Control name and SQLCODE on SELECT/UPDATE failure |
| `DATE-OF-BIRTH-CHECK_DOBC010` | CEEDAYS/CEELOCT failure messages |

---

### PE-10: Credential / Security Fields

No password or credential fields are stored, initialised, or passed in this program. **Rule 12 does not apply.**



## Transformation Risk Areas

### TRA-1: REDEFINES Groups — All 9 Groups

| Anchor | Category | Action |
|---|---|---|
| `PROC-TRAN-EYE-CATCHER` | NOT classified (2 records, no PICs) | Two-record REDEFINES: `PROC-TRAN-LOGICAL-DELETE-AREA` redefines eye-catcher; apply Cat 4a mutual-exclusivity analysis |
| `PROC-TRAN-DATE` | Cat 3a | One record (`PROC-TRAN-DATE-GRP`), one PIC. Model record as Java class; PIC as getter/setter |
| `PROC-TRAN-TIME` | Cat 3a | One record (`PROC-TRAN-TIME-GRP`), one PIC. Model record as Java class; PIC as getter/setter |
| `PROC-TRAN-DESC` | Cat 4a | Six siblings (1 PIC + 5 records: XFR, DELACC, CREACC, DELCUS, CRECUS). **Non-mutually exclusive** — multiple views can be read simultaneously. Apply Pattern B (shared byte[] base class). |
| `WS-TIME-NOW` | Cat 3a | One record (`WS-TIME-NOW-GRP`), one PIC. Model record as Java class |
| `WS-ORIG-DATE` | Cat 3a | One record (`WS-ORIG-DATE-GRP`), one PIC. Model record; PIC view via getter/setter |
| `CUSTOMER-KY2` | Cat 3a | One PIC (`CUSTOMER-KY2-BYTES X(16)`), one record. Byte-array view of key |
| `WS-CICSTSLEVEL` | Cat 3a | One record (`WS-CICSTS-LEVEL-NUM-GRP`), one PIC. Model record |
| `CASE-1-CONDITION-ID` | Cat 4a | Two records (`CASE-2-CONDITION-ID`). **Mutual exclusivity** — checked via EVALUATE on condition code type. Apply Pattern A |

**Highest Risk:** `PROC-TRAN-DESC` (Cat 4a, 6 siblings, 5 record views). Requires Pattern B with `byte[]` base class and per-view getters/setters for all 6 layouts. The `PROC-TRAN-DESC-CRECUS` view is the one actively written in this program.

---

### TRA-2: CICS Asynchronous Child Transactions

The `CREDIT-CHECK` paragraph uses CICS 4.0+ Asynchronous Processing API (`RUN TRANSID`, `FETCH ANY`, `PUT/GET CONTAINER`). This has no direct JCICS equivalent in standard patterns — requires:
- JCICS `ChildTask` or `Task.runAsync()` analogues
- Container/Channel APIs via `com.ibm.cics.server.Channel` and `com.ibm.cics.server.Container`
- Handling of `COMPSTATUS` values: NORMAL, ABEND, SECERROR, OTHER
- Handling of FETCH response codes: NOTFINISHED (RESP2=52), INVREQ (RESP2=1), NOTFND (RESP2=1)
- Hard-coded 3-second `Thread.sleep()` equivalent

The 5-transaction parallel launch loop and accumulator pattern must be preserved exactly.

---

### TRA-3: Language Environment (LE) Runtime Calls

Two non-CICS, non-SQL CALL statements:
- `CALL "CEEDAYS"` — converts a date string to Lilian day number. Java equivalent: `LocalDate.toEpochDay()` or `ChronoField.EPOCH_DAY`, but the Lilian epoch (Oct 15, 1582) differs from Java's Unix epoch. Must use `ChronoField` or adjust offset.
- `CALL "CEELOCT"` — returns today's Lilian day + seconds + Gregorian breakdown. Java equivalent: `LocalDateTime.now()` components.

Both return a feedback code structure (`FC`) with `CEE000` indicating success. The `CEE000` condition must be modelled as a success constant derived from the CEE condition token structure.

---

### TRA-4: DB2 Integer Date Encoding

Three DB2 columns store dates as INTEGER values (not DB2 DATE type):
- `CUSTOMER_DATE_OF_BIRTH` — `(YYYY * 10000) + (MM * 100) + DD`
- `CUSTOMER_CREATED_DATE` — same formula
- `CUSTOMER_CS_REVIEW_DATE` — same formula

Java must reproduce this encoding exactly (not use `LocalDate` directly for the integer value).

In contrast, `PROCTRAN_DATE` is declared as `DATE` type and receives `DD.MM.YYYY` string format from `HV-PROCTRAN-DATE`.

---

### TRA-5: COMM-CS-REVIEW-DATE Dual Encoding

`COMM-CS-REVIEW-DATE` is written two different ways:

1. **Error paths** (multiple locations in CREDIT-CHECK): `STRING WS-ORIG-DATE-DD … WS-ORIG-DATE-MM … WS-ORIG-DATE-YYYY INTO COMM-CS-REVIEW-DATE` — produces `DDMMYYYY` (8 chars, no separators). Since COMM-CS-REVIEW-DATE is structured as `99 + 99 + 9999 = 8 bytes`, the entire group is overwritten as a flat string.

2. **Success path** (after credit score computed): Positional reference-modification `WS-NEW-REVIEW-YYYYMMDD(1:4)` → `COMM-CS-REVIEW-DATE(5:4)`, `(5:2)` → `(3:2)`, `(7:2)` → `(1:2)` — this reorders YYYYMMDD → DDMMYYYY into the commarea components.

Both paths must produce identical byte layout for `COMM-CS-REVIEW-DATE` = DDMMYYYY. Java implementation must replicate both paths with the same resulting format.

---

### TRA-6: CICS Named Counter Service (ENQ/DEQ)

CICS ENQ/DEQ on resource name `NCS-CUST-NO-NAME` (16 bytes = `'BANKZCUST' + SORTCODE + '  '`). This is a CICS-specific concurrency mechanism with no direct Java equivalent. Java transformation options:
- Replace with DB2 `SELECT FOR UPDATE` on CONTROL table (which is already the synchronisation point)
- Or use a distributed lock if running outside CICS

The DEQ is called from 4 call sites. The Java equivalent must be a single method called from 4 places, matching the Fan-In-4 pattern.

---

### TRA-7: PROCTRAN DESC Byte Layout

`HV-PROCTRAN-DESC` is assembled byte-by-byte using positional reference modification:

```
HV-PROCTRAN-DESC(1:6)   ← STORED-SORTCODE  (6 bytes)
HV-PROCTRAN-DESC(7:10)  ← STORED-CUSTNO    (10 bytes)
HV-PROCTRAN-DESC(17:14) ← STORED-NAME      (14 bytes)  [note: gap at 17 not 16+1=17, offset is 17]
HV-PROCTRAN-DESC(31:10) ← STORED-DOB       (10 bytes)
```

**Note:** There is an apparent 0-byte gap between position 16 and 17 (offset 16 is the boundary of the custno field). This must be mapped as an exact byte-offset copy in Java. The layout aligns with `PROC-TRAN-DESC-CRECUS` which has: 6+10+14+DOB(9 bytes with separators). The `STORED-DOB` is formatted as `DD/MM/YYYY` (10 bytes) but only 10 bytes are moved to offset 31–40.

---

### TRA-8: High Fan-In Paragraphs

| Paragraph | Fan-In | Risk |
|---|---|---|
| `GET-ME-OUT-OF-HERE` | High (>8 callers) | Must be modelled as a shared `return`/exit mechanism; cannot inline the CICS RETURN at each call site |
| `DEQ-NAMED-COUNTER` | 4 | Must be a single reusable method |

---

### TRA-9: Unresolved Copybooks / External Includes

| Item | Status |
|---|---|
| `CEEIGZCT` (included in FC structure, line 615) | Not in the copybook list — inlined in expanded source. The structure defines `CASE-1-CONDITION-ID` (SEVERITY + MSG-NO) and `CASE-2-CONDITION-ID` REDEFINES. Resolved from expanded source. |
| All other copybooks | Resolved — see Data Structures section |

No unresolved constants (Rule 9 — no placeholders required).

---

### TRA-10: Cross-Program Data Structures

The following structures defined in this program are consumed by other programs in the suite (Rule 14):

| Structure | Defined In | Consumed By |
|---|---|---|
| `DFHCOMMAREA` / `CRECUST.cpy` commarea layout | This program (LINKAGE) | Callers of CRECUST (any program that LINKs to CRECUST) |
| `CUSTOMER-RECORD` (CUSTOMER.cpy) | LOCAL-STORAGE `OUTPUT-DATA` | Any program reading/writing the CUSTOMER DB2 table via this copybook |
| `PROCTRAN-AREA` (PROCTRAN.cpy) | WORKING-STORAGE | All programs that write to PROCTRAN |
| `WS-CHILD-DATA` / `WS-CHILD-CUSTOMER-RECORD` | WORKING-STORAGE | OCR1–OCR5 child transactions (produce this layout in a CICS container) |

All these structures **must be transformed** regardless of whether the current program actively reads all their fields. They are shared contracts.


