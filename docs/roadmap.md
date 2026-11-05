# fNx Roadmap

**Date:** 2026-11-05  
**Status:** Architecture and research roadmap; priorities evolve as contracts are reviewed

fNx is the architecture family. N is the native architectural width in bits. f128x is one member and the current focus of the executable scheduler/fabric research. Family-wide decisions must not be inferred from f128x-only simulation results.

## Current working specifications

- [x] Rename the architecture family to fNx.
- [x] Define N as native architectural width.
- [x] Establish one common ISA and global opcode namespace across members.
- [x] Establish N-bit GPRs, native arithmetic, pointers, and virtual addresses as the working family direction.
- [x] Allow physical address width/capacity to be smaller than N, with translation/protection.
- [x] Establish one native width per CPU unit and mixed-width system interoperability through a width-neutral fabric.
- [x] Define the current N-bit opcode-field direction and immutable opcode assignments.
- [x] Define compiler/installer instruction expansion as the preferred older-member compatibility path where exact equivalence exists.
- [x] Propose fNx P-Code and a portable `.fnx` application package.
- [x] Propose install-time AOT compilation, optional JIT, and a regenerable native-code cache.
- [x] Propose an fNx Control Processor (FCP) for independent platform management and capability discovery.
- [x] Require hierarchical backplanes and width-neutral management/data contracts.

## 1. Decisions to ratify

- [ ] Decide minimum N (16 bits currently favored; f8x remains open).
- [ ] Ratify N-bit opcode field and finite global opcode registry.
- [ ] Specify opcode allocation, reserved values, illegal-instruction behavior, and ISA revision rules.
- [ ] Specify instruction length, formats, operand fields, register selectors, immediates, and extension words.
- [ ] Decide whether register counts and workspace/thread capacities are family invariants or member-specific.
- [ ] Define N-bit virtual-address semantics and physical-address translation contract.
- [ ] Define the common backplane transaction and management protocols.
- [ ] Decide FCP native width, placement, redundancy, firmware, and trust model.

## 2. Native ISA and ABI

- [ ] Define instruction semantics and machine-readable opcode registry.
- [ ] Complete representative encoding feasibility studies.
- [ ] Define arithmetic, overflow, flags, exceptions, and sub-width operations.
- [ ] Define memory model, atomics, fences, and ordering across registers/memory/fabric.
- [ ] Define privilege, protection, traps, interrupts, and reset state.
- [ ] Define ABI, calling convention, stack, object format, relocations, and system interface.
- [ ] Build reference interpreter, assembler, disassembler, golden vectors, and conformance suite.
- [ ] Prototype GCC and LLVM backends; evaluate Rust, Go, and OpenJDK paths.

## 3. Portable P-Code and application package

- [ ] Compare stack-based, register-based, and other typed P-Code execution models.
- [ ] Specify stable P-Code semantics and versioning independently of native ISA encoding.
- [ ] Specify `.fnx` package manifest, metadata, signatures, dependencies, and capability requirements.
- [ ] Define P-Code validation, sandboxing, memory safety, and runtime trust model.
- [ ] Define install-time AOT compilation and target-specific native cache.
- [ ] Define cache invalidation and regeneration rules.
- [ ] Define optional JIT profiling and optimization.
- [ ] Publish reference instruction expansions for new native opcodes where exact.
- [ ] Define behavior for conditionally expandable and non-expandable operations.
- [ ] Define native-code exceptions for kernels, drivers, boot, real-time, and hardware access.

## 4. FCP and platform management

- [ ] Define FCP boot, firmware trust, update, rollback, and recovery.
- [ ] Define independent power/clock/reset/connectivity requirements.
- [ ] Define hardware and capability inventory schema.
- [ ] Define reset, power, thermal, health, telemetry, and fault services.
- [ ] Define host OS/FCP authority, authentication, and arbitration.
- [ ] Define hierarchical discovery, routing, and fault containment.
- [ ] Prototype FCP firmware and management protocol on a small fNx member.

## 5. Workspace, context, and scheduling

- [ ] Define complete architectural thread/workspace state.
- [ ] Define context residency, backing, save/restore, migration, and privileged observability.
- [ ] Define workspace ownership, isolation, virtualization, lifecycle, and stale-ID prevention.
- [ ] Define shared-register atomicity and memory-ordering semantics.
- [ ] Define scheduler fairness, progress, interrupt, and exception routing.
- [ ] Justify member-specific context capacities with workload modelling.
- [ ] Estimate register banking, ports, area, timing, and power across multiple N values.

## 6. Fabric and physical implementation

- [ ] Define width-neutral fabric transactions, ordering, errors, flow control, and atomic capabilities.
- [ ] Define hierarchical backplane addressing, discovery, and capability propagation.
- [ ] Define link training, retry, CRC/ECC, reset, and fault recovery.
- [ ] Define mixed-width memory sharing and data-layout interoperability.
- [ ] Integrate per-output arbitration into the switched-fabric simulation.
- [ ] Re-run topology, routing, injection, and memory-bank sweeps after arbitration integration.
- [ ] Add explicit multi-width endpoint scenarios to the simulator.
- [ ] Define cache/coherence or explicit non-coherent ownership model.
- [ ] Build reduced reference implementation and FPGA feasibility model.

## 7. Operating system and software platform

- [ ] Define boot, timer, interrupt, memory, and platform contracts.
- [ ] Build P-Code verifier/runtime and fNx package installer.
- [ ] Build freestanding native runtime and bring-up harness.
- [ ] Define process-to-workspace and OS-thread-to-hardware-context mappings.
- [ ] Evaluate NetBSD as first full OS-porting candidate.
- [ ] Study DragonFly BSD scheduler/SMP design; evaluate FreeBSD and Linux alternatives.
- [ ] Build debugger, profiler, and performance-counter interfaces.

## Research boundaries

- Existing scheduler and board/fabric experiments are f128x-oriented models.
- Existing simulation outputs are sensitivity results, not hardware performance predictions.
- Historical f128x capacity studies are retained as research artifacts, not family-wide architectural requirements.
- No external ISA/runtime precedent makes the fNx combination automatically novel; compare JVM, .NET, WebAssembly, binary translation, and existing heterogeneous systems carefully.

## Milestones

**M0: Family direction**  
fNx naming, common family philosophy, N-bit width direction, and mixed-width system requirement documented.

**M1: Normative architectural contracts**  
Ratify width range, opcode namespace, instruction format, address model, workspace/thread semantics, memory model, and platform interfaces.

**M2: Portable software contract**  
Specify P-Code, `.fnx` package, validation, ABI, installer/AOT, and native cache.

**M3: Executable reference architecture**  
Interpreter, assembler/disassembler, conformance tests, compiler lowering, and P-Code runtime.

**M4: Platform management and fabric**  
FCP prototype, width-neutral hierarchical fabric, discovery, telemetry, and fault handling.

**M5: Hardware feasibility**  
Reduced RTL/FPGA prototype, register banking, scheduler, memory, and fabric validation.

**M6: OS bring-up**  
Freestanding runtime, minimal kernel, and OS-port feasibility after contracts stabilize.
