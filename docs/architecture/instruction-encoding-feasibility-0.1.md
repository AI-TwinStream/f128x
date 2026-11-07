# fNx Instruction Encoding Feasibility Study 0.1

**Date:** 2026-11-06  
**Status:** Comparative design study; no instruction format is selected or ratified

## 1. Purpose

This study compares instruction-format families while preserving the current fNx architectural direction:

- Every member has one native architectural width N bits.
- The opcode field is exactly N bits.
- One global opcode registry assigns one numeric identity and one semantic definition to each operation.
- A member directly represents opcode identifiers in the range 0 through 2^N−1.
- Instruction length, operand representation, register selectors, immediates, and alignment are separate decisions.

This study does not assign opcode values, freeze instruction formats, or select the supported minimum/maximum N.

## 2. Consequence of an N-bit opcode field

An N-bit opcode field imposes a hard lower bound of N bits on an instruction containing that field.

| Member width | Opcode field size | Bytes occupied by opcode |
|---:|---:|---:|
| 8 | 8 bits | 1 |
| 16 | 16 bits | 2 |
| 32 | 32 bits | 4 |
| 64 | 64 bits | 8 |
| 128 | 128 bits | 16 |
| 256 | 256 bits | 32 |
| 512 | 512 bits | 64 |
| 1024 | 1024 bits | 128 |

These are arithmetic consequences of the N-bit rule, not performance measurements.

The cost is significant at larger N. A 128-bit member spends at least 16 bytes per instruction on opcode identity before encoding any operands. A 1024-bit member spends at least 128 bytes. This must be measured against the project's stated preference for N governing architectural widths. It must not be hidden by calling the opcode a mnemonic, prefix, or registry handle.

The opcode field width does not imply that every operand, immediate, register selector, or instruction payload is N bits.

## 3. Candidate format families

All candidates below preserve an N-bit opcode field. They differ in how operands and instruction boundaries are represented.

### Candidate A: N-bit opcode followed by a variable operand stream

Each instruction begins with one N-bit opcode field. A format descriptor and operand payload follow, using compact fields whose widths are specified independently of N.

Possible payload components include:
- Operand count and operand kinds.
- Register selectors.
- Immediate values.
- Displacements and addresses.
- Optional extension payloads.

**Advantages**
- Preserves the N-bit opcode rule directly.
- Allows compact register selectors and small immediates.
- Supports operations with different operand counts and shapes.
- Makes the opcode field and operand grammar conceptually distinct.

**Costs and questions**
- Every instruction pays the N-bit opcode cost.
- Decoder must determine payload shape from the opcode definition and/or a format descriptor.
- Variable instruction length complicates fetch, branch targets, disassembly, and recovery after malformed/truncated instructions.
- The format descriptor itself needs versioning and reserved-value rules.

### Candidate B: N-bit opcode followed by N-bit instruction units

The opcode occupies one N-bit unit. Additional N-bit units carry format descriptors and operands. Instructions consume one or more whole N-bit units.

**Advantages**
- Natural alignment to the member's native width.
- Simple unit-based fetch and instruction-boundary accounting.
- Large immediates and addresses can be represented without inventing sub-unit extension words.

**Costs and questions**
- Small instructions may waste most of each N-bit operand unit.
- For large N, even a simple register selector or small immediate may consume a full 128-, 256-, or 1024-bit unit unless subfields are explicitly packed.
- It is not a fixed-one-unit instruction format: the opcode alone consumes the first unit.
- Byte-addressed storage, endianness, and partial final units still need definitions.

### Candidate C: N-bit opcode followed by packed operand descriptors and payload blocks

The opcode occupies N bits. A compact descriptor stream encodes register selectors, operand types, and payload lengths. Larger immediate/address values are carried in explicitly sized payload blocks.

**Advantages**
- Retains N-bit opcode identity while reducing wasted space for small operands.
- Can make common register-register operations more compact than Candidate B.
- Can represent large values without requiring every field to be N bits.

**Costs and questions**
- More complicated decoder and disassembler.
- Packing rules, descriptor termination, malformed encodings, and extension compatibility require precise definitions.
- Random access to instruction boundaries may be more difficult.
- Variable-width fields need strict limits to prevent unbounded decode work.

## 4. Rejected interpretation: one N-bit instruction per operation

A single N-bit instruction unit cannot generally contain both an N-bit opcode and any non-empty operand fields. It is viable only for operandless instructions or if the opcode is not actually N bits.

Therefore, a one-unit fixed-length instruction format is incompatible with a literal N-bit opcode plus independently encoded operands. The architecture must not claim both without defining an additional representation mechanism.

