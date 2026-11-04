# Scheduler Simulation 0.11

**Status:** Executable cycle-level experiment; not a microarchitecture or memory-system performance model.

## Purpose

`ContextSweep` explores how context count, issue width, memory-operation frequency, per-thread outstanding-request limits, memory latency classes, finite shared request capacity, per-cluster issue budgets, and optional byte-oriented bank service affect issue utilization.

The model is intended to expose sensitivities and falsify simplistic assumptions. It does not predict the performance of a physical f128x implementation.

## Model

At each simulated cycle:

1. Completed memory requests release per-thread and shared queue capacity.
2. Threads blocked by a per-thread outstanding-request limit become eligible when a completion frees a slot.
3. Threads waiting to submit a memory instruction are reconsidered when a completion releases shared queue capacity.
4. The scheduler visits threads that were ready at the beginning of the cycle, issuing at most one instruction per thread and no more than the configured issue width.
5. Each selected instruction is a memory operation with probability `memory-fraction`.
6. Memory operations are assigned to one of the configured latency classes.
7. An operation is issued only when the shared outstanding-request capacity has a free slot. Otherwise, that selected memory instruction waits for queue credit and does not consume an issue slot.
8. Completed requests are limited by an optional maximum number of completions per cycle. Requests that reach their nominal latency but cannot complete remain outstanding and contribute service-backlog cycles.
9. A non-memory instruction may optionally incur a serialized dependency stall, controlled by `dependency-fraction` and `dependency-latency`. This is a coarse stall model, not a dependency graph.
10. A thread may continue issuing while it has fewer than `max-outstanding` accepted memory requests in flight.

The scheduler strategy is selected with the JVM property `-Df128x.scheduler=`:

- `fifo`: one flat FIFO ready queue.
- `hierarchical`: per-workspace FIFO queues selected round-robin; each workspace owns 16 thread slots.
- `distributed`: per-cluster queues, with workspaces initially assigned by workspace ID modulo cluster count. The scheduler scans clusters round-robin and steals from another cluster when its target queue is empty. Set the cluster count with `-Df128x.clusters=8`; the default is up to eight clusters.

The distributed strategy uses a round-robin cluster selector, prefers each target cluster's local ready queue, and steals from another cluster when the target is empty. The global `issue-width` remains a hard aggregate ceiling. Set the per-cluster issue limit with `-Df128x.clusterIssueWidth=2`; it defaults to the global issue width. The effective configured capacity is `min(issue-width, clusters × clusterIssueWidth)`. For example, four clusters with a local width of two can issue at most four instructions per cycle when the global issue width is four. Two clusters with a local width of one can issue at most two, even when the global width is four.

This is a logical per-cluster budget experiment, not a model of physically independent pipelines, distributed arbitration networks, clock/power domains, or cluster-local execution-unit inventories. Work stealing changes a thread's logical owner cluster, but migration and locality costs are not charged. The FIFO and hierarchical policies each use one logical cluster; hierarchical scheduling models workspace selection, not a concrete hardware arbiter.

Workspace workload skew can be introduced with JVM properties:

- `-Df128x.hotWorkspaceRatio=0.25`: fraction of workspaces assigned the hot profile; the first `ceil(workspaces × ratio)` workspaces are hot.
- `-Df128x.hotMemoryFraction=0.90` and `-Df128x.coldMemoryFraction=0.05`: memory-operation fractions for hot and cold workspaces.
- `-Df128x.hotDependencyFraction=0.20` and `-Df128x.coldDependencyFraction=0.02`: serialized dependency-stall fractions for non-memory instructions.

When omitted, hot and cold fractions both inherit the command-line workload fractions, so the default remains uniform. These controls let the scheduler policies be compared under skewed readiness rather than only identical workspaces.

Distributed scheduling can model migration and remote access penalties:

- `-Df128x.migrationLatency=3`: cycles a stolen thread waits before it becomes eligible on its new owner cluster; default 0.
- `-Df128x.remoteMemoryLatencyPenalty=7`: extra cycles added to memory-request latency while a thread is remote from its workspace's home cluster; default 0.

A workspace's home cluster is its workspace ID modulo cluster count. A thread becomes remote when work stealing moves it away from that home cluster and becomes local again if it migrates back. The simulator counts migrations, remote wakeups, remote thread-cycles, remote memory issues, added remote-memory latency, and migration-waiter cycles. The remote-memory penalty is applied to every memory operation while remote; this is a deliberately coarse proxy for workspace-local state access, not a cache-coherence or NUMA model.

