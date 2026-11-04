# f128x Roadmap

The first deliverable is a precise, testable ISA specification, not a board full of unverified RTL. The selected working baseline is a TMS9900-derived architecture with 128 general-purpose registers of 128 bits each.

## 0. Architecture definition

- [x] Select the ambitious GPR baseline: 128 registers × 128 bits (2 KiB total).
- [ ] Specify workspace/window semantics and how they relate to the 128-register file.
- [ ] Specify architectural state: GPR access, PC, status, and control registers.
- [ ] Specify 128-bit integer semantics, flags, carry/borrow, overflow, shifts, and rotates.
- [ ] Decide whether 8-, 16-, 32-, 64-, and 128-bit register sub-operations are supported and define their semantics.
- [ ] Choose instruction encoding and define reserved/extension space.
- [ ] Choose byte order, byte addressing, load/store widths, and alignment rules.
- [ ] Choose virtual and physical address widths independently of GPR width.
- [ ] Specify exceptions, interrupts, reset state, and privilege model.
- [ ] Define ABI, stack convention, and calling convention.
- [ ] Publish an initial ISA specification and conformance test format.

## 1. Software reference model

- [ ] Implement an instruction-set simulator/reference model.
- [ ] Add assembler and disassembler.
- [ ] Add instruction-level tests and golden vectors.
- [ ] Add tests for all 128 GPRs, workspace/window transitions, and context preservation.
- [ ] Add small bare-metal examples and a test runner.

## 2. FPGA core

- [ ] Select HDL and FPGA development board.
- [ ] Synthesize and measure candidate 128 × 128-bit register-file organizations.
- [ ] Evaluate register-file banking, read/write port counts, bypassing, and timing.
- [ ] Implement the 128-bit ALU.
- [ ] Implement decode, control flow, load/store, and exception behavior.
- [ ] Integrate block RAM and a minimal debug/serial interface.
- [ ] Run simulation and formal checks where practical.
- [ ] Run the same ISA conformance suite against simulator and RTL.

## 3. TI-99/4A platform bring-up

- [ ] Complete board-level electrical and timing characterization.
- [ ] Choose internal replacement versus external bridge/carrier architecture.
- [ ] Build safe clock/reset and memory interface prototypes.
- [ ] Implement TI device adapters incrementally.
- [ ] Bring up input, video, sound, and selected expansion devices.
- [ ] Publish native f128x TI platform firmware and libraries.

## 4. Toolchain and compatibility

- [ ] Stabilize assembler syntax and object format.
- [ ] Add linker and runtime support.
- [ ] Evaluate compiler backend feasibility.
- [ ] Investigate GPL runtime compatibility.
- [ ] Evaluate legacy TMS9900 software emulation/translation as an optional compatibility project.

## First milestone

**M0: f128x ISA v0.1 draft**

A written, internally consistent specification for architectural state, the 128 × 128-bit GPR file, workspace/window semantics, 128-bit integer operations, addressing model, memory operations, control flow, and exceptions, accompanied by reference-model tests. No FPGA board commitment is required to complete M0.

## Non-goals for the first milestone

- Reproducing the TMS9900 ISA or binary encoding.
- Making every address or bus 128 bits.
- Designing custom silicon.
- Claiming compatibility with all TI-99/4A software.
- Selecting a specific FPGA board before the ISA and platform constraints are understood.
