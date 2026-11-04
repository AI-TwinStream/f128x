# Scheduler Simulation 0.6

**Status:** Executable cycle-level experiment; not a microarchitecture or memory-system performance model.

## Purpose

`ContextSweep` explores how context count, issue width, memory-operation frequency, per-thread outstanding-request limits, memory latency classes, and finite shared request capacity affect issue utilization.

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

The distributed strategy is a logical work-stealing experiment using a global issue-width budget. It does not yet model independent per-cluster issue pipelines, physical arbitration, migration cost, or locality penalties. The hierarchical strategy similarly models workspace fairness, not a concrete hardware arbiter.

Workspace workload skew can be introduced with JVM properties:

- `-Df128x.hotWorkspaceRatio=0.25`: fraction of workspaces assigned the hot profile; the first `ceil(workspaces × ratio)` workspaces are hot.
- `-Df128x.hotMemoryFraction=0.90` and `-Df128x.coldMemoryFraction=0.05`: memory-operation fractions for hot and cold workspaces.
- `-Df128x.hotDependencyFraction=0.20` and `-Df128x.coldDependencyFraction=0.02`: serialized dependency-stall fractions for non-memory instructions.

When omitted, hot and cold fractions both inherit the command-line workload fractions, so the default remains uniform. These controls let the scheduler policies be compared under skewed readiness rather than only identical workspaces.

The default is `fifo`. A deterministic seed makes runs repeatable. Each thread can issue at most one instruction per cycle.

### Memory latency classes

The optional three-class model uses conditional probabilities among memory instructions:

- L1 receives `l1-fraction`.
- L2 receives `l2-fraction`.
- DRAM receives the remaining probability.

Each class has a fixed latency. These classes are labels for latency distributions, not a claim that the model implements real caches, coherence, DRAM banks, or a physical memory controller.

## Build and run

Requires **JDK 25 or newer**. Java 25 is the project's baseline for simulator development and CI. The simulator has no third-party dependencies.

From the repository root:

```sh
mkdir -p build/scheduler-sim
javac --release 25 -Xlint:all -Werror -d build/scheduler-sim tools/scheduler-sim/src/main/java/org/flossware/f128x/sim/ContextSweep.java
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

The dependency fraction applies to non-memory instructions. A selected dependency-bound instruction issues, then makes its thread unavailable for the configured dependency latency. This approximates serialized data-dependency stalls without modelling operands, producer-consumer graphs, or forwarding.

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
- Queue-full admission attempts, accumulated queue-waiter cycles, dependency-waiter cycles, service-backlog cycles, and idle cycles.
- Ready threads, threads waiting at per-thread limits, threads waiting for queue capacity, and outstanding requests at the end.
- Per-class issue and completion counts.
- Scheduler selection count, work-steal count, minimum/maximum workspace issue service, and Jain's raw workspace issue-count fairness index.
- Measured runnable-thread cycles and runnable-workspace cycles per workspace. Both are sampled after wakeups and before issue selection. A runnable-thread cycle counts each ready thread as one service opportunity; a runnable-workspace cycle counts one opportunity when at least one thread in that workspace is ready.
- Instructions issued per runnable-thread-cycle measures service relative to the total ready-thread opportunity. Instructions issued per runnable-workspace-cycle measures aggregate service relative to the time a workspace had any runnable work. Each has its own Jain fairness index. Workspaces with no measured opportunity at the corresponding level are excluded; a workspace with opportunities but no service remains included with a zero rate.
- These metrics answer different questions. Equal service per runnable workspace can still mean unequal service per runnable thread when workspace ready populations differ. The raw issue-count index remains a workload outcome, not a scheduler fairness metric under heterogeneous readiness. All are model-level measures and omit priorities, differing instruction costs, and physical arbitration costs.

The warmup interval is excluded from measured IPC. Other counters are whole-run diagnostics.

## Deliberate omissions

This revision still does not model:

- Operand-level dependency graphs, producer/consumer relationships, forwarding, or multiple independent dependency chains.
- Cache hit/miss generation from addresses or working sets.
- Byte-level bandwidth, banks, channels, row buffers, or request-size-dependent service rates.
- Different request sizes, read/write behavior, coherence, translation, or TLBs.
- Shared-register conflicts or synchronization.
- Independent per-cluster issue pipelines, physical arbitration, migration costs, fairness bounds, or locality penalties.
- Interrupts, exceptions, priorities, or OS behavior.
- Physical register-file banking, ports, timing, area, or power.

The shared queue is a finite count of simultaneously outstanding requests. Queue-full attempts count each failed admission attempt; queue-waiter cycles accumulate the number of threads waiting for a queue slot per cycle. These are distinct measurements. The optional completion rate limits requests completed per cycle, but does not model bytes per cycle, bank conflicts, channels, or request-size effects. Fixed latency classes do not increase with queue occupancy. Dependency stalls are independent of memory requests and apply only to non-memory instructions. The independent memory-operation probability and class distribution are input assumptions, not measured workload properties.

## Interpretation

Use the model to compare controlled scenarios and identify where context count, per-thread MLP, and shared request capacity stop improving issue utilization. Do not generalize one run into an architectural requirement. In particular, a larger queue can improve admission capacity in this model without representing the bandwidth or power cost of sustaining that concurrency.

The next model revision should add independent per-cluster issue budgets, migration/locality costs, byte-oriented bandwidth and bank/channel contention, richer dependency graphs, and calibration from representative workload traces. Keep both runnable-thread-cycle and runnable-workspace-cycle fairness measures when evaluating policy changes. See [Scheduler Simulation 0.6 Results](../../docs/research/scheduler-simulation-0.6-results.md) for the latest comparison.

No architectural decision about 65,536 contexts follows from this model alone.