A byte-oriented memory service model can be enabled with these JVM properties:

- `-Df128x.memoryRequestBytes=64`: fixed bytes per memory request; default 64.
- `-Df128x.memoryRequestMinBytes=16` and `-Df128x.memoryRequestMaxBytes=256`: optional inclusive request-size range. When omitted, both inherit `memoryRequestBytes`; sizes are sampled uniformly from the configured range.
- `-Df128x.memoryChannels=4`: number of independent memory channels; default 1.
- `-Df128x.memoryBanksPerChannel=8`: banks per channel; default 1.
- `-Df128x.memoryBandwidthBytesPerCycle=16`: byte service budget per channel per cycle; default 0 (unbounded/disabled).
- `-Df128x.bankConflictPenalty=2`: extra cycles a bank remains unavailable after servicing a request; default 0.

The memory-system model is disabled by default to preserve the earlier fixed-latency behavior. It becomes active when bandwidth is configured, channel or bank count exceeds one, or a bank-conflict penalty is set. When active, requests are assigned to channels and banks using configurable synthetic addresses. The default pattern remains random. Select a pattern with `-Df128x.addressPattern=`:
- `random`: seeded pseudo-random byte addresses (default).
- `sequential`: cache-line sequential addresses in a per-thread region.
- `strided`: per-thread addresses separated by `-Df128x.addressStride=4096` bytes (default 64).
- `shared_region`: threads generate addresses in the same bounded region, exposing inter-thread overlap; configure its size with `-Df128x.addressRegionBytes=1048576` (default 1 MiB).

Address-to-channel/bank mapping is selected with `-Df128x.addressMapping=`:
- `low_order`: interleave 64-byte cache lines across channels, then banks (default).
- `xor`: fold higher cache-line address bits into channel and bank selection to test whether structured strides can be dispersed.

The random pattern consumes the same seeded random value as the previous implementation, preserving the random workload stream. Sequential, strided, and shared-region patterns are deterministic for a given thread and its memory-request sequence. Bandwidth is divided evenly among a channel's banks; each bank services one request at a time. The transfer time is `ceil(requestBytes × banksPerChannel / bandwidthBytesPerCycle)` cycles when a bandwidth limit is configured. With bandwidth disabled but bank contention enabled, each bank services one request per cycle. Aggregate configured bandwidth is `memoryChannels × memoryBandwidthBytesPerCycle`.

This is a first-order bank service model. It does not consume real address traces or model burst transfers, DRAM row hits, refresh, read/write turnarounds, or a physical controller's scheduling policy.

The default is `fifo`. A deterministic seed makes runs repeatable. Each thread can issue at most one instruction per cycle.

### Memory latency classes

The optional three-class model uses conditional probabilities among memory instructions:

- L1 receives `l1-fraction`.
- L2 receives `l2-fraction`.
- DRAM receives the remaining probability.

Each class has a fixed latency. These classes are labels for latency distributions, not a claim that the model implements real caches, coherence, DRAM row/bank timing, or a physical memory controller. The optional bank model described above adds abstract bank serialization and byte service, not physical DRAM timing.

## Build and run

Requires **JDK 25 or newer**. Java 25 is the project's baseline for simulator development and CI. The simulator has no third-party dependencies.

From the repository root:

```sh
mkdir -p build/scheduler-sim
javac --release 25 -Xlint:all -Werror -d build/scheduler-sim \
  tools/scheduler-sim/src/main/java/org/flossware/f128x/sim/ContextSweep.java \
  tools/scheduler-sim/src/main/java/org/flossware/f128x/sim/MemoryAddressing.java
javac --release 25 -Xlint:all -Werror -cp build/scheduler-sim -d build/scheduler-sim \
  tools/scheduler-sim/src/test/java/org/flossware/f128x/sim/MemoryAddressingTest.java
java -cp build/scheduler-sim org.flossware.f128x.sim.MemoryAddressingTest
```

Single-latency compatibility mode:

```sh
java -cp build/scheduler-sim org.flossware.f128x.sim.ContextSweep 4096 4 300 0.25 200000 42 1
```

Finite queue with three latency classes:

```sh
java -cp build/scheduler-sim org.flossware.f128x.sim.ContextSweep 4096 4 300 0.25 200000 42 4 128 0.70 4 0.20 20 200
```

Add the optional dependency fraction and latency after the five memory-class arguments:

