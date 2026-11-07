# fNx M1 Requirements Traceability Matrix 0.1

**Date:** 2026-11-07  
**Status:** Working traceability baseline; identifiers are stable within this document, but requirements remain proposals until their source contracts are ratified.

## 1. Purpose and rules

This matrix connects architectural obligations to their current proposal sections and the evidence needed to verify them. It is not a substitute for normative specification text and does not promote a recommendation into a ratified requirement.

Requirement identifiers use these domains:

- `ISA`: instruction set, opcode namespace, and encoding.
- `CTX`: workspace, thread, and execution-context lifecycle.
- `MEM`: address translation, memory, ordering, and atomics.
- `EXC`: exceptions, traps, interrupts, and reset.
- `FAB`: fabric, endpoints, and backplanes.
- `CTRL`: FCP and platform management authority.
- `CONF`: conformance profiles, evidence, and verification process.

A requirement's **proposal** status means the behavior is identified as necessary or recommended but has not been ratified. **Open** means a normative choice is still required. **Test pending** means the proposal identifies evidence, but no executable test is yet claimed.

### Status vocabulary

| Status | Meaning |
|---|---|
| Proposal | Contract direction exists; not ratified |
| Open | Architectural choice or exact semantics remain unresolved |
| Test pending | Required verification has not yet been implemented |
| Research | Evidence or quantitative evaluation is still needed |

No row in this initial matrix should be read as proof of implementation or conformance.

## 2. ISA and opcode requirements

| ID | Requirement / obligation | Source | Verification evidence | Status |
|---|---|---|---|---|
| ISA-001 | Each fNx member uses an N-bit opcode field; no implicit fixed-width exception is introduced. | opcode namespace §2; encoding feasibility §2 | Decoder boundary vectors for each supported N; reject truncated encodings | Proposal |
| ISA-002 | A numeric opcode identity has one global semantic meaning wherever directly representable. | opcode namespace §§2–4 | Registry uniqueness and cross-member semantic equivalence tests | Proposal |
| ISA-003 | An implementation must not truncate, alias, or silently reinterpret an opcode outside its N-bit representable range. | opcode namespace §§3, 6 | Negative assembler, decoder, and runtime tests at N-bit boundaries | Proposal |
| ISA-004 | Unassigned, assigned-but-unsupported, malformed, and privilege-prohibited instructions remain distinguishable. | opcode namespace §6; exception model §3.3 | Golden vectors asserting distinct architectural outcomes | Open |
| ISA-005 | Instruction boundaries, operand schemas, immediate/displacement encoding, and branch reach are unambiguous. | instruction encoding feasibility §§3–7 | Encode/decode round trips and boundary corpus across N | Open |
| ISA-006 | Width-dependent standardized operations declare direct-representation limits and any exact expansion behavior. | opcode namespace §§4–8; conformance strategy §5.2 | Registry validation plus differential expansion tests | Open |
| ISA-007 | Opcode assignments are globally unique, immutable once standardized, and never recycled. | opcode namespace §§5–7 | Registry schema and lifecycle-transition validator | Proposal |

## 3. Workspace and execution-context requirements

| ID | Requirement / obligation | Source | Verification evidence | Status |
|---|---|---|---|---|
| CTX-001 | Workspace, hardware thread, and address space are distinct architectural entities; workspace is not inherently an OS process or address space. | workspace/thread §2 | Reference-model entity and ownership tests | Proposal |
| CTX-002 | A thread belongs to at most one workspace at a time under the proposed baseline. | workspace/thread §§2, 5 | Membership transition and adversarial concurrency tests | Proposal |
| CTX-003 | Reused workspace/thread identities cannot make stale references valid; generation-safe identity is required. | workspace/thread §3 | Stale-handle and generation-wrap boundary tests | Proposal |
| CTX-004 | Private architectural state is owned by one thread; shared workspace state has explicit ownership and access rules. | workspace/thread §4 | Isolation tests and state-ownership assertions | Proposal |
| CTX-005 | Shared-register accesses have a specified indivisible visibility unit and serialization rule; multi-register atomicity is never implied. | workspace/thread §6 | Concurrent read/write litmus tests; multi-register negative tests | Open |
| CTX-006 | Workspace and thread lifecycle transitions, admission failures, and teardown behavior are defined. | workspace/thread §§7–8 | State-machine transition coverage and resource-exhaustion tests | Open |
| CTX-007 | Eviction, restoration, and migration preserve the architecturally visible context and authorization state. | workspace/thread §9 | Save/restore differential tests and migration fault injection | Proposal |
| CTX-008 | Resource limits and capabilities are discoverable and distinguish architectural namespace from implementation limits and runtime availability. | workspace/thread §10; conformance strategy §3 | Profile-schema validation and runtime resource-change tests | Proposal |
| CTX-009 | Teardown and owner termination define the disposition of outstanding operations and shared synchronization state. | workspace/thread §§8, 12 | Teardown race tests with in-flight memory and fabric operations | Open |

