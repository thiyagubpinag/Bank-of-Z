## TS Metadata (static analysis output)

**Source language**: cobol
**Middleware**: CICS

### Paragraphs (8 total)

| # | Name | Lines | Calls | In (inputs) | Out (outputs) |
|---|------|-------|-------|-------------|---------------|
| 1 | `PREMIERE_A010` | 279–350 | `GET-ACCOUNTS_GAC010`, `DEL-CUST-DB2_DCD010`, `GET-ME-OUT-OF-HERE_GMOFH010`, `DELETE-ACCOUNTS_DA010` | `WS-INDEX`, `COMM-SCODE`, `COMM-CUSTNO` | `DESIRED-KEY-SORTCODE`, `DESIRED-KEY-CUSTOMER` |
| 3 | `DELETE-ACCOUNTS_DA010` | 357–387 | — | `WS-INDEX`, `NUMBER-OF-ACCOUNTS` | — |
| 5 | `GET-ACCOUNTS_GAC010` | 394–415 | — | `COMM-CUSTNO` | — |
| 7 | `DEL-CUST-DB2_DCD010` | 422–711 | `POPULATE-TIME-DATE_PTD010`, `WRITE-PROCTRAN-CUST_WPC010`, `POPULATE-TIME-DATE_PTD010` | `DESIRED-KEY-SORTCODE`, `DESIRED-KEY-CUSTOMER`, `COMM-SCODE`, `COMM-CUSTNO` | `WS-STOREDC-SORTCODE`, `WS-STOREDC-NUMBER`, `WS-STOREDC-NAME`, `WS-STOREDC-DATE-OF-BIRTH` |
| 9 | `WRITE-PROCTRAN-CUST_WPC010` | 718–723 | `WRITE-PROCTRAN-CUST-DB2_WPCD010` | `WS-STOREDC-SORTCODE`, `WS-STOREDC-NUMBER`, `WS-STOREDC-NAME`, `WS-STOREDC-DATE-OF-BIRTH` | — |
| 11 | `WRITE-PROCTRAN-CUST-DB2_WPCD010` | 729–875 | `POPULATE-TIME-DATE_PTD010` | `WS-STOREDC-SORTCODE`, `WS-STOREDC-NUMBER`, `WS-STOREDC-NAME`, `WS-STOREDC-DATE-OF-BIRTH` | — |
| 13 | `GET-ME-OUT-OF-HERE_GMOFH010` | 882–884 | — | — | — |
| 15 | `POPULATE-TIME-DATE_PTD010` | 891–902 | — | — | — |

### Variable hierarchy (52 top-level of 405 total)

