# fNx Workspace Protection and Shared State 0.3

**Date:** 2026-11-05  
**Status:** Architectural investigation; semantics not frozen

## 1. Terminology

The fNx workspace is a logical resource-sharing and protection domain containing independently schedulable threads. It is not the TMS9900 memory-resident register workspace. The TMS9900 remains historical inspiration, not an ISA or binary-compatibility target.

Workspace terminology is retained provisionally. A workspace is not automatically an OS process, address space, CPU core, or security principal.

## 2. Family width rules

For a member fNx, N is its native architectural width. General-purpose registers and native arithmetic are N bits. The number of private registers, shared registers, workspaces, and thread slots is independent of N and remains subject to separate decisions.

The current 128-workspace × 128-thread × 128-private/128-shared-register target is specific to the 128-bit research profile research profile. It is not yet a family-wide capacity requirement.

## 3. Ownership model to specify

Distinguish:
- Workspace identity and lifecycle.
- Thread identity and lifecycle.
- Address-space ownership.
- Shared-register ownership.
- Workspace-shared memory ownership.
- Kernel/hypervisor ownership.
- Debugger and inspection authority.

Cross-workspace register access is not assumed. Inter-workspace communication must use an explicit permission-checked mechanism.

## 4. Protection questions

Define access rules for:
- A thread's private registers.
- Its workspace's shared registers.
- Other workspaces' registers.
- Workspace-shared memory.
- Inter-workspace messages/channels.
- Kernel access.
- Debug inspection/modification.
- Context migration and workspace destruction.

## 5. Shared-register semantics

The current 128-bit research profile study uses 128 shared GPRs per workspace, each 128 bits wide. Other fNx members use N-bit register width if this register model is retained; register count is still an independent decision.

Specify:
- Read/write atomicity.
- Inter-thread visibility.
- Ordering relative to memory.
- Simultaneous writes to one register.
- Atomic read/modify/write operations.
- Synchronization-state representation.
- Exception, eviction, and migration behavior.

No last-writer-wins behavior should be assumed without a defined ordering and execution model.

## 6. Synchronization candidates

| Candidate | Potential value | Questions |
|---|---|---|
| Atomic read/modify/write | Familiar synchronization | Width, contention, ordering, progress |
| Shared-register locks | Simple software model | Fairness, owner death, priority inversion |
| HEP-style full/empty state | Producer/consumer handoff | State representation, blocking, cancellation |
| Versioned/transactional access | May reduce hot-spot contention | Retries, complexity, memory ordering |
| Explicit message channels | Clear ownership and flow control | Queue capacity, backpressure, deadlock |

HEP-style full/empty mechanisms deserve investigation but are not selected as the mandatory shared-register model.

## 7. Memory consistency domains

Define ordering and visibility across private registers, workspace-shared registers, private memory, workspace-shared memory, inter-workspace communication, and device/I/O memory.

Candidate primitives include relaxed operations, acquire/release, fences, and selected sequentially consistent operations. A fence mnemonic alone is not a memory model.

## 8. Protection and lifecycle invariants

The eventual specification should guarantee:
- No unprivileged cross-workspace register access.
- No stale identity can access a newly allocated context after slot reuse.
- Teardown does not silently discard committed shared state before destruction/ownership transfer completes.
- Migration preserves state and permissions.
- Debug access is authorized and consistent.

Privilege levels, capability model, translation structures, and revocation remain open.