## 4. Address and memory requirements

| ID | Requirement / obligation | Source | Verification evidence | Status |
|---|---|---|---|---|
| MEM-001 | Virtual addresses and architectural pointers are N bits; implemented physical-address width may be narrower than N. | address/memory §§1–3 | Address-width and physical-limit boundary tests | Proposal |
| MEM-002 | Virtual-to-physical translation never silently truncates an address; unrepresentable targets fault according to a defined rule. | address/memory §3 | Translation boundary and fault-attribution vectors | Proposal |
| MEM-003 | Address arithmetic overflow, alignment, unaligned accesses, and instruction-fetch address behavior are explicit. | address/memory §2 | Boundary tests for each addressing operation and access size | Open |
| MEM-004 | Translation and protection are scoped to an identified address space and access type. | address/memory §§3–4 | Cross-address-space isolation and permission tests | Proposal |
| MEM-005 | Mapping changes, identifier reuse, and local/remote invalidation have defined ordering and completion semantics. | address/memory §3.4 | Multi-agent invalidation litmus tests and stale-translation stress | Open |
| MEM-006 | Split or multi-part accesses define partial-completion behavior; failed stores cannot leave undocumented partial effects. | address/memory §4 | Fault injection at each split-transfer boundary | Open |
| MEM-007 | Memory attributes and device/I/O behavior are explicit and cannot silently degrade into ordinary cacheable memory. | address/memory §5 | Attribute conformance and device-ordering tests | Open |
| MEM-008 | The memory model defines permitted observations across threads, workspaces, CPU widths, DMA, shared registers, and I/O. | address/memory §6 | Published executable litmus suite with golden outcomes | Open |
| MEM-009 | Atomicity, ordering, supported widths, alignment, scope, and progress are separate declared properties. | address/memory §7 | Atomic capability matrix and concurrency tests | Open |
| MEM-010 | Coherence or explicit ownership/maintenance rules define CPU, DMA, and device visibility. | address/memory §8 | CPU-to-CPU and CPU-to-DMA visibility tests | Open |
| MEM-011 | Mixed-width members share a defined byte representation and explicit transfer lengths; native width does not imply remote atomic width. | address/memory §9; fabric §11 | Cross-width representation and unsupported-atomic tests | Open |

## 5. Exception, interrupt, and reset requirements

| ID | Requirement / obligation | Source | Verification evidence | Status |
|---|---|---|---|---|
| EXC-001 | Synchronous exceptions, software traps, asynchronous interrupts, non-maskable events, reset, and debug events are distinct event classes. | exception model §2 | Event-class golden vectors | Proposal |
| EXC-002 | Synchronous faults identify the faulting instruction, relevant address/access, and architecturally visible state. | exception model §3.1 | Precise-fault state comparison | Proposal |
| EXC-003 | Restart, nonrestart, and partial-commit behavior are defined per exception class. | exception model §3.2 | Restartability and side-effect boundary tests | Open |
| EXC-004 | Software trap entry/return and authority checks are specified independently of a particular vector-table implementation. | exception model §4 | Trap ABI vectors and illegal-return tests | Open |
| EXC-005 | Interrupt target selection, pending retention, masking, priority, nesting, and acknowledgement are defined. | exception model §5 | Interrupt-controller model and adversarial delivery tests | Open |
| EXC-006 | Interrupts directed to blocked or nonresident contexts are retained and delivered without corrupting context state. | exception model §7; workspace/thread §§9, 12 | Eviction/restore interrupt stress tests | Open |
| EXC-007 | Interrupt and reset behavior accounts for outstanding memory/fabric transactions and prevents duplicate side effects. | exception model §§5, 8; fabric §§7, 10 | In-flight transaction interruption and reset fault injection | Open |
| EXC-008 | Reset scope is explicit and does not implicitly reset unrelated CPU units or backplanes. | exception model §8; fabric §12 | Partial-reset hierarchy tests | Open |
| EXC-009 | Debug inspection and modification require explicit authorization and preserve defined architectural state. | exception model §§6–9; workspace/thread §5 | Unauthorized debug negative tests | Open |

