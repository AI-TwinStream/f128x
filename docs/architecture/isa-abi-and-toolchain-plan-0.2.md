# ISA, ABI, and Toolchain Plan 0.2

**Date:** 2026-11-04  
**Status:** Early design workstream; encoding and ABI not frozen

## 1. Why this work starts early

The instruction encoding, address model, register organization, compiler backend, ABI, and debugger constrain one another. Toolchain work cannot be postponed until after an OS port is underway.

The 128-bit GPR width does not imply 128-bit pointers, virtual addresses, physical addresses, instruction words, or memory buses.

## 2. Address-width decision

Evaluate virtual and physical address widths independently.

Candidate virtual-address starting points include 48-bit and 64-bit designs. These are comparison points, not selected values.

Decision criteria:
- Required virtual address space.
- Page-table depth and translation cost.
- Canonical-address rules, if any.
- Physical memory scalability.
- TLB size and reach.
- Capability/protection model.
- ABI pointer representation.
- Compatibility with compiler and linker ecosystems.
- Long-term extension path.

Also specify byte addressing, endianness, alignment, supported load/store widths, and unaligned-access behavior.

## 3. Instruction encoding

Encoding feasibility is a prerequisite to freezing the ISA. The register namespace is large: 128 private and 128 shared registers. Three explicit register operands can consume substantial encoding space before opcode, addressing mode, immediate, predicate, and extension fields are considered.

Investigate:
- Fixed-width versus variable-length or mixed-width encoding.
- Register-class selection and operand namespace.
- Compact common forms and extended forms.
- Immediate widths and literal construction.
- Load/store addressing modes.
- Branch range and PC-relative forms.
- Atomic and synchronization encodings.
- Privileged/system instruction space.
- Reserved encoding space and future extension rules.
- Decoder complexity and instruction-fetch alignment.

Do not assume a 32-bit fixed-width format is viable without a concrete encoding study.

## 4. Compiler and assembler strategy

**Reuse and port established toolchains before considering a new compiler.** The goal is to make f128x a supported target in existing compiler ecosystems, not to recreate mature language frontends, optimizers, runtimes, and standard libraries.

Keep two meanings of "port" distinct:
- A **hosted cross-toolchain** runs on an existing x86-64 or ARM64 development machine and emits f128x code.
- A **native toolchain** runs on an f128x system and may eventually compile f128x programs locally.

The first practical deliverable is a hosted cross-toolchain. Native self-hosting is a later milestone and is not required to execute compiled programs on f128x.

### Existing compiler and runtime candidates

| Language family | Existing implementation to reuse | f128x work still required |
|---|---|---|
| C / C++ | GCC first candidate; LLVM/Clang also evaluated | Target backend, assembler, linker, ABI, C library/sysroot |
| Rust | `rustc` with its LLVM code-generation path | LLVM f128x target, target specification, runtime/standard-library support |
| Go | Evaluate `gccgo` and the official Go compiler separately | Backend/architecture support, runtime, assembler/linker and ABI integration |
| Java | OpenJDK `javac` and JVM bytecode ecosystem | Port or implement a JVM runtime, garbage collector, native interfaces and platform libraries |

GCC is a strong initial candidate for C and C++ because it already separates language frontends from architecture-specific code generation and has an established target-backend model. Its Go frontend, `gccgo`, is a distinct implementation from the official Go toolchain; compatibility and runtime behavior must be evaluated rather than assumed.

LLVM is a complementary target, not a replacement for the GCC decision. It is especially important for Rust because the mainstream `rustc` code-generation path uses LLVM. Supporting both GCC and LLVM means maintaining two f128x backends, so the work should be staged and justified by language coverage.

Java requires a different approach. Java source can be compiled to portable JVM bytecode without a native f128x code generator, but executing it on f128x still requires a suitable JVM implementation and its runtime, garbage collector, native interfaces, and platform libraries. A bytecode interpreter can be an initial execution path; JIT compilation is a separate later optimization.

Prefer small, reviewable target ports and upstreamable changes over long-lived FlossWare forks. Track upstream releases and keep f128x-specific patches isolated. Do not copy an entire compiler repository into FlossWare merely to claim a port.

### Foundational deliverables

The initial target-enablement work should include:
- Machine-readable ISA description and stable instruction semantics.
- Assembler and disassembler, including encoding/decoding tests.
- Architectural reference interpreter and executable test harness.
- GCC f128x target prototype for a minimal C program.
- Linker target support, relocation definitions, object-file format and initial sysroot.
- ABI document covering calling convention, stack, register use, data layout and atomics.
- Debug information and debugger target.
- Conformance tests and golden vectors.

A minimal assembler and interpreter may precede a production compiler backend, but the ISA must be designed with compiler lowering, instruction selection and register allocation in mind. The simulator must eventually load and execute linked f128x binaries or provide a rigorously defined execution interface for them.

Do not start by implementing new Rust-like, Java-like or Go-like Floss languages. First establish which existing toolchains can target the ISA and identify genuine gaps that cannot reasonably be filled by porting.

## 5. ABI topics

The ABI must define:
- Pointer and integer types.
- 128-bit scalar representation and operations.
- Calling convention and argument passing.
- Return values.
- Stack layout and alignment.
- Callee/caller-saved register policy.
- Thread-local and workspace-shared data.
- Exception and unwind conventions.
- System-call boundary.
- Atomic and synchronization library interface.
- Process/workspace/thread mapping.
- Object-file and relocation formats.

The ABI must not assume that all 128 private registers are equally cheap or physically resident.

## 6. OS implications

A full OS port depends on stable:
- ISA and ABI.
- Privilege levels.
- Address translation.
- Interrupt and exception entry.
- Atomic operations and memory ordering.
- Context lifecycle and debug inspection.
- Timer and platform interfaces.

The OS should manage software processes and threads over hardware workspaces/contexts; it should not expose every hardware context as an OS CPU by default.

## 7. Milestones

1. Encoding feasibility study with representative instruction families and compiler-lowering constraints.
2. Address-width and load/store model decision, including object-format and ABI implications.
3. Minimal assembler/disassembler, architectural interpreter and executable golden-vector tests.
4. Hosted GCC cross-compiler prototype: compile and link a minimal C program for f128x, then execute it in the simulator.
5. LLVM f128x backend feasibility and prototype; use it to establish the Rust target path before committing to full Rust support.
6. Evaluate gccgo versus the official Go toolchain, and define the OpenJDK/JVM runtime port path.
7. Complete ABI, linker/sysroot, debugger and conformance integration.
8. OS port feasibility after platform contracts stabilize.
9. Native/self-hosted toolchain feasibility after a usable f128x execution platform exists.

No instruction width, pointer width, compiler backend, or ABI is selected by this document.
