# f128x Architecture Overview

## 1. Purpose and lineage

f128x is a 128-bit evolution of the Texas Instruments TMS9900 architecture. It takes the TMS9900's workspace-oriented model as a point of departure and redesigns it for a much larger register file, native 128-bit computation, and a modern implementation.

This is not an attempt to reproduce the TMS9900 ISA or to preserve its binary encoding. The goal is to preserve and improve architectural ideas, not fossilize implementation limits.

The architecture remains platform-independent. TI-99/4A integration is a platform implementation and compatibility problem, not the definition of the ISA itself.

## 2. Selected working baseline

| Architectural element | Working direction |
|---|---|
| General-purpose registers | **128 registers** |
| GPR width | **128 bits each** |
| Total GPR storage | **16,384 bits / 2,048 bytes (2 KiB)** |
| Native scalar integer width | 128 bits |
| Register implementation | Hardware register file |
| Workspace heritage | Retain as a modernized workspace/window mechanism; semantics to be specified |
| PC and status | Dedicated architectural control state |
| Address width | Independent decision; do not assume 128 bits |
| Instruction encoding | Open decision |
| External memory bus | Platform-specific |
| SIMD/vector | Separate design decision; 128-bit scalar GPRs do not automatically define SIMD |
| Floating point | Separate specification |

The 128-register choice is the ambitious successor baseline. It replaces the original TMS9900's sixteen 16-bit workspace registers as the design target. The architecture must not quietly fall back to sixteen registers for implementation convenience.

## 3. TMS9900 ideas to preserve or evolve

- **Workspace-oriented programming:** retain a first-class concept for organizing register context and making context changes efficient.
- **Register-indirect and indexed addressing heritage:** evaluate which addressing modes remain useful in a modern load/store and memory model.
- **Orthogonal operations:** favor consistent instruction semantics and predictable operand forms.
- **Memory-to-memory heritage:** preserve the useful programming intent where appropriate, but do not require every operation to execute directly against memory. Evaluate explicit load/store operations and specialized memory operations on their merits.
- **Control state separation:** retain explicit PC/status/control state, with a modern exception and interrupt model.

The architecture must document which concepts are preserved, transformed, or deliberately dropped.

## 4. Width model

Keep these dimensions independent:

1. **Register width:** 128 bits for each GPR.
2. **Scalar integer width:** native 128-bit arithmetic, logical, compare, shift, and rotate operations.
3. **Memory access width:** define byte, 16-bit word, and wider accesses explicitly. The supported set and atomicity guarantees remain to be specified.
4. **Virtual address width:** software-visible address width, if virtual memory is supported.
5. **Physical address width:** width presented to the memory subsystem.
6. **Instruction encoding:** instruction length and operand encoding, independent of GPR width.
7. **External bus width:** property of a particular implementation/platform.

A 128-bit register can hold a pointer, but the architecture must define which bits are meaningful for an address. A 128-bit datapath does not force 128-bit pointers or buses.

## 5. CPU state

The architectural specification must define:

- 128 × 128-bit GPRs.
- Dedicated program counter and status register.
- Workspace/window control state, if retained in the final model.
- Exception, interrupt, privilege, and address-space control state.
- Defined reset state and exception entry/return behavior.

The register-file document records the working organization and open questions. Do not treat implementation details such as banking, bypass paths, or physical register-file port counts as ISA requirements unless software can observe them.

## 6. Memory and addressing

The architecture should make a clear distinction between register operations and memory operations. Define byte order, byte addressing, load/store sizes, alignment, atomicity, and memory ordering before an ABI is frozen.

The initial address width should be selected from realistic platform and software requirements. The TI-99/4A compatibility subsystem may expose a much smaller legacy address space than native f128x software.

## 7. Compatibility strategy

f128x is not TMS9900 binary compatible. Compatibility should be provided at explicit boundaries:

- Peripheral bus adapters for TI-99/4A devices.
- Platform firmware and drivers for reset, interrupts, timers, video, sound, and input.
- Optional TMS9900 emulation or translation, evaluated separately.
- Optional source-level or API-level migration tools.

Do not burden the core ISA with accidental constraints from the original machine.

## 8. Verification principles

- Define instruction semantics before RTL implementation.
- Create an executable reference model and instruction-level tests.
- Test arithmetic boundaries, carry/borrow, signedness, shifts, rotates, and overflow semantics.
- Verify all 128 GPRs, including register aliasing, window transitions, and exception preservation.
- Add formal properties for register, ALU, control-flow, and memory-interface behavior where practical.
- Keep ISA conformance tests independent of any one FPGA board.

## 9. Open decisions

- Exact workspace/window semantics and whether windows overlap.
- Register access rules for GPRs 0 through 127.
- Instruction encoding and instruction length strategy.
- Endianness and byte-addressing model.
- Address width and physical memory map.
- Load/store sizes, alignment, unaligned access behavior, and atomic operations.
- Interrupt, exception, privilege, and virtual-memory architecture.
- Multiplication/division and other expensive operations in the base ISA or extensions.
- ABI, assembler syntax, object format, and compiler/toolchain strategy.
- FPGA family, register-file implementation, and board-level integration.

These decisions require documented trade-off analysis before an ISA version is declared stable.
