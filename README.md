# f128x

f128x is a FlossWare processor-architecture research project exploring a workspace-oriented, massively multithreaded general-purpose processor and a scalable family of width profiles.

The project is not a TMS9900 ISA clone and does not promise TMS9900 binary compatibility. The workspace is a proposed resource-sharing and protection abstraction, not the TMS9900's memory-resident register workspace.

## Current architectural direction

The intended family consists of `f128x`, `f256x`, `f512x`, and `f1024x`. These are width profiles of one architecture family, not separate ISAs. The instruction model and semantics should remain common across profiles; native register/datapath width is profile-dependent.

The current f128x profile direction is:

- 128 workspaces maximum.
- 128 architectural thread contexts per workspace, for 16,384 total architectural thread contexts.
- 128 private 128-bit registers per thread.
- 128 shared 128-bit registers per workspace.
- 128-bit pointers across the family in the current design direction.
- Integrated scalar and vector operations using the same architectural register model.
- 8-, 16-, 32-, and 64-bit sub-width operations in addition to native-width operations.

At 128 workspaces × 128 threads × 128 private registers × 16 bytes per register, private architectural register capacity is 32 MiB. Workspace-shared register capacity is 256 KiB. Total architectural register-state capacity is 32.25 MiB. These are architectural capacity figures, not a claim that every context is physically resident or that every register is simultaneously accessible.

Register width, register count, and selector width are independent. A bank of 128 registers requires 7 selector bits regardless of whether each register is 128, 256, 512, or 1,024 bits wide. If a future design increases the bank to 256 entries, it requires 8 selector bits, not 14. See [Architecture family and mixed-width backplane compatibility 0.1](docs/architecture/family-and-backplane-compatibility-0.1.md).

The system backplane is intended to support mixed-width endpoints. For example, f128x and f256x processor boards, or f128x and f512x boards, should coexist in one system. The interconnect protocol must be width-neutral and independent of native processor datapath width. Protocol details, physical signaling, coherence, and performance remain open design work.

Workspace memory is intended to be independently managed, protected, persistent while the workspace exists, and non-pageable once committed. General system memory may retain ordinary OS-managed virtual memory and paging. Allocation policy and resource limits remain to be specified.

## Documentation

### Architecture family and system structure
- [Architecture family and mixed-width backplane compatibility 0.1](docs/architecture/family-and-backplane-compatibility-0.1.md) — common ISA across width profiles, register selector sizing, and mixed-width backplane requirements.
- [Physical system block diagram 0.1](docs/architecture/physical-system-block-diagram-0.1.md) — conceptual chip, CPU-board, memory, I/O, and backplane boundaries.
- [Logical data paths 0.1](docs/architecture/data-paths-0.1.md) — thread/register execution and local/remote memory and I/O paths.
- [Context state and residency 0.2](docs/architecture/context-state-and-residency-0.2.md)
- [Workspace protection and shared state 0.2](docs/architecture/workspace-protection-and-shared-state-0.2.md)
- [ISA, ABI, and toolchain plan 0.2](docs/architecture/isa-abi-and-toolchain-plan-0.2.md)
- [Scheduling progress and interrupts 0.2](docs/architecture/scheduling-progress-and-interrupts-0.2.md)
- [Review synthesis 0.2](docs/reviews/review-synthesis-0.2.md)

### Research and simulation
- [Register and context scaling proposal 0.1](docs/architecture/register-and-context-scaling-0.1.md) — earlier 4,096-workspace/256-thread candidate study; historical research input, superseded as the current architectural direction by the family and backplane document.
- [Register and context scaling results 0.1](docs/research/register-and-context-scaling-results-0.1.md)
- [Context capacity and latency model 0.2](docs/architecture/context-capacity-and-latency-model-0.2.md)
- [Scheduler simulation](tools/scheduler-sim/README.md)
- [Historical architecture research 0.1](docs/research/historical-architectures-0.1.md)
- [Architecture overview 0.1](docs/architecture/overview-0.1.md)

### Platform and operating systems
- [Operating-system strategy 0.1](docs/platforms/os-strategy-0.1.md)
- [TI-99/4A platform strategy](docs/platforms/ti-99-4a.md)

### Planning
- [Roadmap](docs/roadmap.md)

### Historical documentation
- [Superseded architecture overview](docs/architecture/overview.md)
- [Historical register-file and workspace notes](docs/architecture/register-file.md)

## Status

Architectural research and specification development. The family/profile direction, initial f128x workspace/thread/register targets, and mixed-width backplane requirement are current design directions. They are not yet a frozen ISA, ABI, protocol, RTL implementation, or physical system. Context residency, shared-register synchronization, memory consistency, address translation, scheduler microarchitecture, backplane protocol, and physical implementation still require specification and validation.
