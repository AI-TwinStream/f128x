# Workspace Protection and Shared State 0.2

**Date:** 2026-11-04  
**Status:** Architectural investigation; semantics not frozen

## 1. Terminology and historical distinction

The f128x workspace is currently defined as a logical resource-sharing and protection domain containing independently schedulable threads.

This is not the same mechanism as the TMS9900 workspace. The TMS9900 Workspace Pointer identifies a memory-resident set of sixteen general registers. f128x took inspiration from workspace-oriented register organization, but its proposed workspace is a broader execution and sharing abstraction.

The name remains under review. Alternatives should be evaluated for clarity, historical accuracy, and usefulness in software and toolchain interfaces. No rename is made here.

## 2. Ownership model to specify

The architecture must distinguish:
- Workspace identity and lifecycle.
- Thread identity and lifecycle within a workspace.
- Address-space ownership.
- Shared register ownership.
- Workspace-shared memory ownership.
- Privileged kernel or hypervisor ownership.
- Debugger and inspection authority.

A workspace must not automatically be equated with an OS process, address space, CPU core, or security principal.

## 3. Protection questions

Define the access-control rules for:
- A thread accessing its own private registers.
- A thread accessing its workspace's shared registers.
- A thread accessing another workspace's registers.
- Workspace-shared memory access.
- Inter-workspace message or channel operations.
- Kernel access to all contexts.
- Debugger inspection and modification.
- Context migration and workspace destruction.

Cross-workspace register access is not assumed. Any communication must pass through an explicit, permission-checked mechanism.

## 4. OS virtualization

The architectural namespace of 4,096 workspaces must not be interpreted as a hard limit of 4,096 OS processes or security domains.

The design must decide whether:
- Workspace IDs are physical slots only, with OS-managed virtual workspace identities.
- A workspace context can be saved and restored under a different physical slot.
- Multiple software processes can own separate workspaces over time.
- A process may own multiple workspaces.
- A workspace may contain threads associated with more than one software abstraction.
- Hardware provides generation tags or another mechanism to prevent stale-ID reuse.

The OS should be able to multiplex software workloads over implementation-supported hardware workspace capacity without exposing accidental cross-domain state.

## 5. Shared-register semantics

The 128 shared GPRs per workspace require precise semantics. At minimum, define:
- Read and write atomicity at the architectural width.
- Visibility of writes between threads.
- Ordering relative to ordinary memory loads and stores.
- Behavior for simultaneous writes to the same register.
- Whether operations can be combined atomically (e.g. compare-and-swap, fetch-add, bitwise operations).
- Whether a shared register can represent a synchronization state.
- How exceptions, eviction, and workspace migration preserve shared state.

No “last writer wins” rule should be assumed without a defined ordering and execution model.

## 6. Synchronization candidates

| Candidate | Potential value | Questions |
|---|---|---|
| Atomic read/modify/write operations | Familiar, composable synchronization | Width, contention, ordering, progress |
| Locks in shared registers | Simple software model | Fairness, owner death, priority inversion |
| HEP-style full/empty state | Efficient producer/consumer handoff | State representation, blocking semantics, timeout/cancellation |
| Versioned or transactional access | May reduce hot-spot contention | Complexity, retries, memory ordering |
| Explicit message channels | Clear ownership and flow control | Queue capacity, backpressure, deadlock, ordering |

HEP-style full/empty mechanisms deserve investigation because they connect synchronization state with thread readiness. They are not selected as the mandatory shared-register model.

## 7. Memory consistency domains

The architecture must define ordering and visibility across:
1. Private thread registers.
2. Workspace-shared registers.
3. Thread-private memory.
4. Workspace-shared memory.
5. Inter-workspace communication.
6. Device and I/O memory.

Candidate consistency primitives include relaxed operations, acquire/release semantics, fences, and sequentially consistent operations for selected synchronization primitives.

A memory-model table must state which operations order which domains. The existence of a fence mnemonic alone is not a memory model.

## 8. Protection and lifecycle invariants

The eventual specification should guarantee:
- No unprivileged cross-workspace register access.
- No stale context identity can access a newly allocated context after slot reuse.
- Workspace teardown cannot silently discard architecturally committed shared state before ownership transfer or destruction is complete.
- Context migration preserves state and permissions.
- Debug access is explicitly authorized and does not bypass architectural consistency rules.

The exact privilege levels, capability model, page-table structure, and revocation mechanism remain open.
