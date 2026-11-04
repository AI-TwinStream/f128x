# Board and Backplane Simulation 0.1

**Status:** Initial cycle-level comparative model. Results are exploratory, not hardware predictions.

## Question

Does distributing a fixed amount of execution capacity across multiple CPU boards provide a useful modular architecture, or does inter-board traffic erase the benefit?

This experiment models a board as a collection of execution clusters with workspace-local memory. Workspaces are assigned a home board. Threads remain on that board in this first revision. Remote memory requests traverse a shared, bandwidth-limited backplane fabric.

## Topologies

The first four configurations hold aggregate issue capacity at eight instructions per cycle:

| Boards | Clusters per board | Issue width per cluster | Aggregate issue width |
|---:|---:|---:|---:|
| 1 | 8 | 1 | 8 |
| 2 | 4 | 1 | 8 |
| 4 | 2 | 1 | 8 |
| 8 | 1 | 1 | 8 |

A fifth configuration, 4 boards × 4 clusters × 1 issue slot, raises aggregate capacity to 16. It is a scaling comparison, not a controlled equal-capacity comparison.

The simulator sweeps remote-memory fractions of 10%, 50%, and 90%. Each run uses the same seed and workload probabilities.

## Model assumptions

Defaults:

- 1,024 contexts across 64 workspaces, 16 threads per workspace.
- 30,000 cycles, first 3,000 excluded from IPC.
- 30% memory instructions; 15% dependency-stall instructions among non-memory instructions.
- 80-cycle local memory latency.
- 80-cycle remote memory service latency plus 20-cycle fabric latency.
- 128 fabric bytes/cycle, shared by all boards.
- 64-byte requests; a remote operation transfers 64 bytes in each direction.
- 4,096 outstanding memory requests globally.
- Each cluster issues at most one instruction per cycle in the baseline topology sweep.

A workspace is assigned to a board by workspace ID modulo board count, then to a board-local cluster. All its threads follow that placement. Each board has one modeled local-memory service lane with a configurable byte rate; local requests queue on that board and do not consume fabric bandwidth. Remote requests select another board, use separate full-duplex request and response service lanes on the shared fabric, and queue at the target board's local-memory lane. Fabric latency includes a base per-direction component plus a logarithmic switch-hop proxy. A memory request occupies one outstanding slot until completion. A thread has at most one outstanding memory request.

The model reports IPC, utilization against configured aggregate issue capacity, per-board issue counts, remote request count, fabric bytes, queue-full attempts, idle cluster issue slots, and maximum outstanding requests.

## Run

Requires JDK 25 or newer:

```sh
mkdir -p build/scheduler-sim
javac --release 25 -Xlint:all -Werror \
  -d build/scheduler-sim \
  tools/scheduler-sim/src/main/java/org/flossware/f128x/sim/BoardFabricSweep.java
java -cp build/scheduler-sim org.flossware.f128x.sim.BoardFabricSweep
```

Useful overrides:

```sh
java \
  -Df128x.fabricBytesPerCycle=512 \
  -Df128x.localMemoryLatency=60 \
  -Df128x.remoteMemoryLatency=60 \
  -Df128x.cycles=60000 \
  -cp build/scheduler-sim \
  org.flossware.f128x.sim.BoardFabricSweep
```

## Interpretation cautions

This is deliberately a small first model. It does not yet include:

- Local-memory channels/banks, read/write contention, refresh, or realistic controller scheduling.
- Multiple parallel fabric links, actual switch topology, routing, arbitration fairness, or link-level contention. The model assumes separate request and response directions. The hop count is only a logarithmic latency proxy.
- Cache coherence, cache hierarchy, directory traffic, atomics, or consistency.
- Board-to-board DMA, I/O traffic, interrupts, board failure, hot-plug, or reset behavior.
- Thread migration between boards or workspace migration.
- Clock, power, thermal, area, pin-count, signal-integrity, or physical packaging costs.
- Workload traces, address locality, real instruction streams, or compiler behavior.

The shared full-duplex fabric and single local-memory lane per board are bottleneck candidates, not claims about a proposed electrical interface. The model assumes one local service lane per board and a single globally serialized fabric; it is intended to expose trade-offs, not select a physical interconnect.

The first four configurations compare packaging and fabric pressure at equal aggregate issue capacity. If they differ, the cause in this model is the board-local placement and remote-memory serialization, not additional execution width. The 4×4 configuration is a separate scale-up point.

No ISA, board count, interconnect protocol, or memory-consistency decision follows from these experiments alone.

## Next experiments

1. Add per-board local-memory bandwidth and queueing.
2. Compare a shared fabric with switched point-to-point links and a ring/mesh proxy.
3. Add explicit workspace placement and controlled remote access patterns.
4. Add optional thread/workspace migration and charge migration traffic.
5. Sweep fabric bandwidth and latency to find the break-even region.
6. Add multiple deterministic seeds and workload profiles before using results to guide architecture.
