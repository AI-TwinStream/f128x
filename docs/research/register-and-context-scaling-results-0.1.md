> **Historical experiment notice (2026-11-05):** These results belong to the earlier f128x scheduler model and its synthetic workload. They are not evidence for all fNx widths, nor do they establish physical feasibility or performance. The family-level architectural contract is maintained separately.

> **Historical experiment notice (2026-11-05):** These results belong to the earlier f128x scheduler model and its synthetic workload. They are not evidence for all fNx widths, nor do they establish physical feasibility or performance. The family-level architectural contract is maintained separately.

# Register and context scaling results 0.1

## Summary

The first controlled scheduler experiment supports the hypothesis that a 256-thread workspace can hide more memory latency than a 16-thread workspace when only one workspace is active and the issue width is eight. In the tested synthetic workload, the 256-thread profile sustained the full eight-instruction-per-cycle issue ceiling, while the 16-thread profile sustained approximately 0.77 IPC.

This is evidence for a useful operating point under one workload, not evidence that 256 threads per workspace are universally optimal or that a physical implementation will achieve these rates.

## Controlled latency-hiding experiment

Both profiles used the same workload and scheduler:

| Parameter | Value |
|---|---:|
| Active workspaces | 1 |
| Scheduler | Hierarchical, round-robin across workspace queues |
| Aggregate issue width | 8 instructions/cycle |
| Memory instruction probability | 25% |
| Memory latency | 80 cycles |
| Outstanding memory operations per thread | 1 |
| Simulated cycles | 100,000 |
| Warmup | 10,000 cycles |
| Private/shared GPR configuration | 128 / 128 |
| Register width | 128 bits |

Only the number of thread slots in the workspace changed. The register-count configuration was held at its default so the experiment isolates thread concurrency rather than register-file capacity.

### Results

| Seed | 16 threads/workspace IPC | 256 threads/workspace IPC | IPC ratio |
|---:|---:|---:|---:|
| 7 | 0.7731 | 8.0000 | 10.35× |
| 42 | 0.7653 | 8.0000 | 10.45× |
| 2027 | 0.7854 | 8.0000 | 10.19× |
| **Mean** | **0.7746** | **8.0000** | **10.33×** |

The 256-thread configuration reached 100% utilization of the model's eight-wide issue ceiling in all three runs. The 16-thread configuration reached approximately 9.6–9.8% utilization.

The result is consistent across the three deterministic seeds. The 256-thread workspace supplies enough independent contexts for this one-workspace workload to cover the configured memory latency and keep the issue slots busy.

## Full architectural capacity accounting

A separate CI smoke test instantiated the expanded architectural capacity:

| Property | Value |
|---|---:|
| Workspaces | 4,096 |
| Threads per workspace | 256 |
| Architectural contexts | 1,048,576 |
| Private GPRs per thread | 256 |
| Shared GPRs per workspace | 256 |
| GPR width | 256 bits |
| Aggregate private state | 8 GiB |
| Aggregate shared state | 32 MiB |
| Total architectural register state | 8 GiB + 32 MiB |

The test successfully initialized and ran the hierarchical scheduler against all 1,048,576 contexts with the full candidate register counts and width. A separate distributed-scheduler smoke exercised 256-thread workspace grouping across four clusters. This validates simulator capacity accounting and grouping logic; it does not validate physical register-file feasibility.

## Interpretation and limits

The experiment deliberately isolates one active workspace. With many active workspaces, the aggregate runnable population can hide latency even when each workspace has fewer than 256 threads. The number of threads required per workspace therefore depends on how many workspaces are concurrently active, issue width, memory latency, memory-operation probability, per-thread outstanding limits, and the workload's runnable distribution.

The model uses a fixed memory latency and a simplified per-thread outstanding-request limit. It does not model real cache misses, MSHRs, memory-controller scheduling, register dependencies, instruction-level parallelism, shared-register contention, or physical execution-unit timing.

The experiment says nothing about the performance benefit or physical feasibility of 256 private and 256 shared 256-bit GPRs. The scheduler simulator reports architectural storage capacity, but does not model compiler register allocation, operand encoding, register-file ports, banking, timing, area, or power.

## CI evidence

The full Java 25 scheduler workflow passed in [run 338](https://github.com/AI-TwinStream/f128x/actions/runs/41947595227). The workflow includes:
- Full-capacity 1,048,576-context, 8 GiB private-state, and 32 MiB shared-state accounting.
- Hierarchical and distributed scheduler grouping with 256 thread slots per workspace.
- Three-seed 16-versus-256 thread latency-hiding comparison.
- Existing cache, memory, scheduler, board/backplane, topology, arbitration, routing, and bank-conflict regression tests.

## Next experiments

1. Sweep 32, 64, 128, and 256 thread slots with one, four, and many active workspaces.
2. Repeat with multiple memory latencies and memory-operation probabilities.
3. Vary per-thread outstanding operations and finite shared-queue capacity.
4. Add explicit workspace-shared-register banking and contention.
5. Compare 128 versus 256 private GPR capacity in a compiler/workload model; do not infer a gain from the current scheduler simulator.
6. Continue the separate per-output fabric-arbitration integration before using the fabric model to support topology decisions.
