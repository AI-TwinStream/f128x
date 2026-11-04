# Scheduling Progress and Interrupts 0.2

**Date:** 2026-11-04  
**Status:** Candidate requirements and research plan; scheduler topology not frozen

## 1. Scheduling hierarchy

The 65,536-context target makes a single centralized ready-selection structure an unattractive default. A candidate hierarchy is:
1. Workspace-level service and resource policy.
2. Per-workspace readiness for up to 16 thread slots.
3. Implementation-sized scheduler clusters.
4. Local selection near execution resources.
5. Candidate transfer or work stealing when local work is unavailable.

This is a research direction, not a mandatory physical topology. Workspace policy must not serialize independent thread issue.

## 2. Readiness and issue are distinct

The model distinguishes:
- Resident.
- Runnable/ready.
- Selected.
- Issued.
- Executing or outstanding.
- Blocked on memory or synchronization.
- Suspended.
- Exception/interrupt handling.
- Terminated.

A selected thread may still fail issue validation because operands, permissions, dependencies, or execution resources are unavailable.

## 3. Progress and fairness requirements

The architecture and platform contract must state what progress is guaranteed and under which assumptions.

Questions:
- Can a continuously ready thread be starved indefinitely?
- Is fairness guaranteed within a workspace, across workspaces, or both?
- Can priority override fairness, and what bounds apply?
- How are priority inversion and owner preemption handled for shared synchronization?
- Can locality optimizations indefinitely defer remote work?
- What happens when all runnable threads are blocked on resources held by suspended threads?

A possible baseline to evaluate is bounded service for continuously eligible threads under a nonempty runnable set, with explicit exceptions for privileged priority and resource dependencies. No numeric bound is selected here.

## 4. Deadlock and progress

The specification must distinguish:
- Architectural deadlock.
- Software lock deadlock.
- Resource starvation.
- Scheduler starvation.
- Memory-system forward-progress failure.

Atomic operations and synchronization primitives must state their progress properties. A lock-free instruction does not automatically make an algorithm lock-free if the memory system or scheduler can indefinitely suppress participants.

## 5. Interrupt and exception routing

The architecture must specify:
- Interrupt target selection: thread, workspace, execution cluster, or global handler.
- Whether interrupts can target a nonresident context.
- Whether an interrupt preempts an executing thread or is deferred to a safe boundary.
- Per-thread and per-workspace masking.
- Priority and nesting.
- Affinity and migration behavior.
- State capture and precise return.
- Delivery to a context blocked on memory or synchronization.
- Debug and non-maskable events.
- Latency and fairness guarantees.

Candidate designs include per-thread delivery, workspace-level delivery with target-thread selection, and a hybrid with global/platform interrupts routed to privileged service contexts.

## 6. Candidate scheduler evaluations

Compare:
- Centralized ready selection.
- Hierarchical workspace/thread selection.
- Distributed scheduler clusters with local ready queues and work stealing.

Metrics:
- Area and timing.
- Power.
- Maximum scheduler fan-in.
- Wakeup propagation latency.
- Fairness and starvation behavior.
- Cross-cluster migration cost.
- Shared-register locality.
- Performance under memory stalls and synchronization-heavy workloads.

## 7. Required models

Build a discrete-event or cycle-level model that varies:
- Context count.
- Workspace/thread distribution.
- Issue width.
- Memory latency and outstanding requests.
- Runnable-thread fraction.
- Shared-register contention.
- Priority distribution.
- Interrupt rate and target distribution.

Use the model to justify context capacity against target issue throughput and effective memory latency. The simple latency-hiding estimate (N \geq L \times R) is a starting point only; dependency chains, memory-level parallelism, and workload readiness must also be represented.

No scheduler topology, fairness bound, or interrupt architecture is frozen here.
