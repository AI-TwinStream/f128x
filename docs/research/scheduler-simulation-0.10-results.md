> **Historical simulation note (2026-11-05):** This result set belongs to the f128x scheduler model and its historical parameters. It is retained for reproducibility and is not a family-wide fNx performance claim.

# Scheduler Simulation 0.10 Results

**Date:** 2026-11-04  
**Branch:** `experiment/memory-system-model-0.1`  
**Toolchain:** Temurin JDK 25  
**Status:** Reproducible exploratory results; not hardware performance claims

## 1. Scope

Simulation 0.10 adds variable synthetic request sizes. The existing fixed-size property remains supported:

- `f128x.memoryRequestBytes`: fixed request size, default 64 bytes.
- `f128x.memoryRequestMinBytes` and `f128x.memoryRequestMaxBytes`: optional inclusive bounds. When supplied, each memory request samples a uniform integer size within the range. When omitted, both bounds inherit the fixed-size property.

The selected request size is carried through the memory-system model. It determines issued/completed byte accounting and the bank transfer duration. For each request, the byte service time is rounded up to a whole cycle using the configured channel bandwidth and bank count.

The uniform range is a synthetic workload control, not an assertion that real programs have a uniform memory-access size distribution.

## 2. Fixed-size bandwidth and bank experiment

Parameters:

- 512 contexts; issue width 4.
- Memory-operation fraction 0.50.
- L1/L2/DRAM conditional fractions 0.70/0.20/0.10; latencies 4/20/200.
- Per-thread outstanding limit 4; shared request capacity 128.
- 30,000 cycles; warmup 3,000; seed 19.
- Fixed request size 64 bytes.
- Bounded case: 4 channels, 8 banks/channel, 16 bytes/cycle/channel, 2-cycle bank recovery penalty.

| Metric | Memory model disabled | Bounded memory |
|---|---:|---:|
| Measured IPC | 4.0000 | 1.3879 |
| Memory requests issued | 60,119 | 20,876 |
| Memory requests completed | 60,074 | 20,748 |
| Bytes issued | 3,847,616 | 1,336,064 |
| Bytes completed | 3,844,736 | 1,327,872 |
| Bank service cycles (sum across requests) | 0 | 668,032 |
| Requests delayed by bank contention | 0 | 17,352 |
| Bank-conflict wait cycles (sum across requests) | 0 | 2,623,260 |
| Maximum outstanding requests on one bank | 0 | 18 |
| Queue-full admission attempts | 0 | 21,340 |

The bounded configuration has 64 bytes/cycle of aggregate configured bandwidth. Byte accounting is exact: fixed-size issued and completed byte totals equal their corresponding request counts multiplied by 64.

## 3. Variable-size request experiment

Parameters:

- Same 512-context, four-wide, 50%-memory workload, seed 19, 30,000 cycles, and 3,000-cycle warmup.
- Request sizes sampled uniformly from 16 through 256 bytes, inclusive.
- 4 channels, 8 banks/channel, 16 bytes/cycle/channel.
- No additional bank recovery penalty.

| Metric | Result |
|---|---:|
| Configured request-size range | 16–256 bytes |
| Observed minimum request | 16 bytes |
| Observed maximum request | 256 bytes |
| Measured IPC | 0.7507 |
| Memory requests issued | 11,533 |
| Memory requests completed | 11,405 |
| Bytes issued | 1,572,096 |
| Bytes completed | 1,553,248 |
| Bank service cycles (sum across requests) | 788,946 |
| Requests delayed by bank contention | 9,725 |
| Bank-conflict wait cycles (sum across requests) | 2,767,501 |
| Maximum outstanding requests on one bank | 23 |
| Queue-full admission attempts | 11,908 |
| Queue-waiter cycles | 15,262,670 |

The run exercised both ends of the configured request-size range. Variable sizes feed directly into transfer-cycle calculation and byte accounting. The fixed-size and variable-size rows are separate configurations, not a controlled one-variable comparison: the fixed-size run includes a two-cycle bank recovery penalty while the variable-size run does not.

## 4. Interpretation and limitations

- Request sizes are sampled uniformly as integer byte counts. Real workloads need not follow this distribution.
- Synthetic addresses still determine channel and bank assignment. No real address trace or configurable address-bit mapping is used.
- Bandwidth is divided equally among banks; each bank serializes its own requests.
- `memoryServiceCycles` and `bankConflictWaitCycles` are sums across requests, not wall-clock cycle counts.
- The model does not yet distinguish reads from writes, model burst alignment, DRAM row locality, refresh, or a memory controller's queue scheduling policy.

No architectural decision about request sizes, channel count, bank count, or memory bandwidth follows from these synthetic experiments.

## 5. Next work

Add configurable address-to-channel/bank mapping and richer dependency graphs. Then calibrate request-size distributions, address locality, and memory-operation mix from representative workload traces. Preserve fixed-size mode as a control for isolating the effect of request-size variability.
