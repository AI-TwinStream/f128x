> **Historical candidate-study notice (2026-11-05):** This document explores earlier 128-bit research profile workspace/thread/register-capacity candidates. Its 4,096-workspace and 256-thread assumptions are not the current 128-bit research profile research profile and are not fNx family-wide requirements. Retain it as historical experimental input; use the [fNx family contract](fnx-family-and-architectural-contract-0.1.md) and [context residency study](context-state-and-residency-0.2.md) for current direction.

> **Historical candidate-study notice (2026-11-05):** This document explores earlier 128-bit research profile workspace/thread/register-capacity candidates. Its 4,096-workspace and 256-thread assumptions are not the current 128-bit research profile research profile and are not fNx family-wide requirements. Retain it as historical experimental input; use the [fNx family contract](fnx-family-and-architectural-contract-0.1.md) and [context residency study](context-state-and-residency-0.2.md) for current direction.

# Register and context scaling proposal 0.1

## Purpose

Evaluate a larger 128-bit research profile architectural register namespace and workspace thread population without conflating register count, register width, and physical implementation.

This is a candidate profile for investigation, not a frozen ISA or hardware requirement.

## Candidate profile

The expanded candidate retains 4,096 workspaces while increasing thread slots, private and shared register counts, and architectural register width. It is a candidate profile for study, not a frozen ISA or physical implementation.

| Property | Existing baseline | Expanded candidate |
|---|---:|---:|
| Workspaces | 4,096 | 4,096 |
| Threads per workspace | 16 | 256 |
| Architectural thread contexts | 65,536 | 1,048,576 |
| Private GPRs per thread | 128 | 256 |
| GPR width | 128 bits | 256 bits |
| Shared GPRs per workspace | 128 | 256 |
| Private state per thread | 2 KiB | 8 KiB |
| Shared state per workspace | 2 KiB | 8 KiB |
| Aggregate private state | 128 MiB | 8 GiB |
| Aggregate shared state | 8 MiB | 32 MiB |
| Total architectural register state | 136 MiB | 8 GiB + 32 MiB |

The expanded candidate has 64 times as much aggregate private state as the baseline: twice as many private registers per context, twice the register width, and sixteen times as many contexts. Aggregate shared state is four times the baseline because both the shared register count and register width double.

## Identity and encoding implications

With 4,096 workspaces and 256 thread slots per workspace, a compact architectural thread identifier can be represented as:

- 12-bit workspace identifier.
- 8-bit thread-slot identifier.
- 20-bit combined thread identifier.

A private register index and a shared-register index each require 8 bits to select one of 256 registers. An instruction with three explicit register operands therefore spends 24 bits on register selectors before opcode, immediate, addressing, predicate, or control fields. This is an early encoding pressure test, not a conclusion that every instruction must encode three full register indices.

The ISA study must compare:
- Fixed-width and mixed-width instruction encodings.
- Compact common forms and extended register/immediate forms.
- Separate private and shared register namespaces versus unified selectors.
- Register windows, bank selection, and explicit extension prefixes.
- Operand-count trade-offs and compiler register-allocation consequences.

The register count does not determine pointer width, virtual-address width, instruction width, memory-bus width, or execution-unit width.

## State capacity is not physical residency

The aggregate 8 GiB private state plus 32 MiB of workspace-shared state is architectural capacity. It does not require a monolithic, fully multiported 4 GiB register file.

Implementation studies must compare:
1. Fully resident contexts in a small implementation profile.
2. Banked context storage with a bounded number of resident thread contexts.
3. Hardware-managed context caches with backing storage for nonresident contexts.
4. Software-managed save/restore and hybrid approaches.

For each approach, measure:
- Register-bank capacity, read/write ports, and banking conflicts.
- Context activation, eviction, spill/fill bandwidth, and latency.
- Shared-register access bandwidth and contention.
- Scheduler ready-state storage, wakeup/select cost, and fairness.
- Interrupt and exception delivery to resident and nonresident contexts.
- Context migration and workspace lifecycle semantics.
- Area, power, clock frequency, and physical locality once an RTL implementation exists.

A scaled implementation must preserve the same architectural semantics even when it supports fewer simultaneously resident contexts. The implementation must expose capacity limits and must not silently change architectural thread identity or shared-state visibility during eviction.

## Shared-register policy remains independent

The expanded candidate has 256 shared 256-bit registers per workspace. With 256 threads contending for that namespace, the design must specify:
- Atomicity of reads and writes.
- Ordering and visibility between threads.
- Simultaneous-write resolution.
- Atomic read-modify-write operations.
- Synchronization primitives and memory-order guarantees.
- Access bandwidth, banking, replication, and forwarding.
- Behavior during preemption, eviction, and migration.

A larger shared namespace may reduce some allocation pressure, but it does not inherently solve access contention or synchronization.

## Scheduler and workload hypotheses

The 256-thread workspace is a hypothesis about latency tolerance and useful concurrency, not an assumed performance improvement.

The scheduler model should vary:
- Runnable-thread population from 1 through 256 per workspace.
- Memory and dependency stall distributions.
- Per-thread outstanding memory operations and memory-level parallelism.
- Workspace-local shared-register traffic and contention.
- Issue width and number of scheduler clusters.
- Fairness across threads and across workspaces.
- Interrupt frequency, priority, and target residency.
- Context residency limits independently of architectural capacity.

Measure throughput, latency distributions, runnable-thread service, runnable-workspace service, starvation bounds, and context-management overhead. Do not infer that more architectural contexts improve throughput when issue bandwidth, memory bandwidth, or shared-state service is already saturated.

## Recommended experiment sequence

1. Extend the executable scheduler model to accept up to 256 threads per workspace and compare 16/32/64/128/256 populations at equal aggregate issue capacity.
2. Add explicit register-state capacity accounting for 256 private and 256 shared 256-bit registers. This is storage accounting, not a physical register-file timing model.
3. Model workspace-shared register banking and contention before choosing a synchronization design.
4. Define nonresident context save/restore semantics and test identity, interrupts, and shared-state preservation.
5. Run ISA encoding feasibility experiments with 256-entry private register selectors.
6. Only then select an architectural profile for RTL and toolchain prototyping.

## Initial evidence

The first controlled one-workspace scheduler experiment showed the 256-thread profile sustaining the eight-wide issue ceiling while the 16-thread profile achieved approximately 0.77 IPC under an 80-cycle memory latency and 25% memory-instruction probability. This supports further investigation of 256 threads per workspace, but is not a universal optimum or a physical performance prediction. Full parameters, three-seed results, and limitations are recorded in [Register and context scaling results 0.1](../research/register-and-context-scaling-results-0.1.md).

The experiment held register count and width at the simulator defaults (128 registers of 128 bits). It provides evidence only about thread-count latency hiding, not the performance or physical feasibility of the expanded 256-register, 256-bit configuration.

## Decision status

- 256-bit GPR width: candidate.
- 256 private GPRs per thread: candidate.
- 256 shared GPRs per workspace: candidate.
- 256 threads per workspace: candidate.
- 4,096 workspaces: retained for comparison.
- Physical context residency, spill/fill, synchronization, ISA encoding, and scheduler topology: unresolved.
