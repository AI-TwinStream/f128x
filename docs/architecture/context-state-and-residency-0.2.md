# Context State and Residency 0.2

**Date:** 2026-11-04  
**Status:** Architectural investigation; no physical residency mechanism selected

## 1. Problem

The current capacity target represents 65,536 thread contexts, each with 128 private 128-bit GPRs, plus 4,096 workspaces each with 128 shared 128-bit GPRs. This yields 136 MiB of aggregate architectural register state.

That number must not be mistaken for a practical, fully multiported physical register file. The architecture needs to specify what software observes independently of where or how an implementation stores the state.

## 2. Architectural state versus physical residency

The specification must distinguish:

- **Architectural context:** the complete state associated with a thread identity, including its GPRs, instruction address, execution status, privilege/control state, and any architecturally visible execution metadata.
- **Workspace state:** shared architectural state and control associated with a workspace identity.
- **Resident state:** state currently held in fast physical structures and directly available to execution pipelines.
- **Nonresident state:** architectural state retained elsewhere and not currently allocated to the fast execution structures.
- **In-flight state:** transient execution state required to preserve precise architectural behavior while instructions are outstanding.

An implementation may support fewer simultaneously resident contexts than the maximum architectural namespace, but the specification must define the consequences. A capacity limit, if any, is an implementation/platform property that software can discover and manage.

## 3. Required observability contract

For a given context, software with sufficient privilege must be able to observe architecturally defined state consistently whether that context is resident or nonresident.

The architecture must define:
- Which agent owns context state at each lifecycle stage.
- Whether a suspended context can be inspected or modified.
- How debug facilities access nonresident state.
- How exceptions and interrupts interact with context eviction.
- Whether context identity survives eviction and restoration.
- How context creation, suspension, migration, and destruction affect state.
- How stale references are prevented when a context slot is reused.

The mechanism must preserve precise exceptions and avoid losing or duplicating architectural updates.

## 4. Candidate implementation approaches

| Approach | Advantages | Costs and risks |
|---|---|---|
| Fully resident register state | Low and predictable access latency | Large SRAM/register area, port and wiring cost, difficult scaling |
| Hardware-managed context cache | Fast active contexts; transparent backing possible | Tags, replacement, dirty tracking, save/restore bandwidth, coherence with debug and OS |
| Software-managed save/restore | Simpler hardware; explicit OS control | Context-switch latency and software complexity; shared state complications |
| Hybrid residency | Allows hardware fast paths with explicit OS lifecycle control | More complex contract and ownership transitions |

These are candidates, not a ranking or decision.

## 5. Register-file organization questions

A resource study must include more than bit capacity:
- Read and write port requirements.
- Number of concurrent execution clusters.
- Banking and bank-conflict behavior.
- Replication and coherence for shared registers.
- Bypass and forwarding networks.
- Wakeup/select logic.
- Context save/restore bandwidth.
- SRAM/BRAM/URAM mapping and timing.
- Area, power, and frequency at multiple capacity profiles.

The shared register file is especially sensitive: up to 16 threads may access the same workspace's shared register namespace concurrently. A 128-register namespace does not imply 16 simultaneous reads and writes to every register.

## 6. Initial implementation profiles to evaluate

These are experimental configurations, not frozen architectural variants:

| Profile | Workspaces | Threads/workspace | Contexts |
|---|---:|---:|---:|
| Tiny functional model | 1 | 2 | 2 |
| Small FPGA study | 16 | 4 | 64 |
| Medium FPGA study | 32 | 8 | 256 |
| Larger research prototype | 64 | 8 | 512 |
| Architectural capacity target | 4,096 | 16 | 65,536 |

A reduced profile must preserve the same architectural semantics for the resources it implements. Any implementation-visible limits must be discoverable and must not silently change instruction meaning.

## 7. Required experiments

1. Estimate memory resources for private and shared register storage in each profile.
2. Compare banked, replicated, and time-multiplexed access structures.
3. Model access latency and contention under representative thread mixes.
4. Estimate save/restore traffic for software-managed and hardware-managed residency.
5. Evaluate shared-register hot spots and synchronization traffic.
6. Identify which state must remain resident for precise execution and which may be backed.
7. Define a context state inspection interface suitable for debuggers and kernels.

## 8. Open decisions

- Complete architectural context format.
- Workspace control-state format.
- Residency and replacement policy.
- Hardware versus software save/restore responsibility.
- Context migration semantics.
- Debug and privileged inspection semantics.
- Resource discovery and implementation capacity reporting.

No spill/fill mechanism is selected by this document.