## 6. Fabric and backplane requirements

| ID | Requirement / obligation | Source | Verification evidence | Status |
|---|---|---|---|---|
| FAB-001 | Fabric transactions are width-neutral and independent of any CPU's native pointer or register width. | fabric §§1–3 | Mixed-width endpoint reference-model tests | Proposal |
| FAB-002 | Transactions identify endpoints, address domains, class, transfer length, and relevant attributes explicitly. | fabric §§3–6 | Schema validation and malformed-transaction tests | Open |
| FAB-003 | Request acceptance, target acceptance, completion, and visibility are distinct observable events. | fabric §7 | Delayed/lost/reordered response tests | Proposal |
| FAB-004 | Ordering requirements survive bridges and hierarchical backplanes. | fabric §§7, 12 | Cross-bridge ordering litmus tests | Open |
| FAB-005 | Atomic requests require explicit capability and scope support; unsupported atomics are rejected rather than weakened. | fabric §8; address/memory §7 | Capability negotiation and negative tests | Open |
| FAB-006 | Flow control and resource exhaustion cannot create prohibited deadlock or silent transaction loss. | fabric §9 | Bounded-resource stress and formal deadlock properties | Open |
| FAB-007 | Errors are attributable; retry behavior is safe for side-effecting operations and cannot duplicate effects. | fabric §10 | Integrity, timeout, retry, and duplicate-side-effect injection | Open |
| FAB-008 | Endpoint discovery, route changes, endpoint removal, and partial hierarchy failure have defined behavior. | fabric §§3, 12 | Topology mutation and outstanding-transaction tests | Open |
| FAB-009 | Protection and relevant memory attributes are preserved across all fabric hierarchy levels. | fabric §§6, 10, 13 | Adversarial bridge/endpoint authorization tests | Open |
| FAB-010 | Physical link, packet layout, and routing mechanisms remain implementation choices unless separately standardized. | fabric §§2, 15 | Specification review; implementation profile disclosure | Proposal |

## 7. FCP and control-plane requirements

| ID | Requirement / obligation | Source | Verification evidence | Status |
|---|---|---|---|---|
| CTRL-001 | FCP is a control-plane component, not a mandatory serial hop for normal application instruction execution. | FCP §§1–3 | Architecture review and data-path model | Proposal |
| CTRL-002 | FCP capability reporting reflects actual hardware/platform capabilities and does not manufacture unavailable resources. | FCP §5 | Discovery consistency tests against endpoint profiles | Proposal |
| CTRL-003 | Host software and FCP authority, command ownership, and arbitration are explicit. | FCP §§4, 8–9; fabric §13 | Conflicting-command and unauthorized-command tests | Open |
| CTRL-004 | FCP failure, main-CPU failure, reset, recovery, and partial hierarchy failure have defined observable reporting. | FCP §§4, 7, 9 | Fault-injection matrix across power/reset domains | Open |
| CTRL-005 | Management operations are authenticated, auditable, and cannot bypass CPU privilege, memory protection, or device access controls. | FCP §8 | Threat-model review and privilege-bypass negative tests | Open |
| CTRL-006 | FCP firmware trust, update, rollback, and recovery authority are specified before claiming a secure management plane. | FCP §§4, 8–9 | Secure-boot/update protocol tests and recovery tests | Open |

## 8. Conformance and evidence requirements