```sh
java -cp build/scheduler-sim org.flossware.f128x.sim.ContextSweep 4096 4 300 0.25 200000 42 4 128 0.70 4 0.20 20 200 0.10 12
```

The dependency fraction applies to non-memory instructions. A selected dependency-bound instruction issues and makes its selected dependency chain unavailable for the configured latency. Set `-Df128x.dependencyChains=4` to give each thread four independent synthetic chains; the default is one and the maximum is 128. If another chain is ready, the thread can remain eligible and issue from it. If all chains are busy, the thread waits until the earliest chain becomes ready. The output reports dependency issues overlapped by another ready chain as well as dependency-waiter cycles. This models independent chains, not operand-level producer/consumer graphs, register renaming, forwarding, or real instruction dependencies.

To limit memory request completions to one per cycle, append `1` after the memory-class arguments (14 arguments total). This delays completions that are ready but cannot be serviced in the current cycle. Append the same value after the dependency pair when both options are enabled (16 arguments total). The limit is requests completed per cycle, not bytes transferred per cycle.

Arguments:

```text
<contexts> <issue-width> <default-latency>
<memory-fraction> <cycles> <seed>
[max-outstanding [memory-queue-capacity
[l1-fraction l1-latency l2-fraction l2-latency dram-latency
[dependency-fraction dependency-latency [max-memory-completions-per-cycle]]]]
To specify a completion rate without dependency stalls, append only `max-memory-completions-per-cycle` immediately after the memory-class arguments (14 arguments total).
```

The original six-argument invocation remains valid. `max-outstanding` defaults to 1. The shared memory queue defaults to effectively unbounded capacity unless explicitly configured. Supply all five latency-class arguments together.

Example context and per-thread MLP sweep:

```sh
for outstanding in 1 2 4 8; do
  for contexts in 64 128 256 512 1024; do
    java -cp build/scheduler-sim org.flossware.f128x.sim.ContextSweep \
      "$contexts" 4 300 0.25 200000 42 "$outstanding" 128
  done
done
```

Compare multiple seeds and vary issue width, memory fraction, class probabilities, latencies, and queue capacity. Preserve all other parameters when comparing one dimension.

## Output

The program reports:

- Measured IPC and issue utilization after warmup.
- Accepted memory requests and completions.
- Maximum per-thread outstanding requests.
- Maximum shared request-queue occupancy.
- Queue-full admission attempts, accumulated queue-waiter cycles, dependency-waiter cycles, dependency issues overlapped by another chain, dependency-stall wakeups, threads still waiting on dependencies, service-backlog cycles, and idle cycles.
- Ready threads, threads waiting at per-thread limits, threads waiting for queue capacity, and outstanding requests at the end.
- Per-class issue and completion counts.
- Scheduler selection count, work-steal count, minimum/maximum workspace issue service, and Jain's raw workspace issue-count fairness index.
- Configured per-cluster issue width, effective aggregate issue capacity, utilization against that capacity, and per-cluster issued-instruction totals.
- Scheduler migrations, remote wakeups, remote thread-cycles, remote memory issues and added latency, migration-waiter cycles, and completed migrations.
- Fixed request bytes issued/completed, configured channel bandwidth, bank service cycles, bank-conflict requests and wait cycles, and maximum outstanding requests per bank.
- Measured runnable-thread cycles and runnable-workspace cycles per workspace. Both are sampled after wakeups and before issue selection. A runnable-thread cycle counts each ready thread as one service opportunity; a runnable-workspace cycle counts one opportunity when at least one thread in that workspace is ready.
- Instructions issued per runnable-thread-cycle measures service relative to the total ready-thread opportunity. Instructions issued per runnable-workspace-cycle measures aggregate service relative to the time a workspace had any runnable work. Each has its own Jain fairness index. Workspaces with no measured opportunity at the corresponding level are excluded; a workspace with opportunities but no service remains included with a zero rate.
- These metrics answer different questions. Equal service per runnable workspace can still mean unequal service per runnable thread when workspace ready populations differ. The raw issue-count index remains a workload outcome, not a scheduler fairness metric under heterogeneous readiness. All are model-level measures and omit priorities, differing instruction costs, and physical arbitration costs.

The warmup interval is excluded from measured IPC. Other counters are whole-run diagnostics.

## Deliberate omissions

This revision still does not model:

