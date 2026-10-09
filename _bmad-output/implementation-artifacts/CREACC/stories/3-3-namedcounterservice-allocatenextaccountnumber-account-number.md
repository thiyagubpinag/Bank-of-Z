---
status: review
route: oneshot
---

# Story 3.3 — NamedCounterService.allocateNextAccountNumber() — Account Number Allocation

## Story Information
- **Epic**: 3 (Business Logic)
- **Story Key**: `3-3-namedcounterservice-allocatenextaccountnumber-account-number`
- **Program**: `CREACC`
- **Target Runtime**: `cics_batch` / Java 21 LTS
- **Status**: `review`

## Implementation Details
- **Class**: `com.ibm.bankofz.creacc.service.NamedCounterService`
- **Stub – DAO Interface**: `com.ibm.bankofz.creacc.dao.ControlDao`
- **Stub – Service**: `com.ibm.bankofz.creacc.service.AbendNotificationService`
- **Stub – Exception**: `com.ibm.bankofz.creacc.exception.CreaccSystemException`
- **Test Class**: `com.ibm.bankofz.creacc.service.NamedCounterServiceTest`

### Key Highlights

- Implemented `NamedCounterService.allocateNextAccountNumber(String sortCode)` reproducing COBOL
  `FIND-NEXT-ACCOUNT_FNA010` (lines 476–858, `CREACC.cbl`) with architecture-mandated JCICS Named
  Counter primary path and DB2 CONTROL table fallback (ADR-07).
- **Primary path**: Constructs `com.ibm.cics.server.NamedCounter` with resource name
  `NCS_RESOURCE_BASE + sortCode` (`"BANKZACCT" + sortCode`), calls `get(1L)`. A successful call
  (no `CicsConditionException`) returns the allocated number directly.
- **Fallback path** (Named Counter throws `CicsConditionException`): delegates to
  `ControlDao.selectAccountLast(sortCode)` (SELECT from CONTROL table), increments by 1, calls
  `ControlDao.updateAccountLast(sortCode, newValue)` (UPDATE to CONTROL table).
- **Notifying-abort ordering** (Rule 1 / ADR-03): both DB2 fallback operations are notifying-abort
  paths. On failure: `AbendNotificationService.writeAbendRecord(HNCS, freeform, 0)` is called
  **before** re-throwing `CreaccSystemException`. ABEND code `HNCS` is a named constant in
  `CreaccSystemException.ABEND_CODE_HNCS`.
- **ENQ/DEQ locking** is the caller's responsibility — `NamedCounterService` does not acquire or
  release CICS ENQ locks.
- Created minimal stubs for `ControlDao` (interface, Story 4.1 scope),
  `AbendNotificationService` (Story 5.5 scope), and `CreaccSystemException` (Story 5.6 scope)
  sufficient to compile and test Story 3.3 in isolation.
- All three AC unit tests pass: Named Counter NORMAL path, DB2 fallback success path, and
  DB2 SELECT failure notifying-abort ordering path.

### Code Map

| File | Action | Notes |
|---|---|---|
| `creacc-java/src/main/java/com/ibm/bankofz/creacc/service/NamedCounterService.java` | Created | Primary implementation |
| `creacc-java/src/main/java/com/ibm/bankofz/creacc/dao/ControlDao.java` | Created | Stub interface (Story 4.1 scope) |
| `creacc-java/src/main/java/com/ibm/bankofz/creacc/service/AbendNotificationService.java` | Created | Stub service (Story 5.5 scope) |
| `creacc-java/src/main/java/com/ibm/bankofz/creacc/exception/CreaccSystemException.java` | Created | Stub exception class (Story 5.6 scope) |
| `creacc-java/src/test/java/com/ibm/bankofz/creacc/service/NamedCounterServiceTest.java` | Created | Three AC unit tests |