| ID | Requirement / obligation | Source | Verification evidence | Status |
|---|---|---|---|---|
| CONF-001 | Conformance is reported by domain and exact member width, revision, and capability profile; a single vague compatibility label is insufficient. | conformance strategy §§2–3, 9 | Profile validator and report-schema tests | Proposal |
| CONF-002 | Every normative requirement maps to one or more positive tests, negative tests where applicable, expected outcomes, and evidence status. | conformance strategy §4 | Matrix-to-test manifest consistency check | Test pending |
| CONF-003 | Reference models encode architectural semantics, not implementation-specific scheduler or datapath assumptions. | conformance strategy §5 | Differential tests against independent implementations | Proposal |
| CONF-004 | Registry, assembler, and disassembler share one authoritative schema and reject malformed or unrepresentable encodings. | conformance strategy §5.2–5.3 | Registry validation and round-trip corpus | Open |
| CONF-005 | Golden vectors, memory litmus tests, and fabric tests publish permitted outcomes and reproducible evidence. | conformance strategy §§5.4, 10 | Versioned test artifacts and reproducible CI runs | Test pending |
| CONF-006 | Cross-width tests exercise at least two distinct N values and explicit capability negotiation. | conformance strategy §7 | Mixed-width reference-model test suite | Test pending |
| CONF-007 | Security conformance covers isolation, stale identities, debug authority, DMA, management forgery, and teardown races. | conformance strategy §8 | Adversarial security test suite | Test pending |
| CONF-008 | Simulation-only results do not claim hardware conformance; failed and skipped tests remain visible. | conformance strategy §10 | CI evidence policy and report validation | Proposal |

## 9. Cross-contract invariants requiring explicit reconciliation

These invariants span multiple proposals and must be resolved once, not independently redefined in each document.

1. **Atomicity boundary:** workspace shared-register visibility (CTX-005), memory atomic scope (MEM-009), and fabric atomic transactions (FAB-005) must use compatible scopes and completion semantics. A shared register must not be described as atomic at system scope unless the underlying implementation contract guarantees that scope.
2. **Completion versus visibility:** fabric completion (FAB-003) must be reconciled with memory ordering and visibility (MEM-008, MEM-010). A completed transport response is not automatically a globally visible store unless the memory model says so.
3. **Interrupts and in-flight work:** exception delivery (EXC-006, EXC-007), context teardown (CTX-009), and fabric retry/duplicate suppression (FAB-007) must agree on ownership of outstanding operations.
4. **Authority separation:** CPU privilege, workspace protection, debug authority, host OS authority, and FCP control authority (EXC-009, CTRL-003, CTRL-005) must form one explicit authority model. FCP control privileges must not silently grant application-memory access.
5. **Address and fabric domains:** N-bit virtual addresses (MEM-001), physical address width (MEM-002), DMA protection (MEM-010), and fabric address domains (FAB-002, FAB-009) must be connected by explicit translation and authorization rules.
6. **Identity lifecycle:** generation-safe workspace/thread identities (CTX-003), address-space identifiers (MEM-005), endpoint identities (FAB-008), and management inventory identifiers must define reuse and stale-reference behavior consistently.
7. **Reset and teardown:** reset scope (EXC-008), workspace/thread teardown (CTX-006, CTX-009), and partial fabric hierarchy failure (FAB-008) must define which operations are drained, aborted, retried, or reported as indeterminate.

## 10. Recommended implementation order

1. Ratify vocabulary and entity boundaries: CPU unit, hardware thread, workspace, address space, endpoint, backplane, FCP.
2. Resolve cross-contract invariants 1–7 above, starting with atomicity/completion and authority.
3. Select address representation, byte order, alignment, and baseline memory consistency rules.
4. Specify exception entry/return, pending interrupt retention, and reset scopes.
5. Specify endpoint identity, transaction completion, retry safety, and hierarchy behavior.
6. Freeze the requirement IDs and add machine-readable test-manifest metadata.
7. Implement a small reference model and deterministic tests before assigning opcode values or claiming conformance.

## 11. Source documents

- [Opcode namespace and allocation proposal](opcode-namespace-and-allocation-proposal-0.1.md)
- [Instruction encoding feasibility study](instruction-encoding-feasibility-0.1.md)
- [Address-space and memory-model proposal](address-space-and-memory-model-proposal-0.1.md)
- [Exception, trap, and interrupt model proposal](exception-and-interrupt-model-proposal-0.1.md)
- [Workspace and thread lifecycle contract proposal](workspace-and-thread-lifecycle-contract-proposal-0.1.md)
- [Width-neutral fabric and backplane contract proposal](width-neutral-fabric-and-backplane-contract-proposal-0.1.md)
- [FCP control processor and management plane](fnx-control-and-management-plane-0.1.md)
- [Conformance and reference-model strategy](conformance-and-reference-model-strategy-0.1.md)
- [fNx roadmap](../roadmap.md)
