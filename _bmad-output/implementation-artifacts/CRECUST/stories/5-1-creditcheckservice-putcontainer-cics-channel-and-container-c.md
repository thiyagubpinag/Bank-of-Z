---
title: 'Story 5.1: CreditCheckService.putContainer() — CICS channel and container creation'
type: 'feature'
created: '2026-10-01'
status: 'draft'
route: 'dispatch'
review_loop_iteration: 0
context: ['_bmad-output/planning-artifacts/CRECUST/architecture.md', '_bmad-output/planning-artifacts/CRECUST/epics.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Before launching asynchronous child transactions OCR1–OCR5 for credit rating checks, the input commarea payload must be placed into dedicated CICS containers within a CICS channel.

**Approach:** Implement `CreditCheckService.putContainer()` using JCICS `Channel` and `Container` APIs (`Task.getTask().createChannel()` and `channel.createContainer().put()`) with named constants for channel (`CIPCREDCHANN`) and container names (`CIPA` through `CIPE`).

## Boundaries & Constraints

**Always:**
- Use official JCICS APIs under `com.ibm.cics.server.*` (`Task`, `Channel`, `Container`).
- Declare channel name `CIPCREDCHANN` and container names `CIPA`, `CIPB`, `CIPC`, `CIPD`, `CIPE` as `private static final String` constants (Rule 8 / NFR-2.4).
- Ensure `CreditCheckService` is stateless with respect to per-request state (NFR-5).
- Follow Rule 15: serialize commarea data via canonical serializer / byte conversion without inline manual packing.

**Never:**
- Never create custom wrapper/emulation classes for JCICS objects.
- Never use raw string literals for channel or container names at call sites.
- Never store request-specific commarea or container data in service instance fields.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Successful container put | Valid `CrecustCommarea` / commarea byte array, valid channel name, valid container name | CICS Channel is created or retrieved, container is created/populated with commarea bytes | Propagate/handle JCICS CicsConditionException appropriately |
| Null or invalid commarea | Null input to `putContainer` | Throws `IllegalArgumentException` or appropriate domain exception | Validation guard prevents CICS PUT |
| CICS error during createChannel / put | CICS environment failure / `CicsConditionException` | Handled or propagated according to CICS transformation rules | CicsConditionException handling |

</frozen-after-approval>

## Code Map

- `src/main/java/com/ibm/bankofz/service/CreditCheckService.java` -- Core credit check service class containing `putContainer()` and channel/container constants
- `src/main/java/com/ibm/bankofz/model/CrecustCommarea.java` -- Input commarea data structure
- `src/test/java/com/ibm/bankofz/service/CreditCheckServiceTest.java` -- Unit test class verifying channel creation and container payload population

## Tasks & Acceptance

**Execution:**
- [ ] `src/main/java/com/ibm/bankofz/service/CreditCheckService.java` -- Implement `putContainer(byte[] commareaBytes, String channelName, String containerName)` and declare constants `CHANNEL_NAME = "CIPCREDCHANN"`, `CONTAINER_CIPA = "CIPA"`, `CONTAINER_CIPB = "CIPB"`, `CONTAINER_CIPC = "CIPC"`, `CONTAINER_CIPD = "CIPD"`, `CONTAINER_CIPE = "CIPE"` -- Implements FR-5 and ADR-5 for container setup
- [ ] `src/test/java/com/ibm/bankofz/service/CreditCheckServiceTest.java` -- Create unit tests using mock JCICS Channel/Container/Task to verify `putContainer()` behavior and edge cases -- Ensures test coverage for container creation

**Acceptance Criteria:**
- Given a valid `CrecustCommarea` or byte array, when `creditCheckService.putContainer(commareaBytes, channelName, containerName)` is called, then the CICS channel and named container are created with the serialized commarea bytes placed inside.
- Given `CreditCheckService`, all channel and container names are declared as `private static final String` constants with no raw string literals at call sites.
- Given concurrent executions, `CreditCheckService` holds no per-request state in instance fields.

## Implementation Notes

### Dev Notes (2026-10-09)

**Implementation summary:**

- `putContainer(CrecustCommarea commArea, String channelName, String containerName)` implemented in [`CreditCheckService.java`](crecust-java/src/main/java/com/ibm/cics/botz/crecust/service/CreditCheckService.java).
- Serializes commarea via `CrecustareaSerializer.INSTANCE.toBytes(commArea)` (Rule 15 — no inline byte-packing).
- Creates channel via `Task.getTask().createChannel(channelName)`, creates container, and calls `container.put(commareaBytes)`.
- On `CicsConditionException` (covers `ChannelErrorException`, `ContainerErrorException`, `CCSIDErrorException`, `InvalidRequestException`): sets `commArea.commSuccess='N'`, `commArea.commFailCode='A'` — silent-return path (Rule 1 / PE-8 fail-code 'A').
- Named constants declared: `CIPCREDCHANN` (16-byte padded), `CIPA`–`CIPE`, `AGENCY_COUNT`, `OCR_TRANSIDS[]`, `CONTAINER_NAMES[]`, `DELAY_MILLIS`, `REVIEW_DATE_MAX_DAYS`.
- `performCreditCheck` skeleton loops putContainer for CIPA–CIPE, then stubs `runChildTransaction`, `fetchAny`, `computeReviewDate` (Stories 5-2/5-3/5-4).
- SLF4J `Logger`: `INFO` on `performCreditCheck` entry, `DEBUG` per container PUT, `ERROR` on CICS failure.
- `mvn compile` → **BUILD SUCCESS** (47 source files).

**Exception note:** `Container.put(byte[])` throws `ChannelErrorException`, `ContainerErrorException`, `InvalidRequestException`, and `CCSIDErrorException` — all subclass `CicsConditionException`. Catching the superclass covers all cases cleanly.

## Spec Change Log

## Review Triage Log

## Design Notes

Using standard JCICS Channel and Container pattern:
```java
Channel channel = Task.getTask().createChannel(channelName);
Container container = channel.createContainer(containerName);
container.put(commareaBytes);
```

## Verification

**Commands:**
- `mvn test -Dtest=CreditCheckServiceTest` -- expected: All unit tests for CreditCheckService pass
