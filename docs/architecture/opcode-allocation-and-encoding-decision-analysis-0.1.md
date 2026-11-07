# fNx Opcode Allocation and Encoding Decision Analysis 0.1

**Date:** 2026-11-06  
**Status:** Non-normative decision analysis; no allocation policy or instruction format is selected

## 1. Purpose

The N-bit opcode direction creates two linked design constraints:

1. A member with native width N directly represents opcode identifiers from 0 through 2^N−1.
2. Every instruction carries an N-bit opcode field, so the opcode alone consumes N bits before any operand encoding.

This note evaluates the family-level consequences and gives a small, reproducible encoding corpus. It is intended to make the costs visible before the project chooses a supported N range, registry policy, or instruction format.

Nothing in this note assigns opcode values, fixes register counts, or ratifies an encoding.

## 2. Opcode allocation policy options

Assume the eventual supported family has a smallest member width Nmin. The exact value of Nmin is not selected.

### Policy A: Portable standardized namespace bounded by the smallest member

- Standardized opcode identifiers are allocated only in 0 through 2^Nmin−1.
- Every supported member can directly represent every standardized identifier.
- Optional implementation capabilities may still determine whether a particular CPU implements an operation, but width alone never prevents direct representation.
- The namespace has a clear portability boundary.

**Advantages:** Simple cross-member portability, simpler assemblers and linkers, and no width-specific standardized opcode availability.

**Costs:** The smallest member sets the namespace ceiling for the entire family. This can be restrictive if the project includes very small N values.

### Policy B: One global namespace with wider-only standardized identifiers

- The registry may allocate standardized identifiers above 2^Nmin−1.
- Wider members can directly represent those identifiers.
- Smaller members cannot directly encode them and must report them as unavailable.
- A compiler/runtime may use an exact expansion on a smaller member only when one is specified and semantically exact.

**Advantages:** The global registry can grow with wider members without changing the N-bit field.

**Costs:** A standardized operation is not necessarily directly encodable across the family. Portable binaries need target-aware compilation, capability checks, or exact expansion. A source-level program may be portable while its native instruction stream is not.

### Policy C: Split registry into a portable base and width-dependent standardized extensions

- A base allocation range is restricted to identifiers representable by every supported member.
- Additional standardized ranges are explicitly classified as width-dependent.
- The registry remains globally unique; this is a classification of portability, not separate per-width meanings.
- Toolchains must preserve the distinction between portable-base operations and width-dependent operations.

**Advantages:** Makes the portability boundary explicit while allowing the registry to grow.

**Costs:** Adds registry metadata and toolchain policy. It can become Policy B with better labeling unless conformance rules define the boundary precisely.

## 3. Recommendation for discussion

Prefer **Policy C**, with a strict portable base bounded by the smallest supported N and explicitly marked width-dependent standardized extensions above that boundary.

This recommendation does not select Nmin. It also does not imply that a wider-only operation is a different ISA operation: its numeric identity and semantics remain global and immutable. It means only that not every member can directly encode it.

The registry should record, at minimum:

- Numeric identifier.
- Required minimum opcode width for direct representation.
- Portability class: portable-base or width-dependent.
- Required architectural capabilities.
- Whether an exact expansion exists, with a normative semantic reference.
- ISA revision and lifecycle state.

A compiler must not silently lower an operation to an approximation. An expansion is valid only when it preserves the specified observable semantics, including exceptions, memory ordering, privilege, and side effects.

Before adopting this recommendation, the project must decide whether the family promises that every standardized operation is directly encodable by every member. If that is the promise, Policy A is the only consistent option.

## 4. Illustrative encoding corpus

The following corpus compares compact packed payloads with N-bit instruction-unit payloads. All values are illustrative and exist only to expose scaling behavior.

### Explicit assumptions

- Opcode field: exactly N bits, as the current working direction requires.
- Example register bank: 128 registers, therefore a 7-bit register selector. This is a study assumption, not a family-wide register-count decision.
- Candidate A uses an 8-bit format descriptor for operand-bearing instructions.
- Candidate C uses a 4-bit instruction descriptor and a 4-bit kind tag per operand.
- Immediate and displacement examples use 32 bits; short branch displacement uses 16 bits; long branch target payload uses 128 bits.
- Fields are densely bit-packed for Candidates A and C. The resulting bit count is rounded up to whole bytes for storage.
- Candidate B places the opcode in one N-bit unit and rounds the remaining operand payload up to whole N-bit units.
- No instruction fetch transaction size, cache-line size, or bus width is assumed.

### Representative operations and payload accounting

| Operation | Candidate A payload after opcode | Candidate A total | Candidate C payload after opcode | Candidate C total |
|---|---:|---:|---:|---:|
| No-operand control | 0 | N | 0 | N |
| Register-register arithmetic | 22 bits | N + 22 | 26 bits | N + 26 |
| Register-immediate arithmetic | 47 bits | N + 47 | 51 bits | N + 51 |
| Base-plus-offset load/store | 54 bits | N + 54 | 58 bits | N + 58 |
| Short conditional branch | 24 bits | N + 24 | 24 bits | N + 24 |
| Long branch | 136 bits | N + 136 | 136 bits | N + 136 |

