# Scheduler Simulation 0.5 Results

**Date:** 2026-11-04  
**Branch:** `experiment/memory-system-model-0.1`  
**Toolchain:** Temurin JDK 25  
**Status:** Reproducible exploratory results; not hardware performance claims

## 1. Scope

Simulation 0.5 adds three ready-selection policies to the 0.4 memory model:

- Flat FIFO.
- Hierarchical workspace round-robin, with one ready queue per 16-thread workspace.
- Distributed cluster queues with round-robin cluster selection and work stealing when the target cluster is empty.

The distributed implementation uses a global issue-width budget. It does not represent independent cluster issue pipelines or a physical scheduler network. Its purpose is to expose policy behavior and collect service/fairness metrics before designing a more detailed cluster model.

## 2. Balanced-workload comparison

Parameters:

- 1,024 contexts across 64 workspaces.
- Issue width 4.
- Memory-operation fraction 0.25.
- L1/L2/DRAM conditional fractions 0.70/0.20/0.10 with latencies 4/20/200 cycles.
- Per-thread outstanding limit 4; shared request capacity 128.
- Dependency fraction 0.10 of non-memory instructions; dependency latency 12 cycles.
- 50,000 cycles; warmup 5,000; seed 42.
- Distributed scheduler configured with 8 clusters.

| Scheduler | IPC | Issue utilization | Workspace Jain fairness | Steals |
|---|---:|---:|---:|---:|
| FIFO | 4.0000 | 100.00% | 0.999997 | 0 |
| Hierarchical | 4.0000 | 100.00% | 1.000000 | 0 |
| Distributed | 4.0000 | 100.00% | 0.999997 | 0 |

All three policies saturate the issue width for this homogeneous workload. The hierarchical round-robin policy equalizes aggregate workspace issue counts in this run. This does not establish a general fairness advantage: the workload gives every workspace the same instruction and memory behavior, and the model does not charge scheduler-selection or migration cost.

## 3. Sparse-cluster work-stealing path

Parameters:

- 64 contexts across 4 workspaces.
- 8 distributed scheduler clusters.
- Issue width 4.
- Memory-operation fraction 0.25; single memory latency 300 cycles.
- Per-thread outstanding limit 2; shared request capacity effectively unbounded.
- 20,000 cycles; warmup 2,000; seed 42.

Measured distributed result:

| Metric | Result |
|---|---:|
| IPC | 1.6862 |
| Issue utilization | 42.16% |
| Scheduler steals | 30,403 |
| Workspace Jain fairness | 0.999795 |

The work-stealing path is exercised because there are more scheduler clusters than workspaces. The global round-robin selector visits empty target clusters and steals ready threads from nonempty queues. The high steal count exposes repeated reassignment under this deliberately oversubscribed configuration.

This is not a claim that a physical implementation should use eight clusters for four workspaces. It is a useful warning that cluster count, workspace placement, local issue budgets, and migration policy cannot be chosen independently. The current model does not assign a latency, power, or locality penalty to a steal, so the result cannot quantify its cost.

## 4. Interpretation and next work

The balanced case shows that the three policies can reach the same throughput under a uniform workload. It does not distinguish their behavior under skewed workspace activity, priorities, shared-register hot spots, or different memory profiles.

The next scheduler experiment should introduce workspace-level workload heterogeneity and explicit per-cluster issue budgets. It should then measure:

- Per-workspace and per-thread service distribution.
- Jain fairness, minimum service, and maximum wait.
- Steal frequency, migration count, and time spent remote from workspace-local state.
- Local versus remote wakeup delay.
- Scheduler selection work and idle-cluster time.
- Throughput as the workspace-to-cluster ratio changes.

The current scheduler policies remain experiments, not architectural commitments. No scheduler topology or 65,536-context requirement is frozen by these results.
