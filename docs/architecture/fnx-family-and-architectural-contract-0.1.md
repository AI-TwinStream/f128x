# fNx Family and Architectural Contract 0.1

**Date:** 2026-11-05  
**Status:** Working architectural direction; not a frozen ISA or implementation specification

## 1. Purpose and terminology

**fNx** is the architecture family and open-standard project. The value of N in a member name is its native architectural width in bits.

Examples include `f16x`, `f32x`, `f64x`, a member with a 128-bit native width, `f256x`, `f512x`, and `f1024x`. These are width-specific members of one family, not independent architectures with unrelated ISAs.

The minimum supported N is still under consideration. A 16-bit minimum is the current leading candidate because it provides a useful opcode namespace without introducing a separate fixed-width opcode field. It is a candidate, not yet a ratified minimum. Smaller widths such as f8x remain a question for the family specification.

Use these terms consistently:

- **fNx:** the overall architecture family and standard.
- **fNx member/profile:** one implementation width, such as the 128-bit implementation profile.
- **fNx ISA:** the common native instruction-set contract.
- **fNx P-Code:** the proposed portable application instruction representation.
- **FCP:** the proposed fNx Control Processor, responsible for platform management and capability services.

A repository, document, or research result may refer specifically to the 128-bit implementation profile when it genuinely describes that member. Do not use a width-specific profile as a synonym for the whole family.

## 2. Family invariants

The family is intended to preserve one common architecture across N:

- One ISA and one globally assigned opcode namespace.
- Identical instruction semantics and control-flow meaning across members.
- One architectural register model, scaled to N bits.
- N-bit native arithmetic and data values.
- N-bit pointers and virtual addresses.
- Common privilege, exception, protection, workspace, and thread concepts.
- A common width-neutral platform and backplane contract.
- Explicitly discoverable implementation capabilities.

A CPU architectural unit has exactly one native width. A CPU unit is not a mixture of 16-bit and 128-bit execution modules. Different fNx members may coexist in a system and communicate through the common platform fabric.

An operation has one meaning throughout the family. Its native-width result and operand behavior are determined by N, not by a different opcode interpretation.

## 3. Native-width architectural state

For an fNx member:

| Architectural element | Working rule |
|---|---|
| General-purpose registers | N bits |
| Native integer arithmetic | N bits |
| Native data width | N bits |
| Pointers | N bits |
| Virtual addresses | N bits |
| Native-width address calculations | N-bit architectural operations |
| Smaller scalar/packed operations | Explicit sub-width semantics within the N-bit architecture |

This is a family rule, not a claim that every implementation must build every possible operation as a single-cycle N-bit datapath.

The architecture may define 8-, 16-, 32-, 64-bit and other sub-width operations. Those operations do not create mixed-width CPU modules. They are operations defined within one N-bit member.

Register count is independent of register width. A 128-entry register bank needs a 7-bit selector whether each register contains 16, 128, or 1,024 bits. Register counts and operand selector formats remain separate ISA decisions.

## 4. Addressing and translation

Pointers and virtual addresses are N bits. Do not impose a fixed 48-, 64-, or 128-bit architectural pointer ceiling across the family.

The physical address width and installed physical memory capacity may be smaller than N. Address translation, protection, and implementation-defined physical-address limits map the N-bit virtual address space onto available physical resources.

The specification must define:
- Virtual-address interpretation and the full N-bit arithmetic behavior.
- Translation structures and their architectural visibility, if any.
- Physical-address width discovery and limits.
- Page sizes, permissions, faults, and address-space identifiers.
- Canonicality rules, if any. Do not assume canonical-address compression without a decision.
- Load/store alignment, access sizes, and unaligned behavior.
- Memory-mapped I/O and device address handling.

The size of a virtual address does not require the platform to populate that entire address space with physical memory.

## 5. Opcode namespace and encoding direction

The current direction is for the opcode field width to equal N bits.

The opcode registry is nevertheless **one finite, global registry** shared by all fNx members. Increasing N does not create a different instruction set or automatically define more instructions.

For example, opcode value 42 identifies the same instruction in every member. It is represented in the member's N-bit opcode field, with the same numeric value. Larger members have more representable opcode values, but those values remain unassigned until the standard defines them.

