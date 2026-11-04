# f128x Operating-System Strategy 0.1

**Date:** 2026-11-04  
**Status:** Initial platform strategy; OS selection not frozen

## 1. Objective

Define an operating-system strategy for a new processor architecture without making f128x dependent on a single kernel.

The OS strategy must account for:
- New ISA and ABI.
- Workspace and hardware-thread lifecycle.
- Address spaces and protection.
- Interrupts and exceptions.
- Timers and scheduling.
- Atomic operations and memory ordering.
- SMP and possible NUMA implementations.
- Toolchain, debugger, and emulator support.

## 2. Keep three targets separate

1. **Development host:** Existing x86-64 Linux or BSD machines used for compilation, simulation, and development.
2. **Bring-up environment:** A freestanding runtime and test harness running before a full OS exists.
3. **Full operating system:** A portable OS port once hardware and ISA contracts are stable.

A full OS port should not be the first milestone.

## 3. Candidate operating systems

### 3.1 NetBSD

**Current recommendation: First full OS-porting candidate.**

NetBSD has a long history of supporting diverse processor architectures and explicitly separates machine-independent and machine-dependent code.

Potential benefits:
- Established architecture-porting conventions.
- Machine-independent and machine-dependent separation.
- Broad historical architecture support.
- Established UNIX userland and toolchain ecosystem.

Risks:
- f128x still requires a new machine-dependent port.
- New compiler backend, ABI, debugger, and emulator support will be substantial work.
- The scheduler may need adaptation for workspace-aware hardware contexts.

References:
- https://www.netbsd.org/about/portability.html
- https://www.netbsd.org/ports/

### 3.2 DragonFly BSD

**Current recommendation: High-priority kernel design reference; future port candidate.**

DragonFly BSD has an interesting SMP-oriented kernel design. Its documented architecture describes a per-CPU LWKT scheduler and a separate user-thread scheduler, with topology-aware scheduling and NUMA-aware memory behavior.

Potential research areas:
- Scheduler layering.
- Per-CPU execution management.
- Kernel concurrency and synchronization.
- Locality-aware scheduling.
- NUMA-aware allocation.
- Virtual-kernel development.

Risks:
- Current platform support and architecture-porting path must be verified before treating it as a practical first port.
- Existing scheduler and machine-dependent code may require substantial adaptation.

References:
- https://www.dragonflybsd.org/features/
- https://docs.dragonflybsd.org/user-guide/intro/

### 3.3 FreeBSD

**Current recommendation: Secondary porting candidate.**

Potential benefits:
- Mature kernel and driver ecosystem.
- Established architecture support and porting experience.
- Strong networking, storage, and debugging ecosystem.

Research areas:
- Machine-dependent kernel interfaces.
- Scheduler adaptation.
- Interrupt and SMP abstractions.
- RISC-V and ARM porting experience.

### 3.4 Linux

**Current recommendation: Toolchain and ecosystem reference, not the first OS milestone.**

Potential benefits:
- Broad compiler and application ecosystem.
- Extensive architecture enablement experience.
- Mature emulation and debugging ecosystem.

Risks:
- A new architecture port is substantial work.
- Kernel scheduler and memory-management adaptation may distract from validating the ISA and hardware model.

## 4. Proposed strategy

### Development

Use existing Linux or BSD x86-64 systems as development hosts.

### Initial bring-up

Build a freestanding f128x runtime with:
- Reset and boot sequence.
- Basic console output.
- Memory initialization.
- Exception and interrupt entry.
- Timer support.
- Register tests.
- Thread-context tests.
- Workspace tests.
- Basic physical memory management.

### First full OS

Use NetBSD as the first full OS-porting candidate, subject to a feasibility review after the ISA, MMU, interrupt, and ABI contracts exist.

### Parallel research

Study DragonFly BSD's scheduler and kernel concurrency architecture. Do not make the f128x architecture dependent on DragonFly-specific kernel abstractions.

### Long-term

Keep the architecture OS-neutral and aim for multiple OS implementations.

## 5. Process, workspace, and thread are distinct abstractions

A workspace is a hardware resource-sharing and execution domain. It is not automatically equivalent to an OS process.

A candidate software mapping is:
- **Process:** Protection domain and virtual address-space ownership.
- **Workspace:** Hardware execution and shared-state domain.
- **Thread:** Independently scheduled execution stream mapped to a hardware thread context.

The mapping may be many-to-many or constrained by OS policy, but must be explicitly designed.

The kernel manages ownership, permissions, resource limits, and lifecycle. Hardware provides mechanisms, not a UNIX process model.

## 6. Required architectural support for an OS

The processor architecture will eventually need defined mechanisms for:
- Privilege levels.
- Exception vectors and entry/return.
- Interrupt routing and masking.
- Timers.
- Address translation and protection.
- Atomic operations.
- Memory ordering and fences.
- Workspace and thread management.
- Context inspection and debugging.
- Performance counters.
- Cache and translation maintenance.

These mechanisms remain open design decisions.

## 7. OS development stages

| Stage | Deliverable |
|---|---|
| OS-0 | Reset, boot, and platform contract |
| OS-1 | Freestanding runtime and test harness |
| OS-2 | Exceptions, interrupts, and timers |
| OS-3 | Physical memory and address translation |
| OS-4 | Workspace and hardware-thread management |
| OS-5 | Synchronization and atomic operations |
| OS-6 | Minimal kernel |
| OS-7 | NetBSD port feasibility and initial port |
| OS-8 | DragonFly BSD port feasibility study |
| OS-9 | Drivers, userland, and broader ecosystem |

## 8. Open decisions

- Final OS selection.
- ABI and calling convention.
- Boot protocol.
- Privilege and protection model.
- Process-to-workspace mapping.
- Kernel scheduler interface.
- Address-space architecture.
- Interrupt-controller design.
- SMP and NUMA contracts.
- Debug and performance-monitoring interfaces.

The current recommendation is NetBSD as the first full OS-porting candidate and DragonFly BSD as a major kernel design reference. Neither choice is frozen.
