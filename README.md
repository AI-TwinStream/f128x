# fNx

**fNx** is an open processor-architecture family and standards research project. The value of N in a member name is its native architectural width in bits. Examples include f16x, f32x, f64x, f256x, f512x, and f1024x; a 128-bit member is also part of the family. The minimum supported width remains under consideration; 16 bits is the current leading candidate, not yet a ratified decision.

The family is intended to use one common ISA, one global opcode registry, and common architectural and platform contracts across native widths. A CPU architectural unit has one native width. Different fNx members may coexist in a system through a common width-neutral, hierarchical backplane/fabric.

The project is not a TMS9900 ISA clone and does not promise TMS9900 binary compatibility. The fNx workspace is a proposed resource-sharing and protection abstraction, not the TMS9900's memory-resident register workspace.

## Current architectural direction

For an fNx member:

- General-purpose registers, native arithmetic, native data width, pointers, and virtual addresses are N bits.
- Smaller scalar and packed operations are explicitly defined within the N-bit architecture; they do not create mixed-width CPU modules.
- Physical address width and installed memory capacity may be smaller than N. Translation and protection map N-bit virtual addresses onto available physical resources.
- The current opcode-field direction is N bits, with one finite global opcode registry shared across all members.
- Existing opcode assignments and semantics remain immutable. New operations receive new unique opcode values.
- Instruction length, instruction formats, operand fields, immediates, and extension encoding remain open design work.
- A newer instruction may be expanded by the compiler/installer into an exactly equivalent older instruction sequence where possible. Unsupported non-expandable capabilities must be reported, not silently approximated.

The proposed application model distributes portable **fNx P-Code** in an `.fnx` package. The installer verifies the package and compiles P-Code to native code for the detected target. A JIT may optimize hot paths. Native code is a derived, target-specific cache; P-Code remains the portable source of truth. The names and package format are proposals, not frozen standards.

The proposed **fNx Control Processor (FCP)** provides platform discovery, capability reporting, power/reset/thermal management, fault handling, and hierarchical backplane coordination. It uses the fNx ISA family as a separate processor unit. It is not a serial hop for every main-CPU instruction and is not the application compiler.

## 128-bit research profile

The current 128-bit member workspace/thread/register targets are member-specific research assumptions, not family-wide invariants:

- 128 workspaces maximum.
- 128 architectural thread contexts per workspace, for 16,384 total architectural thread contexts.
- 128 private 128-bit registers per thread.
- 128 shared 128-bit registers per workspace.

At those values, private register-state capacity is 32 MiB and workspace-shared register-state capacity is 256 KiB, for 32.25 MiB total. These are architectural capacity calculations, not claims about physical residency, ports, area, or timing.

The scheduler and board/fabric experiments currently use 128-bit member-oriented models and naming. Their measured results are sensitivity studies for those model assumptions, not performance predictions for every fNx width.

## Normative working specifications

- [fNx family and architectural contract](docs/architecture/fnx-family-and-architectural-contract-0.1.md)
- [fNx P-Code and application compatibility](docs/architecture/fnx-pcode-and-compatibility-0.1.md)
- [fNx control processor and management plane](docs/architecture/fnx-control-and-management-plane-0.1.md)
- [Family and backplane compatibility research](docs/architecture/family-and-backplane-compatibility-0.1.md)
- [ISA, ABI, and toolchain plan](docs/architecture/isa-abi-and-toolchain-plan-0.2.md)

## Architecture and implementation research

- [Architecture overview](docs/architecture/overview-0.1.md)
- [Context state and residency](docs/architecture/context-state-and-residency-0.2.md)
- [Workspace protection and shared state](docs/architecture/workspace-protection-and-shared-state-0.2.md)
- [Scheduling progress and interrupts](docs/architecture/scheduling-progress-and-interrupts-0.2.md)
- [Logical data paths](docs/architecture/data-paths-0.1.md)
- [Physical system block diagram](docs/architecture/physical-system-block-diagram-0.1.md)
- [Review synthesis](docs/reviews/review-synthesis-0.2.md)
- [Roadmap](docs/roadmap.md)

## Research and simulation

- [Scheduler simulation](tools/scheduler-sim/README.md)
- [Board and backplane simulation](docs/research/board-backplane-simulation-0.1.md)
- [Register and context scaling proposal](docs/architecture/register-and-context-scaling-0.1.md)
- [Register and context scaling results](docs/research/register-and-context-scaling-results-0.1.md)
- [Historical architecture research](docs/research/historical-architectures-0.1.md)
- [Scheduling model research](docs/architecture/scheduling-model-0.1.md)
- [Context capacity and latency model](docs/architecture/context-capacity-and-latency-model-0.2.md)

## Platform

- [Operating-system strategy](docs/platforms/os-strategy-0.1.md)
- [TI-99/4A platform strategy](docs/platforms/ti-99-4a.md)

## Status

Architectural research and specification development. fNx is the family; 128-bit member is one member. The N-bit register/arithmetic/pointer/virtual-address direction, common ISA, N-bit opcode-field direction, P-Code distribution model, FCP management concept, and hierarchical width-neutral fabric are working design directions with explicit unresolved details. No complete ISA encoding, P-Code format, ABI, memory model, management protocol, RTL, or physical system is frozen.
