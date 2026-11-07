# fNx Workspace and Thread Lifecycle Contract Proposal 0.1

**Date:** 2026-11-06  
**Status:** Architectural proposal; identity format, privilege encoding, and physical scheduling remain open

## 1. Purpose

This proposal defines the logical contract for workspaces, hardware threads, private architectural state, workspace-shared state, lifecycle transitions, and resource discovery.

It deliberately separates architectural identity and observable semantics from physical register banking, scheduler topology, context-cache design, and OS process abstractions.

A workspace is an fNx logical resource-sharing and protection domain containing independently schedulable threads. It is not inherently an operating-system process, address space, CPU core, or security principal. The term does not imply TMS9900 memory-resident register workspaces or binary compatibility.

## 2. Architectural entities

### 2.1 Workspace

A workspace is the ownership domain for:
- A set of member threads.
- Workspace-shared architectural registers, if implemented by the selected profile.
- Workspace-level execution and scheduling policy.
- Resource limits and capability state.
- Explicitly granted access to memory, communication endpoints, and platform resources.

A workspace has an identity, lifecycle state, authority/ownership metadata, and discoverable limits.

A workspace does not automatically own a unique virtual address space. Address-space ownership and workspace ownership are separate associations. The architecture must permit one workspace to use one address space and must define whether multiple workspaces may share an address space under explicit authorization.

### 2.2 Hardware thread

A hardware thread is an independently schedulable architectural execution context.

Its private state is not directly readable or writable by another unprivileged thread. Privileged software and authorized debug facilities may inspect or modify suspended state under defined rules.

A thread belongs to exactly one workspace at a time. Migration between workspaces, if supported, is an explicit privileged operation with defined state and authority transfer. It is not an ordinary scheduler operation.

### 2.3 Address space

An address space defines the virtual-to-physical translation and memory protection context used by an executing thread.

The address-space association is explicit architectural state or an equivalent protected binding. A workspace is not itself an address space unless a platform chooses that arrangement.

### 2.4 Privileged software

The architecture must identify the authority that creates, configures, suspends, migrates, inspects, and destroys workspaces and threads. This may be a kernel, hypervisor, or partition manager. The number and encoding of privilege levels remain open.

## 3. Identity and stale-reference protection

Workspace and thread identities must not become valid references to unrelated objects after reuse.

The architecture must provide a stale-reference prevention mechanism. Candidate mechanisms include generation-tagged identifiers, non-reused identifiers within a defined epoch, or protected opaque handles.

**Recommendation:** Use generation-tagged logical identities. A workspace identity and thread identity should each contain or resolve through a generation value that changes when the underlying slot is reused.

Requirements:
- Thread identity is unique within its defined system scope.
- Workspace identity is unique within its defined system scope.
- A thread reference resolves only while the referenced generation is live.
- Reuse must not make an old reference identify a new object.
- Generation wrap must be handled explicitly; silent wrap into a still-valid stale reference is forbidden.
- Cross-backplane identity and routing must be defined independently of local scheduler slot numbers.

The bit layout and width of identifiers are not selected. Do not assume that identity width must equal N.

## 4. State ownership

### 4.1 Thread-private state

The complete architectural thread context must include, as applicable:
- N-bit general-purpose registers.
- Instruction address.
- Architectural execution state and condition state.
- Privilege and protected control state.
- Address-space binding and translation context reference.
- Interrupt enable/mask and pending state visible to the thread.
- Exception cause and restart/return metadata.
- Debug state that is architecturally visible.
- Any additional state required to make execution precise and restartable.

The exact register classes and count remain open. The 128-private-register target belongs to the 128-bit research profile, not the family invariant.

### 4.2 Workspace-shared state

If workspace-shared registers are part of the selected ISA profile, their state belongs to the workspace, not to any one thread.

The current 128-shared-register target is a 128-bit research-profile assumption. Register count is independent of N.

Workspace state must also include:
- Workspace identity and lifecycle state.
- Membership and thread references.
- Shared-register state and synchronization metadata.
- Workspace-level scheduling/resource policy.
- Authorized memory and communication bindings.
- Fault and teardown state needed for correct lifecycle transitions.

### 4.3 In-flight state

Implementation-specific in-flight state is not necessarily software-visible, but it must be sufficient to preserve the architectural contract:
- Precise exception behavior.
- Defined ordering and completion of memory/fabric operations.
- Correct interrupt boundaries.
- No duplicated or lost committed operations during eviction, migration, or reset.

