## TS Metadata (static analysis output)

**Source language**: cobol
**Middleware**: CICS

### Paragraphs (10 total)

| # | Name | Lines | Calls | In (inputs) | Out (outputs) |
|---|------|-------|-------|-------------|---------------|
| 1 | `PREMIERE_A010` | 178–328 | `SEND-TERMINATION-MSG_STM010`, `POPULATE-TIME-DATE_PTD010`, `ABEND-THIS-TASK_ATT010`, `PROCESS-MAP_PM010`, `SEND-MAP_SM010`, `SEND-MAP_SM010` | — | `WS-FAIL-INFO`, `WS-CICS-FAIL-MSG`, `WS-CICS-RESP-DISP`, `WS-CICS-RESP2-DISP` |
| 3 | `PROCESS-MAP_PM010` | 335–359 | `RECEIVE-MAP_RM010`, `EDIT-DATA_ED010`, `SEND-MAP_SM010`, `UPD-CRED-DATA_UCD010` | — | — |
| 5 | `RECEIVE-MAP_RM010` | 366–435 | `POPULATE-TIME-DATE_PTD010`, `ABEND-THIS-TASK_ATT010` | — | `WS-FAIL-INFO`, `WS-CICS-FAIL-MSG`, `WS-CICS-RESP-DISP`, `WS-CICS-RESP2-DISP` |
| 7 | `EDIT-DATA_ED010` | 442–474 | `VALIDATE-AMOUNT_VA010` | — | — |
| 9 | `UPD-CRED-DATA_UCD010` | 481–637 | `POPULATE-TIME-DATE_PTD010`, `ABEND-THIS-TASK_ATT010` | `WS-AMOUNT-AS-FLOAT` | `WS-FAIL-INFO`, `WS-CICS-FAIL-MSG`, `WS-CICS-RESP-DISP`, `WS-CICS-RESP2-DISP` |
| 11 | `SEND-MAP_SM010` | 644–868 | `POPULATE-TIME-DATE_PTD010`, `ABEND-THIS-TASK_ATT010`, `POPULATE-TIME-DATE_PTD010`, `ABEND-THIS-TASK_ATT010`, `POPULATE-TIME-DATE_PTD010`, `ABEND-THIS-TASK_ATT010` | — | `WS-FAIL-INFO`, `WS-CICS-FAIL-MSG`, `WS-CICS-RESP-DISP`, `WS-CICS-RESP2-DISP` |
| 13 | `SEND-TERMINATION-MSG_STM010` | 875–944 | `POPULATE-TIME-DATE_PTD010`, `ABEND-THIS-TASK_ATT010` | — | `WS-FAIL-INFO`, `WS-CICS-FAIL-MSG`, `WS-CICS-RESP-DISP`, `WS-CICS-RESP2-DISP` |
| 15 | `ABEND-THIS-TASK_ATT010` | 951–956 | — | `WS-FAIL-INFO`, `WS-CICS-FAIL-MSG`, `WS-CICS-RESP-DISP`, `WS-CICS-RESP2-DISP` | — |
| 17 | `VALIDATE-AMOUNT_VA010` | 963–1148 | — | — | — |
| 19 | `POPULATE-TIME-DATE_PTD010` | 1155–1166 | — | — | — |

### Variable hierarchy (41 top-level of 121 total)

