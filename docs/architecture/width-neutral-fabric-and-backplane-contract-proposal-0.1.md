# fNx Width-Neutral Fabric and Backplane Contract Proposal 0.1

**Date:** 2026-11-06  
**Status:** Platform contract proposal; packet encoding, physical links, and routing algorithms are not selected

## 1. Purpose

This proposal defines the minimum observable contract for communication among fNx CPU units, memory endpoints, I/O endpoints, FCPs, and hierarchical backplanes.

A CPU unit has exactly one native width N. The fabric is independent of that width. It must support mixed-width systems without requiring endpoints to share register width, datapath width, pointer width, or physical link width.

This is a logical protocol contract. It does not select an electrical interface, packet bit layout, link speed, lane count, routing algorithm, or physical topology.

## 2. Design principles

1. **Width neutrality:** Transaction representation and semantics do not depend on the initiating CPU's N.
2. **Explicit length:** Every data transfer has an explicit byte length or an equivalent unambiguous transfer description.
3. **Byte-addressed memory:** Memory transactions use byte addresses, subject to the address-space proposal's final ratification.
4. **Defined completion:** A response distinguishes acceptance, completion, and failure.
5. **No invented atomicity:** Splitting or aggregating transactions cannot silently change atomicity.
6. **End-to-end protection:** Authority and access attributes survive routing and hierarchy traversal.
7. **Discoverable capabilities:** Endpoints and links report supported operations, limits, and ordering guarantees.
8. **Fault containment:** A failed endpoint or link must not silently corrupt unrelated transactions.
9. **Plane separation:** Normal data traffic, control operations, and telemetry have defined interfaces and failure behavior.

## 3. Endpoint model

An endpoint is a uniquely identified participant attached to the fabric. Endpoint classes include:
- fNx CPU unit.
- Memory controller or memory endpoint.
- I/O or device endpoint.
- FCP or management controller.
- Backplane bridge/router.
- Optional accelerator or specialized execution endpoint.

An endpoint identity must be stable within a defined platform scope and must not be confused with a local port or routing slot. Reconfiguration and hot removal require generation-safe identity or an equivalent stale-reference prevention rule.

Endpoint discovery reports:
- Endpoint identity and class.
- Native width, if the endpoint is an fNx CPU unit.
- Supported protocol revision.
- Address ranges and address-domain identifiers.
- Maximum transfer size and alignment constraints.
- Supported transaction types.
- Ordering and completion guarantees.
- Atomic operation support and scope.
- Protection/security attributes.
- Link and endpoint health.
- Reset and power-domain membership.

## 4. Transaction classes

The common protocol must represent at least:

| Class | Purpose |
|---|---|
| Read | Retrieve bytes from memory or a defined endpoint register |
| Write | Store bytes to memory or a defined endpoint register |
| Atomic | Execute a supported indivisible operation at a defined scope |
| Fence/order | Establish ordering and/or completion according to a defined scope |
| Message | Transfer a bounded message to an endpoint or channel |
| Control | Configure, start, stop, reset, or query an endpoint under authorization |
| Discovery | Enumerate endpoints, capabilities, and topology |
| Telemetry/event | Report health, status, errors, and asynchronous events |

The exact operation set and wire opcodes remain open. A protocol revision must not reinterpret an existing transaction type.

## 5. Transaction description

A transaction description must carry or unambiguously determine:
- Source endpoint identity.
- Destination endpoint or address-domain identity.
- Transaction type.
- Address, when applicable.
- Explicit byte length or defined operation width.
- Data or data reference.
- Ordering attributes.
- Protection/authority context.
- Transaction identity/tag for matching completion.
- Optional deadline/priority only if the protocol defines their semantics.
- Integrity information sufficient for the selected link/protocol layers.

The field widths and serialization are not selected. Do not derive them from N. A 16-bit CPU and a 1024-bit CPU must be able to issue the same abstract 64-byte read transaction.

## 6. Address domains and routing

