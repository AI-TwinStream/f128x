# Scheduler Simulation 0.4 Results

**Date:** 2026-11-04  
**Branch:** `experiment/memory-system-model-0.1`  
**Toolchain:** Temurin JDK 25  
**Status:** Reproducible exploratory results; not hardware performance claims

## 1. Purpose

This report records the first experiments after extending `ContextSweep` with memory latency classes, finite shared outstanding-request capacity, queue-credit backpressure, and a coarse serialized dependency-stall model.

The simulator is deliberately not a cache hierarchy, memory controller, bandwidth model, or cycle-accurate implementation of an f128x core.

## 2. Multi-seed context and MLP sweep

Fixed parameters:

- Issue width: 4 instructions/cycle
- Memory-operation fraction: 0.25
- Single memory latency: 300 cycles
- Cycles: 100,000
- Warmup: 10,000 cycles
- Seeds: 1, 7, 42, 101
- Shared request capacity: effectively unbounded
- Dependency fraction: 0

For each seed and per-thread outstanding-request limit, the first tested context count reaching 4.0000 measured IPC was identical:

| Max outstanding requests per thread | First tested saturation point | Result across all four seeds |
|---:|---:|---|
| 1 | 512 contexts | 512 for seeds 1, 7, 42, 101 |
| 2 | 256 contexts | 256 for seeds 1, 7, 42, 101 |
| 4 | 128 contexts | 128 for seeds 1, 7, 42, 101 |
| 8 | 64 contexts | 64 for seeds 1, 7, 42, 101 |

This is useful evidence that the trend is not an artifact of seed 42. It remains specific to the simulator's independent memory-operation process, fixed latency, unconstrained shared request capacity, and tested parameter range. It does not establish that real workloads can expose eight independent outstanding misses per thread.

## 3. Finite shared request-capacity stress case

Parameters:

- 512 contexts, issue width 4
- Memory-operation fraction: 0.50
- Per-thread outstanding limit: 4
- Shared outstanding-request capacity: 8
- Latency classes: L1 70% at 4 cycles, L2 20% at 20 cycles, DRAM 10% at 200 cycles
- Cycles: 50,000; warmup: 5,000; seed: 19

Measured result:

| Metric | Result |
|---|---:|
| IPC | 0.5780 |
| Issue utilization | 14.45% |
| Accepted memory requests | 14,744 |
| Completed requests | 14,736 |
| Maximum shared queue occupancy | 8 |
| Queue-full admission attempts | 15,245 |
| Accumulated queue-waiter cycles | 25,523,328 |
| Idle cycles | 27,522 |
| Threads waiting for queue capacity at end | 512 |

The queue occupancy never exceeded its configured capacity. This experiment demonstrates the impact of a severe shared outstanding-request bottleneck in the model. It is not a bandwidth result: the current model limits the number of simultaneously outstanding requests but does not model request service rate, bytes per cycle, banks, channels, or queue-dependent latency.

The queue-waiter cycle metric is the sum, over cycles, of the number of threads waiting for a queue credit. Queue-full admission attempts count individual unsuccessful admissions. They are different measures.

## 4. Latency classes and serialized dependency stalls

Parameters:

- 1,024 contexts, issue width 4
- Memory-operation fraction: 0.25
- Per-thread outstanding limit: 4
- Shared outstanding-request capacity: 128
- L1: 70% of memory operations, 4-cycle latency
- L2: 20%, 20-cycle latency
- DRAM: 10%, 200-cycle latency
- Dependency fraction: 0.10 of non-memory instructions
- Dependency latency: 12 cycles
- Cycles: 100,000; warmup: 10,000; seed: 42

Measured result:

| Metric | Result |
|---|---:|
| IPC | 4.0000 |
| Issue utilization | 100.00% |
| Memory requests issued | 100,430 |
| L1 requests | 70,315 |
| L2 requests | 20,064 |
| DRAM requests | 10,051 |
| Serialized dependency stalls issued | 29,921 |
| Accumulated dependency-waiter cycles | 359,030 |

This case confirms that the multi-class and dependency paths execute and that the configured issue width remains saturated with 1,024 contexts. It does not tell us the minimum context count for this mixed workload. The dependency mechanism is intentionally coarse: it stalls a thread after a selected non-memory instruction for a fixed latency. It does not model register operands, producer-consumer graphs, forwarding, or multiple independent dependency chains.

## 5. Memory completion-rate stress case

Parameters:

- 512 contexts, issue width 4
- Memory-operation fraction: 0.50
- Per-thread outstanding limit: 4
- Shared outstanding-request capacity: 128
- Maximum memory completions: 1 request/cycle
- Latency classes: L1 70% at 4 cycles, L2 20% at 20 cycles, DRAM 10% at 200 cycles
- Cycles: 50,000; warmup: 5,000; seed: 19

Measured result:

| Metric | Result |
|---|---:|
| IPC | 1.9953 |
| Issue utilization | 49.88% |
| Accepted memory requests | 50,124 |
| Completed requests | 49,996 |
| Maximum shared queue occupancy | 128 |
| Queue-full admission attempts | 50,394 |
| Accumulated queue-waiter cycles | 25,371,347 |
| Cycles with due requests waiting for completion service | 49,996 |

This introduces a per-cycle completion-throughput limit. Requests whose nominal latency has elapsed remain outstanding until a completion slot is available. The experiment demonstrates how a completion bottleneck can constrain throughput and hold shared request capacity occupied. The rate is measured in requests per cycle, not bytes per cycle; it is not a DRAM bandwidth model.

## 6. Model semantics and remaining gaps

The shared request capacity is a limit on accepted outstanding transactions. Queue waiters retain a selected memory operation and wait for a returned credit. Only as many waiters as there are available credits are reactivated, in FIFO order.

The current model still assumes fixed latency per class, independent of occupancy. Therefore it does not represent queueing delay within a memory service system. The next extension should add a bounded request-admission/service rate and a queue whose waiting time emerges from offered load and service capacity. After that, dependency modelling should move from an independent probability to explicit producer-consumer dependencies.

Further work:

1. Add a configurable memory request acceptance/service rate.
2. Measure request queue depth, admission delay, service latency, and completion latency separately.
3. Add explicit dependency chains and independent instruction streams.
4. Compare FIFO, hierarchical workspace/thread, and distributed-ready-queue scheduling.
5. Measure fairness, per-thread service distribution, starvation, and scheduler work.
6. Expand seed and workload profiles; eventually calibrate with representative traces.

No architectural decision about the 65,536-context target follows from these experiments.
