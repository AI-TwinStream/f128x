# fNx Address-Space and Memory-Model Proposal 0.1

**Date:** 2026-11-06  
**Status:** Architectural proposal; translation mechanism and memory consistency model are not ratified

## 1. Purpose and scope

This proposal collects the address-space, translation, protection, and memory-ordering contracts required by the fNx family.

For a member with native width N:

- General-purpose registers and native arithmetic are N bits.
- Pointers and virtual addresses are N bits.
- The physical address width P is implementation-defined and may be smaller than N.
- Physical capacity is independent of the size of the virtual address space.
- Fabric transaction representation is width-neutral and must not inherit a CPU's native pointer width.

The proposal separates architectural guarantees from page-table formats, TLB structures, cache implementations, and physical interconnect choices.

## 2. Addressing model

### 2.1 Candidate: byte-addressed virtual memory

**Recommendation:** Use byte addresses throughout the architectural virtual address space.

Under this model, each N-bit virtual address identifies one byte. Access width is specified separately by the instruction. This avoids making address arithmetic depend on a CPU's preferred scalar access size and supports mixed-width members sharing memory.

This is a recommendation for ratification, not yet a frozen rule. A word-addressed model is possible, but would complicate interoperability, byte-granular data structures, and device interfaces.

### 2.2 N-bit virtual addresses

A virtual address is an N-bit unsigned architectural value. The architecture must define:

- Address-space size and whether the entire 2^N-byte range is potentially valid.
- Address arithmetic and overflow behavior.
- Alignment requirements for each access size.
- Unaligned access behavior.
- Instruction fetch address semantics.
- Address-space identity and isolation.
- Memory-mapped I/O treatment.
- Reserved and implementation-defined virtual regions, if any.

Do not silently assume that a virtual address is a signed integer or that a fixed high-bit canonicality rule applies across all N values.

### 2.3 Address arithmetic

Address calculation must be distinguished from ordinary integer arithmetic.

The architecture must define whether effective-address overflow:
- Wraps modulo 2^N.
- Raises an address-generation exception.
- Is detected only when the resulting address is translated.
- Depends on the specific addressing operation.

**Recommendation:** Define an explicit architectural rule for each address-generation operation and prohibit implementation-dependent behavior. Do not infer pointer overflow semantics from the general integer ADD instruction.

A translation or protection fault must identify the effective virtual address and access type. The architecture must also define whether a faulting operation can have partial effects.

## 3. Virtual-to-physical translation

### 3.1 Width and capacity

Let N be the member's virtual-address width and P be the platform's implemented physical-address width.

- P may be less than N.
- A physical address must not be silently truncated from an N-bit virtual address.
- Translation must either produce a valid implemented physical address or raise a defined fault.
- Physical-address width, physical memory capacity, and mapped device ranges must be discoverable.
- A platform may expose sparse physical address ranges.

The architectural virtual-address width remains N even when the installed platform has much less physical memory.

### 3.2 Translation contract

The architecture must specify the observable behavior of translation without requiring one particular implementation structure.

Required properties:

1. Translation is performed in the context of an identified address space.
2. Permissions are checked for the requested access type.
3. Translation failures produce a defined architectural exception.
4. A translation result cannot grant permissions absent from the governing mapping.
5. Changes to mappings have defined visibility and invalidation semantics.
6. Translation state is isolated across protection domains.
7. Translation faults and access faults are distinguishable where software needs different recovery behavior.
8. DMA and other non-CPU agents use an explicitly defined translation/protection domain.

Page tables, radix trees, inverted tables, segmentation, TLB organization, and hardware/software refill are implementation or ABI choices unless later made architectural.

### 3.3 Mapping granularity

The standard must define:
- Supported mapping sizes or a discovery mechanism for them.
- Minimum protection granularity.
- Alignment requirements for mapping bases and lengths.
- Behavior for overlapping or conflicting mappings.
- Permissions and attributes associated with a mapping.
- Rules for mapping memory versus device/I/O regions.

A single fixed page size should not be assumed before the smallest and largest N members are evaluated.

### 3.4 Address-space identifiers and invalidation

The architecture must define how translations are associated with address spaces and how stale translations are invalidated.

At minimum, specify:
- Address-space identifier semantics and reuse rules.
- Local and remote translation invalidation.
- Ordering between page-table writes and invalidation requests.
- Completion acknowledgement and when a mapping change becomes globally effective.
- Context switch behavior.
- Translation invalidation during migration and teardown.
- Protection against stale identifiers after reuse.

An implementation may use ASIDs, PCIDs, tagged TLBs, or another mechanism, but observable behavior must be equivalent to the contract.

## 4. Protection and access faults

Memory access authorization must distinguish at least:

- Unmapped virtual address.
- Read, write, or execute permission violation.
- Privilege violation.
- Invalid or reserved address encoding, if any.
- Physical target absent or inaccessible.
- Device access violation.
- Alignment fault, if the access is not supported.
- Translation-structure or page-walk failure.

The exception contract must identify the faulting instruction, virtual address, access type, privilege context, and whether the instruction can be restarted.

The architecture must define whether a multi-part or split access can partially complete. A fault must not leave an undocumented partial write.

## 5. Memory regions and attributes

The architecture must define memory attributes independently of physical cache topology.

Candidate attributes include:
- Normal cacheable memory.
- Non-cacheable memory.
- Device/I/O memory.
- Write-combining or streaming memory, if supported.
- Persistent memory, if supported.
- Shared versus private visibility domains.
- Security or confidentiality attributes, if required.