- Operand-level dependency graphs, producer/consumer relationships, forwarding, or multiple independent dependency chains.
- Cache hit/miss generation from working sets or real address traces; channel/bank assignment now supports random, sequential, strided, and shared-region synthetic addresses with low-order or XOR mapping, but no working-set reuse yet.
- DRAM row buffers, row hits, refresh, burst transfers, read/write turnarounds, or controller scheduling policy.
- Variable request sizes, read/write behavior, coherence, translation, or TLBs. The current byte model uses a uniform request-size range and an equal per-bank share of channel bandwidth; it does not model workload-specific size distributions.
- Shared-register conflicts or synchronization.
- Physically independent per-cluster issue pipelines, distributed physical arbitration, network/clock/power costs, detailed cache-coherence or NUMA behavior, and fairness bounds. Migration delay and remote-memory latency are configurable aggregate penalties only.
- Interrupts, exceptions, priorities, or OS behavior.
- Physical register-file banking, ports, timing, area, or power.

The shared queue is a finite count of simultaneously outstanding requests. Queue-full attempts count each failed admission attempt; queue-waiter cycles accumulate the number of threads waiting for a queue slot per cycle. These are distinct measurements. The optional completion rate limits requests completed per cycle independently of the byte-bandwidth and bank-service model. The byte model uses fixed-size requests and equal per-bank bandwidth shares; it does not model variable request sizes or detailed controller arbitration. Fixed latency classes do not increase with queue occupancy. Dependency stalls are independent of memory requests and apply only to non-memory instructions. The independent memory-operation probability and class distribution are input assumptions, not measured workload properties.

## Interpretation

Use the model to compare controlled scenarios and identify where context count, per-thread MLP, and shared request capacity stop improving issue utilization. Do not generalize one run into an architectural requirement. In particular, a larger queue can improve admission capacity in this model without representing the bandwidth or power cost of sustaining that concurrency.

The next model revision should add working-set reuse and cache hit/miss behavior, richer dependency graphs, and calibration from representative workload traces. Keep both runnable-thread-cycle and runnable-workspace-cycle fairness measures when evaluating policy changes. See [Scheduler Simulation 0.6 Results](../../docs/research/scheduler-simulation-0.6-results.md), [Scheduler Simulation 0.7 Results](../../docs/research/scheduler-simulation-0.7-results.md), [Scheduler Simulation 0.8 Results](../../docs/research/scheduler-simulation-0.8-results.md), and [Scheduler Simulation 0.9 Results](../../docs/research/scheduler-simulation-0.9-results.md), and [Scheduler Simulation 0.10 Results](../../docs/research/scheduler-simulation-0.10-results.md), and [Scheduler Simulation 0.11 Results](../../docs/research/scheduler-simulation-0.11-results.md).

No architectural decision about 65,536 contexts follows from this model alone.


## Modular board and backplane experiment

A separate Java 25 entry point, `BoardFabricSweep`, compares modular CPU-board topologies:

- 1 board × 8 clusters × 1 issue slot.
- 2 boards × 4 clusters × 1 issue slot.
- 4 boards × 2 clusters × 1 issue slot.
- 8 boards × 1 cluster × 1 issue slot.
- 4 boards × 4 clusters × 1 issue slot (a 16-wide scale-up case).

The first four hold aggregate issue capacity at eight instructions per cycle. The experiment sweeps 10%, 50%, and 90% remote-memory traffic through a shared bandwidth-limited fabric. Each board has a serialized local-memory service lane with configurable bandwidth; remote requests consume the full-duplex request/response fabric and the destination board's memory service. Workspaces are homed to boards, and threads remain on their home board. The model reports IPC, utilization, per-board issue counts, remote requests, fabric bytes, queue pressure, and idle issue slots.

Compile both simulators and run the board experiment:

```sh
javac --release 25 -Xlint:all -Werror -d build/scheduler-sim \
  tools/scheduler-sim/src/main/java/org/flossware/f128x/sim/ContextSweep.java \
  tools/scheduler-sim/src/main/java/org/flossware/f128x/sim/MemoryAddressing.java \
  tools/scheduler-sim/src/main/java/org/flossware/f128x/sim/BoardFabricSweep.java
java -cp build/scheduler-sim org.flossware.f128x.sim.BoardFabricSweep
```

The board model is intentionally separate from `ContextSweep`. Its first revision does not model local-memory channels/banks, parallel fabric links, cache coherence, migration, or physical board costs. See [Board and Backplane Simulation 0.1](../../docs/research/board-backplane-simulation-0.1.md) for assumptions and limitations.
