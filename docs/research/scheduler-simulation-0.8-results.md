> **Historical simulation note (2026-11-05):** This result set belongs to the f128x scheduler model and its historical parameters. It is retained for reproducibility and is not a family-wide fNx performance claim.

# Scheduler Simulation 0.8 Results

**Date:** 2026-11-04  
**Branch:** `experiment/memory-system-model-0.1`  
**Toolchain:** Temurin JDK 25  
**Status:** Reproducible exploratory results; not hardware performance claims

## 1. Scope

Simulation 0.8 adds configurable migration and remote-memory locality penalties to the distributed scheduler:

- `fnx.migrationLatency`: cycles a thread waits after work stealing moves it to another cluster.
- `fnx.remoteMemoryLatencyPenalty`: extra latency added to each memory operation issued while the thread is remote from its workspace's home cluster.

A workspace's home cluster is its workspace ID modulo cluster count. A thread becomes remote when work stealing moves it away from that home cluster and becomes local again if it migrates back. The model counts migrations, remote wakeups, measured remote thread-cycles, remote memory issues, added remote-memory latency, migration-waiter cycles, and completed migrations.

The remote-memory penalty applies to every memory operation while remote. This is a coarse proxy for access to workspace-local state, not a cache-coherence, NUMA, or interconnect model. Migration delay and remote-memory latency are independent controls and were tested separately as well as together.

## 2. Experiment

Common parameters:

- 64 contexts across 4 workspaces.
- Distributed scheduler with 8 clusters.
- Global issue width 4; per-cluster issue width 1.
- Memory-operation fraction 0.25; single memory latency 300 cycles.
- Per-thread outstanding limit 2; shared queue effectively unbounded.
- 20,000 cycles; warmup 2,000; seed 42.

Four deterministic runs isolate the two costs:

| Scenario | Migration latency | Remote memory penalty | Measured IPC | Migrations | Migration-waiter cycles | Remote memory issues | Added remote-memory latency |
|---|---:|---:|---:|---:|---:|---:|---:|
| No penalties | 0 | 0 | 1.6862 | 30,403 | 0 | 7,312 | 0 |
| Migration only | 3 | 0 | 1.5656 | 43,907 | 131,701 | 6,772 | 0 |
| Remote memory only | 0 | 7 | 1.6575 | 30,175 | 0 | 7,262 | 50,834 |
| Both penalties | 3 | 7 | 1.5377 | 44,819 | 134,455 | 6,652 | 46,564 |

The combined case recorded 5,954 remote wakeups and 1,006,024 measured remote thread-cycles. It completed 44,818 of 44,819 migrations before the simulation ended; one migration remained in flight. The remote-memory latency accounting is exact: 6,652 remote memory issues multiplied by seven extra cycles equals 46,564 added latency cycles.

## 3. Interpretation

For this deliberately sparse and oversubscribed configuration:

- Migration delay alone reduced measured IPC from 1.6862 to 1.5656, approximately 7.2%.
- Remote-memory latency alone reduced measured IPC to 1.6575, approximately 1.7%.
- Applying both reduced IPC to 1.5377, approximately 8.8% below the no-penalty baseline.

Migration delay dominates this particular result. The combined case also changes the number and timing of steals, so the effects are not perfectly additive. These percentages are not hardware predictions: the workload, home-cluster mapping, cluster count, and all costs are synthetic.

The experiment does show why work stealing cannot be treated as free. It changes thread placement, adds migration waiting, and can make subsequent memory operations more expensive. A scheduler policy that looks attractive when migration is free may behave differently when locality matters.

## 4. Measurement semantics and limitations

- `remoteThreadCycles` is measured after warmup and counts threads resident away from their home cluster, including threads that are blocked or waiting for migration.
- Migration count, remote wakeups, remote memory issue count, added latency cycles, migration-waiter cycles, and migration completions are whole-run counters.
- A migration is recorded when a thread is stolen into another cluster's queue. The configured migration delay postpones its return to the ready queue.
- The remote-memory penalty applies to all memory operations while remote. There is no address mapping, cache hierarchy, shared-state working set, or distinction between local and remote memory regions.
- The simulator still uses one global issue-width budget with per-cluster issue limits. It does not implement physical independent pipelines or a distributed arbitration network.

No architectural decision about cluster count, placement policy, or context capacity follows from this experiment.

## 5. Next work

Extend the memory model with request sizes, byte-oriented bandwidth, and bank/channel contention. Keep migration and locality penalties as independent controls so future experiments can separate scheduler placement costs from memory-controller effects. Later, replace the fixed home-cluster mapping with an explicit workspace placement policy and evaluate alternative placement strategies.
