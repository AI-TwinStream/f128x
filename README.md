# f128x

f128x is a FlossWare processor-architecture research project. It began as a 128-bit successor exploration inspired by the Texas Instruments TMS9900 and its workspace-oriented programming model. The current direction expands that concept into a massively multithreaded, workspace-oriented general-purpose processor.

The project is not a TMS9900 ISA clone and does not promise TMS9900 binary compatibility.

## Current architectural direction

| Property | Current target |
|---|---:|
| Workspaces | 4,096 |
| Threads per workspace | 16 |
| Total architectural thread contexts | 65,536 |
| Private registers per thread | 128 × 128-bit |
| Shared registers per workspace | 128 × 128-bit |
| Aggregate architectural register storage | 136 MiB |

These are architectural capacity targets. They do not require every implementation to physically instantiate all contexts or provide arbitrary simultaneous access to every register.

A 128-bit register does not imply 128-bit addresses, instructions, memory buses, or execution units. Those are independent design decisions.

## Documentation

### Architecture
- [Architecture overview 0.1](docs/architecture/overview-0.1.md)
- [Scheduling model research 0.1](docs/architecture/scheduling-model-0.1.md)
- [Historical register-file and workspace notes](docs/architecture/register-file.md)
- [Original architecture overview](docs/architecture/overview.md)

### Research
- [Historical architecture research 0.1](docs/research/historical-architectures-0.1.md)

### Platform and operating systems
- [Operating-system strategy 0.1](docs/platforms/os-strategy-0.1.md)
- [TI-99/4A platform strategy](docs/platforms/ti-99-4a.md)

### Planning
- [Roadmap](docs/roadmap.md)

## Historical inspiration

The current research priorities are:
- TMS9900: workspace-oriented register organization.
- Denelcor HEP and Tera/Cray MTA: fine-grained multithreading and latency hiding.
- CDC 6600: scoreboarding and functional-unit scheduling.
- INMOS Transputer: explicit communication between execution domains.
- XMOS XCore: practical hardware-thread implementation.
- Cray vector architectures: vector execution and memory throughput.

Historical mechanisms are research inputs, not automatic design requirements.

## Operating-system strategy

NetBSD is the preferred first full OS-porting candidate because of its history of supporting diverse architectures and its machine-dependent/machine-independent separation. DragonFly BSD is a high-priority kernel research reference, especially for scheduler layering, per-CPU scheduling, SMP, and NUMA-aware design. Neither OS choice is frozen.

## Status

Architectural research and specification development. The current workspace/thread capacity and register organization are provisional architectural targets. The ISA, ABI, scheduler microarchitecture, memory model, privilege architecture, and physical implementation are not frozen.
