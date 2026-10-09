## TS Metadata (static analysis output)

**Source language**: cobol
**Middleware**: CICS

### Paragraphs (12 total)

| # | Name | Lines | Calls | In (inputs) | Out (outputs) |
|---|------|-------|-------|-------------|---------------|
| 1 | `PREMIERE_P010` | 407–520 | `POPULATE-TIME-DATE_PTD010`, `CREDIT-CHECK_CC010`, `DATE-OF-BIRTH-CHECK_DOBC010`, `ENQ-NAMED-COUNTER_ENC010`, `UPD-NCS_UN010`, `WRITE-CUSTOMER-DB2_WCD010` | `WS-CICS-RESP`, `WS-CICS-RESP2`, `WS-CC-CNT`, `WS-CHILD-TKN`, `WS-CHILD-DATA`, `COMM-EYECATCHER`, `COMM-SORTCODE`, `COMM-NUMBER`, `COMM-CREDIT-SCORE`, `COMM-CS-REVIEW-DATE`, `COMM-SUCCESS`, `COMM-FAIL-CODE` | `COMM-CREDIT-SCORE`, `COMM-CS-REVIEW-DATE` |
| 2 | `POPULATE-TIME-DATE_PTD010` | 523–534 | — | — | — |
| 4 | `ENQ-NAMED-COUNTER_ENC010` | 541–556 | — | — | — |
| 6 | `DEQ-NAMED-COUNTER_DNC010` | 563–581 | — | — | — |
| 8 | `UPD-NCS_UN010` | 588–596 | `GET-LAST-CUSTOMER-DB2_GLCD010` | — | — |
| 10 | `CREDIT-CHECK_CC010` | 605–1131 | — | `WS-CICS-RESP`, `WS-CICS-RESP2`, `WS-CC-CNT`, `WS-CHILD-TKN`, `WS-CHILD-DATA`, `COMM-EYECATCHER`, `COMM-SORTCODE`, `COMM-NUMBER`, `COMM-CREDIT-SCORE`, `COMM-CS-REVIEW-DATE`, `COMM-SUCCESS`, `COMM-FAIL-CODE` | — |
| 12 | `WRITE-CUSTOMER-DB2_WCD010` | 1139–1306 | `DEQ-NAMED-COUNTER_DNC010`, `WRITE-PROCTRAN_WP010`, `DEQ-NAMED-COUNTER_DNC010` | `WS-CUSTOMER-NO-NUM`, `NCS-CUST-NO-VALUE`, `COMM-CREDIT-SCORE`, `COMM-CS-REVIEW-DATE` | `STORED-SORTCODE`, `STORED-CUSTNO`, `STORED-NAME`, `STORED-DOB` |
| 14 | `WRITE-PROCTRAN_WP010` | 1313–1314 | `WRITE-PROCTRAN-DB2_WPD010` | `STORED-SORTCODE`, `STORED-CUSTNO`, `STORED-NAME`, `STORED-DOB` | — |
| 16 | `WRITE-PROCTRAN-DB2_WPD010` | 1321–1458 | `POPULATE-TIME-DATE2_PTD2010`, `DEQ-NAMED-COUNTER_DNC010` | `STORED-SORTCODE`, `STORED-CUSTNO`, `STORED-NAME`, `STORED-DOB` | — |
| 20 | `GET-LAST-CUSTOMER-DB2_GLCD010` | 1477–1549 | `DEQ-NAMED-COUNTER_DNC010`, `DEQ-NAMED-COUNTER_DNC010` | — | — |
| 22 | `DATE-OF-BIRTH-CHECK_DOBC010` | 1556–1609 | — | — | — |
| 24 | `POPULATE-TIME-DATE2_PTD2010` | 1616–1628 | — | — | — |

### Variable hierarchy (89 top-level of 419 total)

