# fNx Family and Backplane Compatibility 0.1

**Date:** 2026-11-05  
**Status:** Working architectural direction; see the [fNx family and architectural contract](fnx-family-and-architectural-contract-0.1.md) for current normative decisions and unresolved items.

This document is retained as the original family/backplane study. Its earlier assumptions that the family begins at a 128-bit native width and that pointers are fixed at 128 bits are superseded.

## Current family model

fNx is the architecture family. N is the native architectural width in bits. Candidate members include f16x, f32x, f64x, the 128-bit member, f256x, f512x, and f1024x. The supported minimum and maximum N are not frozen. The N-bit rule applies to every member, including the opcode field; smaller members consequently have smaller directly representable opcode ranges.

All members share one ISA and one global opcode registry. A CPU architectural unit has one native width. A system may combine different-width CPU units through the common backplane contract; it does not combine different native widths inside one CPU unit.

For every member, the current working rule is N-bit general-purpose registers, native arithmetic, native data width, pointers, and virtual addresses. Physical address width and physical memory capacity may be smaller, with translation/protection mapping the virtual address space onto available resources.

Smaller scalar and packed operations are defined as operations within an N-bit architecture. They do not imply mixed-width CPU modules.

## Opcode and instruction-format direction

The selected working opcode-field direction is N bits. The opcode registry remains one finite global namespace shared across all members. A numeric opcode has the same meaning everywhere; wider opcode fields do not create new instructions automatically.

New instructions receive unique opcode values. Existing assignments and semantics are immutable. Unassigned values remain reserved. Unsupported instructions raise a defined exception unless portable code has already been compiled to an equivalent supported sequence.

Opcode width does not determine total instruction length. Operand encoding, register selectors, immediate representation, instruction length, and extension formats remain open.

## Width-neutral backplane

Different fNx members must interoperate through a common protocol that is independent of native register and datapath width. The protocol should define byte-addressed transactions, transfer length, ordering, completion, errors, flow control, protection attributes, capability negotiation, and atomic-operation support.

Wider local operations may be segmented into protocol transactions only where the memory and atomicity semantics permit it. Segmentation must not invent atomicity.

Backplanes may be connected hierarchically, including backplanes of backplanes. Discovery, routing, addressing, capability propagation, fault containment, reset, and management contracts must work across hierarchy levels.

## Compatibility layers

Compatibility is not a single property:

1. **ISA compatibility:** common instruction semantics and global opcode assignments.
2. **Application compatibility:** portable P-Code and a defined runtime/ABI contract.
3. **Memory interoperability:** common data layout, address, ordering, and atomicity rules.
4. **Fabric compatibility:** common width-neutral backplane protocol.
5. **Performance portability:** not guaranteed; narrower links or members may require additional transfers or instruction expansion.

See [fNx P-Code and application compatibility](fnx-pcode-and-compatibility-0.1.md) for the proposed portable distribution and older-instruction expansion model, and [fNx control and management plane](fnx-control-and-management-plane-0.1.md) for FCP and hierarchical management.

## Historical content retained

The original 128-bit member/f256x/f512x/f1024x examples, 128-bit pointer proposal, and protocol candidate lists remain useful historical design inputs. They are not the current family-wide specification. Do not use this document's earlier 128-bit pointer statements as normative.