- `SORTCODE` *(src/base/cics/copy/SORTCODE.cpy:7)*
- `SYSIDERR-RETRY` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:48)*
- `FILE-RETRY` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:49)*
- `WS-EXIT-RETRY-LOOP` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:50)*
- `HOST-CUSTOMER-ROW` → `HV-CUSTOMER-EYECATCHER`, `HV-CUSTOMER-SORTCODE`, `HV-CUSTOMER-NUMBER`, `HV-CUSTOMER-TITLE`, `HV-CUSTOMER-FIRST-NAME`, `HV-CUSTOMER-LAST-NAME`, `HV-CUSTOMER-DOB`, `HV-CUSTOMER-PHONE`, `HV-CUSTOMER-ADDR-LINE1`, `HV-CUSTOMER-ADDR-LINE2`, `HV-CUSTOMER-CITY`, `HV-CUSTOMER-POSTCODE`, `HV-CUSTOMER-COUNTRY`, `HV-CUSTOMER-STATUS`, `HV-CUSTOMER-CREATE-DATE`, `HV-CUSTOMER-CREDIT-SCORE`, `HV-CUSTOMER-CS-REVIEW-DATE` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:58)*
- `HOST-PROCTRAN-ROW` → `HV-PROCTRAN-EYECATCHER`, `HV-PROCTRAN-SORT-CODE`, `HV-PROCTRAN-ACC-NUMBER`, `HV-PROCTRAN-DATE`, `HV-PROCTRAN-TIME`, `HV-PROCTRAN-REF`, `HV-PROCTRAN-TYPE`, `HV-PROCTRAN-DESC`, `HV-PROCTRAN-AMOUNT` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:83)*
- `SQLCODE-DISPLAY` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:99)*
- `WS-CICS-WORK-AREA` → `WS-CICS-RESP`, `WS-CICS-RESP2` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:102)*
- `EXIT-BROWSE-LOOP` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:106)*
- `OUTPUT-DATA` → `ACCOUNT-DATA` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:108)*
- `OUTPUT-CUST-DATA` → `CUSTOMER-RECORD` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:111)*
- `PROCTRAN-AREA` → `PROC-TRAN-DATA` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:114)*
- `PROCTRAN-RIDFLD` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:117)*
- `PROCTRAN-RETRY` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:118)*
- `ACCOUNT-ACT-BAL-STORE` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:120)*
- `RETURNED-DATA` → `RETURNED-EYE-CATCHER`, `RETURNED-CUST-NO`, `RETURNED-KEY`, `RETURNED-TYPE`, `RETURNED-INTEREST-RATE`, `RETURNED-OPENED`, `RETURNED-OVERDRAFT-LIMIT`, `RETURNED-LAST-STMT-DATE`, `RETURNED-NEXT-STMT-DATE`, `RETURNED-AVAILABLE-BALANCE`, `RETURNED-ACTUAL-BALANCE` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:122)*
- `ACCTCUST-DESIRED-KEY` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:137)*
- `DB2-DATE-REFORMAT` → `DB2-DATE-REF-YR`, `FILLER #2`, `DB2-DATE-REF-MNTH`, `FILLER #3`, `DB2-DATE-REF-DAY` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:139)*
- `DB2-EXIT-LOOP` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:146)*
- `FETCH-DATA-CNT` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:147)*
- `WS-CUST-ALT-KEY-LEN` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:148)*
- `WS-EIBTASKN12` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:151)*
- `WS-CNT` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:153)*
- `CUSTOMER-KY` → `REQUIRED-SORT-CODE` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:156)*
- `WS-ACC-KEY-LEN` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:162)*
- `WS-ACC-NUM` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:164)*
- `WS-CUST-KEY-LEN` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:167)*
- `WS-CUST-NUM` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:169)*
- `DESIRED-KEY-ACCTCUST` → `DESIRED-KEY-CUSTOMER-ACCTCUST`, `DESIRED-KEY-SORTCODE-ACCTCUST` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:172)*
- `DESIRED-KEY` → `DESIRED-KEY-SORTCODE`, `DESIRED-KEY-CUSTOMER` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:178)*
- `WS-U-TIME` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:182)*
- `WS-ORIG-DATE` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:183)*
- `WS-ORIG-DATE-GRP` REDEFINES `WS-ORIG-DATE` → `WS-ORIG-DATE-DD`, `FILLER #4`, `WS-ORIG-DATE-MM`, `FILLER #5`, `WS-ORIG-DATE-YYYY` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:184)*
- `WS-ORIG-DATE-GRP-X` → `WS-ORIG-DATE-DD-X`, `FILLER #6`, `WS-ORIG-DATE-MM-X`, `FILLER #7`, `WS-ORIG-DATE-YYYY-X` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:191)*
- `REMIX-STMT-DATE` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:198)*
- `WS-APPLID` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:200)*
- `VAR-REMIX` → `REMIX-SCODE`, `REMIX-CHAR` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:202)*
- `WS-STOREDC-CUSTOMER` → `WS-STOREDC-EYECATCHER`, `WS-STOREDC-SORTCODE`, `WS-STOREDC-NUMBER`, `WS-STOREDC-NAME`, `WS-STOREDC-ADDRESS`, `WS-STOREDC-DATE-OF-BIRTH`, `WS-STOREDC-CREDIT-SCORE`, `WS-STOREDC-CS-REVIEW-DATE` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:210)*
- `WS-NONE-LEFT` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:222)*
- `WS-EXIT-FETCH` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:224)*
- `DELACC-COMMAREA` → `DELACC-COMM-EYE`, `DELACC-COMM-CUSTNO`, `DELACC-COMM-SCODE`, `DELACC-COMM-ACCNO`, `DELACC-COMM-ACC-TYPE`, `DELACC-COMM-INT-RATE`, `DELACC-COMM-OPENED`, `DELACC-COMM-OVERDRAFT`, `DELACC-COMM-LAST-STMT-DT`, `DELACC-COMM-NEXT-STMT-DT`, `DELACC-COMM-AVAIL-BAL`, `DELACC-COMM-ACTUAL-BAL`, `DELACC-COMM-SUCCESS`, `DELACC-COMM-FAIL-CD`, `DELACC-COMM-DEL-SUCCESS`, `DELACC-COMM-DEL-FAIL-CD`, `DELACC-COMM-APPLID`, `DELACC-COMM-PCB1`, `DELACC-COMM-PCB2` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:226)*
- `WS-TOKEN` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:248)*
- `WS-INDEX` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:249)*
- `INQACCCU-PROGRAM` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:251)*
- `INQACCCU-COMMAREA` → `NUMBER-OF-ACCOUNTS`, `CUSTOMER-NUMBER`, `COMM-SUCCESS`, `COMM-FAIL-CODE`, `CUSTOMER-FOUND`, `COMM-PCB-POINTER`, `ACCOUNT-DETAILS` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:252)*
- `STORM-DRAIN-CONDITION` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:255)*
- `INQCUST-PROGRAM` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:256)*
- `INQCUST-COMMAREA` → `INQCUST-EYE`, `INQCUST-SCODE`, `INQCUST-CUSTNO`, `INQCUST-NAME`, `INQCUST-DOB`, `INQCUST-PHONE`, `INQCUST-ADDR`, `INQCUST-STATUS`, `INQCUST-CREATED-DATE`, `INQCUST-CREDIT-SCORE`, `INQCUST-CS-REVIEW-DT`, `INQCUST-INQ-SUCCESS`, `INQCUST-INQ-FAIL-CD`, `INQCUST-PCB-POINTER` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:257)*
- `WS-TIME-DATA` → `WS-TIME-NOW`, `WS-TIME-NOW-GRP` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:260)*
- `WS-ABEND-PGM` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:267)*
- `ABNDINFO-REC` → `ABND-VSAM-KEY`, `ABND-APPLID`, `ABND-TRANID`, `ABND-DATE`, `ABND-TIME`, `ABND-CODE`, `ABND-PROGRAM`, `ABND-RESPCODE`, `ABND-RESP2CODE`, `ABND-SQLCODE`, `ABND-FREEFORM` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:269)*
- `DFHCOMMAREA` → `COMM-EYE`, `COMM-SCODE`, `COMM-CUSTNO`, `COMM-NAME`, `COMM-DOB`, `COMM-PHONE`, `COMM-ADDR`, `COMM-STATUS`, `COMM-CREATED-DATE`, `COMM-CREDIT-SCORE`, `COMM-CS-REVIEW-DATE`, `COMM-DEL-SUCCESS`, `COMM-DEL-FAIL-CD` *(.bobz/expanded-single/cobol/src/base/cics/cobol/DELCUS.cbl:273)*