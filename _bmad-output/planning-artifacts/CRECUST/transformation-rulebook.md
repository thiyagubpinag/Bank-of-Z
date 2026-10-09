# Transformation Rulebook: CRECUST

> Plain-language fidelity goals for the modernisation of `CRECUST` from Enterprise COBOL to Java.
> These goals are derived from the static-analysis findings in the technical research and contextualised
> to what this specific program does. They inform every story, acceptance criterion, and code-review
> checkpoint in the PRD.

---

## 1. Error-Path Ordering

`CRECUST` has exactly one critical failure path where the order of operations is non-negotiable:
when the `INSERT INTO PROCTRAN` statement fails, the program must call `ABNDPROC` (to record abend
information) **before** it issues `EXEC CICS ABEND ABCODE('HWPT')`. In COBOL, the `LINK` to
`ABNDPROC` happens first, and only after that link returns does the program abend. The Java
equivalent must mirror this order precisely: call `Program.link()` to `ABNDPROC` first, then throw
(or otherwise terminate) — never the other way around. Reversing the order would mean the abend
information is never written, which silently breaks operational diagnostics for anyone monitoring
CICS transaction failures.

All other error exits in this program are **silent-return paths**: they set `COMM-SUCCESS='N'` and
a fail code, then call `GET-ME-OUT-OF-HERE` (which issues `EXEC CICS RETURN`). The Java equivalents
for those paths must do exactly the same — set the fail-code fields and return — without adding any
logging, notification, or side effects that are not in the COBOL source.

---

## 2. Exception-Handler Grounding

Every exception-handler class generated for this program must map to a concrete construct in the
COBOL source. `CRECUST` does not use `EXEC CICS HANDLE CONDITION`; instead it inspects `WS-CICS-RESP`
and `WS-CICS-RESP2` inline after each CICS command, and checks `SQLCODE` after each DB2 statement.
Each distinct EVALUATE or IF block that tests these response codes is the legitimate origin of a
handler. A handler generated solely because a Java framework convention "expects" one — with no
corresponding COBOL response-code check behind it — must not exist. If a catch block's only action
is to re-throw the exception without performing any operation that corresponds to something in the
COBOL source, that catch block is indirection without meaning and must be collapsed.

---

## 3. Byte-Array Contract Integrity

`CRECUST` has one external byte-array interface: the 399-byte `DFHCOMMAREA` defined in
`CRECUST.cpy`. This commarea is the contract between CRECUST and every program that calls it.
Exactly one Java class must own the serialisation of this layout — no other class may re-implement
the field-offset logic inline. All field offsets and lengths must be declared as named constants
derived from the PIC clause widths in the commarea table (PE-1 in the technical research); bare
integer literals for byte positions are prohibited. The same rule applies to the 399-byte
`WS-CHILD-DATA` structure passed to the OCR1–OCR5 child transactions via CICS containers: one
canonical class per layout, no duplicate field-width definitions across classes.

---

## 4. SQL Completeness

The program issues four SQL statements. Every column listed in the technical research must appear
in the corresponding Java SQL string, in the same logical order:

- `INSERT INTO CUSTOMER` — all 17 columns must be present, including all three INTEGER-encoded date
  columns (`CUSTOMER_DATE_OF_BIRTH`, `CUSTOMER_CREATED_DATE`, `CUSTOMER_CS_REVIEW_DATE`), each
  encoded as `(YYYY * 10000) + (MM * 100) + DD`.
- `INSERT INTO PROCTRAN` — all 9 columns must be present; `PROCTRAN_DATE` receives a `DD.MM.YYYY`
  formatted string (not a `java.sql.Date`); `PROCTRAN_AMOUNT` must be `ZEROS` (not null).
- `SELECT CONTROL_VALUE_NUM FROM CONTROL WHERE CONTROL_NAME = ?` — single column, exact predicate.
- `UPDATE CONTROL SET CONTROL_VALUE_NUM = ? WHERE CONTROL_NAME = ?` — exact predicate.

Silently dropping or reordering a column is a defect. If a column is added or removed for a
technical reason, the change must be documented with an explicit comment in the code.

---

## 5. Cross-Program Data-Structure Scope

Several data structures defined in `CRECUST` are consumed by other programs in the Bank-of-Z suite,
not only by this program's own procedure division. All of the following must be transformed to Java
classes, even if a given structure is not fully exercised by every code path in the Java equivalent:

- **`DFHCOMMAREA` / `Crecust.cpy` layout** (399 bytes) — the public API of this program; consumed
  by every program that calls CRECUST.
- **`CUSTOMER-RECORD` / `CUSTOMER.cpy` layout** (397 bytes) — consumed by any program reading or
  writing the CUSTOMER DB2 table through this copybook.
- **`PROCTRAN-AREA` / `PROCTRAN.cpy` layout** — consumed by all programs that write audit records
  to the PROCTRAN table, including the five OCR child transactions.
- **`WS-CHILD-DATA` / `WS-CHILD-CUSTOMER-RECORD` layout** (399 bytes) — the container payload
  exchanged between CRECUST and the OCR1–OCR5 child transactions; those transactions produce this
  layout and CRECUST reads it back.

The PRD scope, architecture class list, and generated stories must include all four structures.
Classifying any of them as "not used locally" and excluding them is a scoping defect.

---

## 6. Serializer Delegation

Because this modernisation retains z/OS CICS infrastructure (the program continues to run under
JCICS and exchange commareas with callers), serialisation of byte-array layouts is required for the
commarea and container data. The canonical serializer for each layout is the single authoritative
implementation of that layout's byte-packing. Any service class or business-logic class that needs
to read or write one of those layouts must delegate to the canonical serializer — it must never
re-implement the field writes inline. This rule exists to ensure that a future change to a field
width (for example, extending a name field) requires a change in exactly one place. Parallel
implementations of the same byte layout are a maintenance trap and are prohibited regardless of
whether they produce identical offsets today.
