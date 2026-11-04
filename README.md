# f128x

**f128x** is an ambitious 128-bit evolution of the Texas Instruments TMS9900 architecture. The project explores how to carry the TMS9900's distinctive workspace-oriented programming model into a modern processor with a substantially larger register file and native 128-bit computation.

The first platform target is the TI-99/4A, whose TMS9900 heritage gives the project its architectural starting point. f128x is a successor architecture, not a plug-in arithmetic coprocessor and not a promise of TMS9900 binary compatibility.

## Architectural direction

The current working baseline is:

- **128 general-purpose registers (GPRs), each 128 bits wide.**
- A native 128-bit scalar integer datapath.
- A hardware register file, with a modernized workspace/window mechanism investigated as an architectural feature.
- Dedicated control state for the program counter, status, interrupt/exception handling, and address-space control.
- A TMS9900-derived programming model: workspace-oriented register organization, orthogonal operations where practical, and a deliberate separation between CPU state and platform devices.
- Explicitly defined byte, word, and wider memory operations.

The 128 GPRs contain 2,048 bytes (2 KiB) of architectural register state. That is a deliberate design point, not an accidental consequence of widening the original sixteen workspace registers.

## Widths are independent

A 128-bit register does not require 128-bit addresses, instructions, or external buses. Register width, scalar arithmetic width, virtual address width, physical address width, instruction encoding, and platform bus width will be specified independently.

## TI-99/4A platform

The TI-99/4A is the first platform target, not a constraint that every f128x implementation must reproduce. The platform work will investigate retaining original video, sound, keyboard, and expansion devices through explicit adapters and bridges. Board-level electrical and timing compatibility must be demonstrated, not assumed.

## Status

Early architecture definition. The 128 × 128-bit GPR baseline is selected for exploration. Instruction encoding, address width, register-window semantics, ABI, memory model, FPGA target, and board-level interface remain open until documented and tested.

## Documents

- [Architecture overview](docs/architecture/overview.md)
- [Register file and workspace model](docs/architecture/register-file.md)
- [TI-99/4A platform strategy](docs/platforms/ti-99-4a.md)
- [Initial roadmap](docs/roadmap.md)