The specification must identify which operations may be outstanding at each lifecycle transition and how they are completed, drained, cancelled, or reported.

## 5. Protection and access rules

The following baseline is recommended:

| State/resource | Owning thread | Other thread in same workspace | Other workspace |
|---|---|---|---|
| Private registers | Direct architectural access | No direct access | No direct access |
| Workspace-shared registers | Access subject to defined operation rules | Access subject to same rules | No access unless explicit privileged grant |
| Thread control state | Own permitted state only | No direct control | No direct control |
| Workspace control state | No direct privileged modification | No direct privileged modification | No direct privileged modification |
| Workspace memory | Access according to translation and permissions | Access according to translation and permissions | Denied unless explicitly mapped/granted |
| Communication endpoint | Authorized endpoint operations | Authorized endpoint operations | Explicitly granted endpoint only |

Privileged software may have broader authority, but its access must be mediated by the privilege and protection model. Debugger access must be separately authorized and auditable where required.

No unprivileged instruction may forge a workspace or thread identity to access another domain's state.

## 6. Shared-register semantics

If workspace-shared registers are exposed as architectural state, their concurrency semantics must be specified rather than inferred from a physical implementation.

**Recommendation for the baseline contract:**
1. A read or write of one shared register is indivisible at the architectural level.
2. Concurrent writes to the same shared register are serialized into a single observable per-register order.
3. The architecture does not promise which writer wins unless synchronization establishes an order.
4. Multi-register updates are not atomic unless an explicit operation says so.
5. Read-modify-write operations require explicit atomic instructions or synchronization primitives with defined ordering.
6. Ordering between shared registers and memory follows the selected memory model and fence rules.
7. An implementation may use banking, replication, queues, or time-multiplexing, provided the architectural behavior is preserved.

This recommendation does not require a single-cycle, physically centralized N-bit register file. It defines observable behavior only. The cost and feasibility of maintaining atomic N-bit visibility for large N must be measured.

The standard must also define behavior when a thread faults or is terminated while holding a software lock. Hardware must not silently reset shared synchronization state unless a defined recovery operation owns that transition.

## 7. Lifecycle state machines

### 7.1 Workspace lifecycle

Candidate logical states:

- **Created:** identity allocated; not yet available to execute threads.
- **Configured:** policy, resource limits, protection bindings, and initial shared state installed.
- **Active:** threads may be admitted and scheduled.
- **Quiescing:** no new work admitted; existing threads and operations are being drained or suspended.
- **Suspended:** no thread is executing; state is retained and inspectable by authorized software.
- **Destroying:** references and resources are being revoked and outstanding operations resolved.
- **Destroyed:** identity is invalid; resources may be reclaimed subject to generation-safe reuse.

Required transitions:
- Creation and configuration are privileged.
- Activation requires complete valid protection and resource configuration.
- Quiescing prevents new thread admission and defines treatment of new asynchronous events.
- Suspension preserves all committed architectural state.
- Destruction requires a defined policy for outstanding threads, shared state, memory mappings, communication endpoints, and in-flight requests.
- A destroyed identity must not become valid again through slot reuse.

### 7.2 Thread lifecycle

Candidate logical states:

- **Created:** identity allocated and associated with a workspace.
- **Ready:** eligible to run.
- **Running:** currently selected on an execution resource.
- **Blocked:** waiting for a defined event or resource.
- **Suspended:** not eligible to run; state retained.
- **Exception handling:** executing or awaiting defined exception service.
- **Terminating:** completion/cancellation and resource release in progress.
- **Terminated:** no longer executable; identity invalid after required cleanup.

The architecture must distinguish architectural state from scheduler bookkeeping. An implementation may use more internal states but must preserve externally observable transitions.

A thread may move between ready, running, and blocked according to scheduling policy. It must not transition directly from running to destroyed while architecturally visible operations remain unresolved.

## 8. Creation, admission, and teardown

Thread creation must define:
- Initial register and instruction state.
- Initial privilege and address-space binding.
- Workspace membership and shared-state access.
- Initial interrupt/debug state.
- Entry point validation.
- Resource reservation or admission failure.
- Parent/creator authority and handle return.

Thread termination must define:
- Whether it is cooperative or forcibly initiated.
- How outstanding loads, stores, atomics, and fabric transactions are handled.
- How pending exceptions and interrupts are resolved.
- How shared synchronization ownership is reported or recovered.
- When the identity becomes invalid.
- When physical resources can be reclaimed.

