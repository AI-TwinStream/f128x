# fNx Scheduling Progress and Interrupts 0.3

**Date:** 2026-11-05  
**Status:** Candidate requirements and research plan; scheduler topology not frozen

## 1. Scope and family width

This is an fNx family research document. Native width N determines architectural register, arithmetic, pointer, and virtual-address width. Scheduler topology and context capacity are independent of N unless a specific implementation profile states otherwise.

The 128-workspace × 128-thread 128-bit research profile research target is not a family-wide requirement. Existing simulations use 128-bit research profile-oriented parameters and should be interpreted accordingly.

## 2. Scheduling hierarchy

A candidate logical hierarchy is:
1. Workspace-level service and resource policy.
2. Per-workspace thread readiness.
3. Implementation-sized scheduler clusters.
4. Local selection near execution resources.
5. Candidate work transfer or work stealing when local work is unavailable.

This is a research direction, not a mandatory physical topology. Workspace policy must not serialize independent thread issue.

## 3. Readiness and issue

Distinguish resident, runnable/ready, selected, issued, executing/outstanding, blocked, suspended, exception/interrupt handling, and terminated states.

A selected thread may still fail issue validation because operands, permissions, dependencies, or execution resources are unavailable.

## 4. Progress and fairness

The architecture and platform contract must state progress guarantees and assumptions. Questions include starvation, fairness within/across workspaces, priority overrides, priority inversion, locality-induced delay, and deadlock when resources are held by suspended threads.

A bounded-service baseline is worth evaluating, but no numeric bound is selected.

## 5. Deadlock and progress

Distinguish architectural deadlock, software lock deadlock, resource starvation, scheduler starvation, and memory-system forward-progress failure. Atomic instructions do not automatically make an algorithm lock-free if the scheduler or memory system can indefinitely suppress participants.

## 6. Interrupt and exception routing

Specify target selection (thread/workspace/cluster/global), delivery to nonresident contexts, preemption boundaries, masking, priority/nesting, affinity/migration, precise state capture/return, delivery to blocked contexts, debug/NMI behavior, and latency/fairness guarantees.

Candidate designs include per-thread delivery, workspace-level delivery with target-thread selection, and privileged service contexts for global/platform events.

## 7. Candidate scheduler evaluations

Compare centralized ready selection, hierarchical workspace/thread selection, and distributed scheduler clusters with local ready queues and work stealing.

Measure area, timing, power, fan-in, wakeup latency, fairness, migration cost, shared-state locality, and performance under memory stalls and synchronization-heavy workloads.

## 8. Required models

Vary context count, workspace/thread distribution, issue width, memory latency, outstanding requests, runnable fraction, shared-state contention, priority distribution, and interrupt rate/target distribution.

Use the model to justify context capacity against throughput and effective latency. Simple latency-hiding estimates are only starting points.

## 9. Open decisions

No scheduler topology, fairness bound, progress guarantee, or interrupt architecture is frozen here. Family-wide architectural semantics must be distinguished from implementation policy.