The fabric must distinguish:
- CPU virtual addresses.
- Translated physical addresses.
- Endpoint-local register/address spaces.
- Message/channel identifiers.
- Management/configuration address spaces.

A CPU virtual address must not be forwarded as a physical address without the defined translation and protection process.

Routing is based on stable endpoint and address-domain information, not on assumptions about CPU native width. A hierarchical system may use local routing at each backplane level, but the end-to-end transaction identity and semantics must remain intact.

The standard must define:
- Global versus hierarchical endpoint naming.
- Address-domain ownership and translation boundaries.
- Route discovery and update.
- Loop prevention.
- Unreachable destination behavior.
- Route changes with outstanding transactions.
- Partial hierarchy failure and isolation.
- Duplicate endpoint detection.

## 7. Ordering and completion

The protocol must distinguish at least:
- Request accepted by the first link or endpoint.
- Request accepted by the final target.
- Operation completed at the target.
- Completion made visible to the requester under the selected memory model.
- Error or timeout reported.

A link-level acknowledgement is not necessarily architectural operation completion.

Ordering attributes must be defined in relation to the ISA memory model:
- Relaxed.
- Acquire/release or equivalent synchronization semantics.
- Strongly ordered/device operations.
- Explicit fence or completion requests.

The final contract must specify which transactions may pass one another, which ordering domains exist, and how ordering is preserved across bridges and backplanes.

A completion response must identify the original transaction and report success, failure, or an explicitly defined partial outcome. Duplicate requests must not cause duplicate side effects unless the transaction class explicitly permits it.

## 8. Atomic operations

Atomicity is a semantic guarantee at a defined scope, not a property inferred from packet size or link width.

For every supported atomic operation, the platform must define:
- Supported operand widths.
- Alignment and boundary restrictions.
- Atomicity scope.
- Serialization/linearization point.
- Ordering semantics.
- Failure and retry behavior.
- Progress guarantees.
- Interaction with caches and DMA.
- Whether an operation may cross a bridge or backplane boundary.

An endpoint or path that cannot provide the requested atomicity must reject the operation before reporting success. Splitting one atomic request into ordinary reads/writes is not an acceptable implementation.

The capability model must distinguish native endpoint support from fabric-wide support. A path supports an atomic operation only when every required component and the target can preserve its semantics.

## 9. Flow control and resource exhaustion

The protocol must prevent uncontrolled buffer overrun and define backpressure.

Required behavior:
- Credit, ready/valid, or equivalent flow-control semantics.
- Bounded buffering or explicit resource reservation.
- Defined behavior when request, response, or reassembly resources are exhausted.
- Fairness or arbitration guarantees where promised.
- Deadlock prevention across virtual channels or resource classes.
- Timeout and retry rules.
- Cancellation behavior, where supported.
- No silent transaction loss.

The exact mechanism is an implementation choice, but it must be possible to prove that cyclic resource dependencies cannot permanently block required traffic classes.

## 10. Errors, integrity, and recovery

The protocol must distinguish:
- Address/decode failure.
- Permission/protection failure.
- Unsupported transaction or capability.
- Endpoint unavailable.
- Link failure.
- Integrity/ECC failure.
- Timeout.
- Resource exhaustion.
- Reset/cancellation.
- Partial completion, only where explicitly supported.

Errors must preserve source and transaction identity and be attributable to a defined endpoint or path segment.

The standard must define retry safety. Reads may often be retried, but writes, atomics, messages, and device operations may have side effects. A retry must not duplicate an operation whose completion is uncertain.

Integrity detection and correction requirements must be specified at the relevant protocol/link layers. The architectural contract must define what error is reported when corruption cannot be corrected.

## 11. Mixed-width transfer semantics

Transfer size is independent of endpoint N.

