# fNx P-Code and Application Compatibility 0.1

**Date:** 2026-11-05  
**Status:** Proposed software distribution architecture; format and ABI not frozen

## 1. Purpose

fNx should support a portable application distribution format that is independent of a particular fNx native width or processor generation.

The proposed model distributes an authoritative portable representation and compiles it for the target machine during installation. Runtime JIT compilation may further optimize hot code. The portable representation remains available so native code can be regenerated when the target hardware or compiler changes.

The working name is **fNx P-Code**. The proposed application/package extension is `.fnx`. Both names and the package layout are proposals, not ratified standards.

## 2. Separate the native ISA from P-Code

These are different contracts:

- **fNx ISA:** the native machine instruction set implemented by an fNx processor.
- **fNx P-Code:** a stable, portable virtual instruction representation distributed with applications.
- **Compiler:** translates source languages into P-Code.
- **Installer/AOT compiler:** validates the package and translates P-Code into native code for the installed system.
- **Runtime:** loads and verifies P-Code metadata, manages execution, and coordinates native code.
- **JIT compiler:** optionally optimizes selected code paths during execution.
- **Native code cache:** stores derived machine-specific code and metadata.

P-Code is not a serialized form of the native ISA. It must have its own versioned specification and semantics so it can remain stable while native ISA encodings and processor implementations evolve.

Do not assume LLVM IR is the package format. LLVM IR is a compiler infrastructure representation with its own versioning and stability considerations. A public application format needs a deliberately governed compatibility, verification, and execution contract.

## 3. Application installation and execution

The intended normal path is:

1. The user installs an `.fnx` application package.
2. The installer verifies package integrity, signatures, manifest, P-Code version, ABI requirements, and declared capabilities.
3. The platform discovers the target fNx member, supported ISA revision, optional instructions, runtime services, and relevant system capabilities.
4. The installer/AOT compiler lowers P-Code to native code for that target.
5. The platform stores the derived native executable/cache with metadata identifying the target profile, ISA revision, ABI, compiler/runtime version, and required capabilities.
6. The runtime loads the native result and provides required services.
7. A JIT may profile and optimize hot paths while preserving P-Code semantics.
8. If the machine or toolchain changes, incompatible native cache entries are invalidated and regenerated from the P-Code.

The user installs the application once. The package remains portable; generated native code is target-specific.

## 4. Compatibility with newer instructions

A newer native ISA revision may add opcodes without changing the semantics of existing assignments.

For each new opcode, the standard should publish a reference expansion into previously defined instructions wherever an exact equivalent exists.

A compiler or installer targeting an older processor can use that expansion before execution. The older processor does not need to recognize the new opcode itself. The generated code may be slower and larger, but the package need not ship permanently expanded machine code.

Compatibility classes:

| Class | Meaning |
|---|---|
| Expandable | Exact behavior can be generated using the older baseline ISA and available runtime services |
| Conditionally expandable | Equivalent behavior requires specified hardware, OS, privilege, or runtime capabilities |
| Non-expandable | Required behavior cannot be faithfully reproduced on the older target |

A target must reject installation or execution when a required non-expandable capability is absent. It must not silently substitute an operation with different semantics.

Equivalence testing must include overflow, flags, exceptions, memory ordering, atomicity, privilege checks, and side effects. For example, multiplication cannot generally be replaced by repeated addition for arbitrary operands. A correct shift-and-add sequence may be appropriate if it preserves the specified integer semantics. Some atomic or device operations cannot be emulated safely without an equivalent primitive.

## 5. Package manifest and contents

A future package specification should define at least:

- Package identity, publisher, version, and signature.
- P-Code format version and required runtime version.
- Application ABI and system interface requirements.
- Minimum fNx family/member requirements, where relevant.
- Required ISA revision and optional capabilities.
- Memory, thread, workspace, and privilege requirements.
- Entry points and module/dependency metadata.
- P-Code sections and optional debug information.
- Optional resources and data.
- Integrity hashes and signature coverage.
- Optional precompiled native variants, clearly marked as derived and non-authoritative.

The package should not require one fixed native width unless the application genuinely depends on a width-specific capability. A portable application may run on f16x, 128-bit member, or another member when the target meets its ABI/runtime requirements and all required operations are natively supported or exactly expandable.

## 6. P-Code design requirements

P-Code needs a formal specification before choosing an encoding. It must define:

- Instruction semantics and versioning.
- Type and integer-width semantics.
- Memory model and atomic operations.
- Control flow, calls, exceptions, and traps.
- Stack/register execution model.
- Module linking and imports/exports.
- Threads, synchronization, and concurrency.
- Bounds, memory safety, and validation rules.
- System calls, host functions, and capability-based access.
- Debugging and source mapping.
- Deterministic behavior requirements, where applicable.
- Native ABI and foreign-function interface boundaries.

A typed register-based format is a candidate worth evaluating, but the execution model is not selected. Stack-based, register-based, and other portable forms must be compared against compiler quality, validation complexity, runtime cost, and long-term compatibility.

## 7. Security model

P-Code is untrusted input until validated. The platform must define:
- Structural and semantic validation.
- Control-flow and type safety checks.
- Memory isolation and bounds enforcement.
- Package signature and publisher trust policy.
- Runtime capability permissions.
- Safe linking and import resolution.
- Native code cache integrity and ownership.
- JIT memory permissions and write/execute separation.
- Debug and profiling authorization.

A valid signature establishes package provenance/integrity, not that the application is safe or trustworthy.

## 8. Native code and exceptions

Portable distribution should be the default application path, not an absolute prohibition on native binaries.

Native executables remain appropriate for boot code, kernels, drivers, hard real-time paths, low-level hardware access, and workloads with justified performance requirements. Their ABI, signing, compatibility, and security contracts must be separately specified.

The P-Code model should not force every system component through a managed runtime.

## 9. Relationship to existing systems

JVM bytecode, .NET CIL, WebAssembly, dynamic binary translation, and ahead-of-time translation systems demonstrate established parts of this approach. fNx is proposing to combine a portable application contract with a family-wide native ISA and install-time target compilation across native widths and processor generations. This is a synthesis of known techniques, not a claim that portable bytecode or binary translation is unprecedented.

## 10. Open decisions

- P-Code execution model and instruction set.
- Package layout and exact `.fnx` format.
- ABI and system-call contract.
- P-Code verification and security model.
- Native cache format and invalidation rules.
- Runtime/JIT responsibilities and policy.
- AOT compiler implementation and toolchain integration.
- Interoperability with native libraries and drivers.
- Capability manifest and compatibility negotiation.

## 11. Related specifications

- [fNx family and architectural contract](fnx-family-and-architectural-contract-0.1.md)
- [fNx control and management plane](fnx-control-and-management-plane-0.1.md)
- [ISA, ABI, and toolchain plan](isa-abi-and-toolchain-plan-0.2.md)
