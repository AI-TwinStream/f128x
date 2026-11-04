# Scheduler Simulation 0.2

**Status:** Minimal executable experiment; intentionally not a microarchitecture model.

## Purpose

The first-order equation in [Context Capacity and Latency Model 0.2](../../docs/architecture/context-capacity-and-latency-model-0.2.md) is useful for framing the problem, but it is not a substitute for a runnable model.

`ContextSweep` is a small Java cycle-level experiment. It tests how context count, issue width, blocking latency, and the fraction of instructions that block affect aggregate issue utilization.

## Model

At each simulated cycle:

1. Threads whose blocking latency has expired become ready.
2. The scheduler selects up to the configured issue width from the ready queue.
3. Each issued instruction independently becomes a long-latency operation with probability `memory-fraction`.
4. A blocking thread becomes ready again after `latency-cycles`.
5. A nonblocking thread becomes eligible again in the next cycle.

The model uses a deterministic seed so a run can be repeated.

## Build and run

Requires **JDK 25 or newer**. Java 25 is the project's baseline for simulator development and CI. The simulator intentionally has no third-party dependencies.

From the repository root:

```sh
mkdir -p build/scheduler-sim
javac --release 25 -Xlint:all -Werror -d build/scheduler-sim tools/scheduler-sim/src/main/java/org/flossware/f128x/sim/ContextSweep.java
java --enable-preview -cp build/scheduler-sim org.flossware.f128x.sim.ContextSweep 4096 4 300 0.25 200000 42
```

Arguments:

```text
<contexts> <issue-width> <latency-cycles> <memory-fraction> <cycles> <seed>
```

Example sweep:

```sh
for contexts in 64 256 512 2048 4096 16384 65536; do
  for latency in 100 300 600 1000; do
    java -cp build/scheduler-sim org.flossware.f128x.sim.ContextSweep \
      "$contexts" 4 "$latency" 0.25 200000 42
  done
done
```

Repeat with issue widths 1 and 8, and memory fractions 0.10, 0.25, 0.50, and 0.75. Preserve the seed when comparing context counts; use multiple seeds when measuring variability.

## Output

The program reports:

- Measured instructions per cycle after a warmup interval.
- Issue utilization relative to configured issue width.
- Number of blocking issues.
- Number of cycles with no ready thread.
- Ready and blocked context counts at the end of the run.

The warmup interval is excluded from measured IPC. The other counters are whole-run diagnostics.

## Deliberate omissions

This is a deliberately narrow first model. It does not represent:

- Multiple outstanding memory operations per thread.
- Cache levels, bandwidth, queues, or memory-level parallelism.
- Dependencies between instructions.
- Different instruction classes or execution-unit constraints.
- Shared-register conflicts or synchronization.
- Scheduler arbitration delay, hierarchy, migration, or work stealing.
- Interrupts, exceptions, priorities, fairness bounds, or OS behavior.
- Physical register-file banking, ports, timing, area, or power.

The model's single FIFO ready queue is a throughput baseline, not a proposed scheduler topology. Its independent blocking probability is an input assumption, not a claim about real workloads.

## Interpretation

Use the output to identify where adding contexts stops improving utilization under a specific parameter set. Do not generalize one run into an architectural requirement. The next model revision should add per-thread outstanding-request limits, multiple latency classes, dependency stalls, shared-state contention, and scheduler-topology alternatives, then calibrate inputs from representative workload traces.

No architectural decision about 65,536 contexts follows from this model alone.