Workspace teardown must not silently discard committed shared state before an explicit destruction or ownership-transfer boundary. It must revoke access before an identity or backing slot is reused.

## 9. Context residency, eviction, and migration

Architectural contexts may be resident or nonresident. Residency is an implementation resource, not a change in architectural identity.

Required invariants:
- Eviction preserves complete architectural state.
- Restoration resumes from the same architectural state.
- Interrupts targeting nonresident contexts remain pending or are delivered through a defined service path.
- Debug inspection of nonresident suspended state is consistent.
- A context cannot be simultaneously active in two execution locations unless the architecture explicitly defines replicated execution semantics.
- Migration preserves identity, permissions, address-space association, shared-state visibility, and outstanding-operation semantics.
- Migration must define a quiescence or equivalent handoff point preventing duplicated or lost committed operations.

No hardware-managed context cache, spill/fill format, or software save/restore mechanism is selected.

## 10. Resource discovery and admission

The platform must expose implementation capabilities and limits, including:
- Maximum supported workspaces.
- Maximum threads per workspace and total admitted threads.
- Maximum resident contexts and current resource pressure.
- Supported private and shared register counts/classes.
- Supported shared-register atomic operations.
- Context save/restore and migration capabilities.
- Workspace and thread identity scope.
- Supported scheduling priorities and guarantees.
- Resource exhaustion and admission failure behavior.

Architectural namespace size and physical capacity are distinct. A platform may support fewer concurrent objects than the architecture can identify, but it must report limits and fail admission explicitly rather than silently aliasing identities or corrupting state.

The 128-workspace × 128-thread × 128-private/128-shared-register configuration remains a 128-bit research profile only. It must not become a family invariant by repetition in diagrams or examples.

## 11. Scheduling and progress obligations

The architecture must specify observable progress guarantees separately from scheduler topology.

At minimum:
- A ready thread may not be silently forgotten.
- Resource exhaustion and blocked state must be distinguishable.
- Priority and fairness policy must be discoverable.
- A thread holding resources must not cause unbounded system-wide deadlock solely because it is nonresident.
- Interrupt and exception delivery must interact correctly with blocked and suspended contexts.
- Implementations must state any bounded-service guarantees and their assumptions.

The standard should not promise universal fairness or a fixed latency without a workload and platform model.

## 12. Conformance requirements

A reference model and tests must cover:
1. Workspace and thread identity uniqueness.
2. Generation-safe reuse and stale-handle rejection.
3. Workspace membership and cross-workspace isolation.
4. Private-register ownership.
5. Shared-register atomic visibility and ordering.
6. Thread and workspace lifecycle transitions.
7. Creation/admission failure and resource exhaustion.
8. Teardown with outstanding memory/fabric operations.
9. Eviction and restoration equivalence.
10. Interrupt delivery to nonresident and blocked contexts.
11. Migration state/permission preservation.
12. Debug inspection and modification authorization.
13. Shared synchronization owner termination.
14. Capability discovery and reported limits.

## 13. Decision status

| Topic | Status |
|---|---|
| Workspace as logical sharing/protection domain | Working direction |
| Workspace equals OS process/address space/security principal | Explicitly not assumed |
| Thread belongs to one workspace at a time | Recommended baseline |
| Identity and generation-safe reuse | Required; representation open |
| Private versus shared state ownership | Required; details open |
| Shared-register atomic visibility | Recommended baseline; cost study required |
| Multi-register atomicity | Not implied |
| Workspace/thread lifecycle states | Proposed logical model |
| Context residency | Implementation choice; semantics required |
| Save/restore and migration | Required contract; mechanism open |
| Scheduler topology and fairness | Open |
| 128/128/128/128 capacities | 128-bit research profile only |
| Privilege and debug authority | Required; model open |
| Resource discovery and admission | Required; schema open |

## 14. Related specifications

- [fNx family and architectural contract](fnx-family-and-architectural-contract-0.1.md)
- [fNx context state and residency](context-state-and-residency-0.2.md)
- [fNx workspace protection and shared state](workspace-protection-and-shared-state-0.2.md)
- [fNx scheduling progress and interrupts](scheduling-progress-and-interrupts-0.2.md)
- [fNx address-space and memory-model proposal](address-space-and-memory-model-proposal-0.1.md)
- [fNx exception and interrupt model proposal](exception-and-interrupt-model-proposal-0.1.md)
- [fNx logical data paths](data-paths-0.1.md)
- [fNx roadmap](../roadmap.md)