- `WS-CICS-WORK-AREA` → `WS-CICS-RESP`, `WS-CICS-RESP2` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:30)*
- `WS-FAIL-INFO` → `FILLER #1`, `WS-CICS-FAIL-MSG`, `FILLER #2`, `WS-CICS-RESP-DISP`, `FILLER #3`, `WS-CICS-RESP2-DISP`, `FILLER #4` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:35)*
- `SWITCHES` → `VALID-DATA-SW` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:44)*
- `FLAGS` → `SEND-FLAG` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:48)*
- `ACTION-ALPHA` → `ACTION-NUM` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:54)*
- `END-OF-SESSION-MESSAGE` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:60)*
- `RESPONSE-CODE` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:62)*
- `COMMUNICATION-AREA` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:64)*
- `AMTI9` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:70)*
- `WS-AMOUNT-AS-FLOAT` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:72)*
- `WS-NUM-COUNT-TOTAL` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:73)*
- `WS-NUM-COUNT-POINT` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:74)*
- `WS-NUM-COUNT-SPACE` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:75)*
- `WS-AMOUNT-UNSTR` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:77)*
- `WS-AMOUNT-UNSTR-L` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:78)*
- `WS-AMOUNT-UNSTR-REVERSE` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:79)*
- `WS-STUFF1` → `WS-COMM-ACT-BAL-UNSIGN` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:81)*
- `WS-STUFF2` REDEFINES `WS-STUFF1` → `WS-COMM-ACT-BAL-X` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:83)*
- `WS-STUFF3` → `WS-COMM-AMT-UNSIGN` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:86)*
- `WS-STUFF4` REDEFINES `WS-STUFF3` → `WS-COMM-AMT-X` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:88)*
- `WS-STUFF5` → `WS-COMM-AV-BAL-UNSIGN` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:91)*
- `WS-STUFF6` REDEFINES `WS-STUFF5` → `WS-COMM-AV-BAL-X` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:93)*
- `WS-COMM-AREA` → `WS-COMM-ACCNO`, `WS-COMM-SIGN`, `WS-COMM-AMT` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:96)*
- `WS-CONVERSIONA` → `WS-CONVERT-PIC1`, `WS-CONVERT-PIC1SP` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:101)*
- `WS-CONVERSIONB` → `WS-CONVERT-PICX`, `WS-CONVERT-SPLIT` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:108)*
- `WS-CONVERTED-VAL1` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:115)*
- `WS-CONVERTED-VAL2` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:116)*
- `WS-CONVERTED-VAL3` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:117)*
- `WS-CONVERTED-VAL4` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:118)*
- `SUBPGM-PARMS` → `SUBPGM-ACCNO`, `SUBPGM-AMT`, `SUBPGM-SORTC`, `SUBPGM-AV-BAL`, `SUBPGM-ACT-BAL`, `SUBPGM-ORIGIN`, `SUBPGM-SUCCESS`, `SUBPGM-FAIL-CODE` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:120)*
- `COMPANY-NAME-FULL` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:136)*
- `AVAILABLE-BALANCE-DISPLAY` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:138)*
- `ACTUAL-BALANCE-DISPLAY` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:139)*
- `WS-U-TIME` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:141)*
- `WS-ORIG-DATE` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:142)*
- `WS-ORIG-DATE-GRP` REDEFINES `WS-ORIG-DATE` → `WS-ORIG-DATE-DD`, `FILLER #6`, `WS-ORIG-DATE-MM`, `FILLER #7`, `WS-ORIG-DATE-YYYY` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:143)*
- `WS-ORIG-DATE-GRP-X` → `WS-ORIG-DATE-DD-X`, `FILLER #8`, `WS-ORIG-DATE-MM-X`, `FILLER #9`, `WS-ORIG-DATE-YYYY-X` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:150)*
- `WS-TIME-DATA` → `WS-TIME-NOW`, `WS-TIME-NOW-GRP` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:157)*
- `WS-ABEND-PGM` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:164)*
- `ABNDINFO-REC` → `ABND-VSAM-KEY`, `ABND-APPLID`, `ABND-TRANID`, `ABND-DATE`, `ABND-TIME`, `ABND-CODE`, `ABND-PROGRAM`, `ABND-RESPCODE`, `ABND-RESP2CODE`, `ABND-SQLCODE`, `ABND-FREEFORM` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:166)*
- `DFHCOMMAREA` → `COMM-ACCNO`, `COMM-SIGN`, `COMM-AMT` *(.bobz/expanded-single/cobol/src/base/cics/cobol/BNK1CRA.cbl:170)*