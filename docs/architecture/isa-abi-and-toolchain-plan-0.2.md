# fNx ISA, ABI, and Toolchain Plan 0.3

**Date:** 2026-11-05  
**Status:** Early design workstream; ISA encoding, ABI, and P-Code are not frozen

## 1. Scope and architecture family

fNx is one architecture family whose member name encodes its native architectural width N in bits. The family is intended to share one ISA, instruction semantics, opcode registry, and platform contract.

For an fNx member, general-purpose registers, native arithmetic, native data width, pointers, and virtual addresses are N bits. Physical address width and physical memory capacity may be smaller and are handled through translation and protection.

The supported minimum and maximum N remain open. The working architectural rule is that the opcode field is N bits, consistent with N governing native architectural widths. One finite global opcode registry is shared across all members; each member directly represents identifiers within its N-bit range. Existing standardized assignments and meanings must never change. New instructions receive new unique opcode values.

**Opcode width is not instruction length.** Instruction formats, operand fields, register selectors, immediates, extension words, and fetch alignment remain unresolved. Do not assume every operand field must be N bits.

See [fNx family and architectural contract](fnx-family-and-architectural-contract-0.1.md).

## 2. ISA evolution and older-member compatibility

The ISA may grow by adding new opcodes and explicitly versioned capabilities. Unknown opcodes must not silently execute as another operation. Native execution of an unsupported opcode raises a defined illegal-instruction/unsupported-operation exception.

Where possible, every new instruction should include a reference expansion into existing instructions. The expansion must reproduce exact semantics, including overflow, flags, exceptions, privilege, memory ordering, atomicity, and side effects.

Compatibility classes:
- **Expandable:** exact behavior can be generated using an older baseline ISA.
- **Conditionally expandable:** requires specified OS, runtime, privilege, or hardware capabilities.
- **Non-expandable:** cannot be faithfully reproduced on the older target.

The preferred application compatibility path is compiler/installer lowering from P-Code to the target's supported native instructions. The FCP reports capabilities and provides platform management; it is not a serial instruction translator.

## 3. Portable application code

The proposed normal application distribution format is fNx P-Code in a proposed `.fnx` package. P-Code is a separate, versioned portable instruction contract, not serialized native ISA and not assumed to be LLVM IR.

The installer verifies package integrity and requirements, discovers the target fNx member and capabilities, and performs ahead-of-time compilation to native code. A JIT may further optimize hot paths. Generated native code is target-specific derived cache; P-Code remains the portable source of truth and allows regeneration after hardware, ABI, compiler, or runtime changes.

Native binaries remain valid for kernels, bootloaders, drivers, hard real-time paths, direct hardware access, and justified high-performance cases.

See [fNx P-Code and application compatibility](fnx-pcode-and-compatibility-0.1.md).

## 4. Address model

Pointers and virtual addresses are N bits for an fNx member. Physical address width may be smaller and must be discoverable. Translation maps N-bit virtual addresses to available physical memory and device regions.

Specify:
- Virtual-address arithmetic and interpretation.
- Physical-address width and platform limits.
- Page sizes, translation, permissions, and faults.
- Address-space identifiers and invalidation.
- Byte addressing and endianness.
- Load/store widths, alignment, and unaligned access.
- Memory-mapped I/O and device attributes.

Do not reintroduce a fixed 48-, 64-, or 128-bit pointer cap without an explicit architecture decision.

## 5. Compiler and assembler strategy

Reuse and port established toolchains before considering a new compiler. The goal is to make fNx a supported target, not to recreate mature language frontends, optimizers, runtimes, and standard libraries.

Keep hosted and native toolchains distinct:
- A hosted cross-toolchain runs on an existing development host and emits fNx P-Code or native fNx objects.
- A target installer/AOT compiler translates portable P-Code to the installed member's native ISA.
- A native toolchain may eventually run on an fNx system; self-hosting is a later milestone.

Candidate ecosystems:

| Language | Existing implementation | fNx work |
|---|---|---|
| C/C++ | GCC; LLVM/Clang | ISA backend, assembler, linker, ABI, sysroot |
| Rust | rustc with LLVM code generation | LLVM target, ABI, runtime/standard library support |
| Go | gccgo and official Go compiler | Evaluate backend/runtime paths separately |
| Java | OpenJDK javac/JVM bytecode | JVM runtime, GC, native interfaces, platform libraries |

GCC is a strong initial C/C++ candidate. LLVM is complementary and important for the mainstream Rust code-generation path. gccgo and the official Go toolchain are separate candidates and must be evaluated separately. Java bytecode already provides a portable language representation, but running Java still requires a suitable JVM and its runtime services.

Prefer small, reviewable, upstreamable target ports. Keep fNx-specific changes isolated and track upstream releases.

## 6. Foundational native ISA deliverables

- Machine-readable ISA and opcode registry.
- Formal instruction semantics and versioning rules.
- Encoding feasibility study for representative instruction families.
- Assembler and disassembler with encoding/decoding tests.
- Architectural reference interpreter and golden-vector harness.
- Native object format, relocations, linker, and initial sysroot.
- ABI: calling convention, stack, register use, data layout, atomics, exceptions, and system interface.
- Debug information and debugger target.
- Conformance tests shared by interpreter, simulator, and RTL.

The first compiler deliverable should be a hosted cross-compiler for a minimal C program, followed by Rust and Go feasibility work. Java execution should be evaluated through the JVM path.

## 7. ABI topics

The ABI must define:
- N-bit pointer representation and supported integer types.
- N-bit scalar and smaller sub-width data layout.
- Calling convention, argument passing, and return values.
- Stack layout and alignment.
- Caller/callee-saved register policy.
- Thread-private and workspace-shared state.
- Exception and unwind conventions.
- System-call boundary.
- Atomic and synchronization interfaces.
- Process/workspace/thread mapping.
- Object format and relocation definitions.
- Interoperability between native code and P-Code runtime modules.

The ABI must not assume all architectural registers are equally cheap or physically resident.

## 8. Milestones

1. Define supported minimum/maximum N and ratify the global opcode registry with N-bit opcode fields.
2. Complete representative instruction encoding and instruction-length feasibility studies while preserving the N-bit opcode rule.
3. Specify N-bit pointer/virtual-address semantics and physical-address translation contract.
4. Specify P-Code execution model, package manifest, validation, and ABI.
5. Build ISA registry, reference interpreter, assembler, disassembler, and conformance tests.
6. Prototype P-Code-to-native AOT compilation and target-specific cache.
7. Prototype native compiler backends and linker/debugger support.
8. Evaluate runtime/JIT, Java VM, and native interoperability.
9. Integrate with OS and platform services after privilege, memory, interrupt, and ABI contracts stabilize.

No complete instruction encoding, P-Code format, ABI, or compiler backend is selected by this plan.
