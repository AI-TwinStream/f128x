# Scheduler Simulation 0.4

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
8. A thread may continue issuing while it has fewer than `max-outstanding` accepted memory requests in flight.

The scheduler uses one FIFO ready queue and one FIFO queue-credit wait list. A deterministic seed makes runs repeatable. Each thread can issue at most one instruction per cycle.

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

Arguments:

```text
<contexts> <issue-width> <default-latency>
<memory-fraction> <cycles> <seed>
[max-outstanding [memory-queue-capacity
[l1-fraction l1-latency l2-fraction l2-latency dram-latency]]]
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
- Queue-credit stalls and idle cycles.
- Ready threads, threads waiting at per-thread limits, threads waiting for queue capacity, and outstanding requests at the end.
- Per-class issue and completion counts.

The warmup interval is excluded from measured IPC. Other counters are whole-run diagnostics.

## Deliberate omissions

This revision still does not model:

- Instruction dependencies or dependency chains.
- Cache hit/miss generation from addresses or working sets.\n- Operand-level dependency graphs, producer/consumer relationships, forwarding, or multiple independent dependency chains.
- Memory bandwidth, banks, channels, row buffers, arbitration, or service-rate limits.
- Different request sizes, read/write behavior, coherence, translation, or TLBs.
- Shared-register conflicts or synchronization.
- Scheduler arbitration delay, hierarchy, migration, fairness, or work stealing.
- Interrupts, exceptions, priorities, or OS behavior.
- Physical register-file banking, ports, timing, area, or power.

The shared queue is a finite count of simultaneously outstanding requests. It is not a bandwidth model. Fixed latency classes do not increase with queue occupancy. Dependency stalls are independent of memory requests and apply only to non-memory instructions. The independent memory-operation probability and class distribution are input assumptions, not measured workload properties.

## Interpretation

Use the model to compare controlled scenarios and identify where context count, per-thread MLP, and shared request capacity stop improving issue utilization. Do not generalize one run into an architectural requirement. In particular, a larger queue can improve admission capacity in this model without representing the bandwidth or power cost of sustaining that concurrency.

The next model revision should add explicit memory service-rate or bandwidth limits, richer dependency graphs, and scheduler-topology alternatives, then calibrate inputs from representative workload traces.

No architectural decision about 65,536 contexts follows from this model alone.
