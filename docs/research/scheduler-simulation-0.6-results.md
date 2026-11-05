> **Historical simulation note (2026-11-05):** This result set belongs to the f128x scheduler model and its historical parameters. It is retained for reproducibility and is not a family-wide fNx performance claim.

# Scheduler Simulation 0.6 Results

**Date:** 2026-11-04  
**Branch:** `experiment/memory-system-model-0.1`  
**Toolchain:** Temurin JDK 25  
**Status:** Reproducible exploratory results; not hardware performance claims

## 1. Scope

Simulation 0.6 adds two distinct opportunity-normalized service measures:

- **Runnable-thread cycles:** each ready thread contributes one opportunity in each measured cycle in which it is eligible for issue.
- **Runnable-workspace cycles:** a workspace contributes one opportunity in each measured cycle in which at least one of its threads is eligible for issue.

Both are sampled after completion/dependency wakeups and queue-credit wakeups, immediately before issue selection. Per-workspace measured issue counts are divided by each opportunity count. Jain fairness is calculated across workspaces that had at least one opportunity at the corresponding level. A workspace with opportunities but zero service remains in the calculation with a zero rate; a workspace with no opportunity is excluded.

These answer different questions. Thread-opportunity fairness asks whether an individual runnable thread receives comparable issue service. Workspace-opportunity fairness asks whether a workspace receives comparable aggregate issue service during the time it has work available. Neither metric accounts for priorities, instruction costs, or physical arbitration costs.

The existing raw issue-count Jain index remains reported for continuity. It is a workload outcome and is not a fairness measure when workspaces have different amounts of runnable work.

## 2. Balanced-workload comparison

Parameters:

- 1,024 contexts across 64 workspaces.
- Issue width 4.
- Memory-operation fraction 0.25.
- L1/L2/DRAM conditional fractions 0.70/0.20/0.10; latencies 4/20/200 cycles.
- Per-thread outstanding limit 4; shared request capacity 128.
- Dependency fraction 0.10 of non-memory instructions; dependency latency 12 cycles.
- 50,000 cycles; warmup 5,000; seed 42.
- Distributed scheduler configured with 8 clusters.

| Scheduler | IPC | Thread-opportunity Jain | Workspace-opportunity Jain | Raw issue-count Jain |
|---|---:|---:|---:|---:|
| FIFO | 4.0000 | 0.999998 | 0.999998 | 0.999997 |
| Hierarchical | 4.0000 | 1.000000 | 1.000000 | 1.000000 |
| Distributed | 4.0000 | 0.999998 | 0.999998 | 0.999997 |

All three policies saturate the four-wide issue budget and are effectively indistinguishable on this homogeneous workload. This does not establish a general scheduler winner.

## 3. Skewed-workload comparison

Parameters:

- 64 contexts across 4 workspaces; issue width 4.
- First workspace is hot (\`hotWorkspaceRatio=0.25\`).
- Hot workspace: memory-operation fraction 0.90; dependency-stall fraction 0.20.
- Other workspaces: memory-operation fraction 0.05; dependency-stall fraction 0.02.
- Memory latency 300 cycles; per-thread outstanding limit 2.
- 50,000 cycles; warmup 5,000; seed 42.
- Distributed scheduler configured with 2 clusters.

| Scheduler | IPC | Thread-opportunity Jain | Workspace-opportunity Jain | Raw issue-count Jain | Steals |
|---|---:|---:|---:|---:|---:|
| FIFO | 4.0000 | 0.998424 | 0.835836 | 0.793313 | 0 |
| Hierarchical | 4.0000 | 0.494826 | 0.989169 | 0.794508 | 0 |
| Distributed | 4.0000 | 0.993326 | 0.827657 | 0.793544 | 9 |

All policies saturate the global four-wide issue budget. The fairness measures expose a real policy distinction hidden by aggregate IPC and raw issue counts:

- Hierarchical round-robin provides nearly equal aggregate issue service per runnable-workspace cycle (0.989169), consistent with its one-workspace-at-a-time rotation.
- Its thread-opportunity fairness is much lower (0.494826). A workspace with a small ready population can receive a similar workspace turn to one with many ready threads, so its individual runnable threads receive more service per opportunity.
- FIFO and distributed scheduling are much closer to equal service per runnable-thread cycle (0.998424 and 0.993326), but provide less equal aggregate service per runnable-workspace cycle in this skewed case (0.835836 and 0.827657).
- Raw issue-count fairness is approximately 0.79 for all three and does not reveal which fairness objective a policy favors.

This is not evidence that one policy is universally fairer. It demonstrates that scheduler fairness must state its unit of entitlement: thread opportunity or workspace opportunity. A design that wants both may need explicit weights or hierarchical arbitration with a defined within-workspace policy.

## 4. Interpretation and limitations

The counters are sampled from the simulator's ready queues, not from a hardware trace. Each ready thread is treated as one equal opportunity regardless of instruction type or downstream cost. A thread ready at the pre-issue sampling point contributes an opportunity even if issue width means it is not selected in that cycle.

The model still uses a global issue-width budget. The distributed policy is not a set of independent cluster issue pipelines. No scheduler-selection, migration, locality, or steal cost is charged. The results do not establish starvation bounds, priority behavior, or a physical arbitration implementation.

No architectural decision about workspace count, cluster count, or 65,536 contexts follows from these runs.

## 5. Next work

The next useful experiments are independent per-cluster issue budgets with an explicitly conserved global capacity, followed by configurable migration/locality penalties. Then add byte-oriented memory bandwidth and bank/channel contention. Preserve both opportunity-normalized fairness metrics when changing the model so policy changes cannot hide one fairness objective behind another.