## 5. Register selector width is independent of N

Register width and register count are separate properties.

For a bank of R architectural registers, a selector requires at least ceil(log2(R)) bits, independent of the width N of each register. For example, selecting among 128 registers requires 7 bits whether each register is 16, 128, or 1024 bits wide.

The number of architectural registers, register classes, operand count, and selector encoding remain open. This study does not assume the 128-bit research profile's register counts are family-wide invariants.

## 6. Required instruction corpus

Before selecting a format, encode a representative corpus including:

1. No-operand control instructions: trap, return, wait, and fence.
2. Register-register arithmetic: add, subtract, compare, and bitwise operations.
3. Register-immediate arithmetic and logical operations.
4. Shifts and rotates with register and immediate counts.
5. Loads and stores with base-plus-offset addressing.
6. Conditional and unconditional branches, including long-range targets.
7. Calls and returns.
8. Multiply and divide, including defined overflow behavior.
9. Atomic read-modify-write and compare/exchange operations.
10. Privileged and system operations.
11. Sub-width scalar and packed-data operations.
12. Operations with large constants or N-bit values.

For every candidate, report encoded size, number of fetch units, decode steps, maximum immediate/displacement reach, and behavior when the instruction is truncated or malformed.

## 7. Evaluation criteria

Use the same semantic corpus and workload assumptions for every candidate.

| Criterion | Required evidence |
|---|---|
| N-bit opcode compliance | Every instruction carries exactly one N-bit opcode field |
| Global registry compatibility | Numeric identity and semantics remain common across members |
| Instruction-boundary recovery | Decoder can locate the next instruction or report a precise fault |
| Code density | Bytes per instruction and bytes per representative program |
| Fetch cost | Number and size of fetch transactions across N values |
| Decode complexity | Bounded steps, state, and implementation complexity |
| Register operands | Selector cost independent of register width |
| Immediate/displacement reach | Direct range and extension cost |
| Branch behavior | Short/long branch encoding and relocation implications |
| Extensibility | New operand forms without changing existing opcode meanings |
| Error behavior | Defined handling for unknown opcode, unsupported operation, malformed/truncated instruction |
| Toolchain support | Assembler, disassembler, linker, debugger, and compiler implementation cost |

At minimum, evaluate the smallest proposed N, f16x, the 128-bit member, and the largest proposed N. Do not extrapolate the 128-bit research profile's simulation results into instruction-encoding performance claims.

## 8. Cross-member opcode allocation

The registry is global, but an N-bit field can represent only identifiers below 2^N.

The allocation policy must make the consequence explicit:
- An identifier representable by a smaller member has the same meaning on every member.
- A larger identifier may be directly representable only on wider members.
- A smaller member must report that the operation is unavailable; it must not reinterpret, truncate, or alias the identifier.
- Portable software may use a published exact expansion where one exists.
- The capability contract must expose both supported operations and the member's representable opcode range.

The project must decide whether standardized opcode allocation is restricted to the smallest supported N or may use the additional identifier space of wider members. This is a family compatibility decision, not merely a registry implementation detail.

## 9. Current assessment

No candidate is selected.

Candidate A is the simplest conceptual starting point for preserving compact operand fields. Candidate C may improve code density but has greater decoder complexity. Candidate B offers simple native-unit alignment but risks severe operand-space waste at large N.

The unavoidable opcode-field cost grows linearly with N. If the project retains N-bit opcode fields through 1024-bit members, the encoding study must accept and quantify the 128-byte opcode field cost per instruction. It must not quietly replace the N-bit field with a shorter token.

## 10. Decisions still required

1. Supported minimum and maximum N.
2. Whether global opcode allocation is limited to identifiers representable by the smallest member.
3. Whether wider-only opcodes are permitted and how software discovers them.
4. Candidate instruction format and instruction-boundary rules.
5. Byte addressing, instruction storage endianness, and alignment.
6. Register counts, register classes, operand count, and selector rules.
7. Immediate, displacement, and branch-target representation.
8. Truncation, malformed-instruction, and decode-fault behavior.
9. Reference corpus and quantitative code-density/fetch/decode results.

## 11. Related specifications

- [fNx family and architectural contract](fnx-family-and-architectural-contract-0.1.md)
- [fNx opcode namespace and allocation proposal](opcode-namespace-and-allocation-proposal-0.1.md)
- [fNx ISA, ABI, and toolchain plan](isa-abi-and-toolchain-plan-0.2.md)
- [fNx roadmap](../roadmap.md)
