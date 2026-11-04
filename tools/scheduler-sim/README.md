# Scheduler Simulation 0.3

**Status:** Minimal executable experiment; intentionally not a microarchitecture model.

## Purpose

The first-order equation in [Context Capacity and Latency Model 0.2](../../docs/architecture/context-capacity-and-latency-model-0.2.md) is useful for framing the problem, but it is not a substitute for a runnable model.

`ContextSweep` is a small Java cycle-level experiment. It tests how context count, issue width, memory latency, memory-operation fraction, and per-thread memory-level parallelism affect aggregate issue utilization.

## Model

At each simulated cycle:

1. Threads whose memory requests have completed are reconsidered for readiness.
2. The scheduler selects up to the configured issue width from the ready queue.
3. Each issued instruction independently becomes a long-latency memory operation with probability `memory-fraction`.
4. A thread may continue issuing while it has fewer than `max-outstanding` requests in flight.
5. Once it reaches that limit, it waits until a request completes and frees a slot.
6. A non-memory instruction makes the thread eligible again in the next cycle.

The model uses a deterministic seed so a run can be repeated. Each thread can issue at most one instruction per cycle.

## Build and run

Requires **JDK 25 or newer**. Java 25 is the project's baseline for simulator development and CI. The simulator intentionally has no third-party dependencies.

From the repository root:

```sh
mkdir -p build/scheduler-sim
javac --release 25 -Xlint:all -Werror -d build/scheduler-sim tools/scheduler-sim/src/main/java/org/flossware/f128x/sim/ContextSweep.java
java -cp build/scheduler-sim org.flossware.f128x.sim.ContextSweep 4096 4 300 0.25 200000 42 1
```

Arguments:

```text
<contexts> <issue-width> <latency-cycles> <memory-fraction> <cycles> <seed> [max-outstanding]
```

If omitted, `max-outstanding` defaults to 1, reproducing the original blocking-thread model.

Example sweep:

```sh
for contexts in 64 256 512 2048 4096 16384 65536; do
  for latency in 100 300 600 1000; do
    java -cp build/scheduler-sim org.flossware.f128x.sim.ContextSweep \
      "$contexts" 4 "$latency" 0.25 200000 42 1
  done
done
```

Repeat with issue widths 1 and 8, memory fractions 0.10, 0.25, 0.50, and 0.75, and per-thread outstanding limits such as 1, 2, 4, and 8. Preserve the seed when comparing context counts; use multiple seeds when measuring variability.

## Output

The program reports:

- Measured instructions per cycle after a warmup interval.
- Issue utilization relative to configured issue width.
- Number of memory requests issued and completed.
- Maximum observed outstanding requests for any thread.
- Number of cycles with no ready thread.
- Ready threads, threads waiting at their outstanding-request limit, and outstanding requests at the end of the run.

The warmup interval is excluded from measured IPC. The other counters are whole-run diagnostics.

## Deliberate omissions

This is a deliberately narrow model. It does not represent:

- Cache levels, bandwidth, shared memory queues, or multiple memory latency classes.
- Dependencies between instructions.
- Different instruction classes or execution-unit constraints.
- Shared-register conflicts or synchronization.
- Scheduler arbitration delay, hierarchy, migration, or work stealing.
- Interrupts, exceptions, priorities, fairness bounds, or OS behavior.
- Physical register-file banking, ports, timing, area, or power.

The model's single FIFO ready queue is a throughput baseline, not a proposed scheduler topology. Memory requests have one fixed latency and no shared queue or bandwidth limit. The independent memory-operation probability and per-thread outstanding limit are input assumptions, not claims about real workloads.

## Interpretation

Use the output to identify where adding contexts or per-thread memory-level parallelism stops improving utilization under a specific parameter set. Do not generalize one run into an architectural requirement. The next model revision should add multiple latency classes, dependency stalls, shared-state contention, and scheduler-topology alternatives, then calibrate inputs from representative workload traces.

No architectural decision about 65,536 contexts follows from this model alone.
