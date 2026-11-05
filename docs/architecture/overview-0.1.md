# fNx Architecture Overview 0.2

**Date:** 2026-11-05  
**Status:** Family-level architectural overview; detailed contracts remain provisional

## 1. Purpose

fNx is a proposed open processor architecture family. N is the native architectural width in bits. Members such as f16x, f32x, f64x, f128x, f256x, f512x, and f1024x share one ISA and one architectural philosophy. The minimum N is unresolved; 16 bits is currently favored provisionally.

The project explores workspace-oriented execution, large numbers of independently schedulable hardware-thread contexts, private thread state, workspace-shared state, and modular systems that can combine different fNx members.

The architecture is not a TMS9900 ISA clone and does not promise TMS9900 binary compatibility. The TMS9900 workspace pointer is historical inspiration only; the proposed fNx workspace is a broader resource-sharing and protection abstraction.

## 2. Family design principles

1. One common ISA and global opcode registry across all fNx members.
2. N-bit general-purpose registers, native arithmetic, native data width, pointers, and virtual addresses.
3. One native width per CPU architectural unit; no mixed native widths inside a CPU unit.
4. Smaller sub-width operations are defined within an N-bit member.
5. Physical address width and physical memory capacity may be smaller than N, with translation/protection.
6. Stable opcode assignments and semantics; ISA growth adds unique instructions.
7. Portable application distribution through the proposed fNx P-Code and `.fnx` package.
8. Install-time AOT compilation to target-native code, with optional JIT.
9. A common width-neutral fabric supports mixed-width CPU units and hierarchical backplanes.
10. An fNx Control Processor (FCP) provides independent platform management and capability discovery, not serial instruction execution.

## 3. Opcode and instruction encoding

The current direction is an N-bit opcode field with one finite global opcode registry. The same numeric opcode means the same instruction across all members. A larger field does not automatically define more instructions.

New instructions receive unique opcode values. Existing meanings are immutable. Unrecognized native opcodes trap; older hardware may run newer portable applications when the installer can lower the operation into an exact supported sequence.

Instruction length, operand fields, register selectors, immediate formats, and extension words remain open. Opcode width must not be confused with total instruction size.

## 4. Workspace and thread model

A workspace is a proposed logical resource-sharing and protection domain containing independently schedulable threads. Each thread has private architectural state; threads in a workspace may access workspace-shared state under defined synchronization and protection rules.

The current f128x research profile uses:
- 128 workspaces.
- 128 thread contexts per workspace.
- 128 private 128-bit registers per thread.
- 128 shared 128-bit registers per workspace.

These are member-specific research targets, not family-wide constants or frozen physical capacities. Context residency, register banking, shared-state synchronization, and physical access costs remain open.

## 5. Address and memory model

Pointers and virtual addresses are N bits for a member. Physical address width may be smaller. Translation maps virtual addresses to installed physical memory and device regions.

The architecture must define memory ordering, atomics, alignment, page translation, permissions, cacheability, coherence, DMA, and I/O. The existence of a wide virtual address space does not require a platform to populate it with physical memory.

## 6. Portable application model

The proposed `.fnx` package contains portable fNx P-Code plus manifest, ABI/runtime requirements, capability requirements, integrity information, and optional resources.

The installer validates the package, discovers target capabilities, and compiles P-Code into native code for the installed member. A JIT may optimize hot paths. Native code is a derived cache; P-Code remains authoritative and can be recompiled after hardware or toolchain changes.

This approach combines established techniques from portable bytecode and binary translation ecosystems. It is not a claim that portable bytecode or AOT/JIT compilation is unprecedented.

## 7. Modular platform and management

Different native-width CPU units may coexist on a common width-neutral fabric. Backplanes may connect hierarchically. Fabric protocol semantics are independent of processor register width.

The proposed FCP uses the fNx ISA family as a separate low-power management processor. It discovers hardware capabilities, manages power/reset/thermal state, reports faults, and coordinates boards and backplanes. It should operate with independent power, clock, reset, and connectivity sufficient to manage the platform when main CPUs are unavailable.

## 8. Status and open decisions

The family-level directions are captured in:
- [fNx family and architectural contract](fnx-family-and-architectural-contract-0.1.md)
- [fNx P-Code and application compatibility](fnx-pcode-and-compatibility-0.1.md)
- [fNx control processor and management plane](fnx-control-and-management-plane-0.1.md)

Still open: minimum N, exact instruction encoding/length, operand formats, P-Code execution model and package format, ABI, privilege and memory model, context residency, workspace/shared-register synchronization, FCP width and firmware, backplane protocol, and physical implementation.

## 9. Development sequence

1. Ratify family width range and opcode namespace.
2. Specify native ISA semantics, encoding, and conformance tests.
3. Specify N-bit virtual addressing and physical translation.
4. Define P-Code and portable package/ABI contracts.
5. Build reference interpreter, assembler, disassembler, and compiler path.
6. Continue f128x scheduler/fabric experiments while clearly bounding their applicability.
7. Prototype reduced hardware and platform management.
8. Port an OS only after privilege, memory, interrupt, and ABI contracts stabilize.
