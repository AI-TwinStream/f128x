# fNx Operating-System Strategy 0.2

**Date:** 2026-11-05  
**Status:** Initial family-level platform strategy; OS selection not frozen

## 1. Objective

Define an operating-system strategy for the fNx architecture family without making the architecture dependent on one kernel or native width.

For a member fNx, N is its native architectural width. General-purpose registers, native arithmetic, pointers, and virtual addresses are N bits. Physical address width may be smaller and is handled by translation/protection. Different native-width CPU units may coexist through a width-neutral hierarchical fabric.

The OS strategy must account for:
- Common ISA and member capability discovery.
- Workspace and hardware-thread lifecycle.
- N-bit virtual addressing and implemented physical-address limits.
- Privilege, protection, interrupts, and exceptions.
- Timers and scheduling.
- Atomics and memory ordering.
- SMP, NUMA, and heterogeneous fNx member coordination.
- Portable P-Code installation/AOT compilation and optional JIT.
- Native toolchain, debugger, and emulator support.
- FCP management and telemetry interfaces.

## 2. Keep four execution environments separate

1. **Development host:** Existing Linux or BSD systems used for cross-compilation, simulation, and development.
2. **Reference environment:** Architectural interpreter, P-Code verifier/runtime, and conformance tests.
3. **Bring-up environment:** Freestanding native runtime and hardware test harness before a full OS exists.
4. **Full operating system:** A portable OS port after ISA, memory, interrupt, ABI, and platform contracts stabilize.

A full OS port should not be the first milestone.

## 3. Candidate operating systems

### NetBSD

**Current recommendation: First full OS-porting candidate.**

NetBSD has a long history of supporting diverse processor architectures and separates machine-independent and machine-dependent code.

Benefits include established architecture-porting conventions, UNIX userland, and a broad portability history. Risks include the substantial new machine-dependent port, compiler/ABI/debugger work, and adaptation for workspace-aware hardware contexts.

References:
- https://www.netbsd.org/about/portability.html
- https://www.netbsd.org/ports/

### DragonFly BSD

**Current recommendation: High-priority kernel design reference; future port candidate.**

Study scheduler layering, per-CPU execution management, kernel concurrency, locality-aware scheduling, NUMA-aware allocation, and virtual-kernel development. Verify current platform support and architecture-porting feasibility before treating it as a practical first port.

References:
- https://www.dragonflybsd.org/features/
- https://docs.dragonflybsd.org/user-guide/intro/

### FreeBSD

**Current recommendation: Secondary porting candidate.**

Study machine-dependent interfaces, scheduler adaptation, interrupt/SMP abstractions, and its RISC-V and ARM porting experience. Its mature networking, storage, driver, and debugging ecosystem is useful.

### Linux

**Current recommendation: Toolchain and ecosystem reference, not the first OS milestone.**

Linux offers broad compiler, application, emulation, and architecture-enablement experience. A new architecture port is substantial work and should not distract from validating the ISA and hardware model.

## 4. Portable application model

Ordinary applications are proposed to be distributed as fNx P-Code in a proposed `.fnx` package. The installer validates the package, ABI/runtime requirements, signatures, and required capabilities, discovers the installed fNx member, and compiles P-Code to target-native code.

The OS/runtime may use JIT optimization for hot paths. Native code is a target-specific derived cache; P-Code remains the portable source of truth. Applications requiring a non-expandable hardware capability must be rejected or require a capable target.

Native binaries remain appropriate for kernels, bootloaders, drivers, hard real-time paths, and direct hardware access. The OS must not force all system components through a managed runtime.

See [fNx P-Code and application compatibility](../architecture/fnx-pcode-and-compatibility-0.1.md).

## 5. FCP and platform management

The proposed fNx Control Processor provides hardware discovery, ISA/capability inventory, reset/power/thermal management, health reporting, and fault coordination. It uses the fNx ISA family as a separate processor unit and communicates through width-neutral management contracts.

The FCP is not a serial hop for every instruction and is not the application compiler. The OS consumes its capability and management interfaces. Management authority, authentication, and host/FCP ownership must be explicitly specified.

See [fNx control processor and management plane](../architecture/fnx-control-and-management-plane-0.1.md).

## 6. Process, workspace, and thread are distinct

A candidate software mapping is:
- **Process:** Protection domain and virtual address-space ownership.
- **Workspace:** Hardware execution and shared-state domain.
- **Thread:** Independently scheduled execution stream mapped to a hardware context.

The mapping may be many-to-many or constrained by OS policy, but must be explicit. A workspace is not automatically an OS process, address space, or security principal.

## 7. Required architectural support

The OS will require defined mechanisms for privilege levels, exception/interrupt entry and return, timers, N-bit virtual addressing, translation/protection, atomics, memory ordering, workspace/thread lifecycle, context inspection, performance counters, and cache/translation maintenance.

## 8. OS development stages

| Stage | Deliverable |
|---|---|
| OS-0 | Reset, boot, and platform contract |
| OS-1 | Reference interpreter, P-Code verifier, and freestanding runtime |
| OS-2 | Native exceptions, interrupts, and timers |
| OS-3 | Physical memory and N-bit virtual-address translation |
| OS-4 | Workspace and hardware-thread management |
| OS-5 | Synchronization and atomic operations |
| OS-6 | Minimal kernel |
| OS-7 | NetBSD port feasibility and initial port |
| OS-8 | DragonFly BSD port feasibility study |
| OS-9 | Drivers, userland, and broader ecosystem |

## 9. Open decisions

Final OS selection, ABI, boot protocol, privilege model, process/workspace mapping, kernel scheduler interface, address-space architecture, interrupt controller, SMP/NUMA contract, FCP authority model, and debug/performance interfaces remain open.

NetBSD remains the first full OS-porting candidate and DragonFly BSD a major kernel design reference. Neither is frozen.
