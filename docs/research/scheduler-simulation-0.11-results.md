# Scheduler Simulation 0.11 Results

**Date:** 2026-11-04  
**Branch:** `experiment/memory-system-model-0.1`  
**Toolchain:** Temurin JDK 25  
**Status:** Reproducible exploratory results; not hardware performance claims

## 1. Scope

Simulation 0.11 replaces the single serialized dependency-stall approximation with a configurable set of independent synthetic dependency chains per thread.

Configure the chain count with `-Df128x.dependencyChains=N`. The default is one; the supported range is 1 through 128. A dependency-bound non-memory instruction issues on one ready chain and makes that chain unavailable for the configured dependency latency. If another chain is ready by the next cycle, the thread remains eligible to issue from it. If no other chain is ready, the thread waits until the earliest chain becomes ready.

The model reports:

- `dependencyIssues`: dependency-bound instructions issued.
- `dependencyIssuesOverlapped`: dependency-bound instructions after which another independent chain remains ready for the next cycle.
- `dependencyWaiterCycles`: accumulated cycles with threads waiting because all their chains are unavailable.
- `dependencyStallWakeups`: thread wakeups after a dependency wait.
- `threadsWaitingOnDependenciesAtEnd`: threads still waiting for a dependency chain when the run ends.

The chain selection is round-robin among ready chains. Non-dependency instructions and memory operations are not assigned operand-level producer/consumer relationships.

## 2. Experiment

Parameters:

- 8 contexts; issue width 4.
- Memory-operation fraction 0.0.
- Dependency-bound fraction 0.50 of non-memory instructions.
- Dependency latency 12 cycles.
- 50,000 cycles; warmup 5,000; seed 42.
- FIFO scheduler; no memory requests.

Only the number of independent dependency chains changed.

| Metric | One chain/thread | Four chains/thread |
|---|---:|---:|
| Measured IPC | 1.2318 | 3.9667 |
| Issue utilization | 30.79% | 99.17% |
| Dependency-bound instructions | 30,744 | 99,156 |
| Dependency issues overlapped by another chain | 0 | 65,141 |
| Dependency-waiter cycles | 368,875 | 121,659 |
| Dependency-stall wakeups | 30,737 | 34,014 |
| Threads waiting on dependencies at end | 7 | 1 |

Four independent chains increased measured IPC by approximately 3.22× in this deliberately dependency-bound workload, bringing the eight-context run close to the four-wide issue ceiling. The number of overlapped dependency issues and the reduction in waiter cycles show the mechanism: the thread can continue on a different ready chain rather than waiting for one serialized chain.

## 3. Interpretation and limitations

This result demonstrates the impact of independent chains in the simulator; it does not establish an expected f128x speedup. The model creates synthetic chain readiness, not a real instruction stream or operand dependency graph.

It does not model register renaming, architectural register hazards, forwarding, execution-unit latency variation, memory dependencies, branch speculation, out-of-order issue, or compiler scheduling. Dependency-bound instructions are assigned to the next ready chain in round-robin order. This is a controlled ILP experiment, not a claim that every thread will expose four independent chains.

The default one-chain setting preserves the previous serialized dependency behavior. The multi-chain setting should be used only when the workload assumptions justify independent instruction chains.

No ISA, register-file, or context-capacity decision follows from this experiment.

## 4. Next work

Add configurable address-to-channel/bank mapping and more explicit synthetic instruction dependency graphs. Then calibrate chain count, dependency latency, and dependency-bound instruction frequency from representative workload traces. Preserve the one-chain configuration as a baseline.