Candidate A's 8-bit descriptor is followed by the relevant selectors and payload. Candidate C's descriptor and operand-kind tags cost more for some operations but allow operand forms to be described independently. These are deliberately simple models, not proposed bit layouts.

Candidate B's total size is:

`N + ceil(payload_bits / N) * N`

This means every non-empty operand payload consumes at least one additional N-bit unit. It is intentionally a straightforward whole-unit baseline, not an optimized packed format.

### Candidate A storage cost

Bytes per instruction after rounding the packed bit count up to whole bytes:

| Operation | f16x | 128-bit member | f1024x |
|---|---:|---:|---:|
| No-operand control | 2 | 16 | 128 |
| Register-register arithmetic | 5 | 19 | 131 |
| Register-immediate arithmetic | 8 | 22 | 134 |
| Base-plus-offset load/store | 9 | 23 | 135 |
| Short conditional branch | 5 | 19 | 131 |
| Long branch | 19 | 33 | 145 |

The table isolates the encoding floor. It does not include alignment padding, cache effects, metadata, instruction fetch granularity, or program-level branch relaxation.

### Candidate B whole-unit storage cost

Bytes per instruction when the operand payload is rounded up to whole N-bit units:

| Operation | f16x | 128-bit member | f1024x |
|---|---:|---:|---:|
| No-operand control | 2 | 16 | 128 |
| Register-register arithmetic | 6 | 32 | 256 |
| Register-immediate arithmetic | 8 | 32 | 256 |
| Base-plus-offset load/store | 10 | 32 | 256 |
| Short conditional branch | 6 | 32 | 256 |
| Long branch | 20 | 48 | 256 |

At 128 bits, even a small operand payload consumes a full additional 16-byte unit. At 1024 bits, the same rule turns many simple instructions into 256-byte encodings. Candidate B therefore provides a useful alignment baseline but is a poor density candidate unless subfields are packed within each unit, at which point it begins to resemble Candidate A or C.

### Candidate C

Candidate C's illustrative payload is four bits larger than Candidate A for the register-register, register-immediate, and load/store examples, and equal for the branch examples. Its byte counts are therefore calculated from the Candidate C totals above. The difference is small relative to the N-bit opcode cost at large N.

This simple corpus does not establish that Candidate C is better or worse. A realistic descriptor grammar may reduce repeated tags or may add continuation and length overhead. That must be tested against a fuller corpus.

## 5. Findings

1. The N-bit opcode field dominates instruction size as N grows. No operand packing strategy removes that cost.
2. Compact operand descriptors matter most at smaller N. At 128 and 1024 bits, descriptor savings are comparatively minor beside the opcode field.
3. Whole-N-bit operand units are especially expensive for large N unless the architecture permits packed subfields.
4. Register selector width is driven by register count, not register data width.
5. The family must distinguish source-level portability, portable native encoding, and implementation capability. They are not interchangeable promises.
6. The instruction format cannot be selected responsibly without a supported N range and an explicit opcode-allocation policy.

## 6. Required next experiments

The next quantitative pass should:

- Add multiple register-bank sizes and operand counts without making any one count normative.
- Include scalar sub-width operations, vector/packed operations, atomics, calls, returns, privilege transitions, and memory-ordering operations.
- Measure complete basic blocks and representative programs, not just isolated instructions.
- Include alignment, branch relaxation, instruction-boundary recovery, and truncated/malformed encodings.
- Compare fixed descriptor, tagged descriptor, and bounded variable-length payload schemes.
- Report bytes per operation, bytes per basic block, fetch units under explicitly stated fetch widths, and maximum decode work.
- Test all proposed member widths, especially the eventual smallest member, the 128-bit member, and the eventual largest member.

## 7. Decision status

| Decision | Status |
|---|---|
| N-bit opcode field | Selected working direction; not ratified |
| Smallest and largest supported N | Open |
| Global standardized allocation policy | Open; Policy C recommended for discussion |
| Width-dependent standardized opcodes | Open |
| Candidate A, B, or C | None selected |
| Register counts and classes | Open |
| Instruction length and boundary rules | Open |
| Byte addressing, endianness, and alignment | Open |
| Trap and malformed-instruction behavior | Open |
| Quantitative program-level comparison | Not yet performed |

## 8. Related specifications

- [fNx opcode namespace and allocation proposal](opcode-namespace-and-allocation-proposal-0.1.md)
- [fNx instruction encoding feasibility study](instruction-encoding-feasibility-0.1.md)
- [fNx family and architectural contract](fnx-family-and-architectural-contract-0.1.md)
- [fNx roadmap](../roadmap.md)
