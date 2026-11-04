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

| Scheduler | IPC | Issue utilization | Raw workspace issue-count Jain index | Steals |
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
| Raw workspace issue-count Jain index | 0.999795 |

The work-stealing path is exercised because there are more scheduler clusters than workspaces. The global round-robin selector visits empty target clusters and steals ready threads from nonempty queues. The high steal count exposes repeated reassignment under this deliberately oversubscribed configuration.

This is not a claim that a physical implementation should use eight clusters for four workspaces. It is a useful warning that cluster count, workspace placement, local issue budgets, and migration policy cannot be chosen independently. The current model does not assign a latency, power, or locality penalty to a steal, so the result cannot quantify its cost.

## 4. Workspace workload-skew comparison

Parameters:

- 64 contexts across 4 workspaces; issue width 4.
- First workspace assigned the hot profile (`hotWorkspaceRatio=0.25)).
- Hot workspace: memory-operation fraction 0.90; dependency-stall fraction 0.20.
- Other workspaces: memory-operation fraction 0.05; dependency-stall fraction 0.02.
- Memory latency: 300 cycles; per-thread outstanding limit 2.
- 50,000 cycles; warmup 5,000; seed 42.
- Distributed configuration: 2 clusters.

| Scheduler | IPC | Issue utilization | Raw workspace issue-count Jain index | Steals |
|---|---:|---:|---:|---:|
| FIFO | 4.0000 | 100.00% | 0.793313 | 0 |
| Hierarchical | 4.0000 | 100.00% | 0.794508 | 0 |
| Distributed | 4.0000 | 100.00% | 0.793544 | 9 |

All policies keep the four-wide issue engine saturated. The raw issue-count fairness index is lower because the hot workspace has a much higher memory and dependency stall rate and therefore less runnable work. This metric is not normalized by runnable time; it must not be interpreted as proof of scheduler starvation or unfair treatment. The small differences between policies are not meaningful evidence of a fairness winner.

The result motivates a better fairness model: measure each workspace's runnable-thread cycles and compare delivered issue service against its actual opportunity to run. That instrumentation is required before making policy claims.

## 5. Interpretation and next work

The balanced and skewed cases both show that the three policies can reach the same aggregate throughput in the tested workloads. The raw workspace issue-count index is confounded by runnable availability, so the current tests do not establish a fairness winner. They also do not cover priorities, shared-register hot spots, or locality-sensitive memory profiles.

The next scheduler experiment should introduce workspace-level workload heterogeneity and explicit per-cluster issue budgets. It should then measure:

- Per-workspace and per-thread service distribution, normalized by runnable-thread cycles.
- Jain fairness, minimum service, and maximum wait.
- Steal frequency, migration count, and time spent remote from workspace-local state.
- Local versus remote wakeup delay.
- Scheduler selection work and idle-cluster time.
- Throughput as the workspace-to-cluster ratio changes.

The current scheduler policies remain experiments, not architectural commitments. No scheduler topology or 65,536-context requirement is frozen by these results.
