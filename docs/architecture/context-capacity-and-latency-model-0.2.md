# Context Capacity and Latency-Hiding Model 0.2

**Date:** 2026-11-04  
**Status:** First-order analytical model plus an executable experiment with per-thread outstanding-request limits  
**Related:** Context state and residency, scheduling progress and interrupts, scheduler simulation.

## 1. Purpose

The 65,536-context target is a design hypothesis, not a performance result. This note establishes a simple model that can be checked, varied, and later calibrated by workload traces and richer simulation.

It does **not** prove that f128x needs 65,536 contexts. It also does not model a realistic cache hierarchy, multiple outstanding requests per thread, dependencies, or scheduler overhead.

## 2. First-order blocking model

For a deliberately narrow model, define:

- L = blocking latency in cycles.
- q = probability that an issued instruction blocks its thread for L cycles.
- W = desired aggregate issue rate in instructions per cycle.
- N = resident hardware thread contexts.

Assume each thread can issue at most one instruction per cycle. A nonblocking instruction makes the thread eligible again in the next cycle. A blocking instruction prevents that thread from issuing for L cycles. Each thread can have at most one such blocking operation outstanding.

The approximate mean service time per issued instruction for one thread is:

    T_thread ≈ 1 + qL

So the approximate aggregate issue capacity across N independent contexts is:

    I ≈ N / (1 + qL)

To sustain target issue rate W:

    N ≳ W(1 + qL)

This is a first-order estimate. It assumes independent blocking events, enough work to keep contexts active, constant latency, no queueing feedback, and no other issue bottlenecks. It is not a universal latency-hiding law.

### Example scenarios

| Blocking latency L | Blocking probability q | Target issue W | Approx. contexts |
|---:|---:|---:|---:|
| 100 cycles | 0.10 | 1 inst/cycle | 11 |
| 300 cycles | 0.25 | 4 inst/cycle | 304 |
| 600 cycles | 0.50 | 8 inst/cycle | 2,408 |
| 1,000 cycles | 0.75 | 8 inst/cycle | 6,008 |

These estimates are much smaller than 65,536 for these assumptions. That demonstrates that a huge context count cannot be justified by quoting memory latency alone. A larger target would need additional motivations, such as much higher latency, larger issue width, low runnable fraction, many execution clusters, or architectural capacity goals independent of latency hiding.

## 3. Initial cycle-level experiment

The Java program in tools/scheduler-sim implements the simple blocking model with a FIFO ready queue and deterministic random seed.

Example command:

    java -cp build/scheduler-sim org.flossware.f128x.sim.ContextSweep 4096 4 300 0.25 200000 42

The following sample was compiled and run with OpenJDK 21. Parameters: issue width 4, blocking latency 300 cycles, blocking probability 0.25, 200,000 cycles, seed 42. Warmup is 10,000 cycles.

| Contexts | Measured IPC | Issue utilization |
|---:|---:|---:|
| 64 | 0.8430 | 21.08% |
| 128 | 1.6901 | 42.25% |
| 256 | 3.3523 | 83.81% |
| 512 | 4.0000 | 100.00% |
| 1,024 | 4.0000 | 100.00% |
| 2,048 | 4.0000 | 100.00% |
| 4,096 | 4.0000 | 100.00% |
| 16,384 | 4.0000 | 100.00% |
| 65,536 | 4.0000 | 100.00% |

For this narrow workload, issue utilization saturates at 512 contexts. More contexts do not improve throughput. The analytical estimate of 304 contexts is a mean-based approximation; the simulation needs additional contexts to cover stochastic variation and keep all four issue slots supplied consistently.

This is one parameter point and one seed, not a performance claim about a real processor.

## 3.1 Per-thread memory-level parallelism experiment

The simulator now accepts an optional `max-outstanding` argument. A thread may continue issuing while it has fewer than this number of memory requests in flight; reaching the limit makes it wait until a request completes. A value of 1 reproduces the original blocking-thread behavior.

