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

A workspace is assigned to a board by workspace ID modulo board count, then to a board-local cluster. All its threads follow that placement. Each board has one modeled local-memory service lane with a configurable byte rate; local requests queue on that board and do not consume fabric bandwidth. For each remote request, the target is selected uniformly from the other boards. The request and response use separate full-duplex service lanes on the shared fabric, and the request queues at the target board's local-memory lane. Fabric latency includes a base per-direction component plus a logarithmic switch-hop proxy. A memory request occupies one outstanding slot until completion. A thread has at most one outstanding memory request.

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


## Preliminary results

Four full-default runs were executed locally with OpenJDK 21, using seeds 42, 7, 101, and 2026. The repository workflow compiles and smoke-tests the committed source with JDK 25; its run status is not yet visible through the available GitHub status interface, so this is not being represented as a completed JDK 25 CI pass.

Configuration:

- 1,024 contexts, 64 workspaces, 16 threads/workspace.
- 30,000 cycles, first 3,000 excluded from IPC.
- 30% memory instructions; 15% dependency-stall instructions among non-memory instructions.
- 64 bytes/cycle per board-local memory service lane.
- 128 bytes/cycle in each fabric direction.
- 64-byte requests; 80-cycle local and remote memory latency.
- 20-cycle base fabric latency per direction plus two cycles per switch hop.
- One outstanding memory request per thread; 4,096-request global cap.

The table shows mean measured IPC across the four seeds:

| Remote-memory probability | 1 board × 8 clusters | 2 × 4 | 4 × 2 | 8 × 1 | 4 × 4 (16-wide) |
|---:|---:|---:|---:|---:|---:|
| 10% | 3.3268 | 6.6553 | 8.0000 | 8.0000 | 13.3189 |
| 50% | 3.3268 | 6.6080 | 6.6525 | 6.6523 | 6.6532 |
| 90% | 3.3268 | 3.7068 | 3.7066 | 3.7066 | 3.7067 |

**Important:** A one-board system has no remote board, so its remote probability is necessarily zero regardless of the configured percentage. Its identical result in all three rows is expected. Do not read the first column as an equal-remote-traffic control.

### What this says, and what it does not

- With mostly local traffic, adding boards adds local-memory service capacity. Four boards reach the eight-wide issue ceiling; eight boards add no further throughput in this workload.
- At 50% remote traffic, the two-, four-, and eight-board configurations are nearly indistinguishable. More local memory lanes are offset by remote traffic.
- At 90% remote traffic, adding boards provides essentially no throughput gain. The modeled remote path dominates.
- Doubling execution capacity from eight to sixteen issue slots helps substantially only in the low-remote case. At 50% and 90% remote traffic, the additional issue capacity is almost entirely unused.

These results are **not** evidence that four boards are optimal. They demonstrate that the assumed ratio of board-local memory capacity to inter-board traffic can dominate the benefit of adding execution clusters. The model is intentionally simple, and its one local-memory service lane per board and single full-duplex fabric path are strong assumptions. More seeds reduce random noise but do not validate those assumptions against real hardware.

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