- `SORTCODE` *(src/base/cics/copy/SORTCODE.cpy:7)*
- `SYSIDERR-RETRY` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:59)*
- `HOST-CUSTOMER-ROW` → `HV-CUSTOMER-EYECATCHER`, `HV-CUSTOMER-SORTCODE`, `HV-CUSTOMER-NUMBER`, `HV-CUSTOMER-TITLE`, `HV-CUSTOMER-FIRST-NAME`, `HV-CUSTOMER-LAST-NAME`, `HV-CUSTOMER-DOB`, `HV-CUSTOMER-PHONE`, `HV-CUSTOMER-ADDR-LINE1`, `HV-CUSTOMER-ADDR-LINE2`, `HV-CUSTOMER-CITY`, `HV-CUSTOMER-POSTCODE`, `HV-CUSTOMER-COUNTRY`, `HV-CUSTOMER-STATUS`, `HV-CUSTOMER-CREATE-DATE`, `HV-CUSTOMER-CREDIT-SCORE`, `HV-CUSTOMER-CS-REVIEW-DATE` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:67)*
- `HOST-PROCTRAN-ROW` → `HV-PROCTRAN-EYECATCHER`, `HV-PROCTRAN-SORT-CODE`, `HV-PROCTRAN-ACC-NUMBER`, `HV-PROCTRAN-DATE`, `HV-PROCTRAN-TIME`, `HV-PROCTRAN-REF`, `HV-PROCTRAN-TYPE`, `HV-PROCTRAN-DESC`, `HV-PROCTRAN-AMOUNT` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:92)*
- `HOST-CONTROL-ROW` → `HV-CONTROL-NAME`, `HV-CONTROL-VALUE-NUM`, `HV-CONTROL-VALUE-STR` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:109)*
- `PROCTRAN-AREA` → `PROC-TRAN-DATA` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:119)*
- `PROCTRAN-RIDFLD` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:122)*
- `WS-CICS-WORK-AREA` → `WS-CICS-RESP`, `WS-CICS-RESP2` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:124)*
- `WS-CUSTOMER-NO-NUM` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:128)*
- `WS-TIME-DATA` → `WS-TIME-NOW`, `WS-TIME-NOW-GRP` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:130)*
- `WS-ABEND-PGM` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:137)*
- `ABNDINFO-REC` → `ABND-VSAM-KEY`, `ABND-APPLID`, `ABND-TRANID`, `ABND-DATE`, `ABND-TIME`, `ABND-CODE`, `ABND-PROGRAM`, `ABND-RESPCODE`, `ABND-RESP2CODE`, `ABND-SQLCODE`, `ABND-FREEFORM` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:138)*
- `FILE-RETRY` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:142)*
- `WS-EXIT-RETRY-LOOP` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:143)*
- `OUTPUT-DATA` → `CUSTOMER-RECORD` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:145)*
- `RETURN-DATA` → `RETURN-DATA-EYECATCHER`, `RETURN-DATA-NUMBER`, `RETURN-DATA-NAME`, `RETURN-DATA-ADDRESS`, `RETURN-DATA-DATE-OF-BIRTH` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:148)*
- `CUSTOMER-KY` → `REQUIRED-SORT-CODE`, `REQUIRED-CUST-NUMBER` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:156)*
- `RANDOM-CUSTOMER` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:160)*
- `HIGHEST-CUST-NUMBER` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:161)*
- `EXIT-VSAM-READ` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:163)*
- `EXIT-DB2-READ` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:164)*
- `WS-V-RETRIED` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:166)*
- `WS-D-RETRIED` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:167)*
- `SQLCODE-DISPLAY` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:169)*
- `NCS-CUST-NO-STUFF` → `NCS-CUST-NO-NAME`, `NCS-CUST-NO-INC`, `NCS-CUST-NO-VALUE`, `NCS-CUST-NO-RESP` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:176)*
- `WS-DISP-CUST-NO-VAL` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:192)*
- `WS-CUST-REC-LEN` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:194)*
- `NCS-UPDATED` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:197)*
- `WS-U-TIME` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:199)*
- `WS-ORIG-DATE` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:200)*
- `WS-ORIG-DATE-GRP` REDEFINES `WS-ORIG-DATE` → `WS-ORIG-DATE-DD`, `FILLER #2`, `WS-ORIG-DATE-MM`, `FILLER #3`, `WS-ORIG-DATE-YYYY` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:201)*
- `WS-ORIG-DATE-GRP-X` → `WS-ORIG-DATE-DD-X`, `FILLER #4`, `WS-ORIG-DATE-MM-X`, `FILLER #5`, `WS-ORIG-DATE-YYYY-X` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:208)*
- `STORED-SORTCODE` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:215)*
- `STORED-CUSTNO` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:216)*
- `STORED-NAME` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:217)*
- `STORED-DOB` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:218)*
- `WS-EIBTASKN12` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:220)*
- `PROCTRAN-RETRY` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:221)*
- `CUSTOMER-KY2` → `REQUIRED-SORT-CODE2`, `REQUIRED-CUST-NUMBER2` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:223)*
- `CUSTOMER-KY2-BYTES` REDEFINES `CUSTOMER-KY2` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:227)*
- `HIGHEST-CUST-NUMBER` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:230)*
- `WS-CC-CNT` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:231)*
- `WS-FINISHED-FETCHING` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:232)*
- `WS-RETRIEVED-CNT` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:233)*
- `WS-CHANNEL-NAME` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:234)*
- `WS-CREDIT-CHECK-ERROR` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:235)*
- `WS-ACTUAL-CS-SCR` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:236)*
- `WS-TOTAL-CS-SCR` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:237)*
- `WS-CHILD-TOKENS` → `WS-ANY-CHILD-TKN`, `WS-ANY-CHILD-FETCH-TKN` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:239)*
- `WS-ANY-CHILD-FETCH-CHAN` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:243)*
- `WS-ANY-CHILD-FETCH-ABCODE` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:244)*
- `WS-CHILD-ISSUED-CNT` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:245)*
- `WS-CHILD-ARRAY` → `WS-CHILD-DETAILS` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:247)*
- `WS-CHILD-RECEIVED-CNT` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:252)*
- `WS-RECEIVE-CHILD-ARRAY` → `WS-RECEIVE-CHILD-DTLS` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:253)*
- `WS-CHILD-FETCH-COMPST` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:257)*
- `WS-CHILD-DATA` → `WS-CHILD-CUSTOMER-RECORD` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:259)*
- `WS-CONTAINER-NAME` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:293)*
- `WS-CHILD-CONTAINER-LEN` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:294)*
- `WS-RUN-TRANSID` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:296)*
- `CICSTSLEVEL` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:298)*
- `WS-CICSTS-LEVEL-DATA` → `WS-CICSTSLEVEL`, `WS-CICSTS-LEVEL-NUM-GRP` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:300)*
- `WS-CURRENT-DATE-DATA` → `WS-CURRENT-DATE`, `WS-CURRENT-TIME`, `WS-DIFFERENCE-FROM-GMT` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:307)*
- `WS-CURRENT-DATE-9` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:319)*
- `WS-TODAY-INT` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:320)*
- `WS-REVIEW-DATE-ADD` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:321)*
- `WS-NEW-REVIEW-DATE-INT` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:322)*
- `WS-NEW-REVIEW-YYYYMMDD` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:323)*
- `WS-PUT-CONT-NAME` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:324)*
- `WS-PUT-CONT-LEN` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:325)*
- `WS-SEED` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:328)*
- `STORM-DRAIN-CONDITION` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:330)*
- `WS-DATE-OF-BIRTH-ERROR` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:332)*
- `WS-DATE-OF-BIRTH-LILLIAN` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:333)*
- `DATE-OF-BIRTH-FORMAT` → `DATE-OF-BIRTH-FORMAT-LENGTH`, `DATE-OF-BIRTH-FORMAT-TEXT` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:335)*
- `DATE-OF-BIRTH-FOR-CEEDAYS` → `DATE-OF-BIRTH-CEEDAYS-LENGTH`, `CEEDAYS-YEAR`, `CEEDAYS-MONTH`, `CEEDAYS-DAY` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:341)*
- `FC` → `CONDITION-TOKEN-VALUE`, `I-S-INFO` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:349)*
- `WS-TODAY-LILLIAN` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:363)*
- `WS-TODAY-SECONDS` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:364)*
- `WS-TODAY-GREGORIAN` → `WS-TODAY-G-YEAR`, `WS-TODAY-G-MONTH`, `WS-TODAY-G-DAY`, `WS-TODAY-G-HOURS`, `WS-TODAY-G-MINUTES`, `WS-TODAY-G-SECONDS`, `WS-TODAY-G-MILLISECONDS` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:365)*
- `CHRDATE` → `VSTRING-LENGTH`, `VSTRING-TEXT` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:374)*
- `PICSTR` → `VSTRING-LENGTH`, `VSTRING-TEXT` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:381)*
- `LILIAN` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:388)*
- `WS-CUSTOMER-AGE` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:390)*
- `CUSTOMER-CONTROL` → `CUSTOMER-CONTROL-RECORD` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:392)*
- `WS-UNSTR-TITLE` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:395)*
- `WS-FULL-NAME` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:396)*
- `WS-TITLE-VALID` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:397)*
- `DFHCOMMAREA` → `COMM-EYECATCHER`, `COMM-KEY`, `COMM-NAME`, `COMM-DOB`, `COMM-PHONE`, `COMM-ADDR`, `COMM-STATUS`, `COMM-CREATED-DATE`, `COMM-CREDIT-SCORE`, `COMM-CS-REVIEW-DATE`, `COMM-SUCCESS`, `COMM-FAIL-CODE` *(.bobz/expanded-single/cobol/src/base/cics/cobol/CRECUST.cbl:401)*