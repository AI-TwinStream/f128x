# fNx Roadmap

**Date:** 2026-11-06  
**Status:** Architecture and research roadmap; priorities evolve as contracts are reviewed

fNx is the architecture family. N is the native architectural width in bits. 128-bit research profile is one member and the current focus of the executable scheduler/fabric research. Family-wide decisions must not be inferred from 128-bit research profile-only simulation results.

## Current working specifications

- [x] Rename the architecture family to fNx.
- [x] Define N as native architectural width.
- [x] Establish one common ISA and global opcode namespace across members.
- [x] Establish N-bit GPRs, native arithmetic, pointers, and virtual addresses as the working family direction.
- [x] Allow physical address width/capacity to be smaller than N, with translation/protection.
- [x] Establish one native width per CPU unit and mixed-width system interoperability through a width-neutral fabric.
- [x] Record N-bit opcode-field direction and proposed immutable-allocation rules; ratification remains open.
- [x] Define compiler/installer instruction expansion as the preferred older-member compatibility path where exact equivalence exists.
- [x] Propose fNx P-Code and a portable `.fnx` application package.
- [x] Propose install-time AOT compilation, optional JIT, and a regenerable native-code cache.
- [x] Propose an fNx Control Processor (FCP) for independent platform management and capability discovery.
- [x] Require hierarchical backplanes and width-neutral management/data contracts.

## 1. Decisions to ratify

**Active M1 work:** [Opcode namespace and allocation proposal](architecture/opcode-namespace-and-allocation-proposal-0.1.md) records N-bit opcode width as the working architectural direction. [Instruction encoding feasibility study](architecture/instruction-encoding-feasibility-0.1.md) compares candidate formats while preserving that rule and makes the opcode-field cost explicit at large N. [Opcode allocation and encoding decision analysis](architecture/opcode-allocation-and-encoding-decision-analysis-0.1.md) compares global allocation policies and quantifies a small illustrative instruction corpus. Each member directly represents identifiers that fit in its N-bit field. Supported N bounds, global allocation policy, and final instruction encoding remain open. [Address-space and memory-model proposal](architecture/address-space-and-memory-model-proposal-0.1.md) now collects the N-bit virtual-address, translation, protection, memory-ordering, atomicity, and coherence contracts. [Exception and interrupt model proposal](architecture/exception-and-interrupt-model-proposal-0.1.md) now collects precise-fault, trap, interrupt, reset, and privilege requirements. [Workspace and thread lifecycle contract proposal](architecture/workspace-and-thread-lifecycle-contract-proposal-0.1.md) now defines logical ownership, generation-safe identity, lifecycle, residency, migration, shared-register semantics, and resource discovery requirements. [Width-neutral fabric and backplane contract proposal](architecture/width-neutral-fabric-and-backplane-contract-proposal-0.1.md) now defines endpoint, transaction, ordering, atomicity, flow-control, error, hierarchy, and control-plane requirements. [Conformance and reference-model strategy](architecture/conformance-and-reference-model-strategy-0.1.md) defines layered conformance domains, implementation profiles, requirement traceability, reference artifacts, and CI evidence. No opcode values have been assigned.

- [ ] Define the supported minimum and maximum N values.
- [ ] Ratify N-bit opcode fields and the finite global opcode registry, including allocation rules for identifiers representable only by wider members.
- [ ] Specify opcode allocation, reserved values, illegal-instruction behavior, and ISA revision rules.
- [ ] Select and specify instruction length, formats, operand fields, register selectors, immediates, and extension rules using the encoding feasibility study.
- [x] Draft workspace/thread ownership, identity, lifecycle, residency, migration, and resource-discovery contract for review.
- [ ] Decide whether register counts and workspace/thread capacities are family invariants or member-specific.
- [ ] Ratify N-bit virtual-address semantics and physical-address translation contract.
- [x] Draft width-neutral endpoint, transaction, ordering, atomicity, flow-control, error, hierarchy, and management protocol requirements.
- [ ] Define normative common backplane transaction and management protocols.
- [ ] Decide FCP native width, placement, redundancy, firmware, and trust model.

## 2. Native ISA and ABI

- [ ] Define instruction semantics and machine-readable opcode registry.
- [x] Establish comparative instruction encoding feasibility study and evaluation criteria.
- [x] Add an illustrative corpus and first-pass encoding-size comparison; full program-level measurement remains open.
- [ ] Encode representative instruction corpus and measure code density, fetch cost, and decode complexity across N values.
- [ ] Define arithmetic, overflow, flags, exceptions, and sub-width operations.
- [x] Draft address-space, translation, memory-model, atomicity, and coherence requirements for review.
- [ ] Define normative memory model, atomics, fences, and ordering across registers/memory/fabric.
- [x] Draft exception, trap, interrupt, reset, and privilege requirements for review.
- [ ] Define normative privilege, protection, traps, interrupts, and reset state.
- [ ] Define ABI, calling convention, stack, object format, relocations, and system interface.
- [x] Define layered conformance domains, implementation-profile reporting, requirement traceability, and reference-model strategy.
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

- [x] Draft complete architectural thread/workspace state and ownership requirements.
- [ ] Ratify complete architectural thread/workspace state.
- [x] Draft context residency, backing, save/restore, migration, and privileged observability requirements.
- [ ] Define normative context residency, backing, save/restore, migration, and privileged observability.
- [x] Draft workspace ownership, isolation, lifecycle, and stale-identity prevention requirements.
- [ ] Define normative workspace ownership, isolation, virtualization, lifecycle, and stale-ID prevention.
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

- Existing scheduler and board/fabric experiments are 128-bit research profile-oriented models.
- Existing simulation outputs are sensitivity results, not hardware performance predictions.
- Historical 128-bit research profile capacity studies are retained as research artifacts, not family-wide architectural requirements.
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