- A 16-bit member may request a transfer longer than 2 bytes when the ISA/runtime and platform API permit it.
- A 1024-bit member may request a transfer smaller than 128 bytes.
- A CPU's native register width does not determine fabric packet width.
- A bridge may segment or aggregate payloads while preserving byte order, transaction identity, ordering, protection, and completion semantics.
- Segment boundaries must not create architectural atomicity.
- Data representation and endianness must be defined by the memory/object model, not inferred from the link.

The system must define how wider native values are serialized when crossing endpoints of different widths.

## 12. Hierarchical backplanes

A hierarchy consists of backplanes connected through bridges or fabric routers. Each level may have local endpoints and child backplanes.

Required properties:
- Globally unambiguous endpoint identity within the platform.
- Discovery and capability aggregation.
- Hierarchical routing and address-domain handling.
- Ordering preservation across bridges.
- Atomicity only where end-to-end supported.
- Fault containment and partial operation behavior.
- Independent reset/power domains with defined isolation.
- Link health and topology-change notification.
- No requirement that a parent controller share the native width of child CPUs.

A hierarchy must not imply one global shared-memory coherence domain. Coherence and visibility are separate contracts.

## 13. Control and management plane

The FCP and authorized host software use control transactions to discover and manage platform resources.

Control-plane operations must have:
- Authenticated authority and explicit privilege.
- Separate resource and ordering treatment from ordinary data traffic.
- Defined behavior when a managed endpoint is stopped or failed.
- Auditable results for reset, power, firmware, and topology changes.
- Protection against a data-plane requester forging management authority.

Telemetry may be lossy or coalesced only where the schema declares that behavior. Fault events that require recovery must have a reliable delivery or persistent-reporting mechanism.

The FCP is not placed in the normal CPU load/store path.

## 14. Conformance and verification

The fabric reference model and conformance suite must cover:
1. Mixed-width endpoints issuing identical abstract transaction types.
2. Explicit byte lengths and alignment validation.
3. Address-domain separation and translation boundaries.
4. Request acceptance versus target completion.
5. Ordering preservation across bridges.
6. Atomic capability negotiation and unsupported-atomic rejection.
7. Duplicate, delayed, reordered, and lost response scenarios.
8. Flow-control exhaustion and recovery.
9. Deadlock freedom for required traffic classes.
10. Endpoint removal and route changes with outstanding work.
11. Integrity error detection and attribution.
12. Reset and partial hierarchy failure.
13. Protection propagation across all hierarchy levels.
14. Retry safety for side-effecting transactions.
15. Discovery and capability consistency.

Formal protocol properties should cover transaction conservation, no duplicate side effects, response matching, ordering, and bounded-resource safety.

## 15. Decision status

| Topic | Status |
|---|---|
| Width-neutral abstract transaction contract | Required working direction |
| Explicit transfer length | Required working direction |
| Byte-addressed transaction model | Recommended; depends on address-model ratification |
| Endpoint identity and discovery | Required; encoding open |
| Transaction classes | Proposed |
| Request/acceptance/completion distinction | Required |
| Ordering model | Required; tied to ISA memory-model decision |
| Atomic operation scope | Required; details open |
| Flow control and deadlock prevention | Required; mechanism open |
| Error taxonomy and retry safety | Required; details open |
| Hierarchical routing | Required; mechanism open |
| Coherence | Separate contract; open |
| Control-plane authority | Required; security model open |
| Physical link, packet layout, and routing algorithm | Open |

## 16. Related specifications

- [fNx family and architectural contract](fnx-family-and-architectural-contract-0.1.md)
- [fNx address-space and memory-model proposal](address-space-and-memory-model-proposal-0.1.md)
- [fNx exception and interrupt model proposal](exception-and-interrupt-model-proposal-0.1.md)
- [fNx workspace and thread lifecycle contract proposal](workspace-and-thread-lifecycle-contract-proposal-0.1.md)
- [fNx logical data paths](data-paths-0.1.md)
- [fNx control processor and management plane](fnx-control-and-management-plane-0.1.md)
- [Family and backplane compatibility](family-and-backplane-compatibility-0.1.md)
- [fNx roadmap](../roadmap.md)