Rules under consideration:

- Each instruction receives one unique global numeric opcode.
- Existing opcode assignments and semantics are immutable once standardized.
- Unassigned values are reserved for future standardization.
- An unrecognized or unsupported opcode must never silently execute as another instruction.
- Native execution of an unsupported opcode raises a defined illegal-instruction/unsupported-operation exception.
- Portable code may be compiled or expanded to an older supported instruction sequence before execution, where an exact compatible implementation exists.

A 16-bit minimum N is currently favored because it yields 65,536 representable opcode values without adding a separate fixed 8-bit opcode scheme. This remains provisional until the minimum N and opcode format are formally decided.

**Do not infer instruction length from opcode width.** The overall instruction format, instruction length, operand fields, register selectors, immediates, extension words, and fetch alignment remain unresolved. In particular, an N-bit opcode does not require every operand field to be N bits.

## 6. ISA evolution and compatibility

The common ISA may evolve by adding uniquely assigned opcodes and explicitly versioned capabilities. It must not change the meaning of an existing opcode.

Every new instruction should include:
- A precise semantic definition.
- Defined exceptions, flags, overflow, privilege, memory ordering, and side effects.
- A reference implementation or executable conformance tests.
- A classification as expandable, conditionally expandable, or non-expandable on older members.
- A canonical lowering/expansion sequence when equivalent behavior can be implemented using an older baseline.

An older processor can execute a newer operation only when its exact semantics can be reproduced using available instructions and platform services. This is not guaranteed for every operation. Atomicity, memory ordering, privileged effects, and hardware-specific side effects must not be approximated away.

The compiler/installer/runtime is the preferred compatibility layer for application code. The FCP is not intended to sit in the execution path for every instruction.

## 7. Mixed-width systems and backplanes

Different fNx members may coexist in one system. Each CPU unit retains one native width; the system fabric provides interoperability.

The fabric contract must be width-neutral. It should define byte-addressed transactions, explicit lengths, transaction types, ordering, completion, errors, flow control, protection attributes, capability negotiation, and supported atomic operations.

Wider local operations may be split into multiple fabric transactions when the architectural semantics permit it. Splitting must not invent atomicity. A transfer requiring atomic completion needs a protocol-supported atomic transaction or must be rejected/handled by a defined alternative.

Backplanes may be connected hierarchically, including backplanes of backplanes. The fabric and management contracts must support discovery, routing, addressing, and fault isolation across those levels. Hierarchical topology does not alter the native width of any CPU unit.

## 8. Architectural capacities versus implementation

Architectural namespaces and capacities must be distinguished from physical residency and implementation resources. An implementation may expose smaller discoverable limits while preserving the semantics of the facilities it supports.

The current 128-bit member workspace/thread/register counts are member-specific research targets, not automatically family-wide constants. Do not assume that every N has identical workspace capacity, thread capacity, or physical register residency until those are separately decided.

## 9. Decision status

| Topic | Current direction | Status |
|---|---|---|
| Family name | fNx | Adopted project terminology |
| N meaning | Native architectural width in bits | Working invariant |
| One native width per CPU unit | Required | Working invariant |
| Common ISA across N | Required | Working invariant |
| GPR/arithmetic/data width | N bits | Working invariant |
| Pointer/virtual-address width | N bits | Working invariant |
| Physical address width | May be smaller than N | Working direction |
| Opcode field width | N bits | Leading design direction |
| Global opcode registry | One finite shared namespace | Working direction |
| Minimum N | 16 bits favored; f8x still unresolved | Provisional |
| Instruction length and operand format | Not decided | Open |
| Register counts across members | Not decided as a family invariant | Open |
| Backplane hierarchy | Supported by common width-neutral contract | Working requirement |
| ISA compatibility expansion | Exact compiler/runtime lowering where possible | Working direction |

## 10. Related specifications

- [fNx P-Code and application compatibility](fnx-pcode-and-compatibility-0.1.md)
- [fNx control and management plane](fnx-control-and-management-plane-0.1.md)
- [Family and backplane compatibility research](family-and-backplane-compatibility-0.1.md)
- [ISA, ABI, and toolchain plan](isa-abi-and-toolchain-plan-0.2.md)
