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

A workspace is assigned to a board by workspace ID modulo board count, then to a board-local cluster. All its threads follow that placement. Each board has a configurable number of independent local-memory service lanes (`f128x.localMemoryLanesPerBoard`, default 1), each with the configured byte rate. A request is assigned to the lane with the earliest available service time; local requests queue on the source board and do not consume fabric bandwidth. Remote targets use `f128x.remotePattern`: `uniform` chooses any other board, `hotspot` directs requests to board 0 (or board 1 when the source is board 0), and `paired` directs requests to the paired board (`board XOR 1`, with a wrap fallback). The request and response use separate full-duplex service lanes on the shared fabric, and the request queues at the target board's local-memory lane. Fabric latency includes a base per-direction component plus a logarithmic switch-hop proxy. A memory request occupies one outstanding slot until completion. A thread has at most one outstanding memory request.

The model reports IPC, utilization against configured aggregate issue capacity, per-board issue counts, remote request count by destination board, fabric bytes, queue-full attempts, idle cluster issue slots, and maximum outstanding requests.

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

Four full-default runs were executed locally with OpenJDK 21, using seeds 42, 7, 101, and 2026. The repository workflow also compiled the committed source with Temurin JDK 25 and passed the board/backplane smoke test and the existing scheduler suite in run [#108](https://github.com/FlossWare/f128x/actions/runs/41851658484). The four-seed throughput table below was generated in the local OpenJDK 21 experiment; JDK 25 CI validates compilation and deterministic smoke behavior, not the full four-seed result table.

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

The shared full-duplex fabric and configurable board-local memory lanes are bottleneck candidates, not claims about a proposed electrical interface. The default is one local service lane per board, and the fabric remains a single globally serialized request path plus response path. These are deliberate abstractions to expose trade-offs, not to select a physical interconnect.

The first four configurations compare packaging and fabric pressure at equal aggregate issue capacity. If they differ, the cause in this model is board-local placement, memory-lane service capacity, and remote-memory serialization, not additional execution width. The 4×4 configuration is a separate scale-up point. The published four-seed table above uses the original one-lane-per-board and uniform-remote baseline; results from the new lane-count and remote-pattern sweeps must be reported separately.

No ISA, board count, interconnect protocol, or memory-consistency decision follows from these experiments alone.

## Next experiments

1. Compare a shared fabric with switched point-to-point links and a ring/mesh proxy.
2. Add optional thread/workspace migration and charge migration traffic.
3. Sweep fabric bandwidth, local-memory lane count, and latency to find break-even regions.
4. Add representative workload profiles and multiple deterministic seeds for the hotspot and paired traffic patterns before using results to guide architecture.


## Switched fabric topology sensitivity experiment

The follow-on `FabricTopologySweep` compares the original shared-fabric abstraction with ring and two-dimensional mesh proxies. Each topology is evaluated at 4, 8, and 16 boards, with 1, 2, and 4 parallel links per neighbor, under uniform, hotspot, and paired remote traffic.

The workload injects one 64-byte remote operation per source board every two cycles. Request and response directions have independent link service state. A packet traverses a deterministic shortest path, each hop consumes link service, and the destination has one local-memory service lane. The output reports average and p95 end-to-end latency and aggregate completed operations per cycle.

Run:

```sh
java -cp build/scheduler-sim org.flossware.f128x.sim.FabricTopologySweep
```

This experiment is intentionally a separate sensitivity model rather than a claim that the original `BoardFabricSweep` has been upgraded to a detailed network simulator. It omits arbitration fairness, virtual channels, adaptive routing, packet fragmentation, credit flow control, congestion control, link failures, and electrical/physical costs. Its fixed injection schedule and deterministic FIFO link reservations are useful for isolating topology and link-parallelism effects, but they are not a substitute for a cycle-accurate network-on-chip or backplane model.

The existing board-fabric smoke path runs a deterministic mesh scenario twice and fails if the results differ. The full topology matrix is opt-in to keep CI runtime bounded. These results should be reported separately from the published four-seed board/backplane baseline.


## Switched-fabric topology matrix results (2026-11-04)

The full default matrix was executed in GitHub Actions run [41929014603](https://github.com/AI-TwinStream/f128x/actions/runs/41929014603), using 2,048 remote operations per board and seed 42. All 81 cases completed successfully. These are outputs of the simplified reservation model described above, not hardware predictions.

### Selected 16-board comparisons

| Traffic | Topology | Links per neighbor | Mean latency | p95 latency | Operations/cycle |
|---|---|---:|---:|---:|---:|
| uniform | shared | 1 | 14463.50 | 27366 | 0.99617 |
| uniform | shared | 2 | 6272.71 | 11803 | 1.98450 |
| uniform | shared | 4 | 2185.77 | 4034 | 3.93185 |
| uniform | ring | 1 | 11842.65 | 22239 | 1.19083 |
| uniform | ring | 2 | 3459.87 | 6350 | 3.03464 |
| uniform | ring | 4 | 298.31 | 403 | 7.23515 |
| uniform | mesh | 1 | 156.63 | 180 | 7.66862 |
| uniform | mesh | 2 | 142.51 | 160 | 7.71012 |
| uniform | mesh | 4 | 140.28 | 156 | 7.71012 |
| hotspot | shared | 1 | 14463.50 | 27366 | 0.99617 |
| hotspot | shared | 2 | 13439.00 | 25420 | 1.06231 |
| hotspot | shared | 4 | 13438.88 | 25420 | 1.06231 |
| hotspot | ring | 1 | 12622.78 | 25347 | 1.06135 |
| hotspot | ring | 2 | 12622.78 | 25347 | 1.06135 |
| hotspot | ring | 4 | 12622.78 | 25347 | 1.06135 |
| hotspot | mesh | 1 | 12616.78 | 25342 | 1.06159 |
| hotspot | mesh | 2 | 12616.78 | 25342 | 1.06159 |
| hotspot | mesh | 4 | 12616.78 | 25342 | 1.06159 |
| paired | shared | 1 | 14463.50 | 27366 | 0.99617 |
| paired | shared | 2 | 6271.50 | 11801 | 1.98474 |
| paired | shared | 4 | 2175.50 | 4019 | 3.93941 |
| paired | ring | 1 | 127.00 | 127 | 7.76309 |
| paired | ring | 2 | 127.00 | 127 | 7.76309 |
| paired | ring | 4 | 127.00 | 127 | 7.76309 |
| paired | mesh | 1 | 127.00 | 127 | 7.76309 |
| paired | mesh | 2 | 127.00 | 127 | 7.76309 |
| paired | mesh | 4 | 127.00 | 127 | 7.76309 |

### Full measured matrix

All values are model cycles and aggregate operations per cycle. Mean is average end-to-end latency; p95 is the 95th-percentile latency.

| Boards | Traffic | Topology | Links | Operations | Mean | p95 | Completion cycle | Operations/cycle |
|---:|---|---|---:|---:|---:|---:|---:|---:|
| 4 | uniform | shared | 1 | 8192 | 2175.50 | 4019 | 8318 | 0.98485 |
| 4 | uniform | shared | 2 | 8192 | 132.24 | 134 | 4228 | 1.93756 |
| 4 | uniform | shared | 4 | 8192 | 127.58 | 129 | 4223 | 1.93985 |
| 4 | uniform | ring | 1 | 8192 | 154.57 | 183 | 4283 | 1.91268 |
| 4 | uniform | ring | 2 | 8192 | 129.72 | 134 | 4228 | 1.93756 |
| 4 | uniform | ring | 4 | 8192 | 129.69 | 134 | 4228 | 1.93756 |
| 4 | uniform | mesh | 1 | 8192 | 129.13 | 133 | 4228 | 1.93756 |
| 4 | uniform | mesh | 2 | 8192 | 129.05 | 133 | 4228 | 1.93756 |
| 4 | uniform | mesh | 4 | 8192 | 129.05 | 133 | 4228 | 1.93756 |
| 4 | hotspot | shared | 1 | 8192 | 2175.50 | 4019 | 8318 | 0.98485 |
| 4 | hotspot | shared | 2 | 8192 | 1151.00 | 2073 | 6270 | 1.30654 |
| 4 | hotspot | shared | 4 | 8192 | 1150.50 | 2072 | 6270 | 1.30654 |
| 4 | hotspot | ring | 1 | 8192 | 896.63 | 2041 | 6274 | 1.30571 |
| 4 | hotspot | ring | 2 | 8192 | 896.63 | 2041 | 6274 | 1.30571 |
| 4 | hotspot | ring | 4 | 8192 | 896.63 | 2041 | 6274 | 1.30571 |
| 4 | hotspot | mesh | 1 | 8192 | 896.63 | 2041 | 6275 | 1.30550 |
| 4 | hotspot | mesh | 2 | 8192 | 896.63 | 2041 | 6275 | 1.30550 |
| 4 | hotspot | mesh | 4 | 8192 | 896.63 | 2041 | 6275 | 1.30550 |
| 4 | paired | shared | 1 | 8192 | 2175.50 | 4019 | 8318 | 0.98485 |
| 4 | paired | shared | 2 | 8192 | 127.50 | 128 | 4222 | 1.94031 |
| 4 | paired | shared | 4 | 8192 | 127.00 | 127 | 4221 | 1.94077 |
| 4 | paired | ring | 1 | 8192 | 127.00 | 127 | 4221 | 1.94077 |
| 4 | paired | ring | 2 | 8192 | 127.00 | 127 | 4221 | 1.94077 |
| 4 | paired | ring | 4 | 8192 | 127.00 | 127 | 4221 | 1.94077 |
| 4 | paired | mesh | 1 | 8192 | 127.00 | 127 | 4221 | 1.94077 |
| 4 | paired | mesh | 2 | 8192 | 127.00 | 127 | 4221 | 1.94077 |
| 4 | paired | mesh | 4 | 8192 | 127.00 | 127 | 4221 | 1.94077 |
| 8 | uniform | shared | 1 | 16384 | 6271.50 | 11801 | 16510 | 0.99237 |
| 8 | uniform | shared | 2 | 16384 | 2178.34 | 4023 | 8323 | 1.96852 |
| 8 | uniform | shared | 4 | 16384 | 143.56 | 150 | 4244 | 3.86051 |
| 8 | uniform | ring | 1 | 16384 | 2370.79 | 4321 | 8639 | 1.89652 |
| 8 | uniform | ring | 2 | 16384 | 141.91 | 155 | 4242 | 3.86233 |
| 8 | uniform | ring | 4 | 16384 | 136.92 | 147 | 4237 | 3.86689 |
| 8 | uniform | mesh | 1 | 16384 | 134.72 | 143 | 4237 | 3.86689 |
| 8 | uniform | mesh | 2 | 16384 | 133.69 | 142 | 4237 | 3.86689 |
| 8 | uniform | mesh | 4 | 16384 | 133.45 | 142 | 4237 | 3.86689 |
| 8 | hotspot | shared | 1 | 16384 | 6271.50 | 11801 | 16510 | 0.99237 |
| 8 | hotspot | shared | 2 | 16384 | 5247.00 | 9855 | 14462 | 1.13290 |
| 8 | hotspot | shared | 4 | 16384 | 5246.75 | 9854 | 14462 | 1.13290 |
| 8 | hotspot | ring | 1 | 16384 | 4613.06 | 9788 | 14474 | 1.13196 |
| 8 | hotspot | ring | 2 | 16384 | 4613.06 | 9788 | 14474 | 1.13196 |
| 8 | hotspot | ring | 4 | 16384 | 4613.06 | 9788 | 14474 | 1.13196 |
| 8 | hotspot | mesh | 1 | 16384 | 4611.81 | 9787 | 14472 | 1.13212 |
| 8 | hotspot | mesh | 2 | 16384 | 4611.81 | 9787 | 14472 | 1.13212 |
| 8 | hotspot | mesh | 4 | 16384 | 4611.81 | 9787 | 14472 | 1.13212 |
| 8 | paired | shared | 1 | 16384 | 6271.50 | 11801 | 16510 | 0.99237 |
| 8 | paired | shared | 2 | 16384 | 2175.50 | 4019 | 8318 | 1.96970 |
| 8 | paired | shared | 4 | 16384 | 127.50 | 128 | 4222 | 3.88063 |
| 8 | paired | ring | 1 | 16384 | 127.00 | 127 | 4221 | 3.88154 |
| 8 | paired | ring | 2 | 16384 | 127.00 | 127 | 4221 | 3.88154 |
| 8 | paired | ring | 4 | 16384 | 127.00 | 127 | 4221 | 3.88154 |
| 8 | paired | mesh | 1 | 16384 | 129.00 | 137 | 4231 | 3.87237 |
| 8 | paired | mesh | 2 | 16384 | 128.50 | 137 | 4231 | 3.87237 |
| 8 | paired | mesh | 4 | 16384 | 128.50 | 137 | 4231 | 3.87237 |
| 16 | uniform | shared | 1 | 32768 | 14463.50 | 27366 | 32894 | 0.99617 |
| 16 | uniform | shared | 2 | 32768 | 6272.71 | 11803 | 16512 | 1.98450 |
| 16 | uniform | shared | 4 | 32768 | 2185.77 | 4034 | 8334 | 3.93185 |
| 16 | uniform | ring | 1 | 32768 | 11842.65 | 22239 | 27517 | 1.19083 |
| 16 | uniform | ring | 2 | 32768 | 3459.87 | 6350 | 10798 | 3.03464 |
| 16 | uniform | ring | 4 | 32768 | 298.31 | 403 | 4529 | 7.23515 |
| 16 | uniform | mesh | 1 | 32768 | 156.63 | 180 | 4273 | 7.66862 |
| 16 | uniform | mesh | 2 | 32768 | 142.51 | 160 | 4250 | 7.71012 |
| 16 | uniform | mesh | 4 | 32768 | 140.28 | 156 | 4250 | 7.71012 |
| 16 | hotspot | shared | 1 | 32768 | 14463.50 | 27366 | 32894 | 0.99617 |
| 16 | hotspot | shared | 2 | 32768 | 13439.00 | 25420 | 30846 | 1.06231 |
| 16 | hotspot | shared | 4 | 32768 | 13438.88 | 25420 | 30846 | 1.06231 |
| 16 | hotspot | ring | 1 | 32768 | 12622.78 | 25347 | 30874 | 1.06135 |
| 16 | hotspot | ring | 2 | 32768 | 12622.78 | 25347 | 30874 | 1.06135 |
| 16 | hotspot | ring | 4 | 32768 | 12622.78 | 25347 | 30874 | 1.06135 |
| 16 | hotspot | mesh | 1 | 32768 | 12616.78 | 25342 | 30867 | 1.06159 |
| 16 | hotspot | mesh | 2 | 32768 | 12616.78 | 25342 | 30867 | 1.06159 |
| 16 | hotspot | mesh | 4 | 32768 | 12616.78 | 25342 | 30867 | 1.06159 |
| 16 | paired | shared | 1 | 32768 | 14463.50 | 27366 | 32894 | 0.99617 |
| 16 | paired | shared | 2 | 32768 | 6271.50 | 11801 | 16510 | 1.98474 |
| 16 | paired | shared | 4 | 32768 | 2175.50 | 4019 | 8318 | 3.93941 |
| 16 | paired | ring | 1 | 32768 | 127.00 | 127 | 4221 | 7.76309 |
| 16 | paired | ring | 2 | 32768 | 127.00 | 127 | 4221 | 7.76309 |
| 16 | paired | ring | 4 | 32768 | 127.00 | 127 | 4221 | 7.76309 |
| 16 | paired | mesh | 1 | 32768 | 127.00 | 127 | 4221 | 7.76309 |
| 16 | paired | mesh | 2 | 32768 | 127.00 | 127 | 4221 | 7.76309 |
| 16 | paired | mesh | 4 | 32768 | 127.00 | 127 | 4221 | 7.76309 |

### Interpretation and limits

- Under 16-board uniform traffic, the 2-D mesh proxy with one link per neighbor reached 156.63 mean latency, p95 180, and 7.66862 operations/cycle; four links changed this only modestly to 140.28, 156, and 7.71012. In this model the mesh's spatially distributed links avoid the global serialization visible in the shared-fabric proxy.
- The 16-board ring proxy needed four links per neighbor to approach the mesh result for uniform traffic: 298.31 mean latency and 7.23515 operations/cycle. With one link, it reached 1.19083 operations/cycle.
- Hotspot traffic was dominated by the single destination-board memory lane. Increasing link count did not materially change the ring/mesh hotspot results, which is useful evidence that this workload is memory-service limited rather than fabric-link limited in this model.
- The shared-fabric proxy is extremely sensitive to the number of global parallel lanes. Its results should not be interpreted as a comparison against a properly switched, nonblocking crossbar unless the shared-fabric resource assumptions are made equivalent.
- These outputs are deterministic single-seed results. The next step is to sweep seeds and injection rates, validate fairness/arbitration assumptions, and compare topologies at explicitly equal bisection bandwidth and endpoint capacity before drawing architectural conclusions.


## Equal-bisection-bandwidth comparison revision

The 2026-11-04 matrix above is the original fixed-per-link-bandwidth experiment. It is retained as historical output, but it is **not** an equal-bisection-bandwidth comparison: with the same per-link bandwidth, different topologies expose different aggregate cut capacity. Do not use that table alone to rank the topologies.

The revised `FabricTopologySweep` computes the minimum balanced bisection edge cut for each board count and topology. The one-link case is normalized to an aggregate bisection budget of 128 bytes/cycle by setting per-link service bandwidth to `128 / bisectionEdges`. Two and four parallel links multiply that common aggregate budget to 256 and 512 bytes/cycle respectively, identically for every topology at a given board count. The result line now reports `bisectionEdges`, `perLinkBytesPerCycle`, and `bisectionBandwidthBytesPerCycle`, making the normalization auditable. The full 81-case matrix remains 3 board counts × 3 traffic patterns × 3 topologies × 3 link counts.

This is a first-order capacity normalization, not a complete physical equivalence proof. It equalizes minimum balanced cut bandwidth, but does not equalize hop count, endpoint degree, link count/area, bisection placement, routing fairness, wire length, or switching energy. The shared topology remains an abstract globally shared resource and should be read as a reference architecture, not a literal crossbar implementation. The model's deterministic transaction reservation order also remains a simplification.

The revised CI matrix asserts that all 81 cases execute and that the aggregate bisection budgets 128/256/512 bytes per cycle are present. New normalized measurements will be recorded below after the workflow passes.


Normalization implementation correction: the initial 128-byte/cycle target was not divisible by the three-edge bisection in the eight-board mesh, and the first ring cut calculation counted only one direction. Both were corrected before accepting normalized measurements. The ring bisection counts both directed neighbor links crossing a partition. The common one-link target is now 192 bytes/cycle, divisible by cut sizes 1, 2, 3, and 4; the two- and four-link budgets are 384 and 768 bytes/cycle. CI asserts the expected cut sizes for all board counts and topologies, in addition to the aggregate bandwidth values.


The normalized fabric service model uses fractional cycle availability for links and memory service. This is deliberate: integer-cycle ceiling for a 64-byte packet would make several nominal link bandwidths collapse to the same one-cycle service time, defeating the normalization. Reported latency and completion cycle values may therefore be fractional. The model remains deterministic and is not claiming sub-cycle hardware timing accuracy; fractional cycles are a capacity-accounting device for the comparative experiment.


## Equal-bisection topology matrix results (2026-11-04)

The corrected normalized matrix completed successfully in [GitHub Actions run 41931395805](https://github.com/AI-TwinStream/f128x/actions/runs/41931395805), at seed 42 and 2,048 operations per board. All 81 combinations passed. Each topology at a given board count received the same aggregate bisection budget for each link-count setting: 192, 384, or 768 bytes/cycle. Fractional link service cycles are used to preserve the bandwidth ratios for 64-byte packets.

### 16-board results

| Traffic | Topology | Links | Cut edges | Per-link B/cycle | Aggregate cut B/cycle | Mean latency | p95 latency | Operations/cycle |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| uniform | shared | 1 | 1 | 192 | 3543.47 | 6618.00 | 2.96436 |
| uniform | shared | 2 | 1 | 192 | 837.18 | 1456.67 | 5.83095 |
| uniform | shared | 4 | 1 | 192 | 138.97 | 154.33 | 7.74352 |
| uniform | ring | 1 | 2 | 96 | 7258.95 | 13575.00 | 1.78142 |
| uniform | ring | 2 | 2 | 96 | 1673.13 | 2986.00 | 4.51931 |
| uniform | ring | 4 | 2 | 96 | 154.56 | 177.67 | 7.67580 |
| uniform | mesh | 1 | 4 | 48 | 408.05 | 655.67 | 6.82809 |
| uniform | mesh | 2 | 4 | 48 | 145.70 | 164.00 | 7.70287 |
| uniform | mesh | 4 | 4 | 48 | 142.58 | 158.67 | 7.70347 |
| hotspot | shared | 1 | 1 | 192 | 13438.02 | 25419.00 | 1.06234 |
| hotspot | shared | 2 | 1 | 192 | 13437.63 | 25418.67 | 1.06236 |
| hotspot | shared | 4 | 1 | 192 | 13437.50 | 25418.67 | 1.06236 |
| hotspot | ring | 1 | 2 | 96 | 12621.09 | 25346.33 | 1.06145 |
| hotspot | ring | 2 | 2 | 96 | 12621.09 | 25346.33 | 1.06145 |
| hotspot | ring | 4 | 2 | 96 | 12621.09 | 25346.33 | 1.06145 |
| hotspot | mesh | 1 | 4 | 48 | 14858.81 | 29867.67 | 0.91918 |
| hotspot | mesh | 2 | 4 | 48 | 12618.45 | 25343.67 | 1.06150 |
| hotspot | mesh | 4 | 4 | 48 | 12618.45 | 25343.67 | 1.06150 |
| paired | shared | 1 | 1 | 192 | 3539.83 | 6612.00 | 2.96597 |
| paired | shared | 2 | 1 | 192 | 809.17 | 1423.67 | 5.86539 |
| paired | shared | 4 | 1 | 192 | 126.17 | 126.67 | 7.76370 |
| paired | ring | 1 | 2 | 96 | 126.33 | 126.33 | 7.76432 |
| paired | ring | 2 | 2 | 96 | 126.33 | 126.33 | 7.76432 |
| paired | ring | 4 | 2 | 96 | 126.33 | 126.33 | 7.76432 |
| paired | mesh | 1 | 4 | 48 | 127.67 | 127.67 | 7.76186 |
| paired | mesh | 2 | 4 | 48 | 127.67 | 127.67 | 7.76186 |
| paired | mesh | 4 | 4 | 48 | 127.67 | 127.67 | 7.76186 |

### Full 81-case results

Latency and completion values are in model cycles. Bandwidth values are bytes/cycle. The `cut` is the minimum balanced bisection edge count for the topology.

| Boards | Traffic | Topology | Links | Cut edges | Per-link B/cycle | Aggregate cut B/cycle | Operations | Mean latency | p95 latency | Completion cycle | Operations/cycle |
|---:|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 4 | uniform | shared | 1 | 1 | 192 | 192 | 8192 | 126.98 | 128.67 | 4222.33 | 1.94016 |
| 4 | uniform | shared | 2 | 1 | 192 | 384 | 8192 | 126.43 | 128.00 | 4222.00 | 1.94031 |
| 4 | uniform | shared | 4 | 1 | 192 | 768 | 8192 | 126.19 | 127.67 | 4221.67 | 1.94047 |
| 4 | uniform | ring | 1 | 2 | 96 | 192 | 8192 | 129.70 | 135.00 | 4229.67 | 1.93680 |
| 4 | uniform | ring | 2 | 2 | 96 | 384 | 8192 | 128.74 | 132.67 | 4226.67 | 1.93817 |
| 4 | uniform | ring | 4 | 2 | 96 | 768 | 8192 | 128.73 | 132.67 | 4226.67 | 1.93817 |
| 4 | uniform | mesh | 1 | 2 | 96 | 192 | 8192 | 128.22 | 132.00 | 4227.00 | 1.93802 |
| 4 | uniform | mesh | 2 | 2 | 96 | 384 | 8192 | 128.20 | 132.00 | 4227.00 | 1.93802 |
| 4 | uniform | mesh | 4 | 2 | 96 | 768 | 8192 | 128.20 | 132.00 | 4227.00 | 1.93802 |
| 4 | hotspot | shared | 1 | 1 | 192 | 192 | 8192 | 1150.08 | 2072.00 | 6269.00 | 1.30675 |
| 4 | hotspot | shared | 2 | 1 | 192 | 384 | 8192 | 1149.50 | 2071.00 | 6268.67 | 1.30682 |
| 4 | hotspot | shared | 4 | 1 | 192 | 768 | 8192 | 1149.00 | 2070.67 | 6268.67 | 1.30682 |
| 4 | hotspot | ring | 1 | 2 | 96 | 192 | 8192 | 895.87 | 2040.00 | 6273.00 | 1.30591 |
| 4 | hotspot | ring | 2 | 2 | 96 | 384 | 8192 | 895.87 | 2040.00 | 6273.00 | 1.30591 |
| 4 | hotspot | ring | 4 | 2 | 96 | 768 | 8192 | 895.87 | 2040.00 | 6273.00 | 1.30591 |
| 4 | hotspot | mesh | 1 | 2 | 96 | 192 | 8192 | 895.87 | 2040.00 | 6274.00 | 1.30571 |
| 4 | hotspot | mesh | 2 | 2 | 96 | 384 | 8192 | 895.87 | 2040.00 | 6274.00 | 1.30571 |
| 4 | hotspot | mesh | 4 | 2 | 96 | 768 | 8192 | 895.87 | 2040.00 | 6274.00 | 1.30571 |
| 4 | paired | shared | 1 | 1 | 192 | 192 | 8192 | 126.17 | 126.67 | 4220.67 | 1.94093 |
| 4 | paired | shared | 2 | 1 | 192 | 384 | 8192 | 125.83 | 126.00 | 4220.00 | 1.94123 |
| 4 | paired | shared | 4 | 1 | 192 | 768 | 8192 | 125.67 | 125.67 | 4219.67 | 1.94139 |
| 4 | paired | ring | 1 | 2 | 96 | 192 | 8192 | 126.33 | 126.33 | 4220.33 | 1.94108 |
| 4 | paired | ring | 2 | 2 | 96 | 384 | 8192 | 126.33 | 126.33 | 4220.33 | 1.94108 |
| 4 | paired | ring | 4 | 2 | 96 | 768 | 8192 | 126.33 | 126.33 | 4220.33 | 1.94108 |
| 4 | paired | mesh | 1 | 2 | 96 | 192 | 8192 | 126.33 | 126.33 | 4220.33 | 1.94108 |
| 4 | paired | mesh | 2 | 2 | 96 | 384 | 8192 | 126.33 | 126.33 | 4220.33 | 1.94108 |
| 4 | paired | mesh | 4 | 2 | 96 | 768 | 8192 | 126.33 | 126.33 | 4220.33 | 1.94108 |
| 8 | uniform | shared | 1 | 1 | 192 | 192 | 16384 | 817.41 | 1434.00 | 5597.00 | 2.92728 |
| 8 | uniform | shared | 2 | 1 | 192 | 384 | 16384 | 129.83 | 135.67 | 4223.67 | 3.87909 |
| 8 | uniform | shared | 4 | 1 | 192 | 768 | 16384 | 128.22 | 133.67 | 4223.00 | 3.87971 |
| 8 | uniform | ring | 1 | 2 | 96 | 192 | 16384 | 944.93 | 1631.33 | 5808.33 | 2.82077 |
| 8 | uniform | ring | 2 | 2 | 96 | 384 | 16384 | 136.50 | 147.33 | 4236.00 | 3.86780 |
| 8 | uniform | ring | 4 | 2 | 96 | 768 | 16384 | 134.98 | 144.33 | 4234.67 | 3.86902 |
| 8 | uniform | mesh | 1 | 3 | 64 | 192 | 16384 | 134.72 | 143.00 | 4237.00 | 3.86689 |
| 8 | uniform | mesh | 2 | 3 | 64 | 384 | 16384 | 133.69 | 142.00 | 4237.00 | 3.86689 |
| 8 | uniform | mesh | 4 | 3 | 64 | 768 | 16384 | 133.45 | 142.00 | 4237.00 | 3.86689 |
| 8 | hotspot | shared | 1 | 1 | 192 | 192 | 16384 | 5246.04 | 9854.00 | 14461.00 | 1.13298 |
| 8 | hotspot | shared | 2 | 1 | 192 | 384 | 16384 | 5245.58 | 9853.67 | 14460.67 | 1.13300 |
| 8 | hotspot | shared | 4 | 1 | 192 | 768 | 16384 | 5245.33 | 9852.67 | 14460.67 | 1.13300 |
| 8 | hotspot | ring | 1 | 2 | 96 | 192 | 16384 | 4612.02 | 9787.33 | 14472.33 | 1.13209 |
| 8 | hotspot | ring | 2 | 2 | 96 | 384 | 16384 | 4612.02 | 9787.33 | 14472.33 | 1.13209 |
| 8 | hotspot | ring | 4 | 2 | 96 | 768 | 16384 | 4612.02 | 9787.33 | 14472.33 | 1.13209 |
| 8 | hotspot | mesh | 1 | 3 | 64 | 192 | 16384 | 4611.81 | 9787.00 | 14472.00 | 1.13212 |
| 8 | hotspot | mesh | 2 | 3 | 64 | 384 | 16384 | 4611.81 | 9787.00 | 14472.00 | 1.13212 |
| 8 | hotspot | mesh | 4 | 3 | 64 | 768 | 16384 | 4611.81 | 9787.00 | 14472.00 | 1.13212 |
| 8 | paired | shared | 1 | 1 | 192 | 192 | 16384 | 809.17 | 1423.67 | 5586.67 | 2.93270 |
| 8 | paired | shared | 2 | 1 | 192 | 384 | 16384 | 126.17 | 126.67 | 4220.67 | 3.88185 |
| 8 | paired | shared | 4 | 1 | 192 | 768 | 16384 | 125.83 | 126.00 | 4220.00 | 3.88246 |
| 8 | paired | ring | 1 | 2 | 96 | 192 | 16384 | 126.33 | 126.33 | 4220.33 | 3.88216 |
| 8 | paired | ring | 2 | 2 | 96 | 384 | 16384 | 126.33 | 126.33 | 4220.33 | 3.88216 |
| 8 | paired | ring | 4 | 2 | 96 | 768 | 16384 | 126.33 | 126.33 | 4220.33 | 3.88216 |
| 8 | paired | mesh | 1 | 3 | 64 | 192 | 16384 | 129.00 | 137.00 | 4231.00 | 3.87237 |
| 8 | paired | mesh | 2 | 3 | 64 | 384 | 16384 | 128.50 | 137.00 | 4231.00 | 3.87237 |
| 8 | paired | mesh | 4 | 3 | 64 | 768 | 16384 | 128.50 | 137.00 | 4231.00 | 3.87237 |
| 16 | uniform | shared | 1 | 1 | 192 | 192 | 32768 | 3543.47 | 6618.00 | 11054.00 | 2.96436 |
| 16 | uniform | shared | 2 | 1 | 192 | 384 | 32768 | 837.18 | 1456.67 | 5619.67 | 5.83095 |
| 16 | uniform | shared | 4 | 1 | 192 | 768 | 32768 | 138.97 | 154.33 | 4231.67 | 7.74352 |
| 16 | uniform | ring | 1 | 2 | 96 | 192 | 32768 | 7258.95 | 13575.00 | 18394.33 | 1.78142 |
| 16 | uniform | ring | 2 | 2 | 96 | 384 | 32768 | 1673.13 | 2986.00 | 7250.67 | 4.51931 |
| 16 | uniform | ring | 4 | 2 | 96 | 768 | 32768 | 154.56 | 177.67 | 4269.00 | 7.67580 |
| 16 | uniform | mesh | 1 | 4 | 48 | 192 | 32768 | 408.05 | 655.67 | 4799.00 | 6.82809 |
| 16 | uniform | mesh | 2 | 4 | 48 | 384 | 32768 | 145.70 | 164.00 | 4254.00 | 7.70287 |
| 16 | uniform | mesh | 4 | 4 | 48 | 768 | 32768 | 142.58 | 158.67 | 4253.67 | 7.70347 |
| 16 | hotspot | shared | 1 | 1 | 192 | 192 | 32768 | 13438.02 | 25419.00 | 30845.00 | 1.06234 |
| 16 | hotspot | shared | 2 | 1 | 192 | 384 | 32768 | 13437.63 | 25418.67 | 30844.67 | 1.06236 |
| 16 | hotspot | shared | 4 | 1 | 192 | 768 | 32768 | 13437.50 | 25418.67 | 30844.67 | 1.06236 |
| 16 | hotspot | ring | 1 | 2 | 96 | 192 | 32768 | 12621.09 | 25346.33 | 30871.00 | 1.06145 |
| 16 | hotspot | ring | 2 | 2 | 96 | 384 | 32768 | 12621.09 | 25346.33 | 30871.00 | 1.06145 |
| 16 | hotspot | ring | 4 | 2 | 96 | 768 | 32768 | 12621.09 | 25346.33 | 30871.00 | 1.06145 |
| 16 | hotspot | mesh | 1 | 4 | 48 | 192 | 32768 | 14858.81 | 29867.67 | 35649.00 | 0.91918 |
| 16 | hotspot | mesh | 2 | 4 | 48 | 384 | 32768 | 12618.45 | 25343.67 | 30869.67 | 1.06150 |
| 16 | hotspot | mesh | 4 | 4 | 48 | 768 | 32768 | 12618.45 | 25343.67 | 30869.67 | 1.06150 |
| 16 | paired | shared | 1 | 1 | 192 | 192 | 32768 | 3539.83 | 6612.00 | 11048.00 | 2.96597 |
| 16 | paired | shared | 2 | 1 | 192 | 384 | 32768 | 809.17 | 1423.67 | 5586.67 | 5.86539 |
| 16 | paired | shared | 4 | 1 | 192 | 768 | 32768 | 126.17 | 126.67 | 4220.67 | 7.76370 |
| 16 | paired | ring | 1 | 2 | 96 | 192 | 32768 | 126.33 | 126.33 | 4220.33 | 7.76432 |
| 16 | paired | ring | 2 | 2 | 96 | 384 | 32768 | 126.33 | 126.33 | 4220.33 | 7.76432 |
| 16 | paired | ring | 4 | 2 | 96 | 768 | 32768 | 126.33 | 126.33 | 4220.33 | 7.76432 |
| 16 | paired | mesh | 1 | 4 | 48 | 192 | 32768 | 127.67 | 127.67 | 4221.67 | 7.76186 |
| 16 | paired | mesh | 2 | 4 | 48 | 384 | 32768 | 127.67 | 127.67 | 4221.67 | 7.76186 |
| 16 | paired | mesh | 4 | 4 | 48 | 768 | 32768 | 127.67 | 127.67 | 4221.67 | 7.76186 |

### Initial interpretation

- At 16 boards and one-link/192-byte-per-cycle bisection, the mesh proxy delivers 6.82809 operations/cycle under uniform traffic, versus 1.78142 for the ring and 2.96436 for the shared reference. The mesh still leads, but the dramatic gap from the original unnormalized matrix is substantially reduced. That is exactly why normalization was necessary.
- At four links (768 aggregate cut bytes/cycle), the 16-board uniform cases converge: shared 7.74352, ring 7.67580, mesh 7.70347 operations/cycle. Under this offered load, the common fabric capacity is no longer the dominant bottleneck.
- One-link hotspot behavior needs separate interpretation: the destination memory service lane is a bottleneck shared by these topologies. Fabric bandwidth alone cannot solve endpoint serialization.
- This remains a deterministic, single-seed comparison with a simplified reservation-order model. Multi-seed and injection-rate sweeps are next; no architecture should be selected from these numbers alone.
