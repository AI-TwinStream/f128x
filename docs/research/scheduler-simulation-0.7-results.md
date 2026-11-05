> **Historical simulation note (2026-11-05):** This result set belongs to the f128x scheduler model and its historical parameters. It is retained for reproducibility and is not a family-wide fNx performance claim.

# Scheduler Simulation 0.7 Results

**Date:** 2026-11-04  
**Branch:** `experiment/memory-system-model-0.1`  
**Toolchain:** Temurin JDK 25  
**Status:** Reproducible exploratory results; not hardware performance claims

## 1. Scope

Simulation 0.7 adds a per-cluster issue budget to the scheduler experiment. The configured global issue width remains a hard aggregate ceiling. Each logical cluster also has a local issue-width limit:

`effectiveIssueCapacity = min(globalIssueWidth, clusterCount × clusterIssueWidth)`

The local width is configured with `-Df128x.clusterIssueWidth=N` and defaults to the global issue width, preserving previous behavior. Cluster selection rotates round-robin. The distributed scheduler first selects from the target cluster's local queue and steals from another cluster if the target is empty. FIFO and hierarchical policies each operate as one logical cluster.

This is not a model of physically independent issue pipelines or a distributed arbitration network. It introduces independent local budgets within a shared cycle-level scheduler while conserving the configured global capacity.

## 2. Local-budget saturation

Parameters:

- 1,024 contexts across 64 workspaces.
- Distributed scheduler, 2 clusters.
- Global issue width 4; local cluster issue width 1.
- Memory fraction 0.25; L1/L2/DRAM fractions 0.70/0.20/0.10 and latencies 4/20/200.
- Per-thread outstanding limit 4; shared queue capacity 128.
- Dependency fraction 0.10 of non-memory instructions; dependency latency 12.
- 20,000 cycles; warmup 2,000; seed 42.

| Metric | Result |
|---|---:|
| Configured global issue width | 4 |
| Cluster count | 2 |
| Per-cluster issue width | 1 |
| Effective aggregate capacity | 2 |
| Measured IPC | 2.0000 |
| Global issue utilization | 50.00% |
| Utilization of effective cluster budget | 100.00% |
| Cluster 0 issued instructions | 20,000 |
| Cluster 1 issued instructions | 20,000 |

Each cluster issues at most one instruction per cycle. The global issue width of four does not allow either cluster to exceed its local budget, so the effective capacity is two instructions per cycle.

## 3. Global-capacity saturation

Parameters are the same except:

- Distributed scheduler, 4 clusters.
- Global issue width 4; local cluster issue width 2.
- 50,000 cycles; warmup 5,000; seed 42.

| Metric | Result |
|---|---:|
| Configured global issue width | 4 |
| Cluster count | 4 |
| Per-cluster issue width | 2 |
| Effective aggregate capacity | 4 |
| Measured IPC | 4.0000 |
| Global issue utilization | 100.00% |
| Utilization of effective cluster budget | 100.00% |
| Instructions issued per cluster | 50,000 each |

The sum of local budgets is eight, but the global issue-width ceiling remains four. The run reaches four IPC without exceeding two issues in any cluster per cycle. This verifies that local capacity does not accidentally multiply the architectural global budget.

## 4. Interpretation and limitations

The tests demonstrate that the simulator enforces both constraints: local issue width per cluster and the aggregate global issue width. They do not establish that a physical f128x implementation should use these cluster counts or widths.

The model still has a central logical scheduler and a global issue-width ceiling. It does not model independent physical pipelines, arbitration latency, cross-cluster links, issue-unit inventories, clock/power domains, or migration/locality penalties. Cluster issue totals are whole-run counts; the local per-cycle cap is enforced by the issue loop.

The 0.6 runnable-thread-cycle and runnable-workspace-cycle fairness metrics remain active. They should be retained when adding migration costs, because a policy can change both service distribution and the opportunity population.

No architectural decision about the number of clusters, issue width, workspace count, or 65,536 contexts follows from these runs.

## 5. Next work

Add configurable migration and locality penalties, including remote wakeups and time spent away from a workspace's preferred cluster. Then extend the memory model with request sizes, byte-oriented bandwidth, and bank/channel contention. These costs should be introduced separately so the scheduler and memory effects remain distinguishable.
