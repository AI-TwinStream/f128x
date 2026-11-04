# Context Capacity and Latency-Hiding Model 0.2

**Date:** 2026-11-04  
**Status:** First-order analytical model; illustrative assumptions only  
**Related:** [Context state and residency](context-state-and-residency-0.2.md), [Scheduling progress and interrupts](scheduling-progress-and-interrupts-0.2.md)

## 1. Purpose

The 65,536-context target is a design hypothesis, not a performance result. This note establishes a simple model that can be checked, varied, and later replaced or calibrated by a cycle-level simulator.

The model asks: how many resident hardware thread contexts are needed to sustain a target instruction issue rate when threads become temporarily ineligible during long-latency operations?

It does **not** prove that f128x needs 65,536 contexts. Nor does it model all causes of stalls, memory-level parallelism, dependencies, or scheduler overhead.

## 2. First-order model

Let:

- \(L\) = effective thread-blocking latency in cycles.
- \(W\) = desired aggregate instruction issue rate in instructions per cycle.
- \(p\) = fraction of resident contexts that are eligible to issue at a given time.
- \(N\) = resident hardware thread contexts.

Under the deliberately simple assumption that one issued instruction can block its thread for \(L\) cycles, and each eligible thread can issue at most one instruction per cycle:

\[
N \geq \frac{L \times W}{p}
\]

The numerator is the number of simultaneously available thread-issue opportunities needed to cover the latency window. The eligibility fraction converts the required ready contexts into a total resident-context estimate.

This is a steady-state lower-bound heuristic, not a scheduler implementation formula. It assumes independent threads, enough independent work, no issue bottlenecks other than thread readiness, and a stable eligible fraction.

### Example scenarios

| Effective latency L | Issue rate W | Eligible fraction p | Estimated resident contexts |
|---:|---:|---:|---:|
| 100 cycles | 1 inst/cycle | 50% | 200 |
| 300 cycles | 4 inst/cycle | 50% | 2,400 |
| 600 cycles | 8 inst/cycle | 25% | 19,200 |
| 1,000 cycles | 8 inst/cycle | 12.5% | 64,000 |

The final row is close to the 65,536 target, but it is an intentionally demanding combination of latency, issue width, and low eligibility. It is not evidence that the target is generally required.

Conversely, a system with lower effective latency, narrower issue, higher runnable fraction, or substantial memory-level parallelism may need far fewer contexts.

## 3. Important limitations

The model must be extended to account for:

1. **Per-thread memory-level parallelism:** a thread may have multiple independent outstanding misses and continue executing.
2. **Dependency chains:** a thread may be blocked by data dependencies even when memory is responsive.
3. **Runnable-work availability:** the OS or application may not provide enough independent ready threads.
4. **Issue restrictions:** a thread may issue less frequently than once per cycle due to dependencies, instruction mix, or execution-unit constraints.
5. **Memory-system queueing:** increasing concurrency can increase latency rather than hold it constant.
6. **Shared-state contention:** synchronization and shared-register hot spots can reduce eligibility.
7. **Scheduler overhead:** selection, wakeup, migration, and arbitration consume area, timing, power, and potentially cycles.
8. **Nonuniform latency:** cache hits, cache misses, translation misses, and I/O have different latency distributions.
9. **Occupancy and locality:** contexts may be resident but not equally useful to every execution cluster.

The value \(p\) must therefore be measured from workload traces or an executable model. It must not be treated as a universal constant.

## 4. Architectural capacity versus implementation residency

The current architectural capacity target is:

\[
4,096\;workspaces \times 16\;threads = 65,536\;thread\;contexts
\]

That namespace is distinct from the number of contexts physically resident in a particular implementation at one time. The architecture must define whether nonresident contexts are preserved in backing storage, managed by software, or handled by a hybrid mechanism.

Implementation profiles should be evaluated independently. A smaller implementation can expose fewer simultaneously resident contexts while preserving the same instruction semantics for supported contexts, provided capacity limits and lifecycle behavior are architecturally discoverable.

## 5. Register-state capacity by profile

Each thread's 128 private 128-bit GPRs represent:

\[
128 \times 128\;bits = 16,384\;bits = 2\;KiB
\]

Each workspace's 128 shared 128-bit GPRs also represent 2 KiB.

| Profile | Threads | Workspaces | Private GPR capacity | Shared GPR capacity | Total GPR capacity |
|---|---:|---:|---:|---:|---:|
| Tiny functional model | 2 | 1 | 4 KiB | 2 KiB | 6 KiB |
| Small FPGA study | 64 | 16 | 128 KiB | 32 KiB | 160 KiB |
| Medium FPGA study | 256 | 32 | 512 KiB | 64 KiB | 576 KiB |
| Larger research prototype | 512 | 64 | 1 MiB | 128 KiB | 1.125 MiB |
| Architectural capacity target | 65,536 | 4,096 | 128 MiB | 8 MiB | 136 MiB |

These figures count GPR bits only. They exclude PCs, status/control state, privilege state, tags, ECC/parity, rename state, in-flight instructions, queues, bypass networks, and physical implementation overhead.

They are capacity figures, not area estimates. SRAM/BRAM mapping, port count, banking, replication, timing, and access topology determine actual implementation cost.

## 6. Required next experiment

Build a parameterized discrete-event or cycle-level model with at least these inputs:

- Context count and workspace/thread distribution.
- Issue width and execution-unit availability.
- Latency distributions for representative memory levels.
- Eligible/runnable fraction, measured over time rather than assumed constant.
- Per-thread outstanding-request limit and memory-level parallelism.
- Dependency-chain length and instruction mix.
- Shared-register contention and synchronization frequency.
- Scheduler topology, selection latency, wakeup latency, and migration cost.

Collect at least:

- Sustained issue rate and utilization.
- Ready-queue depth and fraction of cycles with insufficient ready work.
- Per-thread and per-workspace service distribution.
- Maximum and percentile wait time for continuously eligible threads.
- Memory concurrency, queue occupancy, and observed latency.
- Scheduler overhead and cross-cluster traffic.

Run the same workload assumptions against 64, 256, 512, 2,048, 4,096, 16,384, and 65,536 contexts. Include sensitivity sweeps rather than a single “representative” workload.

## 7. Decision gates

Do not freeze the 65,536-context target until the model answers:

1. Which workload classes benefit materially from additional contexts?
2. At what context count does issue utilization stop improving for each issue width and latency profile?
3. Does additional concurrency increase memory latency or synchronization contention enough to erase the benefit?
4. What scheduler topology can sustain the target issue rate without a global critical path?
5. What is the storage and access cost at each useful point on the curve?
6. Is the full namespace an architectural requirement, an implementation maximum, or simply a research-scale aspiration?

## 8. Current disposition

- Retain 65,536 as a **provisional architectural capacity target** for investigation.
- Do not claim it is necessary to hide memory latency.
- Do not equate architectural contexts with simultaneously resident contexts or OS-visible CPUs.
- Use scaled implementation profiles to validate semantics and explore cost.
- Require simulation and resource estimates before selecting a physical scheduler or register-file topology.