The following sweep was run by GitHub Actions with Temurin JDK 25.0.5. Fixed parameters: issue width 4, latency 300 cycles, memory-operation probability 0.25, 200,000 cycles, warmup 10,000 cycles, and seed 42. The table reports the smallest tested context count that reached 4.0000 measured IPC.

| Max outstanding requests per thread | 64 contexts | 128 contexts | 256 contexts | 512 contexts | First tested saturation point |
|---:|---:|---:|---:|---:|---:|
| 1 | 0.8430 IPC | 1.6901 IPC | 3.3523 IPC | 4.0000 IPC | 512 |
| 2 | 1.6898 IPC | 3.3521 IPC | 4.0000 IPC | 4.0000 IPC | 256 |
| 4 | 3.3513 IPC | 4.0000 IPC | 4.0000 IPC | 4.0000 IPC | 128 |
| 8 | 4.0000 IPC | 4.0000 IPC | 4.0000 IPC | 4.0000 IPC | 64 |

This demonstrates that per-thread memory-level parallelism changes the number of independent contexts needed to keep a fixed-width issue engine busy. In this model, increasing the outstanding-request limit from 1 to 8 moves the first tested saturation point from 512 contexts to 64.

This result is deliberately narrow. It does not establish that real threads can expose 8 independent misses, that a real memory system can sustain them, or that the cost of tracking and servicing those requests is negligible. The simulator assumes independent memory operations, one fixed latency, no shared memory queue or bandwidth ceiling, and one instruction per thread per cycle. It also uses one seed; multiple seeds and workload-derived request/dependency traces remain necessary.

The CI sweep is reproducible in `.github/workflows/scheduler-simulator.yml`. It is an experiment harness, not a benchmark claim about physical hardware.

## 4. Important limitations

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

The simulation's blocking probability q is an input assumption, not a measured workload property. It must not be treated as a universal constant.

## 5. Architectural capacity versus implementation residency

The current architectural capacity target is:

    4,096 workspaces × 16 threads = 65,536 thread contexts

That namespace is distinct from the number of contexts physically resident in a particular implementation at one time. The architecture must define whether nonresident contexts are preserved in backing storage, managed by software, or handled by a hybrid mechanism.

Implementation profiles should be evaluated independently. A smaller implementation can expose fewer simultaneously resident contexts while preserving the same instruction semantics for supported contexts, provided capacity limits and lifecycle behavior are architecturally discoverable.

## 6. Register-state capacity by profile

Each thread's 128 private 128-bit GPRs represent:

    128 × 128 bits = 16,384 bits = 2 KiB

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

## 7. Required next experiment

Extend the model with:

- Multiple outstanding requests per thread.
- Multiple latency classes and queueing.
- Dependencies and instruction mix.
- Shared-register contention and synchronization.
- Scheduler selection/wakeup latency and hierarchical topologies.
- Per-workspace and per-thread fairness measurements.

Run context counts from 64 through 65,536 across issue widths, memory-operation fractions, latency distributions, outstanding-request limits, and multiple seeds. Add dependency chains, shared-state contention, and scheduler topology alternatives. Collect sustained issue rate, utilization, ready-queue depth, per-thread service distribution, wait-time percentiles, memory queue occupancy, and scheduler overhead.

## 8. Decision gates

Do not freeze the 65,536-context target until the model answers:

1. Which workload classes benefit materially from additional contexts?
2. At what context count does issue utilization stop improving for each issue width and latency profile?
3. Does additional concurrency increase memory latency or synchronization contention enough to erase the benefit?
4. What scheduler topology can sustain the target issue rate without a global critical path?
5. What is the storage and access cost at each useful point on the curve?
6. Is the full namespace an architectural requirement, an implementation maximum, or simply a research-scale aspiration?

## 9. Current disposition

- Retain 65,536 as a **provisional architectural capacity target** for investigation.
- Do not claim it is necessary to hide memory latency.
- Do not equate architectural contexts with simultaneously resident contexts or OS-visible CPUs.
- Treat analytical estimates as hypotheses to test, not design proofs.
- Require richer simulation, workload evidence, and resource estimates before selecting a physical scheduler or register-file topology.