The semantics of each attribute must be stable across members. An implementation must report unsupported attributes and must not silently treat device memory as ordinary cacheable memory.

## 6. Memory consistency model

### 6.1 Required distinction

The memory model must define observable ordering among:
- A thread's program-ordered loads and stores.
- Other threads in the same workspace.
- Threads in different workspaces.
- Other CPU units in a mixed-width system.
- DMA and I/O agents.
- Shared registers and shared memory.
- Atomic and non-atomic operations.

A fence instruction name is not a memory model. The standard must state which observations are permitted and which are forbidden.

### 6.2 Candidate models

| Model | Main property | Cost and risk |
|---|---|---|
| Sequential consistency | All memory operations appear in one global order consistent with each thread's program order | Strong constraints can limit implementation freedom and scalability |
| Total-store-order style | Stores are globally ordered with a defined set of load reorderings | More complex software reasoning than sequential consistency |
| Acquire/release model | Synchronization operations establish explicit ordering edges | Requires precise rules for atomics, dependencies, and fences |
| Relaxed model with explicit synchronization | Ordinary operations have few ordering guarantees | High burden on compiler, runtime, and application correctness |

**Recommendation for investigation:** Start with a formally specified acquire/release model plus explicit fences and well-defined atomic operations. Evaluate whether stronger default ordering is warranted for shared registers and device/I/O memory. No model is selected here.

### 6.3 Compiler and hardware ordering

The architecture must define:
- Which compiler reorderings are legal.
- Which hardware reorderings are legal.
- How dependencies affect ordering, if at all.
- How acquire/release operations interact with ordinary accesses.
- Fence scopes and completion semantics.
- Ordering across CPU units and fabric links.
- Whether instruction completion implies memory visibility.
- How exceptions and interrupts interact with outstanding memory operations.

The compiler's language memory model must map to the fNx memory model without weakening required guarantees.

## 7. Atomic operations

Atomicity is distinct from ordering and from fabric transaction width.

The specification must define:
- Supported atomic access widths.
- Alignment constraints.
- Read-modify-write operation set.
- Compare/exchange success and failure ordering.
- Atomicity scope: local thread group, workspace, CPU unit, backplane, or system.
- Progress guarantees and possible failure/retry behavior.
- Interaction with caches, DMA, and non-coherent endpoints.
- Whether operations may span multiple fabric transactions.

A wide N-bit register does not imply that every N-bit memory operation is globally atomic. No split transaction may be described as atomic unless the platform contract guarantees indivisible completion.

## 8. Cache and coherence

The architecture must decide whether shared memory uses:
- Hardware cache coherence.
- Software-managed coherence or explicit ownership.
- Non-cacheable shared regions.
- A hybrid model with defined coherence domains.

The decision must cover CPU-to-CPU, CPU-to-DMA, and CPU-to-device visibility. Cache maintenance operations, ownership transfer, and completion semantics must be defined if coherence is not automatic.

Coherence is not implied by a shared physical address space.

## 9. Mixed-width interoperability

A mixed-width system must preserve the same byte-addressed object representation and memory-ordering semantics across members.

The platform contract must define:
- Transfer lengths independently of N.
- Byte order and scalar representation.
- Alignment and access splitting.
- Atomic capability negotiation.
- Error and partial-completion behavior.
- Memory attributes and protection propagation.
- Ordering across width-neutral fabric transactions.

A wide member must not assume that a narrower member can perform an N-bit atomic or native-width transaction. Unsupported operations must be rejected or handled through an explicitly correct software protocol.

## 10. Conformance requirements

A conforming implementation needs tests for:

1. N-bit virtual-address representation and arithmetic.
2. Translation and physical-address-width limits.
3. Permission checks for read, write, and execute.
4. Address-space isolation and identifier reuse.
5. Mapping updates and local/remote invalidation.
6. Fault attribution and restart behavior.
7. Alignment and split-access behavior.
8. Memory ordering litmus tests.
9. Atomic operation width, scope, and ordering.
10. CPU-to-CPU and CPU-to-DMA visibility.
11. Mixed-width shared-memory representation.
12. Device-memory ordering and access faults.

The standard should publish executable litmus tests and golden outcomes before declaring a memory model complete.

## 11. Decision status

| Topic | Status |
|---|---|
| N-bit virtual-address width | Existing family working direction |
| Physical address width may be less than N | Existing family working direction |
| Byte-addressed virtual memory | Recommended; not ratified |
| Address arithmetic overflow | Open |
| Translation architecture | Open |
| Mapping granularity | Open |
| Address-space identifiers and invalidation | Required contract; details open |
| Permissions and fault taxonomy | Required contract; details open |
| Memory attributes | Open |
| Memory consistency model | Open; acquire/release plus explicit fences recommended for investigation |
| Atomic widths and scope | Open |
| Cache/coherence model | Open |
| Endianness and mixed-width object representation | Open |
| Conformance litmus tests | Required; not yet implemented |

## 12. Related specifications

- [fNx family and architectural contract](fnx-family-and-architectural-contract-0.1.md)
- [fNx logical data paths](data-paths-0.1.md)
- [fNx workspace protection and shared state](workspace-protection-and-shared-state-0.2.md)
- [fNx context state and residency](context-state-and-residency-0.2.md)
- [fNx control processor and management plane](fnx-control-and-management-plane-0.1.md)
- [fNx roadmap](../roadmap.md)
