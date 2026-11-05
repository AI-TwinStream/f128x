> **Historical simulation note (2026-11-05):** This result set belongs to the f128x scheduler model and its historical parameters. It is retained for reproducibility and is not a family-wide fNx performance claim.

# Scheduler Simulation 0.9 Results

**Date:** 2026-11-04  
**Branch:** `experiment/memory-system-model-0.1`  
**Toolchain:** Temurin JDK 25  
**Status:** Reproducible exploratory results; not hardware performance claims

## 1. Scope

Simulation 0.9 adds fixed-size memory requests, per-channel byte bandwidth, bank-level serialization, and configurable bank recovery penalties.

The memory-system model is disabled by default so the prior fixed-latency experiments retain their behavior. It is enabled when bandwidth is configured, channel or bank count exceeds one, or a bank-conflict penalty is set. When active, each memory request is assigned a deterministic synthetic address and mapped to one channel and one bank.

Configured channel bandwidth is divided evenly among the channel's banks. Each bank services one request at a time. For a request of (R) bytes, (B) banks per channel, and channel bandwidth (W) bytes per cycle, the modeled transfer duration is:

`transferCycles = ceil(R × B / W)`

A configured bank-conflict penalty keeps that bank unavailable for additional cycles after service. If bank contention is enabled without a byte-bandwidth limit, each bank services one request per cycle.

This is a first-order service model, not a DRAM controller, cache hierarchy, or address-trace simulator.

## 2. Experiment

Common parameters:

- 512 contexts; issue width 4.
- Memory-operation fraction 0.50.
- L1/L2/DRAM conditional fractions 0.70/0.20/0.10; latencies 4/20/200 cycles.
- Per-thread outstanding limit 4; shared request capacity 128.
- 30,000 cycles; warmup 3,000; seed 19.
- Fixed request size 64 bytes.

Bounded-memory configuration:

- 4 independent memory channels.
- 8 banks per channel.
- 16 bytes/cycle per channel, or 64 bytes/cycle aggregate.
- 2-cycle bank recovery penalty.
- Each bank receives an equal share of its channel's bandwidth: 2 bytes/cycle.
- A 64-byte request therefore requires 32 service cycles per bank, plus the configured recovery penalty before the next request can use that bank.

## 3. Results

| Metric | Memory model disabled | Byte bandwidth and banks enabled |
|---|---:|---:|
| Measured IPC | 4.0000 | 1.3879 |
| Issue utilization | 100.00% | 34.70% |
| Memory requests issued | 60,119 | 20,876 |
| Memory requests completed | 60,074 | 20,748 |
| Bytes issued | 3,847,616 | 1,336,064 |
| Bytes completed | 3,844,736 | 1,327,872 |
| Bank service cycles (sum across requests) | 0 | 668,032 |
| Requests delayed by bank contention | 0 | 17,352 |
| Bank-conflict wait cycles (sum across requests) | 0 | 2,623,260 |
| Maximum outstanding requests on one bank | 0 | 18 |
| Queue-full admission attempts | 0 | 21,340 |
| Queue-waiter cycles | 0 | 15,227,507 |
| Maximum shared queue occupancy | 81 | 128 |

The byte accounting is exact: issued bytes equal issued requests multiplied by 64, and completed bytes equal completed requests multiplied by 64. The aggregate configured channel bandwidth is 64 bytes per cycle.

The bounded case reduced IPC substantially compared with the same workload with the memory-system model disabled. The reduction reflects the combined effects of finite per-bank service time, bank conflicts, bank recovery, and the shared request queue reaching capacity. It is not possible to attribute the full difference to bandwidth alone from this one combined run.

## 4. Interpretation and limitations

- The channel bandwidth is divided equally among banks. This is a modeling choice, not a claim about a physical memory controller's allocation policy.
- Each bank has one serialized request stream. Different banks can service requests concurrently, subject to their equal share of channel bandwidth.
- Bank selection uses synthetic random addresses. There are no real addresses, working sets, cache hits, row buffers, row hits, refresh, burst transfers, or read/write turnarounds.
- Request size is fixed at 64 bytes in this experiment. Variable-size requests are not yet modeled.
- `memoryServiceCycles` is the sum of modeled per-request transfer durations, not wall-clock cycles.
- `bankConflictWaitCycles` is the sum of per-request waits behind an occupied bank, not wall-clock cycles.
- Request completion throughput caps remain independent of byte bandwidth. They can be combined to explore multiple bottlenecks, but the model does not yet represent a complete controller pipeline.

No architectural decision about memory channel count, bank count, request size, or sustained bandwidth follows from this synthetic experiment.

## 5. Next work

Add variable request sizes and an explicit address-to-channel/bank mapping policy. Then introduce richer dependency graphs and calibrate the request mix, addresses, and sizes from representative workload traces. Preserve the existing fixed-latency mode as a control so each added memory constraint can be measured independently.
