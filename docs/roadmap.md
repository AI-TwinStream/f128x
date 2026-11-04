# f128x Roadmap

This roadmap is intentionally staged. The first deliverable is a precise, testable ISA specification, not a board full of unverified RTL.

## 0. Architecture definition

- [ ] Specify architectural state: GPR count, PC, status, control registers.
- [ ] Specify 128-bit integer semantics, flags, carry/borrow, overflow, shifts, and rotates.
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
- [ ] Add small bare-metal examples and a test runner.

## 2. FPGA core

- [ ] Select HDL and FPGA development board.
- [ ] Implement register file and 128-bit ALU.
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
- [ ] Publish a native f128x TI platform firmware and libraries.

## 4. Toolchain and compatibility

- [ ] Stabilize assembler syntax and object format.
- [ ] Add linker and runtime support.
- [ ] Evaluate compiler backend feasibility.
- [ ] Investigate GPL runtime compatibility.
- [ ] Evaluate legacy TMS9900 software emulation/translation as an optional compatibility project.

## First milestone

**M0: f128x ISA v0.1 draft**

A written, internally consistent specification for architectural state, 128-bit integer semantics, addressing model, memory operations, control flow, and exceptions, accompanied by reference-model tests. No FPGA board commitment is required to complete M0.

## Non-goals for the first milestone

- Reproducing the TMS9900 ISA.
- Making every address or bus 128 bits.
- Designing custom silicon.
- Claiming compatibility with all TI-99/4A software.
- Selecting a specific FPGA board before the ISA and platform constraints are understood.
