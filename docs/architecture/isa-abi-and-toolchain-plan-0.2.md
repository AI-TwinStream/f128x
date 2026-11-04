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

Evaluate LLVM and GCC backend feasibility in parallel. The first software deliverables should include:
- Machine-readable ISA description.
- Assembler and disassembler.
- Instruction encoding/decoding tests.
- Architectural reference interpreter.
- Compiler backend prototype.
- Linker relocation definitions.
- ABI document.
- Debug information and debugger target.
- Conformance tests and golden vectors.

A minimal assembler and interpreter may precede a production compiler backend, but the ISA must be designed with compiler lowering and register allocation in mind.

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

1. Encoding feasibility study with representative instruction families.
2. Address-width and load/store model decision.
3. Minimal assembler/disassembler and interpreter.
4. Compiler backend prototype and register-allocation experiment.
5. ABI draft and object-file integration.
6. Debugger and conformance suite.
7. OS port feasibility after platform contracts stabilize.

No instruction width, pointer width, compiler backend, or ABI is selected by this document.
